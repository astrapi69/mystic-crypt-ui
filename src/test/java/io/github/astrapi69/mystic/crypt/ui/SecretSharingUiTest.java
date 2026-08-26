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
 * Functional end-to-end test of the case this tool exists for: a master password is split so that
 * three of five shares rebuild it, and two do not. Everything through the real user interface.
 */
class SecretSharingUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String SECRET = "the master password nobody may lose";

	@Test
	void splitsASecretAndRebuildsItFromEnoughSharesThroughTheUi() throws Exception
	{
		installPluginRequiringItBuilt(SECRET_SHARING_ZIP);

		File databaseFile = new File(tempHome, "secret-sharing.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Split and Combine", "Split and Combine");

		GuiActionRunner.execute(() -> {
			frame.robot().finder().findByName("pwdSecret", javax.swing.JPasswordField.class, true)
				.setText(SECRET);
			frame.spinner("spnThreshold").target().setValue(3);
			frame.spinner("spnTotalShares").target().setValue(5);
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnSplit").target().doClick());
		robot.waitForIdle();

		String[] shares = GuiActionRunner
			.execute(() -> frame.textBox("txtShares").target().getText()).split("\\R");
		assertEquals(5, shares.length, "five shares were asked for: " + result(frame));
		for (String share : shares)
		{
			assertTrue(share.startsWith("MCSS1$3$5$"), share);
			assertFalse(share.contains("master"), "no share may carry the secret: " + share);
		}

		// two of them are not enough, and the window says how many are needed
		GuiActionRunner.execute(() -> frame.textBox("txtShares").target()
			.setText(shares[0] + System.lineSeparator() + shares[1]));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnCombine").target().doClick());
		robot.waitForIdle();
		assertTrue(result(frame).contains("3"),
			"two shares must not rebuild it, and the window has to say how many are needed: "
				+ result(frame));
		assertTrue(
			GuiActionRunner.execute(() -> frame.textBox("txtRebuilt").target().getText()).isEmpty(),
			"nothing may be shown as the secret when nothing was rebuilt");

		// three of them are, and they need not be the first three
		GuiActionRunner.execute(() -> frame.textBox("txtShares").target()
			.setText(String.join(System.lineSeparator(), shares[4], shares[1], shares[2])));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnCombine").target().doClick());
		robot.waitForIdle();

		assertEquals(SECRET,
			GuiActionRunner.execute(() -> frame.textBox("txtRebuilt").target().getText()),
			"any three of the five have to rebuild the secret: " + result(frame));
	}

	private String result(FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> frame.label("lblResult").target().getText());
	}
}
