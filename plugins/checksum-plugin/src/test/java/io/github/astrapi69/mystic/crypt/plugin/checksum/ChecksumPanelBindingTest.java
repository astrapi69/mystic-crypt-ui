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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.swing.JComboBox;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.api.algorithm.ChecksumAlgorithm;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * Proof that the {@link ChecksumPanel} really holds its state in its {@link ChecksumBean}: what is
 * chosen in the combo box or typed into a text area has to be readable from the model, and what a
 * button computes or compares has to come out of the model rather than out of the widgets.
 * <p>
 * The panel is built headless and driven through the components the user operates, over a real file
 * on disk whose digests are published values.
 */
class ChecksumPanelBindingTest
{

	/** The MD5 of the ASCII string "abc" */
	private static final String MD5_OF_ABC = "900150983cd24fb0d6963f7d28e17f72";

	/** The SHA-256 of the ASCII string "abc" */
	private static final String SHA_256_OF_ABC =
		"ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

	@TempDir
	File configurationDirectory;

	@BeforeEach
	void useATemporaryConfigurationDirectory()
	{
		// the panel starts with the configured algorithm; a temporary directory keeps the test off
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
	void modelHoldsTheConfiguredAlgorithmAndTheUntouchedResultWhenThePanelIsBuilt()
	{
		ChecksumPanel panel = new ChecksumPanel();

		assertEquals(ChecksumSettingsContribution.algorithm(),
			panel.getModelObject().getSelectedAlgorithm(),
			"the algorithm the settings name is what the model starts with");
		assertEquals(panel.getModelObject().getSelectedAlgorithm(),
			panel.getCbxChecksumAlgorithm().getSelectedItem(),
			"and the combo box shows exactly that");
		assertEquals("Checksum Match Result", panel.getModelObject().getChecksumMatchResult(),
			"nothing was compared yet, which the model says as well as the field");
		assertEquals(panel.getModelObject().getChecksumMatchResult(),
			panel.getTxtChecksumMatchResult().getText(), "the field shows what the model holds");
		assertEquals("", panel.getModelObject().getGeneratedChecksum(),
			"an untouched text area holds empty text, not absent text");
		assertEquals("", panel.getModelObject().getOwnersChecksum(),
			"which is what the second one holds as well");
	}

	@Test
	void theAlgorithmChosenInTheComboBoxDecidesWhatIsComputed(@TempDir File directory)
		throws Exception
	{
		File file = fileWithAbc(directory);
		ChecksumPanel panel = new ChecksumPanel();
		panel.getModelObject().setSelectedFile(file);

		chooseAlgorithm(panel, ChecksumAlgorithm.MD5);
		assertEquals(ChecksumAlgorithm.MD5, panel.getModelObject().getSelectedAlgorithm(),
			"choosing an algorithm puts it in the model, without any button being pressed");
		assertEquals(MD5_OF_ABC, panel.getModelObject().getGeneratedChecksum(),
			"the computed checksum goes back into the model");
		assertEquals(MD5_OF_ABC, panel.getTxtGeneratedChecksum().getText(),
			"and the text area shows exactly that");

		chooseAlgorithm(panel, ChecksumAlgorithm.SHA_256);
		assertEquals(SHA_256_OF_ABC, panel.getModelObject().getGeneratedChecksum(),
			"the computation reads the algorithm the model holds, not the one it was built with");
	}

	@Test
	void comparingReadsBothChecksumsFromTheModel(@TempDir File directory) throws Exception
	{
		File file = fileWithAbc(directory);
		ChecksumPanel panel = new ChecksumPanel();
		panel.getModelObject().setSelectedFile(file);
		chooseAlgorithm(panel, ChecksumAlgorithm.SHA_256);

		panel.getTxtOwnersChecksum().setText(SHA_256_OF_ABC);
		assertEquals(SHA_256_OF_ABC, panel.getModelObject().getOwnersChecksum(),
			"what is typed into the text area is in the model before any button is pressed");

		panel.getBtnCompare().doClick();
		assertEquals("Match", panel.getModelObject().getChecksumMatchResult(),
			"the button compared the two values the model holds");
		assertEquals("Match", panel.getTxtChecksumMatchResult().getText(),
			"and the field shows what the model holds");

		panel.getTxtOwnersChecksum().setText(MD5_OF_ABC);
		panel.getBtnCompare().doClick();
		assertEquals("No Match", panel.getModelObject().getChecksumMatchResult(),
			"a value that is not the computed one is reported as no match");
	}

	@Test
	void clearingTakesTheFileAndWhatWasComputedForItOutOfTheModel(@TempDir File directory)
		throws Exception
	{
		File file = fileWithAbc(directory);
		ChecksumPanel panel = new ChecksumPanel();
		panel.getModelObject().setSelectedFile(file);
		panel.getTxtOpenFile().setText(file.getName());
		assertEquals(file.getName(), panel.getModelObject().getSelectedFilename(),
			"the name of the chosen file travels into the model through the field");
		chooseAlgorithm(panel, ChecksumAlgorithm.MD5);

		assertTrue(panel.getBtnClearOpenFile().getModel().isEnabled(),
			"the clear button becomes usable once there is a checksum to clear");
		panel.getBtnClearOpenFile().doClick();

		assertNull(panel.getModelObject().getSelectedFile(), "the file is gone from the model");
		assertEquals("", panel.getModelObject().getSelectedFilename(), "and so is its name");
		assertEquals("", panel.getModelObject().getGeneratedChecksum(),
			"and so is what was computed for it");
	}

	@Test
	void copyButtonsStayDisabledUntilThereIsSomethingToCopy(@TempDir File directory)
		throws Exception
	{
		File file = fileWithAbc(directory);
		ChecksumPanel panel = new ChecksumPanel();

		assertFalse(panel.getBtnCopyGeneratedChecksum().getModel().isEnabled(),
			"nothing was generated yet, so there is nothing to copy");
		assertFalse(panel.getBtnCopyOwnersChecksum().getModel().isEnabled(),
			"nothing was typed or loaded yet either");

		panel.getModelObject().setSelectedFile(file);
		chooseAlgorithm(panel, ChecksumAlgorithm.MD5);
		assertTrue(panel.getBtnCopyGeneratedChecksum().getModel().isEnabled(),
			"a checksum was generated, so it can now be copied");

		panel.getTxtOwnersChecksum().setText(MD5_OF_ABC);
		assertTrue(panel.getBtnCopyOwnersChecksum().getModel().isEnabled(),
			"a checksum was typed, so it can now be copied");
	}

	/**
	 * A file whose content is the ASCII string "abc", the input the published digests belong to
	 *
	 * @param directory
	 *            the temporary directory the file is created in
	 * @return the file
	 * @throws Exception
	 *             is thrown when the file cannot be written
	 */
	private static File fileWithAbc(final File directory) throws Exception
	{
		File file = new File(directory, "abc.txt");
		Files.write(file.toPath(), "abc".getBytes(StandardCharsets.UTF_8));
		return file;
	}

	/**
	 * Chooses an algorithm the way the user does, through the combo box. Selecting what is already
	 * selected fires no event, so a choice that would change nothing goes past another algorithm
	 * first
	 *
	 * @param panel
	 *            the panel whose combo box is used
	 * @param algorithm
	 *            the algorithm to choose
	 */
	private static void chooseAlgorithm(final ChecksumPanel panel,
		final ChecksumAlgorithm algorithm)
	{
		JComboBox<ChecksumAlgorithm> comboBox = panel.getCbxChecksumAlgorithm();
		if (algorithm.equals(comboBox.getSelectedItem()))
		{
			comboBox.setSelectedItem(ChecksumAlgorithm.MD5.equals(algorithm)
				? ChecksumAlgorithm.SHA_512
				: ChecksumAlgorithm.MD5);
		}
		comboBox.setSelectedItem(algorithm);
	}
}
