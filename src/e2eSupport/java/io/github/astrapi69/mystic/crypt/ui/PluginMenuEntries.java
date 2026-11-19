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
package io.github.astrapi69.mystic.crypt.ui;

import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.assertj.swing.edt.GuiActionRunner;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;

/**
 * The entries of the running application's Plugins menu, the way the end-to-end tests compare them
 */
final class PluginMenuEntries
{

	private PluginMenuEntries()
	{
	}

	/**
	 * Every entry under the Plugins menu, as its path of texts, and whether it is enabled
	 *
	 * @return the entries in menu order
	 */
	static Map<String, Boolean> ofTheRunningApplication()
	{
		return GuiActionRunner.execute(() -> {
			Map<String, Boolean> entries = new LinkedHashMap<>();
			JMenuBar menuBar = MysticCryptApplicationFrame.getInstance().getJMenuBar();
			for (int index = 0; index < menuBar.getMenuCount(); index++)
			{
				JMenu menu = menuBar.getMenu(index);
				if (menu != null && "Plugins".equals(menu.getText()))
				{
					for (Component child : menu.getMenuComponents())
					{
						collect(child, "Plugins", entries);
					}
				}
			}
			return entries;
		});
	}

	private static void collect(final Component component, final String parentPath,
		final Map<String, Boolean> entries)
	{
		if (!(component instanceof JMenuItem item))
		{
			return;
		}
		String path = parentPath + " > " + item.getText();
		entries.put(path, item.isEnabled());
		if (item instanceof JMenu submenu)
		{
			for (Component child : submenu.getMenuComponents())
			{
				collect(child, path, entries);
			}
		}
	}
}
