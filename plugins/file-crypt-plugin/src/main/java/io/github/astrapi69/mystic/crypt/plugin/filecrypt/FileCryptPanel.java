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

import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.swing.model.component.JMPasswordField;
import io.github.astrapi69.swing.model.component.JMTextArea;
import io.github.astrapi69.swing.model.component.JMTextField;

/**
 * Tool panel for encrypting and decrypting: a file on one tab, a piece of text on the other.
 * <p>
 * The passphrase is what protects the result, so it is asked for twice when something is being
 * encrypted - a typo in a passphrase that nothing checks means the content is gone. On decrypting
 * one field is enough: a wrong passphrase is refused by the cipher itself.
 * <p>
 * Every component is bound to {@link FileCryptPanelModel}, so what the user entered is read from
 * the model when a button is pressed, not out of the widgets.
 */
public class FileCryptPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final FileCryptPanelModel modelObject = new FileCryptPanelModel();
	private final JMTextField txtSourceFile = new JMTextField(38);
	private final JMTextField txtTargetFile = new JMTextField(38);
	private final JMPasswordField pwdFile = new JMPasswordField(20);
	private final JMPasswordField pwdFileRepeated = new JMPasswordField(20);
	private final JMTextArea txtPlainText = new JMTextArea(6, 52);
	private final JMTextArea txtEncryptedText = new JMTextArea(6, 52);
	private final JMPasswordField pwdText = new JMPasswordField(20);
	private final JMPasswordField pwdTextRepeated = new JMPasswordField(20);
	private final JLabel lblResult = new JLabel(modelObject.getResultText());
	private final JTabbedPane tabs = new JTabbedPane();

	public FileCryptPanel()
	{
		super(new BorderLayout(4, 4));

		bindToModel();

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

	/** Binds every entry component to its field in {@link FileCryptPanelModel} */
	private void bindToModel()
	{
		txtSourceFile.setPropertyModel(sourceFileModel());
		txtTargetFile.setPropertyModel(targetFileModel());
		pwdFile.setPropertyModel(
			LambdaModel.of(modelObject::getFilePassphrase, modelObject::setFilePassphrase));
		pwdFileRepeated.setPropertyModel(LambdaModel.of(modelObject::getFilePassphraseRepeated,
			modelObject::setFilePassphraseRepeated));
		txtPlainText
			.setPropertyModel(LambdaModel.of(modelObject::getPlainText, modelObject::setPlainText));
		txtEncryptedText.setPropertyModel(
			LambdaModel.of(modelObject::getEncryptedText, modelObject::setEncryptedText));
		pwdText.setPropertyModel(
			LambdaModel.of(modelObject::getTextPassphrase, modelObject::setTextPassphrase));
		pwdTextRepeated.setPropertyModel(LambdaModel.of(modelObject::getTextPassphraseRepeated,
			modelObject::setTextPassphraseRepeated));
	}

	private IModel<String> sourceFileModel()
	{
		return LambdaModel.of(modelObject::getSourceFile, modelObject::setSourceFile);
	}

	private IModel<String> targetFileModel()
	{
		return LambdaModel.of(modelObject::getTargetFile, modelObject::setTargetFile);
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
		panel.add(
			button("btnBrowseSource", "...", event -> onBrowse(txtSourceFile, sourceFileModel())),
			at(2, row++, GridBagConstraints.WEST));
		panel.add(new JLabel("Write to:"), at(0, row, GridBagConstraints.EAST));
		panel.add(txtTargetFile, at(1, row, GridBagConstraints.WEST));
		panel.add(
			button("btnBrowseTarget", "...", event -> onBrowse(txtTargetFile, targetFileModel())),
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
			String passphrase = matchingPassphrase(modelObject.getFilePassphrase(),
				modelObject.getFilePassphraseRepeated());
			File source = new File(modelObject.getSourceFile().trim());
			File written = FileCryptSupport.encryptFile(source, targetOrNull(), passphrase);
			showTargetFile(written);
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
			File source = new File(modelObject.getSourceFile().trim());
			File written = FileCryptSupport.decryptFile(source, targetOrNull(),
				new String(modelObject.getFilePassphrase()));
			showTargetFile(written);
			return "decrypted to " + written.getName();
		});
	}

	private void onEncryptText()
	{
		run("encrypted", () -> {
			String passphrase = matchingPassphrase(modelObject.getTextPassphrase(),
				modelObject.getTextPassphraseRepeated());
			showEncryptedText(FileCryptSupport.encryptText(modelObject.getPlainText(), passphrase));
			return "encrypted " + modelObject.getPlainText().length() + " characters";
		});
	}

	private void onDecryptText()
	{
		run("decrypted", () -> {
			showPlainText(FileCryptSupport.decryptText(modelObject.getEncryptedText(),
				new String(modelObject.getTextPassphrase())));
			return "decrypted " + modelObject.getPlainText().length() + " characters";
		});
	}

	/**
	 * The passphrase from the two fields, provided they agree - a typo in a passphrase that nothing
	 * checks means the content cannot be got back
	 */
	private String matchingPassphrase(char[] entered, char[] repeated)
	{
		String passphrase = new String(entered);
		String repetition = new String(repeated);
		if (!passphrase.equals(repetition))
		{
			throw new IllegalArgumentException("the two passphrases are not the same");
		}
		return passphrase;
	}

	private File targetOrNull()
	{
		String target = modelObject.getTargetFile().trim();
		return target.isEmpty() ? null : new File(target);
	}

	private void run(String what, FileCryptOperation operation)
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

	/** Puts the file that was written into the model and shows its path in the target field */
	private void showTargetFile(File written)
	{
		modelObject.setTargetFile(written.getAbsolutePath());
		txtTargetFile.setText(modelObject.getTargetFile());
	}

	/** Puts the encrypted text into the model and shows it from its beginning */
	private void showEncryptedText(String encrypted)
	{
		modelObject.setEncryptedText(encrypted);
		txtEncryptedText.setText(modelObject.getEncryptedText());
		txtEncryptedText.setCaretPosition(0);
	}

	/** Puts the decrypted text into the model and shows it from its beginning */
	private void showPlainText(String decrypted)
	{
		modelObject.setPlainText(decrypted);
		txtPlainText.setText(modelObject.getPlainText());
		txtPlainText.setCaretPosition(0);
	}

	/** Puts the message into the model and shows it below the tabs */
	private void showResult(String resultText)
	{
		modelObject.setResultText(resultText);
		lblResult.setText(modelObject.getResultText());
	}

	/** The message shown below the tabs */
	public String getResultText()
	{
		return modelObject.getResultText();
	}

	private void onBrowse(JTextField field, IModel<String> chosenFile)
	{
		JFileChooser fileChooser = new JFileChooser();
		if (!chosenFile.getObject().isBlank())
		{
			fileChooser.setSelectedFile(new File(chosenFile.getObject()));
		}
		if (fileChooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION)
		{
			chosenFile.setObject(fileChooser.getSelectedFile().getAbsolutePath());
			field.setText(chosenFile.getObject());
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
