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
package io.github.astrapi69.mystic.crypt.plugin.signature;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.data.factory.SignatureFactory;
import io.github.astrapi69.crypt.data.key.reader.CertificateReader;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypt.data.key.reader.PublicKeyReader;
import io.github.astrapi69.mystic.crypt.key.Ed25519Signer;
import io.github.astrapi69.mystic.crypt.key.Ed25519Verifier;
import io.github.astrapi69.mystic.crypt.key.MlDsaSigner;
import io.github.astrapi69.mystic.crypt.key.MlDsaVerifier;
import io.github.astrapi69.mystic.crypt.key.SlhDsaSigner;
import io.github.astrapi69.mystic.crypt.key.SlhDsaVerifier;

/**
 * One entry point for the three signature families the mystic-crypt library provides: the classical
 * Ed25519 and the NIST post-quantum ML-DSA (FIPS 204) and SLH-DSA (FIPS 205).
 * <p>
 * Each family has its own signer and verifier class, and Ed25519 differs from the other two in that
 * it takes no algorithm parameter - it is not a {@link KeyPairGeneratorAlgorithm} constant at all.
 * This class hides that split behind one string-keyed API, so the panel and the tests can treat all
 * of them alike.
 */
public final class SignatureSupport
{

	/** The name under which Ed25519 is offered; it has no {@link KeyPairGeneratorAlgorithm} */
	/** The classical Edwards-curve algorithm, which has no constant of its own in the enum */
	public static final String ED25519 = "Ed25519";

	/** The classical algorithms, which are signed through the generic JCE signature factory */
	public static final List<String> CLASSICAL_ALGORITHMS = List.of("SHA256withRSA",
		"SHA512withRSA", "SHA256withECDSA", "SHA512withECDSA", "SHA256withDSA");

	private SignatureSupport()
	{
	}

	/**
	 * Lists every offered signature algorithm: Ed25519 first, then the ML-DSA parameter sets, then
	 * the SLH-DSA ones
	 *
	 * @return the algorithm names
	 */
	public static List<String> algorithms()
	{
		List<String> algorithms = new ArrayList<>();
		algorithms.add(ED25519);
		for (KeyPairGeneratorAlgorithm algorithm : KeyPairGeneratorAlgorithm.values())
		{
			if (algorithm.name().startsWith("ML_DSA"))
			{
				algorithms.add(display(algorithm));
			}
		}
		for (KeyPairGeneratorAlgorithm algorithm : KeyPairGeneratorAlgorithm.values())
		{
			if (algorithm.name().startsWith("SLH_DSA"))
			{
				algorithms.add(display(algorithm));
			}
		}
		return algorithms;
	}

	/**
	 * Every algorithm this tool can sign with: the post-quantum and Edwards ones it can also
	 * generate keys for, plus the classical ones that only work with a key that already exists
	 *
	 * @return the algorithms to offer
	 */
	public static List<String> allAlgorithms()
	{
		List<String> all = new ArrayList<>(algorithms());
		all.addAll(CLASSICAL_ALGORITHMS);
		return all;
	}

	/**
	 * Whether this tool can generate a key pair for the given algorithm. The classical ones are for
	 * keys that come from somewhere else - a key store, a certificate authority, another tool
	 *
	 * @param algorithm
	 *            the algorithm
	 * @return true if a key pair can be generated
	 */
	public static boolean canGenerateKeyPair(final String algorithm)
	{
		return algorithms().contains(algorithm);
	}

	/**
	 * The algorithm that fits a key that already exists, so a loaded key does not have to be
	 * identified by hand
	 *
	 * @param key
	 *            the key, private or public
	 * @return the algorithm to sign or verify with
	 */
	public static String algorithmFor(final java.security.Key key)
	{
		String keyAlgorithm = key.getAlgorithm().toUpperCase(java.util.Locale.ROOT);
		return switch (keyAlgorithm)
		{
			case "RSA" -> "SHA256withRSA";
			case "RSASSA-PSS" -> "SHA256withRSAandMGF1";
			case "EC", "ECDSA" -> "SHA256withECDSA";
			case "DSA" -> "SHA256withDSA";
			// a Bouncy Castle Ed25519 key reports itself as EDDSA
			case "ED25519", "EDDSA" -> ED25519;
			default -> keyAlgorithm.replace('_', '-');
		};
	}

