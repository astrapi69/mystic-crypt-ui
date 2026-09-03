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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
 * Covers three independent behaviors of {@link CertificateMenuContribution}: the "Certificate
 * created" confirmation that used to follow saving has to actually say what was created (#136), not
 * just where it went - the review step that replaced it (#146) still uses the same summary; every
 * key algorithm the settings document as valid ("RSA, EC, DSA or Ed25519") actually has to work, not
 * just the RSA default (#141); and the file the review step ends up saving to has to fall back
 * sensibly when the user never typed a name or picked a directory there, since Finish works from any
 * step of the wizard, not only Review.
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

	private static CertificateInfoModel modelWithSubjectCommonName(String commonName)
		throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		ZonedDateTime now = ZonedDateTime.now();
		return CertificateInfoModel.builder()
			.privateKeyInfo(
				KeyInfoModel.toKeyInfoModel(KeyInfoExtensions.toKeyInfo(keyPair.getPrivate())))
			.publicKeyInfo(
				KeyInfoModel.toKeyInfoModel(KeyInfoExtensions.toKeyInfo(keyPair.getPublic())))
			.issuer(DistinguishedNameInfoModel.builder().commonName("test ca").build())
			.subject(DistinguishedNameInfoModel.builder().commonName(commonName).build())
			.validityModel(
				ValidityModel.builder().notBefore(now).notAfter(now.plusYears(1)).build())
			.serial(BigInteger.valueOf(305441741)).build();
	}

	@Test
	void theSummaryNamesEveryImportantFieldOfTheCertificate() throws Exception
	{
		CertificateInfoModel model = modelWithSubjectCommonName("test subject");
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

	@Test
	void defaultFileNameUsesTheSubjectsCommonName() throws Exception
	{
		CertificateInfoModel model = modelWithSubjectCommonName("mystic-crypt");

		assertEquals("mystic-crypt.crt", CertificateMenuContribution.defaultFileName(model));
	}

	@Test
	void defaultFileNameSanitizesWhatTheFilesystemWouldReject() throws Exception
	{
		CertificateInfoModel model = modelWithSubjectCommonName("*.example.com/test?");

		assertEquals("_.example.com_test_.crt",
			CertificateMenuContribution.defaultFileName(model));
	}

	@Test
	void defaultFileNameFallsBackWhenThereIsNoCommonName() throws Exception
	{
		CertificateInfoModel model = modelWithSubjectCommonName(null);

		assertEquals("certificate.crt", CertificateMenuContribution.defaultFileName(model));
	}

	@Test
	void resolveSaveTargetUsesWhatTheReviewStepHolds() throws Exception
	{
		CertificateInfoModel model = modelWithSubjectCommonName("subject");

		File file = CertificateMenuContribution.resolveSaveTarget("chosen.crt",
			new File("/chosen/directory"), model, new File("/default/directory"));

		assertEquals(new File("/chosen/directory", "chosen.crt"), file);
	}

	@Test
	@DisplayName("Finish works even when the review step was never visited, with sensible defaults")
	void resolveSaveTargetFallsBackWhenNothingWasEnteredYet() throws Exception
	{
		CertificateInfoModel model = modelWithSubjectCommonName("subject");

		File file = CertificateMenuContribution.resolveSaveTarget(null, null, model,
			new File("/default/directory"));

		assertEquals(new File("/default/directory", "subject.crt"), file);
	}

}
