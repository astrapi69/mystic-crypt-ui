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
package io.github.astrapi69.mystic.crypt.plugin.kem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * Tests that the components of {@link KemDemoPanel} are bound to {@link KemDemoPanelModel}: what the
 * button runs with is the mechanism the model holds, and what a run produced arrives in the areas
 * that are bound to it.
 * <p>
 * Nothing here asserts that a setter was called. The mechanism is chosen in the combo box and the
 * proof that it travelled through the model is the ciphertext that comes back: each mechanism
 * produces a ciphertext of its own length, so a run on a stale selection shows up as the wrong
 * number of hex characters.
 */
class KemDemoPanelBindingTest
{

	/** The mechanism that combines X25519 with ML-KEM-768, the one the panel handles separately */
	private static final String HYBRID = "Hybrid X25519 + ML-KEM-768";

	@TempDir
	File configurationDirectory;

	static List<String> algorithms()
	{
		return KemDemoPanel.ALGORITHMS;
	}

	@AfterEach
	void restoreTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	private static <T extends Component> T named(Container container, String name, Class<T> type)
	{
		for (Component component : container.getComponents())
		{
			if (type.isInstance(component) && name.equals(component.getName()))
			{
				return type.cast(component);
			}
			if (component instanceof Container child)
			{
				T found = named(child, name, type);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private static String textOf(Container panel, String name)
	{
		return named(panel, name, JTextComponent.class).getText();
	}

	private static void choose(Container panel, String algorithm)
	{
		named(panel, "cmbAlgorithm", JComboBox.class).setSelectedItem(algorithm);
	}

	private static void click(Container panel, String name)
	{
		named(panel, name, JButton.class).doClick();
	}

	private static String resultOf(Container panel)
	{
		return named(panel, "lblResult", JLabel.class).getText();
	}

	/**
	 * How many hex characters the ciphertext of the given mechanism takes, measured by running that
	 * mechanism directly - the panel has to arrive at the same number
	 *
	 * @param algorithm
	 *            the mechanism
	 * @return the length of its ciphertext in hex characters
	 * @throws Exception
	 *             if the platform cannot perform the exchange
	 */
	private static int ciphertextLengthOf(String algorithm) throws Exception
	{
		NativeKemExchange.Result result = HYBRID.equals(algorithm)
			? NativeKemExchange.hybrid()
			: NativeKemExchange.mlKem(algorithm);
		return result.getCiphertext().length * 2;
	}

	/**
	 * The mechanism chosen in the combo box is the one the run used, for every mechanism the tool
	 * offers: the ciphertext that comes back has that mechanism's length and both sides arrived at
	 * the same secret
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("algorithms")
	@DisplayName("the mechanism chosen in the combo box is the one the run used")
	void theChosenMechanismIsTheOneThatRuns(String algorithm) throws Exception
	{
		KemDemoPanel panel = new KemDemoPanel();

		choose(panel, algorithm);
		click(panel, "btnRun");

		assertEquals("shared secrets match", resultOf(panel));
		assertEquals(ciphertextLengthOf(algorithm), textOf(panel, "txtCiphertext").length(),
			"the run did not use " + algorithm);
		assertFalse(textOf(panel, "txtSenderSecret").isBlank(), resultOf(panel));
		assertEquals(textOf(panel, "txtSenderSecret"), textOf(panel, "txtRecipientSecret"));
	}

	/**
	 * A second run uses what is chosen now and not what was chosen before, which is what binding the
	 * combo box to the model buys: the button reads the model, never the selection it started with
	 */
	@Test
	@DisplayName("a second run uses the mechanism chosen in between")
	void aSecondRunUsesTheMechanismChosenInBetween() throws Exception
	{
		KemDemoPanel panel = new KemDemoPanel();

		choose(panel, "ML-KEM-512");
		click(panel, "btnRun");
		String firstCiphertext = textOf(panel, "txtCiphertext");
		choose(panel, "ML-KEM-1024");
		click(panel, "btnRun");

		assertEquals(ciphertextLengthOf("ML-KEM-512"), firstCiphertext.length());
		assertEquals(ciphertextLengthOf("ML-KEM-1024"), textOf(panel, "txtCiphertext").length());
		assertNotEquals(firstCiphertext, textOf(panel, "txtCiphertext"));
	}

	/**
	 * The tool starts with the mechanism the settings dialog was left on, because that is what the
	 * model was built with - and the run proves it reached the model, not only the combo box
	 */
	@Test
	@DisplayName("the panel starts with the configured mechanism and runs with it")
	void theConfiguredMechanismIsTheOneTheFirstRunUses() throws Exception
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configurationDirectory.getAbsolutePath());
		KemSettingsContribution contribution = new KemSettingsContribution();
		PluginSettings.save(configurationDirectory, contribution.getPluginId(),
			contribution.getDefaults(),
			Map.of(KemSettingsContribution.KEY_ALGORITHM, "ML-KEM-1024"));

		KemDemoPanel panel = new KemDemoPanel();
		click(panel, "btnRun");

		assertEquals("ML-KEM-1024",
			named(panel, "cmbAlgorithm", JComboBox.class).getSelectedItem());
		assertEquals(ciphertextLengthOf("ML-KEM-1024"), textOf(panel, "txtCiphertext").length(),
			"the first run did not use the configured mechanism");
	}

	/**
	 * A mechanism the tool does not offer never reaches the model: the combo box refuses it and the
	 * run stays on the mechanism that was chosen last
	 */
	@Test
	@DisplayName("a mechanism that is not offered leaves the model on the last chosen one")
	void anUnofferedMechanismLeavesTheModelOnTheLastChosenOne() throws Exception
	{
		KemDemoPanel panel = new KemDemoPanel();

		choose(panel, "ML-KEM-512");
		choose(panel, "ML-KEM-2048");
		click(panel, "btnRun");

		assertEquals("shared secrets match", resultOf(panel));
		assertEquals(ciphertextLengthOf("ML-KEM-512"), textOf(panel, "txtCiphertext").length());
	}

	/**
	 * The combo box offers exactly the mechanisms the tool supports, in the order it offers them
	 */
	@Test
	@DisplayName("the combo box offers exactly the mechanisms of the tool")
	void theComboBoxOffersExactlyTheMechanismsOfTheTool()
	{
		KemDemoPanel panel = new KemDemoPanel();

		JComboBox<?> comboBox = named(panel, "cmbAlgorithm", JComboBox.class);

		assertEquals(KemDemoPanel.ALGORITHMS.size(), comboBox.getItemCount());
		for (int index = 0; index < KemDemoPanel.ALGORITHMS.size(); index++)
		{
			assertEquals(KemDemoPanel.ALGORITHMS.get(index), comboBox.getItemAt(index));
		}
	}
}
