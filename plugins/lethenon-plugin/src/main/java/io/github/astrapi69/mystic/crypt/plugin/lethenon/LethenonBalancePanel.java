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
 * Shows what a wallet holds in a chain file (lethenon#2, milestone 5).
 * <p>
 * The balance is computed by {@link WalletBalanceSupport} from the replayed chain - never asked of
 * anybody - and comes in two parts that this window keeps apart: the wallet's direct accounts,
 * which it can spend from, and the payments to its one-time destinations, which reach those
 * accounts through a sweep. The second part is labelled as such and never added to the first. The
 * wallet's published address is shown with them, because it is what a payer needs.
 * <p>
 * The password goes from the field into the model as a {@code char[]}, is handed to the support
 * class, which wipes it, and is cleared from the field afterwards. It appears in no text this
 * window shows and in no log.
 */
public class LethenonBalancePanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** A field that shares its cell with the button that fills it from a file chooser */
	private static final String WITH_BUTTON = "growx, split 2";

	/** Something that keeps its own width instead of growing with the cell */
	private static final String OWN_WIDTH = "alignx left, width pref!";

	private final LethenonBalancePanelModel modelObject = new LethenonBalancePanelModel();

	private final JMTextField txtChainFile = new JMTextField(34);

	private final JMTextField txtWalletFile = new JMTextField(34);

	private final JMPasswordField txtPassword = new JMPasswordField(34);

	private final JMTextArea txtReport = new JMTextArea(8, 62);

	private final JLabel lblResult = new JLabel(" ");

	/**
	 * Instantiates a new {@link LethenonBalancePanel}, prefilled with the chain file from the
	 * plugin's settings
	 */
	public LethenonBalancePanel()
	{
		super(ToolForm.newLayout());
		txtChainFile.setName("txtChainFile");
		txtWalletFile.setName("txtWalletFile");
		txtPassword.setName("txtPassword");
		txtReport.setName("txtReport");
		txtReport.setEditable(false);
		txtReport.setLineWrap(true);
		txtReport.setWrapStyleWord(true);
		txtReport.setFont(new Font("monospaced", Font.PLAIN, 12));
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));

		txtChainFile.setToolTipText(LethenonMessages.getString("lethenon.tooltip.chain.file",
			"the chain file to replay, written by lethenon's own command line"));
		txtWalletFile.setToolTipText(LethenonMessages.getString("lethenon.tooltip.wallet.file",
			"the wallet file lethenon's 'wallet create' or 'wallet restore' wrote"));
		txtPassword.setToolTipText(LethenonMessages.getString("lethenon.tooltip.wallet.password",
			"the wallet file's password; it is used once and then cleared"));
		bindToTheModel();

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
		add(ToolForm.buttons(LethenonSwing.button("btnShowBalance",
			LethenonMessages.getString("lethenon.button.show.balance", "Show the balance"),
			event -> onShow(),
			LethenonMessages.getString("lethenon.tooltip.show.balance",
				"replay the chain and read what this wallet holds in it, asking nobody"))),
			ToolForm.BUTTON_ROW);
		add(new JLabel(LethenonMessages.getString("lethenon.label.balance", "Balance:")),
			"aligny top");
		add(ToolForm.scrolled(txtReport), ToolForm.GROWING);
		add(lblResult, ToolForm.RESULT_LINE);

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
	 * Reads the balance. The password is used once: the support class wipes the array it gets, and
	 * the field and the model are cleared afterwards, whether the wallet opened or not.
	 */
	void onShow()
	{
		String failure = LethenonMessages.getString("lethenon.result.balance.failed",
			"the balance could not be read");
		try
		{
			WalletBalance balance = WalletBalanceSupport.balance(
				Path.of(modelObject.getChainFile().trim()),
				Path.of(modelObject.getWalletFile().trim()), modelObject.getPassword());
			setReport(describe(balance));
			setResult(LethenonMessages.getString("lethenon.result.balance.read",
				"the balance was read from the replayed chain"));
		}
		catch (ChainRejected | IllegalArgumentException refused)
		{
			setReport("");
			setResult(failure + ": " + refused.getMessage());
		}
		catch (IOException unreadable)
		{
			setReport("");
			setResult(failure + ": " + unreadable.getMessage());
		}
		finally
		{
			modelObject.setPassword(null);
			txtPassword.setText("");
		}
	}

	private String describe(final WalletBalance balance)
	{
		StringBuilder text = new StringBuilder();
		text.append(
			LethenonMessages.getString("lethenon.balance.address", "address (publish this):"))
			.append(' ').append(balance.address()).append('\n');
		for (AccountBalance account : balance.accounts())
		{
			text.append(account.suite()).append(' ')
				.append(LethenonMessages.getString("lethenon.balance.account", "account"))
				.append(' ').append(account.account()).append(": ").append(account.spendable())
				.append(" LETH, ")
				.append(LethenonMessages.getString("lethenon.balance.spendable", "spendable"))
				.append('\n');
		}
		text.append(balance.oneTimePayments()).append(' ')
			.append(LethenonMessages.getString("lethenon.balance.one.time",
				"one-time payments holding"))
			.append(' ').append(balance.oneTimeAmount()).append(" LETH, ")
			.append(LethenonMessages.getString("lethenon.balance.one.time.spendable",
				"spendable after a sweep, which shows those destinations and the account together "
					+ "on the chain"))
			.append('\n').append(balance.replaySummary());
		return text.toString();
	}

	private void bindToTheModel()
	{
		txtChainFile
			.setPropertyModel(LambdaModel.of(modelObject::getChainFile, modelObject::setChainFile));
		txtWalletFile.setPropertyModel(
			LambdaModel.of(modelObject::getWalletFile, modelObject::setWalletFile));
		txtPassword
			.setPropertyModel(LambdaModel.of(modelObject::getPassword, modelObject::setPassword));
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
