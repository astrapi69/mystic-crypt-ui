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

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.github.astrapi69.crypt.api.key.KeyFileFormat;
import io.github.astrapi69.crypt.api.key.KeyFormat;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.random.number.RandomByteFactory;

/**
 * What the key generation tool does beyond producing a key pair of a fixed kind: choosing the curve
 * an EC key sits on, generating a symmetric key, producing raw random bytes, and writing a private
 * key in either of the two formats that exist.
 * <p>
 * Free of Swing, so all of it can be tested without a display.
 */
public final class KeygenSupport
{

	/** The curves offered for an EC key, the ones in actual use rather than all thirty */
	public static final List<String> CURVES = List.of("secp256r1", "secp384r1", "secp521r1",
		"secp256k1", "secp224r1", "brainpoolP256r1", "brainpoolP384r1", "brainpoolP512r1");

	/** The symmetric algorithms a key can be generated for */
	public static final List<String> SECRET_KEY_ALGORITHMS = List.of("AES", "ChaCha20", "HmacSHA256",
		"HmacSHA512");

	/** The lengths offered for a symmetric key */
	public static final List<Integer> SECRET_KEY_SIZES = List.of(128, 192, 256, 512);

	/** The lengths offered for a block of random bytes */
	public static final List<Integer> RANDOM_LENGTHS = List.of(12, 16, 24, 32, 64);

	private KeygenSupport()
	{
	}

	/**
	 * Generates an EC key pair on a named curve. Without naming one the provider decides, and what
	 * it decides is neither written down nor the same everywhere - which matters, because a
	 * certificate or a wallet usually requires one particular curve
	 *
	 * @param curve
	 *            the curve, for instance secp256k1
	 * @return the key pair
	 * @throws Exception
	 *             if the curve is not one this machine knows
	 */
	public static KeyPair newEcKeyPair(final String curve) throws Exception
	{
		return KeyPairFactory.newKeyPair(curve, "EC", BouncyCastleProvider.PROVIDER_NAME);
	}

	/**
	 * The curve an EC key sits on, as the key itself reports it
	 *
	 * @param keyPair
	 *            the key pair to look at
	 * @return the name of the curve, or an empty string when it is not an EC key or does not say
	 */
	public static String curveOf(final KeyPair keyPair)
	{
		return curveOf(keyPair.getPublic());
	}

	/**
	 * The curve a public key sits on, as the key itself reports it
	 *
	 * @param publicKey
	 *            the key to look at
	 * @return the name of the curve, or an empty string when it is not an EC key or does not say
	 */
	public static String curveOf(final java.security.PublicKey publicKey)
	{
		if (publicKey instanceof org.bouncycastle.jce.interfaces.ECPublicKey ecPublicKey
			&& ecPublicKey.getParameters() instanceof org.bouncycastle.jce.spec.ECNamedCurveParameterSpec named)
		{
			return named.getName();
		}
		return "";
	}

	/**
	 * Generates a symmetric key
	 *
	 * @param algorithm
	 *            the algorithm, for instance AES
	 * @param keySize
	 *            the length in bits
	 * @return the key
	 * @throws Exception
	 *             if this machine cannot generate such a key
	 */
	public static SecretKey newSecretKey(final String algorithm, final int keySize) throws Exception
	{
		KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm);
		keyGenerator.init(keySize);
		return keyGenerator.generateKey();
	}

	/**
	 * Random bytes, for a nonce, an initialisation vector, a salt or a key for something that takes
	 * raw bytes
	 *
	 * @param length
	 *            how many bytes
	 * @return the bytes
	 */
	public static byte[] randomBytes(final int length)
	{
		if (length < 1)
		{
			throw new IllegalArgumentException("ask for at least one byte");
		}
		return RandomByteFactory.randomByteArray(length);
	}

	/**
	 * Bytes as hex, the way a nonce or a key is usually written down
	 *
	 * @param bytes
	 *            the bytes
	 * @return the hex, in lower case
	 */
	public static String toHex(final byte[] bytes)
	{
		return HexFormat.of().formatHex(bytes);
	}

	/**
	 * Bytes as Base64, the way a key is usually put into a configuration file
	 *
	 * @param bytes
	 *            the bytes
	 * @return the Base64
	 */
	public static String toBase64(final byte[] bytes)
	{
		return Base64.getEncoder().encodeToString(bytes);
	}

	/**
	 * Writes a private key as PEM, in the format that was asked for.
	 * <p>
	 * PKCS#8 - "BEGIN PRIVATE KEY" - is what Java writes and reads. PKCS#1 - "BEGIN RSA PRIVATE
	 * KEY" - is what openssl, nginx and a good deal of the rest of the world expect, and being
	 * unable to produce it is the usual reason a key from a Java tool is rejected elsewhere.
	 *
	 * @param privateKey
	 *            the key to write
	 * @param file
	 *            the file to write it to
	 * @param format
	 *            PKCS#1 or PKCS#8
	 * @throws Exception
	 *             if the key cannot be written in that format
	 */
	public static void writePrivateKey(final PrivateKey privateKey, final File file,
		final KeyFormat format) throws Exception
	{
		if (format == KeyFormat.PKCS_8)
		{
			// written here rather than through the library: its PKCS#8 branch ends up in
			// PrivateKeyExtensions.toPemFormat, which writes PKCS#1 content under an algorithm
			// specific header whatever it was asked for - so both formats would come out the same.
			// What getEncoded() returns already is PKCS#8, it only needs the right wrapper
			try (java.io.Writer writer = Files.newBufferedWriter(file.toPath());
				org.bouncycastle.util.io.pem.PemWriter pemWriter = new org.bouncycastle.util.io.pem.PemWriter(
					writer))
			{
				pemWriter.writeObject(new org.bouncycastle.util.io.pem.PemObject("PRIVATE KEY",
					privateKey.getEncoded()));
			}
			return;
		}
		try (OutputStream out = Files.newOutputStream(file.toPath()))
		{
			PrivateKeyWriter.write(privateKey, out, KeyFileFormat.PEM, format);
		}
	}

	/**
	 * The formats a private key can be written in, with the name to show for each
	 *
	 * @return the formats
	 */
	public static List<KeyFormat> keyFormats()
	{
		List<KeyFormat> formats = new ArrayList<>();
		formats.add(KeyFormat.PKCS_8);
		formats.add(KeyFormat.PKCS_1);
		return formats;
	}

	/**
	 * What a key format is called where people talk about it
	 *
	 * @param format
	 *            the format
	 * @return the name to show
	 */
	public static String displayName(final KeyFormat format)
	{
		return switch (format)
		{
			case PKCS_1 -> "PKCS#1 (openssl, nginx)";
			case PKCS_8 -> "PKCS#8 (Java)";
			default -> String.valueOf(format);
		};
	}
}
