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

import javax.swing.JLabel;
import javax.swing.JPanel;

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
 */
public class LethenonSweepPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** A field that shares its cell with the button that fills it from a file chooser */
	private static final String WITH_BUTTON = "growx, split 2";

	/** Something that keeps its own width instead of growing with the cell */
	private static final String OWN_WIDTH = "alignx left, width pref!";

	private final LethenonSweepPanelModel modelObject = new LethenonSweepPanelModel();

	private final JMTextField txtChainFile = new JMTextField(34);

	private final JMTextField txtWalletFile = new JMTextField(34);

	private final JMPasswordField txtPassword = new JMPasswordField(34);

	private final JMTextField txtFee = new JMTextField(12);

	private final JMTextField txtMemo = new JMTextField(34);

	private final JMTextArea txtCost = new JMTextArea(3, 62);

	private final JMTextArea txtReport = new JMTextArea(4, 62);

	private final JLabel lblResult = new JLabel(" ");

	/**
	 * Instantiates a new {@link LethenonSweepPanel}, prefilled with the chain file from the
	 * plugin's settings
	 */
	public LethenonSweepPanel()
	{
		super(ToolForm.newLayout());
		modelObject.setCostStatement(LethenonMessages.getString("lethenon.sweep.cost",
			"Sweeping signs one transfer per one-time destination, from that destination to this "
				+ "wallet's account, so the chain then shows those destinations and the account "
				+ "together. Receiving is unlinkable; spending is the moment that ends."));
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
	public LethenonSweepPanelModel getModelObject()
	{
		return modelObject;
	}

	/**
	 * Signs the sweep. The password is used once: the support class wipes the array it gets, and
	 * the field and the model are cleared afterwards, whether anything was signed or not.
	 */
	void onSweep()
	{
		String failure = LethenonMessages.getString("lethenon.result.sweep.failed",
			"nothing was swept");
		try
		{
			SweptPayments swept = SweepSupport.sweep(orderFromTheModel(),
				modelObject.getPassword());
			if (swept.transfers() == 0)
			{
				setReport("");
				setResult(LethenonMessages.getString("lethenon.result.sweep.nothing",
					"nothing to sweep: no one-time payment of this wallet holds more than the fee")
					+ " (" + modelObject.getFee().trim() + " LETH)");
				return;
			}
			setReport(describe(swept) + "\n" + modelObject.getCostStatement());
			setResult(LethenonMessages.getString("lethenon.result.sweep.signed",
				"the sweep was signed and waits for the next block"));
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

	private SweepOrder orderFromTheModel()
	{
		return new SweepOrder(Path.of(modelObject.getChainFile().trim()),
			Path.of(modelObject.getWalletFile().trim()), modelObject.getFee(),
			modelObject.getMemo());
	}

	private static String describe(final SweptPayments swept)
	{
		return LethenonMessages.getString("lethenon.sweep.signed", "signed") + " "
			+ swept.transfers() + " "
			+ (swept.transfers() == 1
				? LethenonMessages.getString("lethenon.sweep.transfer", "transfer")
				: LethenonMessages.getString("lethenon.sweep.transfers", "transfers"))
			+ " " + LethenonMessages.getString("lethenon.sweep.moving", "sweeping") + " "
			+ swept.total() + " LETH " + LethenonMessages.getString("lethenon.sweep.onto", "onto")
			+ " " + swept.account() + "; "
			+ LethenonMessages.getString("lethenon.send.waits",
				"it waits for the next block, which mining a pun writes")
			+ " (" + swept.waiting() + " "
			+ LethenonMessages.getString("lethenon.send.waiting", "waiting") + ")";
	}

	private void nameTheComponents()
	{
		txtChainFile.setName("txtChainFile");
		txtWalletFile.setName("txtWalletFile");
		txtPassword.setName("txtPassword");
		txtFee.setName("txtFee");
		txtMemo.setName("txtMemo");
		txtCost.setName("txtCost");
		txtCost.setEditable(false);
		txtCost.setLineWrap(true);
		txtCost.setWrapStyleWord(true);
		txtCost.setOpaque(false);
		txtReport.setName("txtReport");
		txtReport.setEditable(false);
		txtReport.setLineWrap(true);
		txtReport.setWrapStyleWord(true);
		txtReport.setFont(new Font("monospaced", Font.PLAIN, 12));
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
	}

	private void explainTheComponents()
	{
		txtChainFile.setToolTipText(LethenonMessages.getString("lethenon.tooltip.send.chain.file",
			"the chain file; the transfer waits next to it until the next block"));
		txtWalletFile
			.setToolTipText(LethenonMessages.getString("lethenon.tooltip.sweep.wallet.file",
				"the wallet whose one-time payments are swept onto its Ed25519 account"));
		txtPassword.setToolTipText(LethenonMessages.getString("lethenon.tooltip.wallet.password",
			"the wallet file's password; it is used once and then cleared"));
		txtFee.setToolTipText(LethenonMessages.getString("lethenon.tooltip.sweep.fee",
			"the fee of EACH transfer in LETH, taken out of what that destination holds"));
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
		add(new JLabel(
			LethenonMessages.getString("lethenon.label.sweep.fee", "Fee per transfer (LETH):")));
		add(txtFee, OWN_WIDTH);
		add(new JLabel(LethenonMessages.getString("lethenon.label.send.memo", "Memo:")));
		add(txtMemo, ToolForm.FIELD);
		add(txtCost, ToolForm.WIDE);
		add(ToolForm.buttons(LethenonSwing.button("btnSweep",
			LethenonMessages.getString("lethenon.button.sweep", "Sign the sweep"),
			event -> onSweep(),
			LethenonMessages.getString("lethenon.tooltip.sweep",
				"sign one transfer per one-time destination onto this wallet's account, and leave "
					+ "them waiting for the next block"))),
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
		txtFee.setPropertyModel(LambdaModel.of(modelObject::getFee, modelObject::setFee));
		txtFee.setText(modelObject.getFee());
		txtMemo.setPropertyModel(LambdaModel.of(modelObject::getMemo, modelObject::setMemo));
		txtMemo.setText(modelObject.getMemo());
		txtCost.setPropertyModel(
			LambdaModel.of(modelObject::getCostStatement, modelObject::setCostStatement));
		txtCost.setText(modelObject.getCostStatement());
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
