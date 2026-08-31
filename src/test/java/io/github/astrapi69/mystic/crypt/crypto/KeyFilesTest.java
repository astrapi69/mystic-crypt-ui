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
package io.github.astrapi69.mystic.crypt.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.key.KeyFileFormat;
import io.github.astrapi69.crypt.api.key.KeyFormat;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;

/**
 * The key generation window offers RSA, EC, X25519, X448, ML-KEM-768 and ML-DSA-65, saved as PEM or
 * DER - and {@link KeyFiles#readPrivateKey} has to read every one of those back, not only the ones
 * its own fixed algorithm list happened to name (#102).
 */
class KeyFilesTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@ParameterizedTest(name = "{0} as DER")
	@EnumSource(value = KeyPairGeneratorAlgorithm.class, names = { "RSA", "EC", "X25519", "X448",
			"ML_KEM_768", "ML_DSA_65" })
	void readsBackAPrivateKeySavedAsDerForEveryAlgorithmTheWindowOffers(
		KeyPairGeneratorAlgorithm algorithm, @TempDir File directory) throws Exception
	{
		// EC is generated on an explicit curve with the Bouncy Castle provider, the same way the
		// window itself does it (KeygenSupport#newEcKeyPair) - the plain 1-arg factory call leaves
		// the provider to the JDK, which for EC is SunEC, not the provider this application reads
		// DER files with
		KeyPair keyPair = algorithm == KeyPairGeneratorAlgorithm.EC
			? KeyPairFactory.newKeyPair("secp256r1", "EC", BouncyCastleProvider.PROVIDER_NAME)
			: KeyPairFactory.newKeyPair(algorithm);
		File file = new File(directory, "private-" + algorithm + ".der");
		try (OutputStream out = Files.newOutputStream(file.toPath()))
		{
			PrivateKeyWriter.write(keyPair.getPrivate(), out, KeyFileFormat.DER, KeyFormat.PKCS_8);
		}

		assertEquals(keyPair.getPrivate(), KeyFiles.readPrivateKey(file),
			"a " + algorithm + " private key saved as DER must read back as the key that was "
				+ "generated, not throw");
	}

}
