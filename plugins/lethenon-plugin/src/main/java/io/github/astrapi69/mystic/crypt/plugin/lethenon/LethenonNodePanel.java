/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import java.awt.Font;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import io.github.astrapi69.lethenon.ChainRejected;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.component.JMCheckBox;
import io.github.astrapi69.swing.model.component.JMPasswordField;
import io.github.astrapi69.swing.model.component.JMTextArea;
import io.github.astrapi69.swing.model.component.JMTextField;

/**
 * Runs a node of the chain library on a chain file and shows what it does, every second (#530 step
 * 2): its height, its peers, the transfers waiting in its pool, the blocks it mined, and why
 * connections were refused.
 * <p>
 * UI over {@link NodeSupport}, which starts the node and knows no Swing type (architecture.md: no
 * network or chain logic here). Starting can take seconds - every configured peer is dialled - and
 * stopping waits for the mining round in progress, so both run off the event dispatch thread. The
 * node stops with its window as well: closing the window is no way to leave a node listening in
 * the background. The wallet's password is used once and cleared from the field and the model when
 * the node is started, whether it started or not.
 * <p>
 * It answers in fields a child can read (#604), every second while the node runs: first one
 * sentence in a larger font, "Your node runs on port 18480: its chain ends at block 1,200. It has
 * found 3 blocks for you so far.", then the port, the last block, the other nodes connected, the
 * transfers waiting and the blocks found. Why connections were refused is behind "Show details",
 * closed at first.
 */
