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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Functional end-to-end test of the keygen plugin: loads the plugin from its zip, opens the "Key
 * Generation" tool from the Plugins menu, generates an RSA key pair, then encrypts a text and
 * decrypts it again through the panel's Encrypt/Decrypt buttons - all through the real UI. The
 * decrypted text must equal the original
 */
class KeygenPluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	void keyGenerationGeneratesAndRoundTripsEncryptionThroughTheUi() throws Exception
	{
		installPluginRequiringItBuilt(KEYGEN_ZIP);

		File databaseFile = new File(tempHome, "keygen-e2e-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Key Generation", "Key generation demo");

		// generate the key pair (default key size is 1024, fast); onGenerate runs synchronously
		clickButton(frame, "btnGenerate");
		String privateKeyPem = GuiActionRunner
			.execute(() -> frame.textBox("txtPrivateKey").target().getText());
		assertTrue(privateKeyPem.contains("PRIVATE KEY"),
			"the private key area must show the generated PEM, was: " + privateKeyPem);

		// encrypt a text with the generated public key
		String original = "top secret message";
		GuiActionRunner.execute(() -> frame.textBox("txtToEncrypt").target().setText(original));
		robot.waitForIdle();
		clickButton(frame, "btnEncrypt");
		String encrypted = GuiActionRunner
			.execute(() -> frame.textBox("txtEncrypted").target().getText());
		assertTrue(!encrypted.isEmpty() && !encrypted.equals(original),
			"the encrypted hex must be non-empty and differ from the plain text");

		// decrypt it back with the generated private key
		clickButton(frame, "btnDecrypt");
		String decrypted = GuiActionRunner
			.execute(() -> frame.textBox("txtToEncrypt").target().getText());
		assertEquals(original, decrypted, "decrypt must return the original text through the UI");
	}

	private void clickButton(FrameFixture frame, String name)
	{
		GuiActionRunner.execute(() -> frame.button(name).target().doClick());
		robot.waitForIdle();
		UiTestSpeed.step();
	}
}
