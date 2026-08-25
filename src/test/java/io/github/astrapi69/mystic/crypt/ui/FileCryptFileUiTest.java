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
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

/**
 * Functional end-to-end test of the file encryption plugin: encrypts a file through the real user
 * interface and gets it back byte for byte.
 */
class FileCryptFileUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "file-crypt-e2e-pw-123";

	private static final String PASSPHRASE = "file-crypt-e2e-passphrase-456";

	@Test
	void encryptsAndDecryptsAFileThroughTheUi() throws Exception
	{
		installPluginRequiringItBuilt(FILE_CRYPT_ZIP);

		File databaseFile = new File(tempHome, "file-crypt-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Encrypt and Decrypt", "Encrypt and Decrypt");

		File source = new File(tempHome, "secret-notes.txt");
		byte[] original = "the launch code is 1234".getBytes(StandardCharsets.UTF_8);
		Files.write(source.toPath(), original);
		File encrypted = new File(tempHome, "secret-notes.txt.mcenc");

		// encrypt: the passphrase has to be typed twice
		GuiActionRunner.execute(() -> {
			frame.textBox("txtSourceFile").target().setText(source.getAbsolutePath());
			frame.robot().finder().findByName("pwdFile", javax.swing.JPasswordField.class, true)
				.setText(PASSPHRASE);
			frame.robot().finder()
				.findByName("pwdFileRepeated", javax.swing.JPasswordField.class, true)
				.setText(PASSPHRASE);
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnEncryptFile").target().doClick());
		robot.waitForIdle();

		assertTrue(encrypted.exists(), "encrypting must write the file: " + result(frame));
		assertFalse(new String(Files.readAllBytes(encrypted.toPath()), StandardCharsets.ISO_8859_1)
			.contains("launch code"), "the content must not be findable in the encrypted file");

		// decrypt it back to a new name
		File decrypted = new File(tempHome, "recovered.txt");
		GuiActionRunner.execute(() -> {
			frame.textBox("txtSourceFile").target().setText(encrypted.getAbsolutePath());
			frame.textBox("txtTargetFile").target().setText(decrypted.getAbsolutePath());
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnDecryptFile").target().doClick());
		robot.waitForIdle();

		assertTrue(decrypted.exists(), "decrypting must write the file: " + result(frame));
		assertArrayEquals(original, Files.readAllBytes(decrypted.toPath()),
			"what went in must come back out byte for byte");
	}

	private String result(FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> frame.label("lblResult").target().getText());
	}
}
