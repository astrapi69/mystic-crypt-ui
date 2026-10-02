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
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JFileChooser;
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
 */
public class LethenonChainPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** A field that shares its cell with the button that fills it from a file chooser */
	private static final String WITH_BUTTON = "growx, split 2";

	/** Something that keeps its own width instead of growing with the cell */
	private static final String OWN_WIDTH = "alignx left, width pref!";

	private final LethenonChainPanelModel modelObject = new LethenonChainPanelModel();

	private final JMTextField txtChainFile = new JMTextField(34);

	private final JMTextArea txtReport = new JMTextArea(8, 62);

	private final JLabel lblResult = new JLabel(" ");

	/**
	 * Instantiates a new {@link LethenonChainPanel}, prefilled with the chain file from the
	 * plugin's settings
	 */
	public LethenonChainPanel()
	{
		super(ToolForm.newLayout());
		txtChainFile.setName("txtChainFile");
		txtReport.setName("txtReport");
		txtReport.setEditable(false);
		txtReport.setLineWrap(true);
		txtReport.setWrapStyleWord(true);
		txtReport.setFont(new Font("monospaced", Font.PLAIN, 12));
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));

		txtChainFile.setToolTipText(LethenonMessages.getString("lethenon.tooltip.chain.file",
			"the chain file to replay, written by lethenon's own command line"));
		txtReport.setToolTipText(LethenonMessages.getString("lethenon.tooltip.report",
			"what the replay checked, and what it refused"));

		bindToTheModel();

		add(new JLabel(LethenonMessages.getString("lethenon.label.chain.file", "Chain file:")));
		add(txtChainFile, WITH_BUTTON);
		add(button("btnBrowseChainFile", "...", event -> onBrowseChainFile(), LethenonMessages
			.getString("lethenon.tooltip.browse.chain.file", "pick the chain file")), OWN_WIDTH);
		add(ToolForm.buttons(button("btnVerify",
			LethenonMessages.getString("lethenon.button.verify", "Verify the chain"),
			event -> onVerify(),
			LethenonMessages.getString("lethenon.tooltip.verify", "replay the chain from its "
				+ "genesis block: every signature, every state transition, every block hash"))),
			ToolForm.BUTTON_ROW);
		add(new JLabel(LethenonMessages.getString("lethenon.label.report", "Report:")),
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
	public LethenonChainPanelModel getModelObject()
	{
		return modelObject;
	}

	/**
	 * Replays the named file and shows what came back. A refused chain is not an error of this
	 * panel: the reason goes into the result line, and the report area says nothing it cannot back
	 * up.
	 */
	void onVerify()
	{
		try
		{
			ChainReplayReport report = ChainReplaySupport
				.verify(Path.of(modelObject.getChainFile().trim()));
			setReport(report.summary());
			setResult(LethenonMessages.getString("lethenon.result.accepted",
				"the chain was accepted"));
		}
		catch (ChainRejected | IllegalArgumentException refused)
		{
			setReport("");
			setResult(LethenonMessages.getString("lethenon.result.refused", "the chain was refused")
				+ ": " + refused.getMessage());
		}
		catch (IOException unreadable)
		{
			setReport("");
			setResult(LethenonMessages.getString("lethenon.result.refused", "the chain was refused")
				+ ": " + modelObject.getChainFile() + " cannot be read - "
				+ unreadable.getMessage());
		}
	}

	private void bindToTheModel()
	{
		txtChainFile.setPropertyModel(
			LambdaModel.of(modelObject::getChainFile, modelObject::setChainFile));
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

	private void onBrowseChainFile()
	{
		chooseFile(modelObject.getChainFile()).ifPresent(txtChainFile::setText);
	}

	private Optional<String> chooseFile(final String current)
	{
		JFileChooser chooser = new JFileChooser();
		if (current != null && !current.isBlank())
		{
			File chosenBefore = new File(current);
			chooser.setCurrentDirectory(
				chosenBefore.isDirectory() ? chosenBefore : chosenBefore.getParentFile());
		}
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return Optional.empty();
		}
		return Optional.of(chooser.getSelectedFile().getAbsolutePath());
	}

	private static JButton button(final String name, final String text,
		final ActionListener listener, final String tooltip)
	{
		JButton button = new JButton(text);
		button.setName(name);
		button.addActionListener(listener);
		button.setToolTipText(tooltip);
		return button;
	}
}
