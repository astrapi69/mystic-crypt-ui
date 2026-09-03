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
package io.github.astrapi69.mystic.crypt.panel.certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.ValidityModel;

/**
 * Tests that the components of {@link NewCertificateInfoPanel} are bound to the
 * {@link CertificateInfoModel} the panel was constructed with: what is typed or chosen is what the
 * model carries afterwards, and what the buttons write is readable there and not only in a widget.
 * <p>
 * This is the same model the certificate wizard fills on its own steps, so a value that does not
 * arrive here does not reach the certificate either.
 */
class NewCertificateInfoPanelBindingTest
{

	/** The moment the certificates in this test start being valid at */
	private static final ZonedDateTime NOT_BEFORE = ZonedDateTime
		.parse("2026-01-01T00:00:00+01:00");

	/** The moment the certificates in this test stop being valid at */
	private static final ZonedDateTime NOT_AFTER = ZonedDateTime.parse("2027-01-01T00:00:00+01:00");

	private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

	/**
	 * A panel over a certificate info model that carries a validity period, a serial number and a
	 * signature algorithm - the state a panel is opened on when a key pair was generated before
	 *
	 * @return the panel
	 */
	private static NewCertificateInfoPanel newPanel()
	{
		return new NewCertificateInfoPanel(BaseModel.of(CertificateInfoModel.builder()
			.serial(BigInteger.ONE).signatureAlgorithm(SIGNATURE_ALGORITHM)
			.validityModel(
				ValidityModel.builder().notBefore(NOT_BEFORE).notAfter(NOT_AFTER).build())
			.build()));
	}

	/**
	 * The components start on what the model already holds, so nothing is shown that the model does
	 * not carry
	 */
	@Test
	void thePanelStartsOnTheValuesTheModelAlreadyHolds()
	{
		NewCertificateInfoPanel panel = newPanel();

		assertEquals("1", panel.getTxtSerialNumber().getText());
		assertEquals(SIGNATURE_ALGORITHM, panel.getTxtSignatureAlgorithm().getText());
		assertEquals(NOT_BEFORE.toString(), panel.getTxtNotBefore().getText());
		assertEquals(NOT_AFTER.toString(), panel.getTxtNotAfter().getText());
		assertEquals(3, panel.getCmbVersion().getSelectedItem());
	}

	/**
	 * Generating a serial number puts it into the model, not only into the field - the model is
	 * where the certificate is built from
	 */
	@Test
	void generateSerialNumberPutsTheGeneratedSerialIntoTheModel()
	{
		NewCertificateInfoPanel panel = newPanel();

		panel.getBtnGenerateSerialNumber().doClick();

		BigInteger generated = panel.getModelObject().getSerial();
		assertNotNull(generated, "the generated serial number did not reach the model");
		assertNotEquals(BigInteger.ONE, generated, "the serial number was not generated");
		assertEquals(generated.toString(), panel.getTxtSerialNumber().getText(),
			"the field and the model do not show the same serial number");
	}

	/**
	 * A typed serial number is the one the model holds
	 */
	@Test
	void theTypedSerialNumberIsTheOneTheModelHolds()
	{
		NewCertificateInfoPanel panel = newPanel();

		panel.getTxtSerialNumber().setText("4711");

		assertEquals(new BigInteger("4711"), panel.getModelObject().getSerial());
	}

	/**
	 * A typed issuer reaches the model as a distinguished name and not as a piece of text
	 */
	@Test
	void theTypedIssuerReachesTheModelAsADistinguishedName()
	{
		NewCertificateInfoPanel panel = newPanel();

		panel.getTxtIssuer().setText("CN=bound issuer,O=acme,C=DE");

		DistinguishedNameInfoModel issuer = panel.getModelObject().getIssuer();
		assertNotNull(issuer, "the typed issuer did not reach the model");
		assertEquals("bound issuer", issuer.getCommonName());
		assertEquals("acme", issuer.getOrganisation());
		assertEquals("DE", issuer.getCountryCode());
	}

