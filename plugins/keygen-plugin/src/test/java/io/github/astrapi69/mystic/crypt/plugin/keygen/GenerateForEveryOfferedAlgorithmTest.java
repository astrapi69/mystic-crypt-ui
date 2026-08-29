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
package io.github.astrapi69.mystic.crypt.plugin.keygen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;

/**
 * Pressing "Generate keys" has to produce a key pair for every algorithm the box offers, not only
 * for RSA. The existing round trip test builds key pairs through the factory directly, which says
 * nothing about the button: the panel picks the key size, the curve and the writer per algorithm,
 * and every one of those steps can refuse an algorithm on its own.
 */
class GenerateForEveryOfferedAlgorithmTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@ParameterizedTest(name = "pressing generate with {0} fills both key areas")
	@EnumSource(value = KeyPairGeneratorAlgorithm.class, names = { "RSA", "EC", "X25519", "X448",
			"ML_KEM_768", "ML_DSA_65" })
	void pressingGenerateFillsBothKeyAreas(final KeyPairGeneratorAlgorithm algorithm)
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();
		panel.getCmbAlgorithm().setSelectedItem(algorithm);

		panel.getCryptographyPanel().getBtnGenerate().doClick();

		assertNotNull(panel.getModelObject().getPrivateKey(),
			"no private key was generated for " + algorithm);
		assertNotNull(panel.getModelObject().getPublicKey(),
			"no public key was generated for " + algorithm);
		String privateKeyText = panel.getCryptographyPanel().getTxtPrivateKey().getText();
		String publicKeyText = panel.getCryptographyPanel().getTxtPublicKey().getText();
		assertTrue(privateKeyText.contains("PRIVATE KEY"),
			"the private key area shows no key for " + algorithm + ", it shows: " + privateKeyText);
		assertTrue(publicKeyText.contains("PUBLIC KEY"),
			"the public key area shows no key for " + algorithm + ", it shows: " + publicKeyText);
		assertFalse(privateKeyText.startsWith("Generating"),
			"the panel is still saying it generates, so generation failed for " + algorithm);
	}

}
