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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.Security;
import java.security.cert.X509Certificate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.key.KeySize;
import io.github.astrapi69.crypt.data.key.reader.CertificateReader;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;

/**
 * "Save certificate..." offered a wizard and a file chooser and then wrote nothing at all: the body
 * that was to build and write the certificate was commented out. These tests pin that a file is
 * written, that what is written is the certificate of the generated key pair, and that a key pair
 * which cannot carry this certificate is refused with a message instead of an empty file.
 */
class SaveCertificateTest
{

	@TempDir
	File temporaryDirectory;

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private static GenerateKeysPanel panelWithAnRsaKeyPair()
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();
		panel.getCmbAlgorithm().setSelectedItem(KeyPairGeneratorAlgorithm.RSA);
		panel.getCryptographyPanel().getCmbKeySize().setSelectedItem(KeySize.KEYSIZE_1024);
		panel.getCryptographyPanel().getBtnGenerate().doClick();
		return panel;
	}

	private static CertificateInfoModel named(final CertificateInfoModel certificateInfo,
		final String commonName)
	{
		certificateInfo
			.setIssuer(DistinguishedNameInfoModel.builder().commonName(commonName).build());
		certificateInfo
			.setSubject(DistinguishedNameInfoModel.builder().commonName(commonName).build());
		return certificateInfo;
	}

	@Test
	@DisplayName("saving writes the certificate of the generated key pair to the chosen file")
	void savingWritesTheCertificateOfTheGeneratedKeyPair() throws Exception
	{
		GenerateKeysPanel panel = panelWithAnRsaKeyPair();
		CertificateInfoModel certificateInfo = named(
			KeygenSupport.newCertificateInfo(panel.getModelObject().getPublicKey(),
				panel.getModelObject().getPrivateKey()),
			"CN=the generated pair");
		File certificateFile = new File(temporaryDirectory, "generated.pem");

		KeygenSupport.writeCertificate(certificateInfo, certificateFile);

		assertTrue(certificateFile.exists(), "the certificate was not written at all");
		assertTrue(Files.readString(certificateFile.toPath()).contains("BEGIN CERTIFICATE"),
			"what was written is not a certificate in PEM form");
		X509Certificate written = CertificateReader.readPemCertificate(certificateFile);
		assertNotNull(written);
		assertEquals(panel.getModelObject().getPublicKey(), written.getPublicKey(),
			"the certificate does not carry the public key that was generated");
	}

	@Test
	@DisplayName("the certificate written verifies with the key pair that signed it")
	void theCertificateVerifiesWithTheKeyPairThatSignedIt() throws Exception
	{
		GenerateKeysPanel panel = panelWithAnRsaKeyPair();
		File certificateFile = new File(temporaryDirectory, "verifiable.pem");

		KeygenSupport.writeCertificate(
			named(
				KeygenSupport.newCertificateInfo(panel.getModelObject().getPublicKey(),
					panel.getModelObject().getPrivateKey()),
				"CN=verifiable"),
			certificateFile);

		X509Certificate written = CertificateReader.readPemCertificate(certificateFile);
		written.verify(panel.getModelObject().getPublicKey());
	}

	@Test
	@DisplayName("the certificate carries the validity the model describes")
	void theCertificateCarriesTheValidityTheModelDescribes() throws Exception
	{
		GenerateKeysPanel panel = panelWithAnRsaKeyPair();
		CertificateInfoModel certificateInfo = named(
			KeygenSupport.newCertificateInfo(panel.getModelObject().getPublicKey(),
				panel.getModelObject().getPrivateKey()),
			"CN=valid from now");
		File certificateFile = new File(temporaryDirectory, "validity.pem");

		KeygenSupport.writeCertificate(certificateInfo, certificateFile);

		X509Certificate written = CertificateReader.readPemCertificate(certificateFile);
		written.checkValidity();
	}

}
