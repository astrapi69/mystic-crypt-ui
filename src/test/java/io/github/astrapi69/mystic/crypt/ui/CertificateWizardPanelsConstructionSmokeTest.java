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
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.KeyPair;
import java.time.ZonedDateTime;

import javax.swing.JPanel;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.KeyInfoExtensions;
import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.wizard.CertificateWizardContentPanel;
import io.github.astrapi69.mystic.crypt.wizard.CertificateWizardPanel;
import io.github.astrapi69.mystic.crypt.wizard.DatesPanel;
import io.github.astrapi69.mystic.crypt.wizard.ExtensionsPanel;
import io.github.astrapi69.mystic.crypt.wizard.IssuerPanel;
import io.github.astrapi69.mystic.crypt.wizard.SubjectPanel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.KeyInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.ValidityModel;
import io.github.astrapi69.mystic.crypt.wizard.state.CertificateWizardState;

/**
 * Construction smoke tests for the (now live) certificate-wizard panels: each must build with a
 * fully populated model without throwing
 */
class CertificateWizardPanelsConstructionSmokeTest
{

	private static CertificateInfoModel newModel() throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		ZonedDateTime now = ZonedDateTime.now();
		return CertificateInfoModel.builder()
			.privateKeyInfo(
				KeyInfoModel.toKeyInfoModel(KeyInfoExtensions.toKeyInfo(keyPair.getPrivate())))
			.publicKeyInfo(
				KeyInfoModel.toKeyInfoModel(KeyInfoExtensions.toKeyInfo(keyPair.getPublic())))
			.issuer(DistinguishedNameInfoModel.builder().commonName("issuer").build())
			.subject(DistinguishedNameInfoModel.builder().commonName("subject").build())
			.validityModel(
				ValidityModel.builder().notBefore(now).notAfter(now.plusYears(1)).build())
			.serial(BigInteger.ONE).signatureAlgorithm("SHA256withRSA").build();
	}

	private static IModel<BaseWizardStateMachineModel<CertificateInfoModel>> stateMachineModel()
		throws Exception
	{
		return BaseModel.of(BaseWizardStateMachineModel.<CertificateInfoModel> builder()
			.currentState(CertificateWizardState.ISSUER).modelObject(newModel()).build());
	}

	private static void assertConstructs(GuiActionRunnerFactory factory)
	{
		JPanel panel = GuiActionRunner.execute(factory::create);
		assertNotNull(panel, "the panel must be constructed");
		assertTrue(panel.getComponentCount() > 0, "the panel must lay out its components");
	}

	@FunctionalInterface
	private interface GuiActionRunnerFactory
	{
		JPanel create() throws Exception;
	}

	@Test
	void certificateWizardPanelConstructs()
	{
		assertConstructs(() -> new CertificateWizardPanel(BaseModel.of(newModel())));
	}

	@Test
	void certificateWizardContentPanelConstructs()
	{
		assertConstructs(() -> new CertificateWizardContentPanel(stateMachineModel()));
	}

	@Test
	void issuerPanelConstructs()
	{
		assertConstructs(() -> new IssuerPanel(stateMachineModel()));
	}

	@Test
	void subjectPanelConstructs()
	{
		assertConstructs(() -> new SubjectPanel(stateMachineModel()));
	}

	@Test
	void datesPanelConstructs()
	{
		assertConstructs(() -> new DatesPanel(stateMachineModel()));
	}

	@Test
	void extensionsPanelConstructs()
	{
		assertConstructs(() -> new ExtensionsPanel(stateMachineModel()));
	}
}
