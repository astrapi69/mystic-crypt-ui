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
package io.github.astrapi69.mystic.crypt.plugin.checksum;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.*;

import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.swing.model.component.JMCheckBox;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMPasswordField;
import io.github.astrapi69.swing.model.component.JMTextArea;
import io.github.astrapi69.swing.model.component.JMTextField;

/**
 * Tool panel for the two questions about integrity that are not the same question.
 * <p>
 * A checksum says whether something arrived unchanged, and anyone can compute one - including
 * whoever changed it. A message authentication code takes a key, so only someone holding that key
 * can produce one that checks out. Each has its own tab, both work on typed text or on a file, and
 * both can check a value that was pasted in rather than only produce one.
 * <p>
 * Every component is bound to {@link ChecksumAndMacPanelModel}, so a button reads what the user
 * entered from the model instead of out of the widgets.
 */
public class ChecksumAndMacPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/**
	 * The width in pixels a text component of this panel is never laid out below, so that even the
	 * minimum layout of a narrow window shows a usable field instead of a sliver
	 */
	private static final int MINIMUM_TEXT_WIDTH = 160;

	/** Everything this panel holds; every component below writes into it */
	private final ChecksumAndMacPanelModel modelObject = new ChecksumAndMacPanelModel();

	private final JMComboBox<String, ComboBoxModel<String>> cmbDigest = new JMComboBox<>(
		ChecksumSupport.DIGESTS.toArray(new String[0]));
	private final JMTextArea txtChecksumText = new JMTextArea(4, 52);
	private final JMTextField txtChecksumFile = new JMTextField(38);
	private final JMCheckBox chkChecksumUseFile = new JMCheckBox(
		"use the file instead of the text");
	private final JMTextArea txtChecksum = new JMTextArea(2, 52);
	private final JMTextField txtExpected = new JMTextField(52);

	private final JMComboBox<String, ComboBoxModel<String>> cmbMac = new JMComboBox<>(
		ChecksumSupport.MACS.toArray(new String[0]));
	private final JMTextArea txtMacText = new JMTextArea(4, 52);
	private final JMTextField txtMacFile = new JMTextField(38);
	private final JMCheckBox chkMacUseFile = new JMCheckBox("use the file instead of the text");
	private final JMPasswordField pwdMacKey = new JMPasswordField(24);
	private final JMTextArea txtMac = new JMTextArea(2, 52);
	private final JMTextField txtMacExpected = new JMTextField(52);

	private final JLabel lblResult = new JLabel();
	private final JTabbedPane tabs = new JTabbedPane();

	public ChecksumAndMacPanel()
	{
		super(new BorderLayout(4, 4));

		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
		showResult(modelObject.getResultMessage());
		tabs.setName("tabChecksum");
		// the model carries what the tool starts with into the two combo boxes when they bind
		modelObject.setDigest(ChecksumSettingsContribution.digest());
		modelObject.setMacAlgorithm(ChecksumSupport.MACS.get(0));
		tabs.addTab("Checksum", newChecksumTab());
		tabs.addTab("Message authentication code", newMacTab());

		add(tabs, BorderLayout.CENTER);
		add(lblResult, BorderLayout.SOUTH);
	}

	private JPanel newChecksumTab()
	{
		cmbDigest.setName("cmbDigest");
		configure(txtChecksumText, "txtChecksumText");
		configure(txtChecksumFile, "txtChecksumFile");
		chkChecksumUseFile.setName("chkChecksumUseFile");
		configureReadOnly(txtChecksum, "txtChecksum");
		configure(txtExpected, "txtExpected");
		bindChecksumComponents();

		JPanel panel = new JPanel(new GridBagLayout());
		int row = 0;
		panel.add(new JLabel("Digest:"), at(0, row, GridBagConstraints.EAST));
		panel.add(cmbDigest, stretched(1, row++));
		panel.add(new JLabel("Text:"), at(0, row, GridBagConstraints.NORTHEAST));
		panel.add(scrollPaneAround(txtChecksumText), stretched(1, row++));
		panel.add(new JLabel("File:"), at(0, row, GridBagConstraints.EAST));
		panel.add(txtChecksumFile, stretched(1, row));
		panel.add(button("btnBrowseChecksumFile", "...",
			event -> onBrowse(txtChecksumFile, modelObject.getChecksumFile())),
			at(2, row++, GridBagConstraints.WEST));
		panel.add(chkChecksumUseFile, at(1, row++, GridBagConstraints.WEST));
		panel.add(new JLabel("Checksum:"), at(0, row, GridBagConstraints.NORTHEAST));
		panel.add(scrollPaneAround(txtChecksum), stretched(1, row++));
		panel.add(new JLabel("Compare with:"), at(0, row, GridBagConstraints.EAST));
		panel.add(txtExpected, stretched(1, row++));
		panel.add(buttonRow(button("btnChecksum", "Compute", event -> onChecksum()),
			button("btnCompare", "Compare", event -> onCompare())),
			at(1, row, GridBagConstraints.WEST));
		return panel;
	}

	private void bindChecksumComponents()
	{
		cmbDigest.setPropertyModel(LambdaModel.of(modelObject::getDigest, modelObject::setDigest));
		txtChecksumText.setPropertyModel(
			LambdaModel.of(modelObject::getChecksumText, modelObject::setChecksumText));
		txtChecksumFile.setPropertyModel(
			LambdaModel.of(modelObject::getChecksumFile, modelObject::setChecksumFile));
		chkChecksumUseFile.setPropertyModel(
			LambdaModel.of(modelObject::isChecksumOverFile, modelObject::setChecksumOverFile));
		txtChecksum
			.setPropertyModel(LambdaModel.of(modelObject::getChecksum, modelObject::setChecksum));
		txtExpected.setPropertyModel(
			LambdaModel.of(modelObject::getExpectedChecksum, modelObject::setExpectedChecksum));
	}

	private JPanel newMacTab()
	{
		cmbMac.setName("cmbMac");
		configure(txtMacText, "txtMacText");
		configure(txtMacFile, "txtMacFile");
		chkMacUseFile.setName("chkMacUseFile");
		configure(pwdMacKey, "pwdMacKey");
		configureReadOnly(txtMac, "txtMac");
		configure(txtMacExpected, "txtMacExpected");
		bindMacComponents();

		JPanel panel = new JPanel(new GridBagLayout());
		int row = 0;
		panel.add(new JLabel("Code:"), at(0, row, GridBagConstraints.EAST));
		panel.add(cmbMac, stretched(1, row++));
		panel.add(new JLabel("Key:"), at(0, row, GridBagConstraints.EAST));
		panel.add(pwdMacKey, stretched(1, row++));
		panel.add(new JLabel("Text:"), at(0, row, GridBagConstraints.NORTHEAST));
		panel.add(scrollPaneAround(txtMacText), stretched(1, row++));
		panel.add(new JLabel("File:"), at(0, row, GridBagConstraints.EAST));
		panel.add(txtMacFile, stretched(1, row));
		panel.add(
			button("btnBrowseMacFile", "...",
				event -> onBrowse(txtMacFile, modelObject.getMacFile())),
			at(2, row++, GridBagConstraints.WEST));
		panel.add(chkMacUseFile, at(1, row++, GridBagConstraints.WEST));
		panel.add(new JLabel("Code:"), at(0, row, GridBagConstraints.NORTHEAST));
		panel.add(scrollPaneAround(txtMac), stretched(1, row++));
		panel.add(new JLabel("Compare with:"), at(0, row, GridBagConstraints.EAST));
		panel.add(txtMacExpected, stretched(1, row++));
		panel.add(buttonRow(button("btnMac", "Compute", event -> onMac()),
			button("btnCompareMac", "Compare", event -> onCompareMac())),
			at(1, row, GridBagConstraints.WEST));
		return panel;
	}

	private void bindMacComponents()
	{
		cmbMac.setPropertyModel(
			LambdaModel.of(modelObject::getMacAlgorithm, modelObject::setMacAlgorithm));
		pwdMacKey.setPropertyModel(LambdaModel.of(modelObject::getMacKey, modelObject::setMacKey));
		txtMacText
			.setPropertyModel(LambdaModel.of(modelObject::getMacText, modelObject::setMacText));
		txtMacFile
			.setPropertyModel(LambdaModel.of(modelObject::getMacFile, modelObject::setMacFile));
		chkMacUseFile.setPropertyModel(
			LambdaModel.of(modelObject::isMacOverFile, modelObject::setMacOverFile));
		txtMac.setPropertyModel(LambdaModel.of(modelObject::getMac, modelObject::setMac));
		txtMacExpected.setPropertyModel(
			LambdaModel.of(modelObject::getExpectedMac, modelObject::setExpectedMac));
	}

	private void onChecksum()
	{
		run("computed", () -> {
			String digest = modelObject.getDigest();
			String checksum = modelObject.isChecksumOverFile()
				? ChecksumSupport.checksumOfFile(new File(modelObject.getChecksumFile().trim()),
					digest)
				: ChecksumSupport.checksumOfText(modelObject.getChecksumText(), digest);
			txtChecksum.setText(checksum);
			txtChecksum.setCaretPosition(0);
			return digest + " over " + (modelObject.isChecksumOverFile() ? "the file" : "the text");
		});
	}

	private void onCompare()
	{
		String expected = modelObject.getExpectedChecksum();
		String suggestion = ChecksumSupport.digestByLength(expected);
		if (suggestion != null && !suggestion.equals(modelObject.getDigest()))
		{
			// a value pasted from a download page says by its length which digest made it
			cmbDigest.setSelectedItem(suggestion);
		}
		onChecksum();
		if (modelObject.getChecksum().isBlank())
		{
			return;
		}
		showResult(ChecksumSupport.matches(expected, modelObject.getChecksum())
			? "the checksums are the same"
			: "the checksums are NOT the same");
	}

	private void onMac()
	{
		run("computed", () -> {
			String algorithm = modelObject.getMacAlgorithm();
			String key = new String(modelObject.getMacKey());
			String code = modelObject.isMacOverFile()
				? ChecksumSupport.macOfFile(new File(modelObject.getMacFile().trim()), key,
					algorithm)
				: ChecksumSupport.macOfText(modelObject.getMacText(), key, algorithm);
			txtMac.setText(code);
			txtMac.setCaretPosition(0);
			return algorithm + " over " + (modelObject.isMacOverFile() ? "the file" : "the text");
		});
	}

	private void onCompareMac()
	{
		onMac();
		if (modelObject.getMac().isBlank())
		{
			return;
		}
		showResult(ChecksumSupport.matches(modelObject.getExpectedMac(), modelObject.getMac())
			? "the codes are the same"
			: "the codes are NOT the same");
	}

	/**
	 * The state of this panel: what is entered in both tabs, what was computed and what the panel
	 * last said about it
	 *
	 * @return the model every component of this panel is bound to
	 */
	public ChecksumAndMacPanelModel getModelObject()
	{
		return modelObject;
	}

	/** The message shown below the tabs */
	public String getResultText()
	{
		return lblResult.getText();
	}

	private void run(String what, ChecksumOperation operation)
	{
		try
		{
			showResult(operation.execute());
		}
		catch (Exception exception)
		{
			showResult("not " + what + ": " + message(exception));
		}
	}

	/**
	 * Keeps the message in the model and shows it below the tabs
	 *
	 * @param resultMessage
	 *            what the panel has to say about what it did
	 */
	private void showResult(String resultMessage)
	{
		modelObject.setResultMessage(resultMessage);
		lblResult.setText(modelObject.getResultMessage());
	}

	/**
	 * Lets the user pick a file, starting at the path the model holds, and writes the chosen path
	 * into the field - which is what carries it back into the model
	 *
	 * @param target
	 *            the field the chosen path is written into
	 * @param currentPath
	 *            the path the model holds for that field
	 */
	private void onBrowse(JTextField target, String currentPath)
	{
		JFileChooser fileChooser = new JFileChooser();
		if (!currentPath.isBlank())
		{
			fileChooser.setSelectedFile(new File(currentPath));
		}
		if (fileChooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION)
		{
			target.setText(fileChooser.getSelectedFile().getAbsolutePath());
		}
	}

	private static String message(Exception exception)
	{
		return exception.getMessage() != null
			? exception.getMessage()
			: exception.getClass().getSimpleName();
	}

	@FunctionalInterface
	private interface ChecksumOperation
	{
		String execute() throws Exception;
	}

	private static void configure(JTextArea textArea, String name)
	{
		textArea.setName(name);
		textArea.setLineWrap(true);
	}

	private static void configureReadOnly(JTextArea textArea, String name)
	{
		configure(textArea, name);
		textArea.setEditable(false);
	}

	/**
	 * Names a text field and gives it the width it is never laid out below - a text field reports a
	 * minimum width of nearly zero, which lets a narrow window collapse it to a sliver
	 *
	 * @param textField
	 *            the field to configure, a password field included
	 * @param name
	 *            the name the field is looked up by
	 */
	private static void configure(JTextField textField, String name)
	{
		textField.setName(name);
		textField.setMinimumSize(
			new Dimension(MINIMUM_TEXT_WIDTH, textField.getPreferredSize().height));
	}

	/**
	 * Wraps a text area in the scroll pane the panel shows it in, with the width that scroll pane is
	 * never laid out below - the area itself takes whatever the viewport gives it, so the minimum
	 * belongs on the scroll pane
	 *
	 * @param textArea
	 *            the text area to wrap
	 * @return the scroll pane around the text area
	 */
	private static JScrollPane scrollPaneAround(JTextArea textArea)
	{
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setMinimumSize(
			new Dimension(MINIMUM_TEXT_WIDTH, scrollPane.getPreferredSize().height));
		return scrollPane;
	}

	private static JButton button(String name, String text, java.awt.event.ActionListener listener)
	{
		JButton button = new JButton(text);
		button.setName(name);
		button.addActionListener(listener);
		return button;
	}

	private static JPanel buttonRow(JButton... buttons)
	{
		JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
		for (JButton button : buttons)
		{
			panel.add(button);
		}
		return panel;
	}

	private static GridBagConstraints at(int column, int row, int anchor)
	{
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = column;
		constraints.gridy = row;
		constraints.anchor = anchor;
		constraints.insets = new Insets(4, 4, 4, 4);
		return constraints;
	}

	/**
	 * Constraints for the column that holds the inputs: that column takes the width the labels and
	 * the buttons leave over, and the input in it is stretched across it. Without this the grid
	 * falls back to the minimum widths as soon as the window is narrower than it wants, and the
	 * inputs vanish instead of getting smaller.
	 *
	 * @param column
	 *            the column of the grid the input sits in
	 * @param row
	 *            the row of the grid the input sits in
	 * @return the constraints for an input component
	 */
	private static GridBagConstraints stretched(int column, int row)
	{
		GridBagConstraints constraints = at(column, row, GridBagConstraints.WEST);
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1.0;
		return constraints;
	}
}
