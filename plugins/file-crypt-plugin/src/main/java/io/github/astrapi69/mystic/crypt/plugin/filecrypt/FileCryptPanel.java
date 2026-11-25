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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;

import javax.swing.*;
import javax.swing.text.JTextComponent;

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
 * <p>
 * Deriving the key from the passphrase takes a noticeable moment, so it does not run on the event
 * dispatch thread (#1053): while it runs the window keeps answering, says that it works, takes no
 * second press and offers Cancel. A cancelled file operation writes nothing, a cancelled text
 * operation shows nothing. What the user entered is taken from the model when the button is
 * pressed, so typing on while the key is derived does not change the operation that runs.
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
	private final JButton btnCancel = button("btnCancelFileCrypt",
		FileCryptMessages.getString("filecrypt.button.cancel", "Cancel"), event -> onCancel(),
		FileCryptMessages.getString("filecrypt.tooltip.cancel.button",
			"gives up the operation that runs - a file is not written, a text is not shown"));
	/** The four buttons that start an operation, which take no press while one runs */
	private final List<JButton> actionButtons = new ArrayList<>();
	/** Where an operation runs, the one that derives the key */
	private final Executor work;
	/** Where the answer of an operation is put on screen */
	private final Executor screen;
	/** Whether the user cancelled the operation that runs, asked from the thread it runs on */
	private volatile boolean cancelRequested;

	/**
	 * The window of the application: an operation runs on a thread of its own and its answer is put
	 * on screen on the event dispatch thread
	 */
	public FileCryptPanel()
	{
		this(FileCryptPanel::onAThreadOfItsOwn, SwingUtilities::invokeLater);
	}

	/**
	 * The window over the given executors
	 *
	 * @param work
	 *            where an operation runs, which derives the key from the passphrase
	 * @param screen
	 *            where the answer of an operation is put on screen, the event dispatch thread in
	 *            the application
	 */
	public FileCryptPanel(final Executor work, final Executor screen)
	{
		// one layout for every tool window: the tabs take the height the window gives them, the
		// message about what happened sits underneath them
		super(ToolForm.newLayout());
		this.work = work;
		this.screen = screen;

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

		JTextComponent lblIntro = ToolForm.intro(FileCryptMessages.getString("filecrypt.intro",
			"Encrypts or decrypts a file or a piece of text with a passphrase - use the File tab "
				+ "for a file on disk, the Text tab to encrypt or decrypt text directly. There is "
				+ "no way to recover the content if the passphrase is lost - nothing here can "
				+ "bypass it."));
		lblIntro.setName("lblIntro");

		add(lblIntro, ToolForm.INTRO_ROW);
		add(tabs, ToolForm.GROWING);
		add(lblResult, ToolForm.RESULT_LINE + ", split 2");
		btnCancel.setVisible(false);
		add(btnCancel);
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
		pwdFileRepeated.setToolTipText(FileCryptMessages.getString(
			"filecrypt.tooltip.file.passphrase.repeated",
			"repeat the passphrase to catch a typo - only checked when encrypting, a wrong passphrase when decrypting is refused by the cipher itself"));

		JPanel panel = new JPanel(ToolForm.newLayout());
		panel.add(new JLabel("File:"));
		panel.add(ToolForm.sized(txtSourceFile), FIELD_WITH_BUTTON);
		panel.add(
			button("btnBrowseSource", "...", event -> onBrowse(txtSourceFile, sourceFileModel()),
				FileCryptMessages.getString("filecrypt.tooltip.browse.source.button",
					"choose the file to encrypt or decrypt")));
		panel.add(new JLabel("Write to:"));
		panel.add(ToolForm.sized(txtTargetFile), FIELD_WITH_BUTTON);
		panel.add(button("btnBrowseTarget", "...",
			event -> onBrowse(txtTargetFile, targetFileModel()), FileCryptMessages.getString(
				"filecrypt.tooltip.browse.target.button", "choose where to write the result")));
		panel.add(new JLabel("Passphrase:"));
		panel.add(ToolForm.sized(pwdFile), ToolForm.FIELD);
		panel.add(new JLabel("Repeat (to encrypt):"));
		panel.add(ToolForm.sized(pwdFileRepeated), ToolForm.FIELD);
		panel
			.add(
				ToolForm
					.buttons(
						actionButton("btnEncryptFile", "Encrypt", event -> onEncryptFile(),
							FileCryptMessages.getString("filecrypt.tooltip.encrypt.file.button",
								"encrypts the file above with the passphrase")),
						actionButton("btnDecryptFile", "Decrypt", event -> onDecryptFile(),
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
		txtEncryptedText
			.setToolTipText(FileCryptMessages.getString("filecrypt.tooltip.encrypted.text",
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
		panel
			.add(
				ToolForm
					.buttons(
						actionButton("btnEncryptText", "Encrypt", event -> onEncryptText(),
							FileCryptMessages.getString("filecrypt.tooltip.encrypt.text.button",
								"encrypts the text above with the passphrase")),
						actionButton("btnDecryptText", "Decrypt", event -> onDecryptText(),
							FileCryptMessages.getString("filecrypt.tooltip.decrypt.text.button",
								"decrypts the text above with the passphrase"))),
				ToolForm.BUTTON_ROW);
		return panel;
	}

	private void onEncryptFile()
	{
		char[] entered = modelObject.getFilePassphrase().clone();
		char[] repeated = modelObject.getFilePassphraseRepeated().clone();
		File source = new File(modelObject.getSourceFile().trim());
		File target = targetOrNull();
		boolean deleteSource = FileCryptSettingsContribution.deleteSourceAfterEncrypt();
		start("encrypted", () -> {
			File written = FileCryptSupport.encryptFile(source, target,
				matchingPassphrase(entered, repeated), () -> cancelRequested);
			String message = "encrypted to " + written.getName();
			if (deleteSource && Files.deleteIfExists(source.toPath()))
			{
				message += ", removed " + source.getName();
			}
			return Outcome.written(message, () -> showTargetFile(written));
		});
	}

	private void onDecryptFile()
	{
		char[] passphrase = modelObject.getFilePassphrase().clone();
		File source = new File(modelObject.getSourceFile().trim());
		File target = targetOrNull();
		start("decrypted", () -> {
			File written = FileCryptSupport.decryptFile(source, target, new String(passphrase),
				() -> cancelRequested);
			return Outcome.written("decrypted to " + written.getName(),
				() -> showTargetFile(written));
		});
	}

	private void onEncryptText()
	{
		char[] entered = modelObject.getTextPassphrase().clone();
		char[] repeated = modelObject.getTextPassphraseRepeated().clone();
		String plainText = modelObject.getPlainText();
		start("encrypted", () -> {
			String encrypted = FileCryptSupport.encryptText(plainText,
				matchingPassphrase(entered, repeated));
			return Outcome.shown("encrypted " + plainText.length() + " characters",
				() -> showEncryptedText(encrypted));
		});
	}

	private void onDecryptText()
	{
		char[] passphrase = modelObject.getTextPassphrase().clone();
		String encryptedText = modelObject.getEncryptedText();
		start("decrypted", () -> {
			String decrypted = FileCryptSupport.decryptText(encryptedText, new String(passphrase));
			return Outcome.shown("decrypted " + decrypted.length() + " characters",
				() -> showPlainText(decrypted));
		});
	}

	/** Asks the operation that runs to stop before it writes or shows anything */
	private void onCancel()
	{
		cancelRequested = true;
		btnCancel.setEnabled(false);
		showResult(FileCryptMessages.getString("filecrypt.result.cancelling",
			"cancelling: the operation stops before it writes or shows anything ..."));
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

	/**
	 * Starts an operation where the work runs, after saying that it works and offering Cancel, and
	 * puts its answer on screen once it is there
	 */
	private void start(final String what, final FileCryptOperation operation)
	{
		cancelRequested = false;
		showWorking(true);
		showResult(FileCryptMessages.getString("filecrypt.result.working",
			"working: the key is being derived from the passphrase ..."));
		work.execute(() -> {
			Outcome outcome = outcomeOf(operation);
			screen.execute(() -> finish(what, outcome));
		});
	}

	/**
	 * Runs the operation and says what came of it. A file larger than an array can hold is refused
	 * with an {@link OutOfMemoryError} by the JDK before anything is read; on the thread of its own
	 * that would end the work without an answer and leave the window waiting for one, so it is an
	 * answer like any other refusal
	 */
	private static Outcome outcomeOf(final FileCryptOperation operation)
	{
		try
		{
			return operation.execute();
		}
		catch (CancellationException exception)
		{
			return Outcome.stoppedByCancel();
		}
		catch (Exception | OutOfMemoryError failure)
		{
			return Outcome.notDone(message(failure));
		}
	}

	/**
	 * Puts the answer of an operation on screen. A text the user cancelled is not shown, even when
	 * it was complete before the cancel arrived; a file that was written is reported, because it is
	 * there
	 */
	private void finish(final String what, final Outcome outcome)
	{
		if (outcome.cancelled() || cancelRequested && outcome.withdrawnByCancel())
		{
			showResult("not " + what + ": " + FileCryptMessages
				.getString("filecrypt.result.cancelled", "cancelled before anything was written"));
		}
		else if (outcome.failure() != null)
		{
			showResult("not " + what + ": " + outcome.failure());
		}
		else
		{
			outcome.onScreen().run();
			showResult(outcome.message());
		}
		showWorking(false);
	}

	/** Takes the presses away from the four buttons while an operation runs and offers Cancel */
	private void showWorking(final boolean working)
	{
		actionButtons.forEach(actionButton -> actionButton.setEnabled(!working));
		btnCancel.setEnabled(working);
		btnCancel.setVisible(working);
	}

	/** Runs an operation on a daemon thread, which does not keep the application from ending */
	private static void onAThreadOfItsOwn(final Runnable operation)
	{
		Thread.ofPlatform().daemon().name("file-crypt").start(operation);
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

	private static String message(Throwable failure)
	{
		// a wrong passphrase surfaces as an exception without a message often enough that the class
		// name is the only thing left to show
		return failure.getMessage() != null
			? failure.getMessage()
			: failure.getClass().getSimpleName();
	}

	@FunctionalInterface
	private interface FileCryptOperation
	{
		Outcome execute() throws Exception;
	}

	/**
	 * What came of an operation, worked out where it ran and put on screen afterwards
	 *
	 * @param message
	 *            the message below the tabs when it succeeded
	 * @param onScreen
	 *            what it changes on screen when it succeeded
	 * @param withdrawnByCancel
	 *            whether a cancel that arrives after the work still keeps it off the screen, true
	 *            for a text, false for a file that is already written
	 * @param cancelled
	 *            whether it stopped because the user cancelled
	 * @param failure
	 *            why it failed, or {@code null}
	 */
	private record Outcome(String message, Runnable onScreen, boolean withdrawnByCancel,
		boolean cancelled, String failure) {
		static Outcome shown(final String message, final Runnable onScreen)
		{
			return new Outcome(message, onScreen, true, false, null);
		}

		static Outcome written(final String message, final Runnable onScreen)
		{
			return new Outcome(message, onScreen, false, false, null);
		}

		static Outcome stoppedByCancel()
		{
			return new Outcome(null, () -> {
			}, false, true, null);
		}

		static Outcome notDone(final String failure)
		{
			return new Outcome(null, () -> {
			}, false, false, failure);
		}
	}

	/** A button that starts an operation, which takes no press while one runs */
	private JButton actionButton(String name, String text, java.awt.event.ActionListener listener,
		String tooltip)
	{
		JButton actionButton = button(name, text, listener, tooltip);
		actionButtons.add(actionButton);
		return actionButton;
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
