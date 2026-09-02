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
package io.github.astrapi69.mystic.crypt.plugin.certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.KeyInfoExtensions;
import io.github.astrapi69.mystic.crypt.wizard.CertificateInfoModelToX509;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.KeyInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.ValidityModel;

/**
 * Covers three independent behaviors of {@link CertificateMenuContribution}: there is no in-app
 * viewer for a certificate file, so "open" after creating one has to go through whatever the
 * operating system associates with it - a system that offers no way to open files must fail with a
 * reason a user can act on, rather than silently doing nothing or throwing something unexplained;
 * the "Certificate created" confirmation has to actually say what was created (#136), not just
 * where it went; and every key algorithm the settings document as valid ("RSA, EC, DSA or
 * Ed25519") actually has to work, not just the RSA default (#141).
 */
class CertificateMenuContributionTest
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
	void aSystemWithNoWayToOpenFilesSaysSo(@TempDir File directory) throws Exception
	{
		File file = new File(directory, "test.crt");
		file.createNewFile();

		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
		{
			// the CI/Xvfb environment this normally runs in has no desktop session to open
			// anything with, which is exactly the case this test pins - a real desktop is free to
			// skip it
			return;
		}

		IOException thrown = assertThrows(IOException.class,
			() -> CertificateMenuContribution.openWithSystemDefault(file));
		assertTrue(thrown.getMessage() != null && !thrown.getMessage().isBlank(),
			"the reason has to be shown, not swallowed");
	}

	@Test
	void theSummaryNamesEveryImportantFieldOfTheCertificate() throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		ZonedDateTime now = ZonedDateTime.now();
		CertificateInfoModel model = CertificateInfoModel.builder()
			.privateKeyInfo(
				KeyInfoModel.toKeyInfoModel(KeyInfoExtensions.toKeyInfo(keyPair.getPrivate())))
			.publicKeyInfo(
				KeyInfoModel.toKeyInfoModel(KeyInfoExtensions.toKeyInfo(keyPair.getPublic())))
			.issuer(DistinguishedNameInfoModel.builder().commonName("test ca").build())
			.subject(DistinguishedNameInfoModel.builder().commonName("test subject").build())
			.validityModel(
				ValidityModel.builder().notBefore(now).notAfter(now.plusYears(1)).build())
			.serial(BigInteger.valueOf(305441741)).build();
		X509Certificate certificate = CertificateInfoModelToX509.toX509Certificate(model);

		String summary = CertificateMenuContribution.certificateSummary(certificate);

		assertTrue(summary.contains("test subject"), "the subject must be named: " + summary);
		assertTrue(summary.contains("test ca"), "the issuer must be named: " + summary);
		assertTrue(summary.contains("1234abcd"),
			"the serial must be shown, in the hex a certificate viewer reports it in: " + summary);
		assertTrue(summary.toUpperCase(java.util.Locale.ROOT).contains("SHA256WITHRSA"),
			"the signature algorithm must be named: " + summary);
		assertTrue(summary.contains("RSA"), "the public key algorithm must be named: " + summary);
	}

	@ParameterizedTest
	@ValueSource(strings = { "RSA", "EC", "DSA", "Ed25519" })
	@DisplayName("every key algorithm the settings document as valid actually generates a key pair")
	void everyDocumentedKeyAlgorithmGeneratesAKeyPair(String keyAlgorithm) throws Exception
	{
		// Ed25519 (and any other fixed-size algorithm) has no meaningful classical bit size and
		// crashes if forced through KeyPairFactory's size-defaulting convenience method
		// (java.security.InvalidParameterException: "Unsupported size: 2048") - #141
		KeyPair keyPair = CertificateMenuContribution.newCertificateKeyPair(keyAlgorithm);

		String actual = keyPair.getPublic().getAlgorithm().toUpperCase(java.util.Locale.ROOT);
		// the JDK reports an EdDSA key's algorithm as the family name "EdDSA", not the specific
		// curve it was requested with - not a bug, just how java.security.interfaces.EdECKey works
		String expected = "ED25519".equals(keyAlgorithm.toUpperCase(java.util.Locale.ROOT))
			? "EDDSA"
			: keyAlgorithm.toUpperCase(java.util.Locale.ROOT);
		assertEquals(expected, actual);
	}

}
