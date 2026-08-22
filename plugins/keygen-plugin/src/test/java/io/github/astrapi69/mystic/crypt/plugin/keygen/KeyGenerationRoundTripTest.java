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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.key.KeySize;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.PrivateKeyExtensions;
import io.github.astrapi69.crypt.data.key.PublicKeyExtensions;
import io.github.astrapi69.mystic.crypt.key.PrivateKeyHexDecryptor;
import io.github.astrapi69.mystic.crypt.key.PublicKeyHexEncryptor;

/**
 * Headless proof of the key-generation demo's core: {@link GenerateKeysPanel} generates an RSA key
 * pair and builds a {@link PublicKeyHexEncryptor}/{@link PrivateKeyHexDecryptor} from it, then its
 * Encrypt/Decrypt buttons run text through them. This test performs exactly that generate +
 * encrypt/decrypt round trip and asserts the decrypted text equals the original
 */
class KeyGenerationRoundTripTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@Test
	void generatedKeyPairEncryptsAndDecryptsBackToTheOriginalText() throws Exception
	{
		// what onGenerate does
		KeyPair keyPair = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA,
			KeySize.KEYSIZE_2048.getKeySize());
		assertNotNull(keyPair.getPrivate());
		assertNotNull(keyPair.getPublic());

		PublicKeyHexEncryptor encryptor = new PublicKeyHexEncryptor(keyPair.getPublic());
		PrivateKeyHexDecryptor decryptor = new PrivateKeyHexDecryptor(keyPair.getPrivate());

		String original = "the quick brown fox jumps over the lazy dog";
		// what onEncrypt does
		String encrypted = encryptor.encrypt(original);
		assertNotEquals(original, encrypted, "the encrypted hex must differ from the plain text");

		// what onDecrypt does
		String decrypted = decryptor.decrypt(encrypted);
		assertEquals(original, decrypted, "decrypting the hex must return the original text");
	}

	/**
	 * Proves the modern-algorithm branch of {@link GenerateKeysPanel#onGenerate}: each of the
	 * curated modern algorithms (the X25519/X448 key-agreement curves and the post-quantum ML-KEM /
	 * ML-DSA parameter sets) generates a key pair whose private and public keys serialize to PEM,
	 * exactly what the panel writes into its two text areas
	 *
	 * @param algorithm
	 *            the modern key-pair algorithm under test
	 */
	@ParameterizedTest
	@EnumSource(value = KeyPairGeneratorAlgorithm.class, names = { "X25519", "X448", "ML_KEM_768",
			"ML_DSA_65" })
	void modernAlgorithmsGenerateAndSerializeToPem(KeyPairGeneratorAlgorithm algorithm)
		throws Exception
	{
		// what onGenerate does for a non-RSA algorithm: no classical key size
		KeyPair keyPair = KeyPairFactory.newKeyPair(algorithm);
		assertNotNull(keyPair.getPrivate());
		assertNotNull(keyPair.getPublic());

		String privateKeyPem = PrivateKeyExtensions.toPemFormat(keyPair.getPrivate());
		String publicKeyPem = PublicKeyExtensions.toPemFormat(keyPair.getPublic());

		assertTrue(privateKeyPem.contains("PRIVATE KEY"),
			"the generated " + algorithm + " private key must serialize to PEM");
		assertTrue(publicKeyPem.contains("PUBLIC KEY"),
			"the generated " + algorithm + " public key must serialize to PEM");
	}
}
