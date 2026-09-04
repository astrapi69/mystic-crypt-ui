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
package io.github.astrapi69.mystic.crypt.plugin.keystore;

import java.io.File;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.github.astrapi69.crypt.api.algorithm.HashAlgorithm;
import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.type.KeystoreType;
import io.github.astrapi69.crypt.data.factory.CertFactory;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.factory.KeyStoreFactory;
import io.github.astrapi69.crypt.data.key.CertificateExtensions;
import io.github.astrapi69.crypt.data.key.KeyStoreExtensions;
import io.github.astrapi69.crypt.data.key.reader.CertificateReader;
import io.github.astrapi69.crypt.data.key.writer.CertificateWriter;
import io.github.astrapi69.mystic.crypt.crypto.KeyFiles;
import io.github.astrapi69.mystic.crypt.ssl.KeystoreVerifier;

/**
 * Everything the key store tool does, without any user interface: opening and creating a key store,
 * listing what is inside it, adding a freshly generated key pair with a self-signed certificate,
 * importing and exporting certificates, and removing an alias.
 * <p>
 * Keeping this apart from the panel is what makes the behaviour testable without a display - and
 * every method here works on a real key store file, exactly like the tool does.
 */
public final class KeyStoreSupport
{

	/** The store types worth offering in a desktop tool; PKCS11 and DKS need external hardware or a
	 * configuration file and are left out on purpose */
	public static final List<KeystoreType> USABLE_TYPES = List.of(KeystoreType.PKCS12,
		KeystoreType.JKS, KeystoreType.JCEKS);

	/** The key algorithms offered for a new entry: the two classical ones plus the post-quantum
	 * ML-DSA, so a store can hold a quantum safe key as well */
	public static final List<KeyPairGeneratorAlgorithm> KEY_ALGORITHMS = List
		.of(KeyPairGeneratorAlgorithm.RSA, KeyPairGeneratorAlgorithm.EC,
			KeyPairGeneratorAlgorithm.ML_DSA_65);

	/** The key size used for the size based algorithms */
	public static final int DEFAULT_KEY_SIZE = 2048;

	/** How long a generated self-signed certificate is valid */
	public static final int DEFAULT_DAYS_VALID = 365;

	private KeyStoreSupport()
	{
	}

	/**
	 * The signature algorithm that fits a key algorithm; a certificate can only be signed with a
	 * signature algorithm the key itself supports
	 *
	 * @param algorithm
	 *            the key algorithm
	 * @return the matching signature algorithm
	 * @throws IllegalArgumentException
	 *             if the algorithm cannot sign a certificate, such as a key exchange algorithm
	 */
	public static String signatureAlgorithmFor(final KeyPairGeneratorAlgorithm algorithm)
	{
		return switch (algorithm)
		{
			case RSA -> "SHA256withRSA";
			// an RSASSA-PSS key signed with the PKCS#1 v1.5 scheme produces a certificate that
			// contradicts RFC 4055 and that openssl rejects when verifying it
			case RSASSA_PSS -> "SHA256withRSAandMGF1";
			case EC -> "SHA256withECDSA";
			case DSA -> "SHA256withDSA";
			case ML_DSA_44 -> "ML-DSA-44";
			case ML_DSA_65 -> "ML-DSA-65";
			case ML_DSA_87 -> "ML-DSA-87";
			default -> throw new IllegalArgumentException(
				"'" + algorithm + "' cannot sign a certificate");
		};
	}

	/**
	 * What one entry of a key store shows in the table.
	 *
	 * @param alias
	 *            the name the entry is stored under
	 * @param entryKind
	 *            "private key" or "certificate"
	 * @param algorithm
	 *            the key or signature algorithm, empty when it cannot be determined
	 * @param subject
	 *            the certificate subject, empty when the entry carries no certificate
	 * @param validUntil
	 *            the end of the certificate validity, empty when there is none
	 * @param fingerprint
	 *            the SHA-256 fingerprint of the certificate, empty when there is none
	 */
	public record EntryInfo(String alias, String entryKind, String algorithm, String subject,
		String validUntil, String fingerprint)
	{
	}