	/**
	 * Reads a private key from a file, in whichever of the two formats it is in
	 *
	 * @param file
	 *            the key file, PEM or DER
	 * @return the private key
	 * @throws Exception
	 *             if the file holds no readable private key
	 */
	public static PrivateKey readPrivateKey(final java.io.File file) throws Exception
	{
		boolean pem = PrivateKeyReader.isPemFormat(file);
		PrivateKey privateKey = null;
		Exception firstFailure = null;
		try
		{
			privateKey = pem
				? PrivateKeyReader.readPemPrivateKey(file)
				: PrivateKeyReader.readPrivateKey(file);
		}
		catch (Exception exception)
		{
			firstFailure = exception;
		}
		if (privateKey == null)
		{
			// the reader guesses RSA unless it is told otherwise, and it decodes with whichever
			// provider the JDK picks - which cannot read an EC key on a Bouncy Castle named curve.
			// Decoding the bytes with Bouncy Castle, once per key type, answers both
			privateKey = readPemPrivateKeyWithBouncyCastle(file);
			if (privateKey == null)
			{
				byte[] encoded = encodedKey(file, pem);
				for (String algorithm : KEY_ALGORITHMS_TO_TRY)
				{
					try
					{
						privateKey = java.security.KeyFactory
							.getInstance(algorithm,
								org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME)
							.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(encoded));
						break;
					}
					catch (Exception nextOne)
					{
						// try the next key type
					}
				}
			}
		}
		if (privateKey == null)
		{
			throw new IllegalArgumentException("'" + file + "' holds no private key this tool can "
				+ "read", firstFailure);
		}
		return privateKey;
	}

	/**
	 * Reads a public key from a file: a public key of its own, or the one inside a certificate -
	 * which is how a public key usually arrives
	 *
	 * @param file
	 *            the file, PEM or DER
	 * @return the public key
	 * @throws Exception
	 *             if the file holds neither a public key nor a certificate
	 */
	public static PublicKey readPublicKey(final java.io.File file) throws Exception
	{
		try
		{
			byte[] encoded = encodedKey(file, PrivateKeyReader.isPemFormat(file));
			for (String algorithm : KEY_ALGORITHMS_TO_TRY)
			{
				try
				{
					return java.security.KeyFactory
						.getInstance(algorithm,
							org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME)
						.generatePublic(new java.security.spec.X509EncodedKeySpec(encoded));
				}
				catch (Exception nextOne)
				{
					// try the next key type
				}
			}
		}
		catch (Exception notAKeyFileAtAll)
		{
			// fall through to the certificate
		}
		try
		{
			PublicKey publicKey = PublicKeyReader.readPublicKey(file);
			if (publicKey != null)
			{
				return publicKey;
			}
		}
		catch (Exception notADerPublicKeyEither)
		{
			// fall through to the certificate
		}
		// a public key most often arrives inside a certificate
		java.security.cert.X509Certificate certificate = CertificateReader.readCertificate(file);
		if (certificate == null)
		{
			throw new IllegalArgumentException(
				"'" + file + "' holds neither a public key nor a certificate");
		}
		return certificate.getPublicKey();
	}

	/** The key types whose files this tool can read, in the order they are tried */
	private static final List<String> KEY_ALGORITHMS_TO_TRY = List.of("RSA", "EC", "DSA", "EdDSA",
		"Ed25519");

	/**
	 * Reads a private key with Bouncy Castle's own pem parser, which is the one thing that knows
	 * every shape a private key file comes in - PKCS#8 ("BEGIN PRIVATE KEY"), the openssl style
	 * ("BEGIN RSA PRIVATE KEY", "BEGIN EC PRIVATE KEY") and a key pair file that carries both
	 * halves
	 *
	 * @param file
	 *            the key file
	 * @return the private key, or {@code null} when this file is not one
	 */
	private static PrivateKey readPemPrivateKeyWithBouncyCastle(final java.io.File file)
	{
		try (java.io.Reader reader = java.nio.file.Files.newBufferedReader(file.toPath());
			org.bouncycastle.openssl.PEMParser parser = new org.bouncycastle.openssl.PEMParser(
				reader))
		{
			Object parsed = parser.readObject();
			org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter converter = new org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter()
				.setProvider(org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME);
			if (parsed instanceof org.bouncycastle.openssl.PEMKeyPair keyPair)
			{
				return converter.getKeyPair(keyPair).getPrivate();
			}
			if (parsed instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo privateKeyInfo)
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

	/**
	 * The raw bytes of a key file: the Base64 body for a PEM file, the file itself for a DER one
	 */
	private static byte[] encodedKey(final java.io.File file, final boolean pem) throws Exception
	{
		if (!pem)
		{
			return java.nio.file.Files.readAllBytes(file.toPath());
		}
		String base64 = java.nio.file.Files.readAllLines(file.toPath()).stream()
			.filter(line -> !line.startsWith("-----")).reduce("", String::concat)
			.replaceAll("\\s", "");
		return java.util.Base64.getDecoder().decode(base64);
	}

	/** Whether the given name is a classical signature algorithm rather than one of the families */
	private static boolean isClassical(final String algorithm)
	{
		return CLASSICAL_ALGORITHMS.contains(algorithm) || algorithm.contains("with");
	}

	private static <T> T withBouncyCastle(final String algorithm,
		final SignatureOperation<T> operation) throws Exception
	{
		return operation.perform(java.security.Signature.getInstance(algorithm,
			org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME));
	}

	@FunctionalInterface
	private interface SignatureOperation<T>
	{
		T perform(java.security.Signature signature) throws Exception;
	}

	private static String display(final KeyPairGeneratorAlgorithm algorithm)
	{
		return algorithm.name().replace('_', '-');
	}

	private static KeyPairGeneratorAlgorithm parse(final String name)
	{
		return KeyPairGeneratorAlgorithm.valueOf(name.trim().toUpperCase().replace('-', '_'));
	}

	/**
	 * Generates a key pair for the given algorithm
	 *
	 * @param algorithm
	 *            one of {@link #algorithms()}
	 * @return the new key pair
	 * @throws Exception
	 *             if the platform cannot generate a key pair for the algorithm
	 */
	public static KeyPair newKeyPair(final String algorithm) throws Exception
	{
		if (ED25519.equals(algorithm))
		{
			return Ed25519Signer.newKeyPair();
		}
		KeyPairGeneratorAlgorithm parsed = parse(algorithm);
		return parsed.name().startsWith("ML_DSA")
			? MlDsaSigner.newKeyPair(parsed)
			: SlhDsaSigner.newKeyPair(parsed);
	}

	/**
	 * Signs the given bytes
	 *
	 * @param algorithm
	 *            one of {@link #algorithms()}
	 * @param privateKey
	 *            the signing key
	 * @param data
	 *            the bytes to sign
	 * @return the signature
	 * @throws Exception
	 *             if signing fails
	 */
	public static byte[] sign(final String algorithm, final PrivateKey privateKey,
		final byte[] data) throws Exception
	{
		if (isClassical(algorithm))
		{
			// deliberately Bouncy Castle rather than whatever the JDK picks: the keys this
			// application generates are Bouncy Castle keys, and the default provider cannot always
			// use them - an EC key on a named curve fails with "Curve not supported". Bouncy Castle
			// reads the JDK's own keys as well, so this direction works for both
			try
			{
				return withBouncyCastle(algorithm, signature -> {
					signature.initSign(privateKey);
					signature.update(data);
					return signature.sign();
				});
			}
			catch (GeneralSecurityException bouncyCastleCannotDoIt)
			{
				return SignatureFactory.sign(privateKey, algorithm, data);
			}
		}
		if (ED25519.equals(algorithm))
		{
			return new Ed25519Signer(privateKey).sign(data);
		}
		KeyPairGeneratorAlgorithm parsed = parse(algorithm);
		return parsed.name().startsWith("ML_DSA")
			? new MlDsaSigner(privateKey, parsed).sign(data)
			: new SlhDsaSigner(privateKey, parsed).sign(data);
	}

	/**
	 * Verifies a signature over the given bytes
	 *
	 * @param algorithm
	 *            one of {@link #algorithms()}
	 * @param publicKey
	 *            the verification key
	 * @param data
	 *            the signed bytes
	 * @param signature
	 *            the signature to check
	 * @return true if the signature belongs to the data and the key
	 * @throws Exception
	 *             if verifying fails
	 */
	public static boolean verify(final String algorithm, final PublicKey publicKey,
		final byte[] data, final byte[] signature) throws Exception
	{
		if (isClassical(algorithm))
		{
			// the same provider that signed has to verify - and a provider that cannot use the key
			// answers "not valid" rather than failing, which would look like a bad signature
			try
			{
				return withBouncyCastle(algorithm, jceSignature -> {
					jceSignature.initVerify(publicKey);
					jceSignature.update(data);
					return jceSignature.verify(signature);
				});
			}
			catch (GeneralSecurityException bouncyCastleCannotDoIt)
			{
				return SignatureFactory.verify(publicKey, algorithm, data, signature);
			}
		}
		if (ED25519.equals(algorithm))
		{
			return new Ed25519Verifier(publicKey).verify(data, signature);
		}
		KeyPairGeneratorAlgorithm parsed = parse(algorithm);
		return parsed.name().startsWith("ML_DSA")
			? new MlDsaVerifier(publicKey, parsed).verify(data, signature)
			: new SlhDsaVerifier(publicKey, parsed).verify(data, signature);
	}
}
