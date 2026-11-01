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
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import io.github.astrapi69.lethenon.ChainRejected;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.component.JMTextArea;
import io.github.astrapi69.swing.model.component.JMTextField;

/**
 * Replays a chain file and shows what was verified.
 * <p>
 * The whole panel is UI over two library calls (architecture.md: no chain logic here);
 * {@link ChainReplaySupport} does the reading and the replaying and knows no Swing type. A refused
 * chain is shown with the reason the chain library gave, which is the only thing that makes a
 * report about it actionable - and the counts are shown only for a chain that was accepted.
 * <p>
 * It answers in fields a child can read (#604): first one sentence in a larger font, "Your chain is
 * in order: every block, transfer and signature in it checked out.", then the blocks, transfers and
 * signatures checked and all the money there is. The chain library's own sentence about the replay
 * is behind "Show details", closed at first.
 */
public class LethenonChainPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** A field that shares its cell with the button that fills it from a file chooser */
	private static final String WITH_BUTTON = "growx, split 2";

	/** Something that keeps its own width instead of growing with the cell */
	private static final String OWN_WIDTH = "alignx left, width pref!";

	/** What a count reads as: "none", "1 block", "3 blocks" */
	private static final String COUNT = "{0,choice,0#none|1#1 {1}|1<{0,number,integer} {2}}";

	private final LethenonChainPanelModel modelObject = new LethenonChainPanelModel();

	/** Whom to ask before a chain file of lethenon 0.5.0 or earlier is converted (#581) */
	private final ChainConversionConsent consent;

	private final Locale locale = Locale.getDefault();

	private final JMTextField txtChainFile = new JMTextField(34);

	private final JMTextArea txtAnswer = AnswerFields.answerArea(text("lethenon.verify.tooltip.answer",
		"whether the chain is in order, in one sentence; the details are below"));

	private final JMTextField txtBlocks = AnswerFields.readOnly("txtBlocks",
		text("lethenon.verify.tooltip.blocks",
			"how many blocks the chain has, the first included; each was checked, and so was the "
				+ "way each one follows the one before"));

	private final JMTextField txtTransfers = AnswerFields.readOnly("txtTransfers",
		text("lethenon.verify.tooltip.transfers",
			"how many transfers the blocks carry; each was applied, and none spends more than its "
				+ "account holds"));

	private final JMTextField txtSignatures = AnswerFields.readOnly("txtSignatures",
		text("lethenon.verify.tooltip.signatures",
			"how many signatures were checked; a transfer counts only when its owner signed it"));

	private final JMTextField txtSupply = AnswerFields.readOnly("txtSupply",
		AnswerFields.supplyTooltip(locale));

	private final JButton btnDetails;

	private final JPanel pnlDetails = new JPanel(ToolForm.newLayout());

	private final JMTextArea txtSummary = new JMTextArea(3, 40);

	private final JLabel lblResult = new JLabel(" ");

	/** Where a failure shows, in the pattern of draft 07 (#710, #712) */
	private final transient FailureView failureView;

	/**
	 * Instantiates a new {@link LethenonChainPanel}, prefilled with the chain file from the
	 * plugin's settings
	 */
	public LethenonChainPanel()
	{
		this(ChainConversionConsent.ASK);
	}

	/**
	 * Instantiates a new {@link LethenonChainPanel} that asks through the given consent before it
	 * converts a chain file of lethenon 0.5.0 or earlier (#581)
	 *
	 * @param consent
	 *            whom to ask
	 */
	LethenonChainPanel(final ChainConversionConsent consent)
	{
		super(ToolForm.newLayout());
		this.consent = consent;
		btnDetails = LethenonSwing.button("btnDetails",
			AnswerFields.detailsButtonText(false, locale), event -> onDetails(),
			text("lethenon.verify.tooltip.details",
				"show the chain library's own sentence about what it checked"));
		nameAndExplainTheComponents();
		bindToTheModel();
		layOutTheInputs();
		int answerStarts = getComponentCount();
		layOutTheAnswer();
		failureView = FailureView.below(this,
			List.of(getComponents()).subList(answerStarts, getComponentCount()), locale);
		add(lblResult, ToolForm.RESULT_LINE);
		pnlDetails.setVisible(false);
		txtChainFile.setText(LethenonSettingsContribution.chainFile());
	}

	/**
	 * Gets the panel's state, which is what a test reads instead of the widgets
	 *
	 * @return the model object every component of this panel is bound to
	 */
	public LethenonChainPanelModel getModelObject()
	{
		return modelObject;
	}

	/**
	 * Replays the named file and shows what came back. A refused chain is not an error of this
	 * panel: the reason goes into the result line, the answer says the chain is not in order, and
	 * no field shows a count it cannot back up.
	 */
	void onVerify()
	{
		String refused = text("lethenon.verify.result.refused", "the chain was refused");
		try
		{
			Path chainFile = Path.of(modelObject.getChainFile().trim());
			if (!ChainConversion.convertedIfAgreed(this, consent, chainFile))
			{
				showNothing(ChainConversion.notConverted(), "");
				return;
			}
			ChainReplayReport report = ChainReplaySupport.verify(chainFile);
			LethenonSettingsContribution.remember(
				Map.of(LethenonSettingsContribution.KEY_CHAIN_FILE, chainFile.toString()));
			showTheAnswer(report);
			setResult(text("lethenon.verify.result.accepted", "the chain was accepted"));
		}
		catch (ChainRejected | IllegalArgumentException rejected)
		{
			showFailure(refused + ": " + rejected.getMessage(), rejected);
		}
		catch (IOException unreadable)
		{
			showFailure(refused + ": " + LethenonMessages.format("lethenon.verify.unreadable",
				"{0} cannot be read - {1}", locale, modelObject.getChainFile(),
				unreadable.getMessage()), unreadable);
		}
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

	private void showTheAnswer(final ChainReplayReport report)
	{
		modelObject.clearAnswer();
		modelObject.setAnswer(text("lethenon.verify.answer",
			"Your chain is in order: every block, transfer and signature in it checked out."));
		modelObject.setBlocks(count(report.blocks(), "lethenon.verify.blocks", "block", "blocks"));
		modelObject.setTransfers(
			count(report.transactions(), "lethenon.verify.transfers", "transfer", "transfers"));
		modelObject.setSignatures(
			count(report.signatures(), "lethenon.verify.signatures", "signature", "signatures"));
		modelObject.setSupply(LethAmounts.format(report.supply(), locale));
		modelObject.setSummary(report.summary());
		showTheModel();
	}

	private String count(final long count, final String key, final String one, final String many)
	{
		return LethenonMessages.format(key, COUNT.replace("{1}", one).replace("{2}", many), locale,
			count);
	}

	private String notInOrder()
	{
		return text("lethenon.verify.answer.refused",
			"This chain is not in order, so nothing in it can be trusted. The line below says why.");
	}

	/**
	 * Shows a failure in the pattern of draft 07 (#712): the cause and what to do, the reason under
	 * the details, the answer hidden; the model still says the chain is not in order, and keeps the
	 * whole line for whoever reads the state
	 */
	private void showFailure(final String result, final Throwable thrown)
	{
		modelObject.clearAnswer();
		modelObject.setAnswer(notInOrder());
		showTheModel();
		modelObject.setResultText(result);
		lblResult.setText(" ");
		failureView.show(Failures.of(thrown,
			new Refusal("lethenon.verify.failure", "The chain was not checked."), locale));
	}

	private void showNothing(final String result, final String answer)
	{
		modelObject.clearAnswer();
		modelObject.setAnswer(answer);
		showTheModel();
		setResult(result);
	}

	/** Puts what the model holds into the read-only fields */
	private void showTheModel()
	{
		txtAnswer.setText(modelObject.getAnswer());
		AnswerFields.setText(txtBlocks, modelObject.getBlocks());
		AnswerFields.setText(txtTransfers, modelObject.getTransfers());
		AnswerFields.setText(txtSignatures, modelObject.getSignatures());
		AnswerFields.setText(txtSupply, modelObject.getSupply());
		txtSummary.setText(modelObject.getSummary());
	}

	private void nameAndExplainTheComponents()
	{
		txtChainFile.setName("txtChainFile");
		pnlDetails.setName("pnlDetails");
		txtSummary.setName("txtSummary");
		txtSummary.setEditable(false);
		txtSummary.setLineWrap(true);
		txtSummary.setWrapStyleWord(true);
		txtSummary.setOpaque(false);
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
		txtChainFile.setToolTipText(text("lethenon.verify.tooltip.chain.file",
			"the file that holds the chain; it is checked here from its first block to its last, "
				+ "without asking anybody"));
		txtSummary.setToolTipText(text("lethenon.verify.tooltip.summary",
			"what the chain library says it checked, in its own words"));
	}

	private void layOutTheInputs()
	{
		add(new JLabel(text("lethenon.input.label.chain.file", "Chain file:")));
		add(txtChainFile, WITH_BUTTON);
		add(LethenonSwing.button("btnBrowseChainFile", "...", event -> onBrowseChainFile(),
			text("lethenon.input.tooltip.browse.chain.file", "pick the chain file on your disk")),
			OWN_WIDTH);
		add(ToolForm.buttons(LethenonSwing.button("btnVerify",
			text("lethenon.verify.button.verify", "Check my chain"), event -> onVerify(),
			text("lethenon.verify.tooltip.verify",
				"check every block, transfer and signature of the chain, from the first block on"))),
			ToolForm.BUTTON_ROW);
	}

	private void layOutTheAnswer()
	{
		add(txtAnswer, ToolForm.WIDE + ", wmin 0");
		add(new JLabel(text("lethenon.verify.label.blocks", "Blocks checked:")));
		add(txtBlocks, ToolForm.FIELD);
		add(new JLabel(text("lethenon.verify.label.transfers", "Transfers checked:")));
		add(txtTransfers, ToolForm.FIELD);
		add(new JLabel(text("lethenon.verify.label.signatures", "Signatures checked:")));
		add(txtSignatures, ToolForm.FIELD);
		add(AnswerFields.supplyLabel(locale));
		add(txtSupply, ToolForm.FIELD);
		add(ToolForm.buttons(btnDetails), ToolForm.BUTTON_ROW);
		pnlDetails.add(new JLabel(text("lethenon.verify.label.summary", "In the chain's words:")),
			"aligny top");
		pnlDetails.add(txtSummary, ToolForm.FIELD + ", wmin 0");
		add(pnlDetails, ToolForm.WIDE + ", hidemode 3");
	}

	private void bindToTheModel()
	{
		txtChainFile
			.setPropertyModel(LambdaModel.of(modelObject::getChainFile, modelObject::setChainFile));
		AnswerFields.show(txtAnswer, modelObject::getAnswer);
		AnswerFields.show(txtBlocks, modelObject::getBlocks);
		AnswerFields.show(txtTransfers, modelObject::getTransfers);
		AnswerFields.show(txtSignatures, modelObject::getSignatures);
		AnswerFields.show(txtSupply, modelObject::getSupply);
		AnswerFields.show(txtSummary, modelObject::getSummary);
	}

	private String text(final String key, final String defaultValue)
	{
		return LethenonMessages.getString(key, defaultValue, locale);
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