	/**
	 * Opens an existing key store
	 *
	 * @param file
	 *            the key store file
	 * @param type
	 *            the store type
	 * @param password
	 *            the store password
	 * @return the opened key store
	 * @throws Exception
	 *             if the file cannot be read or the password is wrong
	 */
	public static KeyStore open(final File file, final KeystoreType type, final String password)
		throws Exception
	{
		return KeyStoreFactory.loadKeyStore(file, type.getType(), password);
	}

	/**
	 * Creates a new, empty key store and writes it to disk
	 *
	 * @param file
	 *            the key store file to create
	 * @param type
	 *            the store type
	 * @param password
	 *            the store password
	 * @return the created key store
	 * @throws Exception
	 *             if the file cannot be written
	 */
	public static KeyStore create(final File file, final KeystoreType type, final String password)
		throws Exception
	{
		return KeyStoreFactory.newKeyStore(file, type.getType(), password);
	}

	/**
	 * Lists what a key store holds, one row per alias
	 *
	 * @param keyStore
	 *            the key store to inspect
	 * @return the entries, in the order the store returns its aliases
	 * @throws Exception
	 *             if the store cannot be read
	 */
	public static List<EntryInfo> entries(final KeyStore keyStore) throws Exception
	{
		List<EntryInfo> entries = new ArrayList<>();
		for (String alias : Collections.list(keyStore.aliases()))
		{
			boolean isKey = keyStore.isKeyEntry(alias);
			Certificate certificate = keyStore.getCertificate(alias);
			String algorithm = "";
			String subject = "";
			String validUntil = "";
			String fingerprint = "";
			if (certificate instanceof X509Certificate x509)
			{
				algorithm = x509.getSigAlgName();
				subject = CertificateExtensions.getSubject(x509);
				validUntil = String.valueOf(x509.getNotAfter());
				fingerprint = CertificateExtensions.getFingerprint(x509, HashAlgorithm.SHA256);
			}
			entries.add(new EntryInfo(alias, isKey ? "private key" : "certificate", algorithm,
				subject, validUntil, fingerprint));
		}
		return entries;
	}

	/**
	 * Generates a key pair, wraps it in a self-signed certificate and stores both under the given
	 * alias
	 *
	 * @param keyStore
	 *            the key store to add to
	 * @param file
	 *            the key store file, written again afterwards
	 * @param storePassword
	 *            the store password
	 * @param alias
	 *            the alias for the new entry
	 * @param distinguishedName
	 *            the certificate subject and issuer, for instance {@code CN=my server}
	 * @param algorithm
	 *            the key algorithm
	 * @param keySize
	 *            the key size, used only by the size based algorithms such as RSA
	 * @param signatureAlgorithm
	 *            the certificate signature algorithm, for instance {@code SHA256withRSA}
	 * @param daysValid
	 *            how long the certificate is valid
	 * @throws Exception
	 *             if generating or storing fails
	 */
	public static void addKeyPair(final KeyStore keyStore, final File file,
		final String storePassword, final String alias, final String distinguishedName,
		final KeyPairGeneratorAlgorithm algorithm, final int keySize,
		final String signatureAlgorithm, final int daysValid) throws Exception
	{
		KeyPair keyPair = isSizeBased(algorithm)
			? KeyPairFactory.newKeyPair(algorithm, keySize)
			: KeyPairFactory.newKeyPair(algorithm);
		X500Name name = parseDistinguishedName(distinguishedName);
		X509Certificate certificate = CertFactory.newX509CertificateV3(keyPair, name, daysValid,
			name, signatureAlgorithm);
		KeyStoreExtensions.addAndStorePrivateKey(keyStore, file, alias, keyPair.getPrivate(),
			storePassword.toCharArray(), new Certificate[] { certificate });
	}

