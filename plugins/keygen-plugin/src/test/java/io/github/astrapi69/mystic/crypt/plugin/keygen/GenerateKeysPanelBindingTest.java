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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.PublicKey;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.key.KeyFormat;
import io.github.astrapi69.crypt.api.key.KeySize;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * Tests that the components of {@link GenerateKeysPanel} are bound to the
 * {@link GenerateKeysModelBean} the panel holds: the algorithm, the curve and the key file format
 * that were chosen are what the buttons work with, and the boxes offer exactly the values the tool
 * supports, in the order it offers them.
 * <p>
 * The proof is the key that comes out and the file that appears, not a setter that was called.
 */
@DisplayName("The key generation tool window reads what it generates and saves from its model")
class GenerateKeysPanelBindingTest
{

	/** The curve chosen in the test, one that is not the first the box offers */
	private static final String CHOSEN_CURVE = "secp256k1";

	@TempDir
	File temporaryDirectory;

	@TempDir
	File configurationDirectory;

	@BeforeAll
	static void registerBouncyCastle()
	{
		// self-contained on purpose: the application registers the provider at startup, but a test
		// must never depend on another class having run first
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@BeforeEach
	void useATemporaryConfigurationDirectory()
	{
		// the panel preselects the algorithm from the installed configuration directory; a
		// temporary one keeps the test off the real settings of this machine
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configurationDirectory.getAbsolutePath());
	}

	@AfterEach
	void restoreTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	private static List<Object> itemsOf(final JComboBox<?> comboBox)
	{
		List<Object> items = new ArrayList<>();
		for (int index = 0; index < comboBox.getItemCount(); index++)
		{
			items.add(comboBox.getItemAt(index));
		}
		return items;
	}

	@Test
	@DisplayName("generating uses the algorithm and the curve chosen in the bound boxes")
	void generateUsesTheAlgorithmAndTheCurveFromTheBoundBoxes()
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();

		panel.getCmbAlgorithm().setSelectedItem(KeyPairGeneratorAlgorithm.EC);
		panel.getCmbCurve().setSelectedItem(CHOSEN_CURVE);
		panel.getCryptographyPanel().getBtnGenerate().doClick();

		PublicKey publicKey = panel.getModelObject().getPublicKey();
		assertEquals("EC", publicKey.getAlgorithm(),
			"the algorithm of the bound box did not reach the generation");
		assertEquals(CHOSEN_CURVE, KeygenSupport.curveOf(publicKey),
			"the key must sit on the curve that was picked, not on the provider's default");
	}

	@ParameterizedTest(name = "a private key saved as {0} begins with {1}")
	@DisplayName("the key file format chosen in the bound box decides what is written")
	@CsvSource({ "PKCS_8, -----BEGIN PRIVATE KEY-----", "PKCS_1, -----BEGIN RSA PRIVATE KEY-----" })
	void theKeyFileFormatFromTheBoundBoxDecidesWhatIsWritten(final KeyFormat keyFormat,
		final String header) throws Exception
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();
		panel.getCryptographyPanel().getCmbKeySize().setSelectedItem(KeySize.KEYSIZE_1024);
		panel.getCmbKeyFormat().setSelectedItem(keyFormat);
		panel.getCryptographyPanel().getBtnGenerate().doClick();

		File privateKeyFile = new File(temporaryDirectory, "private-" + keyFormat + ".pem");
		panel.savePrivateKeyTo(privateKeyFile);

		String pem = Files.readString(privateKeyFile.toPath());
		assertTrue(pem.startsWith(header), "the format of the bound box did not reach the file: "
			+ pem.lines().findFirst().orElse(""));
	}

	@Test
	@DisplayName("clearing puts the configured algorithm back into the model")
	void clearPutsTheConfiguredAlgorithmBackIntoTheModel()
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();

		panel.getCmbAlgorithm().setSelectedItem(KeyPairGeneratorAlgorithm.ML_DSA_65);
		assertEquals(KeyPairGeneratorAlgorithm.ML_DSA_65, panel.getModelObject().getAlgorithm(),
			"what was chosen in the box has to be in the model");
		assertFalse(panel.getCryptographyPanel().getCmbKeySize().isEnabled(),
			"a modern algorithm has a fixed parameter set, so the key size is irrelevant");

		panel.getCryptographyPanel().getBtnClear().doClick();

		assertEquals(KeyPairGeneratorAlgorithm.RSA, panel.getModelObject().getAlgorithm(),
			"clearing has to put the configured algorithm back into the model");
		assertEquals(KeySize.KEYSIZE_1024, panel.getModelObject().getKeySize());
		assertTrue(panel.getCryptographyPanel().getCmbKeySize().isEnabled());
	}

	@Test
	@DisplayName("the boxes offer the supported values in the order the tool offers them")
	void theBoxesOfferTheSupportedValuesInTheOfferedOrder()
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();

		assertEquals(KeygenSupport.CURVES, itemsOf(panel.getCmbCurve()));
		assertEquals(KeygenSupport.keyFormats(), itemsOf(panel.getCmbKeyFormat()));
		assertEquals(KeygenSupport.CURVES.get(0), panel.getModelObject().getCurve(),
			"the curve the box starts on has to be in the model");
		assertEquals(KeygenSupport.keyFormats().get(0), panel.getModelObject().getKeyFormat(),
			"the key file format the box starts on has to be in the model");
	}
}
