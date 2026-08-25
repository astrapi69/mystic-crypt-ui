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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;
import java.time.ZonedDateTime;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.KeyInfoExtensions;
import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.KeyInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.ValidityModel;
import io.github.astrapi69.mystic.crypt.wizard.state.CertificateWizardState;

/**
 * Tests of the wizard step that collects the extensions: picking one by name fills in its object id
 * and says what its value looks like, a value that cannot be built is refused with the reason, and
 * only what was accepted reaches the table.
 */
class ExtensionsPanelTest
{

	/** Captures what would have been shown in a dialog, so the test can read it */
	static class TestableExtensionsPanel extends ExtensionsPanel
	{
		private static final long serialVersionUID = 1L;

		String shownError;

		TestableExtensionsPanel(IModel<BaseWizardStateMachineModel<CertificateInfoModel>> model)
		{
			super(model);
		}

		@Override
		protected void showErrorDialog(String title, String message)
		{
			this.shownError = message;
		}
	}

	/** Finds a component by the name the panel gave it, the way the end-to-end tests do */
	@SuppressWarnings("unchecked")
	private static <T extends java.awt.Component> T byName(java.awt.Container container,
		String name)
	{
		for (java.awt.Component child : container.getComponents())
		{
			if (name.equals(child.getName()))
			{
				return (T)child;
			}
			if (child instanceof java.awt.Container childContainer)
			{
				T found = byName(childContainer, name);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private static javax.swing.JComboBox<String> kindChooser(TestableExtensionsPanel panel)
	{
		return byName(panel, "cmbExtensionKind");
	}

	private static javax.swing.JTextField extensionId(TestableExtensionsPanel panel)
	{
		return byName(panel, "txtExtensionId");
	}

	private static javax.swing.JTextField extensionValue(TestableExtensionsPanel panel)
	{
		return byName(panel, "txtExtensionValue");
	}

	private static javax.swing.JLabel valueHint(TestableExtensionsPanel panel)
	{
		return byName(panel, "lblValueHint");
	}

	private static javax.swing.JCheckBox critical(TestableExtensionsPanel panel)
	{
		return byName(panel, "chkCritical");
	}

	private static javax.swing.table.DefaultTableModel rows(TestableExtensionsPanel panel)
	{
		javax.swing.JTable table = byName(panel, "tblExtensions");
		return (javax.swing.table.DefaultTableModel)table.getModel();
	}

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private static TestableExtensionsPanel newPanel() throws Exception
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
		return new TestableExtensionsPanel(BaseModel.of(BaseWizardStateMachineModel
			.<CertificateInfoModel> builder().currentState(CertificateWizardState.EXTENSIONS)
			.modelObject(certificateInfoModel).build()));
	}

	@ParameterizedTest
	@CsvSource({ "Basic Constraints,2.5.29.19,CA:true", "Key Usage,2.5.29.15,digitalSignature",
			"Subject Alternative Name,2.5.29.17,DNS:example.com" })
	void pickingAnExtensionByNameFillsInItsObjectIdAndSaysWhatTheValueLooksLike(String name,
		String expectedId, String hintFragment) throws Exception
	{
		TestableExtensionsPanel panel = newPanel();

		kindChooser(panel).setSelectedItem(name);

		assertEquals(expectedId, extensionId(panel).getText());
		assertTrue(valueHint(panel).getText().contains(hintFragment.split(":")[0]),
			"the hint must show what to type, was: " + valueHint(panel).getText());
	}

	@Test
	void anExtensionThatIsNotOneOfTheKnownOnesAsksForHex() throws Exception
	{
		TestableExtensionsPanel panel = newPanel();

		kindChooser(panel).setSelectedItem(ExtensionsPanel.OTHER_EXTENSION);

		assertTrue(valueHint(panel).getText().contains("hex"), valueHint(panel).getText());
	}

	@Test
	void whatIsAcceptedEndsUpInTheTable() throws Exception
	{
		TestableExtensionsPanel panel = newPanel();
		kindChooser(panel).setSelectedItem("Key Usage");
		extensionValue(panel).setText("digitalSignature,keyCertSign");
		critical(panel).setSelected(true);

		panel.onAddExtension();

		assertEquals(1, rows(panel).getRowCount());
		assertEquals("2.5.29.15", rows(panel).getValueAt(0, 0));
		assertEquals(true, rows(panel).getValueAt(0, 1));
		assertEquals("digitalSignature,keyCertSign", rows(panel).getValueAt(0, 2));
		assertNull(panel.shownError, "a usable extension must not produce a complaint");
	}

	@ParameterizedTest
	@CsvSource({ "Key Usage,nonsense", "Basic Constraints,CA",
			"Subject Alternative Name,example.com", "Other (object id),not hex" })
	void aValueThatCannotBeBuiltIsRefusedAndNothingIsAdded(String kind, String value)
		throws Exception
	{
		TestableExtensionsPanel panel = newPanel();
		kindChooser(panel).setSelectedItem(kind);
		if (ExtensionsPanel.OTHER_EXTENSION.equals(kind))
		{
			extensionId(panel).setText("1.3.6.1.4.1.99999.1");
		}
		extensionValue(panel).setText(value);

		panel.onAddExtension();

		assertEquals(0, rows(panel).getRowCount(),
			"an extension that cannot be built must never reach the certificate");
		assertTrue(panel.shownError != null && !panel.shownError.isBlank(),
			"the reason has to be shown, not swallowed");
	}

	@Test
	void theReasonNamesTheExtensionAndWhatWasExpected() throws Exception
	{
		TestableExtensionsPanel panel = newPanel();
		kindChooser(panel).setSelectedItem("Basic Constraints");
		extensionValue(panel).setText("yes please");

		panel.onAddExtension();

		assertTrue(panel.shownError.contains("Basic Constraints"), panel.shownError);
		assertTrue(panel.shownError.contains("CA:true"), panel.shownError);
	}
}
