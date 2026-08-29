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
import java.nio.file.Files;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.key.KeySize;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypt.data.key.reader.PublicKeyReader;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * "Generate keys" has to work for every algorithm the box offers, not only for RSA, and what it
 * shows has to be a real key: the PEM the window puts on screen is written to disk here and read
 * back with the library's own readers, so a text that merely looks like a key fails the test.
 * <p>
 * One window for all six: the tool opens in the running application, and opening it once per
 * algorithm would leave several of them on the desktop with the same component names.
 */
class KeygenEveryAlgorithmUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final KeyPairGeneratorAlgorithm[] OFFERED = { KeyPairGeneratorAlgorithm.RSA,
			KeyPairGeneratorAlgorithm.EC, KeyPairGeneratorAlgorithm.X25519,
			KeyPairGeneratorAlgorithm.X448, KeyPairGeneratorAlgorithm.ML_KEM_768,
			KeyPairGeneratorAlgorithm.ML_DSA_65 };

	@Test
	@DisplayName("every algorithm the box offers generates a real key pair")
	void everyAlgorithmTheBoxOffersGeneratesARealKeyPair() throws Exception
	{
		installPluginRequiringItBuilt(KEYGEN_ZIP);
		File databaseFile = new File(tempHome, "keygen-algorithms.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Key Generation", "Key generation demo");

		// where the panels sit before anything is generated; the window is built to be tried out
		// algorithm by algorithm, so nothing in it may move while that is done
		int encryptPanelLeftEdge = leftEdgeOf(frame, "txtToEncrypt");
		int keyAreaLeftEdge = leftEdgeOf(frame, "txtPrivateKey");

		for (KeyPairGeneratorAlgorithm algorithm : OFFERED)
		{
			generateAndCheck(frame, algorithm);
			assertEquals(encryptPanelLeftEdge, leftEdgeOf(frame, "txtToEncrypt"),
				"the encrypt panel moved after generating with " + algorithm);
			assertEquals(keyAreaLeftEdge, leftEdgeOf(frame, "txtPrivateKey"),
				"the key areas moved after generating with " + algorithm);
		}

		// a longer key writes longer lines into the key areas, and a form whose columns follow
		// what happens to be in them moves while it is being used
		GuiActionRunner.execute(() -> frame.comboBox("cmbAlgorithm").target()
			.setSelectedItem(KeyPairGeneratorAlgorithm.RSA));
		robot.waitForIdle();
		for (KeySize keySize : new KeySize[] { KeySize.KEYSIZE_1024, KeySize.KEYSIZE_2048,
				KeySize.KEYSIZE_4096 })
		{
			GuiActionRunner
				.execute(() -> frame.comboBox("cmbKeySize").target().setSelectedItem(keySize));
			robot.waitForIdle();
			GuiActionRunner.execute(() -> frame.button("btnGenerate").target().doClick());
			robot.waitForIdle();

			assertEquals(encryptPanelLeftEdge, leftEdgeOf(frame, "txtToEncrypt"),
				"the encrypt panel moved after generating a " + keySize + " key");
			assertEquals(keyAreaLeftEdge, leftEdgeOf(frame, "txtPrivateKey"),
				"the key areas moved after generating a " + keySize + " key");
		}
	}

	/**
	 * Where the named text area starts on screen
	 *
	 * @param frame
	 *            the application frame
	 * @param name
	 *            the component name
	 * @return the left edge in screen coordinates
	 */
	private int leftEdgeOf(final FrameFixture frame, final String name)
	{
		return GuiActionRunner.execute(() -> frame.textBox(name).target().getLocationOnScreen().x);
	}

	/**
	 * Picks the given algorithm in the window, presses generate, and reads what appears back in as
	 * a key
	 *
	 * @param frame
	 *            the application frame
	 * @param algorithm
	 *            the algorithm to generate with
	 */
	private void generateAndCheck(final FrameFixture frame,
		final KeyPairGeneratorAlgorithm algorithm) throws Exception
	{
		GuiActionRunner
			.execute(() -> frame.comboBox("cmbAlgorithm").target().setSelectedItem(algorithm));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnGenerate").target().doClick());
		robot.waitForIdle();

		String privateKeyPem = GuiActionRunner
			.execute(() -> frame.textBox("txtPrivateKey").target().getText());
		String publicKeyPem = GuiActionRunner
			.execute(() -> frame.textBox("txtPublicKey").target().getText());

		assertTrue(privateKeyPem.contains("PRIVATE KEY"),
			"no private key on screen for " + algorithm + ", it shows: " + privateKeyPem);
		assertTrue(publicKeyPem.contains("PUBLIC KEY"),
			"no public key on screen for " + algorithm + ", it shows: " + publicKeyPem);

		// what is on screen has to be a key the library can read back, not a text that looks like
		// one - written out and read in again with the readers the rest of the application uses
		File privateKeyFile = new File(tempHome, "on-screen-private-" + algorithm + ".pem");
		File publicKeyFile = new File(tempHome, "on-screen-public-" + algorithm + ".pem");
		Files.writeString(privateKeyFile.toPath(), privateKeyPem);
		Files.writeString(publicKeyFile.toPath(), publicKeyPem);

		PrivateKey privateKey = PrivateKeyReader.readPemPrivateKey(privateKeyFile);
		PublicKey publicKey = PublicKeyReader.readPemPublicKey(publicKeyFile);
		assertEquals(privateKey.getAlgorithm(), publicKey.getAlgorithm(),
			"the two keys on screen do not belong to the same algorithm for " + algorithm);
	}

}
