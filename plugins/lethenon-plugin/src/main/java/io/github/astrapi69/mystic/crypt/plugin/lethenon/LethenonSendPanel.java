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

import java.awt.Component;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import io.github.astrapi69.lethenon.Amount;
import io.github.astrapi69.lethenon.ChainRejected;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.combobox.EnumComboBoxModel;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMPasswordField;
import io.github.astrapi69.swing.model.component.JMTextArea;
import io.github.astrapi69.swing.model.component.JMTextField;

/**
 * Sends LETH with a memo to an account key or to a published address (lethenon#2, milestone 5) -
 * what lethenon's {@code send --to} and {@code send --to-address} do on the command line.
 * <p>
 * Which of the two the recipient is, the person sending says, rather than the window guessing it
 * from the text. A published address is paid at a one-time destination, and the report names the
 * ADDRESS it was derived from, never the destination: the sender's own screen is a place where that
 * link would be written down.
 * <p>
 * The transfer is signed by {@link TransferSupport} and waits next to the chain file until the next
 * block carries it, or, with a node named, is handed to that running node, which serves the chain
 * file and keeps it in its pool (#530 step 3); this window writes no block. A local node answers at
 * once, so the handover runs in the window like the signing. The password goes from the field into
 * the model as a {@code char[]}, is wiped by the support class, and is cleared from the field
 * afterwards, whether the transfer was signed or refused.
 * <p>
 * It answers in fields a child can read (#604): first one sentence in a larger font, "Sent:
 * 12.50000000 LETH to the account you named. It counts once the next block is mined.", then the
 * amount, the fee, the recipient and where the transfer waits. The transfer's number from the
 * account and how many transfers wait are behind "Show details", closed at first.
 */
