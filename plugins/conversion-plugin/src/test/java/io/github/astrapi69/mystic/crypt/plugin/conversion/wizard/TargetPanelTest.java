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
package io.github.astrapi69.mystic.crypt.plugin.conversion.wizard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.security.Security;

import javax.swing.JComponent;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.bouncycastle.asn1.x500.X500Name;

import io.github.astrapi69.crypt.data.factory.CertFactory;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.CertificateWriter;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.plugin.conversion.ConversionSupport;

/**
 * Tests of the wizard's Target step: only the conversions valid for what the Source step detected
 * are enabled, choosing one writes it into the wizard model and pre-fills a default target file
 * name, and a target the user already typed is never clobbered by that default.
 */
class TargetPanelTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private static TargetPanel newPanel(ConversionWizardModel model)
	{
		BaseWizardStateMachineModel<ConversionWizardModel> stateMachine = BaseWizardStateMachineModel
			.<ConversionWizardModel> builder().currentState(ConversionWizardState.TARGET)
			.modelObject(model).build();
		return new TargetPanel(BaseModel.of(stateMachine));
	}

	private File writePrivateKeyPem(File directory) throws Exception
	{
		File file = new File(directory, "key.pem");
		PrivateKeyWriter.writeInPemFormat(KeyPairFactory.newKeyPair("RSA").getPrivate(), file);
		return file;
	}

	private File writeCertificatePem(File directory) throws Exception
	{
		java.security.KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		X500Name name = new X500Name("CN=target panel test");
		java.security.cert.X509Certificate certificate = CertFactory.newX509CertificateV3(keyPair,
			name, java.math.BigInteger.ONE, new java.util.Date(System.currentTimeMillis() - 1000),
			new java.util.Date(System.currentTimeMillis() + 86_400_000L), name, "SHA256withRSA");
		File file = new File(directory, "certificate.pem");
		CertificateWriter.writeInPemFormat(certificate, file);
		return file;
	}

	@Test
	void onlyTheConversionsValidForThePrivateKeyAreEnabled(@TempDir File directory) throws Exception
	{
		File source = writePrivateKeyPem(directory);
		ConversionSupport.FileKind kind = ConversionSupport.kindOf(source);
		ConversionWizardModel model = new ConversionWizardModel();
		model.setSourceFilePath(source.getAbsolutePath());
		TargetPanel panel = newPanel(model);

		panel.refresh(kind, source);

		assertTrue(radio(panel, "rdoPemToDer").isEnabled(), "a pem file can be converted to der");
		assertFalse(radio(panel, "rdoDerToPem").isEnabled(), "a pem file is already pem");
		assertTrue(radio(panel, "rdoToPkcs8").isEnabled(), "a private key can become PKCS#8");
		assertTrue(radio(panel, "rdoToPkcs1").isEnabled(), "a private key can become PKCS#1");
	}

	@Test
	void selectingAConversionWritesItIntoTheModelAndFillsADefaultTarget(@TempDir File directory)
		throws Exception
	{
		File source = writePrivateKeyPem(directory);
		ConversionSupport.FileKind kind = ConversionSupport.kindOf(source);
		ConversionWizardModel model = new ConversionWizardModel();
		model.setSourceFilePath(source.getAbsolutePath());
		TargetPanel panel = newPanel(model);
		panel.refresh(kind, source);

		radio(panel, "rdoPemToDer").doClick();

		assertEquals(ConversionOperation.PEM_TO_DER, model.getOperation(),
			"the chosen conversion has to be in the model at once");
		assertEquals(ConversionOperation.PEM_TO_DER.defaultTargetFile(source).getAbsolutePath(),
			targetField(panel).getText(), "an unfilled target field gets the operation's default");
		assertEquals(targetField(panel).getText(), model.getTargetFilePath());
	}

	@Test
	void aTargetTheUserAlreadyTypedIsNeverClobbered(@TempDir File directory) throws Exception
	{
		File source = writePrivateKeyPem(directory);
		ConversionSupport.FileKind kind = ConversionSupport.kindOf(source);
		ConversionWizardModel model = new ConversionWizardModel();
		model.setSourceFilePath(source.getAbsolutePath());
		TargetPanel panel = newPanel(model);
		panel.refresh(kind, source);
		String customTarget = new File(directory, "custom.der").getAbsolutePath();
		targetField(panel).setText(customTarget);

		radio(panel, "rdoPemToDer").doClick();

		assertEquals(customTarget, targetField(panel).getText(),
			"a target the user already typed must not be overwritten by the default");
	}

	@Test
	void aSelectionThatBecomesInvalidOnRefreshIsCleared(@TempDir File directory) throws Exception
	{
		File source = writePrivateKeyPem(directory);
		ConversionSupport.FileKind kind = ConversionSupport.kindOf(source);
		ConversionWizardModel model = new ConversionWizardModel();
		model.setSourceFilePath(source.getAbsolutePath());
		TargetPanel panel = newPanel(model);
		panel.refresh(kind, source);
		radio(panel, "rdoToPkcs8").doClick();

		// the user went back to Source and picked a certificate instead - TO_PKCS8 no longer makes
		// sense once refreshed for a file that holds no private key
		File certificateFile = writeCertificatePem(directory);
		ConversionSupport.FileKind certificateKind = ConversionSupport.kindOf(certificateFile);
		panel.refresh(certificateKind, certificateFile);

		assertFalse(radio(panel, "rdoToPkcs8").isSelected(),
			"a selection that is no longer valid must be cleared");
		assertNull(model.getOperation(), "the model must not still name an invalid conversion");
	}

	@Test
	void everyFieldExplainsItselfWithATooltip()
	{
		TargetPanel panel = newPanel(new ConversionWizardModel());

		assertHasTooltip((JComponent)componentNamed(panel, "txtTargetFile"), "target file");
		assertHasTooltip((JComponent)componentNamed(panel, "btnBrowseTarget"), "browse target button");
		for (ConversionOperation operation : ConversionOperation.values())
		{
			assertHasTooltip(radio(panel, "rdo" + radioSuffix(operation)), operation + " radio button");
		}
	}

	private static String radioSuffix(ConversionOperation operation)
	{
		return switch (operation)
		{
			case PEM_TO_DER -> "PemToDer";
			case DER_TO_PEM -> "DerToPem";
			case TO_PKCS8 -> "ToPkcs8";
			case TO_PKCS1 -> "ToPkcs1";
		};
	}

	private static void assertHasTooltip(JComponent component, String fieldName)
	{
		String tooltip = component.getToolTipText();
		assertTrue(tooltip != null && !tooltip.isBlank(), fieldName + " must have a tooltip");
	}

	private JRadioButton radio(Container container, String name)
	{
		return (JRadioButton)componentNamed(container, name);
	}

	private JTextField targetField(Container container)
	{
		return (JTextField)componentNamed(container, "txtTargetFile");
	}

	private static Component componentNamed(Container container, String name)
	{
		for (Component child : container.getComponents())
		{
			if (name.equals(child.getName()))
			{
				return child;
			}
			if (child instanceof Container nested)
			{
				Component found = componentNamed(nested, name);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}
}
