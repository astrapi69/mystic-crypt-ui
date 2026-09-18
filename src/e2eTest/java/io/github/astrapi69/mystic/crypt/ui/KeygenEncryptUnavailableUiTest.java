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

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Negative-path end-to-end test of the keygen plugin's modern-algorithm branch: after generating a
 * non-RSA (X25519) key pair, the hex Encrypt/Decrypt demo does not apply. The buttons must then be
 * out of reach and carry the reason, so the click that leads nowhere never happens. Drives that
 * through the real UI.
 */
class KeygenEncryptUnavailableUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	void encryptOnANonRsaKeyIsOutOfReachAndSaysWhy() throws Exception
	{
		installPluginRequiringItBuilt(KEYGEN_ZIP);

		File databaseFile = new File(tempHome, "keygen-guard-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Key Generation", "Key generation demo");

		// generate an X25519 key pair - a non-RSA algorithm, so encrypt/decrypt is unavailable
		GuiActionRunner.execute(() -> frame.comboBox("cmbAlgorithm").target().setSelectedItem(
			io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm.X25519));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnGenerate").target().doClick());
		robot.waitForIdle();

		// text alone used to be enough to enable the button; the algorithm has the last word now
		GuiActionRunner.execute(() -> frame.textBox("txtToEncrypt").target().setText("anything"));
		robot.waitForIdle();

		assertFalse(GuiActionRunner.execute(() -> frame.button("btnEncrypt").target().isEnabled()),
			"encrypt offered itself for an algorithm it cannot serve");
		String reason = GuiActionRunner
			.execute(() -> frame.button("btnEncrypt").target().getToolTipText());
		assertTrue(reason != null && reason.contains("RSA"),
			"the button that is out of reach must explain that this demo is RSA-only, was: "
				+ reason);
	}
}
