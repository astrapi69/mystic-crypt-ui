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

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import io.github.astrapi69.crypt.data.key.reader.CertificateReader;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypt.data.key.reader.PublicKeyReader;

/**
 * Reading a key or a certificate out of a file, whatever shape it happens to be in.
 * <p>
 * There are more of those shapes than one would like: PKCS#8 ("BEGIN PRIVATE KEY"), the openssl
 * styles ("BEGIN RSA PRIVATE KEY", "BEGIN EC PRIVATE KEY"), a file carrying both halves of a pair,
 * the same again as raw DER, and a public key that arrives inside a certificate rather than on its
 * own. On top of that the provider matters: the keys this application generates are Bouncy Castle
 * keys, on curves the provider the JDK picks by default cannot decode - an EC key then fails with
 * "Unable to decode key" for no reason a user could act on.
 * <p>
 * So everything here decodes with Bouncy Castle, which reads the JDK's own keys as well, and every
 * method fails by saying which file could not be read rather than by returning null.
 */
public final class KeyFiles
{

	/**
	 * The key types tried in turn when a file does not say which one it holds - every algorithm the
	 * key generation window offers has to be in here, or its DER-saved keys come back unreadable
	 * (#102)
	 */
	private static final List<String> KEY_ALGORITHMS = List.of("RSA", "EC", "DSA", "EdDSA",
		"Ed25519", "X25519", "X448", "ML-KEM-768", "ML-DSA-65");

	private KeyFiles()
	{
	}

	/**
	 * Reads a private key from a file
	 *
	 * @param file
	 *            the key file, PEM or DER
	 * @return the private key
	 * @throws Exception
	 *             if the file holds no private key this application can read
	 */
	public static PrivateKey readPrivateKey(final File file) throws Exception
	{
		PrivateKey fromPemParser = readPemPrivateKey(file);
		if (fromPemParser != null)
		{
			return fromPemParser;
		}
		byte[] encoded = encodedKey(file);
		for (String algorithm : KEY_ALGORITHMS)
		{
			try
			{
				return keyFactory(algorithm).generatePrivate(new PKCS8EncodedKeySpec(encoded));
			}
			catch (Exception nextOne)
			{
				// try the next key type
			}
		}
		// last resort: the library's own reader, in case it knows a shape this does not
		PrivateKey fromLibrary = PrivateKeyReader.isPemFormat(file)
			? PrivateKeyReader.readPemPrivateKey(file)
			: PrivateKeyReader.readPrivateKey(file);
		if (fromLibrary == null)
		{
			throw new IllegalArgumentException(
				"'" + file + "' holds no private key this application can read");
		}
		return fromLibrary;
	}

	/**
	 * Reads a public key from a file: one of its own, or the one inside a certificate
	 *
	 * @param file
	 *            the file, PEM or DER
	 * @return the public key
	 * @throws Exception
	 *             if the file holds neither a public key nor a certificate
	 */
	public static PublicKey readPublicKey(final File file) throws Exception
	{
		byte[] encoded = null;
		try
		{
			encoded = encodedKey(file);
		}
		catch (Exception notEvenBase64)
		{
			// leave it to the certificate reader below
		}
		if (encoded != null)
		{
			for (String algorithm : KEY_ALGORITHMS)
			{
				try
				{
					return keyFactory(algorithm).generatePublic(new X509EncodedKeySpec(encoded));
				}
				catch (Exception nextOne)
				{
					// try the next key type
				}
			}
		}
		try
		{
			PublicKey fromLibrary = PublicKeyReader.readPublicKey(file);
			if (fromLibrary != null)
			{
				return fromLibrary;
			}
		}
		catch (Exception notAPublicKeyFile)
		{
			// fall through to the certificate
		}
		return readCertificate(file).getPublicKey();
	}

	/**
	 * Reads a certificate from a file
	 *
	 * @param file
	 *            the certificate file, PEM or DER
	 * @return the certificate
	 * @throws Exception
	 *             if the file holds no certificate
	 */
	public static X509Certificate readCertificate(final File file) throws Exception
	{
		X509Certificate certificate = CertificateReader.readCertificate(file);
		if (certificate == null)
		{
			throw new IllegalArgumentException("'" + file + "' holds no certificate");
		}
		return certificate;
	}

	/**
	 * Reads a private key with Bouncy Castle's own pem parser, the one thing that knows every shape
	 * a private key file comes in
	 *
	 * @param file
	 *            the key file
	 * @return the private key, or {@code null} when this file is not one
	 */
	private static PrivateKey readPemPrivateKey(final File file)
	{
		try (Reader reader = Files.newBufferedReader(file.toPath());
			PEMParser parser = new PEMParser(reader))
		{
			Object parsed = parser.readObject();
			JcaPEMKeyConverter converter = new JcaPEMKeyConverter()
				.setProvider(BouncyCastleProvider.PROVIDER_NAME);
			if (parsed instanceof PEMKeyPair keyPair)
			{
				return converter.getKeyPair(keyPair).getPrivate();
			}
			if (parsed instanceof PrivateKeyInfo privateKeyInfo)
			{
				return converter.getPrivateKey(privateKeyInfo);
			}
			return null;
		}
		catch (Exception notReadableThisWay)
		{
			return null;
		}
	}

	private static KeyFactory keyFactory(final String algorithm) throws Exception
	{
		return KeyFactory.getInstance(algorithm, BouncyCastleProvider.PROVIDER_NAME);
	}

	/**
	 * The raw bytes of a key file: the Base64 body for a PEM file, the file itself for a DER one
	 */
	private static byte[] encodedKey(final File file) throws Exception
	{
		if (!PrivateKeyReader.isPemFormat(file))
		{
			return Files.readAllBytes(file.toPath());
		}
		String base64 = Files.readAllLines(file.toPath()).stream()
			.filter(line -> !line.startsWith("-----")).reduce("", String::concat)
			.replaceAll("\\s", "");
		return Base64.getDecoder().decode(base64);
	}
}
