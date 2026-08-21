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
package io.github.astrapi69.mystic.crypt.plugin.conversion;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.File;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;

/**
 * Headless proof of the DER-to-PEM conversion the plugin's {@link FileConversionPanel} performs for
 * a private key: read the DER file with {@link PrivateKeyReader#readPrivateKey(File)} and write it
 * out with {@link PrivateKeyWriter#writeInPemFormat} - exactly the two calls the panel makes. The
 * key read back from the produced PEM must be byte-for-byte the original key
 */
class DerToPemConversionTest
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
	void privateKeyConvertedFromDerToPemStaysIdentical(@TempDir File tempDir) throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);
		PrivateKey originalKey = keyPair.getPrivate();

		File derFile = new File(tempDir, "key.der");
		PrivateKeyWriter.write(originalKey, derFile);

		// the two calls the panel makes for KeyType.PRIVATE_KEY
		PrivateKey keyFromDer = PrivateKeyReader.readPrivateKey(derFile);
		File pemFile = new File(tempDir, "key.pem");
		PrivateKeyWriter.writeInPemFormat(keyFromDer, pemFile);

		PrivateKey keyFromPem = PrivateKeyReader.readPemPrivateKey(pemFile);

		assertArrayEquals(originalKey.getEncoded(), keyFromDer.getEncoded(),
			"the key read from the DER file must equal the original");
		assertArrayEquals(originalKey.getEncoded(), keyFromPem.getEncoded(),
			"the key read back from the produced PEM must equal the original");
	}
}
