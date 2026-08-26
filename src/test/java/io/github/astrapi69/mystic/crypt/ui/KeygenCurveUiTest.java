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
import java.security.PublicKey;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.crypto.KeyFiles;

/**
 * Functional end-to-end test of generating an EC key on a chosen curve: a certificate or a wallet
 * usually requires one particular curve, so the one that was picked has to be the one the key sits
 * on - not whatever the provider felt like.
 */
class KeygenCurveUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	void generatesAnEcKeyOnTheChosenCurveThroughTheUi() throws Exception
	{
		installPluginRequiringItBuilt(KEYGEN_ZIP);

		File databaseFile = new File(tempHome, "keygen-curve.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Key Generation", "Key generation demo");

		GuiActionRunner.execute(() -> {
			frame.comboBox("cmbAlgorithm").target().setSelectedItem(KeyPairGeneratorAlgorithm.EC);
			frame.comboBox("cmbCurve").target().setSelectedItem("secp256k1");
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnGenerate").target().doClick());
		robot.waitForIdle();

		String publicKeyPem = GuiActionRunner
			.execute(() -> frame.textBox("txtPublicKey").target().getText());
		assertTrue(publicKeyPem.contains("PUBLIC KEY"),
			"the generated key must be shown as PEM, was: " + publicKeyPem);

		File publicKeyFile = new File(tempHome, "generated-public.pem");
		Files.writeString(publicKeyFile.toPath(), publicKeyPem);
		PublicKey publicKey = KeyFiles.readPublicKey(publicKeyFile);

		assertEquals("EC", publicKey.getAlgorithm());
		assertEquals("secp256k1", curveOf(publicKey),
			"the key must sit on the curve that was picked, not on the provider's default");
	}

	/**
	 * The curve of a key, read here rather than through the plugin class: the plugin runs in its
	 * own class loader, so the test cannot reach into it
	 */
	private String curveOf(PublicKey publicKey)
	{
		if (publicKey instanceof org.bouncycastle.jce.interfaces.ECPublicKey ecPublicKey
			&& ecPublicKey
				.getParameters()instanceof org.bouncycastle.jce.spec.ECNamedCurveParameterSpec named)
		{
			return named.getName();
		}
		return "";
	}
}
