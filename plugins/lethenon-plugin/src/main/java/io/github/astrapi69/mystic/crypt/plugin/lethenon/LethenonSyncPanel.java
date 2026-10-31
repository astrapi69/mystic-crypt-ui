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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import io.github.astrapi69.lethenon.ChainRejected;
import io.github.astrapi69.lethenon.transport.Sync;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.component.JMTextArea;
import io.github.astrapi69.swing.model.component.JMTextField;

/**
 * Brings a chain file up to a node's tip, every block verified here (#530 step 4, lethenon#107).
 * <p>
 * UI over one library call (architecture.md: no network or chain logic here); {@link SyncSupport}
 * does the work and knows no Swing type. The node is asked for blocks only - no balance, no
 * account, nothing about a wallet - and nothing it sends is taken on its word: the chain library's
 * replay checks every block, and the file is written only when the whole result verifies. A sync
 * can take minutes through Tor, so it runs off the event dispatch thread, with the button off until
 * it has ended.
 * <p>
 * It answers in fields a child can read (#604): first one sentence in a larger font, "Your chain
 * file is up to date now: it took 3 new blocks and ends at block 2.", then the new blocks, the last
 * block and the node they came from. The chain and whether its first block was taken on first use
 * are behind "Show details", closed at first.
 */
public class LethenonSyncPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = Logger.getLogger(LethenonSyncPanel.class.getName());

	/** A field that shares its cell with the button that fills it from a file chooser */
	private static final String WITH_BUTTON = "growx, split 2";

	/** Something that keeps its own width instead of growing with the cell */
	private static final String OWN_WIDTH = "alignx left, width pref!";

	private final LethenonSyncPanelModel modelObject = new LethenonSyncPanelModel();

	/** Whom to ask before a chain file of lethenon 0.5.0 or earlier is converted (#581) */
	private final ChainConversionConsent consent;

	private final JMTextField txtChainFile = new JMTextField(34);

	private final JMTextField txtNode = new JMTextField(34);

	private final JMTextField txtProxy = new JMTextField(34);

	private final Locale locale = Locale.getDefault();

	private final JMTextArea txtAnswer = AnswerFields.answerArea(text("lethenon.sync.tooltip.answer",
		"whether your chain file is up to date, in one sentence; the details are below"));

	private final JMTextField txtTaken = AnswerFields.readOnly("txtTaken",
		text("lethenon.sync.tooltip.taken",
			"how many blocks the node had that your chain file did not; each was checked here "
				+ "before it was written"));

	private final JMTextField txtLastBlock = AnswerFields.readOnly("txtLastBlock",
		text("lethenon.sync.tooltip.last.block",
			"the number of the last block in your chain file now; the first block is block 0"));

	private final JMTextField txtFrom = AnswerFields.readOnly("txtFrom",
		text("lethenon.sync.tooltip.from",
			"the node the blocks came from; it was asked for blocks only, nothing about you"));

	private final JButton btnDetails;

	private final JPanel pnlDetails = new JPanel(ToolForm.newLayout());

	private final JMTextField txtChain = AnswerFields.readOnly("txtChain",
		text("lethenon.sync.tooltip.chain",
			"the name of the chain in your file; a test chain and the main chain never mix"));

	private final JMTextField txtFirstUse = AnswerFields.readOnly("txtFirstUse",
		text("lethenon.sync.tooltip.first.use",
			"an empty file has nothing to check the first block against, so it is taken from the "
				+ "node as it is; every later sync checks against it"));

	private final JLabel lblResult = new JLabel(" ");

	/** Where a failure shows, in the pattern of draft 07 (#710, #712) */
	private final transient FailureView failureView;

	private final JButton btnSync = LethenonSwing.button("btnSync",
		text("lethenon.sync.button.sync", "Bring my chain up to date"), event -> onSync(),
		text("lethenon.sync.tooltip.sync",
			"get the blocks your chain file is missing from the node; every block is checked "
				+ "here, and the node is asked for blocks only"));

	/**
	 * Instantiates a new {@link LethenonSyncPanel}, prefilled with the chain file from the plugin's
	 * settings
	 */
	public LethenonSyncPanel()
	{
		this(ChainConversionConsent.ASK);
	}

	/**
	 * Instantiates a new {@link LethenonSyncPanel} that asks through the given consent before it
	 * converts a chain file of lethenon 0.5.0 or earlier (#581)
	 *
	 * @param consent
	 *            whom to ask
	 */
	LethenonSyncPanel(final ChainConversionConsent consent)
	{
		super(ToolForm.newLayout());
		this.consent = consent;
		btnDetails = LethenonSwing.button("btnDetails",
			AnswerFields.detailsButtonText(false, locale), event -> onDetails(),
			text("lethenon.sync.tooltip.details",
				"show the chain's name and where its first block came from"));
		nameTheComponents();
		explainTheComponents();
		bindToTheModel();
		layOutTheInputs();
		int answerStarts = getComponentCount();
		layOutTheAnswer();
		failureView = FailureView.below(this,
			List.of(getComponents()).subList(answerStarts, getComponentCount()), locale);
		add(lblResult, ToolForm.RESULT_LINE);
		pnlDetails.setVisible(false);
		txtChainFile.setText(LethenonSettingsContribution.chainFile());
		txtNode.setText(LethenonSettingsContribution.node());
		txtProxy.setText(LethenonSettingsContribution.proxy());
	}

	/**
	 * Gets the panel's state, which is what a test reads instead of the widgets
	 *
	 * @return the model object every component of this panel is bound to
	 */
	public LethenonSyncPanelModel getModelObject()
	{
		return modelObject;
	}

	/**
	 * Starts the sync on a worker thread and shows its outcome when it has ended. A refusal is not
	 * an error of this panel: the reason goes into the result line, and the report says nothing it
	 * cannot back up.
	 */
	void onSync()
	{
		Path chainFile;
		try
		{
			chainFile = Path.of(modelObject.getChainFile().trim());
		}
		catch (InvalidPathException notAPath)
		{
			showFailure(failure() + ": " + notAPath.getMessage(), notAPath);
			return;
		}
		if (!convertedIfAgreed(chainFile))
		{
			return;
		}
		String node = modelObject.getNode();
		String proxy = modelObject.getProxy();
		setSynchronising(true);
		showStatus(text("lethenon.sync.result.running",
			"getting the blocks from the node; every block is checked here"));
		new SwingWorker<SyncSupport.SyncedFrom, Void>()
		{
			@Override
			protected SyncSupport.SyncedFrom doInBackground() throws IOException
			{
				return SyncSupport.sync(chainFile, node, proxy);
			}

			@Override
			protected void done()
			{
				showTheOutcome(this, chainFile, node, proxy);
			}
		}.execute();
	}

	/**
	 * Converts a chain file of lethenon 0.5.0 or earlier once the person agreed, before the sync
	 * runs on a worker thread: the question belongs on this one, and the sync's first read would
	 * convert the file without it (#581)
	 *
	 * @return whether the sync may run; otherwise the result line says why not
	 */
	private boolean convertedIfAgreed(final Path chainFile)
	{
		try
		{
			if (ChainConversion.convertedIfAgreed(this, consent, chainFile))
			{
				return true;
			}
			showStatus(failure() + ": " + ChainConversion.notConverted());
		}
		catch (ChainRejected | IllegalArgumentException | IOException refused)
		{
			showFailure(failure() + ": " + refused.getMessage(), refused);
		}
		return false;
	}

	private void showTheOutcome(final SwingWorker<SyncSupport.SyncedFrom, Void> worker,
		final Path chainFile, final String node, final String proxy)
	{
		try
		{
			SyncSupport.SyncedFrom syncedFrom = worker.get();
			rememberWhatWorked(chainFile, node, proxy);
			showTheAnswer(syncedFrom);
			setResult(syncedFrom.synced().taken() > 0
				? text("lethenon.sync.result.written", "the chain file is at the tip of the node now")
				: text("lethenon.sync.result.unchanged",
					"the chain file holds at least what the node has: nothing was written"));
		}
		catch (ExecutionException failed)
		{
			showFailure(failure() + ": " + reasonOf(failed.getCause()), failed.getCause());
		}
		catch (InterruptedException interrupted)
		{
			Thread.currentThread().interrupt();
			String reason = text("lethenon.sync.result.interrupted",
				"the sync was interrupted; start it again");
			showFailure(failure() + ": " + reason, new IllegalStateException(reason));
		}
		finally
		{
			setSynchronising(false);
		}
	}

	/**
	 * Remembers what a sync that worked used (#605): the chain file, the node (empty for the seed
	 * nodes) and the SOCKS proxy it went through; no proxy used is no proxy remembered
	 */
	private static void rememberWhatWorked(final Path chainFile, final String node,
		final String proxy)
	{
		Map<String, String> worked = new java.util.LinkedHashMap<>();
		worked.put(LethenonSettingsContribution.KEY_CHAIN_FILE, chainFile.toString());
		worked.put(LethenonSettingsContribution.KEY_NODE, node == null ? "" : node.trim());
		if (proxy != null && !proxy.isBlank())
		{
			worked.put(LethenonSettingsContribution.KEY_PROXY, proxy.trim());
		}
		LethenonSettingsContribution.remember(worked);
	}

	/**
	 * What the user reads about a failed sync: the library's own sentence for what it refuses, and
	 * for anything else the exception itself, which is also logged, because it is a defect
	 */
	private static String reasonOf(final Throwable cause)
	{
		if (cause instanceof IOException || cause instanceof IllegalArgumentException
			|| cause instanceof ChainRejected)
		{
			return cause.getMessage();
		}
		LOGGER.log(Level.WARNING, "a sync failed unexpectedly", cause);
		return String.valueOf(cause);
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
	 * The sentence that answers what the window was opened for (#604, rule 1): whether the chain
	 * file is up to date now, and where it ends
	 *
	 * @param synced
	 *            what the sync did
	 * @param locale
	 *            the language of the window
	 * @return the sentence
	 */
	static String answerFor(final Sync.Synced synced, final Locale locale)
	{
		long lastBlock = synced.blocksAfter() - 1;
		if (synced.taken() == 0)
		{
			return LethenonMessages.format("lethenon.sync.answer.unchanged",
				"Your chain file was already up to date: it ends at block {0,number,integer}.",
				locale, lastBlock);
		}
		return LethenonMessages.format("lethenon.sync.answer",
			"Your chain file is up to date now: it took {0,choice,1#1 new block|1<{0,number,integer} "
				+ "new blocks} and ends at block {1,number,integer}.",
			locale, synced.taken(), lastBlock);
	}

	private void showTheAnswer(final SyncSupport.SyncedFrom syncedFrom)
	{
		Sync.Synced synced = syncedFrom.synced();
		modelObject.clearAnswer();
		modelObject.setAnswer(answerFor(synced, locale));
		modelObject.setTaken(LethenonMessages.format("lethenon.sync.taken",
			"{0,choice,0#none|1#1 block|1<{0,number,integer} blocks}", locale, synced.taken()));
		modelObject.setLastBlock(LethenonMessages.format("lethenon.sync.last.block",
			"{0,number,integer}", locale, synced.blocksAfter() - 1));
		modelObject.setFrom(syncedFrom.seedNode()
			? LethenonMessages.format("lethenon.sync.from.seed",
				"{0} (a seed node of the test network)", locale, syncedFrom.node().toString())
			: syncedFrom.node().toString());
		modelObject.setChain(synced.chainIdentifier());
		modelObject.setFirstUse(synced.blocksBefore() == 0 && synced.taken() > 0
			? text("lethenon.sync.first.use.yes",
				"yes: the file was empty, so its first block was taken from this node as it is")
			: text("lethenon.sync.first.use.no", "no"));
		showTheModel();
	}

	/**
	 * Shows a failure in the pattern of draft 07 (#712): the cause and what to do, the reason under
	 * the details, the answer hidden; the status line does not repeat it, the model keeps the whole
	 * line for whoever reads the state
	 */
	private void showFailure(final String result, final Throwable thrown)
	{
		modelObject.clearAnswer();
		showTheModel();
		modelObject.setResultText(result);
		lblResult.setText(" ");
		failureView.show(Failures.of(thrown, new Refusal("lethenon.sync.failure", "The chain was not synchronised."), locale));
	}

	/**
	 * Shows what the person decided or what is under way: no failure, so the status line says it
	 */
	private void showStatus(final String result)
	{
		modelObject.clearAnswer();
		showTheModel();
		setResult(result);
	}

	/** Puts what the model holds into the read-only fields */
	private void showTheModel()
	{
		txtAnswer.setText(modelObject.getAnswer());
		AnswerFields.setText(txtTaken, modelObject.getTaken());
		AnswerFields.setText(txtLastBlock, modelObject.getLastBlock());
		AnswerFields.setText(txtFrom, modelObject.getFrom());
		AnswerFields.setText(txtChain, modelObject.getChain());
		AnswerFields.setText(txtFirstUse, modelObject.getFirstUse());
	}

	private String failure()
	{
		return text("lethenon.sync.result.failed", "the chain file was not synchronised");
	}

	private String text(final String key, final String defaultValue)
	{
		return LethenonMessages.getString(key, defaultValue, locale);
	}

	private void nameTheComponents()
	{
		txtChainFile.setName("txtChainFile");
		txtNode.setName("txtNode");
		txtProxy.setName("txtProxy");
		pnlDetails.setName("pnlDetails");
		lblResult.setName("lblResult");
	}

	private void explainTheComponents()
	{
		txtChainFile.setToolTipText(text("lethenon.sync.tooltip.chain.file",
			"your chain file on the test network; one that does not exist yet starts with the "
				+ "node's first block"));
		txtNode.setToolTipText(text("lethenon.sync.tooltip.node",
			"leave it empty to ask the seed nodes of the test network, through Tor at their onion "
				+ "addresses; or type the host:port of a node you trust, for example 127.0.0.1:18480"));
		txtProxy.setToolTipText(text("lethenon.sync.tooltip.proxy",
			"only for Tor: its SOCKS proxy, usually 127.0.0.1:9050; leave it empty to connect "
				+ "directly. An onion address needs it"));
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
	}

	private void bindToTheModel()
	{
		txtChainFile
			.setPropertyModel(LambdaModel.of(modelObject::getChainFile, modelObject::setChainFile));
		txtNode.setPropertyModel(LambdaModel.of(modelObject::getNode, modelObject::setNode));
		txtProxy.setPropertyModel(LambdaModel.of(modelObject::getProxy, modelObject::setProxy));
		AnswerFields.show(txtAnswer, modelObject::getAnswer);
		AnswerFields.show(txtTaken, modelObject::getTaken);
		AnswerFields.show(txtLastBlock, modelObject::getLastBlock);
		AnswerFields.show(txtFrom, modelObject::getFrom);
		AnswerFields.show(txtChain, modelObject::getChain);
		AnswerFields.show(txtFirstUse, modelObject::getFirstUse);
	}

	private void layOutTheInputs()
	{
		add(new JLabel(text("lethenon.input.label.chain.file", "Chain file:")));
		add(txtChainFile, WITH_BUTTON);
		add(LethenonSwing.button("btnBrowseChainFile", "...", event -> onBrowseChainFile(),
			text("lethenon.input.tooltip.browse.chain.file", "pick the chain file on your disk")),
			OWN_WIDTH);
		add(new JLabel(text("lethenon.sync.label.node", "Node (optional):")));
		add(txtNode, ToolForm.FIELD);
		add(new JLabel(text("lethenon.sync.label.proxy", "Tor proxy (optional):")));
		add(txtProxy, ToolForm.FIELD);
		add(ToolForm.buttons(btnSync), ToolForm.BUTTON_ROW);
	}

	private void layOutTheAnswer()
	{
		add(txtAnswer, ToolForm.WIDE + ", wmin 0");
		add(new JLabel(text("lethenon.sync.label.taken", "New blocks:")));
		add(txtTaken, ToolForm.FIELD);
		add(new JLabel(text("lethenon.sync.label.last.block", "Last block now:")));
		add(txtLastBlock, ToolForm.FIELD);
		add(new JLabel(text("lethenon.sync.label.from", "From the node:")));
		add(txtFrom, ToolForm.FIELD);
		add(ToolForm.buttons(btnDetails), ToolForm.BUTTON_ROW);
		pnlDetails.add(new JLabel(text("lethenon.sync.label.chain", "Chain:")));
		pnlDetails.add(txtChain, ToolForm.FIELD);
		pnlDetails.add(new JLabel(text("lethenon.sync.label.first.use", "First block taken as is:")));
		pnlDetails.add(txtFirstUse, ToolForm.FIELD);
		add(pnlDetails, ToolForm.WIDE + ", hidemode 3");
	}

	private void setSynchronising(final boolean running)
	{
		btnSync.setEnabled(!running);
		modelObject.setSynchronising(running);
	}

	private void setResult(final String text)
	{
		failureView.clear();
		modelObject.setResultText(text);
		lblResult.setText(text);
	}

	private void onBrowseChainFile()
	{
		LethenonSwing.chooseFile(this, modelObject.getChainFile()).ifPresent(txtChainFile::setText);
	}
}
