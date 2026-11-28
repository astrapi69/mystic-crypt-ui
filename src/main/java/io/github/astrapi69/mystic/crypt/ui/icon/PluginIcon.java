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

import java.awt.Component;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import lombok.extern.java.Log;

/**
 * The icon of an internal plugin, in the style of the application icon (#1192, the sheet of
 * 2026-11-26): a 16 px image for the menu, with the 32 px image as the variant a scaled screen
 * draws instead. The sources and how the images were made are in {@code design/plugin-icons/}.
 * <p>
 * Each plugin carries its icon as a resource of its own jar, {@code icon/<plugin>-16.png} and
 * {@code icon/<plugin>-32.png}, where {@code <plugin>} is its directory under {@code plugins/}, and
 * sets it on its menu items in {@code getMenuItems()}:
 *
 * <pre>
 * return PluginIcon.onEvery(List.of(verifyChecksum, checksumAndMac), getClass(),
 * 	"checksum-plugin");
 * </pre>
 *
 * The icon is read through the class loader of the class that is passed, so a plugin's own jar is
 * searched. A missing or unreadable image leaves the items without an icon and is logged: the host
 * asks every plugin for its items inside one guard, and a plugin whose items failed for want of an
 * image would vanish from the menu. {@code EveryPluginCarriesItsIconTest} keeps the images there.
 */
@Log
public final class PluginIcon
{

	/** The sizes a plugin icon is drawn in, in pixels, smallest first */
	public static final List<Integer> SIZES = List.of(16, 32);

	private PluginIcon()
	{
	}

	/**
	 * The class path resource of a plugin's icon in the given size
	 *
	 * @param plugin
	 *            the plugin's directory name under {@code plugins/}, for example
	 *            {@code checksum-plugin}
	 * @param size
	 *            one of {@link #SIZES}
	 * @return for example {@code icon/checksum-plugin-16.png}
	 */
	public static String resource(final String plugin, final int size)
	{
		return "icon/" + plugin + "-" + size + ".png";
	}

	/**
	 * The plugin's icon: the 16 px image, with the 32 px image as its variant for a scaled screen
	 *
	 * @param owner
	 *            a class of the plugin, whose class loader finds the plugin's resources
	 * @param plugin
	 *            the plugin's directory name under {@code plugins/}
	 * @return the icon, or null when one of the two images is missing or not readable, which is
	 *         logged
	 */
	public static Icon of(final Class<?> owner, final String plugin)
	{
		return null;
	}

	/**
	 * Sets the plugin's icon on every one of its menu items, the items of a submenu among them
	 *
	 * @param items
	 *            the menu items the plugin contributes
	 * @param owner
	 *            a class of the plugin, whose class loader finds the plugin's resources
	 * @param plugin
	 *            the plugin's directory name under {@code plugins/}
	 * @return the same items, for {@code return} in {@code getMenuItems()}
	 */
	public static List<JMenuItem> onEvery(final List<JMenuItem> items, final Class<?> owner,
		final String plugin)
	{
		return items;
	}

	private static void setOnItemAndSubmenu(final JMenuItem item, final Icon icon)
	{
		item.setIcon(icon);
		if (item instanceof JMenu submenu)
		{
			for (Component child : submenu.getMenuComponents())
			{
				if (child instanceof JMenuItem childItem)
				{
					setOnItemAndSubmenu(childItem, icon);
				}
			}
		}
	}

	private static Image read(final Class<?> owner, final String resource)
	{
		try (InputStream stream = owner.getClassLoader().getResourceAsStream(resource))
		{
			if (stream == null)
			{
				log.log(Level.WARNING,
					"the plugin icon " + resource + " is not on the class path of "
						+ owner.getName() + "; its menu items stay without an icon");
				return null;
			}
			BufferedImage image = ImageIO.read(stream);
			if (image == null)
			{
				log.log(Level.WARNING, "the plugin icon " + resource
					+ " is not an image ImageIO can read; its menu items stay without an icon");
			}
			return image;
		}
		catch (IOException exception)
		{
			log.log(Level.WARNING, "the plugin icon " + resource
				+ " could not be read; its menu items stay without an icon", exception);
			return null;
		}
	}
}
