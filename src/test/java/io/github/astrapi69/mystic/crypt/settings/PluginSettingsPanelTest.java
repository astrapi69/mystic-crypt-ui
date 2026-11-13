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

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;

import org.junit.jupiter.api.DisplayName;
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

		@Override
		public String getDescription(final String key)
		{
			// a description for one key only, so the other one shows what a key without one shows
			return "first".equals(key) ? FIRST_DESCRIPTION : null;
		}
	}

	/** What the test plugin says about its key "first" */
	static final String FIRST_DESCRIPTION = "how many times the first thing happens";

	/** The tooltip the table has for a setting no description comes with */
	static final String TABLE_TOOLTIP = "only the value column is editable - the setting names are fixed by the plugin";

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

	/** The tooltip the table shows when the mouse rests on the row of this setting */
	private static String tooltipOverTheRowOf(final PluginSettingsPanel panel, final String key)
	{
		JTable table = component(panel, "tblPluginSettings", JTable.class);
		for (int row = 0; row < table.getRowCount(); row++)
		{
			if (key.equals(table.getValueAt(row, 0)))
			{
				Rectangle cell = table.getCellRect(row, 1, true);
				MouseEvent overTheCell = new MouseEvent(table, MouseEvent.MOUSE_MOVED,
					System.currentTimeMillis(), 0, (int)cell.getCenterX(), (int)cell.getCenterY(),
					0, false);
				return table.getToolTipText(overTheCell);
			}
		}
		throw new AssertionError("the table shows no setting " + key);
	}

	private static <T extends JComponent> T component(final JComponent parent, final String name,
		final Class<T> type)
	{
		if (name.equals(parent.getName()) && type.isInstance(parent))
		{
			return type.cast(parent);
		}
		for (java.awt.Component child : parent.getComponents())
		{
			if (child instanceof JComponent childComponent)
			{
				T found = component(childComponent, name, type);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	@Test
	@DisplayName("the description a plugin gives for a setting is not shown, its row has the table's tooltip (#886, measured)")
	void theDescriptionOfASettingIsNotShown(@TempDir File configurationDirectory)
	{
		PluginSettingsPanel panel = new PluginSettingsPanel(configurationDirectory,
			List.of(new TestContribution()));

		assertEquals(TABLE_TOOLTIP, tooltipOverTheRowOf(panel, "first"),
			"today nobody asks the plugin for its description");
		assertEquals(TABLE_TOOLTIP, tooltipOverTheRowOf(panel, "second"));
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
