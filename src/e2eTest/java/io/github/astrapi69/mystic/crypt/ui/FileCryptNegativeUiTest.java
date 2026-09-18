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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * The two ways of getting a passphrase wrong, through the real user interface: mistyping the
 * repetition, which must stop the encryption before anything is written, and using a wrong one to
 * decrypt, which must not produce a file full of rubbish.
 */
class FileCryptNegativeUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String PASSPHRASE = TestPasswords.throwaway();

	@Test
	void twoDifferentPassphrasesAreRefusedAndAWrongOneDoesNotOpenTheFile() throws Exception
	{
		installPluginRequiringItBuilt(FILE_CRYPT_ZIP);

		File databaseFile = new File(tempHome, "file-crypt-negative.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Encrypt and Decrypt", "Encrypt and Decrypt");

		File source = new File(tempHome, "typo.txt");
		Files.writeString(source.toPath(), "content");

		// a typo in the repeated passphrase must stop the whole thing
		GuiActionRunner.execute(() -> {
			frame.textBox("txtSourceFile").target().setText(source.getAbsolutePath());
			frame.robot().finder().findByName("pwdFile", javax.swing.JPasswordField.class, true)
				.setText(PASSPHRASE);
			frame.robot().finder()
				.findByName("pwdFileRepeated", javax.swing.JPasswordField.class, true)
				.setText(PASSPHRASE + "typo");
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnEncryptFile").target().doClick());
		robot.waitForIdle();

		assertFalse(new File(tempHome, "typo.txt.mcenc").exists(),
			"a passphrase that was mistyped must not be used, the content would be gone");
		assertTrue(result(frame).contains("not the same"), result(frame));

		// now encrypt properly and try to open it with the wrong passphrase
		GuiActionRunner.execute(() -> frame.robot().finder()
			.findByName("pwdFileRepeated", javax.swing.JPasswordField.class, true)
			.setText(PASSPHRASE));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnEncryptFile").target().doClick());
		robot.waitForIdle();
		assertTrue(new File(tempHome, "typo.txt.mcenc").exists(), result(frame));

		GuiActionRunner.execute(() -> {
			frame.textBox("txtSourceFile").target()
				.setText(new File(tempHome, "typo.txt.mcenc").getAbsolutePath());
			frame.textBox("txtTargetFile").target()
				.setText(new File(tempHome, "never-written.txt").getAbsolutePath());
			frame.robot().finder().findByName("pwdFile", javax.swing.JPasswordField.class, true)
				.setText("not the passphrase");
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnDecryptFile").target().doClick());
		robot.waitForIdle();

		assertFalse(new File(tempHome, "never-written.txt").exists(),
			"a wrong passphrase must not produce a file full of rubbish");
		assertTrue(result(frame).startsWith("not decrypted"), result(frame));
	}

	private String result(FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> frame.label("lblResult").target().getText());
	}
}
