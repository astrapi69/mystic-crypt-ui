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
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import io.github.astrapi69.lethenon.Amount;
import io.github.astrapi69.lethenon.ChainRejected;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.component.JMPasswordField;
import io.github.astrapi69.swing.model.component.JMTextArea;
import io.github.astrapi69.swing.model.component.JMTextField;

/**
 * Sweeps what was paid to a wallet's one-time destinations onto its own Ed25519 account
 * (lethenon#37) - what lethenon's {@code sweep} does on the command line.
 * <p>
 * The transfers are signed by {@link SweepSupport}, one per destination, and wait next to the chain
 * file until the next block carries them; this window writes no block.
 * <p>
 * It says what sweeping costs BEFORE anything is signed, in a line that stays on screen, and again
 * in the report afterwards: the transfers name those destinations and the account together, so
 * whoever reads the chain knows they belong to one holder. A window that let a person sweep without
 * telling them would mislead them by omission.
 * <p>
 * The password goes from the field into the model as a {@code char[]}, is wiped by the support
 * class, and is cleared from the field afterwards, whether the sweep was signed or refused.
 * <p>
 * It answers in fields a child can read (#604): first one sentence in a larger font, "Swept:
 * 4.90000000 LETH from 1 payment onto your account. It counts once the next block is mined.",
 * then what moved, how many payments, the account and where the transfers wait. The warning
 * before signing stays on the screen as one plain sentence, with the whole cost statement as its
 * tooltip; after a sweep the statement is repeated behind "Show details", with how many transfers
 * wait.
 */
