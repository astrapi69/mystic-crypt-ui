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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;

import javax.swing.JButton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pf4j.DefaultPluginManager;

/**
 * "Install from Zip..." runs a zip file with the same access as the application, which is worth a
 * warning-style tooltip - and none of the five buttons of {@link PluginsSettingsPanel} explained
 * themselves at all (#163)
 */
class PluginsSettingsPanelTooltipTest
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

	private static void assertHasTooltip(Component component, String fieldName)
	{
		String tooltip = ((JButton)component).getToolTipText();
		assertTrue(tooltip != null && !tooltip.isBlank(), fieldName + " must have a tooltip");
	}

	@Test
	void everyButtonExplainsItselfWithATooltip(@TempDir File pluginsRoot)
	{
		PluginsSettingsPanel panel = new PluginsSettingsPanel(
			new DefaultPluginManager(pluginsRoot.toPath()), () -> {
			});

		assertHasTooltip(named(panel, "btnEnablePlugin", JButton.class), "enable button");
		assertHasTooltip(named(panel, "btnDisablePlugin", JButton.class), "disable button");
		assertHasTooltip(named(panel, "btnInstallPlugin", JButton.class), "install button");
		assertHasTooltip(named(panel, "btnOpenPluginsFolder", JButton.class),
			"open plugins folder button");
		assertHasTooltip(named(panel, "btnRefreshPlugins", JButton.class), "refresh button");
	}
}