public class LethenonNodePanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = Logger.getLogger(LethenonNodePanel.class.getName());

	/** How often the status is read from the running node */
	private static final int REFRESH_MILLIS = 1_000;

	/** A field that shares its cell with the button that fills it from a file chooser */
	private static final String WITH_BUTTON = "growx, split 2";

	/** Something that keeps its own width instead of growing with the cell */
	private static final String OWN_WIDTH = "alignx left, width pref!";

	private final LethenonNodePanelModel modelObject = new LethenonNodePanelModel();

	/** Whom to ask before a chain file of lethenon 0.5.0 or earlier is converted (#581) */
	private final ChainConversionConsent consent;

	private final JMTextField txtChainFile = new JMTextField(34);

	private final JMTextField txtPort = new JMTextField(8);

	private final JMTextField txtPeers = new JMTextField(34);

	/**
	 * The language of the window; the field initializers below read it, so it has its value before
	 * the constructor sets the one it was given
	 */
	private Locale locale = Locale.getDefault();

	/** The wallet the window signs with: the vault's, or a wallet file (#706) */
	private final transient WalletRow walletRow;

	private final JMCheckBox chkMine = new JMCheckBox(
		text("lethenon.node.check.mine", "find blocks too, paid to my wallet"));

	private final JMTextField txtWalletFile = new JMTextField(34);

	private final JMPasswordField txtPassword = new JMPasswordField(34);

	private final JMTextField txtPun = new JMTextField(34);

	private final JMTextArea txtAnswer = AnswerFields.answerArea(text("lethenon.node.tooltip.answer",
		"what your node does, in one sentence, read again every second"));

	private final JMTextField txtListening = AnswerFields.readOnly("txtListening",
		text("lethenon.node.tooltip.listening",
			"the port other nodes reach yours on; give it with your address to whoever should "
				+ "connect"));

	private final JMTextField txtLastBlock = AnswerFields.readOnly("txtLastBlock",
		text("lethenon.node.tooltip.last.block",
			"the number of the last block in your node's chain; it grows as new blocks arrive"));

	private final JMTextField txtConnected = AnswerFields.readOnly("txtConnected",
		text("lethenon.node.tooltip.connected",
			"how many other nodes your node talks to now; they pass blocks and transfers along"));

	private final JMTextField txtWaiting = AnswerFields.readOnly("txtWaiting",
		text("lethenon.node.tooltip.waiting",
			"how many transfers wait in your node for the next block"));

	private final JMTextField txtMined = AnswerFields.readOnly("txtMined",
		text("lethenon.node.tooltip.mined",
			"how many blocks your node found and kept; each one pays your wallet"));

	private final JButton btnDetails;

	private final JPanel pnlDetails = new JPanel(ToolForm.newLayout());

	private final JMTextArea txtRefusals = new JMTextArea(3, 40);

	private final JLabel lblResult = new JLabel(" ");

	private final JButton btnStart = LethenonSwing.button("btnStartNode",
		text("lethenon.node.button.start", "Start my node"), event -> onStart(),
		text("lethenon.node.tooltip.start",
			"share your chain file with other nodes, connect to the ones listed, and find blocks "
				+ "when that is ticked"));

	private final JButton btnStop = LethenonSwing.button("btnStopNode",
		text("lethenon.node.button.stop", "Stop my node"), event -> onStop(),
		text("lethenon.node.tooltip.stop",
			"stop finding blocks once the search in progress ends, then stop the node"));

	private final Timer refresh = new Timer(REFRESH_MILLIS, event -> showTheStatus());

	/** The node this window started, null while none runs */
	private transient RunningNode node;

	/** Whether the window was closed: a node that finishes starting afterwards is stopped again */
	private boolean closed;

	private boolean stopsWithItsWindow;

	/**
	 * Instantiates a new {@link LethenonNodePanel}, prefilled with the chain file from the plugin's
	 * settings
	 */
	public LethenonNodePanel()
	{
		this(ChainConversionConsent.ASK);
	}

	/**
	 * Instantiates a new {@link LethenonNodePanel} that asks through the given consent before it
	 * converts a chain file of lethenon 0.5.0 or earlier (#581)
	 *
	 * @param consent
	 *            whom to ask
	 */
	LethenonNodePanel(final ChainConversionConsent consent)
	{
		this(consent, LethenonVault.current(), Locale.getDefault());
	}

	/**
	 * Instantiates a new {@link LethenonNodePanel} over the given vault
	 *
	 * @param consent
	 *            whom to ask before a chain file is converted
	 * @param vault
	 *            the plugin's view of the vault; its wallet is the one signed with
	 * @param locale
	 *            the language of the window
	 */
	LethenonNodePanel(final ChainConversionConsent consent, final LethenonVault vault,
		final Locale locale)
	{
		super(ToolForm.newLayout());
		this.consent = consent;
		this.locale = locale;
		walletRow = new WalletRow(vault, locale, txtWalletFile,
			() -> LethenonSwing.chooseFile(this, modelObject.getWalletFile())
				.ifPresent(txtWalletFile::setText));
		btnDetails = LethenonSwing.button("btnDetails",
			AnswerFields.detailsButtonText(false, locale), event -> onDetails(),
			text("lethenon.node.tooltip.details", "show why connections to your node were refused"));
		nameTheComponents();
		explainTheComponents();
		bindToTheModel();
		layOutTheInputs();
		layOutTheAnswer();
		add(lblResult, ToolForm.RESULT_LINE);
		pnlDetails.setVisible(false);
		txtChainFile.setText(LethenonSettingsContribution.chainFile());
		txtWalletFile.setText(LethenonSettingsContribution.walletFile());
		txtPort.setText(modelObject.getPort());
		txtPun.setText(modelObject.getPun());
		updateTheControls();
	}

	/**
	 * Gets the panel's state, which is what a test reads instead of the widgets
	 *
	 * @return the model object every component of this panel is bound to
	 */
	public LethenonNodePanelModel getModelObject()
	{
		return modelObject;
	}

	@Override
	public void addNotify()
	{
		super.addNotify();
		if (stopsWithItsWindow)
		{
			return;
		}
		JInternalFrame window = (JInternalFrame)SwingUtilities
			.getAncestorOfClass(JInternalFrame.class, this);
		if (window != null)
		{
			window.addInternalFrameListener(new InternalFrameAdapter()
			{
				@Override
				public void internalFrameClosed(final InternalFrameEvent event)
				{
					closed = true;
					stopInTheBackground();
				}
			});
			stopsWithItsWindow = true;
		}
	}

	/**
	 * Starts the node on a worker thread. The password leaves the field and the model here; the
	 * support class wipes the array once the wallet is open, or at once when the node does not mine.
	 */
	void onStart()
	{
		char[] password = modelObject.getPassword();
		dropThePassword();
		NodeOrder order;
		try
		{
			order = orderFromTheModel();
		}
		catch (InvalidPathException notAPath)
		{
			NodeSupport.wipe(password);
			showNothing(notStarted() + ": " + notAPath.getMessage());
			return;
		}
		if (!convertedIfAgreed(order.chainFile()))
		{
			NodeSupport.wipe(password);
			return;
		}
		setStarting(true);
		showNothing(text("lethenon.node.result.starting",
			"starting your node; every node listed is called"));
		new SwingWorker<RunningNode, Void>()
		{
			@Override
			protected RunningNode doInBackground() throws IOException
			{
				try
				{
					return NodeSupport.startWith(order,
						walletRow.source(password, order.walletFile()));
				}
				finally
				{
					NodeSupport.wipe(password);
				}
			}

			@Override
			protected void done()
			{
				showTheStart(this, order);
			}
		}.execute();
	}

	/**
	 * Converts a chain file of lethenon 0.5.0 or earlier once the person agreed, before the node is
	 * started on a worker thread: the question belongs on this one (#581)
	 *
	 * @return whether the node may start; otherwise the result line says why not
	 */
	private boolean convertedIfAgreed(final Path chainFile)
	{
		try
		{
			if (ChainConversion.convertedIfAgreed(this, consent, chainFile))
			{
				return true;
			}
			showNothing(notStarted() + ": " + ChainConversion.notConverted());
		}
		catch (ChainRejected | IllegalArgumentException | IOException refused)
		{
			showNothing(notStarted() + ": " + refused.getMessage());
		}
		return false;
	}

	/**
	 * Stops the node on a worker thread and shows where it stopped
	 */
	void onStop()
	{
		RunningNode stopping = node;
		if (stopping == null)
		{
			return;
		}
		node = null;
		refresh.stop();
		btnStop.setEnabled(false);
		setResult(text("lethenon.node.result.stopping",
			"stopping your node once the search for a block in progress ends"));
		new SwingWorker<NodeStatus, Void>()
		{
			@Override
			protected NodeStatus doInBackground()
			{
				stopping.close();
				return stopping.status();
			}

			@Override
			protected void done()
			{
				showTheStop(this);
			}
		}.execute();
	}

	private void showTheStart(final SwingWorker<RunningNode, Void> worker, final NodeOrder order)
	{
		try
		{
			RunningNode started = worker.get();
			if (closed)
			{
				started.close();
				return;
			}
			LethenonSettingsContribution.remember(order.mine()
				? Map.of(LethenonSettingsContribution.KEY_CHAIN_FILE, order.chainFile().toString(),
					LethenonSettingsContribution.KEY_WALLET_FILE, order.walletFile().toString())
				: Map.of(LethenonSettingsContribution.KEY_CHAIN_FILE, order.chainFile().toString()));
			node = started;
			modelObject.setListeningPort(started.port());
			setRunning(true);
			setResult(text("lethenon.node.result.running", "the node runs and serves the chain file"));
			showTheStatus();
			refresh.start();
		}
		catch (ExecutionException failed)
		{
			showNothing(notStarted() + ": " + reasonOf(failed.getCause()));
		}
		catch (InterruptedException interrupted)
		{
			Thread.currentThread().interrupt();
			showNothing(notStarted() + ": " + text("lethenon.node.result.interrupted",
				"the start was interrupted; start it again"));
		}
		finally
		{
			setStarting(false);
		}
	}

	private void showTheStop(final SwingWorker<NodeStatus, Void> worker)
	{
		try
		{
			showTheAnswer(worker.get(), false);
			setResult(text("lethenon.node.result.stopped", "the node was stopped"));
		}
		catch (ExecutionException failed)
		{
			setResult(stopFailed() + ": " + reasonOf(failed.getCause()));
		}
		catch (InterruptedException interrupted)
		{
			Thread.currentThread().interrupt();
			setResult(stopFailed());
		}
		finally
		{
			modelObject.setListeningPort(0);
			setRunning(false);
		}
	}

	private void showTheStatus()
	{
		RunningNode running = node;
		if (running != null)
		{
			showTheAnswer(running.status(), true);
		}
	}

	/**
	 * Stops the node of a window that was closed; nothing is shown any more, so nothing waits for it
	 */
	private void stopInTheBackground()
	{
		refresh.stop();
		RunningNode stopping = node;
		node = null;
		if (stopping != null)
		{
			Thread.ofPlatform().name("lethenon-node-stop").start(stopping::close);
		}
	}

	private NodeOrder orderFromTheModel()
	{
		return new NodeOrder(Path.of(modelObject.getChainFile().trim()), modelObject.getPort(),
			modelObject.getPeers(), modelObject.isMine(),
			Path.of(modelObject.getWalletFile().trim()), modelObject.getPun());
	}

	/** Opens or closes "Show details" */
	void onDetails()
	{
		modelObject.setDetailsShown(!modelObject.isDetailsShown());
		pnlDetails.setVisible(modelObject.isDetailsShown());
		btnDetails.setText(AnswerFields.detailsButtonText(modelObject.isDetailsShown(), locale));
		revalidate();
		repaint();
	}

	/**
	 * The sentence that answers what the window was opened for (#604, rule 1): whether the node
	 * runs, where its chain ends, and for a node that mines how many blocks it found
	 *
	 * @param status
	 *            what the node holds
	 * @param mining
	 *            whether it mines
	 * @param running
	 *            whether it runs, or has stopped
	 * @param locale
	 *            the language of the window
	 * @return the sentence
	 */
	static String answerFor(final NodeStatus status, final boolean mining, final boolean running,
		final Locale locale)
	{
		String where = running
			? LethenonMessages.format("lethenon.node.answer.running",
				"Your node runs on port {0,number,#}: its chain ends at block {1,number,integer}.",
				locale, status.port(), status.height())
			: LethenonMessages.format("lethenon.node.answer.stopped",
				"Your node has stopped: its chain ends at block {0,number,integer}.", locale,
				status.height());
		if (!mining)
		{
			return where;
		}
		String found = running
			? LethenonMessages.format("lethenon.node.answer.found.so.far",
				"It has found {0,choice,0#no block|1#1 block|1<{0,number,integer} blocks} for you "
					+ "so far.",
				locale, status.minedBlocks())
			: LethenonMessages.format("lethenon.node.answer.found",
				"It found {0,choice,0#no block|1#1 block|1<{0,number,integer} blocks} for you.",
				locale, status.minedBlocks());
		return where + " " + found;
	}

	private void showTheAnswer(final NodeStatus status, final boolean running)
	{
		boolean mining = modelObject.isMine();
		modelObject.clearAnswer();
		modelObject.setAnswer(answerFor(status, mining, running, locale));
		modelObject.setListening(LethenonMessages.format("lethenon.node.listening",
			"{0,number,#}", locale, status.port()));
		modelObject.setLastBlock(LethenonMessages.format("lethenon.node.last.block",
			"{0,number,integer}", locale, status.height()));
		modelObject.setConnected(LethenonMessages.format("lethenon.node.connected",
			"{0,choice,0#none|1#1 node|1<{0,number,integer} nodes}", locale, status.peers()));
		modelObject.setWaiting(LethenonMessages.format("lethenon.node.waiting",
			"{0,choice,0#none|1#1 transfer|1<{0,number,integer} transfers}", locale,
			status.waiting()));
		modelObject.setMined(mining
			? LethenonMessages.format("lethenon.node.mined",
				"{0,choice,0#none yet|1#1 block|1<{0,number,integer} blocks}", locale,
				status.minedBlocks())
			: text("lethenon.node.not.mining", "not mining"));
		modelObject.setRefusals(status.refusals().isEmpty()
			? text("lethenon.node.refusals.none", "none")
			: String.join("\n", status.refusals()));
		showTheModel();
	}

	private void showNothing(final String result)
	{
		modelObject.clearAnswer();
		showTheModel();
		setResult(result);
	}

	/** Puts what the model holds into the read-only fields */
	private void showTheModel()
	{
		txtAnswer.setText(modelObject.getAnswer());
		AnswerFields.setText(txtListening, modelObject.getListening());
		AnswerFields.setText(txtLastBlock, modelObject.getLastBlock());
		AnswerFields.setText(txtConnected, modelObject.getConnected());
		AnswerFields.setText(txtWaiting, modelObject.getWaiting());
		AnswerFields.setText(txtMined, modelObject.getMined());
		txtRefusals.setText(modelObject.getRefusals());
	}

	/**
	 * What the user reads about a failed start: the library's own sentence for what it refuses, and
	 * for anything else the exception itself, which is also logged, because it is a defect
	 */
	private static String reasonOf(final Throwable cause)
	{
		if (cause instanceof IOException || cause instanceof IllegalArgumentException
			|| cause instanceof ChainRejected)
		{
			return cause.getMessage();
		}
		LOGGER.log(Level.WARNING, "a node failed unexpectedly", cause);
		return String.valueOf(cause);
	}

	private String notStarted()
	{
		return text("lethenon.node.result.failed", "the node was not started");
	}

	private String stopFailed()
	{
		return text("lethenon.node.result.stop.failed",
			"the node did not stop cleanly; close the window to make sure it is stopped");
	}

	private String text(final String key, final String defaultValue)
	{
		return LethenonMessages.getString(key, defaultValue, locale);
	}

	private void dropThePassword()
	{
		txtPassword.setText("");
		modelObject.setPassword(null);
	}

	private void nameTheComponents()
	{
		txtChainFile.setName("txtChainFile");
		txtPort.setName("txtPort");
		txtPeers.setName("txtPeers");
		chkMine.setName("chkMine");
		txtWalletFile.setName("txtWalletFile");
		txtPassword.setName("txtPassword");
		txtPun.setName("txtPun");
		pnlDetails.setName("pnlDetails");
		txtRefusals.setName("txtRefusals");
		lblResult.setName("lblResult");
	}

	private void explainTheComponents()
	{
		txtChainFile.setToolTipText(text("lethenon.node.tooltip.chain.file",
			"your chain file on the test network; your node shares it and adds every new block "
				+ "to it"));
		txtPort.setToolTipText(text("lethenon.node.tooltip.port",
			"the number other nodes reach yours on; 0 lets the computer pick a free one"));
		txtPeers.setToolTipText(text("lethenon.node.tooltip.peers",
			"other nodes to connect to, as host:port, separated by spaces or commas; may be empty"));
		chkMine.setToolTipText(text("lethenon.node.tooltip.mine",
			"also look for new blocks; every block your node finds pays the Ed25519 account of "
				+ "your wallet"));
		txtWalletFile.setToolTipText(text("lethenon.node.tooltip.wallet.file",
			"your wallet file; only needed when your node finds blocks"));
		txtPassword.setToolTipText(text("lethenon.input.tooltip.password",
			"the password of your wallet file; it is used once and then forgotten"));
		txtPun.setToolTipText(text("lethenon.node.tooltip.pun",
			"any words you like; they go into every block your node finds"));
		txtRefusals.setToolTipText(text("lethenon.node.tooltip.refusals",
			"connections your node turned away, and why, oldest first"));
		txtRefusals.setEditable(false);
		txtRefusals.setLineWrap(true);
		txtRefusals.setWrapStyleWord(true);
		txtRefusals.setOpaque(false);
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
		chkMine.addActionListener(event -> updateTheControls());
	}

	private void bindToTheModel()
	{
		txtChainFile
			.setPropertyModel(LambdaModel.of(modelObject::getChainFile, modelObject::setChainFile));
		txtPort.setPropertyModel(LambdaModel.of(modelObject::getPort, modelObject::setPort));
		txtPeers.setPropertyModel(LambdaModel.of(modelObject::getPeers, modelObject::setPeers));
		chkMine.setPropertyModel(LambdaModel.of(modelObject::isMine, modelObject::setMine));
		txtWalletFile.setPropertyModel(
			LambdaModel.of(modelObject::getWalletFile, modelObject::setWalletFile));
		txtPassword
			.setPropertyModel(LambdaModel.of(modelObject::getPassword, modelObject::setPassword));
		txtPun.setPropertyModel(LambdaModel.of(modelObject::getPun, modelObject::setPun));
		AnswerFields.show(txtAnswer, modelObject::getAnswer);
		AnswerFields.show(txtListening, modelObject::getListening);
		AnswerFields.show(txtLastBlock, modelObject::getLastBlock);
		AnswerFields.show(txtConnected, modelObject::getConnected);
		AnswerFields.show(txtWaiting, modelObject::getWaiting);
		AnswerFields.show(txtMined, modelObject::getMined);
		AnswerFields.show(txtRefusals, modelObject::getRefusals);
	}

	private void layOutTheInputs()
	{
		add(new JLabel(text("lethenon.input.label.chain.file", "Chain file:")));
		add(txtChainFile, WITH_BUTTON);
		add(LethenonSwing.button("btnBrowseChainFile", "...",
			event -> LethenonSwing.chooseFile(this, modelObject.getChainFile())
				.ifPresent(txtChainFile::setText),
			text("lethenon.input.tooltip.browse.chain.file", "pick the chain file on your disk")),
			OWN_WIDTH);
		add(new JLabel(text("lethenon.node.label.port", "Port:")));
		add(txtPort, OWN_WIDTH);
		add(new JLabel(text("lethenon.node.label.peers", "Other nodes (optional):")));
		add(txtPeers, ToolForm.FIELD);
		add(new JLabel(text("lethenon.node.label.mining", "Blocks:")));
		add(chkMine, OWN_WIDTH);
		walletRow.addTo(this);
		add(new JLabel(text("lethenon.input.label.password", "Password:")));
		add(txtPassword, ToolForm.FIELD);
		add(new JLabel(text("lethenon.node.label.pun", "Your pun:")));
		add(txtPun, ToolForm.FIELD);
		add(ToolForm.buttons(btnStart, btnStop), ToolForm.BUTTON_ROW);
	}

	private void layOutTheAnswer()
	{
		add(txtAnswer, ToolForm.WIDE + ", wmin 0");
		add(new JLabel(text("lethenon.node.label.listening", "Reachable on port:")));
		add(txtListening, ToolForm.FIELD);
		add(new JLabel(text("lethenon.node.label.last.block", "Last block:")));
		add(txtLastBlock, ToolForm.FIELD);
		add(new JLabel(text("lethenon.node.label.connected", "Other nodes connected:")));
		add(txtConnected, ToolForm.FIELD);
		add(new JLabel(text("lethenon.node.label.waiting", "Transfers waiting:")));
		add(txtWaiting, ToolForm.FIELD);
		add(new JLabel(text("lethenon.node.label.mined", "Blocks found for you:")));
		add(txtMined, ToolForm.FIELD);
		add(ToolForm.buttons(btnDetails), ToolForm.BUTTON_ROW);
		pnlDetails.add(new JLabel(text("lethenon.node.label.refusals", "Refused connections:")),
			"aligny top");
		pnlDetails.add(txtRefusals, ToolForm.FIELD + ", wmin 0");
		add(pnlDetails, ToolForm.WIDE + ", hidemode 3");
	}

	/**
	 * Start while nothing runs, stop while the node runs, and the wallet fields only for mining
	 */
	private void updateTheControls()
	{
		boolean idle = !modelObject.isStarting() && !modelObject.isRunning();
		btnStart.setEnabled(idle);
		btnStop.setEnabled(modelObject.isRunning());
		boolean mining = idle && modelObject.isMine();
		walletRow.setEnabled(mining);
		txtPassword.setEnabled(mining);
		txtPun.setEnabled(mining);
		txtChainFile.setEnabled(idle);
		txtPort.setEnabled(idle);
		txtPeers.setEnabled(idle);
		chkMine.setEnabled(idle);
	}

	private void setStarting(final boolean starting)
	{
		modelObject.setStarting(starting);
		updateTheControls();
	}

	private void setRunning(final boolean running)
	{
		modelObject.setRunning(running);
		updateTheControls();
	}

	private void setResult(final String text)
	{
		modelObject.setResultText(text);
		lblResult.setText(text);
	}

	@Override
	public void removeNotify()
	{
		walletRow.stop();
		super.removeNotify();
	}
}
