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
import io.github.astrapi69.swing.menu.enumeration.Anchor;

/**
 * Whether an anchor puts a submenu where it belongs is
 * {@link io.github.astrapi69.mystic.crypt.menu.PluginMenuOrderTest}'s job - a plain ordering
 * question, tested there without a display. This class is the Swing-level counterpart: does
 * {@link DesktopMenu#addPluginsMenu} actually build the {@code JMenu} tree that decision describes.
 */
class PluginMenuOrderTest
{

	private static PluginMenuContribution contributing(final String menuName)
	{
		return contributing(menuName, Anchor.LAST, null);
	}

	private static PluginMenuContribution contributing(final String menuName, final Anchor anchor,
		final String relativeToMenuId)
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

			@Override
			public Anchor getAnchor()
			{
				return anchor;
			}

			@Override
			public String getRelativeToMenuId()
			{
				return relativeToMenuId;
			}
		};
	}

	private static List<String> submenusFor(final List<String> asTheyWereFound)
	{
		return submenusForContributions(
			asTheyWereFound.stream().map(PluginMenuOrderTest::contributing).toList());
	}

	private static List<String> submenusForContributions(
		final List<PluginMenuContribution> contributions)
	{
		DesktopMenu menu = new DesktopMenu(new JFrame());
		JMenu pluginsMenu = menu.addPluginsMenu(contributions);
		return java.util.Arrays.stream(pluginsMenu.getMenuComponents())
			.filter(JMenu.class::isInstance).map(component -> ((JMenu)component).getText())
			.toList();
	}

	@Test
	@DisplayName("with no anchor declared, plugins are shown alphabetically, case-insensitively")
	void withNoAnchorDeclaredPluginsAreShownAlphabetically()
	{
		assertEquals(List.of("apple", "Banana", "cherry"),
			submenusFor(List.of("cherry", "Banana", "apple")),
			"the menu follows the order the plugins happened to be read in, not the alphabet");
	}

	@Test
	@DisplayName("a contribution without a name does not throw the sorting off")
	void aContributionWithoutANameDoesNotThrowTheSortingOff()
	{
		List<String> shown = submenusFor(java.util.Arrays.asList("Zeta", null, "Alpha"));

		assertEquals(List.of("Alpha", "Zeta"), shown,
			"a contribution with no submenu name has to be added directly, not sorted as a submenu");
	}

	@Test
	@DisplayName("a plugin's declared anchor is honoured in the JMenu the host actually builds")
	void aPluginsDeclaredAnchorIsHonouredInTheJMenuTheHostActuallyBuilds()
	{
		List<PluginMenuContribution> contributions = List.of(contributing("Checksum"),
			contributing("Obfuscation"), contributing("Zebra", Anchor.AFTER, "Checksum"));

		assertEquals(List.of("Checksum", "Zebra", "Obfuscation"),
			submenusForContributions(contributions),
			"DesktopMenu did not place the submenu where PluginMenuOrder decided it belongs");
	}

	@Test
	@DisplayName("a contribution that declares no anchor at all defaults to LAST with no target")
	void aContributionThatDeclaresNoAnchorAtAllDefaultsToLastWithNoTarget()
	{
		// neither getAnchor() nor getRelativeToMenuId() overridden - every existing plugin today
		PluginMenuContribution bare = new PluginMenuContribution()
		{
			@Override
			public String getMenuName()
			{
				return "Solo";
			}

			@Override
			public List<JMenuItem> getMenuItems()
			{
				return List.of(new JMenuItem("Solo"));
			}
		};

		assertEquals(Anchor.LAST, bare.getAnchor());
		assertEquals(null, bare.getRelativeToMenuId());
	}

}
