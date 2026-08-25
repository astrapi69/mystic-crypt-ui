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
package io.github.astrapi69.mystic.crypt.wizard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.KeyInfoExtensions;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.ExtensionInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.KeyInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.ValidityModel;

/**
 * Tests that a certificate really carries what the wizard collected. The "Extensions" step used to
 * fill the model and go no further - key usage, subject alternative names and basic constraints
 * were built and then dropped on the way to the certificate.
 */
class CertificateInfoModelToX509Test
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private CertificateInfoModel.CertificateInfoModelBuilder<?, ?> newModel(String keyAlgorithm)
		throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair(keyAlgorithm);
		ZonedDateTime now = ZonedDateTime.now();
		return CertificateInfoModel.builder()
			.privateKeyInfo(
				KeyInfoModel.toKeyInfoModel(KeyInfoExtensions.toKeyInfo(keyPair.getPrivate())))
			.publicKeyInfo(
				KeyInfoModel.toKeyInfoModel(KeyInfoExtensions.toKeyInfo(keyPair.getPublic())))
			.issuer(DistinguishedNameInfoModel.builder().commonName("test ca").build())
			.subject(DistinguishedNameInfoModel.builder().commonName("test subject").build())
			.validityModel(
				ValidityModel.builder().notBefore(now).notAfter(now.plusYears(1)).build())
			.serial(BigInteger.valueOf(now.toInstant().toEpochMilli()));
	}

	@Test
	void theExtensionsOfTheWizardEndUpInTheCertificate() throws Exception
	{
		CertificateInfoModel model = newModel("RSA").extensions(new ExtensionInfoModel[] {
				ExtensionInfoModel.builder()
					.extensionId(CertificateExtensionValues.BASIC_CONSTRAINTS).critical(true)
					.value("CA:true,pathlen:1").build(),
				ExtensionInfoModel.builder().extensionId(CertificateExtensionValues.KEY_USAGE)
					.critical(true).value("digitalSignature,keyCertSign,cRLSign").build(),
				ExtensionInfoModel.builder()
					.extensionId(CertificateExtensionValues.SUBJECT_ALTERNATIVE_NAME)
					.critical(false).value("DNS:example.com,IP:10.0.0.1").build() })
			.build();

		X509Certificate certificate = CertificateInfoModelToX509.toX509Certificate(model);

		assertEquals(1, certificate.getBasicConstraints(),
			"the path length from the wizard must be in the certificate");
		boolean[] keyUsage = certificate.getKeyUsage();
		assertNotNull(keyUsage, "the key usage from the wizard must be in the certificate");
		assertTrue(keyUsage[0], "digitalSignature");
		assertTrue(keyUsage[5], "keyCertSign");
		assertTrue(keyUsage[6], "cRLSign");
		List<?> alternativeNames = List.copyOf(certificate.getSubjectAlternativeNames());
		assertEquals(2, alternativeNames.size(),
			"both alternative names must be in the certificate");
		assertTrue(
			certificate.getCriticalExtensionOIDs()
				.contains(CertificateExtensionValues.BASIC_CONSTRAINTS),
			"an extension marked critical must be critical in the certificate");
		assertFalse(certificate.getCriticalExtensionOIDs()
			.contains(CertificateExtensionValues.SUBJECT_ALTERNATIVE_NAME));
	}

	@Test
	void aCertificateWithoutExtensionsStillWorks() throws Exception
	{
		X509Certificate certificate = CertificateInfoModelToX509
			.toX509Certificate(newModel("RSA").build());

		assertEquals(-1, certificate.getBasicConstraints(), "no extensions means no constraints");
		certificate.verify(certificate.getPublicKey());
	}

	@Test
	void anExtensionThatCannotBeBuiltStopsTheCertificate() throws Exception
	{
		CertificateInfoModel model = newModel("RSA").extensions(new ExtensionInfoModel[] {
				ExtensionInfoModel.builder().extensionId(CertificateExtensionValues.KEY_USAGE)
					.critical(true).value("nonsense").build() })
			.build();

		assertThrows(IllegalArgumentException.class,
			() -> CertificateInfoModelToX509.toX509Certificate(model),
			"a certificate with a broken extension must not be written at all");
	}

	@ParameterizedTest
	@CsvSource({ "RSA,SHA256withRSA", "EC,SHA256withECDSA", "DSA,SHA256withDSA",
			"RSASSA-PSS,SHA256withRSAandMGF1", "Ed25519,Ed25519" })
	void picksASignatureAlgorithmThatTheKeyCanActuallyProduce(String keyAlgorithm, String expected)
	{
		assertEquals(expected,
			CertificateInfoModelToX509.defaultSignatureAlgorithmFor(keyAlgorithm));
	}

	@ParameterizedTest
	@CsvSource({ "SHA256withRSA,RSA,true", "SHA512withRSA,RSA,true", "SHA256withECDSA,RSA,false",
			"SHA256withECDSA,EC,true", "SHA256withRSA,EC,false", "SHA256withDSA,DSA,true",
			"SHA256withECDSA,DSA,false", "ML-DSA-65,ML-DSA-65,true" })
	void knowsWhichSignatureAlgorithmFitsWhichKey(String signatureAlgorithm, String keyAlgorithm,
		boolean fits)
	{
		assertEquals(fits,
			CertificateInfoModelToX509.signatureAlgorithmFits(signatureAlgorithm, keyAlgorithm));
	}

	@Test
	void aSignatureAlgorithmTheKeyCannotProduceIsRefusedWithAReadableMessage() throws Exception
	{
		CertificateInfoModel model = newModel("RSA").signatureAlgorithm("SHA256withECDSA").build();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> CertificateInfoModelToX509.toX509Certificate(model));

		assertTrue(exception.getMessage().contains("SHA256withECDSA"), exception.getMessage());
		assertTrue(exception.getMessage().contains("SHA256withRSA"),
			"the message must say what would work: " + exception.getMessage());
	}

	@Test
	void anEcKeyIsSignedTheEcWay() throws Exception
	{
		X509Certificate certificate = CertificateInfoModelToX509
			.toX509Certificate(newModel("EC").build());

		assertTrue(certificate.getSigAlgName().toUpperCase().contains("ECDSA"),
			"was: " + certificate.getSigAlgName());
		certificate.verify(certificate.getPublicKey());
	}
}
