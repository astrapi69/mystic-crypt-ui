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
package io.github.astrapi69.mystic.crypt.plugin.password;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.util.UUID;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * Proof that {@link PasswordHashPanel} really holds its state in its
 * {@link PasswordHashPanelModel}: what is typed or chosen in a component has to be readable from
 * the model, and the two buttons have to work with what the model holds rather than with what they
 * read back out of the widgets.
 * <p>
 * The panel is built headless and driven through the components the user operates, and every
 * assertion is on what a button did - the hash that appears, the verdict it reports - so a binding
 * that did not carry the value cannot pass.
 */
class PasswordHashPanelBindingTest
{

	/**
	 * The password this test hashes. It is made up per run rather than written into the source, so
	 * nothing here reads as a credential.
	 */
	private static final String PASSWORD = "bound-" + UUID.randomUUID();

	/** A password that is not the one that was hashed */
	private static final String OTHER_PASSWORD = "other-" + UUID.randomUUID();

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
	@DisplayName("the panel starts with the configured algorithm and a choice lands in the model")
	void modelHoldsTheAlgorithmTheComboBoxShows()
	{
		PasswordHashPanel panel = new PasswordHashPanel();

		assertEquals(PasswordHashSettingsContribution.algorithm(),
			panel.getModelObject().getAlgorithm(),
			"the algorithm the settings name is what the model starts with");
		assertEquals(panel.getModelObject().getAlgorithm(),
			find(panel, "cmbAlgorithm", JComboBox.class).getSelectedItem(),
			"and the combo box shows exactly that");

		find(panel, "cmbAlgorithm", JComboBox.class).setSelectedItem(PasswordHashSupport.PBKDF2);

		assertEquals(PasswordHashSupport.PBKDF2, panel.getModelObject().getAlgorithm(),
			"choosing another algorithm puts it in the model, without any button being pressed");
		assertEquals(PasswordHashSupport.describe(PasswordHashSupport.PBKDF2),
			find(panel, "lblAbout", JLabel.class).getText(),
			"and the line under the combo box describes what the model now holds");
	}

	@Test
	@DisplayName("the Hash button hashes the password and the algorithm the model holds")
	void hashesThePasswordAndTheAlgorithmTheModelHolds()
	{
		PasswordHashPanel panel = new PasswordHashPanel();
		find(panel, "cmbAlgorithm", JComboBox.class).setSelectedItem(PasswordHashSupport.PBKDF2);
		find(panel, "txtPassword", JPasswordField.class).setText(PASSWORD);

		assertArrayEquals(PASSWORD.toCharArray(), panel.getModelObject().getPassword(),
			"a password stays characters in the model, it does not become a string");

		find(panel, "btnHash", JButton.class).doClick();

		String hash = panel.getModelObject().getHash();
		assertEquals(PasswordHashSupport.PBKDF2, PasswordHashSupport.algorithmOf(hash),
			"the hash was made with the algorithm the model holds: " + panel.getResultText());
		assertTrue(PasswordHashSupport.verify(PASSWORD.toCharArray(), hash),
			"the hash is one of the typed password: " + panel.getResultText());
		assertEquals(hash, find(panel, "txtHash", JTextArea.class).getText(),
			"what the model holds is what the area shows");
		assertTrue(panel.getResultText().startsWith(PasswordHashSupport.PBKDF2 + " took "),
			"the message names the algorithm that was used, was: " + panel.getResultText());
		assertEquals(panel.getModelObject().getResultMessage(), panel.getResultText(),
			"the label shows what the model holds");
	}

	@Test
	@DisplayName("the Verify button checks the password of the model against its hash")
	void verifiesTheHashAgainstThePasswordTheBoundFieldHolds()
	{
		PasswordHashPanel panel = new PasswordHashPanel();
		find(panel, "cmbAlgorithm", JComboBox.class).setSelectedItem(PasswordHashSupport.PBKDF2);
		find(panel, "txtPassword", JPasswordField.class).setText(PASSWORD);
		find(panel, "btnHash", JButton.class).doClick();

		find(panel, "txtVerifyPassword", JPasswordField.class).setText(PASSWORD);
		assertArrayEquals(PASSWORD.toCharArray(), panel.getModelObject().getVerifyPassword(),
			"the password to check with is in the model before the button is pressed");

		find(panel, "btnVerify", JButton.class).doClick();
		assertEquals("matches (" + PasswordHashSupport.PBKDF2 + ")", panel.getResultText());

		find(panel, "txtVerifyPassword", JPasswordField.class).setText(OTHER_PASSWORD);
		find(panel, "btnVerify", JButton.class).doClick();
		assertEquals("does not match", panel.getResultText(),
			"the second verdict comes from the second password, not from the first");
	}

	@Test
	@DisplayName("a hash pasted into the area is verified, whatever the combo box shows")
	void verifiesAHashThatWasPastedIntoTheBoundArea() throws Exception
	{
		PasswordHashPanel panel = new PasswordHashPanel();
		String pasted = PasswordHashSupport.hash(PasswordHashSupport.PBKDF2,
			PASSWORD.toCharArray());

		find(panel, "txtHash", JTextArea.class).setText(pasted);
		assertEquals(pasted, panel.getModelObject().getHash(),
			"the pasted hash is in the model as it was pasted");
		find(panel, "txtVerifyPassword", JPasswordField.class).setText(PASSWORD);

		find(panel, "btnVerify", JButton.class).doClick();

		assertEquals("matches (" + PasswordHashSupport.PBKDF2 + ")", panel.getResultText(),
			"which algorithm made the hash is read out of the pasted value");
	}

	@Test
	@DisplayName("without a password and without a hash the buttons report it from the model")
	void refusesToHashWithoutAPasswordAndToVerifyWithoutAHash()
	{
		PasswordHashPanel panel = new PasswordHashPanel();

		find(panel, "btnVerify", JButton.class).doClick();
		assertEquals("hash a password first", panel.getResultText(),
			"an untouched area holds empty text, and that is what the button reads");

		find(panel, "btnHash", JButton.class).doClick();
		assertTrue(panel.getResultText().startsWith("not hashed: "),
			"an empty password is refused, was: " + panel.getResultText());
		assertEquals("", panel.getModelObject().getHash(),
			"and nothing was written into the hash");

		find(panel, "txtHash", JTextArea.class).setText("this is not a hash");
		find(panel, "btnVerify", JButton.class).doClick();
		assertEquals("this does not look like a hash any of these algorithms made",
			panel.getResultText(), "what the area holds is what the button judges");
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

	private static void assertHasTooltip(javax.swing.JComponent component, String fieldName)
	{
		String tooltip = component.getToolTipText();
		assertTrue(tooltip != null && !tooltip.isBlank(), fieldName + " must have a tooltip");
	}

	@Test
	void everyFieldExplainsItselfWithATooltip()
	{
		PasswordHashPanel panel = new PasswordHashPanel();

		assertHasTooltip(find(panel, "cmbAlgorithm", JComboBox.class), "algorithm");
		assertHasTooltip(find(panel, "txtPassword", javax.swing.JPasswordField.class), "password");
		assertHasTooltip(find(panel, "txtHash", JTextArea.class), "hash");
		assertHasTooltip(find(panel, "txtVerifyPassword", javax.swing.JPasswordField.class),
			"verify password");
		assertHasTooltip(find(panel, "btnHash", JButton.class), "hash button");
		assertHasTooltip(find(panel, "btnVerify", JButton.class), "verify button");
	}
}
