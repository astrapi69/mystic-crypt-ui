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
package io.github.astrapi69.mystic.crypt.plugin.keygen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.security.interfaces.RSAPublicKey;

import javax.swing.JComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.api.key.KeySize;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * Tests that the components of {@link CryptographyPanel} are bound to the
 * {@link GenerateKeysModelBean} the panel holds: the key size that was chosen in the box is the
 * size the generated key pair has, and the two key areas carry what they show into the model.
 * <p>
 * Where a button is pressed the panel is built the way the application composes it - inside a
 * {@link GenerateKeysPanel} - because its own buttons are callbacks the surrounding panel fills
 * in, and the proof is the key that comes out, not a setter that was called.
 */
@DisplayName("The key generation panel reads what it generates from its model")
class CryptographyPanelBindingTest
{

	/** The key size the tool is configured with when no settings were stored yet */
	private static final KeySize CONFIGURED_KEY_SIZE = KeySize.KEYSIZE_2048;

	@TempDir
	File configurationDirectory;

	@BeforeEach
	void useATemporaryConfigurationDirectory()
	{
		// the panel preselects the key size from the installed configuration directory; pointing it
		// at a temporary one keeps the test off the real settings of this machine
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configurationDirectory.getAbsolutePath());
	}

	@AfterEach
	void restoreTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	@Test
	@DisplayName("the key size box and the model start at the configured key size")
	void theKeySizeBoxAndTheModelStartAtTheConfiguredKeySize()
	{
		CryptographyPanel panel = new CryptographyPanel();

		assertEquals(CONFIGURED_KEY_SIZE, panel.getCmbKeySize().getSelectedItem());
		assertEquals(CONFIGURED_KEY_SIZE, panel.getModelObject().getKeySize(),
			"the configured key size has to be in the model, not only in the box");
	}

	@Test
	@DisplayName("generating uses the key size that was chosen in the bound box")
	void generateUsesTheKeySizeThatWasChosenInTheBoundBox()
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();
		CryptographyPanel cryptographyPanel = panel.getCryptographyPanel();

		cryptographyPanel.getCmbKeySize().setSelectedItem(KeySize.KEYSIZE_1024);
		cryptographyPanel.getBtnGenerate().doClick();

		RSAPublicKey publicKey = (RSAPublicKey)panel.getModelObject().getPublicKey();
		assertEquals(1024, publicKey.getModulus().bitLength(),
			"the generated key must have the size that was chosen in the box");
	}

	@Test
	@DisplayName("what the key areas show is what the model holds")
	void theGeneratedKeyPairReachesTheModelAsPem()
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();
		CryptographyPanel cryptographyPanel = panel.getCryptographyPanel();
		cryptographyPanel.getCmbKeySize().setSelectedItem(KeySize.KEYSIZE_1024);

		cryptographyPanel.getBtnGenerate().doClick();

		GenerateKeysModelBean modelObject = panel.getModelObject();
		assertTrue(modelObject.getPrivateKeyPem().contains("PRIVATE KEY"),
			"the private key has to be in the model as PEM: " + modelObject.getPrivateKeyPem());
		assertTrue(modelObject.getPublicKeyPem().contains("PUBLIC KEY"),
			"the public key has to be in the model as PEM, was: " + modelObject.getPublicKeyPem());
		assertEquals(cryptographyPanel.getTxtPrivateKey().getText(),
			modelObject.getPrivateKeyPem());
		assertEquals(cryptographyPanel.getTxtPublicKey().getText(), modelObject.getPublicKeyPem());
	}

	@Test
	@DisplayName("clearing the keys empties them in the model too")
	void clearEmptiesTheKeyAreasInTheModelToo()
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();
		CryptographyPanel cryptographyPanel = panel.getCryptographyPanel();
		cryptographyPanel.getCmbKeySize().setSelectedItem(KeySize.KEYSIZE_1024);
		cryptographyPanel.getBtnGenerate().doClick();

		cryptographyPanel.getBtnClear().doClick();

		GenerateKeysModelBean modelObject = panel.getModelObject();
		assertEquals("", modelObject.getPrivateKeyPem());
		assertEquals("", modelObject.getPublicKeyPem());
		assertEquals(KeySize.KEYSIZE_1024, modelObject.getKeySize());
		assertNull(modelObject.getPrivateKey());
		assertNull(modelObject.getPublicKey());
	}

	private static void assertHasTooltip(JComponent component, String fieldName)
	{
		String tooltip = component.getToolTipText();
		assertTrue(tooltip != null && !tooltip.isBlank(), fieldName + " must have a tooltip");
	}

	@Test
	@DisplayName("the key size box and every button explain themselves")
	void everyFieldExplainsItselfWithATooltip()
	{
		CryptographyPanel panel = new CryptographyPanel();

		assertHasTooltip(panel.getCmbKeySize(), "key size");
		assertHasTooltip(panel.getBtnGenerate(), "generate button");
		assertHasTooltip(panel.getBtnClear(), "clear button");
		assertHasTooltip(panel.getBtnSavePrivateKey(), "save private key button");
		assertHasTooltip(panel.getBtnSavePrivKeyWithPw(), "save private key with password button");
		assertHasTooltip(panel.getBtnSavePublicKey(), "save public key button");
		assertHasTooltip(panel.getBtnSaveCertificate(), "save certificate button");
	}
}
