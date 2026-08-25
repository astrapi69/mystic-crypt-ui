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
package io.github.astrapi69.mystic.crypt.plugin.filecrypt;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.*;

/**
 * Tool panel for encrypting and decrypting: a file on one tab, a piece of text on the other.
 * <p>
 * The passphrase is what protects the result, so it is asked for twice when something is being
 * encrypted - a typo in a passphrase that nothing checks means the content is gone. On decrypting
 * one field is enough: a wrong passphrase is refused by the cipher itself.
 */
public class FileCryptPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final JTextField txtSourceFile = new JTextField(38);
	private final JTextField txtTargetFile = new JTextField(38);
	private final JPasswordField pwdFile = new JPasswordField(20);
	private final JPasswordField pwdFileRepeated = new JPasswordField(20);
	private final JTextArea txtPlainText = new JTextArea(6, 52);
	private final JTextArea txtEncryptedText = new JTextArea(6, 52);
	private final JPasswordField pwdText = new JPasswordField(20);
	private final JPasswordField pwdTextRepeated = new JPasswordField(20);
	private final JLabel lblResult = new JLabel(" ");
	private final JTabbedPane tabs = new JTabbedPane();

	public FileCryptPanel()
	{
		super(new BorderLayout(4, 4));

		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
		tabs.setName("tabFileCrypt");
		tabs.addTab("File", newFileTab());
		tabs.addTab("Text", newTextTab());
		// the tool starts on the tab the user configured in the settings dialog
		tabs.setSelectedIndex("text".equals(FileCryptSettingsContribution.startTab()) ? 1 : 0);

		add(tabs, BorderLayout.CENTER);
		add(lblResult, BorderLayout.SOUTH);
	}

	private JPanel newFileTab()
	{
		txtSourceFile.setName("txtSourceFile");
		txtTargetFile.setName("txtTargetFile");
		pwdFile.setName("pwdFile");
		pwdFileRepeated.setName("pwdFileRepeated");

		JPanel panel = new JPanel(new GridBagLayout());
		int row = 0;
		panel.add(new JLabel("File:"), at(0, row, GridBagConstraints.EAST));
		panel.add(txtSourceFile, at(1, row, GridBagConstraints.WEST));
		panel.add(button("btnBrowseSource", "...", event -> onBrowse(txtSourceFile)),
			at(2, row++, GridBagConstraints.WEST));
		panel.add(new JLabel("Write to:"), at(0, row, GridBagConstraints.EAST));
		panel.add(txtTargetFile, at(1, row, GridBagConstraints.WEST));
		panel.add(button("btnBrowseTarget", "...", event -> onBrowse(txtTargetFile)),
			at(2, row++, GridBagConstraints.WEST));
		panel.add(new JLabel("Passphrase:"), at(0, row, GridBagConstraints.EAST));
		panel.add(pwdFile, at(1, row++, GridBagConstraints.WEST));
		panel.add(new JLabel("Repeat (to encrypt):"), at(0, row, GridBagConstraints.EAST));
		panel.add(pwdFileRepeated, at(1, row++, GridBagConstraints.WEST));
		panel.add(buttonRow(button("btnEncryptFile", "Encrypt", event -> onEncryptFile()),
			button("btnDecryptFile", "Decrypt", event -> onDecryptFile())),
			at(1, row, GridBagConstraints.WEST));
		return panel;
	}

	private JPanel newTextTab()
	{
		txtPlainText.setName("txtPlainText");
		txtPlainText.setLineWrap(true);
		txtEncryptedText.setName("txtEncryptedText");
		txtEncryptedText.setLineWrap(true);
		pwdText.setName("pwdText");
		pwdTextRepeated.setName("pwdTextRepeated");

		JPanel panel = new JPanel(new GridBagLayout());
		int row = 0;
		panel.add(new JLabel("Text:"), at(0, row, GridBagConstraints.NORTHEAST));
		panel.add(new JScrollPane(txtPlainText), at(1, row++, GridBagConstraints.WEST));
		panel.add(new JLabel("Encrypted (Base64):"), at(0, row, GridBagConstraints.NORTHEAST));
		panel.add(new JScrollPane(txtEncryptedText), at(1, row++, GridBagConstraints.WEST));
		panel.add(new JLabel("Passphrase:"), at(0, row, GridBagConstraints.EAST));
		panel.add(pwdText, at(1, row++, GridBagConstraints.WEST));
		panel.add(new JLabel("Repeat (to encrypt):"), at(0, row, GridBagConstraints.EAST));
		panel.add(pwdTextRepeated, at(1, row++, GridBagConstraints.WEST));
		panel.add(buttonRow(button("btnEncryptText", "Encrypt", event -> onEncryptText()),
			button("btnDecryptText", "Decrypt", event -> onDecryptText())),
			at(1, row, GridBagConstraints.WEST));
		return panel;
	}

	private void onEncryptFile()
	{
		run("encrypted", () -> {
			String passphrase = matchingPassphrase(pwdFile, pwdFileRepeated);
			File source = new File(txtSourceFile.getText().trim());
			File written = FileCryptSupport.encryptFile(source, targetOrNull(), passphrase);
			txtTargetFile.setText(written.getAbsolutePath());
			String message = "encrypted to " + written.getName();
			if (FileCryptSettingsContribution.deleteSourceAfterEncrypt()
				&& java.nio.file.Files.deleteIfExists(source.toPath()))
			{
				message += ", removed " + source.getName();
			}
			return message;
		});
	}

	private void onDecryptFile()
	{
		run("decrypted", () -> {
			File written = FileCryptSupport.decryptFile(new File(txtSourceFile.getText().trim()),
				targetOrNull(), new String(pwdFile.getPassword()));
			txtTargetFile.setText(written.getAbsolutePath());
			return "decrypted to " + written.getName();
		});
	}

	private void onEncryptText()
	{
		run("encrypted", () -> {
			String passphrase = matchingPassphrase(pwdText, pwdTextRepeated);
			txtEncryptedText.setText(FileCryptSupport.encryptText(txtPlainText.getText(),
				passphrase));
			txtEncryptedText.setCaretPosition(0);
			return "encrypted " + txtPlainText.getText().length() + " characters";
		});
	}

	private void onDecryptText()
	{
		run("decrypted", () -> {
			txtPlainText.setText(FileCryptSupport.decryptText(txtEncryptedText.getText(),
				new String(pwdText.getPassword())));
			txtPlainText.setCaretPosition(0);
			return "decrypted " + txtPlainText.getText().length() + " characters";
		});
	}

	/**
	 * The passphrase from the two fields, provided they agree - a typo in a passphrase that nothing
	 * checks means the content cannot be got back
	 */
	private String matchingPassphrase(JPasswordField first, JPasswordField repeated)
	{
		String passphrase = new String(first.getPassword());
		String repetition = new String(repeated.getPassword());
		if (!passphrase.equals(repetition))
		{
			throw new IllegalArgumentException("the two passphrases are not the same");
		}
		return passphrase;
	}

	private File targetOrNull()
	{
		String target = txtTargetFile.getText().trim();
		return target.isEmpty() ? null : new File(target);
	}

	private void run(String what, FileCryptOperation operation)
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

	/** The message shown below the tabs */
	public String getResultText()
	{
		return lblResult.getText();
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
		// a wrong passphrase surfaces as an exception without a message often enough that the class
		// name is the only thing left to show
		return exception.getMessage() != null
			? exception.getMessage()
			: exception.getClass().getSimpleName();
	}

	@FunctionalInterface
	private interface FileCryptOperation
	{
		String execute() throws Exception;
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
