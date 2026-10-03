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
 * Mines a pun (lethenon#2, milestone 5): the next block of the chain file, carrying the transfers
 * that wait next to it and paying the wallet's Ed25519 account - what lethenon's {@code mine} does
 * on the command line.
 * <p>
 * {@link MiningSupport} mines and replays the extended chain before it writes anything. Mining runs
 * on the event dispatch thread, as the other tools of this plugin do, so the window does not answer
 * while it mines: at the minimum difficulty that is a fraction of a second, every further bit of
 * difficulty doubles the expected attempts, and the attempts are capped at the command line's
 * default. The password is used once: wiped by the support class, cleared from the field
 * afterwards, whether a block was mined or not.
 */
public class LethenonMinePanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** As many variations of the pun as lethenon's command line tries by default */
	static final long ATTEMPTS = 10_000_000L;

	/** A field that shares its cell with the button that fills it from a file chooser */
	private static final String WITH_BUTTON = "growx, split 2";

	/** Something that keeps its own width instead of growing with the cell */
	private static final String OWN_WIDTH = "alignx left, width pref!";

	private final LethenonMinePanelModel modelObject = new LethenonMinePanelModel();

	private final JMTextField txtChainFile = new JMTextField(34);

	private final JMTextField txtWalletFile = new JMTextField(34);

	private final JMPasswordField txtPassword = new JMPasswordField(34);

	private final JMTextField txtPun = new JMTextField(34);

	private final JMTextArea txtReport = new JMTextArea(4, 62);

	private final JLabel lblResult = new JLabel(" ");

	/**
	 * Instantiates a new {@link LethenonMinePanel}, prefilled with the chain file from the plugin's
	 * settings and lethenon's default pun
	 */
	public LethenonMinePanel()
	{
		super(ToolForm.newLayout());
		nameTheComponents();
		explainTheComponents();
		bindToTheModel();
		layOut();
		txtChainFile.setText(LethenonSettingsContribution.chainFile());
		txtPun.setText(modelObject.getPun());
	}

	/**
	 * Gets the panel's state, which is what a test reads instead of the widgets
	 *
	 * @return the model object every component of this panel is bound to
	 */
	public LethenonMinePanelModel getModelObject()
	{
		return modelObject;
	}

	/**
	 * Mines the next block. The password is used once: the support class wipes the array it gets,
	 * and the field and the model are cleared afterwards, whether a block was mined or not.
	 */
	void onMine()
	{
		String failure = LethenonMessages.getString("lethenon.result.mine.failed",
			"no block was mined");
		try
		{
			MinedBlock mined = MiningSupport.mine(
				new MiningOrder(Path.of(modelObject.getChainFile().trim()),
					Path.of(modelObject.getWalletFile().trim()), modelObject.getPun(), ATTEMPTS),
				modelObject.getPassword(), System.currentTimeMillis());
			setReport(describe(mined));
			setResult(LethenonMessages.getString("lethenon.result.mine.written",
				"the block was mined and written to the chain file"));
		}
		catch (ChainRejected | IllegalArgumentException | IllegalStateException refused)
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

	private static String describe(final MinedBlock mined)
	{
		return LethenonMessages.getString("lethenon.mine.mined", "mined block") + " "
			+ mined.height() + " " + LethenonMessages.getString("lethenon.mine.with", "with") + " "
			+ mined.transfers() + " "
			+ LethenonMessages.getString("lethenon.mine.transfers", "transfer(s), paying") + " "
			+ mined.beneficiary() + ": \"" + mined.pun() + "\"\n" + mined.replaySummary();
	}

	private void nameTheComponents()
	{
		txtChainFile.setName("txtChainFile");
		txtWalletFile.setName("txtWalletFile");
		txtPassword.setName("txtPassword");
		txtPun.setName("txtPun");
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
		txtChainFile.setToolTipText(LethenonMessages.getString("lethenon.tooltip.mine.chain.file",
			"the chain file the block is added to; where there is none yet, the block is its genesis"));
		txtWalletFile.setToolTipText(LethenonMessages.getString("lethenon.tooltip.mine.wallet.file",
			"the wallet whose Ed25519 account the block pays"));
		txtPassword.setToolTipText(LethenonMessages.getString("lethenon.tooltip.wallet.password",
			"the wallet file's password; it is used once and then cleared"));
		txtPun.setToolTipText(LethenonMessages.getString("lethenon.tooltip.mine.pun",
			"the words mining starts from; a counter is appended until the block meets the difficulty"));
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
		add(new JLabel(LethenonMessages.getString("lethenon.label.mine.pun", "Pun:")));
		add(txtPun, ToolForm.FIELD);
		add(ToolForm.buttons(LethenonSwing.button("btnMine",
			LethenonMessages.getString("lethenon.button.mine", "Mine the pun"), event -> onMine(),
			LethenonMessages.getString("lethenon.tooltip.mine",
				"mine the next block with this pun and write it to the chain file"))),
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
		txtPun.setPropertyModel(LambdaModel.of(modelObject::getPun, modelObject::setPun));
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
