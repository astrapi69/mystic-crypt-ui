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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.cli.MysticCryptCli;

/**
 * Functional end-to-end test of the file encryption plugin with a file it did not write itself
 * (#877): the library's {@code encrypt}, the command {@code --cli} runs, seals a file, and the
 * window, installed from its zip into the running application, opens it byte for byte.
 */
class FileCryptOpensWhatEncryptWroteUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String PASSPHRASE = TestPasswords.throwaway();

	@Test
	void decryptsAFileTheCommandLineEncryptedThroughTheUi() throws Exception
	{
		File source = new File(tempHome, "sealed-on-the-command-line.txt");
		byte[] original = "sealed by encrypt, opened in the window"
			.getBytes(StandardCharsets.UTF_8);
		Files.write(source.toPath(), original);
		File encrypted = new File(tempHome, "sealed-on-the-command-line.txt.mcenc");
		assertEquals(0, encryptOnTheCommandLine(source, encrypted),
			"the library's encrypt must seal the file");
		assertEquals("MCFILE",
			new String(Files.readAllBytes(encrypted.toPath()), 0, 6, StandardCharsets.US_ASCII),
			"encrypt writes its own marker, not the window's");

		installPluginRequiringItBuilt(FILE_CRYPT_ZIP);
		File databaseFile = new File(tempHome, "file-crypt-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Encrypt and Decrypt", "Encrypt and Decrypt");

		File decrypted = new File(tempHome, "opened-in-the-window.txt");
		GuiActionRunner.execute(() -> {
			frame.textBox("txtSourceFile").target().setText(encrypted.getAbsolutePath());
			frame.textBox("txtTargetFile").target().setText(decrypted.getAbsolutePath());
			frame.robot().finder().findByName("pwdFile", javax.swing.JPasswordField.class, true)
				.setText(PASSPHRASE);
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnDecryptFile").target().doClick());
		robot.waitForIdle();

		assertTrue(decrypted.exists(), "decrypting must write the file: " + result(frame));
		assertArrayEquals(original, Files.readAllBytes(decrypted.toPath()),
			"the window must open what encrypt sealed, byte for byte");
	}

	/**
	 * Seals a file with the library's {@code encrypt}, the command {@code --cli} runs, the
	 * passphrase on standard input
	 */
	private static int encryptOnTheCommandLine(File source, File encrypted)
	{
		InputStream originalIn = System.in;
		try
		{
			System.setIn(
				new ByteArrayInputStream((PASSPHRASE + "\n").getBytes(StandardCharsets.UTF_8)));
			return MysticCryptCli.execute("encrypt", "-i", source.getAbsolutePath(), "-o",
				encrypted.getAbsolutePath(), "--passphrase-stdin");
		}
		finally
		{
			System.setIn(originalIn);
		}
	}

	private String result(FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> frame.label("lblResult").target().getText());
	}
}
