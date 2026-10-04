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
import java.security.Security;

import javax.swing.JButton;
import javax.swing.JComboBox;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The dropdown offers the classical algorithms on purpose - an RSA key that came from a key store
 * signs and verifies here - but this tool cannot GENERATE one. The offer that has to disappear is
 * therefore the button, not the algorithm, and it has to disappear before it is pressed rather than
 * answer with a refusal afterwards (#488).
 */
class GenerateOnlyWhatCanBeGeneratedTest
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
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			workingDirectory.getAbsolutePath());
		panel = new PqcSignaturePanel();
	}

	@AfterEach
	void restoreTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	static java.util.List<String> theClassicalOnes()
	{
		return SignatureSupport.CLASSICAL_ALGORITHMS;
	}

	static java.util.List<String> theGeneratableOnes()
	{
		return SignatureSupport.algorithms();
	}

	@ParameterizedTest(name = "{0} cannot be generated here, so the button is off")
	@MethodSource("theClassicalOnes")
	@DisplayName("a classical algorithm turns the generate button off")
	void aClassicalAlgorithm_turnsTheButtonOff(String algorithm)
	{
		comboBox().setSelectedItem(algorithm);

		assertFalse(button("btnGenerate").isEnabled(),
			"pressing it could only produce a refusal, so it must not be pressable");
	}

	@ParameterizedTest(name = "{0} can be generated here, so the button is on")
	@MethodSource("theGeneratableOnes")
	@DisplayName("an algorithm this tool generates keeps the button on")
	void aGeneratableAlgorithm_keepsTheButtonOn(String algorithm)
	{
		comboBox().setSelectedItem(SignatureSupport.CLASSICAL_ALGORITHMS.get(0));

		comboBox().setSelectedItem(algorithm);

		assertTrue(button("btnGenerate").isEnabled(),
			"the button has to come back, or choosing RSA once disables generating forever");
	}

	@Test
	@DisplayName("the panel says why before the press, not after it")
	void thePanelSaysWhy_beforeThePress()
	{
		comboBox().setSelectedItem("SHA256withRSA");

		String said = panel.getResultText();
		assertTrue(said.contains("SHA256withRSA"), "the reason has to name the algorithm: " + said);
		assertTrue(said.contains("file"),
			"and it has to say where such a key comes from instead: " + said);
	}

	@Test
	@DisplayName("choosing a generatable algorithm again clears the reason")
	void choosingAGeneratableAlgorithmAgain_clearsTheReason()
	{
		comboBox().setSelectedItem("SHA256withRSA");

		comboBox().setSelectedItem("Ed25519");

		assertFalse(panel.getResultText().contains("SHA256withRSA"),
			"a reason that outlives its cause reads as the result of the next action");
	}

	@Test
	@DisplayName("the button starts in the state the configured algorithm calls for")
	void theButtonStartsInTheStateTheConfiguredAlgorithmCallsFor()
	{
		assertEquals(SignatureSupport.canGenerateKeyPair((String)comboBox().getSelectedItem()),
			button("btnGenerate").isEnabled(),
			"the rule has to hold for the state the panel opens in, not only after a change");
	}

	private JComboBox<?> comboBox()
	{
		return component(JComboBox.class, "cmbAlgorithm");
	}

	private JButton button(String name)
	{
		return component(JButton.class, name);
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
}
