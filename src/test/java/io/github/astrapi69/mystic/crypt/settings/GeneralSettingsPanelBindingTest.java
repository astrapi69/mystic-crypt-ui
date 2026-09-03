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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.formdev.flatlaf.FlatDarkLaf;

import io.github.astrapi69.swing.enumeration.FrameMode;

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
	 * A FlatLaf theme is not a JDK-bundled look and feel, but once
	 * {@link FlatLafTheme#installAll()} has registered it with {@link UIManager} it is
	 * indistinguishable from one to this panel: found in the dropdown, written into the settings
	 * the moment it is chosen, and applied from there (#125)
	 */
	@Test
	@DisplayName("a registered FlatLaf theme cooperates with the settings model like any other look and feel")
	void aRegisteredFlatLafThemeCanBeChosenPersistedAndApplied()
	{
		FlatLafTheme.installAll();
		MysticCryptSettings settings = new MysticCryptSettings();
		GeneralSettingsPanel panel = new GeneralSettingsPanel(settings);
		JComboBox<?> comboBox = named(panel, "cmbLookAndFeel", JComboBox.class);

		comboBox.setSelectedItem("FlatLaf Dark");

		assertEquals("FlatLaf Dark", settings.getLookAndFeel(),
			"the chosen FlatLaf theme did not reach the settings");
		assertEquals(FlatDarkLaf.class, UIManager.getLookAndFeel().getClass(),
			"the settings' FlatLaf theme was not applied, the same way a JDK-bundled one is");
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

	@Test
	@DisplayName("the chosen view lands in the settings")
	void theChosenViewLandsInTheSettings()
	{
		MysticCryptSettings settings = new MysticCryptSettings();
		GeneralSettingsPanel panel = new GeneralSettingsPanel(settings);

		named(panel, "cmbViewMode", JComboBox.class).setSelectedItem(FrameMode.DESKTOP_PANE);

		assertEquals(FrameMode.DESKTOP_PANE, settings.getViewMode(),
			"what was chosen in the box did not reach the settings");
	}

	@Test
	@DisplayName("the view box starts on what the settings hold")
	void theViewBoxStartsOnWhatTheSettingsHold()
	{
		MysticCryptSettings settings = new MysticCryptSettings();
		settings.setViewMode(FrameMode.DESKTOP_PANE);

		GeneralSettingsPanel panel = new GeneralSettingsPanel(settings);

		assertEquals(FrameMode.DESKTOP_PANE,
			named(panel, "cmbViewMode", JComboBox.class).getSelectedItem(),
			"the box does not show the view the settings already hold");
	}

	@Test
	@DisplayName("building the panel does not switch the view")
	void buildingThePanelDoesNotSwitchTheView()
	{
		MysticCryptSettings settings = new MysticCryptSettings();
		settings.setViewMode(FrameMode.DESKTOP_PANE);

		// binding selects what the settings hold, which fires an action event; nothing may act on
		// it here - there is no application frame in a headless test, and the settings dialog can
		// be opened before signing in
		assertDoesNotThrow(() -> new GeneralSettingsPanel(settings));
	}

	/**
	 * Unchecking "tooltips" puts that into the settings and applies it live - the switch reads the
	 * flag from the settings, so {@link ToolTipManager} being disabled afterwards proves the choice
	 * arrived there (#170)
	 */
	@Test
	@DisplayName("unchecking tooltips lands in the settings and is applied")
	void togglingTooltipsLandsInTheSettingsAndIsApplied()
	{
		ToolTipManager.sharedInstance().setEnabled(true);
		MysticCryptSettings settings = new MysticCryptSettings();
		GeneralSettingsPanel panel = new GeneralSettingsPanel(settings);
		JCheckBox checkBox = named(panel, "chkTooltipsEnabled", JCheckBox.class);
		assertNotNull(checkBox, "the tooltips checkbox must keep its name");

		checkBox.doClick();

		assertFalse(settings.isTooltipsEnabled(), "unchecking tooltips did not reach the settings");
		assertFalse(ToolTipManager.sharedInstance().isEnabled(),
			"unchecking tooltips must disable them application-wide");

		checkBox.doClick();

		assertTrue(settings.isTooltipsEnabled(), "checking tooltips did not reach the settings");
		assertTrue(ToolTipManager.sharedInstance().isEnabled(),
			"checking tooltips must enable them application-wide again");
	}

	@Test
	@DisplayName("the tooltips checkbox starts on what the settings hold")
	void theTooltipsCheckboxStartsOnWhatTheSettingsHold()
	{
		MysticCryptSettings settings = new MysticCryptSettings();
		settings.setTooltipsEnabled(false);

		GeneralSettingsPanel panel = new GeneralSettingsPanel(settings);

		assertFalse(named(panel, "chkTooltipsEnabled", JCheckBox.class).isSelected(),
			"the checkbox does not show what the settings hold");
	}

	@Test
	@DisplayName("building the panel does not toggle tooltips")
	void buildingThePanelDoesNotToggleTooltips()
	{
		ToolTipManager.sharedInstance().setEnabled(true);
		MysticCryptSettings settings = new MysticCryptSettings();
		settings.setTooltipsEnabled(false);

		// binding selects what the settings hold, which fires an action event; nothing may act on
		// it here, the same way building the panel must not switch the look and feel
		assertDoesNotThrow(() -> new GeneralSettingsPanel(settings));
		assertTrue(ToolTipManager.sharedInstance().isEnabled(),
			"building the settings panel must not change the shared ToolTipManager");
	}

}
