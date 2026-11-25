/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.swing.JButton;
import javax.swing.JPasswordField;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * End-to-end test of Cancel in the file encryption window (#1053): while the key is derived the
 * window offers Cancel, and an encryption cancelled then writes no file and says so.
 */
class FileCryptCancelUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String PASSPHRASE = TestPasswords.throwaway();

	@Test
	@DisplayName("an encryption cancelled while the key is derived writes no file and says so (#1053)")
	void anEncryptionCancelledWhileTheKeyIsDerived_writesNoFile() throws Exception
	{
		installPluginRequiringItBuilt(FILE_CRYPT_ZIP);

		File databaseFile = new File(tempHome, "file-crypt-cancel-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Encrypt and Decrypt", "Encrypt and Decrypt");

		File source = new File(tempHome, "cancelled-notes.txt");
		Files.write(source.toPath(), "nothing to see".getBytes(StandardCharsets.UTF_8));
		File encrypted = new File(tempHome, "cancelled-notes.txt.mcenc");
		GuiActionRunner.execute(() -> {
			frame.textBox("txtSourceFile").target().setText(source.getAbsolutePath());
			robot.finder().findByName("pwdFile", JPasswordField.class, true).setText(PASSPHRASE);
			robot.finder().findByName("pwdFileRepeated", JPasswordField.class, true)
				.setText(PASSPHRASE);
		});
		robot.waitForIdle();

		JButton encrypt = GuiActionRunner.execute(() -> frame.button("btnEncryptFile").target());
		JButton cancel = GuiActionRunner.execute(() -> FileCryptWindow.cancelButton(robot));
		// both presses in one step of the event dispatch thread: the key derivation, which takes
		// hundreds of milliseconds, cannot be over between them
		boolean cancelWasOffered = GuiActionRunner.execute(() -> {
			encrypt.doClick();
			boolean offered = cancel.isShowing() && !encrypt.isEnabled();
			cancel.doClick();
			return offered;
		});
		FileCryptWindow.awaitTheAnswer(robot, "btnCancelFileCrypt");

		assertTrue(cancelWasOffered, "while the key was derived the window offered no Cancel");
		assertFalse(encrypted.exists(), "the cancelled encryption wrote the file");
		assertEquals("not encrypted: cancelled before anything was written", result(frame));
		assertTrue(GuiActionRunner.execute(encrypt::isEnabled),
			"the window takes no press after the cancel");
	}

	private String result(FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> frame.label("lblResult").target().getText());
	}
}
