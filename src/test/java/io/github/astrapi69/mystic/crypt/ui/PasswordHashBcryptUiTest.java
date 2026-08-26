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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Functional end-to-end test of hashing a password with bcrypt and checking it again, plus the
 * refusal bcrypt needs: it looks at the first 72 bytes of a password and no further, so a longer
 * one must not be accepted as if all of it counted.
 */
class PasswordHashBcryptUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String PASSWORD = TestPasswords.throwaway();

	@Test
	void hashesWithBcryptAndChecksItAgainThroughTheUi() throws Exception
	{
		installPluginRequiringItBuilt(PASSWORD_HASH_ZIP);

		File databaseFile = new File(tempHome, "password-hash-bcrypt.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Password Hashing", "Password Hashing");

		GuiActionRunner.execute(() -> {
			frame.comboBox("cmbAlgorithm").target().setSelectedItem("bcrypt");
			frame.robot().finder().findByName("txtPassword", javax.swing.JPasswordField.class, true)
				.setText(PASSWORD);
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnHash").target().doClick());
		robot.waitForIdle();

		String hash = GuiActionRunner.execute(() -> frame.textBox("txtHash").target().getText());
		assertTrue(hash.startsWith("$2"), "a bcrypt hash says so in its first characters: " + hash);
		assertFalse(hash.contains(PASSWORD), "the password must not be in the hash");

		// the right password matches
		GuiActionRunner.execute(() -> frame.robot().finder()
			.findByName("txtVerifyPassword", javax.swing.JPasswordField.class, true)
			.setText(PASSWORD));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnVerify").target().doClick());
		robot.waitForIdle();
		assertTrue(result(frame).startsWith("matches"), result(frame));

		// another one does not
		GuiActionRunner.execute(() -> frame.robot().finder()
			.findByName("txtVerifyPassword", javax.swing.JPasswordField.class, true)
			.setText("wrong password"));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnVerify").target().doClick());
		robot.waitForIdle();
		assertEquals("does not match", result(frame));

		// and a password longer than bcrypt looks at is refused rather than silently truncated
		GuiActionRunner.execute(() -> frame.robot().finder()
			.findByName("txtPassword", javax.swing.JPasswordField.class, true)
			.setText("a".repeat(80)));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnHash").target().doClick());
		robot.waitForIdle();

		assertTrue(result(frame).contains("72"),
			"the window has to say why the password was not hashed: " + result(frame));
		assertTrue(
			GuiActionRunner.execute(() -> frame.textBox("txtHash").target().getText()).isEmpty(),
			"nothing may be shown as a hash when nothing was hashed");
	}

	private String result(FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> frame.label("lblResult").target().getText());
	}
}
