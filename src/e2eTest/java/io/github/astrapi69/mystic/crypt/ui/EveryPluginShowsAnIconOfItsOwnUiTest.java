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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Every plugin under {@code plugins/} shows an icon of its own on its menu items (#1192): each item
 * a user clicks in a plugin's submenu of the "Plugins" menu carries a 16 px icon with a 32 px
 * variant for a scaled screen, and no two plugins show the same picture.
 * <p>
 * The plugins are read from the directory and installed from their built zips, the way a user
 * installs them, so a new plugin is measured here by existing, and its icon is looked up through
 * the plugin's own class loader, where a missing resource would show. The menu designer is asked
 * for, since it is a plugin under {@code plugins/} too.
 */
@ExtendWith(MenuDesignerShown.class)
class EveryPluginShowsAnIconOfItsOwnUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final int ICON_SIZE = 16;

	private static final int SCALED_ICON_SIZE = 32;

	@Test
	@DisplayName("every plugin under plugins/ shows an icon of its own on every menu item")
	void everyPluginShowsAnIconOfItsOwnOnEveryMenuItem() throws Exception
	{
		List<String> installed = installEveryPluginRequiringItBuilt(Set.of());
		File databaseFile = new File(tempHome, "plugin-icons.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		signInWithExistingDatabase(databaseFile, MASTER_PASSWORD).showMainFrame();

		Map<String, List<ItemIcon>> iconsByPlugin = iconsByPlugin();
		List<String> withoutAMenu = installed.stream().filter(plugin -> !bringsAMenu(plugin))
			.toList();
		assertEquals(installed.size() - withoutAMenu.size(), iconsByPlugin.size(),
			"every plugin under plugins/ with a menu contribution must have its submenu in the "
				+ "Plugins menu, installed: " + installed + ", without a menu (their entry action "
				+ "carries the icon, WifiQrPluginUiTest): " + withoutAMenu + ", submenus: "
				+ iconsByPlugin.keySet());

		List<String> defects = new ArrayList<>();
		iconsByPlugin.values().forEach(
			itemIcons -> itemIcons.forEach(itemIcon -> defectOf(itemIcon).ifPresent(defects::add)));
		assertEquals(List.of(), defects, "every plugin menu item must carry the plugin's icon");

		assertEquals(Map.of(), sharedPictures(iconsByPlugin),
			"no two plugins may show the same picture");
	}

	/**
	 * Whether a plugin brings a menu: one of its classes implements {@code PluginMenuContribution}.
	 * A plugin without one offers only an action on an entry (#1120), which has no submenu here
	 */
	private static boolean bringsAMenu(final String plugin)
	{
		Pattern implementing = Pattern
			.compile("\\bclass\\s+\\w+\\s+implements\\s+[^{]*\\bPluginMenuContribution\\b");
		try (Stream<Path> sources = Files.walk(Path.of("plugins", plugin, "src/main/java")))
		{
			return sources.filter(path -> path.toString().endsWith(".java")).anyMatch(path -> {
				try
				{
					return implementing.matcher(Files.readString(path)).find();
				}
				catch (IOException exception)
				{
					throw new UncheckedIOException(exception);
				}
			});
		}
		catch (IOException exception)
		{
			throw new UncheckedIOException(exception);
		}
	}

	/** A menu item of a plugin's submenu with the icon it shows */
	private record ItemIcon(String plugin, String item, Icon icon) {
		String where()
		{
			return "'" + item + "' in the plugin menu '" + plugin + "'";
		}
	}

	private static Map<String, List<ItemIcon>> iconsByPlugin()
	{
		Map<String, List<JMenuItem>> itemsByPlugin = PluginMenuEntries.clickableItemsByPlugin();
		return GuiActionRunner.execute(() -> {
			Map<String, List<ItemIcon>> iconsByPlugin = new LinkedHashMap<>();
			itemsByPlugin.forEach((plugin, items) -> iconsByPlugin.put(plugin, items.stream()
				.map(item -> new ItemIcon(plugin, item.getText(), item.getIcon())).toList()));
			return iconsByPlugin;
		});
	}

	/**
	 * What is wrong with an item's icon, if anything: missing, not 16 px, or without the 32 px
	 * variant a scaled screen draws
	 */
	private static Optional<String> defectOf(final ItemIcon itemIcon)
	{
		Icon icon = itemIcon.icon();
		if (icon == null)
		{
			return Optional.of(itemIcon.where() + " has no icon");
		}
		if (icon.getIconWidth() != ICON_SIZE || icon.getIconHeight() != ICON_SIZE)
		{
			return Optional.of(itemIcon.where() + " has an icon of " + icon.getIconWidth() + " x "
				+ icon.getIconHeight() + " px instead of " + ICON_SIZE + " x " + ICON_SIZE);
		}
		List<String> sizes = variantSizes(icon);
		if (!sizes.contains(SCALED_ICON_SIZE + " x " + SCALED_ICON_SIZE))
		{
			return Optional.of(itemIcon.where() + " has no " + SCALED_ICON_SIZE
				+ " px variant for a scaled screen, its sizes are " + sizes);
		}
		return Optional.empty();
	}

	private static List<String> variantSizes(final Icon icon)
	{
		if (icon instanceof ImageIcon imageIcon
			&& imageIcon.getImage()instanceof MultiResolutionImage images)
		{
			return images.getResolutionVariants().stream()
				.map(variant -> variant.getWidth(null) + " x " + variant.getHeight(null)).toList();
		}
		return List.of(icon.getIconWidth() + " x " + icon.getIconHeight());
	}

	/**
	 * The pictures more than one plugin shows, each with the plugins that show it; a plugin is
	 * represented by the 16 px pixels of its first item's icon
	 */
	private static Map<String, List<String>> sharedPictures(
		final Map<String, List<ItemIcon>> iconsByPlugin)
	{
		Map<String, List<String>> pluginsByPicture = new TreeMap<>();
		iconsByPlugin.forEach((plugin, itemIcons) -> itemIcons.stream().findFirst()
			.map(ItemIcon::icon).filter(ImageIcon.class::isInstance).map(ImageIcon.class::cast)
			.ifPresent(icon -> pluginsByPicture
				.computeIfAbsent(pixelsOf(icon.getImage()), picture -> new ArrayList<>())
				.add(plugin)));
		pluginsByPicture.values().removeIf(plugins -> plugins.size() < 2);
		Map<String, List<String>> shared = new TreeMap<>();
		pluginsByPicture.values().forEach(plugins -> shared.put(plugins.get(0), plugins));
		return shared;
	}

	private static String pixelsOf(final Image image)
	{
		BufferedImage canvas = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = canvas.createGraphics();
		graphics.drawImage(image, 0, 0, ICON_SIZE, ICON_SIZE, null);
		graphics.dispose();
		return Arrays.toString(canvas.getRGB(0, 0, ICON_SIZE, ICON_SIZE, null, 0, ICON_SIZE));
	}
}
