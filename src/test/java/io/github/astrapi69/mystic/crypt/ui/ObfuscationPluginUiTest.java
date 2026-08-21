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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;

/**
 * End-to-end proof that the internal obfuscation plugin is loaded from its packaged zip and
 * contributes its tools to the host's "Plugins" menu - the definitive integration test for
 * "obfuscation as a plugin".
 * <p>
 * The test drops the pre-built plugin zip into the (temp, isolated) config plugins directory before
 * launching the app, signs in, and asserts the "Plugins" menu now carries both obfuscation tools.
 * It requires the plugin zip to be built first (e.g. {@code make plugin-obfuscation}); when the zip
 * is absent - as on a plain {@code ./gradlew test} that did not build the plugin - the test skips
 * instead of failing
 */
class ObfuscationPluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "obfuscation-plugin-db-pw-123";
	private static final Path PLUGIN_ZIP = Path
		.of("plugins/obfuscation-plugin/build/plugin-dist/obfuscation-plugin-1.0.0.zip");

	@Test
	void obfuscationPluginLoadsFromZipAndContributesItsMenuItems() throws Exception
	{
		Assumptions.assumeTrue(Files.exists(PLUGIN_ZIP),
			"obfuscation plugin zip not built - run 'make plugin-obfuscation' first");

		// place the plugin into the app's (isolated, per-test) config plugins directory before the
		// app starts, so its DefaultPluginManager discovers and loads it during initialization
		File pluginsDir = new File(tempHome, ".config/mystic-crypt-ui/plugins");
		assertTrue(pluginsDir.mkdirs() || pluginsDir.isDirectory());
		Files.copy(PLUGIN_ZIP, pluginsDir.toPath().resolve(PLUGIN_ZIP.getFileName()),
			StandardCopyOption.REPLACE_EXISTING);

		File databaseFile = new File(tempHome, "obfuscation-plugin-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		List<String> pluginMenuItemTexts = readPluginMenuItemTexts();
		assertTrue(pluginMenuItemTexts.contains("Simple Obfuscation"),
			"the loaded plugin must contribute a 'Simple Obfuscation' menu item, found: "
				+ pluginMenuItemTexts);
		assertTrue(pluginMenuItemTexts.contains("Operated Obfuscation"),
			"the loaded plugin must contribute an 'Operated Obfuscation' menu item, found: "
				+ pluginMenuItemTexts);
	}

	/** Reads the texts of all items under the frame's "Plugins" menu, on the EDT */
	private List<String> readPluginMenuItemTexts()
	{
		return GuiActionRunner.execute(() -> {
			List<String> texts = new ArrayList<>();
			JMenuBar menuBar = MysticCryptApplicationFrame.getInstance().getJMenuBar();
			if (menuBar == null)
			{
				return texts;
			}
			for (int i = 0; i < menuBar.getMenuCount(); i++)
			{
				JMenu menu = menuBar.getMenu(i);
				if (menu != null && "Plugins".equals(menu.getText()))
				{
					for (MenuElement element : menu.getSubElements())
					{
						collectItemTexts(element, texts);
					}
				}
			}
			return texts;
		});
	}

	private void collectItemTexts(MenuElement element, List<String> texts)
	{
		if (element instanceof JMenuItem menuItem)
		{
			texts.add(menuItem.getText());
		}
		for (MenuElement child : element.getSubElements())
		{
			collectItemTexts(child, texts);
		}
	}
}
