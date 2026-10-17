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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import io.github.astrapi69.lethenon.Amount;
import io.github.astrapi69.lethenon.ChainRejected;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.component.JMPasswordField;
import io.github.astrapi69.swing.model.component.JMTextArea;
import io.github.astrapi69.swing.model.component.JMTextField;

/**
 * Shows what a wallet holds in a chain file (lethenon#2, milestone 5), in fields a child can read
 * (#604).
 * <p>
 * The answer comes first, in one sentence in a larger font: "You hold 3.00000000 LETH." Below it,
 * one labelled, read-only field per value: the address to give to people who pay, every account
 * and what it holds, and the payments that wait for a sweep. What a person passes on has a Copy
 * button, and a value too long to read is shortened on the screen while Copy copies all of it.
 * What only a specialist reads, the replay's counts and all the money on the chain, is behind
 * "Show details", closed at first. Amounts read in the language of the window, every decimal kept.
 * <p>
 * The balance is computed by {@link WalletBalanceSupport} from the replayed chain - never asked of
 * anybody - and comes in two parts that this window keeps apart: the wallet's direct accounts,
 * which it can spend from, and the payments to its one-time destinations, which reach those
 * accounts through a sweep. The second part is named as such and never added to the first.
 * <p>
 * The password goes from the field into the model as a {@code char[]}, is handed to the support
 * class, which wipes it, and is cleared from the field afterwards. It appears in no text this
 * window shows and in no log.
 */
