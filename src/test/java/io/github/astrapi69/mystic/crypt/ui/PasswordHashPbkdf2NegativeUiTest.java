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
 * Negative-path end-to-end test of the password-hash plugin: selects PBKDF2, hashes a password, then
 * verifies a WRONG password against that hash and asserts the panel reports "does not match" through
 * the real UI. The happy path (Argon2id, matching) lives in {@link PasswordHashPluginUiTest}.
 */
class PasswordHashPbkdf2NegativeUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "pwhash-neg-e2e-pw-123";

	@Test
	void pbkdf2VerifyingAWrongPasswordReportsNoMatchThroughTheUi() throws Exception
	{
		installPluginRequiringItBuilt(PASSWORD_HASH_ZIP);

		File databaseFile = new File(tempHome, "pwhash-neg-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Password Hashing", "Password Hashing");

		// hash with PBKDF2
		GuiActionRunner.execute(() -> {
			frame.comboBox("cmbAlgorithm").target().setSelectedItem("PBKDF2");
			frame.textBox("txtPassword").target().setText("correct-secret");
			frame.button("btnHash").target().doClick();
		});
		robot.waitForIdle();

		String hash = GuiActionRunner.execute(() -> frame.textBox("txtHash").target().getText());
		assertFalse(hash.isBlank(), "hashing must produce an encoded PBKDF2 hash");

		// verify a WRONG password against that hash
		GuiActionRunner.execute(() -> {
			frame.textBox("txtVerifyPassword").target().setText("wrong-secret");
			frame.button("btnVerify").target().doClick();
		});
		robot.waitForIdle();

		assertEquals("does not match",
			GuiActionRunner.execute(() -> frame.label("lblResult").target().getText()),
			"verifying a wrong password must report no match");
	}
}
