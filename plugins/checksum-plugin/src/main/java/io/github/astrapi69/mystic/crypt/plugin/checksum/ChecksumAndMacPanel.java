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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.*;

/**
 * Tool panel for the two questions about integrity that are not the same question.
 * <p>
 * A checksum says whether something arrived unchanged, and anyone can compute one - including
 * whoever changed it. A message authentication code takes a key, so only someone holding that key
 * can produce one that checks out. Each has its own tab, both work on typed text or on a file, and
 * both can check a value that was pasted in rather than only produce one.
 */
public class ChecksumAndMacPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final JComboBox<String> cmbDigest = new JComboBox<>(
		ChecksumSupport.DIGESTS.toArray(new String[0]));
	private final JTextArea txtChecksumText = new JTextArea(4, 52);
	private final JTextField txtChecksumFile = new JTextField(38);
	private final JCheckBox chkChecksumUseFile = new JCheckBox("use the file instead of the text");
	private final JTextArea txtChecksum = new JTextArea(2, 52);
	private final JTextField txtExpected = new JTextField(52);

	private final JComboBox<String> cmbMac = new JComboBox<>(
		ChecksumSupport.MACS.toArray(new String[0]));
	private final JTextArea txtMacText = new JTextArea(4, 52);
	private final JTextField txtMacFile = new JTextField(38);
	private final JCheckBox chkMacUseFile = new JCheckBox("use the file instead of the text");
	private final JPasswordField pwdMacKey = new JPasswordField(24);
	private final JTextArea txtMac = new JTextArea(2, 52);
	private final JTextField txtMacExpected = new JTextField(52);

	private final JLabel lblResult = new JLabel(" ");
	private final JTabbedPane tabs = new JTabbedPane();

	public ChecksumAndMacPanel()
	{
		super(new BorderLayout(4, 4));

		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
		tabs.setName("tabChecksum");
		tabs.addTab("Checksum", newChecksumTab());
		tabs.addTab("Message authentication code", newMacTab());
		cmbDigest.setSelectedItem(ChecksumSettingsContribution.digest());

		add(tabs, BorderLayout.CENTER);
		add(lblResult, BorderLayout.SOUTH);
	}

	private JPanel newChecksumTab()
	{
		cmbDigest.setName("cmbDigest");
		configure(txtChecksumText, "txtChecksumText");
		txtChecksumFile.setName("txtChecksumFile");
		chkChecksumUseFile.setName("chkChecksumUseFile");
		configureReadOnly(txtChecksum, "txtChecksum");
		txtExpected.setName("txtExpected");

		JPanel panel = new JPanel(new GridBagLayout());
		int row = 0;
		panel.add(new JLabel("Digest:"), at(0, row, GridBagConstraints.EAST));
		panel.add(cmbDigest, at(1, row++, GridBagConstraints.WEST));
		panel.add(new JLabel("Text:"), at(0, row, GridBagConstraints.NORTHEAST));
		panel.add(new JScrollPane(txtChecksumText), at(1, row++, GridBagConstraints.WEST));
		panel.add(new JLabel("File:"), at(0, row, GridBagConstraints.EAST));
		panel.add(txtChecksumFile, at(1, row, GridBagConstraints.WEST));
		panel.add(button("btnBrowseChecksumFile", "...", event -> onBrowse(txtChecksumFile)),
			at(2, row++, GridBagConstraints.WEST));
		panel.add(chkChecksumUseFile, at(1, row++, GridBagConstraints.WEST));
		panel.add(new JLabel("Checksum:"), at(0, row, GridBagConstraints.NORTHEAST));
		panel.add(new JScrollPane(txtChecksum), at(1, row++, GridBagConstraints.WEST));
		panel.add(new JLabel("Compare with:"), at(0, row, GridBagConstraints.EAST));
		panel.add(txtExpected, at(1, row++, GridBagConstraints.WEST));
		panel.add(buttonRow(button("btnChecksum", "Compute", event -> onChecksum()),
			button("btnCompare", "Compare", event -> onCompare())),
			at(1, row, GridBagConstraints.WEST));
		return panel;
	}

	private JPanel newMacTab()
	{
		cmbMac.setName("cmbMac");
		configure(txtMacText, "txtMacText");
		txtMacFile.setName("txtMacFile");
		chkMacUseFile.setName("chkMacUseFile");
		pwdMacKey.setName("pwdMacKey");
		configureReadOnly(txtMac, "txtMac");
		txtMacExpected.setName("txtMacExpected");

		JPanel panel = new JPanel(new GridBagLayout());
		int row = 0;
		panel.add(new JLabel("Code:"), at(0, row, GridBagConstraints.EAST));
		panel.add(cmbMac, at(1, row++, GridBagConstraints.WEST));
		panel.add(new JLabel("Key:"), at(0, row, GridBagConstraints.EAST));
		panel.add(pwdMacKey, at(1, row++, GridBagConstraints.WEST));
		panel.add(new JLabel("Text:"), at(0, row, GridBagConstraints.NORTHEAST));
		panel.add(new JScrollPane(txtMacText), at(1, row++, GridBagConstraints.WEST));
		panel.add(new JLabel("File:"), at(0, row, GridBagConstraints.EAST));
		panel.add(txtMacFile, at(1, row, GridBagConstraints.WEST));
		panel.add(button("btnBrowseMacFile", "...", event -> onBrowse(txtMacFile)),
			at(2, row++, GridBagConstraints.WEST));
		panel.add(chkMacUseFile, at(1, row++, GridBagConstraints.WEST));
		panel.add(new JLabel("Code:"), at(0, row, GridBagConstraints.NORTHEAST));
		panel.add(new JScrollPane(txtMac), at(1, row++, GridBagConstraints.WEST));
		panel.add(new JLabel("Compare with:"), at(0, row, GridBagConstraints.EAST));
		panel.add(txtMacExpected, at(1, row++, GridBagConstraints.WEST));
		panel.add(buttonRow(button("btnMac", "Compute", event -> onMac()),
			button("btnCompareMac", "Compare", event -> onCompareMac())),
			at(1, row, GridBagConstraints.WEST));
		return panel;
	}

	private void onChecksum()
	{
		run("computed", () -> {
			String digest = String.valueOf(cmbDigest.getSelectedItem());
			String checksum = chkChecksumUseFile.isSelected()
				? ChecksumSupport.checksumOfFile(new File(txtChecksumFile.getText().trim()), digest)
				: ChecksumSupport.checksumOfText(txtChecksumText.getText(), digest);
			txtChecksum.setText(checksum);
			txtChecksum.setCaretPosition(0);
			return digest + " over " + (chkChecksumUseFile.isSelected() ? "the file" : "the text");
		});
	}

	private void onCompare()
	{
		String expected = txtExpected.getText();
		String suggestion = ChecksumSupport.digestByLength(expected);
		if (suggestion != null && !suggestion.equals(cmbDigest.getSelectedItem()))
		{
			// a value pasted from a download page says by its length which digest made it
			cmbDigest.setSelectedItem(suggestion);
		}
		onChecksum();
		if (txtChecksum.getText().isBlank())
		{
			return;
		}
		lblResult.setText(ChecksumSupport.matches(expected, txtChecksum.getText())
			? "the checksums are the same"
			: "the checksums are NOT the same");
	}

	private void onMac()
	{
		run("computed", () -> {
			String algorithm = String.valueOf(cmbMac.getSelectedItem());
			String key = new String(pwdMacKey.getPassword());
			String code = chkMacUseFile.isSelected()
				? ChecksumSupport.macOfFile(new File(txtMacFile.getText().trim()), key, algorithm)
				: ChecksumSupport.macOfText(txtMacText.getText(), key, algorithm);
			txtMac.setText(code);
			txtMac.setCaretPosition(0);
			return algorithm + " over " + (chkMacUseFile.isSelected() ? "the file" : "the text");
		});
	}

	private void onCompareMac()
	{
		onMac();
		if (txtMac.getText().isBlank())
		{
			return;
		}
		lblResult.setText(ChecksumSupport.matches(txtMacExpected.getText(), txtMac.getText())
			? "the codes are the same"
			: "the codes are NOT the same");
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
			lblResult.setText(operation.execute());
		}
		catch (Exception exception)
		{
			lblResult.setText("not " + what + ": " + message(exception));
		}
	}

	private void onBrowse(JTextField target)
	{
		JFileChooser fileChooser = new JFileChooser();
		if (!target.getText().isBlank())
		{
			fileChooser.setSelectedFile(new File(target.getText()));
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
}
