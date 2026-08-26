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
package io.github.astrapi69.mystic.crypt.plugin.checksum;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * Proof that the {@link ChecksumAndMacPanel} really holds its state in its
 * {@link ChecksumAndMacPanelModel}: what is typed into a component has to be readable from the
 * model, and what a button computes has to come out of the model rather than out of the widgets.
 * <p>
 * The panel is built headless and driven the way the end-to-end test drives it, so the binding is
 * checked against the components the user actually operates.
 */
class ChecksumAndMacPanelBindingTest
{

	/** The SHA-256 of no bytes at all, the state the panel starts in */
	private static final String SHA_256_OF_NOTHING =
		"e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

	/** The SHA-256 of the ASCII string "abc" */
	private static final String SHA_256_OF_ABC =
		"ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

	/** The published HmacSHA256 of the fox sentence under the key "key" */
	private static final String HMAC_OF_THE_FOX =
		"f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8";

	@TempDir
	File configurationDirectory;

	@BeforeEach
	void useATemporaryConfigurationDirectory()
	{
		// the panel starts with the configured digest; a temporary directory keeps the test off
		// the real settings of this machine
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configurationDirectory.getAbsolutePath());
	}

	@AfterEach
	void restoreTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	@Test
	void modelHoldsTheConfiguredDigestAndTheFirstCodeWhenThePanelIsBuilt()
	{
		ChecksumAndMacPanel panel = new ChecksumAndMacPanel();

		assertEquals(ChecksumSettingsContribution.digest(), panel.getModelObject().getDigest(),
			"the digest the settings name is what the model starts with");
		assertEquals(panel.getModelObject().getDigest(),
			find(panel, "cmbDigest", JComboBox.class).getSelectedItem(),
			"and the combo box shows exactly that");
		assertEquals(ChecksumSupport.MACS.get(0), panel.getModelObject().getMacAlgorithm(),
			"the code tab starts with the first code, the one its combo box shows");

		find(panel, "cmbDigest", JComboBox.class).setSelectedItem("SHA3-256");
		assertEquals("SHA3-256", panel.getModelObject().getDigest(),
			"choosing another digest puts it in the model, without any button being pressed");
	}

	@Test
	void computesTheChecksumOfTheTextTheModelHolds()
	{
		ChecksumAndMacPanel panel = new ChecksumAndMacPanel();

		find(panel, "btnChecksum", JButton.class).doClick();
		assertEquals(SHA_256_OF_NOTHING, panel.getModelObject().getChecksum(),
			"an untouched text field holds empty text, not absent text: " + panel.getResultText());

		find(panel, "txtChecksumText", JTextArea.class).setText("abc");
		assertEquals("abc", panel.getModelObject().getChecksumText(),
			"what is typed into the text area is in the model before any button is pressed");

		find(panel, "btnChecksum", JButton.class).doClick();
		assertEquals(SHA_256_OF_ABC, panel.getModelObject().getChecksum(),
			"the computed checksum goes back into the model: " + panel.getResultText());
		assertEquals("SHA-256 over the text", panel.getModelObject().getResultMessage(),
			"and so does the message shown below the tabs");
		assertEquals(panel.getModelObject().getResultMessage(), panel.getResultText(),
			"the label shows what the model holds");
	}

	@Test
	void comparesTheComputedChecksumWithTheValueThatWasPastedIn()
	{
		ChecksumAndMacPanel panel = new ChecksumAndMacPanel();
		find(panel, "txtChecksumText", JTextArea.class).setText("abc");

		find(panel, "txtExpected", JTextField.class).setText(SHA_256_OF_ABC.toUpperCase());
		assertEquals(SHA_256_OF_ABC.toUpperCase(), panel.getModelObject().getExpectedChecksum(),
			"the pasted value is in the model as it was pasted");

		find(panel, "btnCompare", JButton.class).doClick();
		assertEquals("the checksums are the same", panel.getModelObject().getResultMessage(),
			"a value pasted in upper case is still the same value");
	}

	@Test
	void computesTheChecksumOverTheFileWhenTheCheckBoxSaysSo(@TempDir File tempDir) throws Exception
	{
		File file = new File(tempDir, "abc.txt");
		Files.write(file.toPath(), "abc".getBytes(StandardCharsets.UTF_8));
		ChecksumAndMacPanel panel = new ChecksumAndMacPanel();

		find(panel, "txtChecksumFile", JTextField.class).setText(file.getAbsolutePath());
		find(panel, "chkChecksumUseFile", JCheckBox.class).setSelected(true);
		assertEquals(file.getAbsolutePath(), panel.getModelObject().getChecksumFile(),
			"the chosen path is in the model");
		assertTrue(panel.getModelObject().isChecksumOverFile(),
			"and so is the answer to which of the two inputs is used");

		find(panel, "btnChecksum", JButton.class).doClick();
		assertEquals(SHA_256_OF_ABC, panel.getModelObject().getChecksum(),
			"the file is what was hashed: " + panel.getResultText());
		assertEquals("SHA-256 over the file", panel.getModelObject().getResultMessage(),
			"the message says which of the two inputs it was");
	}

	@Test
	void keepsTheKeyAsCharactersAndComputesTheCodeFromTheModel()
	{
		ChecksumAndMacPanel panel = new ChecksumAndMacPanel();

		find(panel, "txtMacText", JTextArea.class)
			.setText("The quick brown fox jumps over the lazy dog");
		find(panel, "pwdMacKey", JPasswordField.class).setText("key");
		assertArrayEquals("key".toCharArray(), panel.getModelObject().getMacKey(),
			"a password stays characters in the model, it does not become a string");

		find(panel, "btnMac", JButton.class).doClick();
		assertEquals(HMAC_OF_THE_FOX, panel.getModelObject().getMac(),
			"the published value for that message and that key: " + panel.getResultText());

		find(panel, "txtMacExpected", JTextField.class).setText(HMAC_OF_THE_FOX);
		find(panel, "btnCompareMac", JButton.class).doClick();
		assertEquals("the codes are the same", panel.getModelObject().getResultMessage(),
			"the comparison reads both values from the model");
	}

	private static <T extends Component> T find(Container root, String name, Class<T> type)
	{
		T found = search(root, name, type);
		assertNotNull(found, "no " + type.getSimpleName() + " named " + name + " in the panel");
		return found;
	}

	private static <T extends Component> T search(Container root, String name, Class<T> type)
	{
		for (Component component : root.getComponents())
		{
			if (type.isInstance(component) && name.equals(component.getName()))
			{
				return type.cast(component);
			}
			if (component instanceof Container container)
			{
				T found = search(container, name, type);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}
}
