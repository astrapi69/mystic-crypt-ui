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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.security.KeyPair;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;

/**
 * The hex encrypt/decrypt demo used to be RSA only, because encrypting with the public key itself
 * is an RSA operation and EC has no such primitive (#195). Since mystic-crypt 12.2 the library
 * carries ECIES, so an EC key pair can drive the same demo - this proves the pairing per algorithm
 * and a real round trip on both.
 */
class HexEnDecryptionForAlgorithmTest
{

	@BeforeAll
	static void addBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@Test
	@DisplayName("an RSA key pair still round trips through the hex demo")
	void rsaRoundTrips() throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);

		String encrypted = KeygenSupport
			.newHexEncryptor(KeyPairGeneratorAlgorithm.RSA, keyPair.getPublic())
			.encrypt("the secret");

		assertNotEquals("the secret", encrypted);
		assertEquals("the secret", KeygenSupport
			.newHexDecryptor(KeyPairGeneratorAlgorithm.RSA, keyPair.getPrivate())
			.decrypt(encrypted));
	}

	@Test
	@DisplayName("an EC key pair round trips through the hex demo as well")
	void ecRoundTrips() throws Exception
	{
		KeyPair keyPair = KeygenSupport.newEcKeyPair("secp256r1");

		String encrypted = KeygenSupport
			.newHexEncryptor(KeyPairGeneratorAlgorithm.EC, keyPair.getPublic())
			.encrypt("the secret");

		assertNotEquals("the secret", encrypted);
		assertEquals("the secret",
			KeygenSupport.newHexDecryptor(KeyPairGeneratorAlgorithm.EC, keyPair.getPrivate())
				.decrypt(encrypted));
	}

	@Test
	@DisplayName("RSA and EC are the two algorithms the demo can be built for")
	void rsaAndEcHaveAnEncryptor() throws Exception
	{
		assertNotNull(KeygenSupport.newHexEncryptor(KeyPairGeneratorAlgorithm.RSA,
			KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048).getPublic()));
		assertNotNull(KeygenSupport.newHexEncryptor(KeyPairGeneratorAlgorithm.EC,
			KeygenSupport.newEcKeyPair("secp256r1").getPublic()));
	}

	/**
	 * X25519/X448 are key agreement, ML-KEM-768 is a pure KEM and ML-DSA-65 is a signature scheme -
	 * none of them has an encrypt-with-the-public-key primitive at all, so the demo stays out of
	 * reach for them and must say so rather than fail late
	 */
	@ParameterizedTest(name = "{0} has no hex encrypt/decrypt demo")
	@EnumSource(value = KeyPairGeneratorAlgorithm.class, names = { "RSA", "EC" }, mode = EnumSource.Mode.EXCLUDE)
	void everyOtherAlgorithmHasNone(final KeyPairGeneratorAlgorithm algorithm)
	{
		assertNull(KeygenSupport.newHexEncryptor(algorithm, null));
		assertNull(KeygenSupport.newHexDecryptor(algorithm, null));
	}
}
