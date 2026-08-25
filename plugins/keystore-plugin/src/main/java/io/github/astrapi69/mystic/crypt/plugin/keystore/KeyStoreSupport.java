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
			case RSA, RSASSA_PSS -> "SHA256withRSA";
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
		X500Name name = new X500Name(distinguishedName);
		X509Certificate certificate = CertFactory.newX509CertificateV3(keyPair, name, daysValid,
			name, signatureAlgorithm);
		KeyStoreExtensions.addAndStorePrivateKey(keyStore, file, alias, keyPair.getPrivate(),
			storePassword.toCharArray(), new Certificate[] { certificate });
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
