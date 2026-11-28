/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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
package io.github.astrapi69.mystic.crypt.ui.icon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.image.MultiResolutionImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A plugin's icon (#1192): read from the plugin's own resources in 16 and 32 px, set on every menu
 * item the plugin contributes, and left out without an exception when an image is missing, so the
 * plugin's items still reach the menu. The images under {@code src/test/resources/icon/} stand in
 * for a plugin's: {@code test-plugin} is complete, {@code half-plugin} lacks its 32 px image and
 * {@code broken-plugin}'s 16 px image is not a PNG.
 */
class PluginIconTest
{

	@Test
	@DisplayName("the resource of a plugin icon is icon/<plugin>-<size>.png")
	void theResource()
	{
		assertEquals("icon/checksum-plugin-16.png", PluginIcon.resource("checksum-plugin", 16));
		assertEquals("icon/checksum-plugin-32.png", PluginIcon.resource("checksum-plugin", 32));
	}

	@Test
	@DisplayName("the icon is 16 px wide and carries the 16 and 32 px images for a scaled screen")
	void theIconCarriesBothSizes()
	{
		Icon icon = PluginIcon.of(PluginIconTest.class, "test-plugin");

		assertEquals(16, icon.getIconWidth());
		assertEquals(16, icon.getIconHeight());
		MultiResolutionImage image = assertInstanceOf(MultiResolutionImage.class,
			((ImageIcon)icon).getImage());
		assertEquals(List.of(16, 32),
			image.getResolutionVariants().stream().map(variant -> variant.getWidth(null)).toList());
	}

	@Test
	@DisplayName("every item gets the same icon, and the list that comes back is the one passed in")
	void everyItemGetsTheIcon()
	{
		JMenuItem first = new JMenuItem("first");
		JMenuItem second = new JMenuItem("second");
		List<JMenuItem> items = List.of(first, second);

		List<JMenuItem> returned = PluginIcon.onEvery(items, PluginIconTest.class, "test-plugin");

		assertSame(items, returned);
		assertEquals(16, first.getIcon().getIconWidth());
		assertSame(first.getIcon(), second.getIcon());
	}

	@Test
	@DisplayName("the items of a submenu get the icon too, as lethenon's 'For Operators' under 'More ...'")
	void theItemsOfASubmenuGetTheIcon()
	{
		JMenuItem nested = new JMenuItem("For Operators");
		JMenu more = new JMenu("More ...");
		more.add(nested);

		PluginIcon.onEvery(List.of(more), PluginIconTest.class, "test-plugin");

		assertEquals(16, more.getIcon().getIconWidth());
		assertSame(more.getIcon(), nested.getIcon());
	}

	@Test
	@DisplayName("a plugin without items gets an empty list back")
	void noItems()
	{
		List<JMenuItem> none = new ArrayList<>();

		assertSame(none, PluginIcon.onEvery(none, PluginIconTest.class, "test-plugin"));
	}

	@ParameterizedTest(name = "{0}: no icon and no exception, the items still come back")
	@ValueSource(strings = { "absent-plugin", "half-plugin", "broken-plugin" })
	void aMissingOrUnreadableImageLeavesTheItemsWithoutAnIcon(final String plugin)
	{
		JMenuItem item = new JMenuItem("item");

		assertNull(PluginIcon.of(PluginIconTest.class, plugin));
		assertEquals(List.of(item),
			PluginIcon.onEvery(List.of(item), PluginIconTest.class, plugin));
		assertNull(item.getIcon());
	}
}
