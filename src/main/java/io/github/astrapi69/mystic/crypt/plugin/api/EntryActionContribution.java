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
package io.github.astrapi69.mystic.crypt.plugin.api;

import java.awt.Component;

import javax.swing.Icon;

import org.pf4j.ExtensionPoint;

import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;

/**
 * An action a plugin offers on one entry of the vault, in the context menu of the entry table
 * (#1120). The host reaches a plugin's action only in the three limits the maintainer set on
 * 2026-11-24: it hands over the one entry the user picked, only when the user clicks the action,
 * and as a copy. A plugin never sees any other entry through this, and changing the copy changes
 * nothing in the vault.
 * <p>
 * The host shows the action with the icon {@link #getIcon()} returns and enables it only while
 * exactly one entry is selected.
 */
public interface EntryActionContribution extends ExtensionPoint
{

	/**
	 * The text of the action's menu item, in the user's language
	 *
	 * @return the text
	 */
	String getActionName();

	/**
	 * The plugin's icon, the same a plugin with a menu sets on its menu items (#1192): its
	 * {@code icon-16.png} and {@code icon-32.png}, read next to the contribution class, as one
	 * {@code BaseMultiResolutionImage}
	 *
	 * @return the icon the host shows on the action's menu item
	 */
	Icon getIcon();

	/**
	 * Runs the action on the event thread, at the user's click
	 *
	 * @param entry
	 *            a copy of the entry the user picked, sharing no array with the vault's own; the
	 *            plugin overwrites the secrets it holds once it no longer needs them
	 * @param parent
	 *            the component the action was chosen in, to place a window over
	 */
	void act(MysticCryptEntryModelBean entry, Component parent);
}
