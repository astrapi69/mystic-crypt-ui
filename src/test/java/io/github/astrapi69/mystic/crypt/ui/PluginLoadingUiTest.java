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
 * End-to-end proof that the internal plugins are loaded from their packaged zips and contribute
 * their tools to the host's "Plugins" menu - the definitive integration test for the "feature as a
 * plugin" approach (obfuscation and checksum).
 * <p>
 * The test drops the pre-built plugin zips into the (temp, isolated) config plugins directory
 * before launching the app, signs in, and asserts the "Plugins" menu carries every contributed
 * tool. It requires the plugin zips to be built first (e.g. {@code make plugin-obfuscation} and
 * {@code make plugin-checksum}); each plugin is skipped individually when its zip is absent - as on
 * a plain {@code ./gradlew test} that did not build the plugins - and the whole test skips when
 * none are present
 */
class PluginLoadingUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "plugin-loading-db-pw-123";

	private static final Path OBFUSCATION_ZIP = Path
		.of("plugins/obfuscation-plugin/build/plugin-dist/obfuscation-plugin-1.0.0.zip");
	private static final Path CHECKSUM_ZIP = Path
		.of("plugins/checksum-plugin/build/plugin-dist/checksum-plugin-1.0.0.zip");
	private static final Path CONVERSION_ZIP = Path
		.of("plugins/conversion-plugin/build/plugin-dist/conversion-plugin-1.0.0.zip");
	private static final Path CONSOLE_ZIP = Path
		.of("plugins/console-plugin/build/plugin-dist/console-plugin-1.0.0.zip");

	@Test
	void internalPluginsLoadFromZipAndContributeTheirMenuItems() throws Exception
	{
		boolean obfuscationBuilt = Files.exists(OBFUSCATION_ZIP);
		boolean checksumBuilt = Files.exists(CHECKSUM_ZIP);
		boolean conversionBuilt = Files.exists(CONVERSION_ZIP);
		boolean consoleBuilt = Files.exists(CONSOLE_ZIP);
		Assumptions.assumeTrue(obfuscationBuilt || checksumBuilt || conversionBuilt || consoleBuilt,
			"no plugin zips built - run 'make plugins' first");

		// place the plugins into the app's (isolated, per-test) config plugins directory before the
		// app starts, so its DefaultPluginManager discovers and loads them during initialization
		File pluginsDir = new File(tempHome, ".config/mystic-crypt-ui/plugins");
		assertTrue(pluginsDir.mkdirs() || pluginsDir.isDirectory());
		if (obfuscationBuilt)
		{
			installPlugin(pluginsDir, OBFUSCATION_ZIP);
		}
		if (checksumBuilt)
		{
			installPlugin(pluginsDir, CHECKSUM_ZIP);
		}
		if (conversionBuilt)
		{
			installPlugin(pluginsDir, CONVERSION_ZIP);
		}
		if (consoleBuilt)
		{
			installPlugin(pluginsDir, CONSOLE_ZIP);
		}

		File databaseFile = new File(tempHome, "plugin-loading-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		List<String> pluginMenuItemTexts = readPluginMenuItemTexts();
		if (obfuscationBuilt)
		{
			assertTrue(pluginMenuItemTexts.contains("Simple Obfuscation"),
				"the obfuscation plugin must contribute 'Simple Obfuscation', found: "
					+ pluginMenuItemTexts);
			assertTrue(pluginMenuItemTexts.contains("Operated Obfuscation"),
				"the obfuscation plugin must contribute 'Operated Obfuscation', found: "
					+ pluginMenuItemTexts);
		}
		if (checksumBuilt)
		{
			assertTrue(pluginMenuItemTexts.contains("Verify Checksum"),
				"the checksum plugin must contribute 'Verify Checksum', found: "
					+ pluginMenuItemTexts);
		}
		if (conversionBuilt)
		{
			assertTrue(pluginMenuItemTexts.contains("Convert DER to PEM"),
				"the conversion plugin must contribute 'Convert DER to PEM', found: "
					+ pluginMenuItemTexts);
		}
		if (consoleBuilt)
		{
			assertTrue(pluginMenuItemTexts.contains("Console"),
				"the console plugin must contribute 'Console', found: " + pluginMenuItemTexts);
		}
	}

	private static void installPlugin(File pluginsDir, Path zip) throws Exception
	{
		Files.copy(zip, pluginsDir.toPath().resolve(zip.getFileName()),
			StandardCopyOption.REPLACE_EXISTING);
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
