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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.security.Security;
import java.util.UUID;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.key.KeySize;
import io.github.astrapi69.crypt.data.key.reader.EncryptedPrivateKeyReader;

/**
 * Drives the key generation window the way a user drives it: through its buttons. What the buttons
 * do is what these tests pin - the round trip through the encryptor objects is covered elsewhere
 * and says nothing about whether pressing the button ever reaches them.
 */
class KeyGenerationButtonsTest
{

	private static final String PLAIN_TEXT = "the text that goes in";

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private static GenerateKeysPanel panelWithAKeyPairOf(KeyPairGeneratorAlgorithm algorithm)
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();
		panel.getCmbAlgorithm().setSelectedItem(algorithm);
		panel.getCryptographyPanel().getCmbKeySize().setSelectedItem(KeySize.KEYSIZE_1024);
		panel.getCryptographyPanel().getBtnGenerate().doClick();
		return panel;
	}

	@Test
	@DisplayName("pressing encrypt puts cipher text into the right area")
	void pressingEncryptPutsCipherTextIntoTheRightArea()
	{
		GenerateKeysPanel panel = panelWithAKeyPairOf(KeyPairGeneratorAlgorithm.RSA);
		panel.getEnDecryptPanel().getTxtToEncrypt().setText(PLAIN_TEXT);

		panel.getEnDecryptPanel().getBtnEncrypt().doClick();

		String encrypted = panel.getEnDecryptPanel().getTxtEncrypted().getText();
		assertFalse(encrypted.isEmpty(), "pressing encrypt left the encrypted area empty");
		assertFalse(encrypted.contains(PLAIN_TEXT), "the text was not encrypted at all");
	}

	@Test
	@DisplayName("pressing decrypt brings the same text back")
	void pressingDecryptBringsTheSameTextBack()
	{
		GenerateKeysPanel panel = panelWithAKeyPairOf(KeyPairGeneratorAlgorithm.RSA);
		panel.getEnDecryptPanel().getTxtToEncrypt().setText(PLAIN_TEXT);
		panel.getEnDecryptPanel().getBtnEncrypt().doClick();

		panel.getEnDecryptPanel().getBtnDecrypt().doClick();

		assertEquals(PLAIN_TEXT, panel.getEnDecryptPanel().getTxtToEncrypt().getText(),
			"the text did not come back through the decrypt button");
	}

	@Test
	@DisplayName("the encrypt button is off for an algorithm that has no hex demo, and says why")
	void theEncryptButtonIsOffForAnAlgorithmThatHasNoHexDemo()
	{
		GenerateKeysPanel panel = panelWithAKeyPairOf(KeyPairGeneratorAlgorithm.EC);
		panel.getEnDecryptPanel().getTxtToEncrypt().setText(PLAIN_TEXT);

		assertFalse(panel.getEnDecryptPanel().getBtnEncrypt().isEnabled(),
			"encrypt offers itself for an algorithm it cannot serve");
		String reason = panel.getEnDecryptPanel().getBtnEncrypt().getToolTipText();
		assertNotNull(reason, "a button that is off must say why");
		assertFalse(reason.isBlank(), "a button that is off must say why");
	}

	@Test
	@DisplayName("the encrypt button is on again after an RSA key pair is generated")
	void theEncryptButtonIsOnAgainAfterAnRsaKeyPairIsGenerated()
	{
		GenerateKeysPanel panel = panelWithAKeyPairOf(KeyPairGeneratorAlgorithm.EC);
		panel.getCmbAlgorithm().setSelectedItem(KeyPairGeneratorAlgorithm.RSA);
		panel.getCryptographyPanel().getBtnGenerate().doClick();

		panel.getEnDecryptPanel().getTxtToEncrypt().setText(PLAIN_TEXT);

		assertTrue(panel.getEnDecryptPanel().getBtnEncrypt().isEnabled(),
			"encrypt stayed off although the key pair it needs was generated");
	}

	@Test
	@DisplayName("saving with a password writes a key that only that password opens")
	void savingWithAPasswordWritesAKeyThatOnlyThatPasswordOpens(@TempDir File directory)
		throws Exception
	{
		GenerateKeysPanel panel = panelWithAKeyPairOf(KeyPairGeneratorAlgorithm.RSA);
		File keyFile = new File(directory, "protected.pem");
		String password = "pw-" + UUID.randomUUID();

		panel.savePrivateKeyWithPasswordTo(keyFile, password);

		assertTrue(keyFile.exists(), "the protected private key was not written at all");
		assertEquals(panel.getModelObject().getPrivateKey(),
			EncryptedPrivateKeyReader.readPasswordProtectedPrivateKey(keyFile, password),
			"the key that comes back with the password is not the key that was generated");
	}

}
