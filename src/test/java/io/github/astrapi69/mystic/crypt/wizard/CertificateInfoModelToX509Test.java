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

import java.io.File;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.crypt.data.factory.CertFactory;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.factory.KeyPairGeneratorFactory;
import io.github.astrapi69.crypt.data.key.KeyInfoExtensions;
import io.github.astrapi69.crypt.data.key.reader.CertificateReader;
import io.github.astrapi69.crypt.data.key.writer.CertificateWriter;
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
		return newModel(newKeyPairFor(keyAlgorithm));
	}

	/**
	 * RSA and DSA need an explicit classical bit size; EC, Ed25519 and every other fixed-parameter
	 * algorithm have none and crash if forced through {@link KeyPairFactory#newKeyPair(String)}'s
	 * size-defaulting convenience method (a default of 2048 makes no sense for a curve or a
	 * fixed-parameter algorithm -
	 * {@code java.security.InvalidParameterException: Unsupported size: 2048} for Ed25519, #141)
	 */
	private static KeyPair newKeyPairFor(String keyAlgorithm) throws Exception
	{
		if ("RSA".equalsIgnoreCase(keyAlgorithm) || "DSA".equalsIgnoreCase(keyAlgorithm))
		{
			return KeyPairFactory.newKeyPair(keyAlgorithm);
		}
		return KeyPairGeneratorFactory.newKeyPairGenerator(keyAlgorithm).generateKeyPair();
	}

	private CertificateInfoModel.CertificateInfoModelBuilder<?, ?> newModel(KeyPair keyPair)
	{
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
	@ValueSource(strings = { "Ed25519", "Ed448" })
	@DisplayName("a real EdDSA key's own curve decides its default signature algorithm, not the generic family name it reports")
	void aRealEdDsaKeyPicksItsOwnCurveAsSignatureAlgorithm(String curve) throws Exception
	{
		PrivateKey privateKey = KeyPairGeneratorFactory.newKeyPairGenerator(curve).generateKeyPair()
			.getPrivate();
		// what a real Ed25519/Ed448 key actually reports - the generic family name, not the
		// specific curve KeyInfoExtensions.toPrivateKey() lost no more than the JDK itself does
		assertEquals("EdDSA", privateKey.getAlgorithm(),
			"the premise this test builds on: getAlgorithm() really is generic (#142)");

		assertEquals(curve, CertificateInfoModelToX509.defaultSignatureAlgorithmFor(privateKey),
			"the key's own EdECKey params must recover the exact curve BouncyCastle needs, "
				+ "since it refuses the bare 'EdDSA' as a signature algorithm name");
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

	@ParameterizedTest
	@ValueSource(strings = { "RSA", "DSA", "EC", "Ed25519" })
	@DisplayName("every key algorithm the app offers round-trips through certificate generation")
	void everyOfferedKeyAlgorithmProducesAWorkingCertificate(String keyAlgorithm) throws Exception
	{
		X509Certificate certificate = CertificateInfoModelToX509
			.toX509Certificate(newModel(keyAlgorithm).build());

		assertEquals(keyAlgorithm.toUpperCase(Locale.ROOT),
			certificate.getPublicKey().getAlgorithm().toUpperCase(Locale.ROOT),
			"the certificate's public key must be of the algorithm that was asked for");
		certificate.verify(certificate.getPublicKey());
	}

	@ParameterizedTest
	@ValueSource(ints = { 2048, 4096 })
	@DisplayName("an RSA certificate's modulus reflects the chosen key size, not a fixed default")
	void anRsaCertificatesModulusReflectsTheChosenKeySize(int keySize) throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA", keySize);
		X509Certificate certificate = CertificateInfoModelToX509
			.toX509Certificate(newModel(keyPair).build());

		assertEquals(keySize, ((RSAPublicKey)certificate.getPublicKey()).getModulus().bitLength(),
			"the certificate must carry the key at the size it was generated with, not some "
				+ "other default");
	}

	@Test
	@DisplayName("a CA-signed certificate verifies against the CA's key, not the subject's own")
	void aCaSignedCertificateVerifiesAgainstTheCaNotTheSubject() throws Exception
	{
		KeyPair caKeyPair = KeyPairFactory.newKeyPair("RSA");
		KeyPair subjectKeyPair = KeyPairFactory.newKeyPair("RSA");
		X500Name issuer = DistinguishedNameInfoModel.toDistinguishedNameInfo(
			DistinguishedNameInfoModel.builder().commonName("real ca").build()).toX500Name();
		X500Name subject = DistinguishedNameInfoModel
			.toDistinguishedNameInfo(
				DistinguishedNameInfoModel.builder().commonName("leaf certificate").build())
			.toX500Name();
		ZonedDateTime now = ZonedDateTime.now();

		// every other test in this class signs with the subject's own key (what the wizard's
		// model supports today) - this drives CertFactory directly with two separate key pairs to
		// prove a genuinely CA-signed (non-self-signed) chain actually verifies the way one has to
		PrivateKey caPrivateKey = caKeyPair.getPrivate();
		PublicKey subjectPublicKey = subjectKeyPair.getPublic();
		X509Certificate certificate = CertFactory.newX509CertificateV3(caPrivateKey,
			subjectPublicKey, issuer, BigInteger.valueOf(now.toInstant().toEpochMilli()),
			Date.from(now.toInstant()), Date.from(now.plusYears(1).toInstant()), subject,
			"SHA256withRSA");

		certificate.verify(caKeyPair.getPublic());
		assertThrows(Exception.class, () -> certificate.verify(subjectPublicKey),
			"a certificate signed by the CA must not also verify against the subject's own key - "
				+ "that would mean it was self-signed after all");
	}

	@ParameterizedTest
	@CsvSource({ "10, false", "-1, true" })
	@DisplayName("the certificate's validity window matches what was asked for, including edge cases")
	void theCertificatesValidityWindowMatchesWhatWasAsked(int yearsFromNow, boolean expectExpired)
		throws Exception
	{
		// truncated to whole seconds: X.509's UTCTime/GeneralizedTime encoding has no sub-second
		// precision, so comparing a certificate's dates against a millisecond-precise
		// ZonedDateTime.now() would fail on the truncation alone, not on anything this test means
		// to check
		ZonedDateTime notBefore = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS)
			.minusYears(expectExpired ? 2 : 0);
		ZonedDateTime notAfter = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS)
			.plusYears(yearsFromNow);
		CertificateInfoModel model = newModel("RSA")
			.validityModel(ValidityModel.builder().notBefore(notBefore).notAfter(notAfter).build())
			.build();

		X509Certificate certificate = CertificateInfoModelToX509.toX509Certificate(model);

		assertEquals(Date.from(notBefore.toInstant()), certificate.getNotBefore(),
			"the start of the validity window must be exactly what was asked for");
		assertEquals(Date.from(notAfter.toInstant()), certificate.getNotAfter(),
			"the end of the validity window must be exactly what was asked for, however far in "
				+ "the past or future");
		assertEquals(expectExpired, certificate.getNotAfter().before(new Date()),
			"an already-expired validity window has to actually produce an expired certificate");
	}

	@Test
	@DisplayName("a TLS-server extension profile is flagged as an end-entity certificate, not a CA")
	void aTlsServerProfileIsFlaggedAsAnEndEntityCertificate() throws Exception
	{
		CertificateInfoModel model = newModel("RSA").extensions(new ExtensionInfoModel[] {
				ExtensionInfoModel.builder()
					.extensionId(CertificateExtensionValues.BASIC_CONSTRAINTS).critical(true)
					.value("CA:false").build(),
				ExtensionInfoModel.builder().extensionId(CertificateExtensionValues.KEY_USAGE)
					.critical(true).value("digitalSignature,keyEncipherment").build(),
				ExtensionInfoModel.builder()
					.extensionId(CertificateExtensionValues.SUBJECT_ALTERNATIVE_NAME)
					.critical(false).value("DNS:example.com").build() })
			.build();

		X509Certificate certificate = CertificateInfoModelToX509.toX509Certificate(model);

		assertEquals(-1, certificate.getBasicConstraints(),
			"CA:false must not report a path length - -1 is 'not a CA' the way X509Certificate "
				+ "reports it");
		boolean[] keyUsage = certificate.getKeyUsage();
		assertTrue(keyUsage[0], "digitalSignature");
		assertTrue(keyUsage[2], "keyEncipherment");
		assertFalse(keyUsage[5], "keyCertSign must not be set on an end-entity certificate");
	}

	@Test
	@DisplayName("a CA extension profile is flagged as able to sign other certificates")
	void aCaProfileIsFlaggedAsAbleToSignOtherCertificates() throws Exception
	{
		CertificateInfoModel model = newModel("RSA").extensions(new ExtensionInfoModel[] {
				ExtensionInfoModel.builder()
					.extensionId(CertificateExtensionValues.BASIC_CONSTRAINTS).critical(true)
					.value("CA:true,pathlen:0").build(),
				ExtensionInfoModel.builder().extensionId(CertificateExtensionValues.KEY_USAGE)
					.critical(true).value("keyCertSign,cRLSign").build() })
			.build();

		X509Certificate certificate = CertificateInfoModelToX509.toX509Certificate(model);

		assertEquals(0, certificate.getBasicConstraints(),
			"CA:true,pathlen:0 must report a path length of 0, not 'not a CA'");
		boolean[] keyUsage = certificate.getKeyUsage();
		assertTrue(keyUsage[5], "keyCertSign");
		assertTrue(keyUsage[6], "cRLSign");
		assertFalse(keyUsage[0], "digitalSignature must not be set on a CA-only certificate");
	}

	@Test
	@DisplayName("a certificate written to PEM and read back is byte-identical to the original")
	void aWrittenCertificateReadsBackIdentical(@TempDir File directory) throws Exception
	{
		X509Certificate certificate = CertificateInfoModelToX509
			.toX509Certificate(newModel("RSA").build());
		File file = new File(directory, "round-trip.crt");

		CertificateWriter.writeInPemFormat(certificate, file);
		X509Certificate readBack = CertificateReader.readPemCertificate(file);

		assertEquals(certificate, readBack,
			"a certificate read back from what was just written must equal the original");
		assertTrue(Arrays.equals(certificate.getEncoded(), readBack.getEncoded()),
			"and their encoded form must be byte-identical, not merely logically equal");
	}
}
