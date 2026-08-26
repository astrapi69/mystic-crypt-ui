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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.Security;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.SecretKey;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.crypt.api.key.KeyFormat;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.mystic.crypt.crypto.KeyFiles;

/**
 * Tests of what the key generation tool produces: a key on the curve that was asked for, a
 * symmetric key of the length that was asked for, random bytes, and a private key file in the
 * format the rest of the world expects.
 */
class KeygenSupportTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	static java.util.List<String> curves()
	{
		return KeygenSupport.CURVES;
	}

	/** Every curve the tool offers has to exist on this machine and be the one that comes back */
	@ParameterizedTest
	@MethodSource("curves")
	void generatesAKeyOnTheCurveThatWasAskedFor(String curve) throws Exception
	{
		KeyPair keyPair = KeygenSupport.newEcKeyPair(curve);

		assertEquals("EC", keyPair.getPrivate().getAlgorithm());
		assertEquals(curve, KeygenSupport.curveOf(keyPair),
			"a certificate or a wallet usually requires one particular curve, so it has to be the "
				+ "one that was chosen");
	}

	@Test
	void twoKeysOnTheSameCurveAreStillDifferentKeys() throws Exception
	{
		KeyPair first = KeygenSupport.newEcKeyPair("secp256r1");
		KeyPair second = KeygenSupport.newEcKeyPair("secp256r1");

		assertNotEquals(first.getPrivate(), second.getPrivate());
	}

	@ParameterizedTest
	@ValueSource(strings = { "not a curve", "secp256r99", "" })
	void aCurveThisMachineDoesNotKnowIsRefused(String curve)
	{
		assertThrows(Exception.class, () -> KeygenSupport.newEcKeyPair(curve));
	}

	@Test
	void aKeyThatSitsOnNoNamedCurveSaysSo() throws Exception
	{
		assertEquals("", KeygenSupport.curveOf(KeyPairFactory.newKeyPair("RSA")),
			"an RSA key has no curve to report");
	}

	@ParameterizedTest
	@CsvSource({ "AES,128,16", "AES,192,24", "AES,256,32", "ChaCha20,256,32", "HmacSHA256,256,32",
			"HmacSHA512,512,64" })
	void generatesASymmetricKeyOfTheLengthThatWasAskedFor(String algorithm, int keySize,
		int expectedBytes) throws Exception
	{
		SecretKey key = KeygenSupport.newSecretKey(algorithm, keySize);

		assertEquals(algorithm, key.getAlgorithm());
		assertEquals(expectedBytes, key.getEncoded().length);
	}

	@Test
	void aSymmetricKeyIsDifferentEveryTime() throws Exception
	{
		Set<String> seen = new HashSet<>();
		for (int run = 0; run < 5; run++)
		{
			seen.add(KeygenSupport.toHex(KeygenSupport.newSecretKey("AES", 256).getEncoded()));
		}

		assertEquals(5, seen.size(), "a key generator that repeats itself is not one");
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 12, 16, 32, 64, 1024 })
	void producesExactlyAsManyRandomBytesAsAskedFor(int length)
	{
		assertEquals(length, KeygenSupport.randomBytes(length).length);
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, -1 })
	void askingForNoBytesIsRefused(int length)
	{
		assertThrows(IllegalArgumentException.class, () -> KeygenSupport.randomBytes(length));
	}

	@Test
	void randomBytesAreDifferentEveryTime()
	{
		Set<String> seen = new HashSet<>();
		for (int run = 0; run < 5; run++)
		{
			seen.add(KeygenSupport.toHex(KeygenSupport.randomBytes(32)));
		}

		assertEquals(5, seen.size());
	}

	@Test
	void writesBytesTheWayTheyAreUsuallyWrittenDown()
	{
		byte[] bytes = { 0x00, 0x01, (byte)0xab, (byte)0xff };

		assertEquals("0001abff", KeygenSupport.toHex(bytes));
		assertEquals("AAGr/w==", KeygenSupport.toBase64(bytes));
		assertArrayEquals(bytes, java.util.Base64.getDecoder()
			.decode(KeygenSupport.toBase64(bytes)));
	}

	private static void assertArrayEquals(byte[] expected, byte[] actual)
	{
		assertTrue(Arrays.equals(expected, actual),
			"expected " + Arrays.toString(expected) + " but was " + Arrays.toString(actual));
	}

	@ParameterizedTest
	@EnumSource(value = KeyFormat.class, names = { "PKCS_1", "PKCS_8" })
	void writesAPrivateKeyInBothFormatsAndReadsItBack(KeyFormat format, @TempDir File directory)
		throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		File file = new File(directory, "private-" + format + ".pem");

		KeygenSupport.writePrivateKey(keyPair.getPrivate(), file, format);

		String pem = Files.readString(file.toPath());
		assertTrue(pem.contains("PRIVATE KEY"), pem.lines().findFirst().orElse(""));
		if (format == KeyFormat.PKCS_1)
		{
			assertTrue(pem.contains("BEGIN RSA PRIVATE KEY"),
				"PKCS#1 is what openssl and nginx expect, and it says RSA in its header: "
					+ pem.lines().findFirst().orElse(""));
		}
		else
		{
			assertTrue(pem.contains("BEGIN PRIVATE KEY"),
				pem.lines().findFirst().orElse(""));
			assertFalse(pem.contains("BEGIN RSA PRIVATE KEY"));
		}
		assertEquals(keyPair.getPrivate(), KeyFiles.readPrivateKey(file),
			"whatever was written has to be readable again, and be the same key");
	}

	@Test
	void saysWhatItOffersAndWhatTheFormatsAreCalled()
	{
		assertEquals(2, KeygenSupport.keyFormats().size());
		assertTrue(KeygenSupport.displayName(KeyFormat.PKCS_1).contains("openssl"));
		assertTrue(KeygenSupport.displayName(KeyFormat.PKCS_8).contains("Java"));
		assertTrue(KeygenSupport.CURVES.contains("secp256k1"),
			"the curve a wallet uses has to be among them");
		assertTrue(KeygenSupport.SECRET_KEY_SIZES.contains(256));
		assertTrue(KeygenSupport.RANDOM_LENGTHS.contains(12), "a GCM nonce is twelve bytes");
	}
}
