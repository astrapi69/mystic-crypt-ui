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
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.ZonedDateTime;
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
import io.github.astrapi69.crypt.data.key.KeyInfoExtensions;
import io.github.astrapi69.crypt.data.key.writer.CertificateWriter;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.mystic.crypt.wizard.CertificateInfoModelToX509;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.KeyInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.ValidityModel;
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

	/** What a file holding PEM text is called */
	public static final String PEM_ENDING = ".pem";

	/** What a file holding the binary encoding is called */
	public static final String DER_ENDING = ".der";

	/**
	 * The given file with the given ending, added when it is not already there.
	 * <p>
	 * The endings are not decoration: this window writes PEM text for a plain private key and a
	 * certificate, and the binary encoding for a public key and for a password protected private
	 * key. A file named after the wrong one is refused by whatever it is handed to next.
	 *
	 * @param file
	 *            the file the user picked
	 * @param ending
	 *            the ending the written content calls for
	 * @return the file to write to
	 */
	public static File withEnding(final File file, final String ending)
	{
		String name = file.getName();
		// a name that already carries an ending is left as it is, whatever the ending: someone who
		// types one means it, and turning "key.pem" into "key.pem.der" helps nobody
		return 0 < name.lastIndexOf('.')
			? file
			: new File(file.getParentFile(), name + ending);
	}

	/**
	 * Describes the certificate that fits the given key pair: the two keys, a serial, one year of
	 * validity from now and the signature algorithm the private key can actually produce.
	 * <p>
	 * Issuer and subject are deliberately left open - they are what the user is asked for before
	 * the certificate is written.
	 *
	 * @param publicKey
	 *            the public key the certificate is for
	 * @param privateKey
	 *            the private key that signs it
	 * @return the description, ready to be completed with issuer and subject
	 */
	/** What the certificate is issued to and by when the user does not say otherwise */
	public static final String DEFAULT_COMMON_NAME = "mystic-crypt";

	public static CertificateInfoModel newCertificateInfo(final PublicKey publicKey,
		final PrivateKey privateKey)
	{
		ZonedDateTime now = ZonedDateTime.now();
		// issuer and subject are filled in rather than left open: this is a self signed certificate
		// for a key that was just generated, and pressing OK without typing a name used to end in a
		// null pointer instead of a certificate
		DistinguishedNameInfoModel name = DistinguishedNameInfoModel.builder()
			.commonName(DEFAULT_COMMON_NAME).build();
		return CertificateInfoModel.builder().issuer(name).subject(name)
			.publicKeyInfo(KeyInfoModel.toKeyInfoModel(KeyInfoExtensions.toKeyInfo(publicKey)))
			.privateKeyInfo(KeyInfoModel.toKeyInfoModel(KeyInfoExtensions.toKeyInfo(privateKey)))
			.serial(BigInteger.ONE)
			.validityModel(
				ValidityModel.builder().notBefore(now).notAfter(now.plusYears(1)).build())
			.signatureAlgorithm(CertificateInfoModelToX509
				.defaultSignatureAlgorithmFor(privateKey.getAlgorithm()))
			.build();
	}

	/**
	 * Writes the certificate the given description asks for, in PEM form
	 *
	 * @param certificateInfo
	 *            what the certificate is to contain
	 * @param file
	 *            the file to write it to
	 * @throws Exception
	 *             if the keys cannot be restored, the description does not add up, or the file
	 *             cannot be written
	 */
	public static void writeCertificate(final CertificateInfoModel certificateInfo, final File file)
		throws Exception
	{
		CertificateWriter.writeInPemFormat(
			CertificateInfoModelToX509.toX509Certificate(certificateInfo), file);
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
	/**
	 * The file ending that belongs to the given encoding
	 *
	 * @param saveFormat
	 *            the encoding a file is written in
	 * @return the ending, including the dot
	 */
	public static String endingFor(final KeyFileFormat saveFormat)
	{
		return KeyFileFormat.DER.equals(saveFormat) ? DER_ENDING : PEM_ENDING;
	}

	/**
	 * Writes a private key in the given encoding, in the given structure
	 *
	 * @param privateKey
	 *            the key to write
	 * @param file
	 *            the file to write it to
	 * @param format
	 *            PKCS#1 or PKCS#8
	 * @param saveFormat
	 *            PEM text or the binary encoding
	 * @throws Exception
	 *             if the key cannot be written that way
	 */
	public static void writePrivateKey(final PrivateKey privateKey, final File file,
		final KeyFormat format, final KeyFileFormat saveFormat) throws Exception
	{
		if (KeyFileFormat.DER.equals(saveFormat))
		{
			// the binary encoding is the structure itself, so PKCS#8 or PKCS#1 is all there is to
			// say about it
			try (OutputStream out = Files.newOutputStream(file.toPath()))
			{
				PrivateKeyWriter.write(privateKey, out, KeyFileFormat.DER, format);
			}
			return;
		}
		writePrivateKey(privateKey, file, format);
	}

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