public class LethenonSweepPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** A field that shares its cell with the button that fills it from a file chooser */
	private static final String WITH_BUTTON = "growx, split 2";

	/** Something that keeps its own width instead of growing with the cell */
	private static final String OWN_WIDTH = "alignx left, width pref!";

	private final LethenonSweepPanelModel modelObject = new LethenonSweepPanelModel();

	/** Whom to ask before a chain file of lethenon 0.5.0 or earlier is converted (#581) */
	private final ChainConversionConsent consent;

	private final JMTextField txtChainFile = new JMTextField(34);

	private final JMTextField txtWalletFile = new JMTextField(34);

	private final JMPasswordField txtPassword = new JMPasswordField(34);

	private final JMTextField txtFee = new JMTextField(12);

	private final JMTextField txtMemo = new JMTextField(34);

	/**
	 * The language of the window; the field initializers below read it, so it has its value before
	 * the constructor sets the one it was given
	 */
	private Locale locale = Locale.getDefault();

	/** The wallet the window signs with: the vault's, or a wallet file (#706) */
	private final transient WalletRow walletRow;

	private final JMTextArea txtCost = new JMTextArea(2, 62);

	private final JMTextArea txtAnswer = AnswerFields.answerArea(text("lethenon.sweep.tooltip.answer",
		"what your sweep did, in one sentence; the details are below"));

	private final JMTextField txtSwept = AnswerFields.readOnly("txtSwept",
		text("lethenon.sweep.tooltip.swept",
			"what moves onto your account, the fees already taken out; you can spend it once the "
				+ "next block is mined"));

	private final JMTextField txtPayments = AnswerFields.readOnly("txtPayments",
		text("lethenon.sweep.tooltip.payments",
			"how many payments to your address were swept; each becomes one transfer"));

	private final JMTextField txtOntoAccount = AnswerFields.readOnly("txtOntoAccount",
		text("lethenon.sweep.tooltip.onto.account",
			"your Ed25519 account, where the money goes; see it in the window Show a Balance"));

	private final JMTextField txtWhereItWaits = AnswerFields.readOnly("txtWhereItWaits",
		text("lethenon.sweep.tooltip.where.it.waits",
			"a transfer counts once a block carries it; until then it waits here"));

	private final JButton btnDetails;

	private final JPanel pnlDetails = new JPanel(ToolForm.newLayout());

	private final JMTextField txtWaiting = AnswerFields.readOnly("txtWaiting",
		text("lethenon.sweep.tooltip.waiting",
			"how many transfers wait for the next block now, these included"));

	private final JMTextArea txtCostDetail = new JMTextArea(3, 40);

	private final JLabel lblResult = new JLabel(" ");

	/**
	 * Instantiates a new {@link LethenonSweepPanel}, prefilled with the chain file from the
	 * plugin's settings
	 */
	public LethenonSweepPanel()
	{
		this(ChainConversionConsent.ASK);
	}

	/**
	 * Instantiates a new {@link LethenonSweepPanel} that asks through the given consent before it
	 * converts a chain file of lethenon 0.5.0 or earlier (#581)
	 *
	 * @param consent
	 *            whom to ask
	 */
	LethenonSweepPanel(final ChainConversionConsent consent)
	{
		this(consent, LethenonVault.current(), Locale.getDefault());
	}

	/**
	 * Instantiates a new {@link LethenonSweepPanel} over the given vault
	 *
	 * @param consent
	 *            whom to ask before a chain file is converted
	 * @param vault
	 *            the plugin's view of the vault; its wallet is the one signed with
	 * @param locale
	 *            the language of the window
	 */
	LethenonSweepPanel(final ChainConversionConsent consent, final LethenonVault vault,
		final Locale locale)
	{
		super(ToolForm.newLayout());
		this.consent = consent;
		this.locale = locale;
		walletRow = new WalletRow(vault, locale, txtWalletFile,
			() -> LethenonSwing.chooseFile(this, modelObject.getWalletFile())
				.ifPresent(txtWalletFile::setText));
		modelObject.setCostStatement(text("lethenon.sweep.cost",
			"Sweeping signs one transfer per one-time destination, from that destination to this "
				+ "wallet's account, so the chain then shows those destinations and the account "
				+ "together. Receiving is unlinkable; spending is the moment that ends."));
		btnDetails = LethenonSwing.button("btnDetails",
			AnswerFields.detailsButtonText(false, locale), event -> onDetails(),
			text("lethenon.sweep.tooltip.details",
				"show how many transfers wait and what a sweep shows to whoever reads the chain"));
		nameTheComponents();
		explainTheComponents();
		bindToTheModel();
		layOutTheInputs();
		layOutTheAnswer();
		add(lblResult, ToolForm.RESULT_LINE);
		pnlDetails.setVisible(false);
		txtChainFile.setText(LethenonSettingsContribution.chainFile());
		txtWalletFile.setText(LethenonSettingsContribution.walletFile());
	}

	/**
	 * Gets the panel's state, which is what a test reads instead of the widgets
	 *
	 * @return the model object every component of this panel is bound to
	 */
	public LethenonSweepPanelModel getModelObject()
	{
		return modelObject;
	}

	/**
	 * Signs the sweep. The password is used once: the support class wipes the array it gets, and
	 * the field and the model are cleared afterwards, whether anything was signed or not. A failure
	 * leaves no value of an earlier sweep on the screen.
	 */
	void onSweep()
	{
		String failure = text("lethenon.sweep.result.failed", "nothing was swept");
		try
		{
			SweepOrder order = orderFromTheModel();
			if (!ChainConversion.convertedIfAgreed(this, consent, order.chainFile()))
			{
				showFailure(failure + ": " + ChainConversion.notConverted());
				return;
			}
			char[] password = modelObject.getPassword();
			SweptPayments swept;
			try
			{
				swept = SweepSupport.sweep(order, walletRow.source(password, order.walletFile()));
			}
			finally
			{
				Arrays.fill(password, '\0');
			}
			LethenonSettingsContribution.remember(Map.of(LethenonSettingsContribution.KEY_CHAIN_FILE,
				order.chainFile().toString(), LethenonSettingsContribution.KEY_WALLET_FILE,
				order.walletFile().toString()));
			showTheAnswer(swept);
			setResult(swept.transfers() == 0
				? text("lethenon.sweep.result.nothing", "nothing to sweep")
				: text("lethenon.sweep.result.signed",
					"the sweep was signed and waits for the next block"));
		}
		catch (ChainRejected | IllegalArgumentException refused)
		{
			showFailure(failure + ": " + refused.getMessage());
		}
		catch (IOException unwritable)
		{
			showFailure(failure + ": " + unwritable.getMessage());
		}
		finally
		{
			modelObject.setPassword(null);
			txtPassword.setText("");
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

	/**
	 * The sentence that answers what the window was opened for (#604, rule 1): what was swept onto
	 * the account and when it counts, or that there was nothing to sweep and why
	 *
	 * @param swept
	 *            what the sweep signed
	 * @param fee
	 *            the fee of each transfer, as typed
	 * @param locale
	 *            the language of the window
	 * @return the sentence
	 */
	static String answerFor(final SweptPayments swept, final String fee, final Locale locale)
	{
		if (swept.transfers() == 0)
		{
			return LethenonMessages.format("lethenon.sweep.answer.nothing",
				"There was nothing to sweep: no payment to your address holds more than the fee "
					+ "of {0}.",
				locale, LethAmounts.format(feeOf(fee), locale));
		}
		return LethenonMessages.format("lethenon.sweep.answer",
			"Swept: {0} from {1,choice,1#1 payment|1<{1,number,integer} payments} onto your "
				+ "account. It counts once the next block is mined.",
			locale, LethAmounts.format(swept.total(), locale), swept.transfers());
	}

	private static Amount feeOf(final String fee)
	{
		return fee == null || fee.isBlank() ? Amount.ZERO : Amount.parseLeth(fee.trim());
	}

	private void showTheAnswer(final SweptPayments swept)
	{
		modelObject.clearAnswer();
		modelObject.setAnswer(answerFor(swept, modelObject.getFee(), locale));
		if (swept.transfers() > 0)
		{
			modelObject.setSwept(LethAmounts.format(swept.total(), locale));
			modelObject.setPayments(LethenonMessages.format("lethenon.sweep.payments",
				"{0,choice,1#1 payment|1<{0,number,integer} payments}", locale, swept.transfers()));
			modelObject.setOntoAccount(swept.account());
			modelObject.setWhereItWaits(text("lethenon.sweep.waits.here",
				"next to the chain file, until the next block is mined"));
			modelObject.setWaiting(LethenonMessages.format("lethenon.sweep.waiting",
				"{0,choice,0#none|1#1 transfer|1<{0,number,integer} transfers}", locale,
				swept.waiting()));
			modelObject.setCostDetail(modelObject.getCostStatement());
		}
		showTheModel();
	}

	private void showFailure(final String result)
	{
		modelObject.clearAnswer();
		showTheModel();
		setResult(result);
	}

	/** Puts what the model holds into the read-only fields */
	private void showTheModel()
	{
		txtAnswer.setText(modelObject.getAnswer());
		AnswerFields.setText(txtSwept, modelObject.getSwept());
		AnswerFields.setText(txtPayments, modelObject.getPayments());
		AnswerFields.setText(txtOntoAccount,
			LongValues.shortened(modelObject.getOntoAccount(), locale));
		AnswerFields.setText(txtWhereItWaits, modelObject.getWhereItWaits());
		AnswerFields.setText(txtWaiting, modelObject.getWaiting());
		txtCostDetail.setText(modelObject.getCostDetail());
	}

	private SweepOrder orderFromTheModel()
	{
		return new SweepOrder(Path.of(modelObject.getChainFile().trim()),
			Path.of(modelObject.getWalletFile().trim()), modelObject.getFee(),
			modelObject.getMemo());
	}

	private void nameTheComponents()
	{
		txtChainFile.setName("txtChainFile");
		txtWalletFile.setName("txtWalletFile");
		txtPassword.setName("txtPassword");
		txtFee.setName("txtFee");
		txtMemo.setName("txtMemo");
		for (JMTextArea statement : java.util.List.of(txtCost, txtCostDetail))
		{
			statement.setEditable(false);
			statement.setLineWrap(true);
			statement.setWrapStyleWord(true);
			statement.setOpaque(false);
		}
		txtCost.setName("txtCost");
		txtCost.setFont(txtCost.getFont().deriveFont(Font.BOLD));
		txtCostDetail.setName("txtCostDetail");
		pnlDetails.setName("pnlDetails");
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
	}

	private void explainTheComponents()
	{
		txtChainFile.setToolTipText(text("lethenon.sweep.tooltip.chain.file",
			"the file that holds the chain; the payments to your address are found in it, and the "
				+ "sweep waits next to it until the next block"));
		txtWalletFile.setToolTipText(text("lethenon.sweep.tooltip.wallet.file",
			"your wallet file; payments to its address are moved onto its Ed25519 account"));
		txtPassword.setToolTipText(text("lethenon.input.tooltip.password",
			"the password of your wallet file; it is used once and then forgotten"));
		txtFee.setToolTipText(text("lethenon.sweep.tooltip.fee",
			"what each payment gives up to whoever mines the block; a payment that holds no more "
				+ "than this is left where it is"));
		txtMemo.setToolTipText(text("lethenon.sweep.tooltip.memo",
			"a note that goes with each transfer; everyone who reads the chain can read it too"));
		txtCost.setToolTipText(modelObject.getCostStatement());
		txtCostDetail.setToolTipText(text("lethenon.sweep.tooltip.cost.detail",
			"what a sweep shows to whoever reads the chain, and why"));
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
		walletRow.addTo(this);
		add(new JLabel(text("lethenon.input.label.password", "Password:")));
		add(txtPassword, ToolForm.FIELD);
		add(new JLabel(text("lethenon.sweep.label.fee", "Fee per payment (LETH):")));
		add(txtFee, OWN_WIDTH);
		add(new JLabel(text("lethenon.sweep.label.memo", "Note:")));
		add(txtMemo, ToolForm.FIELD);
		add(txtCost, ToolForm.WIDE + ", wmin 0");
		add(ToolForm.buttons(LethenonSwing.button("btnSweep",
			text("lethenon.sweep.button.sweep", "Sweep onto my account"), event -> onSweep(),
			text("lethenon.sweep.tooltip.sweep",
				"sign one transfer for each payment to your address, onto your account, and leave "
					+ "them for the next block"))),
			ToolForm.BUTTON_ROW);
	}

	private void layOutTheAnswer()
	{
		add(txtAnswer, ToolForm.WIDE + ", wmin 0");
		add(new JLabel(text("lethenon.sweep.label.swept", "Moved onto your account:")));
		add(txtSwept, ToolForm.FIELD);
		add(new JLabel(text("lethenon.sweep.label.payments", "Payments swept:")));
		add(txtPayments, ToolForm.FIELD);
		add(new JLabel(text("lethenon.sweep.label.onto.account", "Your account:")));
		add(txtOntoAccount, ToolForm.FIELD);
		add(new JLabel(text("lethenon.sweep.label.where.it.waits", "Waits:")));
		add(txtWhereItWaits, ToolForm.FIELD);
		add(ToolForm.buttons(btnDetails), ToolForm.BUTTON_ROW);
		pnlDetails.add(new JLabel(text("lethenon.sweep.label.waiting", "Waiting now:")));
		pnlDetails.add(txtWaiting, ToolForm.FIELD);
		pnlDetails.add(new JLabel(text("lethenon.sweep.label.cost.detail", "What the chain sees:")),
			"aligny top");
		pnlDetails.add(txtCostDetail, ToolForm.FIELD + ", wmin 0");
		add(pnlDetails, ToolForm.WIDE + ", hidemode 3");
	}

	private void bindToTheModel()
	{
		txtChainFile
			.setPropertyModel(LambdaModel.of(modelObject::getChainFile, modelObject::setChainFile));
		txtWalletFile.setPropertyModel(
			LambdaModel.of(modelObject::getWalletFile, modelObject::setWalletFile));
		txtPassword
			.setPropertyModel(LambdaModel.of(modelObject::getPassword, modelObject::setPassword));
		txtFee.setPropertyModel(LambdaModel.of(modelObject::getFee, modelObject::setFee));
		txtFee.setText(modelObject.getFee());
		txtMemo.setPropertyModel(LambdaModel.of(modelObject::getMemo, modelObject::setMemo));
		txtMemo.setText(modelObject.getMemo());
		txtCost.setText(text("lethenon.sweep.warning",
			"Careful: after a sweep, anyone who reads the chain can see that these payments belong "
				+ "to you."));
		AnswerFields.show(txtAnswer, modelObject::getAnswer);
		AnswerFields.show(txtSwept, modelObject::getSwept);
		AnswerFields.show(txtPayments, modelObject::getPayments);
		AnswerFields.show(txtOntoAccount,
			() -> LongValues.shortened(modelObject.getOntoAccount(), locale));
		AnswerFields.show(txtWhereItWaits, modelObject::getWhereItWaits);
		AnswerFields.show(txtWaiting, modelObject::getWaiting);
		AnswerFields.show(txtCostDetail, modelObject::getCostDetail);
	}

	private String text(final String key, final String defaultValue)
	{
		return LethenonMessages.getString(key, defaultValue, locale);
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