public class LethenonBalancePanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** A field that shares its cell with the button beside it */
	private static final String WITH_BUTTON = "growx, split 2";

	/** Something that keeps its own width instead of growing with the cell */
	private static final String OWN_WIDTH = "alignx left, width pref!";

	/** How much larger the answer is than the text around it */
	private static final float ANSWER_SCALE = 1.5f;

	private final LethenonBalancePanelModel modelObject = new LethenonBalancePanelModel();

	/** Whom to ask before a chain file of lethenon 0.5.0 or earlier is converted (#581) */
	private final ChainConversionConsent consent;

	private final Locale locale = Locale.getDefault();

	private final JMTextField txtChainFile = new JMTextField(34);

	private final JMTextField txtWalletFile = new JMTextField(34);

	private final JMPasswordField txtPassword = new JMPasswordField(34);

	private final JMTextArea txtAnswer = new JMTextArea(2, 40);

	private final JMTextField txtAddress = readOnly("txtAddress");

	private final Map<String, JMTextField> txtAccounts = new LinkedHashMap<>();

	private final Map<String, JMTextField> txtHolds = new LinkedHashMap<>();

	private final JMTextField txtWaitingForSweep = readOnly("txtWaitingForSweep");

	private final JButton btnDetails;

	private final JPanel pnlDetails = new JPanel(ToolForm.newLayout());

	private final JMTextField txtChainChecked = readOnly("txtChainChecked");

	private final JMTextField txtSupply = readOnly("txtSupply");

	private final JLabel lblResult = new JLabel(" ");

	/**
	 * Instantiates a new {@link LethenonBalancePanel}, prefilled with the chain file from the
	 * plugin's settings
	 */
	public LethenonBalancePanel()
	{
		this(ChainConversionConsent.ASK);
	}

	/**
	 * Instantiates a new {@link LethenonBalancePanel} that asks through the given consent before it
	 * converts a chain file of lethenon 0.5.0 or earlier (#581)
	 *
	 * @param consent
	 *            whom to ask
	 */
	LethenonBalancePanel(final ChainConversionConsent consent)
	{
		super(ToolForm.newLayout());
		this.consent = consent;
		for (SignatureSuite suite : SignatureSuite.values())
		{
			txtAccounts.put(suite.identifier(), readOnly("txtAccount_" + suite.identifier()));
			txtHolds.put(suite.identifier(), readOnly("txtHolds_" + suite.identifier()));
		}
		btnDetails = LethenonSwing.button("btnDetails", detailsButtonText(), event -> onDetails(),
			text("lethenon.balance.tooltip.details",
				"show what was checked to get this answer: the blocks, transfers and signatures of "
					+ "the chain, and all the money there is"));
		nameAndExplainTheInputs();
		bindToTheModel();
		layOutTheInputs();
		layOutTheAnswer();
		add(lblResult, ToolForm.RESULT_LINE);
		pnlDetails.setVisible(false);
		txtChainFile.setText(LethenonSettingsContribution.chainFile());
	}

	/**
	 * Gets the panel's state, which is what a test reads instead of the widgets
	 *
	 * @return the model object every component of this panel is bound to
	 */
	public LethenonBalancePanelModel getModelObject()
	{
		return modelObject;
	}

	/**
	 * The sentence that answers what the window was opened for (#604, rule 1): what the accounts
	 * hold together, and what waits for a sweep named apart and never added to it
	 *
	 * @param balance
	 *            the balance
	 * @param locale
	 *            the language of the window
	 * @return the sentence
	 */
	static String answerFor(final WalletBalance balance, final Locale locale)
	{
		Amount held = Amount.ZERO;
		for (AccountBalance account : balance.accounts())
		{
			held = held.plus(account.spendable());
		}
		if (balance.oneTimePayments() == 0)
		{
			return LethenonMessages.format("lethenon.balance.answer", "You hold {0}.", locale,
				LethAmounts.format(held, locale));
		}
		return LethenonMessages.format("lethenon.balance.answer.waiting",
			"You hold {0}, and {1} came to your address and waits for a sweep.", locale,
			LethAmounts.format(held, locale), LethAmounts.format(balance.oneTimeAmount(), locale));
	}

	/**
	 * Reads the balance. The password is used once: the support class wipes the array it gets, and
	 * the field and the model are cleared afterwards, whether the wallet opened or not. A failure
	 * leaves no value of an earlier answer on the screen.
	 */
	void onShow()
	{
		String failure = text("lethenon.balance.result.failed", "the balance could not be read");
		try
		{
			Path chainFile = Path.of(modelObject.getChainFile().trim());
			if (!ChainConversion.convertedIfAgreed(this, consent, chainFile))
			{
				showFailure(failure + ": " + ChainConversion.notConverted());
				return;
			}
			WalletBalance balance = WalletBalanceSupport.balance(chainFile,
				Path.of(modelObject.getWalletFile().trim()), modelObject.getPassword());
			showTheAnswer(balance);
			setResult(text("lethenon.balance.result.read",
				"the balance was read from the replayed chain"));
		}
		catch (ChainRejected | IllegalArgumentException refused)
		{
			showFailure(failure + ": " + refused.getMessage());
		}
		catch (IOException unreadable)
		{
			showFailure(failure + ": " + unreadable.getMessage());
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
		btnDetails.setText(detailsButtonText());
		revalidate();
		repaint();
	}

	private void showTheAnswer(final WalletBalance balance)
	{
		modelObject.clearAnswer();
		modelObject.setAnswer(answerFor(balance, locale));
		modelObject.setAddress(balance.address());
		for (AccountBalance account : balance.accounts())
		{
			modelObject.setAccount(account.suite(), account.account());
			modelObject.setHolds(account.suite(), LethAmounts.format(account.spendable(), locale));
		}
		modelObject.setWaitingForSweep(balance.oneTimePayments() == 0
			? text("lethenon.balance.waiting.none", "nothing")
			: LethenonMessages.format("lethenon.balance.waiting",
				"{0,choice,1#1 payment|1<{0,number,integer} payments}, {1}", locale,
				balance.oneTimePayments(), LethAmounts.format(balance.oneTimeAmount(), locale)));
		modelObject.setChainChecked(LethenonMessages.format("lethenon.balance.chain.checked",
			"{0,choice,0#no blocks|1#1 block|1<{0,number,integer} blocks}, "
				+ "{1,choice,0#no transfers|1#1 transfer|1<{1,number,integer} transfers}, "
				+ "{2,choice,0#no signatures|1#1 signature|1<{2,number,integer} signatures}",
			locale, balance.replay().blocks(), balance.replay().transactions(),
			balance.replay().signatures()));
		modelObject.setSupply(LethAmounts.format(balance.replay().supply(), locale));
		showTheModel();
	}

	private void showFailure(final String result)
	{
		modelObject.clearAnswer();
		showTheModel();
		setResult(result);
	}

	/** Puts what the model holds into the read-only fields, shortening what is too long to read */
	private void showTheModel()
	{
		txtAnswer.setText(modelObject.getAnswer());
		txtAddress.setText(LongValues.shortened(modelObject.getAddress(), locale));
		txtAccounts.forEach((suite, field) -> field
			.setText(LongValues.shortened(modelObject.getAccount(suite), locale)));
		txtHolds.forEach((suite, field) -> field.setText(modelObject.getHolds(suite)));
		txtWaitingForSweep.setText(modelObject.getWaitingForSweep());
		txtChainChecked.setText(modelObject.getChainChecked());
		txtSupply.setText(modelObject.getSupply());
		for (JMTextField field : resultFields())
		{
			field.setCaretPosition(0);
		}
	}

	/** @return the read-only fields of the answer, which show their values from the beginning */
	private java.util.List<JMTextField> resultFields()
	{
		java.util.List<JMTextField> fields = new java.util.ArrayList<>();
		fields.add(txtAddress);
		fields.addAll(txtAccounts.values());
		fields.addAll(txtHolds.values());
		fields.add(txtWaitingForSweep);
		fields.add(txtChainChecked);
		fields.add(txtSupply);
		return fields;
	}

	private void nameAndExplainTheInputs()
	{
		txtChainFile.setName("txtChainFile");
		txtWalletFile.setName("txtWalletFile");
		txtPassword.setName("txtPassword");
		txtAnswer.setName("txtAnswer");
		txtAnswer.setEditable(false);
		txtAnswer.setLineWrap(true);
		txtAnswer.setWrapStyleWord(true);
		txtAnswer.setOpaque(false);
		txtAnswer.setBorder(BorderFactory.createEmptyBorder());
		lblResult.setName("lblResult");
		pnlDetails.setName("pnlDetails");
		txtAnswer.setFont(lblResult.getFont().deriveFont(Font.BOLD,
			lblResult.getFont().getSize2D() * ANSWER_SCALE));
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
		txtChainFile.setToolTipText(text("lethenon.balance.tooltip.chain.file",
			"the file that holds the chain; what your wallet holds is read from it here, without "
				+ "asking anybody"));
		txtWalletFile.setToolTipText(text("lethenon.balance.tooltip.wallet.file",
			"the file with your keys, made when you created your wallet"));
		txtPassword.setToolTipText(text("lethenon.balance.tooltip.password",
			"the password of your wallet file; it is used once and then forgotten"));
		txtAnswer.setToolTipText(text("lethenon.balance.tooltip.answer",
			"what your wallet holds, read from the chain just now"));
		txtAddress.setToolTipText(text("lethenon.balance.tooltip.address",
			"give this address to anyone who wants to pay you; it never changes, and the chain "
				+ "does not show who paid it"));
		txtWaitingForSweep.setToolTipText(text("lethenon.balance.tooltip.waiting",
			"payments that came to your address; to spend them, sweep them onto your account in "
				+ "the window Sweep"));
		txtChainChecked.setToolTipText(text("lethenon.balance.tooltip.chain.checked",
			"every block, transfer and signature of the chain was checked here before the balance "
				+ "was read"));
		txtSupply.setToolTipText(text("lethenon.balance.tooltip.supply",
			"all the LETH on this chain, everybody's together; it grows only by the small reward "
				+ "each new block adds"));
		String account = text("lethenon.balance.tooltip.account",
			"one of your accounts; somebody can pay you here directly, but your address keeps "
				+ "payments private, so give the address");
		String holds = text("lethenon.balance.tooltip.holds",
			"what this account holds; you can spend it");
		txtAccounts.values().forEach(field -> field.setToolTipText(account));
		txtHolds.values().forEach(field -> field.setToolTipText(holds));
	}

	private void layOutTheInputs()
	{
		add(new JLabel(text("lethenon.balance.label.chain.file", "Chain file:")));
		add(txtChainFile, WITH_BUTTON);
		add(LethenonSwing.button("btnBrowseChainFile", "...",
			event -> LethenonSwing.chooseFile(this, modelObject.getChainFile())
				.ifPresent(txtChainFile::setText),
			text("lethenon.balance.tooltip.browse.chain.file", "pick the chain file on your disk")),
			OWN_WIDTH);
		add(new JLabel(text("lethenon.balance.label.wallet.file", "Wallet file:")));
		add(txtWalletFile, WITH_BUTTON);
		add(LethenonSwing.button("btnBrowseWalletFile", "...",
			event -> LethenonSwing.chooseFile(this, modelObject.getWalletFile())
				.ifPresent(txtWalletFile::setText),
			text("lethenon.balance.tooltip.browse.wallet.file",
				"pick your wallet file on your disk")),
			OWN_WIDTH);
		add(new JLabel(text("lethenon.balance.label.password", "Password:")));
		add(txtPassword, ToolForm.FIELD);
		add(ToolForm.buttons(LethenonSwing.button("btnShowBalance",
			text("lethenon.balance.button.show", "Show my balance"), event -> onShow(),
			text("lethenon.balance.tooltip.show",
				"read the chain and show what your wallet holds; nobody is asked, everything is "
					+ "checked here"))),
			ToolForm.BUTTON_ROW);
	}

	private void layOutTheAnswer()
	{
		add(txtAnswer, ToolForm.WIDE + ", wmin 0");
		add(new JLabel(text("lethenon.balance.label.address", "Your address:")));
		add(txtAddress, WITH_BUTTON);
		add(copyButton("btnCopyAddress", modelObject::getAddress,
			text("lethenon.balance.tooltip.copy.address",
				"copy your whole address, to paste it into a message")),
			OWN_WIDTH);
		String copyAccount = text("lethenon.balance.tooltip.copy.account",
			"copy the whole account, the part that is not shown included");
		for (SignatureSuite suite : SignatureSuite.values())
		{
			String id = suite.identifier();
			add(new JLabel(LethenonMessages.format("lethenon.balance.label.account",
				"Your {0} account:", locale, suiteName(suite))));
			add(txtAccounts.get(id), WITH_BUTTON);
			add(copyButton("btnCopyAccount_" + id, () -> modelObject.getAccount(id), copyAccount),
				OWN_WIDTH);
			add(new JLabel(text("lethenon.balance.label.holds", "It holds:")));
			add(txtHolds.get(id), ToolForm.FIELD);
		}
		add(new JLabel(text("lethenon.balance.label.waiting", "Waiting for a sweep:")));
		add(txtWaitingForSweep, ToolForm.FIELD);
		add(ToolForm.buttons(btnDetails), ToolForm.BUTTON_ROW);
		pnlDetails.add(new JLabel(text("lethenon.balance.label.chain.checked", "Chain checked:")));
		pnlDetails.add(txtChainChecked, ToolForm.FIELD);
		pnlDetails.add(new JLabel(text("lethenon.balance.label.supply", "All the money there is:")));
		pnlDetails.add(txtSupply, ToolForm.FIELD);
		add(pnlDetails, ToolForm.WIDE + ", hidemode 3");
	}

	private JButton copyButton(final String name, final java.util.function.Supplier<String> value,
		final String tooltip)
	{
		return LethenonSwing.button(name, text("lethenon.balance.button.copy", "Copy"),
			event -> LethenonSwing.copy(value.get()), tooltip);
	}

	private String detailsButtonText()
	{
		return modelObject.isDetailsShown()
			? text("lethenon.balance.button.details.hide", "Hide details")
			: text("lethenon.balance.button.details.show", "Show details");
	}

	private void bindToTheModel()
	{
		txtChainFile
			.setPropertyModel(LambdaModel.of(modelObject::getChainFile, modelObject::setChainFile));
		txtWalletFile.setPropertyModel(
			LambdaModel.of(modelObject::getWalletFile, modelObject::setWalletFile));
		txtPassword
			.setPropertyModel(LambdaModel.of(modelObject::getPassword, modelObject::setPassword));
		txtAddress.setPropertyModel(
			LambdaModel.of(() -> LongValues.shortened(modelObject.getAddress(), locale),
				LethenonBalancePanel::shownOnly));
		txtAccounts.forEach((suite, field) -> field.setPropertyModel(LambdaModel.of(
			() -> LongValues.shortened(modelObject.getAccount(suite), locale),
			LethenonBalancePanel::shownOnly)));
		txtHolds.forEach((suite, field) -> field.setPropertyModel(
			LambdaModel.of(() -> modelObject.getHolds(suite), LethenonBalancePanel::shownOnly)));
		txtWaitingForSweep.setPropertyModel(
			LambdaModel.of(modelObject::getWaitingForSweep, LethenonBalancePanel::shownOnly));
		txtChainChecked.setPropertyModel(
			LambdaModel.of(modelObject::getChainChecked, LethenonBalancePanel::shownOnly));
		txtAnswer.setPropertyModel(
			LambdaModel.of(modelObject::getAnswer, LethenonBalancePanel::shownOnly));
		txtSupply.setPropertyModel(
			LambdaModel.of(modelObject::getSupply, LethenonBalancePanel::shownOnly));
	}

	/**
	 * What a read-only field writes back into the model: nothing. The field shows the model and
	 * never writes into it, so a shortened address cannot replace the whole one Copy copies
	 *
	 * @param shown
	 *            the text the field shows
	 */
	private static void shownOnly(final String shown)
	{
		// a read-only field's text is derived from the model, never the other way round
	}

	private static JMTextField readOnly(final String name)
	{
		JMTextField field = new JMTextField(34);
		field.setName(name);
		field.setEditable(false);
		return field;
	}

	private static String suiteName(final SignatureSuite suite)
	{
		return suite == SignatureSuite.ED25519 ? "Ed25519"
			: suite.identifier().toUpperCase(Locale.ROOT);
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
}
