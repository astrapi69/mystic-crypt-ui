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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.swing.JComboBox;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.api.key.KeyFileFormat;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypt.data.key.reader.PublicKeyReader;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;

/**
 * The window writes PEM unless the user asks for the binary encoding. The choice has to reach the
 * file, and the file ending has to say which of the two it is - a name that promises PEM over
 * binary content is refused by whatever it is handed to next.
 */
class SaveFormatTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@Test
	@DisplayName("the window starts on PEM")
	void theWindowStartsOnPem()
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();

		assertEquals(KeyFileFormat.PEM, panel.getModelObject().getSaveFormat());
		assertEquals(KeyFileFormat.PEM, panel.getCmbSaveFormat().getSelectedItem(),
			"the box does not show the format the window is set to");
	}

	@Test
	@DisplayName("what is chosen in the box is what the window saves in")
	void whatIsChosenInTheBoxIsWhatTheWindowSavesIn()
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();

		((JComboBox<?>)panel.getCmbSaveFormat()).setSelectedItem(KeyFileFormat.DER);

		assertEquals(KeyFileFormat.DER, panel.getModelObject().getSaveFormat(),
			"the chosen format did not reach the model the save buttons read");
	}

	@Test
	@DisplayName("a private key saved as PEM is text, as DER it is not")
	void aPrivateKeySavedAsPemIsTextAsDerItIsNot(@TempDir File directory) throws Exception
	{
		GenerateKeysPanel panel = KeygenTestPanels.panelWithAnRsaKeyPair();
		File asPem = new File(directory, "key.pem");
		File asDer = new File(directory, "key.der");

		panel.getModelObject().setSaveFormat(KeyFileFormat.PEM);
		panel.savePrivateKeyTo(asPem);
		panel.getModelObject().setSaveFormat(KeyFileFormat.DER);
		panel.savePrivateKeyTo(asDer);

		assertTrue(Files.readString(asPem.toPath()).contains("PRIVATE KEY"),
			"the PEM file does not hold PEM");
		assertFalse(new String(Files.readAllBytes(asDer.toPath())).contains("PRIVATE KEY"),
			"the DER file holds PEM text");
		assertArrayEquals(panel.getModelObject().getPrivateKey().getEncoded(),
			PrivateKeyReader.readPrivateKey(asDer).getEncoded(),
			"the key read back from the DER file is not the key that was generated");
	}

	@Test
	@DisplayName("a public key saved as PEM is text, as DER it is not")
	void aPublicKeySavedAsPemIsTextAsDerItIsNot(@TempDir File directory) throws Exception
	{
		GenerateKeysPanel panel = KeygenTestPanels.panelWithAnRsaKeyPair();
		File asPem = new File(directory, "key.pem");
		File asDer = new File(directory, "key.der");

		panel.getModelObject().setSaveFormat(KeyFileFormat.PEM);
		panel.savePublicKeyTo(asPem);
		panel.getModelObject().setSaveFormat(KeyFileFormat.DER);
		panel.savePublicKeyTo(asDer);

		assertTrue(Files.readString(asPem.toPath()).contains("PUBLIC KEY"),
			"the PEM file does not hold PEM");
		assertFalse(new String(Files.readAllBytes(asDer.toPath())).contains("PUBLIC KEY"),
			"the DER file holds PEM text");
		assertArrayEquals(panel.getModelObject().getPublicKey().getEncoded(),
			PublicKeyReader.readPublicKey(asDer).getEncoded(),
			"the key read back from the DER file is not the key that was generated");
	}

	@Test
	@DisplayName("a certificate saved as PEM is text, as DER it is binary and reads back as the same certificate")
	void aCertificateSavedAsPemIsTextAsDerItIsBinary(@TempDir File directory) throws Exception
	{
		GenerateKeysPanel panel = KeygenTestPanels.panelWithAnRsaKeyPair();
		CertificateInfoModel certificateInfo = KeygenSupport.newCertificateInfo(
			panel.getModelObject().getPublicKey(), panel.getModelObject().getPrivateKey());
		certificateInfo
			.setIssuer(DistinguishedNameInfoModel.builder().commonName("CN=the format").build());
		certificateInfo
			.setSubject(DistinguishedNameInfoModel.builder().commonName("CN=the format").build());
		File asPem = new File(directory, "certificate.pem");
		File asDer = new File(directory, "certificate.der");

		KeygenSupport.writeCertificate(certificateInfo, asPem, KeyFileFormat.PEM);
		KeygenSupport.writeCertificate(certificateInfo, asDer, KeyFileFormat.DER);

		assertTrue(Files.readString(asPem.toPath()).contains("BEGIN CERTIFICATE"),
			"the PEM file does not hold PEM");
		assertFalse(
			new String(Files.readAllBytes(asDer.toPath()), java.nio.charset.StandardCharsets.ISO_8859_1)
				.contains("CERTIFICATE"),
			"the DER file holds PEM text");
		try (java.io.InputStream in = Files.newInputStream(asDer.toPath()))
		{
			X509Certificate read = (X509Certificate)CertificateFactory.getInstance("X.509")
				.generateCertificate(in);
			assertEquals(panel.getModelObject().getPublicKey(), read.getPublicKey(),
				"the certificate read back from the DER file does not carry the generated key");
			read.verify(panel.getModelObject().getPublicKey());
		}
	}

	@Test
	@DisplayName("the file ending follows the chosen format")
	void theFileEndingFollowsTheChosenFormat()
	{
		assertEquals(".pem", KeygenSupport.endingFor(KeyFileFormat.PEM));
		assertEquals(".der", KeygenSupport.endingFor(KeyFileFormat.DER));
	}

}
