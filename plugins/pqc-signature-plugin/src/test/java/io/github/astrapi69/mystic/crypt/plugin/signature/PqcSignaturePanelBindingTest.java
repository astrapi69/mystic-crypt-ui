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
package io.github.astrapi69.mystic.crypt.plugin.signature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.Security;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.crypt.data.key.writer.PublicKeyWriter;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * Tests that the panel really works off its {@link PqcSignaturePanelModel}: what is typed into a
 * component has to reach the model, because every action reads the model and no longer the widget.
 * The panel is driven the way the end-to-end tests drive it - components looked up by their name,
 * buttons clicked - but headlessly, without a frame.
 */
class PqcSignaturePanelBindingTest
{

	@TempDir
	File workingDirectory;

	private PqcSignaturePanel panel;

	@BeforeEach
	void createThePanelWithATemporaryConfigurationDirectory()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
		// the panel starts with the configured algorithm and message; a temporary configuration
		// directory keeps the test off the real settings of this machine
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			workingDirectory.getAbsolutePath());
		panel = new PqcSignaturePanel();
	}

	@AfterEach
	void restoreTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	/**
	 * The key files, the file to sign and the checkbox all reach the model: signing and verifying
	 * work with nothing but what was typed into the components
	 */
	@Test
	void signsAndVerifiesWithWhatWasTypedIntoTheComponents() throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		File privateKeyFile = new File(workingDirectory, "signing-key.pem");
		File publicKeyFile = new File(workingDirectory, "signing-key-public.pem");
		PrivateKeyWriter.writeInPemFormat(keyPair.getPrivate(), privateKeyFile);
		PublicKeyWriter.writeInPemFormat(keyPair.getPublic(), publicKeyFile);
		File documentFile = new File(workingDirectory, "contract.txt");
		Files.writeString(documentFile.toPath(), "this is the document that gets signed");

		type("txtPrivateKeyFile", privateKeyFile.getAbsolutePath());
		type("txtPublicKeyFile", publicKeyFile.getAbsolutePath());
		type("txtDataFile", documentFile.getAbsolutePath());
		checkBox("chkUseFile").setSelected(true);

		click("btnSign");
		assertTrue(panel.getResultText().contains("SHA256withRSA"),
			"the algorithm has to come from the key file that was typed in: "
				+ panel.getResultText());

		click("btnVerify");
		assertEquals("signature is valid", panel.getResultText());
	}

	/** The message field reaches the model: what is signed is the text that stands in it */
	@Test
	void signsTheMessageThatStandsInTheMessageField()
	{
		click("btnGenerate");
		type("txtMessage", "the message that is signed");

		click("btnSign");
		click("btnVerify");
		assertEquals("signature is valid", panel.getResultText());

		type("txtMessage", "a different message");
		click("btnVerify");
		assertEquals("signature is not valid", panel.getResultText(),
			"a changed message must not verify with the old signature");
	}

	/** The algorithm combo box reaches the model: the key pair is generated for the chosen one */
	@ParameterizedTest
	@ValueSource(strings = { "ML-DSA-44", "Ed25519" })
	void generatesAKeyPairForTheAlgorithmChosenInTheComboBox(String algorithm)
	{
		comboBox().setSelectedItem(algorithm);

		click("btnGenerate");

		assertTrue(panel.getResultText().contains(algorithm),
			"the chosen algorithm has to reach the model: " + panel.getResultText());
	}

	/** The signature file field reaches the model, and a loaded signature reaches it back */
	@Test
	void savesAndLoadsTheSignatureThroughTheFileField()
	{
		File signatureFile = new File(workingDirectory, "message.sig");
		click("btnGenerate");
		type("txtMessage", "the message that is signed");
		click("btnSign");

		type("txtSignatureFile", signatureFile.getAbsolutePath());
		click("btnSaveSignature");
		assertTrue(signatureFile.exists(), "saving must write the file: " + panel.getResultText());

		type("txtSignature", "");
		click("btnVerify");
		assertEquals("there is no signature to check", panel.getResultText(),
			"an emptied signature field has to empty the model too");

		click("btnLoadSignature");
		click("btnVerify");
		assertEquals("signature is valid", panel.getResultText());
	}

	/** Without a key file and without a generated pair there is nothing to sign with */
	@Test
	void withoutAKeyThereIsNothingToSignWith()
	{
		click("btnSign");

		assertTrue(panel.getResultText().contains("generate a key pair first"),
			panel.getResultText());
		assertFalse(panel.getResultText().contains("signed 0 bytes"));
	}

	private void type(String name, String text)
	{
		component(JTextComponent.class, name).setText(text);
	}

	private void click(String name)
	{
		component(JButton.class, name).doClick();
	}

	private JCheckBox checkBox(String name)
	{
		return component(JCheckBox.class, name);
	}

	private JComboBox<?> comboBox()
	{
		return component(JComboBox.class, "cmbAlgorithm");
	}

	private <T extends Component> T component(Class<T> type, String name)
	{
		T found = find(panel, type, name);
		if (found == null)
		{
			throw new AssertionError("no " + type.getSimpleName() + " named '" + name + "'");
		}
		return found;
	}

	private <T extends Component> T find(Container container, Class<T> type, String name)
	{
		for (Component component : container.getComponents())
		{
			if (type.isInstance(component) && name.equals(component.getName()))
			{
				return type.cast(component);
			}
			if (component instanceof Container)
			{
				T found = find((Container)component, type, name);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private static void assertHasTooltip(javax.swing.JComponent component, String fieldName)
	{
		String tooltip = component.getToolTipText();
		assertTrue(tooltip != null && !tooltip.isBlank(), fieldName + " must have a tooltip");
	}

	@Test
	void everyFieldExplainsItselfWithATooltip()
	{
		assertHasTooltip(component(javax.swing.JComponent.class, "cmbAlgorithm"), "algorithm");
		assertHasTooltip(component(javax.swing.JComponent.class, "txtMessage"), "message");
		assertHasTooltip(component(javax.swing.JComponent.class, "txtDataFile"), "data file");
		assertHasTooltip(component(javax.swing.JComponent.class, "chkUseFile"), "use file");
		assertHasTooltip(component(javax.swing.JComponent.class, "txtPrivateKeyFile"),
			"private key file");
		assertHasTooltip(component(javax.swing.JComponent.class, "txtPublicKeyFile"),
			"public key file");
		assertHasTooltip(component(javax.swing.JComponent.class, "txtSignatureFile"),
			"signature file");
		assertHasTooltip(component(javax.swing.JComponent.class, "txtPublicKey"), "public key");
		assertHasTooltip(component(javax.swing.JComponent.class, "txtSignature"), "signature");
		assertHasTooltip(component(javax.swing.JComponent.class, "btnGenerate"), "generate");
		assertHasTooltip(component(javax.swing.JComponent.class, "btnBrowsePrivateKey"),
			"browse private key");
		assertHasTooltip(component(javax.swing.JComponent.class, "btnBrowsePublicKey"),
			"browse public key");
		assertHasTooltip(component(javax.swing.JComponent.class, "btnBrowseDataFile"),
			"browse data file");
		assertHasTooltip(component(javax.swing.JComponent.class, "btnBrowseSignatureFile"),
			"browse signature file");
		assertHasTooltip(component(javax.swing.JComponent.class, "btnSign"), "sign");
		assertHasTooltip(component(javax.swing.JComponent.class, "btnVerify"), "verify");
		assertHasTooltip(component(javax.swing.JComponent.class, "btnSaveSignature"),
			"save signature");
		assertHasTooltip(component(javax.swing.JComponent.class, "btnLoadSignature"),
			"load signature");
	}
}
