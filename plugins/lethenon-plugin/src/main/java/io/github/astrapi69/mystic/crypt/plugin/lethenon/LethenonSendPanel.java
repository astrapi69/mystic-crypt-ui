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
import java.util.Set;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

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
 * block carries it; this window writes no block. The password goes from the field into the model as
 * a {@code char[]}, is wiped by the support class, and is cleared from the field afterwards,
 * whether the transfer was signed or refused.
 */
public class LethenonSendPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** A field that shares its cell with the button that fills it from a file chooser */
	private static final String WITH_BUTTON = "growx, split 2";

	/** Something that keeps its own width instead of growing with the cell */
	private static final String OWN_WIDTH = "alignx left, width pref!";

	private final LethenonSendPanelModel modelObject = new LethenonSendPanelModel();

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

	private final JMTextArea txtReport = new JMTextArea(4, 62);

	private final JLabel lblResult = new JLabel(" ");

	/**
	 * Instantiates a new {@link LethenonSendPanel}, prefilled with the chain file from the plugin's
	 * settings
	 */
	public LethenonSendPanel()
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
	public LethenonSendPanelModel getModelObject()
	{
		return modelObject;
	}

	/**
	 * Signs the transfer. The password is used once: the support class wipes the array it gets, and
	 * the field and the model are cleared afterwards, whether the transfer was signed or not.
	 */
	void onSend()
	{
		String failure = LethenonMessages.getString("lethenon.result.send.failed",
			"the transfer was not signed");
		try
		{
			SentTransfer sent = TransferSupport.send(orderFromTheModel(),
				modelObject.getPassword());
			setReport(describe(sent));
			setResult(LethenonMessages.getString("lethenon.result.send.signed",
				"the transfer was signed and waits for the next block"));
		}
		catch (ChainRejected | IllegalArgumentException refused)
		{
			setReport("");
			setResult(failure + ": " + refused.getMessage());
		}
		catch (IOException unwritable)
		{
			setReport("");
			setResult(failure + ": " + unwritable.getMessage());
		}
		finally
		{
			modelObject.setPassword(null);
			txtPassword.setText("");
		}
	}

	private TransferOrder orderFromTheModel()
	{
		return new TransferOrder(Path.of(modelObject.getChainFile().trim()),
			Path.of(modelObject.getWalletFile().trim()), modelObject.getSuite(),
			modelObject.getRecipientKind(), modelObject.getRecipient(), modelObject.getAmount(),
			modelObject.getFee(), modelObject.getMemo());
	}

	private static String describe(final SentTransfer sent)
	{
		return LethenonMessages.getString("lethenon.send.signed", "signed a transfer of") + " "
			+ sent.amount() + " LETH " + LethenonMessages.getString("lethenon.send.to", "to") + " "
			+ whomItNames(sent) + " "
			+ LethenonMessages.getString("lethenon.send.with.nonce", "with nonce") + " "
			+ sent.nonce() + "; "
			+ LethenonMessages.getString("lethenon.send.waits",
				"it waits for the next block, which mining a pun writes")
			+ " (" + sent.waiting() + " "
			+ LethenonMessages.getString("lethenon.send.waiting", "waiting") + ")";
	}

	/**
	 * What the report says the money went to: the account key, or for a published address "a
	 * one-time destination of" the address - never the destination itself
	 */
	private static String whomItNames(final SentTransfer sent)
	{
		return switch (sent.recipientKind())
		{
			case ACCOUNT_KEY -> sent.recipient();
			case PUBLISHED_ADDRESS -> LethenonMessages.getString("lethenon.send.one.time.of",
				"a one-time destination of") + " " + sent.recipient();
		};
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
		txtReport.setName("txtReport");
		txtReport.setEditable(false);
		txtReport.setLineWrap(true);
		txtReport.setWrapStyleWord(true);
		txtReport.setFont(new Font("monospaced", Font.PLAIN, 12));
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
		cbxSuite.setRenderer(new DefaultListCellRenderer()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value,
				final int index, final boolean selected, final boolean focused)
			{
				Object shown = value instanceof SignatureSuite suite ? suite.identifier() : value;
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
				Object shown = value instanceof RecipientKind kind ? kind.description() : value;
				return super.getListCellRendererComponent(list, shown, index, selected, focused);
			}
		});
	}

	private void explainTheComponents()
	{
		txtChainFile.setToolTipText(LethenonMessages.getString("lethenon.tooltip.send.chain.file",
			"the chain file; the transfer waits next to it until the next block"));
		txtWalletFile.setToolTipText(LethenonMessages.getString("lethenon.tooltip.wallet.file",
			"the wallet file lethenon's 'wallet create' or 'wallet restore' wrote"));
		txtPassword.setToolTipText(LethenonMessages.getString("lethenon.tooltip.wallet.password",
			"the wallet file's password; it is used once and then cleared"));
		cbxSuite.setToolTipText(LethenonMessages.getString("lethenon.tooltip.send.suite",
			"the account the transfer is paid from: the wallet has one per signature suite"));
		cbxRecipientKind
			.setToolTipText(LethenonMessages.getString("lethenon.tooltip.send.recipient.kind",
				"an account key is named on the chain as it is; a published address is paid at a "
					+ "one-time destination nobody but its holder can connect to it"));
		txtRecipient.setToolTipText(LethenonMessages.getString("lethenon.tooltip.send.recipient",
			"the recipient's account key in hexadecimal, or a published address: view key and "
				+ "spend key in hexadecimal, separated by ':'"));
		txtAmount.setToolTipText(LethenonMessages.getString("lethenon.tooltip.send.amount",
			"the amount in LETH, up to eight decimals, e.g. 12.5"));
		txtFee.setToolTipText(LethenonMessages.getString("lethenon.tooltip.send.fee",
			"the fee in LETH, paid to the pool; 0 is allowed"));
		txtMemo.setToolTipText(LethenonMessages.getString("lethenon.tooltip.send.memo",
			"text signed with the transfer; everyone who reads the chain reads it too"));
	}

	private void layOut()
	{
		add(new JLabel(LethenonMessages.getString("lethenon.label.chain.file", "Chain file:")));
		add(txtChainFile, WITH_BUTTON);
		add(LethenonSwing.button("btnBrowseChainFile", "...",
			event -> LethenonSwing.chooseFile(this, modelObject.getChainFile())
				.ifPresent(txtChainFile::setText),
			LethenonMessages.getString("lethenon.tooltip.browse.chain.file",
				"pick the chain file")),
			OWN_WIDTH);
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
		add(new JLabel(LethenonMessages.getString("lethenon.label.send.suite", "From account:")));
		add(cbxSuite, OWN_WIDTH);
		add(new JLabel(LethenonMessages.getString("lethenon.label.send.recipient", "To:")));
		add(cbxRecipientKind, "growx, split 2, width pref!");
		add(txtRecipient, ToolForm.FIELD);
		add(new JLabel(LethenonMessages.getString("lethenon.label.send.amount", "Amount (LETH):")));
		add(txtAmount, OWN_WIDTH);
		add(new JLabel(LethenonMessages.getString("lethenon.label.send.fee", "Fee (LETH):")));
		add(txtFee, OWN_WIDTH);
		add(new JLabel(LethenonMessages.getString("lethenon.label.send.memo", "Memo:")));
		add(txtMemo, ToolForm.FIELD);
		add(ToolForm.buttons(LethenonSwing.button("btnSend",
			LethenonMessages.getString("lethenon.button.send", "Sign the transfer"),
			event -> onSend(),
			LethenonMessages.getString("lethenon.tooltip.send",
				"sign the transfer and leave it waiting for the next block"))),
			ToolForm.BUTTON_ROW);
		add(new JLabel(LethenonMessages.getString("lethenon.label.report", "Report:")),
			"aligny top");
		add(ToolForm.scrolled(txtReport), ToolForm.GROWING);
		add(lblResult, ToolForm.RESULT_LINE);
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
		txtReport.setPropertyModel(LambdaModel.of(modelObject::getReport, modelObject::setReport));
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
}
