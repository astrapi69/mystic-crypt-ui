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
package io.github.astrapi69.mystic.crypt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginMenuContribution;

/**
 * The plugin manager hands its contributions over in the order it reads the plugin directory, so
 * reinstalling a plugin used to rearrange the Plugins menu for no reason the user could see. The
 * menu sorts them by name instead.
 */
class PluginMenuOrderTest
{

	private static PluginMenuContribution contributing(final String menuName)
	{
		return new PluginMenuContribution()
		{
			@Override
			public String getMenuName()
			{
				return menuName;
			}

			@Override
			public List<JMenuItem> getMenuItems()
			{
				return List.of(new JMenuItem(menuName));
			}
		};
	}

	private List<String> submenusFor(final List<String> asTheyWereFound)
	{
		DesktopMenu menu = new DesktopMenu(new JFrame());
		JMenu pluginsMenu = menu.addPluginsMenu(
			asTheyWereFound.stream().map(PluginMenuOrderTest::contributing).toList());
		return java.util.Arrays.stream(pluginsMenu.getMenuComponents())
			.filter(JMenu.class::isInstance).map(component -> ((JMenu)component).getText())
			.toList();
	}

	@Test
	@DisplayName("the plugins are shown in the same order however they were found")
	void thePluginsAreShownInTheSameOrderHoweverTheyWereFound()
	{
		assertEquals(List.of("Certificate", "Checksum", "Key Generation", "Obfuscation"),
			submenusFor(List.of("Obfuscation", "Checksum", "Key Generation", "Certificate")),
			"the menu follows the order the plugins happened to be read in");
	}

	@Test
	@DisplayName("the order does not depend on the case of the names")
	void theOrderDoesNotDependOnTheCaseOfTheNames()
	{
		assertEquals(List.of("apple", "Banana", "cherry"),
			submenusFor(List.of("cherry", "Banana", "apple")),
			"a lower case name was sorted behind every upper case one");
	}

	@Test
	@DisplayName("a contribution without a name does not throw the sorting off")
	void aContributionWithoutANameDoesNotThrowTheSortingOff()
	{
		List<String> shown = submenusFor(java.util.Arrays.asList("Zeta", null, "Alpha"));

		assertEquals(List.of("Alpha", "Zeta"), shown,
			"a contribution with no submenu name has to be added directly, not sorted as a submenu");
	}

}