	/**
	 * Parses a distinguished name, naming the expected format when it is not one.
	 * <p>
	 * {@link X500Name}'s own constructor throws with BouncyCastle's raw internal parsing message
	 * ("badly formatted directory string") - accurate, but it reads like a file-path problem and
	 * is not actionable without already knowing X.500 DN syntax (#196).
	 *
	 * @param distinguishedName
	 *            the distinguished name to parse, for instance {@code CN=example.com}
	 * @return the parsed name
	 * @throws IllegalArgumentException
	 *             if the given string is not a valid distinguished name
	 */
	private static X500Name parseDistinguishedName(final String distinguishedName)
	{
		try
		{
			return new X500Name(distinguishedName);
		}
		catch (final IllegalArgumentException invalidDistinguishedName)
		{
			throw new IllegalArgumentException(
				"'" + distinguishedName + "' is not a valid distinguished name - use the form "
					+ "'CN=name', for example 'CN=example.com': "
					+ invalidDistinguishedName.getMessage(),
				invalidDistinguishedName);
		}
	}

	/**
	 * Generates a key pair with the defaults of this tool - {@link #DEFAULT_KEY_SIZE} where a size
	 * applies, the signature algorithm matching the key and {@link #DEFAULT_DAYS_VALID} days of
	 * validity - and stores it under the given alias
	 *
	 * @param keyStore
	 *            the key store to add to
	 * @param file
	 *            the key store file, written again afterwards
	 * @param storePassword
	 *            the store password
	 * @param alias
	 *            the alias for the new entry
	 * @param distinguishedName
	 *            the certificate subject and issuer, for instance {@code CN=my server}
	 * @param algorithm
	 *            the key algorithm
	 * @throws Exception
	 *             if generating or storing fails
	 */
	public static void addKeyPair(final KeyStore keyStore, final File file,
		final String storePassword, final String alias, final String distinguishedName,
		final KeyPairGeneratorAlgorithm algorithm) throws Exception
	{
		addKeyPair(keyStore, file, storePassword, alias, distinguishedName, algorithm,
			DEFAULT_KEY_SIZE, signatureAlgorithmFor(algorithm), DEFAULT_DAYS_VALID);
	}

	/** Whether the given key algorithm is initialized with a classical key size */
	public static boolean isSizeBased(final KeyPairGeneratorAlgorithm algorithm)
	{
		return algorithm == KeyPairGeneratorAlgorithm.RSA
			|| algorithm == KeyPairGeneratorAlgorithm.DSA
			|| algorithm == KeyPairGeneratorAlgorithm.DIFFIE_HELLMAN
			|| algorithm == KeyPairGeneratorAlgorithm.DH;
	}

	/**
	 * Imports a certificate from a PEM or DER file and stores it under the given alias
	 *
	 * @param keyStore
	 *            the key store to add to
	 * @param file
	 *            the key store file, written again afterwards
	 * @param storePassword
	 *            the store password
	 * @param alias
	 *            the alias for the imported certificate
	 * @param certificateFile
	 *            the certificate file to read
	 * @throws Exception
	 *             if reading or storing fails
	 */
	public static void importCertificate(final KeyStore keyStore, final File file,
		final String storePassword, final String alias, final File certificateFile) throws Exception
	{
		X509Certificate certificate = CertificateReader.readCertificate(certificateFile);
		KeyStoreExtensions.addAndStoreCertificate(keyStore, file, storePassword, alias, certificate);
	}

	/**
	 * Writes the certificate stored under the given alias as PEM
	 *
	 * @param keyStore
	 *            the key store to read from
	 * @param alias
	 *            the alias whose certificate is exported
	 * @param target
	 *            the file to write
	 * @throws Exception
	 *             if the alias holds no certificate or writing fails
	 */
	public static void exportCertificate(final KeyStore keyStore, final String alias,
		final File target) throws Exception
	{
		Certificate certificate = KeyStoreExtensions.getCertificate(keyStore, alias);
		if (!(certificate instanceof X509Certificate x509))
		{
			throw new IllegalArgumentException("'" + alias + "' holds no X.509 certificate");
		}
		CertificateWriter.writeInPemFormat(x509, target);
	}

