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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.menu.MenuLayoutSupport;

/**
 * A saved arrangement of the plugins menu has to survive a plugin being disabled and re-enabled,
 * which rebuilds the menu from scratch and used to fall back to the alphabetical default - silently
 * discarding what the user had arranged (#95).
 */
class PluginMenuOrderSurvivesPluginToggleUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("a saved plugins menu order survives disabling and re-enabling a plugin")
	void aSavedPluginsMenuOrderSurvivesDisablingAndReenablingAPlugin() throws Exception
	{
		installPluginRequiringItBuilt(OBFUSCATION_ZIP);
		installPluginRequiringItBuilt(CHECKSUM_ZIP);

		File databaseFile = new File(tempHome, "plugin-menu-order-toggle.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();

		saveARearrangedPluginsMenu();
		List<String> arranged = pluginSubmenuNames();

		DialogFixture settings = application.openSettingsDialog();
		JTable table = settings.table("tblPlugins").target();
		int row = GuiActionRunner.execute(() -> rowNamed(table, "obfuscation"));
		assertTrue(row >= 0, "the installed obfuscation plugin must be listed");
		GuiActionRunner.execute(() -> {
			table.setRowSelectionInterval(row, row);
			settings.button("btnDisablePlugin").target().doClick();
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> {
			int disabledRow = rowNamed(table, "obfuscation");
			table.setRowSelectionInterval(disabledRow, disabledRow);
			settings.button("btnEnablePlugin").target().doClick();
		});
		robot.waitForIdle();
		assertEquals(arranged, pluginSubmenuNames(),
			"the arrangement was lost after the first disable/enable cycle");

		// a second cycle: if any step still reads the menu bar through a reference the first
		// setJMenuBar replaced, this one operates on the menu the user no longer sees
		GuiActionRunner.execute(() -> {
			int obfuscationRow = rowNamed(table, "obfuscation");
			table.setRowSelectionInterval(obfuscationRow, obfuscationRow);
			settings.button("btnDisablePlugin").target().doClick();
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> {
			int disabledRow = rowNamed(table, "obfuscation");
			table.setRowSelectionInterval(disabledRow, disabledRow);
			settings.button("btnEnablePlugin").target().doClick();
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> settings.button("btnCloseSettings").target().doClick());
		robot.waitForIdle();

		assertEquals(arranged, pluginSubmenuNames(),
			"the arrangement was lost after the second disable/enable cycle");
	}

	/**
	 * The test plan's TC-0131: the arranged order of the plugin submenus survives installing a
	 * plugin's zip again through Settings > Install from Zip... and a restart of the application
	 */
	@Test
	@DisplayName("a saved plugins menu order is the same after installing a plugin's zip again and after a restart")
	void aSavedPluginsMenuOrder_isTheSameAfterAReinstallAndARestart() throws Exception
	{
		installPluginRequiringItBuilt(OBFUSCATION_ZIP);
		installPluginRequiringItBuilt(CHECKSUM_ZIP);
		File databaseFile = new File(tempHome, "plugin-menu-order-reinstall.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		List<String> alphabetical = pluginSubmenuNames();
		saveARearrangedPluginsMenu();
		List<String> arranged = pluginSubmenuNames();
		assertNotEquals(alphabetical, arranged,
			"the precondition: the saved arrangement is not the one the menu starts with");

		installFromZipInTheSettings(application, OBFUSCATION_ZIP);
		assertEquals(arranged, pluginSubmenuNames(),
			"the arrangement was lost by installing the plugin's zip again");

		shutdownApplication();
		signInWithExistingDatabase(databaseFile, MASTER_PASSWORD).showMainFrame();
		assertEquals(arranged, pluginSubmenuNames(),
			"the arrangement was lost across the restart after the reinstall");
	}

	/** Saves an arrangement of the plugins menu that is not the alphabetical one and applies it */
	private void saveARearrangedPluginsMenu() throws java.io.IOException
	{
		String rearrangedXml = GuiActionRunner.execute(() -> {
			JMenuBar menuBar = MysticCryptApplicationFrame.getInstance().getJMenuBar();
			JMenu pluginsMenu = findMenu(menuBar, "global.menu.plugins");
			pluginsMenu.add(pluginsMenu.getMenuComponent(0));
			return MenuLayoutSupport.exportXml(menuBar);
		});
		MenuLayoutSupport.save(rearrangedXml, new File(tempHome, ".config/mystic-crypt-ui"));
		GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().applyPersistedMenuLayout());
		robot.waitForIdle();
	}

	/** Chooses the zip in Settings > Install from Zip... and closes the settings again */
	private void installFromZipInTheSettings(final ApplicationSteps application, final Path zip)
	{
		DialogFixture settings = application.openSettingsDialog();
		SwingUtilities.invokeLater(() -> settings.button("btnInstallPlugin").target().doClick());
		JFileChooser fileChooser = JFileChooserFinder.findFileChooser()
			.withTimeout(10, TimeUnit.SECONDS).using(robot).target();
		File zipFile = zip.toFile().getAbsoluteFile();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(zipFile);
			fileChooser.approveSelection();
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> settings.button("btnCloseSettings").target().doClick());
		robot.waitForIdle();
	}

	private List<String> pluginSubmenuNames()
	{
		return GuiActionRunner.execute(() -> {
			JMenuBar menuBar = MysticCryptApplicationFrame.getInstance().getJMenuBar();
			JMenu pluginsMenu = findMenu(menuBar, "global.menu.plugins");
			return java.util.Arrays.stream(pluginsMenu.getMenuComponents())
				.filter(JMenu.class::isInstance).map(component -> ((JMenu)component).getText())
				.toList();
		});
	}

	private static JMenu findMenu(final javax.swing.MenuElement element, final String name)
	{
		for (javax.swing.MenuElement child : element.getSubElements())
		{
			if (child instanceof JMenu candidate)
			{
				if (name.equals(candidate.getName()))
				{
					return candidate;
				}
				JMenu found = findMenu(candidate, name);
				if (found != null)
				{
					return found;
				}
			}
			else
			{
				JMenu found = findMenu(child, name);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private static int rowNamed(final JTable table, final String pluginId)
	{
		for (int row = 0; row < table.getRowCount(); row++)
		{
			if (String.valueOf(table.getValueAt(row, 0)).toLowerCase().contains(pluginId))
			{
				return row;
			}
		}
		return -1;
	}

}
