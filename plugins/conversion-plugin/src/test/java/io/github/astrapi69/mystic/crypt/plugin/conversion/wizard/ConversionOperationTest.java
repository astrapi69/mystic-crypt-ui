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
package io.github.astrapi69.mystic.crypt.plugin.conversion.wizard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.api.key.PemType;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.mystic.crypt.crypto.KeyFiles;
import io.github.astrapi69.mystic.crypt.plugin.conversion.ConversionSupport;

/**
 * Tests of what each {@link ConversionOperation} offers itself for, what it produces, and what
 * default target file name it proposes - the logic that used to live scattered across
 * {@code ConversionPanel.setConversionsFor(...)} and its four button handlers.
 */
class ConversionOperationTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private File writePrivateKeyPem(File directory, String name) throws Exception
	{
		File file = new File(directory, name);
		PrivateKeyWriter.writeInPemFormat(KeyPairFactory.newKeyPair("RSA").getPrivate(), file);
		return file;
	}

	@Test
	void pemToDerIsValidOnlyForAPemFile(@TempDir File directory) throws Exception
	{
		File pemFile = writePrivateKeyPem(directory, "key.pem");
		ConversionSupport.FileKind pemKind = ConversionSupport.kindOf(pemFile);

		assertTrue(ConversionOperation.PEM_TO_DER.isValidFor(pemKind, pemFile));

		File derFile = new File(directory, "key.der");
		java.nio.file.Files.write(derFile.toPath(),
			KeyPairFactory.newKeyPair("RSA").getPrivate().getEncoded());
		ConversionSupport.FileKind derKind = ConversionSupport.kindOf(derFile);
		assertFalse(ConversionOperation.PEM_TO_DER.isValidFor(derKind, derFile));
	}

	@Test
	void pemToDerIsInvalidWithNoDetectedKind()
	{
		assertFalse(ConversionOperation.PEM_TO_DER.isValidFor(null, null));
	}

	@Test
	void derToPemIsValidOnlyForARecognisedDerFile(@TempDir File directory) throws Exception
	{
		PrivateKey privateKey = KeyPairFactory.newKeyPair("RSA").getPrivate();
		File derFile = new File(directory, "key.der");
		java.nio.file.Files.write(derFile.toPath(), privateKey.getEncoded());
		ConversionSupport.FileKind derKind = ConversionSupport.kindOf(derFile);

		assertTrue(ConversionOperation.DER_TO_PEM.isValidFor(derKind, derFile));

		File pemFile = writePrivateKeyPem(directory, "key.pem");
		ConversionSupport.FileKind pemKind = ConversionSupport.kindOf(pemFile);
		assertFalse(ConversionOperation.DER_TO_PEM.isValidFor(pemKind, pemFile));
	}

	@Test
	void derToPemIsInvalidForAFileTheToolDoesNotRecognise(@TempDir File directory) throws Exception
	{
		File notes = new File(directory, "notes.txt");
		java.nio.file.Files.writeString(notes.toPath(), "just text");
		ConversionSupport.FileKind kind = ConversionSupport.kindOf(notes);

		assertFalse(ConversionOperation.DER_TO_PEM.isValidFor(kind, notes));
	}

	@Test
	void toPkcs8AndToPkcs1AreValidOnlyForAPrivateKey(@TempDir File directory) throws Exception
	{
		File keyFile = writePrivateKeyPem(directory, "key.pem");
		ConversionSupport.FileKind kind = ConversionSupport.kindOf(keyFile);

		assertTrue(ConversionOperation.TO_PKCS8.isValidFor(kind, keyFile));
		assertTrue(ConversionOperation.TO_PKCS1.isValidFor(kind, keyFile));

		File certificateFile = new File(directory, "cert.pem");
		org.bouncycastle.asn1.x500.X500Name name = new org.bouncycastle.asn1.x500.X500Name(
			"CN=conversion operation test");
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		java.security.cert.X509Certificate certificate = io.github.astrapi69.crypt.data.factory.CertFactory
			.newX509CertificateV3(keyPair, name, java.math.BigInteger.ONE,
				new java.util.Date(System.currentTimeMillis() - 1000),
				new java.util.Date(System.currentTimeMillis() + 86_400_000L), name, "SHA256withRSA");
		io.github.astrapi69.crypt.data.key.writer.CertificateWriter.writeInPemFormat(certificate,
			certificateFile);
		ConversionSupport.FileKind certificateKind = ConversionSupport.kindOf(certificateFile);
		assertFalse(ConversionOperation.TO_PKCS8.isValidFor(certificateKind, certificateFile));
		assertFalse(ConversionOperation.TO_PKCS1.isValidFor(certificateKind, certificateFile));
	}

	@Test
	void toPkcs8IsInvalidWithNoSourceFile()
	{
		assertFalse(ConversionOperation.TO_PKCS8.isValidFor(null, null));
	}

	@Test
	void everyOperationHasAReadableLabel()
	{
		for (ConversionOperation operation : ConversionOperation.values())
		{
			assertFalse(operation.getLabel().isBlank(), operation + " must have a label");
		}
	}

	@Test
	void pemToDerExecutesTheRealConversion(@TempDir File directory) throws Exception
	{
		File pemFile = writePrivateKeyPem(directory, "key.pem");
		File derFile = new File(directory, "key.der");

		ConversionOperation.PEM_TO_DER.execute(pemFile, derFile);

		assertTrue(derFile.exists());
	}

	@Test
	void derToPemExecutesTheRealConversion(@TempDir File directory) throws Exception
	{
		PrivateKey original = KeyPairFactory.newKeyPair("RSA").getPrivate();
		File derFile = new File(directory, "key.der");
		java.nio.file.Files.write(derFile.toPath(), original.getEncoded());
		File pemFile = new File(directory, "key.pem");

		ConversionOperation.DER_TO_PEM.execute(derFile, pemFile);

		assertEquals(original, KeyFiles.readPrivateKey(pemFile));
	}

	@Test
	void toPkcs8ExecutesTheRealConversion(@TempDir File directory) throws Exception
	{
		File source = writePrivateKeyPem(directory, "pkcs1.pem");
		File target = new File(directory, "pkcs8.pem");

		ConversionOperation.TO_PKCS8.execute(source, target);

		assertEquals(PemType.PRIVATE_KEY, ConversionSupport.kindOf(target).pemType());
	}

	@Test
	void toPkcs1ExecutesTheRealConversion(@TempDir File directory) throws Exception
	{
		File source = writePrivateKeyPem(directory, "original.pem");
		File pkcs8 = new File(directory, "pkcs8.pem");
		ConversionSupport.toPkcs8(source, pkcs8);
		File pkcs1 = new File(directory, "pkcs1.pem");

		ConversionOperation.TO_PKCS1.execute(pkcs8, pkcs1);

		assertEquals(PemType.RSA_PRIVATE_KEY, ConversionSupport.kindOf(pkcs1).pemType());
	}

	@Test
	void pemToDerDefaultsToTheSourceNameWithADerEnding(@TempDir File directory)
	{
		File source = new File(directory, "my-key.pem");

		File defaultTarget = ConversionOperation.PEM_TO_DER.defaultTargetFile(source);

		assertEquals(new File(directory, "my-key.der"), defaultTarget);
	}

	@Test
	void derToPemDefaultsToTheSourceNameWithAPemEnding(@TempDir File directory)
	{
		File source = new File(directory, "my-key.der");

		File defaultTarget = ConversionOperation.DER_TO_PEM.defaultTargetFile(source);

		assertEquals(new File(directory, "my-key.pem"), defaultTarget);
	}

	@Test
	void toPkcs8DefaultsToASuffixedPemNextToTheSource(@TempDir File directory)
	{
		File source = new File(directory, "my-key.pem");

		File defaultTarget = ConversionOperation.TO_PKCS8.defaultTargetFile(source);

		assertEquals(new File(directory, "my-key-pkcs8.pem"), defaultTarget);
	}

	@Test
	void toPkcs1DefaultsToASuffixedPemNextToTheSource(@TempDir File directory)
	{
		File source = new File(directory, "my-key.pem");

		File defaultTarget = ConversionOperation.TO_PKCS1.defaultTargetFile(source);

		assertEquals(new File(directory, "my-key-pkcs1.pem"), defaultTarget);
	}
}
