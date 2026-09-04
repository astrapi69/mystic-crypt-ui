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

import java.awt.Font;
import java.io.File;

import javax.swing.*;

import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
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
 * <p>
 * Both tabs are laid out with the shared tool window form ({@link ToolForm}), so this window agrees
 * with the one next to it: labels in a narrow right aligned column, entry components filling the
 * width the window has left over, the two text areas taking the height that is left, and every text
 * component carrying a minimum width of its own so that a narrow window shrinks the fields instead
 * of collapsing them to nothing.
 */
public class FileCryptPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** A field that shares its cell with the button that fills it in */
	private static final String FIELD_WITH_BUTTON = ToolForm.FIELD + ", split 2";

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
		// one layout for every tool window: the tabs take the height the window gives them, the
		// message about what happened sits underneath them
		super(ToolForm.newLayout());

		bindToModel();

		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
		lblResult.setToolTipText(
			FileCryptMessages.getString("filecrypt.tooltip.result", "what the last operation did"));
		tabs.setName("tabFileCrypt");
		tabs.addTab("File", newFileTab());
		tabs.addTab("Text", newTextTab());
		// the tool starts on the tab the user configured in the settings dialog
		tabs.setSelectedIndex("text".equals(FileCryptSettingsContribution.startTab()) ? 1 : 0);

		JLabel lblIntro = new JLabel("<html>" + FileCryptMessages.getString("filecrypt.intro",
			"Encrypts or decrypts a file or a piece of text with a passphrase - use the File tab "
				+ "for a file on disk, the Text tab to encrypt or decrypt text directly. There is "
				+ "no way to recover the content if the passphrase is lost - nothing here can "
				+ "bypass it.") + "</html>");
		lblIntro.setName("lblIntro");

		add(lblIntro, ToolForm.WIDE);
		add(tabs, ToolForm.GROWING);
		add(lblResult, ToolForm.RESULT_LINE);
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

		txtSourceFile.setToolTipText(FileCryptMessages.getString("filecrypt.tooltip.source.file",
			"the file to encrypt or decrypt"));
		txtTargetFile.setToolTipText(FileCryptMessages.getString("filecrypt.tooltip.target.file",
			"the file to write to - left blank, the result is written next to the source file"));
		pwdFile.setToolTipText(FileCryptMessages.getString("filecrypt.tooltip.file.passphrase",
			"the passphrase that protects the file - required for both encrypting and decrypting"));
		pwdFileRepeated.setToolTipText(
			FileCryptMessages.getString("filecrypt.tooltip.file.passphrase.repeated",
				"repeat the passphrase to catch a typo - only checked when encrypting, a wrong passphrase when decrypting is refused by the cipher itself"));

		JPanel panel = new JPanel(ToolForm.newLayout());
		panel.add(new JLabel("File:"));
		panel.add(ToolForm.sized(txtSourceFile), FIELD_WITH_BUTTON);
		panel.add(button("btnBrowseSource", "...", event -> onBrowse(txtSourceFile, sourceFileModel()),
			FileCryptMessages.getString("filecrypt.tooltip.browse.source.button",
				"choose the file to encrypt or decrypt")));
		panel.add(new JLabel("Write to:"));
		panel.add(ToolForm.sized(txtTargetFile), FIELD_WITH_BUTTON);
		panel.add(button("btnBrowseTarget", "...", event -> onBrowse(txtTargetFile, targetFileModel()),
			FileCryptMessages.getString("filecrypt.tooltip.browse.target.button",
				"choose where to write the result")));
		panel.add(new JLabel("Passphrase:"));
		panel.add(ToolForm.sized(pwdFile), ToolForm.FIELD);
		panel.add(new JLabel("Repeat (to encrypt):"));
		panel.add(ToolForm.sized(pwdFileRepeated), ToolForm.FIELD);
		panel.add(
			ToolForm.buttons(
				button("btnEncryptFile", "Encrypt", event -> onEncryptFile(),
					FileCryptMessages.getString("filecrypt.tooltip.encrypt.file.button",
						"encrypts the file above with the passphrase")),
				button("btnDecryptFile", "Decrypt", event -> onDecryptFile(),
					FileCryptMessages.getString("filecrypt.tooltip.decrypt.file.button",
						"decrypts the file above with the passphrase"))),
			ToolForm.BUTTON_ROW);
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

		txtPlainText.setToolTipText(FileCryptMessages.getString("filecrypt.tooltip.plain.text",
			"the text to encrypt, or where the decrypted text appears"));
		txtEncryptedText.setToolTipText(FileCryptMessages.getString("filecrypt.tooltip.encrypted.text",
			"the encrypted text, Base64 encoded - paste one in to decrypt it"));
		pwdText.setToolTipText(FileCryptMessages.getString("filecrypt.tooltip.text.passphrase",
			"the passphrase that protects the text - required for both encrypting and decrypting"));
		pwdTextRepeated.setToolTipText(
			FileCryptMessages.getString("filecrypt.tooltip.text.passphrase.repeated",
				"repeat the passphrase to catch a typo - only checked when encrypting"));

		JPanel panel = new JPanel(ToolForm.newLayout());
		panel.add(new JLabel("Text:"), "aligny top");
		panel.add(ToolForm.scrolled(txtPlainText), "grow, push");
		panel.add(new JLabel("Encrypted (Base64):"), "aligny top");
		panel.add(ToolForm.scrolled(txtEncryptedText), "grow, push");
		panel.add(new JLabel("Passphrase:"));
		panel.add(ToolForm.sized(pwdText), ToolForm.FIELD);
		panel.add(new JLabel("Repeat (to encrypt):"));
		panel.add(ToolForm.sized(pwdTextRepeated), ToolForm.FIELD);
		panel.add(
			ToolForm.buttons(
				button("btnEncryptText", "Encrypt", event -> onEncryptText(),
					FileCryptMessages.getString("filecrypt.tooltip.encrypt.text.button",
						"encrypts the text above with the passphrase")),
				button("btnDecryptText", "Decrypt", event -> onDecryptText(),
					FileCryptMessages.getString("filecrypt.tooltip.decrypt.text.button",
						"decrypts the text above with the passphrase"))),
			ToolForm.BUTTON_ROW);
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
