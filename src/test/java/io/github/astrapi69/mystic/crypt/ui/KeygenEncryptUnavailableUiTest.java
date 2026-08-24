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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.junit.jupiter.api.Test;

/**
 * Negative-path end-to-end test of the keygen plugin's modern-algorithm branch: after generating a
 * non-RSA (X25519) key pair, the hex Encrypt/Decrypt demo does not apply, so pressing Encrypt must
 * pop an information dialog rather than throw. Drives that guard through the real UI.
 */
class KeygenEncryptUnavailableUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "keygen-guard-e2e-pw-123";

	@Test
	void encryptOnANonRsaKeyShowsAnInformationDialog() throws Exception
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

		// give the Encrypt button some text so it is enabled, then press it. The click opens a
		// modal
		// dialog, so trigger it off the test thread (invokeLater) and find the dialog separately.
		GuiActionRunner.execute(() -> frame.textBox("txtToEncrypt").target().setText("anything"));
		robot.waitForIdle();
		SwingUtilities.invokeLater(() -> frame.button("btnEncrypt").target().doClick());

		JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().withTimeout(5000)
			.using(robot);
		String message = GuiActionRunner
			.execute(() -> String.valueOf(optionPane.target().getMessage()));
		assertTrue(message.contains("RSA"),
			"the guard dialog must explain that encrypt/decrypt is RSA-only, was: " + message);

		// close the dialog locale-independently (the OK button label is localized)
		GuiActionRunner.execute(() -> optionPane.target().setValue(JOptionPane.CLOSED_OPTION));
		robot.waitForIdle();
	}
}
