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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Negative-path end-to-end test of the keygen plugin's modern-algorithm branch: after generating a
 * key pair that cannot encrypt (X25519, X448, ML-KEM-768, ML-DSA-65), the hex Encrypt/Decrypt demo
 * does not apply. Both buttons must then be out of reach and carry the reason, so the click that
 * leads nowhere never happens. RSA and EC are the counterpart: their buttons stay usable. Drives
 * that through the real UI.
 */
class KeygenEncryptUnavailableUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private FrameFixture openTheKeyGenerationWindow() throws Exception
	{
		installPluginRequiringItBuilt(KEYGEN_ZIP);

		File databaseFile = new File(tempHome, "keygen-guard-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Key Generation", "Key generation demo");
		return frame;
	}

	/**
	 * Generates with the given algorithm and puts text into BOTH text areas, so that an empty text
	 * can never be the reason a button is off: after generating a key pair that cannot encrypt the
	 * areas are emptied, and the Decrypt button would be off for lack of text whatever the
	 * algorithm says
	 */
	private void generateAndFillBothTextAreas(final FrameFixture frame,
		final KeyPairGeneratorAlgorithm algorithm)
	{
		GuiActionRunner
			.execute(() -> frame.comboBox("cmbAlgorithm").target().setSelectedItem(algorithm));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnGenerate").target().doClick());
		robot.waitForIdle();
		GuiActionRunner.execute(() -> {
			frame.textBox("txtToEncrypt").target().setText("anything");
			frame.textBox("txtEncrypted").target().setText("a1b2c3");
		});
		robot.waitForIdle();
	}

	@Test
	@DisplayName("only an algorithm that cannot encrypt puts both buttons out of reach")
	void encryptAndDecryptAreOutOfReachAndSayWhyForAnAlgorithmThatCannotEncrypt() throws Exception
	{
		// one window for every algorithm and one test method: a second window of the tool stays
		// open beside the first with the same component names
		FrameFixture frame = openTheKeyGenerationWindow();

		for (KeyPairGeneratorAlgorithm algorithm : new KeyPairGeneratorAlgorithm[] {
				KeyPairGeneratorAlgorithm.X25519, KeyPairGeneratorAlgorithm.X448,
				KeyPairGeneratorAlgorithm.ML_KEM_768, KeyPairGeneratorAlgorithm.ML_DSA_65 })
		{
			generateAndFillBothTextAreas(frame, algorithm);

			for (String button : new String[] { "btnEncrypt", "btnDecrypt" })
			{
				assertFalse(
					GuiActionRunner.execute(() -> frame.button(button).target().isEnabled()),
					button + " offered itself for " + algorithm + ", which cannot serve it");
				String reason = GuiActionRunner
					.execute(() -> frame.button(button).target().getToolTipText());
				assertTrue(reason != null && reason.contains("RSA") && reason.contains("EC"),
					button + " is out of reach for " + algorithm
						+ " and must explain that this demo needs RSA or EC, was: " + reason);
				assertEquals(reason,
					GuiActionRunner.execute(() -> frame.label("lblReason").target().getText()),
					"the label under the buttons says something else than the tooltip of " + button
						+ " for " + algorithm);
			}
		}

		// the counterpart: RSA and EC keep both buttons usable, in the same window
		for (KeyPairGeneratorAlgorithm algorithm : new KeyPairGeneratorAlgorithm[] {
				KeyPairGeneratorAlgorithm.RSA, KeyPairGeneratorAlgorithm.EC })
		{
			generateAndFillBothTextAreas(frame, algorithm);

			for (String button : new String[] { "btnEncrypt", "btnDecrypt" })
			{
				assertTrue(GuiActionRunner.execute(() -> frame.button(button).target().isEnabled()),
					button + " is out of reach for " + algorithm + ", which can serve it");
			}
		}
	}
}
