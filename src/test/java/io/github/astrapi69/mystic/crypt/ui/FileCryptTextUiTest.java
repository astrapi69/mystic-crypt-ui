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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

/**
 * Functional end-to-end test of encrypting a piece of text through the real user interface.
 */
class FileCryptTextUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "file-crypt-e2e-pw-123";

	private static final String PASSPHRASE = "file-crypt-e2e-passphrase-456";

	@Test
	void encryptsAndDecryptsATextThroughTheUi() throws Exception
	{
		installPluginRequiringItBuilt(FILE_CRYPT_ZIP);

		File databaseFile = new File(tempHome, "file-crypt-text.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Encrypt and Decrypt", "Encrypt and Decrypt");

		GuiActionRunner.execute(() -> {
			frame.tabbedPane("tabFileCrypt").target().setSelectedIndex(1);
			frame.textBox("txtPlainText").target().setText("a note to myself");
			frame.robot().finder().findByName("pwdText", javax.swing.JPasswordField.class, true)
				.setText(PASSPHRASE);
			frame.robot().finder()
				.findByName("pwdTextRepeated", javax.swing.JPasswordField.class, true)
				.setText(PASSPHRASE);
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnEncryptText").target().doClick());
		robot.waitForIdle();

		String encrypted = GuiActionRunner
			.execute(() -> frame.textBox("txtEncryptedText").target().getText());
		assertFalse(encrypted.isBlank(), "encrypting must produce something: " + result(frame));
		assertFalse(encrypted.contains("note to myself"));

		GuiActionRunner.execute(() -> frame.textBox("txtPlainText").target().setText(""));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnDecryptText").target().doClick());
		robot.waitForIdle();

		assertEquals("a note to myself",
			GuiActionRunner.execute(() -> frame.textBox("txtPlainText").target().getText()),
			"the text must come back: " + result(frame));
	}

	private String result(FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> frame.label("lblResult").target().getText());
	}
}
