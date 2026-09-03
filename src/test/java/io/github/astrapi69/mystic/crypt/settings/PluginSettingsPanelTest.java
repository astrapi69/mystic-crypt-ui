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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;

/**
 * Tests of the "Plugin settings" tab. Swing components can be built and driven without a display,
 * so this stays a plain unit test: the buttons are pressed the way a user would press them and the
 * result is checked on disk.
 */
class PluginSettingsPanelTest
{

	static class TestContribution implements PluginSettingsContribution
	{
		@Override
		public String getPluginId()
		{
			return "test-plugin";
		}

		@Override
		public String getDisplayName()
		{
			return "Test Plugin";
		}

		@Override
		public Map<String, String> getDefaults()
		{
			Map<String, String> defaults = new LinkedHashMap<>();
			defaults.put("first", "1");
			defaults.put("second", "two");
			return defaults;
		}
	}

	/** A contribution that fails the way a plugin with a missing class would */
	static class BrokenContribution implements PluginSettingsContribution
	{
		@Override
		public String getPluginId()
		{
			throw new IllegalStateException("this plugin is broken");
		}

		@Override
		public Map<String, String> getDefaults()
		{
			return Map.of();
		}
	}

	/** A contribution that declares nothing, so there is nothing to edit */
	static class EmptyContribution implements PluginSettingsContribution
	{
		@Override
		public String getPluginId()
		{
			return "empty-plugin";
		}

		@Override
		public Map<String, String> getDefaults()
		{
			return Map.of();
		}
	}

	private static JButton button(JComponent component, String name)
	{
		if (name.equals(component.getName()) && component instanceof JButton found)
		{
			return found;
		}
		for (java.awt.Component child : component.getComponents())
		{
			if (child instanceof JComponent childComponent)
			{
				JButton found = button(childComponent, name);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	@Test
	void showsTheDeclaredSettingsOfTheFirstPlugin(@TempDir File configurationDirectory)
	{
		PluginSettingsPanel panel = new PluginSettingsPanel(configurationDirectory,
			List.of(new TestContribution()));

		assertEquals(Map.of("first", "1", "second", "two"), panel.getShownValues());
		assertEquals("Test Plugin", panel.getSelectedContribution().getDisplayName());
	}

	@Test
	void applyingWritesWhatWasEdited(@TempDir File configurationDirectory)
	{
		PluginSettingsPanel panel = new PluginSettingsPanel(configurationDirectory,
			List.of(new TestContribution()));

		panel.setShownValue("second", "edited");
		button(panel, "btnApplyPluginSettings").doClick();

		assertEquals("edited",
			PluginSettings
				.load(configurationDirectory, "test-plugin", new TestContribution().getDefaults())
				.get("second"),
			"what the user typed must be what the plugin reads, message was: "
				+ panel.getResultText());
	}

	@Test
	void resettingBringsTheDefaultsBack(@TempDir File configurationDirectory)
	{
		PluginSettingsPanel panel = new PluginSettingsPanel(configurationDirectory,
			List.of(new TestContribution()));
		panel.setShownValue("second", "edited");
		button(panel, "btnApplyPluginSettings").doClick();

		button(panel, "btnResetPluginSettings").doClick();

		assertEquals("two", panel.getShownValues().get("second"),
			"the table must show the defaults again");
		assertEquals("two",
			PluginSettings
				.load(configurationDirectory, "test-plugin", new TestContribution().getDefaults())
				.get("second"),
			"the stored file must be gone");
	}

	@Test
	void withoutAnyContributionTheTabSaysSo(@TempDir File configurationDirectory)
	{
		PluginSettingsPanel panel = new PluginSettingsPanel(configurationDirectory, List.of());

		assertNull(panel.getSelectedContribution());
		assertTrue(panel.getShownValues().isEmpty());
		assertEquals("no plugin brings its own settings", panel.getResultText());
		// pressing the buttons with nothing selected must not fail
		button(panel, "btnApplyPluginSettings").doClick();
		button(panel, "btnResetPluginSettings").doClick();
	}

	@Test
	void nullInsteadOfAListIsNotAnError(@TempDir File configurationDirectory)
	{
		assertNotNull(new PluginSettingsPanel(configurationDirectory, null));
	}

	@Test
	void aBrokenOrEmptyContributionIsLeftOut()
	{
		List<PluginSettingsContribution> usable = PluginSettingsPanel.usable(
			List.of(new BrokenContribution(), new EmptyContribution(), new TestContribution()));

		assertEquals(1, usable.size(),
			"only the contribution that declares settings and does not fail may be shown");
		assertEquals("test-plugin", usable.get(0).getPluginId());
	}

	@Test
	void noContributionsAtAllIsNotAnError()
	{
		assertTrue(PluginSettingsPanel.usable(null).isEmpty());
		assertTrue(PluginSettingsPanel.usable(List.of()).isEmpty());
	}

	@Test
	void thePluginIdIsTheDisplayNameWhenNoneIsGiven()
	{
		assertEquals("empty-plugin", new EmptyContribution().getDisplayName());
		assertNull(new EmptyContribution().getDescription("first"),
			"a plugin need not explain its settings");
	}

	private static JComponent componentNamed(JComponent component, String name)
	{
		if (name.equals(component.getName()))
		{
			return component;
		}
		for (java.awt.Component child : component.getComponents())
		{
			if (child instanceof JComponent childComponent)
			{
				JComponent found = componentNamed(childComponent, name);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private static void assertHasTooltip(JComponent component, String fieldName)
	{
		String tooltip = component.getToolTipText();
		assertTrue(tooltip != null && !tooltip.isBlank(), fieldName + " must have a tooltip");
	}

	/**
	 * Only the value column is editable, and "Reset to defaults" deletes the stored settings file -
	 * neither is obvious from the table or the button label alone (#163)
	 */
	@Test
	void theTableAndBothButtonsExplainThemselvesWithATooltip(@TempDir File configurationDirectory)
	{
		PluginSettingsPanel panel = new PluginSettingsPanel(configurationDirectory,
			List.of(new TestContribution()));

		assertHasTooltip(componentNamed(panel, "tblPluginSettings"), "settings table");
		assertHasTooltip(button(panel, "btnApplyPluginSettings"), "apply button");
		assertHasTooltip(button(panel, "btnResetPluginSettings"), "reset button");
	}
}
