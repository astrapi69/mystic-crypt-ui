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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.key.PemType;
import io.github.astrapi69.crypt.data.factory.CertFactory;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.CertificateWriter;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.crypt.data.key.writer.PublicKeyWriter;
import io.github.astrapi69.mystic.crypt.crypto.KeyFiles;

/**
 * Tests of working out what a key file holds rather than being told, and of converting it between
 * the shapes it can have.
 */
class ConversionSupportTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private File writePrivateKeyPem(File directory, String algorithm, String name) throws Exception
	{
		File file = new File(directory, name);
		PrivateKeyWriter.writeInPemFormat(KeyPairFactory.newKeyPair(algorithm).getPrivate(), file);
		return file;
	}

	private File writeCertificatePem(File directory) throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		X500Name name = new X500Name("CN=conversion test");
		X509Certificate certificate = CertFactory.newX509CertificateV3(keyPair, name, BigInteger.ONE,
			new Date(System.currentTimeMillis() - 1000),
			new Date(System.currentTimeMillis() + 86_400_000L), name, "SHA256withRSA");
		File file = new File(directory, "certificate.pem");
		CertificateWriter.writeInPemFormat(certificate, file);
		return file;
	}

	/** What the library writes for each key type, and what this tool then says it is */
	@ParameterizedTest
	@CsvSource({ "RSA,RSA_PRIVATE_KEY", "EC,EC_PRIVATE_KEY", "DSA,DSA_PRIVATE_KEY" })
	void saysWhatAPrivateKeyFileHolds(String algorithm, PemType expected, @TempDir File directory)
		throws Exception
	{
		File file = writePrivateKeyPem(directory, algorithm, "private.pem");

		ConversionSupport.FileKind kind = ConversionSupport.kindOf(file);

		assertTrue(kind.pem());
		assertEquals(expected, kind.pemType());
		assertFalse(kind.description().isBlank());
	}

	@Test
	void saysWhatACertificateFileHolds(@TempDir File directory) throws Exception
	{
		ConversionSupport.FileKind kind = ConversionSupport.kindOf(writeCertificatePem(directory));

		assertEquals(PemType.CERTIFICATE, kind.pemType());
		assertEquals("a certificate", kind.description());
	}

	@Test
	void saysWhatAPublicKeyFileHolds(@TempDir File directory) throws Exception
	{
		File file = new File(directory, "public.pem");
		PublicKeyWriter.writeInPemFormat(KeyPairFactory.newKeyPair("RSA").getPublic(), file);

		assertEquals(PemType.PUBLIC_KEY, ConversionSupport.kindOf(file).pemType());
	}

	@Test
	void saysWhatADerFileHolds(@TempDir File directory) throws Exception
	{
		PrivateKey privateKey = KeyPairFactory.newKeyPair("RSA").getPrivate();
		File der = new File(directory, "private.der");
		Files.write(der.toPath(), privateKey.getEncoded());

		ConversionSupport.FileKind kind = ConversionSupport.kindOf(der);

		assertFalse(kind.pem(), "a der file carries no header");
		assertEquals("a private key in DER form", kind.description());
	}

	@Test
	void aFileThatHoldsNothingOfTheSortSaysThat(@TempDir File directory) throws Exception
	{
		File notes = new File(directory, "notes.txt");
		Files.writeString(notes.toPath(), "just some text that is not a key");

		assertEquals("nothing this tool recognises",
			ConversionSupport.kindOf(notes).description());
	}

	@Test
	void aMissingFileIsSaidToBeMissing(@TempDir File directory)
	{
		assertThrows(IllegalArgumentException.class,
			() -> ConversionSupport.kindOf(new File(directory, "not there")));
	}

	@Test
	void convertsPemToDerAndBackAgain(@TempDir File directory) throws Exception
	{
		PrivateKey original = KeyPairFactory.newKeyPair("RSA").getPrivate();
		File pem = new File(directory, "key.pem");
		PrivateKeyWriter.writeInPemFormat(original, pem);
		File der = new File(directory, "key.der");
		File backToPem = new File(directory, "back.pem");

		ConversionSupport.pemToDer(pem, der);
		assertTrue(der.exists());
		assertFalse(Files.readString(der.toPath(), java.nio.charset.StandardCharsets.ISO_8859_1)
			.contains("BEGIN"), "der carries no header");

		ConversionSupport.derToPem(der, backToPem);

		assertTrue(Files.readString(backToPem.toPath()).contains("BEGIN"));
		assertEquals(original, KeyFiles.readPrivateKey(backToPem),
			"the key has to survive the way there and back");
	}

	@Test
	void convertsAPkcs1KeyToPkcs8(@TempDir File directory) throws Exception
	{
		// what the library writes is PKCS#1 under an algorithm specific header
		PrivateKey original = KeyPairFactory.newKeyPair("RSA").getPrivate();
		File pkcs1 = new File(directory, "pkcs1.pem");
		PrivateKeyWriter.writeInPemFormat(original, pkcs1);
		assertEquals(PemType.RSA_PRIVATE_KEY, ConversionSupport.kindOf(pkcs1).pemType());
		File pkcs8 = new File(directory, "pkcs8.pem");

		ConversionSupport.toPkcs8(pkcs1, pkcs8);

		assertEquals(PemType.PRIVATE_KEY, ConversionSupport.kindOf(pkcs8).pemType(),
			"PKCS#8 is what Java reads and writes, and it says so in its header");
		assertEquals(original, KeyFiles.readPrivateKey(pkcs8), "it has to be the same key");
	}

	@Test
	void convertsAPkcs8KeyToPkcs1(@TempDir File directory) throws Exception
	{
		PrivateKey original = KeyPairFactory.newKeyPair("RSA").getPrivate();
		File source = new File(directory, "source.pem");
		PrivateKeyWriter.writeInPemFormat(original, source);
		File pkcs8 = new File(directory, "pkcs8.pem");
		ConversionSupport.toPkcs8(source, pkcs8);
		File pkcs1 = new File(directory, "pkcs1.pem");

		ConversionSupport.toPkcs1(pkcs8, pkcs1);

		assertEquals(PemType.RSA_PRIVATE_KEY, ConversionSupport.kindOf(pkcs1).pemType(),
			"PKCS#1 is what openssl and nginx expect, and it names the algorithm in its header");
		assertEquals(original, KeyFiles.readPrivateKey(pkcs1),
			"a key converted there and back has to stay the same key");
	}

	@Test
	void aFileThatIsAlreadyPemIsNotConvertedToPemAgain(@TempDir File directory) throws Exception
	{
		File pem = writePrivateKeyPem(directory, "RSA", "already.pem");

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> ConversionSupport.derToPem(pem, new File(directory, "again.pem")));

		assertTrue(exception.getMessage().contains("already pem"), exception.getMessage());
	}

	@Test
	void aFileThatIsNotPemIsNotConvertedToDer(@TempDir File directory) throws Exception
	{
		File notes = new File(directory, "notes.txt");
		Files.writeString(notes.toPath(), "not a pem file");

		assertThrows(Exception.class,
			() -> ConversionSupport.pemToDer(notes, new File(directory, "out.der")));
	}

	@Test
	void anExistingFileIsNeverOverwritten(@TempDir File directory) throws Exception
	{
		File pem = writePrivateKeyPem(directory, "RSA", "key.pem");
		File occupied = new File(directory, "occupied.der");
		Files.writeString(occupied.toPath(), "something valuable");

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> ConversionSupport.pemToDer(pem, occupied));

		assertTrue(exception.getMessage().contains("already exists"), exception.getMessage());
		assertEquals("something valuable", Files.readString(occupied.toPath()));
	}

	@ParameterizedTest
	@CsvSource({ "RSA,true", "EC,true", "DSA,true" })
	void saysWhetherAFileHoldsAPrivateKey(String algorithm, boolean expected,
		@TempDir File directory) throws Exception
	{
		assertEquals(expected,
			ConversionSupport.holdsAPrivateKey(writePrivateKeyPem(directory, algorithm, "key.pem")));
	}

	@Test
	void aCertificateIsNotAPrivateKey(@TempDir File directory) throws Exception
	{
		assertFalse(ConversionSupport.holdsAPrivateKey(writeCertificatePem(directory)),
			"the two format conversions only make sense for a private key");
	}

	@ParameterizedTest
	@ValueSource(strings = { "RSA_PRIVATE_KEY", "PRIVATE_KEY", "EC_PRIVATE_KEY", "CERTIFICATE",
			"PUBLIC_KEY", "X509_CRL", "PKCS7_KEY" })
	void saysWhatEveryKnownTypeIsCalled(String pemTypeName)
	{
		String description = ConversionSupport.describe(PemType.valueOf(pemTypeName));

		assertFalse(description.isBlank());
		assertFalse(description.contains("does not recognise"),
			"a type the tool knows must be named, was: " + description);
	}

	@Test
	void aTypeTheToolDoesNotKnowSaysSo()
	{
		assertTrue(ConversionSupport.describe(PemType.UNKNOWN).contains("does not recognise"));
	}

	/**
	 * A key whose algorithm has no traditional form cannot be written as PKCS#1. crypt-data's writer
	 * falls through to PKCS#8 for those, so this used to produce a file the user did not ask for and
	 * say nothing about it. It must refuse, and leave nothing behind. See crypt-data#42.
	 *
	 * @param algorithm
	 *            an algorithm whose private key has no traditional form
	 * @param directory
	 *            the directory the files are written to
	 * @throws Exception
	 *             if the source key cannot be written
	 */
	@ParameterizedTest(name = "toPkcs1 refuses {0}")
	@EnumSource(value = KeyPairGeneratorAlgorithm.class,
		names = { "ML_DSA_65", "ML_KEM_768", "X25519", "X448" })
	void refusesToWritePkcs1ForAKeyThatHasNoTraditionalForm(KeyPairGeneratorAlgorithm algorithm,
		@TempDir File directory) throws Exception
	{
		PrivateKey original = KeyPairFactory.newKeyPair(algorithm).getPrivate();
		File source = new File(directory, "source.pem");
		PrivateKeyWriter.writeInPemFormat(original, source);
		File pkcs1 = new File(directory, "pkcs1.pem");

		Exception refused = assertThrows(Exception.class,
			() -> ConversionSupport.toPkcs1(source, pkcs1));

		assertTrue(refused.getMessage() != null && refused.getMessage().contains("PKCS#1"),
			"the message must say what was asked for, but was: '" + refused.getMessage() + "'");
		assertFalse(pkcs1.exists(),
			"nothing may be left behind when the requested format cannot be produced");
	}

	/**
	 * The counterpart, so the refusal above cannot be satisfied by refusing everything: the
	 * algorithms that do have a traditional form still convert, and the file carries their own
	 * header rather than the generic PKCS#8 one.
	 *
	 * @param algorithm
	 *            an algorithm whose private key has a traditional form
	 * @param directory
	 *            the directory the files are written to
	 * @throws Exception
	 *             if the key cannot be written or read back
	 */
	@ParameterizedTest(name = "toPkcs1 keeps working for {0}")
	@EnumSource(value = KeyPairGeneratorAlgorithm.class, names = { "RSA", "DSA" })
	void stillConvertsTheAlgorithmsThatDoHaveATraditionalForm(KeyPairGeneratorAlgorithm algorithm,
		@TempDir File directory) throws Exception
	{
		PrivateKey original = KeyPairFactory.newKeyPair(algorithm).getPrivate();
		File source = new File(directory, "source.pem");
		PrivateKeyWriter.writeInPemFormat(original, source);
		File pkcs1 = new File(directory, "pkcs1.pem");

		ConversionSupport.toPkcs1(source, pkcs1);

		assertFalse("-----BEGIN PRIVATE KEY-----".equals(Files.readAllLines(pkcs1.toPath()).get(0)),
			algorithm + " has a traditional form, so the generic PKCS#8 label is the wrong answer");
		assertEquals(original, KeyFiles.readPrivateKey(pkcs1), "it has to be the same key");
	}

	/**
	 * EC gets its own case because the product never generates a curveless EC key: this plugin and
	 * mystic-crypt's CLI both name a curve, and a key from the curveless factory call cannot be
	 * written to PEM at all - Bouncy Castle throws, because the PEM key pair it builds carries no
	 * public key. Driving the path the product does not use would prove nothing about the one it
	 * does.
	 *
	 * @param directory
	 *            the directory the files are written to
	 * @throws Exception
	 *             if the key cannot be written or read back
	 */
	@Test
	void stillConvertsAnEcKeyOnANamedCurve(@TempDir File directory) throws Exception
	{
		PrivateKey original = KeyPairFactory
			.newKeyPair("secp256r1", "EC", BouncyCastleProvider.PROVIDER_NAME).getPrivate();
		File source = new File(directory, "source.pem");
		PrivateKeyWriter.writeInPemFormat(original, source);
		File pkcs1 = new File(directory, "pkcs1.pem");

		ConversionSupport.toPkcs1(source, pkcs1);

		assertEquals("-----BEGIN EC PRIVATE KEY-----", Files.readAllLines(pkcs1.toPath()).get(0),
			"RFC 5915 is EC's traditional form and names the algorithm in its header");
	}
}
