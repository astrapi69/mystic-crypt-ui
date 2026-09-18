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
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;

import javax.swing.JPanel;

import org.assertj.swing.edt.GuiActionRunner;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.CertificateExtensions;
import io.github.astrapi69.crypt.data.key.KeyInfoExtensions;
import io.github.astrapi69.crypt.data.model.X509CertificateV3Info;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.panel.certificate.CertificatePanel;
import io.github.astrapi69.mystic.crypt.panel.certificate.NewCertificateAttributesPanel;
import io.github.astrapi69.mystic.crypt.panel.dbtree.AttachmentPanel;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryPanel;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryTabbedPanel;
import io.github.astrapi69.mystic.crypt.panel.privatekey.PrivateKeyPanel;
import io.github.astrapi69.mystic.crypt.panel.privatekey.PrivateKeyViewPanel;
import io.github.astrapi69.mystic.crypt.panel.properties.PropertiesNewEntryPanel;
import io.github.astrapi69.mystic.crypt.panel.properties.PropertiesPanel;
import io.github.astrapi69.mystic.crypt.panel.table.NewTableEntryModel;
import io.github.astrapi69.mystic.crypt.panel.table.NewTableEntryPanel;
import io.github.astrapi69.mystic.crypt.wizard.CertificateInfoModelToX509;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.KeyInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.ValidityModel;
import io.github.astrapi69.swing.model.label.LabelModel;

/**
 * Construction smoke tests: each of these host panels must build (on the EDT, with a valid model)
 * without throwing and lay out at least one child component
 */
class HostPanelConstructionSmokeTest
{

	@BeforeAll
	static void addBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@FunctionalInterface
	private interface PanelFactory
	{
		JPanel create() throws Exception;
	}

	private static void assertConstructs(PanelFactory factory)
	{
		JPanel panel = GuiActionRunner.execute(factory::create);
		assertNotNull(panel, "the panel must be constructed");
		assertTrue(panel.getComponentCount() > 0, "the panel must lay out its components");
	}

	@Test
	void attachmentPanelConstructs()
	{
		assertConstructs(AttachmentPanel::new);
	}

	@Test
	void mysticCryptEntryPanelConstructs()
	{
		assertConstructs(MysticCryptEntryPanel::new);
	}

	@Test
	void mysticCryptEntryTabbedPanelConstructs()
	{
		assertConstructs(MysticCryptEntryTabbedPanel::new);
	}

	@Test
	void newTableEntryPanelConstructs()
	{
		assertConstructs(() -> new NewTableEntryPanel(BaseModel.of(NewTableEntryModel.builder()
			.labelModelName(new LabelModel("Name", null, null)).build())));
	}

	@Test
	void propertiesPanelConstructs()
	{
		assertConstructs(PropertiesPanel::new);
	}

	@Test
	void propertiesNewEntryPanelConstructs()
	{
		assertConstructs(PropertiesNewEntryPanel::new);
	}

	@Test
	void privateKeyPanelConstructs()
	{
		assertConstructs(PrivateKeyPanel::new);
	}

	@Test
	void privateKeyViewPanelConstructs()
	{
		assertConstructs(PrivateKeyViewPanel::new);
	}

	@Test
	void newCertificateAttributesPanelConstructs()
	{
		assertConstructs(NewCertificateAttributesPanel::new);
	}

	@Test
	void certificatePanelConstructs()
	{
		assertConstructs(() -> {
			X509Certificate certificate = CertificateInfoModelToX509
				.toX509Certificate(newCertificateInfoModel());
			X509CertificateV3Info info = CertificateExtensions.toX509CertificateV3Info(certificate);
			return new CertificatePanel(BaseModel.of(info));
		});
	}

	private static CertificateInfoModel newCertificateInfoModel() throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		ZonedDateTime now = ZonedDateTime.now();
		return CertificateInfoModel.builder()
			.privateKeyInfo(
				KeyInfoModel.toKeyInfoModel(KeyInfoExtensions.toKeyInfo(keyPair.getPrivate())))
			.publicKeyInfo(
				KeyInfoModel.toKeyInfoModel(KeyInfoExtensions.toKeyInfo(keyPair.getPublic())))
			.issuer(DistinguishedNameInfoModel.builder().commonName("smoke").build())
			.subject(DistinguishedNameInfoModel.builder().commonName("smoke").build())
			.validityModel(
				ValidityModel.builder().notBefore(now).notAfter(now.plusYears(1)).build())
			.serial(BigInteger.ONE).signatureAlgorithm("SHA256withRSA").build();
	}
}