	/**
	 * A typed subject reaches the model as a distinguished name and not as a piece of text
	 */
	@Test
	void theTypedSubjectReachesTheModelAsADistinguishedName()
	{
		NewCertificateInfoPanel panel = newPanel();

		panel.getTxtSubject().setText("CN=bound subject,OU=security");

		DistinguishedNameInfoModel subject = panel.getModelObject().getSubject();
		assertNotNull(subject, "the typed subject did not reach the model");
		assertEquals("bound subject", subject.getCommonName());
		assertEquals("security", subject.getOrganisationUnit());
	}

	/**
	 * A typed signature algorithm is the one the model holds
	 */
	@Test
	void theTypedSignatureAlgorithmIsTheOneTheModelHolds()
	{
		NewCertificateInfoPanel panel = newPanel();

		panel.getTxtSignatureAlgorithm().setText("SHA512withECDSA");

		assertEquals("SHA512withECDSA", panel.getModelObject().getSignatureAlgorithm());
	}

	/**
	 * The chosen version is the one the model holds
	 */
	@Test
	void theChosenVersionIsTheOneTheModelHolds()
	{
		NewCertificateInfoPanel panel = newPanel();

		panel.getCmbVersion().setSelectedItem(1);

		assertEquals(Integer.valueOf(1), panel.getModelObject().getVersion());
	}

	/**
	 * A typed date moves the validity period of the model
	 */
	@Test
	void theTypedDatesMoveTheValidityPeriodOfTheModel()
	{
		NewCertificateInfoPanel panel = newPanel();
		ZonedDateTime movedNotAfter = NOT_AFTER.plusYears(1);

		panel.getTxtNotAfter().setText(movedNotAfter.toString());

		assertEquals(movedNotAfter, panel.getModelObject().getValidityModel().getNotAfter());
	}

	/**
	 * Text that does not describe a moment is an edit in progress and must not lose the period the
	 * certificate already has
	 *
	 * @param halfTyped
	 *            the text in the field while it is being typed
	 */
	@ParameterizedTest(name = "\"{0}\" leaves the validity period as it was")
	@ValueSource(strings = { "", "   ", "2027-01", "not a date" })
	void textThatIsNotAMomentLeavesTheValidityPeriodAsItWas(String halfTyped)
	{
		NewCertificateInfoPanel panel = newPanel();

		panel.getTxtNotAfter().setText(halfTyped);

		assertEquals(NOT_AFTER, panel.getModelObject().getValidityModel().getNotAfter());
	}

	/**
	 * An emptied issuer field says that nothing was entered and must not throw away the issuer the
	 * model holds
	 */
	@Test
	void anEmptiedIssuerFieldLeavesTheIssuerTheModelHolds()
	{
		NewCertificateInfoPanel panel = newPanel();
		panel.getTxtIssuer().setText("CN=bound issuer");

		panel.getTxtIssuer().setText("");

		assertEquals("bound issuer", panel.getModelObject().getIssuer().getCommonName());
	}

	private static void assertHasTooltip(javax.swing.JComponent component, String fieldName)
	{
		String tooltip = component.getToolTipText();
		org.junit.jupiter.api.Assertions.assertTrue(tooltip != null && !tooltip.isBlank(),
			fieldName + " must have a tooltip");
	}

	/**
	 * "Not Before"/"Not After" expect a full ISO {@code ZonedDateTime} string and silently ignore
	 * anything else while it is being typed (#155) - a first-time user has no way to know that
	 * format is expected without a tooltip
	 */
	@Test
	void everyFieldExplainsItselfWithATooltip()
	{
		NewCertificateInfoPanel panel = newPanel();

		assertHasTooltip(panel.getCmbVersion(), "version");
		assertHasTooltip(panel.getTxtSerialNumber(), "serial number");
		assertHasTooltip(panel.getTxtIssuer(), "issuer");
		assertHasTooltip(panel.getTxtSubject(), "subject");
		assertHasTooltip(panel.getTxtNotBefore(), "not before");
		assertHasTooltip(panel.getTxtNotAfter(), "not after");
		assertHasTooltip(panel.getTxtSignatureAlgorithm(), "signature algorithm");
		assertHasTooltip(panel.getTxtPublicKey(), "public key");
		assertHasTooltip(panel.getBtnCreateIssuer(), "create issuer");
		assertHasTooltip(panel.getBtnCreateSubject(), "create subject");
		assertHasTooltip(panel.getBtnGenerateSerialNumber(), "generate serial number");
	}
}
