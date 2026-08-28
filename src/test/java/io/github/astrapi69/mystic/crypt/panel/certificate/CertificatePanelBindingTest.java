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

import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.data.model.DistinguishedNameInfo;
import io.github.astrapi69.crypt.data.model.Validity;
import io.github.astrapi69.crypt.data.model.X509CertificateV1Info;
import io.github.astrapi69.crypt.data.model.X509CertificateV3Info;
import io.github.astrapi69.model.BaseModel;

/**
 * Tests that the components of {@link CertificatePanel} are bound to {@link CertificatePanelModel}:
 * what the panel shows about a certificate is readable from that one object instead of having to be
 * collected from nine widgets.
 */
class CertificatePanelBindingTest
{

	/**
	 * A panel over a certificate the way the application hands one to it. The certificate info the
	 * panel is constructed with is immutable and demands every one of its parts, so it is built
	 * here rather than taken from the no argument constructor.
	 *
	 * @return the panel
	 */
	private static CertificatePanel newPanel()
	{
		ZonedDateTime notBefore = ZonedDateTime.parse("2026-01-01T00:00:00+01:00");
		DistinguishedNameInfo distinguishedNameInfo = DistinguishedNameInfo.builder()
			.commonName("bound certificate").build();
		X509CertificateV1Info certificateV1Info = X509CertificateV1Info.builder()
			.issuer(distinguishedNameInfo).subject(distinguishedNameInfo).serial(BigInteger.ONE)
			.signatureAlgorithm("SHA256withRSA")
			.validity(
				Validity.builder().notBefore(notBefore).notAfter(notBefore.plusYears(1)).build())
			.build();
		return new CertificatePanel(BaseModel
			.of(X509CertificateV3Info.builder().certificateV1Info(certificateV1Info).build()));
	}

	private static List<Object> itemsOf(JComboBox<?> comboBox)
	{
		List<Object> items = new ArrayList<>();
		for (int index = 0; index < comboBox.getItemCount(); index++)
		{
			items.add(comboBox.getItemAt(index));
		}
		return items;
	}

	/**
	 * Everything the panel shows lands in the model, so the certificate on screen can be read back
	 * from one object
	 */
	@Test
	void everythingTheComponentsShowIsReadableFromTheModel()
	{
		CertificatePanel panel = newPanel();

		panel.getTxtIssuedTo().setText("CN=bound subject");
		panel.getTxtIssuedBy().setText("CN=bound issuer");
		panel.getTxtVersion().setText("3");
		panel.getTxtSerialNumber().setText("4711");
		panel.getTxtValidFrom().setText("2026-01-01");
		panel.getTxtValidUntil().setText("2027-01-01");
		panel.getTxtSignatureAlgorithm().setText("SHA256withRSA");
		panel.getTxtFingerprint().setText("ab:cd:ef");
		panel.getTxtPublicKey().setText("-----BEGIN PUBLIC KEY-----");

		CertificatePanelModel certificateModel = panel.getCertificateModel();
		assertEquals("CN=bound subject", certificateModel.getIssuedTo());
		assertEquals("CN=bound issuer", certificateModel.getIssuedBy());
		assertEquals("3", certificateModel.getVersion());
		assertEquals("4711", certificateModel.getSerialNumber());
		assertEquals("2026-01-01", certificateModel.getValidFrom());
		assertEquals("2027-01-01", certificateModel.getValidUntil());
		assertEquals("SHA256withRSA", certificateModel.getSignatureAlgorithm());
		assertEquals("ab:cd:ef", certificateModel.getFingerprint());
		assertEquals("-----BEGIN PUBLIC KEY-----", certificateModel.getPublicKey());
	}

	/**
	 * A cleared field clears the model with it, so the model cannot keep showing a value the panel
	 * no longer shows
	 */
	@Test
	void aClearedFieldClearsTheValueInTheModel()
	{
		CertificatePanel panel = newPanel();
		panel.getTxtFingerprint().setText("ab:cd:ef");

		panel.getTxtFingerprint().setText("");

		assertEquals("", panel.getCertificateModel().getFingerprint());
	}

	/**
	 * The chosen fingerprint algorithm is the one the model holds
	 */
	@Test
	void theChosenFingerprintAlgorithmIsTheOneTheModelHolds()
	{
		CertificatePanel panel = newPanel();

		panel.getCmbFingerprintAlgorithm().setSelectedItem("Item 3");

		assertEquals("Item 3", panel.getCertificateModel().getFingerprintAlgorithm());
	}

	/**
	 * The panel starts on what the model holds: the fingerprint algorithm the combo box offers
	 * first, and no certificate values, because nothing filled them in yet
	 */
	@Test
	void thePanelStartsOnWhatTheModelHolds()
	{
		CertificatePanel panel = newPanel();

		assertEquals(List.of("Item 1", "Item 2", "Item 3", "Item 4"),
			itemsOf(panel.getCmbFingerprintAlgorithm()));
		assertEquals("Item 1", panel.getCmbFingerprintAlgorithm().getSelectedItem());
		assertEquals("Item 1", panel.getCertificateModel().getFingerprintAlgorithm());
		assertEquals("", panel.getCertificateModel().getIssuedTo());
		assertEquals("", panel.getTxtIssuedTo().getText());
	}
}
