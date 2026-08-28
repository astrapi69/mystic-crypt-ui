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
package io.github.astrapi69.mystic.crypt.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JComboBox;
import javax.swing.UIManager;

import org.junit.jupiter.api.Test;

/**
 * Tests that the two combo boxes of {@link GeneralSettingsPanel} are bound to the
 * {@link MysticCryptSettings} the panel edits: a choice is in the settings the moment it is made,
 * and the look-and-feel switch takes the name from there.
 * <p>
 * The proof is what the choice did: the look and feel the {@link UIManager} reports afterwards, and
 * the language the settings carry into the next start.
 */
class GeneralSettingsPanelBindingTest
{

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

	/**
	 * Choosing a look and feel puts it into the settings and applies it - the switch reads the name
	 * from the settings, so the applied look and feel proves the choice arrived there
	 */
	@Test
	void choosingALookAndFeelLandsInTheSettingsAndIsApplied()
	{
		MysticCryptSettings settings = new MysticCryptSettings();
		GeneralSettingsPanel panel = new GeneralSettingsPanel(settings);
		JComboBox<?> comboBox = named(panel, "cmbLookAndFeel", JComboBox.class);
		assertNotNull(comboBox, "the look and feel combo box must keep its name");

		comboBox.setSelectedItem("Metal");

		assertEquals("Metal", settings.getLookAndFeel(),
			"the chosen look and feel did not reach the settings");
		assertEquals("Metal", UIManager.getLookAndFeel().getID(),
			"the look and feel of the settings was not applied");
	}

	/**
	 * Choosing a language puts it into the settings, which is the only place it is read from at the
	 * next start
	 */
	@Test
	void choosingALanguageLandsInTheSettings()
	{
		MysticCryptSettings settings = new MysticCryptSettings();
		GeneralSettingsPanel panel = new GeneralSettingsPanel(settings);
		JComboBox<?> comboBox = named(panel, "cmbLanguage", JComboBox.class);
		assertNotNull(comboBox, "the language combo box must keep its name");

		comboBox.setSelectedItem("de");
		assertEquals("de", settings.getLanguage(),
			"the chosen language did not reach the settings");

		comboBox.setSelectedItem("en");
		assertEquals("en", settings.getLanguage(),
			"the chosen language did not reach the settings");
	}

	/**
	 * The combo boxes start on what the settings hold, so the dialog shows the state the
	 * application is in
	 */
	@Test
	void theComboBoxesStartOnWhatTheSettingsHold()
	{
		MysticCryptSettings settings = new MysticCryptSettings();
		settings.setLookAndFeel("Metal");
		settings.setLanguage("de");

		GeneralSettingsPanel panel = new GeneralSettingsPanel(settings);

		assertEquals("Metal", named(panel, "cmbLookAndFeel", JComboBox.class).getSelectedItem(),
			"the look and feel combo box does not show what the settings hold");
		assertEquals("de", named(panel, "cmbLanguage", JComboBox.class).getSelectedItem(),
			"the language combo box does not show what the settings hold");
	}

	/**
	 * Building the panel must not switch the look and feel: the combo boxes are bound before their
	 * listener is added, so opening the settings dialog changes nothing by itself
	 */
	@Test
	void buildingThePanelDoesNotSwitchTheLookAndFeel()
	{
		GeneralSettingsPanel.applyLookAndFeel("Metal");
		String lookAndFeelBefore = UIManager.getLookAndFeel().getID();
		MysticCryptSettings settings = new MysticCryptSettings();
		settings.setLookAndFeel("Nimbus");

		new GeneralSettingsPanel(settings);

		assertEquals(lookAndFeelBefore, UIManager.getLookAndFeel().getID(),
			"building the settings panel must not switch the look and feel");
	}
}