public class LethenonSendPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** A field that shares its cell with the button that fills it from a file chooser */
	private static final String WITH_BUTTON = "growx, split 2";

	/** Something that keeps its own width instead of growing with the cell */
	private static final String OWN_WIDTH = "alignx left, width pref!";

	private final LethenonSendPanelModel modelObject = new LethenonSendPanelModel();

	/** Whom to ask before a chain file of lethenon 0.5.0 or earlier is converted (#581) */
	private final ChainConversionConsent consent;

	private final JMTextField txtChainFile = new JMTextField(34);

	private final JMTextField txtWalletFile = new JMTextField(34);

	private final JMPasswordField txtPassword = new JMPasswordField(34);

	private final JMComboBox<SignatureSuite, ComboBoxModel<SignatureSuite>> cbxSuite = new JMComboBox<>(
		new EnumComboBoxModel<>(SignatureSuite.class, SignatureSuite.ED25519, Set.of()));

	private final JMComboBox<RecipientKind, ComboBoxModel<RecipientKind>> cbxRecipientKind = new JMComboBox<>(
		new EnumComboBoxModel<>(RecipientKind.class, RecipientKind.ACCOUNT_KEY, Set.of()));

	private final JMTextField txtRecipient = new JMTextField(34);

	private final JMTextField txtAmount = new JMTextField(12);

	private final JMTextField txtFee = new JMTextField(12);

	private final JMTextField txtMemo = new JMTextField(34);

	private final JMTextField txtNode = new JMTextField(34);

	/**
	 * The language of the window; the field initializers below read it, so it has its value before
	 * the constructor sets the one it was given
	 */
	private Locale locale = Locale.getDefault();

	/** The plugin's view of the vault, whose wallet the window sends from when there is one (#704) */
	private final transient LethenonVault vault;

	/** Whom to ask before a send on the main network or on an unknown network (#704) */
	private final transient SendConfirmation confirmation;

	/** The wallet the window signs with: the vault's, or a wallet file (#706) */
	private final transient WalletRow walletRow;

	private final JMTextArea txtAnswer = AnswerFields.answerArea(text("lethenon.send.tooltip.answer",
		"what happened to your transfer, in one sentence; the details are below"));

	private final JMTextField txtSentAmount = AnswerFields.readOnly("txtSentAmount",
		text("lethenon.send.tooltip.sent.amount", "what the recipient gets"));

	private final JMTextField txtSentFee = AnswerFields.readOnly("txtSentFee",
		text("lethenon.send.tooltip.sent.fee",
			"what you pay on top, to whoever mines the block that carries your transfer"));

	private final JMTextField txtSentTo = AnswerFields.readOnly("txtSentTo",
		text("lethenon.send.tooltip.sent.to",
			"whom you paid, as you typed it; for an address the chain shows a one-time "
				+ "destination instead, which only you and the recipient can connect to it"));

	private final JMTextField txtWhereItWaits = AnswerFields.readOnly("txtWhereItWaits",
		text("lethenon.send.tooltip.where.it.waits",
			"a transfer counts once a block carries it; until then it waits here"));

	private final JButton btnDetails;

	private final JPanel pnlDetails = new JPanel(ToolForm.newLayout());

	private final JMTextField txtNonce = AnswerFields.readOnly("txtNonce",
		text("lethenon.send.tooltip.nonce",
			"your account numbers its transfers 0, 1, 2 and so on; the chain takes them in this "
				+ "order and never one number twice"));

	private final JMTextField txtWaiting = AnswerFields.readOnly("txtWaiting",
		text("lethenon.send.tooltip.waiting",
			"how many transfers wait for the next block now, yours included"));

	private final JLabel lblResult = new JLabel(" ");

	/**
	 * Instantiates a new {@link LethenonSendPanel}, prefilled with the chain file from the plugin's
	 * settings
	 */
	public LethenonSendPanel()
	{
		this(ChainConversionConsent.ASK);
	}

	/**
	 * Instantiates a new {@link LethenonSendPanel} that asks through the given consent before it
	 * converts a chain file of lethenon 0.5.0 or earlier (#581)
	 *
	 * @param consent
	 *            whom to ask
	 */
	LethenonSendPanel(final ChainConversionConsent consent)
	{
		this(consent, LethenonVault.current(), SendConfirmation.ASK, Locale.getDefault());
	}

	/**
	 * Instantiates a new {@link LethenonSendPanel} over the given vault
	 *
	 * @param consent
	 *            whom to ask before a chain file is converted
	 * @param vault
	 *            the plugin's view of the vault; its wallet is the one sent from
	 * @param confirmation
	 *            whom to ask before a send on the main network or an unknown one
	 * @param locale
	 *            the language of the window
	 */
	LethenonSendPanel(final ChainConversionConsent consent, final LethenonVault vault,
		final SendConfirmation confirmation, final Locale locale)
	{
		super(ToolForm.newLayout());
		this.consent = consent;
		this.vault = vault;
		this.confirmation = confirmation;
		this.locale = locale;
		walletRow = new WalletRow(vault, locale, txtWalletFile,
			() -> LethenonSwing.chooseFile(this, modelObject.getWalletFile())
				.ifPresent(txtWalletFile::setText));
		btnDetails = LethenonSwing.button("btnDetails",
			AnswerFields.detailsButtonText(false, locale), event -> onDetails(),
			text("lethenon.send.tooltip.details",
				"show the number of your transfer and how many transfers wait"));
		nameTheComponents();
		explainTheComponents();
		bindToTheModel();
		layOutTheInputs();
		layOutTheAnswer();
		add(lblResult, ToolForm.RESULT_LINE);
		pnlDetails.setVisible(false);
		txtChainFile.setText(LethenonSettingsContribution.chainFile());
		txtWalletFile.setText(LethenonSettingsContribution.walletFile());
		txtNode.setText(LethenonSettingsContribution.node());
	}

	@Override
	public void removeNotify()
	{
		walletRow.stop();
		super.removeNotify();
	}

	/**
	 * Gets the panel's state, which is what a test reads instead of the widgets
	 *
	 * @return the model object every component of this panel is bound to
	 */
	public LethenonSendPanelModel getModelObject()
	{
		return modelObject;
	}

	/**
	 * Signs the transfer. The password is used once: the support class wipes the array it gets, and
	 * the field and the model are cleared afterwards, whether the transfer was signed or not. A
	 * failure leaves no value of an earlier transfer on the screen.
	 */
	void onSend()
	{
		String failure = text("lethenon.send.result.failed", "the transfer was not signed");
		try
		{
			TransferOrder order = orderFromTheModel();
			if (!ChainConversion.convertedIfAgreed(this, consent, order.chainFile()))
			{
				showFailure(failure + ": " + ChainConversion.notConverted());
				return;
			}
			if (!confirmedForItsNetwork(order))
			{
				showFailure(failure + ": " + text("lethenon.send.result.cancelled",
					"you cancelled before anything was signed"));
				return;
			}
			SentTransfer sent = sendFromTheSource(order);
			rememberWhatWorked(order);
			showTheAnswer(sent);
			setResult(sent.node().isEmpty()
				? text("lethenon.send.result.signed",
					"the transfer was signed and waits for the next block")
				: text("lethenon.send.result.handed", "the transfer was signed and handed to the node"));
		}
		catch (ChainRejected | IllegalArgumentException | IllegalStateException refused)
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

	/**
	 * The question before a send from the vault's wallet: none on the test network, T5 on the main
	 * network, and on an unknown network first which network it is (draft 02)
	 *
	 * @return whether the send may go on
	 */
	private boolean confirmedForItsNetwork(final TransferOrder order)
	{
		Optional<VaultWallet> chosen = vault.chosen();
		if (chosen.isEmpty())
		{
			return true;
		}
		NetworkKind network = NetworkKind.of(chosen.get().network());
		if (network == NetworkKind.UNKNOWN)
		{
			Optional<NetworkKind> answered = confirmation.askNetwork(this);
			if (answered.isEmpty())
			{
				return false;
			}
			network = answered.get();
		}
		return network != NetworkKind.MAIN
			|| confirmation.confirmMainNetwork(this, mainNetworkTransfer(order));
	}

	private MainNetworkTransfer mainNetworkTransfer(final TransferOrder order)
	{
		Amount amount = Amount.parseLeth(order.amount().trim());
		Amount fee = order.fee().isBlank() ? Amount.ZERO : Amount.parseLeth(order.fee().trim());
		return new MainNetworkTransfer(LethAmounts.format(amount, locale),
			LethAmounts.format(fee, locale), LethAmounts.format(amount.plus(fee), locale),
			order.recipient().trim());
	}

	/** Signs with the vault's wallet, unsealed for this send only, or with the wallet file */
	private SentTransfer sendFromTheSource(final TransferOrder order) throws IOException
	{
		char[] password = modelObject.getPassword();
		try
		{
			return TransferSupport.send(order, walletRow.source(password, order.walletFile()));
		}
		finally
		{
			Arrays.fill(password, '\0');
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
	 * The sentence that answers what the window was opened for (#604, rule 1): what was sent, to
	 * which kind of recipient, and when it counts. It names neither the recipient nor, for a
	 * published address, the one-time destination: the recipient is in its own field, and the
	 * destination nowhere on this screen
	 *
	 * @param sent
	 *            the transfer
	 * @param locale
	 *            the language of the window
	 * @return the sentence
	 */
	static String answerFor(final SentTransfer sent, final Locale locale)
	{
		String amount = LethAmounts.format(sent.amount(), locale);
		String key = sent.recipientKind() == RecipientKind.PUBLISHED_ADDRESS
			? "lethenon.send.answer.address"
			: "lethenon.send.answer.account";
		String defaultValue = sent.recipientKind() == RecipientKind.PUBLISHED_ADDRESS
			? "Sent: {0} to the address you named."
			: "Sent: {0} to the account you named.";
		String what = LethenonMessages.format(key, defaultValue, locale, amount);
		String when = sent.node().isEmpty()
			? LethenonMessages.getString("lethenon.send.answer.waits",
				"It counts once the next block is mined.", locale)
			: LethenonMessages.format("lethenon.send.answer.handed",
				"The node at {0} puts it into one of its next blocks.", locale, sent.node());
		return what + " " + when;
	}

	private void showTheAnswer(final SentTransfer sent)
	{
		modelObject.clearAnswer();
		modelObject.setAnswer(answerFor(sent, locale));
		modelObject.setSentAmount(LethAmounts.format(sent.amount(), locale));
		modelObject.setSentFee(LethAmounts.format(sent.fee(), locale));
		modelObject.setSentTo(sent.recipient());
		modelObject.setWhereItWaits(sent.node().isEmpty()
			? text("lethenon.send.waits.here", "next to the chain file, until the next block is mined")
			: LethenonMessages.format("lethenon.send.waits.node", "in the pool of the node at {0}",
				locale, sent.node()));
		modelObject.setNonce(
			LethenonMessages.format("lethenon.send.nonce", "{0,number,integer}", locale, sent.nonce()));
		modelObject.setWaiting(LethenonMessages.format("lethenon.send.waiting.count",
			"{0,choice,0#none|1#1 transfer|1<{0,number,integer} transfers}", locale, sent.waiting()));
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
		AnswerFields.setText(txtSentAmount, modelObject.getSentAmount());
		AnswerFields.setText(txtSentFee, modelObject.getSentFee());
		AnswerFields.setText(txtSentTo, LongValues.shortened(modelObject.getSentTo(), locale));
		AnswerFields.setText(txtWhereItWaits, modelObject.getWhereItWaits());
		AnswerFields.setText(txtNonce, modelObject.getNonce());
		AnswerFields.setText(txtWaiting, modelObject.getWaiting());
	}

	/**
	 * Remembers what a signed transfer used (#605): the chain file, the wallet, and the node it was
	 * handed to; a transfer left waiting beside the chain names no node, so none is remembered
	 */
	private static void rememberWhatWorked(final TransferOrder order)
	{
		Map<String, String> worked = new java.util.LinkedHashMap<>();
		worked.put(LethenonSettingsContribution.KEY_CHAIN_FILE, order.chainFile().toString());
		worked.put(LethenonSettingsContribution.KEY_WALLET_FILE, order.walletFile().toString());
		if (order.node() != null && !order.node().isBlank())
		{
			worked.put(LethenonSettingsContribution.KEY_NODE, order.node().trim());
		}
		LethenonSettingsContribution.remember(worked);
	}

	private TransferOrder orderFromTheModel()
	{
		return new TransferOrder(Path.of(modelObject.getChainFile().trim()),
			Path.of(modelObject.getWalletFile() == null ? "" : modelObject.getWalletFile().trim()),
			modelObject.getSuite(),
			modelObject.getRecipientKind(), modelObject.getRecipient(), modelObject.getAmount(),
			modelObject.getFee(), modelObject.getMemo(), modelObject.getNode());
	}

	private void nameTheComponents()
	{
		txtChainFile.setName("txtChainFile");
		txtWalletFile.setName("txtWalletFile");
		txtPassword.setName("txtPassword");
		cbxSuite.setName("cbxSuite");
		cbxRecipientKind.setName("cbxRecipientKind");
		txtRecipient.setName("txtRecipient");
		txtAmount.setName("txtAmount");
		txtFee.setName("txtFee");
		txtMemo.setName("txtMemo");
		txtNode.setName("txtNode");
		pnlDetails.setName("pnlDetails");
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
		cbxSuite.setRenderer(new DefaultListCellRenderer()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value,
				final int index, final boolean selected, final boolean focused)
			{
				Object shown = value instanceof SignatureSuite suite
					? LethenonMessages.format("lethenon.send.suite", "your {0} account", locale,
						AnswerFields.suiteName(suite))
					: value;
				return super.getListCellRendererComponent(list, shown, index, selected, focused);
			}
		});
		cbxRecipientKind.setRenderer(new DefaultListCellRenderer()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value,
				final int index, final boolean selected, final boolean focused)
			{
				Object shown = value instanceof RecipientKind kind
					? LethenonMessages.getString("lethenon.recipient.kind." + kind.name(),
						kind.description(), locale)
					: value;
				return super.getListCellRendererComponent(list, shown, index, selected, focused);
			}
		});
	}

	private void explainTheComponents()
	{
		txtChainFile.setToolTipText(text("lethenon.send.tooltip.chain.file",
			"the file that holds the chain; your transfer waits next to it until the next block "
				+ "carries it"));
		txtWalletFile.setToolTipText(text("lethenon.send.tooltip.wallet.file",
			"your wallet file; the money is paid from one of its accounts"));
		txtPassword.setToolTipText(text("lethenon.input.tooltip.password",
			"the password of your wallet file; it is used once and then forgotten"));
		cbxSuite.setToolTipText(text("lethenon.send.tooltip.suite",
			"which of your accounts pays: your wallet has one for each kind of signature"));
		cbxRecipientKind.setToolTipText(text("lethenon.send.tooltip.recipient.kind",
			"an account key is named on the chain as it is; a published address is paid at a "
				+ "one-time destination that a reader of the chain cannot connect to the address, "
				+ "while its holder and the sender can; the sender and the amount are on the chain "
				+ "either way"));
		txtRecipient.setToolTipText(text("lethenon.send.tooltip.recipient",
			"paste what the recipient gave you: an account, or an address with a ':' in the "
				+ "middle"));
		txtAmount.setToolTipText(text("lethenon.send.tooltip.amount",
			"how much the recipient gets, in LETH, for example 12.5"));
		txtFee.setToolTipText(text("lethenon.send.tooltip.fee",
			"what you pay on top to whoever mines the block that carries your transfer; 0 is "
				+ "allowed"));
		txtMemo.setToolTipText(text("lethenon.send.tooltip.memo",
			"a note that goes with the transfer; everyone who reads the chain can read it too, so "
				+ "write nothing private"));
		txtNode.setToolTipText(text("lethenon.send.tooltip.node",
			"leave it empty to keep the transfer next to your chain file; or type the host:port of "
				+ "a running node, which then puts it into one of its next blocks"));
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
		add(new JLabel(text("lethenon.send.label.suite", "Pay from:")));
		add(cbxSuite, OWN_WIDTH);
		add(new JLabel(text("lethenon.send.label.recipient", "To:")));
		add(cbxRecipientKind, "growx, split 2, width pref!");
		add(txtRecipient, ToolForm.FIELD);
		add(new JLabel(text("lethenon.send.label.amount", "Amount (LETH):")));
		add(txtAmount, OWN_WIDTH);
		add(new JLabel(text("lethenon.send.label.fee", "Fee (LETH):")));
		add(txtFee, OWN_WIDTH);
		add(new JLabel(text("lethenon.send.label.memo", "Note:")));
		add(txtMemo, ToolForm.FIELD);
		add(new JLabel(text("lethenon.send.label.node", "Node (optional):")));
		add(txtNode, ToolForm.FIELD);
		add(ToolForm.buttons(LethenonSwing.button("btnSend",
			text("lethenon.send.button.send", "Send"), event -> onSend(),
			text("lethenon.send.tooltip.send",
				"sign the transfer with your password and leave it for the next block; nothing "
					+ "is sent anywhere unless you named a node"))),
			ToolForm.BUTTON_ROW);
	}

	private void layOutTheAnswer()
	{
		add(txtAnswer, ToolForm.WIDE + ", wmin 0");
		add(new JLabel(text("lethenon.send.label.sent.amount", "Amount:")));
		add(txtSentAmount, ToolForm.FIELD);
		add(new JLabel(text("lethenon.send.label.sent.fee", "Fee:")));
		add(txtSentFee, ToolForm.FIELD);
		add(new JLabel(text("lethenon.send.label.sent.to", "Paid to:")));
		add(txtSentTo, ToolForm.FIELD);
		add(new JLabel(text("lethenon.send.label.where.it.waits", "Waits:")));
		add(txtWhereItWaits, ToolForm.FIELD);
		add(ToolForm.buttons(btnDetails), ToolForm.BUTTON_ROW);
		pnlDetails.add(new JLabel(text("lethenon.send.label.nonce", "Number of this transfer:")));
		pnlDetails.add(txtNonce, ToolForm.FIELD);
		pnlDetails.add(new JLabel(text("lethenon.send.label.waiting", "Waiting now:")));
		pnlDetails.add(txtWaiting, ToolForm.FIELD);
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
		cbxSuite.setPropertyModel(LambdaModel.of(modelObject::getSuite, modelObject::setSuite));
		cbxRecipientKind.setPropertyModel(
			LambdaModel.of(modelObject::getRecipientKind, modelObject::setRecipientKind));
		txtRecipient
			.setPropertyModel(LambdaModel.of(modelObject::getRecipient, modelObject::setRecipient));
		txtAmount.setPropertyModel(LambdaModel.of(modelObject::getAmount, modelObject::setAmount));
		txtFee.setPropertyModel(LambdaModel.of(modelObject::getFee, modelObject::setFee));
		txtFee.setText(modelObject.getFee());
		txtMemo.setPropertyModel(LambdaModel.of(modelObject::getMemo, modelObject::setMemo));
		txtNode.setPropertyModel(LambdaModel.of(modelObject::getNode, modelObject::setNode));
		AnswerFields.show(txtAnswer, modelObject::getAnswer);
		AnswerFields.show(txtSentAmount, modelObject::getSentAmount);
		AnswerFields.show(txtSentFee, modelObject::getSentFee);
		AnswerFields.show(txtSentTo, () -> LongValues.shortened(modelObject.getSentTo(), locale));
		AnswerFields.show(txtWhereItWaits, modelObject::getWhereItWaits);
		AnswerFields.show(txtNonce, modelObject::getNonce);
		AnswerFields.show(txtWaiting, modelObject::getWaiting);
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
