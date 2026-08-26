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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.type.KeystoreType;
import io.github.astrapi69.crypt.data.factory.CertFactory;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.CertificateWriter;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;

/**
 * Tests of what a key store tool has to do beyond generating its own keys: taking in a key that
 * came from somewhere else, saying what a certificate contains, holding a symmetric key, and
 * working out what kind of store a file is.
 */
class KeyStoreImportAndDetailsTest
{

	private static final String STORE_PASSWORD = "keystore-import-pw-1969";

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	/** A key pair and its certificate on disk, the way they arrive from a certificate authority */
	private KeyPair writeKeyAndCertificate(File directory, String keyAlgorithm,
		String signatureAlgorithm, Date notAfter) throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair(keyAlgorithm);
		X500Name name = new X500Name("CN=imported, O=elsewhere");
		X509Certificate certificate = CertFactory.newX509CertificateV3(keyPair, name,
			BigInteger.valueOf(42), new Date(System.currentTimeMillis() - 86_400_000L), notAfter,
			name, signatureAlgorithm);
		PrivateKeyWriter.writeInPemFormat(keyPair.getPrivate(), new File(directory, "key.pem"));
		CertificateWriter.writeInPemFormat(certificate, new File(directory, "certificate.pem"));
		return keyPair;
	}

	@ParameterizedTest
	@CsvSource({ "RSA,SHA256withRSA", "EC,SHA256withECDSA" })
	void takesInAKeyThatCameFromSomewhereElse(String keyAlgorithm, String signatureAlgorithm,
		@TempDir File directory) throws Exception
	{
		writeKeyAndCertificate(directory, keyAlgorithm, signatureAlgorithm,
			new Date(System.currentTimeMillis() + 86_400_000L));
		File storeFile = new File(directory, "imported.p12");
		KeyStore keyStore = KeyStoreSupport.create(storeFile, KeystoreType.PKCS12, STORE_PASSWORD);

		KeyStoreSupport.importKeyPair(keyStore, storeFile, STORE_PASSWORD, "from-the-ca",
			new File(directory, "key.pem"), new File(directory, "certificate.pem"));

		KeyStore reopened = KeyStoreSupport.open(storeFile, KeystoreType.PKCS12, STORE_PASSWORD);
		List<KeyStoreSupport.EntryInfo> entries = KeyStoreSupport.entries(reopened);
		assertEquals(1, entries.size());
		assertEquals("private key", entries.get(0).entryKind(),
			"an imported key pair must be a key entry, not merely a certificate");
		assertNotNull(KeyStoreSupport.privateKey(reopened, "from-the-ca", STORE_PASSWORD),
			"the imported key must be usable again");
		assertTrue(entries.get(0).subject().contains("CN=imported"));
	}

	@Test
	void aKeyAndACertificateThatDoNotBelongTogetherAreRefused(@TempDir File directory)
		throws Exception
	{
		writeKeyAndCertificate(directory, "RSA", "SHA256withRSA",
			new Date(System.currentTimeMillis() + 86_400_000L));
		// an EC key next to the RSA certificate
		File otherKey = new File(directory, "other-key.pem");
		PrivateKeyWriter.writeInPemFormat(KeyPairFactory.newKeyPair("EC").getPrivate(), otherKey);
		File storeFile = new File(directory, "mismatch.p12");
		KeyStore keyStore = KeyStoreSupport.create(storeFile, KeystoreType.PKCS12, STORE_PASSWORD);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> KeyStoreSupport.importKeyPair(keyStore, storeFile, STORE_PASSWORD, "mismatch",
				otherKey, new File(directory, "certificate.pem")));

		assertTrue(exception.getMessage().contains("do not belong together"), exception.getMessage());
		assertTrue(KeyStoreSupport.entries(keyStore).isEmpty(),
			"nothing may be stored when the two halves do not match");
	}

	@Test
	void saysEverythingACertificateContains(@TempDir File directory) throws Exception
	{
		File storeFile = new File(directory, "details.p12");
		KeyStore keyStore = KeyStoreSupport.create(storeFile, KeystoreType.PKCS12, STORE_PASSWORD);
		KeyStoreSupport.addKeyPair(keyStore, storeFile, STORE_PASSWORD, "server",
			"CN=the server, O=the organisation", KeyPairGeneratorAlgorithm.RSA);

		KeyStoreSupport.CertificateDetails details = KeyStoreSupport.details(keyStore, "server");

		assertEquals("server", details.alias());
		assertTrue(details.subject().contains("CN=the server"), details.subject());
		assertTrue(details.issuer().contains("CN=the server"),
			"a self-signed certificate is its own issuer");
		assertFalse(details.validFrom().isBlank());
		assertFalse(details.validUntil().isBlank());
		assertFalse(details.expired(), "a certificate valid for a year is not expired");
		assertEquals("SHA256WITHRSA", details.signatureAlgorithm().toUpperCase(java.util.Locale.ROOT),
			"the name is whatever the certificate calls it, was: " + details.signatureAlgorithm());
		assertEquals("RSA", details.keyAlgorithm());
		assertEquals(3, details.version(), "the tool writes X.509 version 3 certificates");
		assertFalse(details.serialNumber().isBlank());
		assertEquals(64, details.fingerprint().length(), "a SHA-256 fingerprint is 64 hex digits");
		assertTrue(details.pem().contains("BEGIN CERTIFICATE"),
			"the details must include the certificate itself, to copy it out");
	}

	@Test
	void anExpiredCertificateIsSaidToBeExpired(@TempDir File directory) throws Exception
	{
		writeKeyAndCertificate(directory, "RSA", "SHA256withRSA",
			new Date(System.currentTimeMillis() - 3600_000L));
		File storeFile = new File(directory, "expired.p12");
		KeyStore keyStore = KeyStoreSupport.create(storeFile, KeystoreType.PKCS12, STORE_PASSWORD);
		KeyStoreSupport.importKeyPair(keyStore, storeFile, STORE_PASSWORD, "old",
			new File(directory, "key.pem"), new File(directory, "certificate.pem"));

		assertTrue(KeyStoreSupport.details(keyStore, "old").expired(),
			"a certificate whose validity ended must say so, that is the point of showing it");
	}

	@Test
	void detailsOfAnAliasThatHoldsNoCertificateAreRefused(@TempDir File directory) throws Exception
	{
		File storeFile = new File(directory, "empty.p12");
		KeyStore keyStore = KeyStoreSupport.create(storeFile, KeystoreType.PKCS12, STORE_PASSWORD);

		assertThrows(IllegalArgumentException.class,
			() -> KeyStoreSupport.details(keyStore, "does not exist"));
	}

	@ParameterizedTest
	@CsvSource({ "AES,128", "AES,256" })
	void holdsASymmetricKey(String algorithm, int keySize, @TempDir File directory) throws Exception
	{
		File storeFile = new File(directory, "secret.jceks");
		KeyStore keyStore = KeyStoreSupport.create(storeFile, KeystoreType.JCEKS, STORE_PASSWORD);

		KeyStoreSupport.addSecretKey(keyStore, storeFile, STORE_PASSWORD, "the-secret", algorithm,
			keySize);

		KeyStore reopened = KeyStoreSupport.open(storeFile, KeystoreType.JCEKS, STORE_PASSWORD);
		assertTrue(reopened.containsAlias("the-secret"));
		assertEquals(algorithm,
			reopened.getKey("the-secret", STORE_PASSWORD.toCharArray()).getAlgorithm());
		assertEquals(keySize / 8,
			reopened.getKey("the-secret", STORE_PASSWORD.toCharArray()).getEncoded().length,
			"the key must be as long as it was asked to be");
	}

	@Test
	void aSymmetricKeyIsListedAsOne(@TempDir File directory) throws Exception
	{
		File storeFile = new File(directory, "listed.jceks");
		KeyStore keyStore = KeyStoreSupport.create(storeFile, KeystoreType.JCEKS, STORE_PASSWORD);
		KeyStoreSupport.addSecretKey(keyStore, storeFile, STORE_PASSWORD, "the-secret", "AES", 256);

		List<KeyStoreSupport.EntryInfo> entries = KeyStoreSupport
			.entries(KeyStoreSupport.open(storeFile, KeystoreType.JCEKS, STORE_PASSWORD));

		assertEquals(1, entries.size());
		assertEquals("the-secret", entries.get(0).alias());
	}

	@ParameterizedTest
	@EnumSource(value = KeystoreType.class, names = { "PKCS12", "JKS", "JCEKS" })
	void worksOutWhatKindOfStoreAFileIs(KeystoreType type, @TempDir File directory) throws Exception
	{
		File storeFile = new File(directory, "store-" + type.name());
		KeyStore keyStore = KeyStoreSupport.create(storeFile, type, STORE_PASSWORD);
		KeyStoreSupport.addKeyPair(keyStore, storeFile, STORE_PASSWORD, "entry", "CN=entry",
			KeyPairGeneratorAlgorithm.RSA);

		assertEquals(type, KeyStoreSupport.detectType(storeFile, STORE_PASSWORD),
			"the file has to say what it is, so nobody has to know beforehand");
	}

	@ParameterizedTest
	@ValueSource(strings = { "not the password", "" })
	void aFileThatDoesNotOpenHasNoDetectableType(String password, @TempDir File directory)
		throws Exception
	{
		File storeFile = new File(directory, "locked.p12");
		KeyStoreSupport.create(storeFile, KeystoreType.PKCS12, STORE_PASSWORD);

		assertNull(KeyStoreSupport.detectType(storeFile, password),
			"a wrong password must not be answered with a guess");
	}

	@Test
	void aFileThatIsNoKeyStoreAtAllHasNoType(@TempDir File directory) throws Exception
	{
		File notAStore = new File(directory, "notes.txt");
		java.nio.file.Files.writeString(notAStore.toPath(), "just some text");

		assertNull(KeyStoreSupport.detectType(notAStore, STORE_PASSWORD));
	}
}
