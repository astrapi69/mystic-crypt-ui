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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.crypt.data.factory.CertFactory;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.CertificateWriter;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.crypt.data.key.writer.PublicKeyWriter;

/**
 * Tests of signing with keys that already exist rather than with a throwaway pair: a private key
 * read from a file, a public key read from a file or out of a certificate, and the classical
 * algorithms next to the post-quantum ones.
 */
class SignatureWithRealKeysTest
{

	private static final byte[] DATA = "the quick brown fox".getBytes(StandardCharsets.UTF_8);

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	static java.util.List<String> generatableAlgorithms()
	{
		return SignatureSupport.algorithms();
	}

	/** Every algorithm this tool can generate a key for must sign and verify with it */
	@ParameterizedTest
	@MethodSource("generatableAlgorithms")
	void signsAndVerifiesWithEveryGeneratableAlgorithm(String algorithm) throws Exception
	{
		KeyPair keyPair = SignatureSupport.newKeyPair(algorithm);

		byte[] signature = SignatureSupport.sign(algorithm, keyPair.getPrivate(), DATA);

		assertTrue(SignatureSupport.verify(algorithm, keyPair.getPublic(), DATA, signature));
	}

	/** And the classical ones, which only work with a key that came from somewhere else */
	@ParameterizedTest
	@CsvSource({ "RSA,SHA256withRSA", "RSA,SHA512withRSA", "EC,SHA256withECDSA",
			"EC,SHA512withECDSA", "DSA,SHA256withDSA" })
	void signsAndVerifiesWithTheClassicalAlgorithms(String keyAlgorithm, String signatureAlgorithm)
		throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair(keyAlgorithm);

		byte[] signature = SignatureSupport.sign(signatureAlgorithm, keyPair.getPrivate(), DATA);

		assertTrue(SignatureSupport.verify(signatureAlgorithm, keyPair.getPublic(), DATA,
			signature));
	}

	@ParameterizedTest
	@CsvSource({ "RSA,SHA256withRSA", "EC,SHA256withECDSA", "DSA,SHA256withDSA" })
	void aKeyItselfSaysWhatItCanSignWith(String keyAlgorithm, String expected) throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair(keyAlgorithm);

		assertEquals(expected, SignatureSupport.algorithmFor(keyPair.getPrivate()));
		assertEquals(expected, SignatureSupport.algorithmFor(keyPair.getPublic()));
	}

	@Test
	void anEd25519KeySaysSoToo() throws Exception
	{
		KeyPair keyPair = SignatureSupport.newKeyPair(SignatureSupport.ED25519);

		assertEquals(SignatureSupport.ED25519,
			SignatureSupport.algorithmFor(keyPair.getPrivate()));
	}

	@ParameterizedTest
	@ValueSource(strings = { "Ed25519", "ML-DSA-65" })
	void aGeneratableAlgorithmIsSaidToBeGeneratable(String algorithm)
	{
		assertTrue(SignatureSupport.canGenerateKeyPair(algorithm));
	}

	@ParameterizedTest
	@ValueSource(strings = { "SHA256withRSA", "SHA256withECDSA", "SHA256withDSA" })
	void aClassicalAlgorithmIsNotGeneratableHere(String algorithm)
	{
		assertFalse(SignatureSupport.canGenerateKeyPair(algorithm),
			"a classical key comes from a key store or a certificate authority, not from here");
		assertTrue(SignatureSupport.allAlgorithms().contains(algorithm),
			"but it must still be offered for signing");
	}

	@ParameterizedTest
	@CsvSource({ "RSA,SHA256withRSA", "EC,SHA256withECDSA" })
	void signsWithAPrivateKeyReadFromAPemFile(String keyAlgorithm, String signatureAlgorithm,
		@TempDir File directory) throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair(keyAlgorithm);
		File privateKeyFile = new File(directory, "private.pem");
		PrivateKeyWriter.writeInPemFormat(keyPair.getPrivate(), privateKeyFile);
		File publicKeyFile = new File(directory, "public.pem");
		PublicKeyWriter.writeInPemFormat(keyPair.getPublic(), publicKeyFile);

		PrivateKey privateKey = SignatureSupport.readPrivateKey(privateKeyFile);
		PublicKey publicKey = SignatureSupport.readPublicKey(publicKeyFile);

		byte[] signature = SignatureSupport.sign(signatureAlgorithm, privateKey, DATA);
		assertTrue(SignatureSupport.verify(signatureAlgorithm, publicKey, DATA, signature),
			"a key that came from a file must work exactly like one that was generated");
	}

	@Test
	void verifiesAgainstThePublicKeyInsideACertificate(@TempDir File directory) throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		X500Name name = new X500Name("CN=signature test");
		X509Certificate certificate = CertFactory.newX509CertificateV3(keyPair, name,
			BigInteger.ONE, new Date(System.currentTimeMillis() - 1000),
			new Date(System.currentTimeMillis() + 86_400_000L), name, "SHA256withRSA");
		File certificateFile = new File(directory, "certificate.pem");
		CertificateWriter.writeInPemFormat(certificate, certificateFile);

		PublicKey publicKey = SignatureSupport.readPublicKey(certificateFile);

		byte[] signature = SignatureSupport.sign("SHA256withRSA", keyPair.getPrivate(), DATA);
		assertTrue(SignatureSupport.verify("SHA256withRSA", publicKey, DATA, signature),
			"a public key most often arrives inside a certificate");
	}

	@Test
	void aChangedMessageDoesNotVerify() throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		byte[] signature = SignatureSupport.sign("SHA256withRSA", keyPair.getPrivate(), DATA);

		assertFalse(SignatureSupport.verify("SHA256withRSA", keyPair.getPublic(),
			"the quick brown cat".getBytes(StandardCharsets.UTF_8), signature));
	}

	@Test
	void anotherKeyDoesNotVerify() throws Exception
	{
		KeyPair signing = KeyPairFactory.newKeyPair("RSA");
		KeyPair other = KeyPairFactory.newKeyPair("RSA");
		byte[] signature = SignatureSupport.sign("SHA256withRSA", signing.getPrivate(), DATA);

		assertFalse(SignatureSupport.verify("SHA256withRSA", other.getPublic(), DATA, signature));
	}

	@Test
	void aFileThatHoldsNoKeyIsReportedAsSuch(@TempDir File directory) throws Exception
	{
		File notAKey = new File(directory, "notes.txt");
		Files.writeString(notAKey.toPath(), "just some text");

		assertThrows(Exception.class, () -> SignatureSupport.readPrivateKey(notAKey));
		assertThrows(Exception.class, () -> SignatureSupport.readPublicKey(notAKey));
	}

	@Test
	void aSignatureOverAFileIsTheSameAsOverItsBytes(@TempDir File directory) throws Exception
	{
		KeyPair keyPair = SignatureSupport.newKeyPair(SignatureSupport.ED25519);
		File file = new File(directory, "content.bin");
		Files.write(file.toPath(), DATA);

		byte[] fromFile = SignatureSupport.sign(SignatureSupport.ED25519, keyPair.getPrivate(),
			Files.readAllBytes(file.toPath()));

		assertTrue(SignatureSupport.verify(SignatureSupport.ED25519, keyPair.getPublic(), DATA,
			fromFile));
		assertArrayEquals(DATA, Files.readAllBytes(file.toPath()),
			"signing must leave the file alone");
	}
}
