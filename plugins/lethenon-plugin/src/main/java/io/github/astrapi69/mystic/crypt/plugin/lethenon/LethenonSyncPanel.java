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

	private final JMTextField txtChainFile = new JMTextField(34);

	private final JMTextField txtNode = new JMTextField(34);

	private final JMTextField txtProxy = new JMTextField(34);

	private final JMTextArea txtReport = new JMTextArea(5, 62);

	private final JLabel lblResult = new JLabel(" ");

	private final JButton btnSync = LethenonSwing.button("btnSync",
		LethenonMessages.getString("lethenon.button.sync", "Synchronise"), event -> onSync(),
		LethenonMessages.getString("lethenon.tooltip.sync",
			"bring the chain file up to the tip of the node; every block is verified here, and "
				+ "the node is asked for blocks only"));

	/**
	 * Instantiates a new {@link LethenonSyncPanel}, prefilled with the chain file from the plugin's
	 * settings
	 */
	public LethenonSyncPanel()
	{
		super(ToolForm.newLayout());
		nameTheComponents();
		explainTheComponents();
		bindToTheModel();
		layOut();
		txtChainFile.setText(LethenonSettingsContribution.chainFile());
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
			setReport("");
			setResult(failure() + ": " + notAPath.getMessage());
			return;
		}
		String node = modelObject.getNode();
		String proxy = modelObject.getProxy();
		setSynchronising(true);
		setReport("");
		setResult(LethenonMessages.getString("lethenon.result.sync.running",
			"synchronising with the node; every block is verified here"));
		new SwingWorker<Sync.Synced, Void>()
		{
			@Override
			protected Sync.Synced doInBackground() throws IOException
			{
				return SyncSupport.sync(chainFile, node, proxy);
			}

			@Override
			protected void done()
			{
				showTheOutcome(this, node);
			}
		}.execute();
	}

	private void showTheOutcome(final SwingWorker<Sync.Synced, Void> worker, final String node)
	{
		try
		{
			Sync.Synced synced = worker.get();
			setReport(describe(synced, node.trim()));
			setResult(synced.taken() > 0
				? LethenonMessages.getString("lethenon.result.sync.written",
					"the chain file is at the tip of the node now")
				: LethenonMessages.getString("lethenon.result.sync.unchanged",
					"the chain file holds at least what the node has: nothing was written"));
		}
		catch (ExecutionException failed)
		{
			setReport("");
			setResult(failure() + ": " + reasonOf(failed.getCause()));
		}
		catch (InterruptedException interrupted)
		{
			Thread.currentThread().interrupt();
			setReport("");
			setResult(failure() + ": " + LethenonMessages
				.getString("lethenon.result.sync.interrupted", "the sync was interrupted"));
		}
		finally
		{
			setSynchronising(false);
		}
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

	private static String describe(final Sync.Synced synced, final String node)
	{
		String report = LethenonMessages.getString("lethenon.sync.chain", "chain") + " "
			+ synced.chainIdentifier() + " "
			+ LethenonMessages.getString("lethenon.sync.from", "from the node at") + " " + node
			+ ": " + LethenonMessages.getString("lethenon.sync.took", "took") + " "
			+ synced.taken() + " "
			+ LethenonMessages.getString("lethenon.sync.height", "block(s), now at height") + " "
			+ (synced.blocksAfter() - 1);
		if (synced.blocksBefore() == 0)
		{
			report += "\n" + LethenonMessages.getString("lethenon.sync.first.use",
				"the file was empty: its genesis block came from the node, taken on first use");
		}
		return report;
	}

	private static String failure()
	{
		return LethenonMessages.getString("lethenon.result.sync.failed",
			"the chain file was not synchronised");
	}

	private void nameTheComponents()
	{
		txtChainFile.setName("txtChainFile");
		txtNode.setName("txtNode");
		txtProxy.setName("txtProxy");
		txtReport.setName("txtReport");
		lblResult.setName("lblResult");
	}

	private void explainTheComponents()
	{
		txtChainFile.setToolTipText(LethenonMessages.getString("lethenon.tooltip.sync.chain.file",
			"the chain file on the test network; one that does not exist yet starts from the "
				+ "genesis block of the node"));
		txtNode.setToolTipText(LethenonMessages.getString("lethenon.tooltip.sync.node",
			"host:port of a running lethenon node, for example 127.0.0.1:18480"));
		txtProxy.setToolTipText(LethenonMessages.getString("lethenon.tooltip.sync.proxy",
			"only for Tor: its SOCKS proxy, usually 127.0.0.1:9050; empty for a direct "
				+ "connection; an onion address needs it"));
		txtReport.setToolTipText(LethenonMessages.getString("lethenon.tooltip.sync.report",
			"what the sync took from the node"));
		txtReport.setEditable(false);
		txtReport.setLineWrap(true);
		txtReport.setWrapStyleWord(true);
		txtReport.setFont(new Font("monospaced", Font.PLAIN, 12));
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
	}

	private void bindToTheModel()
	{
		txtChainFile
			.setPropertyModel(LambdaModel.of(modelObject::getChainFile, modelObject::setChainFile));
		txtNode.setPropertyModel(LambdaModel.of(modelObject::getNode, modelObject::setNode));
		txtProxy.setPropertyModel(LambdaModel.of(modelObject::getProxy, modelObject::setProxy));
		txtReport.setPropertyModel(LambdaModel.of(modelObject::getReport, modelObject::setReport));
	}

	private void layOut()
	{
		add(new JLabel(LethenonMessages.getString("lethenon.label.chain.file", "Chain file:")));
		add(txtChainFile, WITH_BUTTON);
		add(LethenonSwing.button("btnBrowseChainFile", "...", event -> onBrowseChainFile(),
			LethenonMessages.getString("lethenon.tooltip.browse.chain.file", "pick the chain file")),
			OWN_WIDTH);
		add(new JLabel(LethenonMessages.getString("lethenon.label.node", "Node:")));
		add(txtNode, ToolForm.FIELD);
		add(new JLabel(LethenonMessages.getString("lethenon.label.proxy", "SOCKS proxy:")));
		add(txtProxy, ToolForm.FIELD);
		add(ToolForm.buttons(btnSync), ToolForm.BUTTON_ROW);
		add(new JLabel(LethenonMessages.getString("lethenon.label.report", "Report:")),
			"aligny top");
		add(ToolForm.scrolled(txtReport), ToolForm.GROWING);
		add(lblResult, ToolForm.RESULT_LINE);
	}

	private void setSynchronising(final boolean running)
	{
		btnSync.setEnabled(!running);
		modelObject.setSynchronising(running);
	}

	private void setReport(final String text)
	{
		modelObject.setReport(text);
		txtReport.setText(text);
	}

	private void setResult(final String text)
	{
		modelObject.setResultText(text);
		lblResult.setText(text);
	}

	private void onBrowseChainFile()
	{
		LethenonSwing.chooseFile(this, modelObject.getChainFile()).ifPresent(txtChainFile::setText);
	}
}
