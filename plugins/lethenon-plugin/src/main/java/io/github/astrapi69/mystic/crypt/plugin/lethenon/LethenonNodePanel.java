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

	private final JMTextField txtChainFile = new JMTextField(34);

	private final JMTextField txtPort = new JMTextField(8);

	private final JMTextField txtPeers = new JMTextField(34);

	private final JMCheckBox chkMine = new JMCheckBox(
		LethenonMessages.getString("lethenon.node.mine", "mine, paying this wallet"));

	private final JMTextField txtWalletFile = new JMTextField(34);

	private final JMPasswordField txtPassword = new JMPasswordField(34);

	private final JMTextField txtPun = new JMTextField(34);

	private final JMTextArea txtStatus = new JMTextArea(6, 62);

	private final JLabel lblResult = new JLabel(" ");

	private final JButton btnStart = LethenonSwing.button("btnStartNode",
		LethenonMessages.getString("lethenon.button.node.start", "Start the node"),
		event -> onStart(), LethenonMessages.getString("lethenon.tooltip.node.start",
			"serve the chain file, connect the peers, and mine when that is ticked"));

	private final JButton btnStop = LethenonSwing.button("btnStopNode",
		LethenonMessages.getString("lethenon.button.node.stop", "Stop the node"),
		event -> onStop(), LethenonMessages.getString("lethenon.tooltip.node.stop",
			"stop mining after the round in progress, then the node"));

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
		super(ToolForm.newLayout());
		nameTheComponents();
		explainTheComponents();
		bindToTheModel();
		layOut();
		txtChainFile.setText(LethenonSettingsContribution.chainFile());
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
			setResult(notStarted() + ": " + notAPath.getMessage());
			return;
		}
		setStarting(true);
		setResult(LethenonMessages.getString("lethenon.result.node.starting",
			"starting the node; every peer is dialled"));
		new SwingWorker<RunningNode, Void>()
		{
			@Override
			protected RunningNode doInBackground() throws IOException
			{
				return NodeSupport.start(order, password);
			}

			@Override
			protected void done()
			{
				showTheStart(this);
			}
		}.execute();
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
		setResult(LethenonMessages.getString("lethenon.result.node.stopping",
			"stopping the node after the mining round in progress"));
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

	private void showTheStart(final SwingWorker<RunningNode, Void> worker)
	{
		try
		{
			RunningNode started = worker.get();
			if (closed)
			{
				started.close();
				return;
			}
			node = started;
			modelObject.setListeningPort(started.port());
			setRunning(true);
			setResult(LethenonMessages.getString("lethenon.result.node.running",
				"the node runs and serves the chain file"));
			showTheStatus();
			refresh.start();
		}
		catch (ExecutionException failed)
		{
			setResult(notStarted() + ": " + reasonOf(failed.getCause()));
		}
		catch (InterruptedException interrupted)
		{
			Thread.currentThread().interrupt();
			setResult(notStarted() + ": " + LethenonMessages
				.getString("lethenon.result.node.interrupted", "the start was interrupted"));
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
			setStatus(describeTheStop(worker.get()));
			setResult(LethenonMessages.getString("lethenon.result.node.stopped",
				"the node was stopped"));
		}
		catch (ExecutionException failed)
		{
			setResult(LethenonMessages.getString("lethenon.result.node.stop.failed",
				"the node did not stop cleanly") + ": " + reasonOf(failed.getCause()));
		}
		catch (InterruptedException interrupted)
		{
			Thread.currentThread().interrupt();
			setResult(LethenonMessages.getString("lethenon.result.node.stop.failed",
				"the node did not stop cleanly"));
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
			setStatus(describe(running.status()));
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

	private static String describe(final NodeStatus status)
	{
		return LethenonMessages.getString("lethenon.node.listening", "listening on port") + " "
			+ status.port() + ", "
			+ LethenonMessages.getString("lethenon.node.height", "height") + " "
			+ status.height() + ", " + status.peers() + " "
			+ LethenonMessages.getString("lethenon.node.peers", "peer(s) connected") + ", "
			+ status.waiting() + " "
			+ LethenonMessages.getString("lethenon.node.waiting", "transfer(s) waiting") + ", "
			+ LethenonMessages.getString("lethenon.node.mined", "mined") + " "
			+ status.minedBlocks() + " "
			+ LethenonMessages.getString("lethenon.node.blocks", "block(s)") + refusals(status);
	}

	private static String describeTheStop(final NodeStatus status)
	{
		return LethenonMessages.getString("lethenon.node.stopped.at", "stopped at height") + " "
			+ status.height() + ", " + LethenonMessages.getString("lethenon.node.mined", "mined")
			+ " " + status.minedBlocks() + " "
			+ LethenonMessages.getString("lethenon.node.blocks", "block(s)") + ", "
			+ status.waiting() + " "
			+ LethenonMessages.getString("lethenon.node.waiting", "transfer(s) waiting")
			+ refusals(status);
	}

	private static String refusals(final NodeStatus status)
	{
		if (status.refusals().isEmpty())
		{
			return "";
		}
		return "\n" + LethenonMessages.getString("lethenon.node.refusals", "refused:") + "\n"
			+ String.join("\n", status.refusals());
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

	private static String notStarted()
	{
		return LethenonMessages.getString("lethenon.result.node.failed", "the node was not started");
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
		txtStatus.setName("txtStatus");
		lblResult.setName("lblResult");
	}

	private void explainTheComponents()
	{
		txtChainFile.setToolTipText(LethenonMessages.getString("lethenon.tooltip.node.chain.file",
			"the chain file the node serves, on the test network; it keeps its pool next to it"));
		txtPort.setToolTipText(LethenonMessages.getString("lethenon.tooltip.node.port",
			"the TCP port the node listens on; 0 for any free one"));
		txtPeers.setToolTipText(LethenonMessages.getString("lethenon.tooltip.node.peers",
			"host:port of the nodes to connect to, separated by spaces or commas; may be empty"));
		chkMine.setToolTipText(LethenonMessages.getString("lethenon.tooltip.node.mine",
			"mine on the tip and the pool of the node; every block pays the Ed25519 account of "
				+ "the wallet"));
		txtWalletFile.setToolTipText(LethenonMessages.getString("lethenon.tooltip.wallet.file",
			"the wallet file lethenon's 'wallet create' or 'wallet restore' wrote"));
		txtPassword.setToolTipText(LethenonMessages.getString("lethenon.tooltip.wallet.password",
			"the wallet file's password; it is used once and then cleared"));
		txtPun.setToolTipText(LethenonMessages.getString("lethenon.tooltip.mine.pun",
			"the words mining starts from; a counter is appended until the block meets the "
				+ "difficulty"));
		txtStatus.setToolTipText(LethenonMessages.getString("lethenon.tooltip.node.status",
			"what the node holds, read every second while it runs"));
		txtStatus.setEditable(false);
		txtStatus.setLineWrap(true);
		txtStatus.setWrapStyleWord(true);
		txtStatus.setFont(new Font("monospaced", Font.PLAIN, 12));
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
		txtStatus.setPropertyModel(LambdaModel.of(modelObject::getStatus, modelObject::setStatus));
	}

	private void layOut()
	{
		add(new JLabel(LethenonMessages.getString("lethenon.label.chain.file", "Chain file:")));
		add(txtChainFile, WITH_BUTTON);
		add(LethenonSwing.button("btnBrowseChainFile", "...",
			event -> LethenonSwing.chooseFile(this, modelObject.getChainFile())
				.ifPresent(txtChainFile::setText),
			LethenonMessages.getString("lethenon.tooltip.browse.chain.file", "pick the chain file")),
			OWN_WIDTH);
		add(new JLabel(LethenonMessages.getString("lethenon.label.node.port", "Port:")));
		add(txtPort, OWN_WIDTH);
		add(new JLabel(LethenonMessages.getString("lethenon.label.node.peers", "Peers:")));
		add(txtPeers, ToolForm.FIELD);
		add(new JLabel(LethenonMessages.getString("lethenon.label.node.mining", "Mining:")));
		add(chkMine, OWN_WIDTH);
		add(new JLabel(LethenonMessages.getString("lethenon.label.wallet.file", "Wallet file:")));
		add(txtWalletFile, WITH_BUTTON);
		add(LethenonSwing.button("btnBrowseWalletFile", "...",
			event -> LethenonSwing.chooseFile(this, modelObject.getWalletFile())
				.ifPresent(txtWalletFile::setText),
			LethenonMessages.getString("lethenon.tooltip.browse.wallet.file",
				"pick the wallet file")),
			OWN_WIDTH);
		add(new JLabel(LethenonMessages.getString("lethenon.label.wallet.password", "Password:")));
		add(txtPassword, ToolForm.FIELD);
		add(new JLabel(LethenonMessages.getString("lethenon.label.mine.pun", "Pun:")));
		add(txtPun, ToolForm.FIELD);
		add(ToolForm.buttons(btnStart, btnStop), ToolForm.BUTTON_ROW);
		add(new JLabel(LethenonMessages.getString("lethenon.label.node.status", "Status:")),
			"aligny top");
		add(ToolForm.scrolled(txtStatus), ToolForm.GROWING);
		add(lblResult, ToolForm.RESULT_LINE);
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
		txtWalletFile.setEnabled(mining);
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

	private void setStatus(final String text)
	{
		modelObject.setStatus(text);
		txtStatus.setText(text);
	}

	private void setResult(final String text)
	{
		modelObject.setResultText(text);
		lblResult.setText(text);
	}
}
