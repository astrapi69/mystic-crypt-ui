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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.Security;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.type.KeystoreType;

/**
 * Tests of {@link KeyStoreSupport} against real key store files in a temporary directory: creating,
 * filling, listing, exporting, importing and deleting - the same sequence the tool performs.
 */
class KeyStoreSupportTest
{

	private static final String STORE_PASSWORD = "throwaway-" + java.util.UUID.randomUUID();

	@BeforeAll
	static void registerBouncyCastle()
	{
		// self-contained on purpose: the application registers the provider at startup, but a test
		// must never depend on another class having run first
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	static List<Arguments> keyAlgorithms()
	{
		return KeyStoreSupport.KEY_ALGORITHMS.stream().map(Arguments::of).toList();
	}

	private File newStoreFile(File directory, String name)
	{
		return new File(directory, name);
	}

	/**
	 * Every offered store type must survive the full round trip: create it, put a key pair in, close
	 * it, open it again and find the entry with its certificate details.
	 */
	@ParameterizedTest
	@EnumSource(value = KeystoreType.class, names = { "PKCS12", "JKS", "JCEKS" })
	void createsFillsAndReopensEveryOfferedStoreType(KeystoreType type, @TempDir File directory)
		throws Exception
	{
		File file = newStoreFile(directory, "store-" + type.name() + ".ks");

		KeyStore created = KeyStoreSupport.create(file, type, STORE_PASSWORD);
		KeyStoreSupport.addKeyPair(created, file, STORE_PASSWORD, "server", "CN=server, O=test",
			KeyPairGeneratorAlgorithm.RSA);

		KeyStore reopened = KeyStoreSupport.open(file, type, STORE_PASSWORD);
		List<KeyStoreSupport.EntryInfo> entries = KeyStoreSupport.entries(reopened);

		assertEquals(1, entries.size(), "the reopened store must hold the entry that was added");
		KeyStoreSupport.EntryInfo entry = entries.get(0);
		assertEquals("server", entry.alias());
		assertEquals("private key", entry.entryKind());
		assertTrue(entry.subject().contains("CN=server"),
			"the subject must come from the distinguished name, was: " + entry.subject());
		assertFalse(entry.fingerprint().isBlank(), "a certificate entry must show a fingerprint");
		assertFalse(entry.validUntil().isBlank(), "a certificate entry must show its validity");
	}

	/**
	 * Every key algorithm the tool offers must produce a usable entry - including the post-quantum
	 * ML-DSA, which is the reason this plugin offers more than RSA.
	 */
	@ParameterizedTest
	@MethodSource("keyAlgorithms")
	void addsAKeyPairForEveryOfferedAlgorithm(KeyPairGeneratorAlgorithm algorithm,
		@TempDir File directory) throws Exception
	{
		File file = newStoreFile(directory, "algorithms.p12");
		KeyStore keyStore = KeyStoreSupport.create(file, KeystoreType.PKCS12, STORE_PASSWORD);

		KeyStoreSupport.addKeyPair(keyStore, file, STORE_PASSWORD, "entry", "CN=" + algorithm,
			algorithm);

		KeyStore reopened = KeyStoreSupport.open(file, KeystoreType.PKCS12, STORE_PASSWORD);
		assertNotNull(KeyStoreSupport.privateKey(reopened, "entry", STORE_PASSWORD),
			"the stored private key must be readable again");
		assertNotNull(KeyStoreSupport.serialNumber(reopened, "entry"),
			"the self-signed certificate must carry a serial number");
	}

	@ParameterizedTest
	@CsvSource({ "RSA,SHA256withRSA", "RSASSA_PSS,SHA256withRSAandMGF1", "EC,SHA256withECDSA",
			"DSA,SHA256withDSA", "ML_DSA_44,ML-DSA-44", "ML_DSA_65,ML-DSA-65",
			"ML_DSA_87,ML-DSA-87" })
	void mapsEverySigningAlgorithmToItsSignatureAlgorithm(KeyPairGeneratorAlgorithm algorithm,
		String expected)
	{
		assertEquals(expected, KeyStoreSupport.signatureAlgorithmFor(algorithm));
	}

	/** Key exchange algorithms cannot sign a certificate and must be rejected, not silently mapped */
	@ParameterizedTest
	@EnumSource(value = KeyPairGeneratorAlgorithm.class, names = { "X25519", "X448", "ML_KEM_768",
			"DIFFIE_HELLMAN" })
	void refusesAlgorithmsThatCannotSignACertificate(KeyPairGeneratorAlgorithm algorithm)
	{
		assertThrows(IllegalArgumentException.class,
			() -> KeyStoreSupport.signatureAlgorithmFor(algorithm));
	}

	@ParameterizedTest
	@CsvSource({ "RSA,true", "DSA,true", "DIFFIE_HELLMAN,true", "DH,true", "EC,false",
			"X25519,false", "ML_DSA_65,false", "ML_KEM_768,false" })
	void knowsWhichAlgorithmsNeedAKeySize(KeyPairGeneratorAlgorithm algorithm, boolean sizeBased)
	{
		assertEquals(sizeBased, KeyStoreSupport.isSizeBased(algorithm));
	}

	@Test
	void exportsACertificateAsPemAndImportsItBack(@TempDir File directory) throws Exception
	{
		File file = newStoreFile(directory, "export.p12");
		KeyStore keyStore = KeyStoreSupport.create(file, KeystoreType.PKCS12, STORE_PASSWORD);
		KeyStoreSupport.addKeyPair(keyStore, file, STORE_PASSWORD, "server", "CN=server",
			KeyPairGeneratorAlgorithm.RSA);

		File pem = new File(directory, "server.pem");
		KeyStoreSupport.exportCertificate(keyStore, "server", pem);

		assertTrue(Files.readString(pem.toPath()).contains("BEGIN CERTIFICATE"),
			"the exported file must be PEM");

		KeyStoreSupport.importCertificate(keyStore, file, STORE_PASSWORD, "trusted", pem);

		KeyStore reopened = KeyStoreSupport.open(file, KeystoreType.PKCS12, STORE_PASSWORD);
		List<KeyStoreSupport.EntryInfo> entries = KeyStoreSupport.entries(reopened);
		assertEquals(2, entries.size(), "the store must hold the key entry and the imported one");
		KeyStoreSupport.EntryInfo imported = entries.stream()
			.filter(entry -> "trusted".equals(entry.alias())).findFirst().orElseThrow();
		assertEquals("certificate", imported.entryKind(),
			"an imported certificate must not be listed as a private key");
		assertEquals(
			entries.stream().filter(entry -> "server".equals(entry.alias())).findFirst()
				.orElseThrow().fingerprint(),
			imported.fingerprint(),
			"the imported certificate must be the very same one that was exported");
	}

	@Test
	void deletesAnAliasFromTheFile(@TempDir File directory) throws Exception
	{
		File file = newStoreFile(directory, "delete.p12");
		KeyStore keyStore = KeyStoreSupport.create(file, KeystoreType.PKCS12, STORE_PASSWORD);
		KeyStoreSupport.addKeyPair(keyStore, file, STORE_PASSWORD, "first", "CN=first",
			KeyPairGeneratorAlgorithm.RSA);
		KeyStoreSupport.addKeyPair(keyStore, file, STORE_PASSWORD, "second", "CN=second",
			KeyPairGeneratorAlgorithm.RSA);

		KeyStore afterDeletion = KeyStoreSupport.deleteAlias(file, KeystoreType.PKCS12,
			STORE_PASSWORD, "first");

		assertEquals(List.of("second"),
			KeyStoreSupport.entries(afterDeletion).stream()
				.map(KeyStoreSupport.EntryInfo::alias).toList(),
			"the deleted alias must be gone from the file, the other one must stay");
	}

	@Test
	void aWrongStorePasswordIsReported(@TempDir File directory) throws Exception
	{
		File file = newStoreFile(directory, "wrong-password.p12");
		KeyStoreSupport.create(file, KeystoreType.PKCS12, STORE_PASSWORD);

		assertThrows(Exception.class,
			() -> KeyStoreSupport.open(file, KeystoreType.PKCS12, "not the password"));
	}

	@Test
	void anAliasWithoutACertificateIsReportedInsteadOfReturningNull(@TempDir File directory)
		throws Exception
	{
		File file = newStoreFile(directory, "empty.p12");
		KeyStore keyStore = KeyStoreSupport.create(file, KeystoreType.PKCS12, STORE_PASSWORD);

		assertThrows(IllegalArgumentException.class,
			() -> KeyStoreSupport.exportCertificate(keyStore, "does not exist",
				new File(directory, "nothing.pem")));
		assertThrows(IllegalArgumentException.class,
			() -> KeyStoreSupport.serialNumber(keyStore, "does not exist"));
	}

	@Test
	void anEmptyStoreListsNoEntries(@TempDir File directory) throws Exception
	{
		File file = newStoreFile(directory, "no-entries.p12");

		assertTrue(
			KeyStoreSupport.entries(KeyStoreSupport.create(file, KeystoreType.PKCS12,
				STORE_PASSWORD)).isEmpty(),
			"a freshly created store has nothing to list");
	}
}
