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
 * Functional end-to-end test of the post-quantum signature plugin: generates an ML-DSA-65 key pair,
 * signs a message, verifies the signature, and then changes the message so the very same signature
 * must be rejected - all through the real UI.
 */
/**
 * The generate button follows the chosen algorithm: this tool generates Ed25519, ML-DSA and SLH-DSA
 * keys, while the classical algorithms in the same dropdown are there for keys that come from a
 * file. Its own class rather than another method beside the signing flow, because a second tool
 * window of the same name in one application run makes every lookup ambiguous (#488).
 */
class SignatureGenerateOfferUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	/**
	 * The dropdown offers the classical algorithms for keys that come from a file, and this tool
	 * generates none of them - so the generate button has to be off while one is chosen, with the
	 * reason on screen before the press rather than after it (#488)
	 */
	@Test
	void doesNotOfferToGenerateAKeyItCannotGenerate() throws Exception
	{
		installPluginRequiringItBuilt(PQC_SIGNATURE_ZIP);

		File databaseFile = new File(tempHome, "generate-offer-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Sign and Verify", "Sign and Verify");

		GuiActionRunner.execute(
			() -> frame.comboBox("cmbAlgorithm").target().setSelectedItem("SHA256withRSA"));
		robot.waitForIdle();

		assertFalse(GuiActionRunner.execute(() -> frame.button("btnGenerate").target().isEnabled()),
			"a key for SHA256withRSA has to come from a file, so generating must not be offered");
		String said = GuiActionRunner.execute(() -> frame.label("lblResult").target().getText());
		assertTrue(said.contains("SHA256withRSA") && said.contains("file"),
			"the panel has to say why before the press: " + said);

		GuiActionRunner
			.execute(() -> frame.comboBox("cmbAlgorithm").target().setSelectedItem("Ed25519"));
		robot.waitForIdle();

		assertTrue(GuiActionRunner.execute(() -> frame.button("btnGenerate").target().isEnabled()),
			"choosing an algorithm this tool generates has to bring the button back");
	}
}
