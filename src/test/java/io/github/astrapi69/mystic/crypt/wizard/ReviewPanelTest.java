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

import java.io.File;
import java.math.BigInteger;
import java.security.KeyPair;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.KeyInfoExtensions;
import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.KeyInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.ValidityModel;
import io.github.astrapi69.mystic.crypt.wizard.state.CertificateWizardState;

/**
 * Tests of the wizard's last step: it shows whatever preview it was given and fills in the file
 * name and save directory it was given as defaults - but only while the user has not typed
 * something of their own there, since a later refresh must not silently overwrite an edit
 */
class ReviewPanelTest
{

	private static ReviewPanel newPanel() throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		ZonedDateTime now = ZonedDateTime.now();
		CertificateInfoModel certificateInfoModel = CertificateInfoModel.builder()
			.privateKeyInfo(
				KeyInfoModel.toKeyInfoModel(KeyInfoExtensions.toKeyInfo(keyPair.getPrivate())))
			.publicKeyInfo(
				KeyInfoModel.toKeyInfoModel(KeyInfoExtensions.toKeyInfo(keyPair.getPublic())))
			.issuer(DistinguishedNameInfoModel.builder().commonName("issuer").build())
			.subject(DistinguishedNameInfoModel.builder().commonName("subject").build())
			.validityModel(
				ValidityModel.builder().notBefore(now).notAfter(now.plusYears(1)).build())
			.serial(BigInteger.ONE).build();
		return new ReviewPanel(BaseModel.of(BaseWizardStateMachineModel
			.<CertificateInfoModel> builder().currentState(CertificateWizardState.REVIEW)
			.modelObject(certificateInfoModel).build()));
	}

	@Test
	void refreshShowsThePreviewItWasGiven() throws Exception
	{
		ReviewPanel panel = newPanel();

		panel.refresh("-----BEGIN CERTIFICATE-----", "subject.crt", new File("/tmp"));

		assertEquals("-----BEGIN CERTIFICATE-----", panel.getTxtPreview().getText());
	}

	@Test
	void refreshFillsInTheDefaultsWhenNothingWasTypedYet() throws Exception
	{
		ReviewPanel panel = newPanel();

		panel.refresh("preview", "subject.crt", new File("/tmp"));

		assertEquals("subject.crt", panel.getReviewFormModel().getFileName());
		assertEquals(new File("/tmp"), panel.getReviewFormModel().getSaveDirectory());
	}

	@Test
	void refreshDoesNotOverwriteAFileNameTheUserAlreadyTyped() throws Exception
	{
		ReviewPanel panel = newPanel();
		panel.getTxtFileName().setText("my-own-name.crt");

		panel.refresh("preview", "subject.crt", new File("/tmp"));

		assertEquals("my-own-name.crt", panel.getReviewFormModel().getFileName());
	}

	@Test
	void refreshDoesNotOverwriteADirectoryTheUserAlreadyPicked() throws Exception
	{
		ReviewPanel panel = newPanel();
		panel.getReviewFormModel().setSaveDirectory(new File("/opt/somewhere"));

		panel.refresh("preview", "subject.crt", new File("/tmp"));

		assertEquals(new File("/opt/somewhere"), panel.getReviewFormModel().getSaveDirectory());
	}

	@Test
	void refreshingTwiceWithADifferentPreviewReplacesIt() throws Exception
	{
		ReviewPanel panel = newPanel();
		panel.refresh("first version", "subject.crt", new File("/tmp"));

		panel.refresh("second version", "subject.crt", new File("/tmp"));

		assertEquals("second version", panel.getTxtPreview().getText());
	}

	private static void assertHasTooltip(javax.swing.JComponent component, String fieldName)
	{
		String tooltip = component.getToolTipText();
		assertNotNull(tooltip, fieldName + " must have a tooltip");
		assertFalse(tooltip.isBlank(), fieldName + " must have a tooltip");
	}

	@Test
	void everyFieldExplainsItselfWithATooltip() throws Exception
	{
		ReviewPanel panel = newPanel();

		assertHasTooltip(panel.getTxtPreview(), "preview");
		assertHasTooltip(panel.getTxtFileName(), "file name");
		assertHasTooltip(panel.getTxtSaveDirectory(), "save directory");
		assertHasTooltip(panel.getBtnBrowseDirectory(), "browse");
	}
}