	/**
	 * What a certificate says about itself, for the dialog that shows one entry in full.
	 *
	 * @param alias
	 *            the alias the certificate is stored under
	 * @param subject
	 *            who the certificate was issued to
	 * @param issuer
	 *            who issued it
	 * @param validFrom
	 *            the beginning of its validity
	 * @param validUntil
	 *            the end of its validity
	 * @param expired
	 *            whether that end is in the past
	 * @param serialNumber
	 *            the serial number
	 * @param signatureAlgorithm
	 *            how the certificate is signed
	 * @param keyAlgorithm
	 *            the algorithm of the key inside
	 * @param version
	 *            the X.509 version
	 * @param fingerprint
	 *            the SHA-256 fingerprint
	 * @param pem
	 *            the certificate itself, as PEM
	 */
	public record CertificateDetails(String alias, String subject, String issuer, String validFrom,
		String validUntil, boolean expired, String serialNumber, String signatureAlgorithm,
		String keyAlgorithm, int version, String fingerprint, String pem)
	{
	}

	/**
	 * Everything a certificate says about itself
	 *
	 * @param keyStore
	 *            the key store to read from
	 * @param alias
	 *            the alias whose certificate is read
	 * @return the details
	 * @throws Exception
	 *             if the alias holds no X.509 certificate
	 */
	public static CertificateDetails details(final KeyStore keyStore, final String alias)
		throws Exception
	{
		Certificate certificate = KeyStoreExtensions.getCertificate(keyStore, alias);
		if (!(certificate instanceof X509Certificate x509))
		{
			throw new IllegalArgumentException("'" + alias + "' holds no X.509 certificate");
		}
		return new CertificateDetails(alias, CertificateExtensions.getSubject(x509),
			CertificateExtensions.getIssuedBy(x509), String.valueOf(x509.getNotBefore()),
			String.valueOf(x509.getNotAfter()), x509.getNotAfter().before(new java.util.Date()),
			String.valueOf(CertificateExtensions.getSerialNumber(x509)), x509.getSigAlgName(),
			x509.getPublicKey().getAlgorithm(), x509.getVersion(),
			CertificateExtensions.getFingerprint(x509, HashAlgorithm.SHA256),
			toPem(x509));
	}

	/**
	 * Puts a private key that came from somewhere else into the store, together with the
	 * certificate that belongs to it. This is the usual way a key arrives: from a certificate
	 * authority, from another tool, from a server that is being moved
	 *
	 * @param keyStore
	 *            the key store to add to
	 * @param file
	 *            the key store file, written again afterwards
	 * @param storePassword
	 *            the store password
	 * @param alias
	 *            the alias for the new entry
	 * @param privateKeyFile
	 *            the private key file, PEM or DER
	 * @param certificateFile
	 *            the certificate that belongs to that key
	 * @throws Exception
	 *             if either file cannot be read, the two do not belong together, or storing fails
	 */
	public static void importKeyPair(final KeyStore keyStore, final File file,
		final String storePassword, final String alias, final File privateKeyFile,
		final File certificateFile) throws Exception
	{
		PrivateKey privateKey = KeyFiles.readPrivateKey(privateKeyFile);
		X509Certificate certificate = KeyFiles.readCertificate(certificateFile);
		if (!belongTogether(privateKey, certificate))
		{
			throw new IllegalArgumentException("the private key and the certificate do not belong "
				+ "together - the key is " + privateKey.getAlgorithm() + ", the certificate holds a "
				+ certificate.getPublicKey().getAlgorithm() + " key");
		}
		KeyStoreExtensions.addAndStorePrivateKey(keyStore, file, alias, privateKey,
			storePassword.toCharArray(), new Certificate[] { certificate });
	}

