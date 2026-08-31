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
 * The plugin manager hands its contributions over in the order it reads the plugin directory, so
 * reinstalling a plugin used to rearrange the Plugins menu for no reason the user could see.
 * Alphabetical order is the fallback for a plugin that does not care; a plugin that does can anchor
 * itself before, after, or first relative to another plugin's menu name, the way IntelliJ lets a
 * platform action declare its place among its siblings.
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

	private List<String> submenusFor(final List<String> asTheyWereFound)
	{
		return submenusForContributions(
			asTheyWereFound.stream().map(PluginMenuOrderTest::contributing).toList());
	}

	private List<String> submenusForContributions(final List<PluginMenuContribution> contributions)
	{
		DesktopMenu menu = new DesktopMenu(new JFrame());
		JMenu pluginsMenu = menu.addPluginsMenu(contributions);
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

	@Test
	@DisplayName("a plugin anchored after another one lands right after it")
	void aPluginAnchoredAfterAnotherOneLandsRightAfterIt()
	{
		List<PluginMenuContribution> contributions = List.of(contributing("Checksum"),
			contributing("Obfuscation"), contributing("Zebra", Anchor.AFTER, "Checksum"));

		List<String> shown = submenusForContributions(contributions);

		assertEquals(List.of("Checksum", "Zebra", "Obfuscation"), shown,
			"the anchored plugin did not land right after the one it named");
	}

	@Test
	@DisplayName("a plugin anchored before another one lands right before it")
	void aPluginAnchoredBeforeAnotherOneLandsRightBeforeIt()
	{
		List<PluginMenuContribution> contributions = List.of(contributing("Checksum"),
			contributing("Obfuscation"), contributing("Aardvark", Anchor.BEFORE, "Obfuscation"));

		List<String> shown = submenusForContributions(contributions);

		assertEquals(List.of("Checksum", "Aardvark", "Obfuscation"), shown,
			"the anchored plugin did not land right before the one it named");
	}

	@Test
	@DisplayName("a plugin anchored first overrides the alphabetical position")
	void aPluginAnchoredFirstOverridesTheAlphabeticalPosition()
	{
		List<PluginMenuContribution> contributions = List.of(contributing("Checksum"),
			contributing("Obfuscation"), contributing("Zebra", Anchor.FIRST, null));

		List<String> shown = submenusForContributions(contributions);

		assertEquals(List.of("Zebra", "Checksum", "Obfuscation"), shown,
			"first must win over the alphabetical order every unanchored plugin still follows");
	}

	@Test
	@DisplayName("an anchor naming a plugin that is not installed does not break the menu")
	void anAnchorNamingAPluginThatIsNotInstalledDoesNotBreakTheMenu()
	{
		List<PluginMenuContribution> contributions = List.of(contributing("Checksum"),
			contributing("Zebra", Anchor.AFTER, "Not Installed"));

		List<String> shown = submenusForContributions(contributions);

		assertEquals(List.of("Checksum", "Zebra"), shown,
			"an anchor to an absent plugin must not throw or drop the item");
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
