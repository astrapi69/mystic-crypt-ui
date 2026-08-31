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
package io.github.astrapi69.mystic.crypt.menu;

import java.util.ArrayList;
import java.util.List;

import io.github.astrapi69.swing.menu.build.MenuInfoExtensions;
import io.github.astrapi69.swing.menu.enumeration.Anchor;
import io.github.astrapi69.swing.menu.model.MenuInfo;

/**
 * Decides the order plugin submenus appear in, given each one's declared anchor.
 * <p>
 * No {@code JMenu}, no {@code JMenuItem}, no {@link io.github.astrapi69.mystic.crypt.DesktopMenu}
 * is built or touched here - only names, so this can be tested, and mutation-tested, without a
 * display. {@link io.github.astrapi69.mystic.crypt.DesktopMenu#addPluginsMenu} calls this to decide
 * where each plugin's submenu belongs, then does the Swing-side building itself.
 */
public final class PluginMenuOrder
{

	private PluginMenuOrder()
	{
	}

	/**
	 * One plugin's submenu, as far as ordering is concerned: its name, and where it wants to sit
	 * among its siblings
	 *
	 * @param name
	 *            the plugin's submenu name - what another plugin's {@code relativeToMenuId} names
	 *            to anchor against this one
	 * @param anchor
	 *            where this submenu wants to sit; {@link Anchor#LAST} is "no preference"
	 * @param relativeToMenuId
	 *            the name of the submenu this one is anchored to, used only when {@code anchor} is
	 *            {@link Anchor#BEFORE} or {@link Anchor#AFTER}
	 */
	public record Entry(String name, Anchor anchor, String relativeToMenuId) {
	}

	/**
	 * Orders the given entries by anchor.
	 * <p>
	 * {@link Anchor#FIRST}, {@link Anchor#BEFORE} and {@link Anchor#AFTER} move an entry to its
	 * declared place; every other entry - the default, {@link Anchor#LAST} - flows through in the
	 * order it was given. Entries fed in already sorted alphabetically therefore keep that order
	 * unless they ask for something else, which is what makes alphabetical order the fallback for a
	 * plugin that does not care, rather than the order plugins happened to be found in.
	 * <p>
	 * An anchor naming an entry that is not in the given list is not an error: that entry is placed
	 * as if it had not named a target.
	 *
	 * @param entries
	 *            the entries to order, normally pre-sorted alphabetically by name
	 * @return the entry names, in the order they belong in
	 */
	public static List<String> orderedNames(final List<Entry> entries)
	{
		List<MenuInfo> orderingKeys = entries.stream()
			.map(entry -> MenuInfo.builder().name(entry.name()).anchor(entry.anchor())
				.relativeToMenuId(entry.relativeToMenuId()).build())
			.toList();
		List<String> ordered = new ArrayList<>();
		for (MenuInfo info : MenuInfoExtensions.orderByAnchor(orderingKeys))
		{
			ordered.add(info.getName());
		}
		return ordered;
	}

}
