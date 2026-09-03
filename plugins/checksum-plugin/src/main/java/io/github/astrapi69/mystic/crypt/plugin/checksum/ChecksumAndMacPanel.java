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

import java.awt.Font;
import java.io.File;

import javax.swing.*;

import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.component.JMCheckBox;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMPasswordField;
import io.github.astrapi69.swing.model.component.JMTextArea;
import io.github.astrapi69.swing.model.component.JMTextField;
import net.miginfocom.swing.MigLayout;

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

	/** A label that belongs to a text area sits at the top of it, not in the middle */
	private static final String LABEL_OF_AN_AREA = "aligny top";

	/**
	 * The area the user types into, which takes the larger share of the height a window has spare
	 */
	private static final String INPUT_AREA = "grow, pushy 200";

	/** The area a computed value appears in, which takes the smaller share */
	private static final String OUTPUT_AREA = "grow, pushy 100";

	/** Something that belongs under the field above it rather than next to a label of its own */
	private static final String UNDER_THE_FIELD = "skip 1, growx";

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
		// one layout for every tool window, so this one looks like the one next to it: labels in a
		// narrow right aligned column, fields taking the width, the tabs taking the height that is
		// left, the result line under them
		super(ToolForm.newLayout());

		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
		showResult(modelObject.getResultMessage());
		tabs.setName("tabChecksum");
		// the model carries what the tool starts with into the two combo boxes when they bind
		modelObject.setDigest(ChecksumSettingsContribution.digest());
		modelObject.setMacAlgorithm(ChecksumSupport.MACS.get(0));
		tabs.addTab("Checksum", newChecksumTab());
		tabs.addTab("Message authentication code", newMacTab());

		add(tabs, ToolForm.GROWING);
		add(lblResult, ToolForm.RESULT_LINE);
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

		cmbDigest.setToolTipText(ChecksumMessages.getString("checksum.and.mac.tooltip.digest",
			"the digest algorithm to compute the checksum with"));
		txtChecksumText.setToolTipText(ChecksumMessages
			.getString("checksum.and.mac.tooltip.checksum.text", "the text to compute the checksum of"));
		txtChecksumFile.setToolTipText(ChecksumMessages.getString(
			"checksum.and.mac.tooltip.checksum.file",
			"the file to compute the checksum of, used instead of the text below when checked"));
		chkChecksumUseFile.setToolTipText(ChecksumMessages.getString(
			"checksum.and.mac.tooltip.checksum.use.file",
			"compute over the file above instead of the typed text"));
		txtChecksum.setToolTipText(
			ChecksumMessages.getString("checksum.and.mac.tooltip.checksum", "the computed checksum"));
		txtExpected.setToolTipText(ChecksumMessages.getString(
			"checksum.and.mac.tooltip.expected.checksum",
			"paste a checksum here to compare it against the computed one"));

		JPanel panel = new JPanel(ToolForm.newLayout());
		panel.add(new JLabel("Digest:"));
		panel.add(cmbDigest, ToolForm.FIELD);
		panel.add(new JLabel("Text:"), LABEL_OF_AN_AREA);
		panel.add(ToolForm.scrolled(txtChecksumText), INPUT_AREA);
		panel.add(new JLabel("File:"));
		panel.add(fileRow(txtChecksumFile,
			button("btnBrowseChecksumFile", "...",
				event -> onBrowse(txtChecksumFile, modelObject.getChecksumFile()),
				ChecksumMessages.getString("checksum.and.mac.tooltip.browse.checksum.file",
					"choose the file to compute the checksum of"))),
			ToolForm.FIELD);
		panel.add(chkChecksumUseFile, UNDER_THE_FIELD);
		panel.add(new JLabel("Checksum:"), LABEL_OF_AN_AREA);
		panel.add(ToolForm.scrolled(txtChecksum), OUTPUT_AREA);
		panel.add(new JLabel("Compare with:"));
		panel.add(txtExpected, ToolForm.FIELD);
		panel.add(ToolForm.buttons(
			button("btnChecksum", "Compute", event -> onChecksum(),
				ChecksumMessages.getString("checksum.and.mac.tooltip.compute.checksum",
					"computes the checksum of the text or file above")),
			button("btnCompare", "Compare", event -> onCompare(),
				ChecksumMessages.getString("checksum.and.mac.tooltip.compare.checksum",
					"computes the checksum and compares it with what was pasted below - also "
						+ "picks the digest the pasted value's length implies, if different"))),
			ToolForm.BUTTON_ROW);
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

		cmbMac.setToolTipText(ChecksumMessages.getString("checksum.and.mac.tooltip.mac.algorithm",
			"the message authentication code algorithm - unlike a checksum, only someone holding "
				+ "the key can produce a matching code"));
		txtMacText.setToolTipText(ChecksumMessages.getString("checksum.and.mac.tooltip.mac.text",
			"the text to compute the code for"));
		txtMacFile.setToolTipText(ChecksumMessages.getString("checksum.and.mac.tooltip.mac.file",
			"the file to compute the code for, used instead of the text below when checked"));
		chkMacUseFile.setToolTipText(ChecksumMessages.getString(
			"checksum.and.mac.tooltip.mac.use.file",
			"compute over the file above instead of the typed text"));
		pwdMacKey.setToolTipText(ChecksumMessages.getString("checksum.and.mac.tooltip.mac.key",
			"the shared secret key the code is computed with"));
		txtMac.setToolTipText(
			ChecksumMessages.getString("checksum.and.mac.tooltip.mac", "the computed code"));
		txtMacExpected.setToolTipText(ChecksumMessages.getString(
			"checksum.and.mac.tooltip.expected.mac",
			"paste a code here to compare it against the computed one"));

		JPanel panel = new JPanel(ToolForm.newLayout());
		panel.add(new JLabel("Code:"));
		panel.add(cmbMac, ToolForm.FIELD);
		panel.add(new JLabel("Key:"));
		panel.add(pwdMacKey, ToolForm.FIELD);
		panel.add(new JLabel("Text:"), LABEL_OF_AN_AREA);
		panel.add(ToolForm.scrolled(txtMacText), INPUT_AREA);
		panel.add(new JLabel("File:"));
		panel.add(fileRow(txtMacFile,
			button("btnBrowseMacFile", "...",
				event -> onBrowse(txtMacFile, modelObject.getMacFile()),
				ChecksumMessages.getString("checksum.and.mac.tooltip.browse.mac.file",
					"choose the file to compute the code for"))),
			ToolForm.FIELD);
		panel.add(chkMacUseFile, UNDER_THE_FIELD);
		panel.add(new JLabel("Code:"), LABEL_OF_AN_AREA);
		panel.add(ToolForm.scrolled(txtMac), OUTPUT_AREA);
		panel.add(new JLabel("Compare with:"));
		panel.add(txtMacExpected, ToolForm.FIELD);
		panel.add(ToolForm.buttons(
			button("btnMac", "Compute", event -> onMac(),
				ChecksumMessages.getString("checksum.and.mac.tooltip.compute.mac",
					"computes the code of the text or file above, with the key above")),
			button("btnCompareMac", "Compare", event -> onCompareMac(),
				ChecksumMessages.getString("checksum.and.mac.tooltip.compare.mac",
					"computes the code and compares it with what was pasted below"))),
			ToolForm.BUTTON_ROW);
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
		ToolForm.sized(textField);
	}

	/**
	 * Puts a path field next to the button that fills it in, so the field still takes the width the
	 * form gives its second column while the button keeps only the width it needs
	 *
	 * @param pathField
	 *            the field that holds the path
	 * @param browseButton
	 *            the button that opens the file chooser for that field
	 * @return the row to add to the form
	 */
	private static JPanel fileRow(JTextField pathField, JButton browseButton)
	{
		JPanel row = new JPanel(new MigLayout("insets 0, gap 6", "[grow,fill][]", "[]"));
		row.setOpaque(false);
		row.add(pathField, "growx");
		row.add(browseButton);
		return row;
	}

	private static JButton button(String name, String text, java.awt.event.ActionListener listener,
		String tooltip)
	{
		JButton button = new JButton(text);
		button.setName(name);
		button.addActionListener(listener);
		button.setToolTipText(tooltip);
		return button;
	}

}