	/**
	 * Generates a symmetric key and puts it into the store. Only JCEKS and PKCS12 can hold one; a
	 * JKS store cannot, and says so
	 *
	 * @param keyStore
	 *            the key store to add to
	 * @param file
	 *            the key store file, written again afterwards
	 * @param storePassword
	 *            the store password
	 * @param alias
	 *            the alias for the new entry
	 * @param algorithm
	 *            the algorithm, for instance AES
	 * @param keySize
	 *            the key size in bits
	 * @throws Exception
	 *             if the store cannot hold a symmetric key or writing fails
	 */
	public static void addSecretKey(final KeyStore keyStore, final File file,
		final String storePassword, final String alias, final String algorithm, final int keySize)
		throws Exception
	{
		javax.crypto.KeyGenerator keyGenerator = javax.crypto.KeyGenerator.getInstance(algorithm);
		keyGenerator.init(keySize);
		KeyStoreExtensions.setKeyEntry(keyStore, alias, keyGenerator.generateKey(),
			storePassword.toCharArray(), null);
		try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(file.toPath()))
		{
			keyStore.store(out, storePassword.toCharArray());
		}
	}

	/**
	 * Works out what kind of key store a file is, so nobody has to know beforehand
	 *
	 * @param file
	 *            the key store file
	 * @param password
	 *            the store password
	 * @return the type, or {@code null} when the password does not open it as any of them
	 */
	public static KeystoreType detectType(final File file, final String password)
	{
		KeystoreType byItsFirstBytes = typeByMagicNumber(file);
		if (byItsFirstBytes != null && KeystoreVerifier.isKeystoreFile(file,
			password.toCharArray(), byItsFirstBytes.getType()))
		{
			return byItsFirstBytes;
		}
		// asking the readers in turn is only the fallback, and PKCS12 has to be asked last: since
		// JDK 9 its reader opens a JKS file as well, so it answers yes for everything
		for (KeystoreType type : List.of(KeystoreType.JKS, KeystoreType.JCEKS,
			KeystoreType.PKCS12))
		{
			if (KeystoreVerifier.isKeystoreFile(file, password.toCharArray(), type.getType()))
			{
				return type;
			}
		}
		return null;
	}

	/**
	 * What a key store file says it is in its first four bytes. JKS and JCEKS each start with a
	 * number of their own; a PKCS12 file is ASN.1 and starts with a sequence
	 *
	 * @param file
	 *            the file to look at
	 * @return the type, or {@code null} when the first bytes say nothing
	 */
	public static KeystoreType typeByMagicNumber(final File file)
	{
		try (java.io.InputStream in = java.nio.file.Files.newInputStream(file.toPath()))
		{
			byte[] first = in.readNBytes(4);
			if (first.length < 4)
			{
				return null;
			}
			int magic = ((first[0] & 0xff) << 24) | ((first[1] & 0xff) << 16)
				| ((first[2] & 0xff) << 8) | (first[3] & 0xff);
			if (magic == 0xFEEDFEED)
			{
				return KeystoreType.JKS;
			}
			if (magic == 0xCECECECE)
			{
				return KeystoreType.JCEKS;
			}
			return (first[0] & 0xff) == 0x30 ? KeystoreType.PKCS12 : null;
		}
		catch (Exception unreadable)
		{
			return null;
		}
	}

	/**
	 * Whether a private key and a certificate are two halves of the same pair.
	 * <p>
	 * Comparing the algorithm names is not enough, and not even reliable: the same EC key calls
	 * itself "ECDSA" while the certificate calls its half "EC". So the two are actually used
	 * together - something is signed with the key and checked against the certificate. That answers
	 * the question rather than approximating it.
	 */
	private static boolean belongTogether(final PrivateKey privateKey,
		final X509Certificate certificate)
	{
		try
		{
			String signatureAlgorithm = SIGNATURE_FOR_CHECK
				.getOrDefault(family(privateKey.getAlgorithm()), null);
			if (signatureAlgorithm == null)
			{
				// an algorithm that cannot sign cannot be checked this way; fall back to the family
				return family(privateKey.getAlgorithm())
					.equals(family(certificate.getPublicKey().getAlgorithm()));
			}
			byte[] probe = "does this key belong to this certificate"
				.getBytes(java.nio.charset.StandardCharsets.UTF_8);
			java.security.Signature signature = java.security.Signature
				.getInstance(signatureAlgorithm, BouncyCastleProvider.PROVIDER_NAME);
			signature.initSign(privateKey);
			signature.update(probe);
			byte[] signed = signature.sign();

			java.security.Signature check = java.security.Signature
				.getInstance(signatureAlgorithm, BouncyCastleProvider.PROVIDER_NAME);
			check.initVerify(certificate.getPublicKey());
			check.update(probe);
			return check.verify(signed);
		}
		catch (Exception theyDoNotWorkTogether)
		{
			return false;
		}
	}

	/** The signature algorithm used to check whether a key and a certificate match */
	private static final java.util.Map<String, String> SIGNATURE_FOR_CHECK = java.util.Map.of("RSA",
		"SHA256withRSA", "EC", "SHA256withECDSA", "DSA", "SHA256withDSA", "ED25519", "Ed25519");

	/** The family a key belongs to, whatever name it happens to give itself */
	private static String family(final String algorithm)
	{
		String upper = algorithm.toUpperCase(java.util.Locale.ROOT);
		if (upper.startsWith("EC"))
		{
			return "EC";
		}
		if (upper.startsWith("ED"))
		{
			return "ED25519";
		}
		if (upper.startsWith("RSA"))
		{
			return "RSA";
		}
		return upper;
	}

	private static String toPem(final X509Certificate certificate) throws Exception
	{
		java.io.File temporary = java.io.File.createTempFile("certificate", ".pem");
		try
		{
			CertificateWriter.writeInPemFormat(certificate, temporary);
			return java.nio.file.Files.readString(temporary.toPath());
		}
		finally
		{
			java.nio.file.Files.deleteIfExists(temporary.toPath());
		}
	}

	/**
	 * Refuses a path that already holds something, because creating a key store there writes an
	 * empty one over whatever was in it.
	 * <p>
	 * Shared by every entry point that creates a key store - the dense "Manage Key Store" panel and
	 * the guided creation wizard - so the two cannot drift into different overwrite behaviour.
	 *
	 * @param file
	 *            the key store file a create operation is about to write to
	 * @throws IllegalStateException
	 *             if the file already exists and is not empty
	 */
	public static void requireFreeFile(final File file)
	{
		if (file.exists() && file.length() > 0)
		{
			throw new IllegalStateException(
				"'" + file.getName() + "' already exists; open it instead, or choose another file");
		}
	}

	/**
	 * Removes an alias from a key store file
	 *
	 * @param file
	 *            the key store file
	 * @param type
	 *            the store type
	 * @param storePassword
	 *            the store password
	 * @param alias
	 *            the alias to remove
	 * @return the key store as it is after the removal
	 * @throws Exception
	 *             if the store cannot be read or written
	 */
	public static KeyStore deleteAlias(final File file, final KeystoreType type,
		final String storePassword, final String alias) throws Exception
	{
		KeyStoreExtensions.deleteAlias(file, alias, storePassword);
		return open(file, type, storePassword);
	}

	/**
	 * Reads the private key stored under an alias, to prove the entry is usable
	 *
	 * @param keyStore
	 *            the key store to read from
	 * @param alias
	 *            the alias of the key
	 * @param password
	 *            the key password
	 * @return the private key
	 * @throws Exception
	 *             if the alias holds no key or the password is wrong
	 */
	public static PrivateKey privateKey(final KeyStore keyStore, final String alias,
		final String password) throws Exception
	{
		return KeyStoreExtensions.getPrivateKey(keyStore, alias, password.toCharArray());
	}

	/**
	 * Gets the serial number of the certificate stored under an alias
	 *
	 * @param keyStore
	 *            the key store to read from
	 * @param alias
	 *            the alias whose certificate is read
	 * @return the serial number
	 * @throws Exception
	 *             if the alias holds no X.509 certificate
	 */
	public static BigInteger serialNumber(final KeyStore keyStore, final String alias)
		throws Exception
	{
		Certificate certificate = KeyStoreExtensions.getCertificate(keyStore, alias);
		if (!(certificate instanceof X509Certificate x509))
		{
			throw new IllegalArgumentException("'" + alias + "' holds no X.509 certificate");
		}
		return CertificateExtensions.getSerialNumber(x509);
	}
}
