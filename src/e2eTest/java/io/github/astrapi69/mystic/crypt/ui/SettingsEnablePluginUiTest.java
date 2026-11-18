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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JTable;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Functional end-to-end test of the settings dialog's plugin management: a plugin can be disabled
 * and then enabled again through the UI, its state in the plugins table reflects each change, and a
 * disabled plugin stays out of the Plugins menu across a restart until it is enabled again
 */
class SettingsEnablePluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	/**
	 * The test plan's TC-0132: with two plugins installed, disabling one in the settings takes its
	 * entries out of the Plugins menu after a restart and leaves the other plugin's entries;
	 * enabling it again brings exactly the first menu back after the next restart
	 */
	@Test
	@DisplayName("a disabled plugin's menu entries are gone after a restart, and back after enabling it and another restart")
	void aDisabledPlugin_isGoneFromTheMenuAfterARestart_andBackOnceEnabled() throws Exception
	{
		installPluginRequiringItBuilt(OBFUSCATION_ZIP);
		installPluginRequiringItBuilt(CHECKSUM_ZIP);
		File databaseFile = new File(tempHome, "settings-plugin-restart.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		Set<String> withBoth = PluginMenuEntries.ofTheRunningApplication().keySet();
		assertFalse(withBoth.isEmpty(),
			"the precondition: the installed plugins have entries in the Plugins menu");

		switchTheObfuscationPlugin(application, "btnDisablePlugin");
		Set<String> rightAfterDisabling = PluginMenuEntries.ofTheRunningApplication().keySet();
		shutdownApplication();
		application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		Set<String> afterDisabling = PluginMenuEntries.ofTheRunningApplication().keySet();
		assertEquals(afterDisabling, rightAfterDisabling,
			"the menu loses the disabled plugin's entries at once, not only after the restart");

		Set<String> gone = new HashSet<>(withBoth);
		gone.removeAll(afterDisabling);
		assertTrue(withBoth.containsAll(afterDisabling),
			"disabling adds no entry: " + afterDisabling);
		assertFalse(gone.isEmpty(),
			"the disabled plugin's entries are gone after the restart: " + afterDisabling);
		assertFalse(afterDisabling.isEmpty(), "the other plugin's entries stay");
		assertEquals(1, topLevelEntriesOf(gone).size(),
			"what is gone belongs to one plugin's submenu: " + gone);

		switchTheObfuscationPlugin(application, "btnEnablePlugin");
		shutdownApplication();
		signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		assertEquals(withBoth, PluginMenuEntries.ofTheRunningApplication().keySet(),
			"enabled again, its entries are back after the restart");
	}

	private void switchTheObfuscationPlugin(final ApplicationSteps application,
		final String buttonName)
	{
		DialogFixture settings = application.openSettingsDialog();
		JTable table = settings.table("tblPlugins").target();
		GuiActionRunner.execute(() -> {
			int row = findObfuscationRow(table);
			assertTrue(row >= 0, "the installed obfuscation plugin must be listed");
			table.setRowSelectionInterval(row, row);
			settings.button(buttonName).target().doClick();
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> settings.button("btnCloseSettings").target().doClick());
		robot.waitForIdle();
	}

	private static Set<String> topLevelEntriesOf(final Set<String> paths)
	{
		return paths.stream().map(path -> {
			String[] parts = path.split(" > ");
			return parts.length > 1 ? parts[0] + " > " + parts[1] : path;
		}).collect(Collectors.toSet());
	}

	@Test
	void aPluginCanBeDisabledThenEnabledAgain() throws Exception
	{
		installPluginRequiringItBuilt(OBFUSCATION_ZIP);

		File databaseFile = new File(tempHome, "settings-enable-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		DialogFixture settings = application.openSettingsDialog();
		JTable table = settings.table("tblPlugins").target();

		int row = GuiActionRunner.execute(() -> findObfuscationRow(table));
		assertTrue(row >= 0, "the installed obfuscation plugin must be listed");

		GuiActionRunner.execute(() -> {
			table.setRowSelectionInterval(row, row);
			settings.button("btnDisablePlugin").target().doClick();
		});
		robot.waitForIdle();
		assertEquals("DISABLED", stateOfObfuscation(table),
			"disabling must set the plugin state to DISABLED");

		GuiActionRunner.execute(() -> {
			int disabledRow = findObfuscationRow(table);
			table.setRowSelectionInterval(disabledRow, disabledRow);
			settings.button("btnEnablePlugin").target().doClick();
		});
		robot.waitForIdle();
		assertEquals("STARTED", stateOfObfuscation(table),
			"enabling must start the plugin again (state STARTED)");

		GuiActionRunner.execute(() -> settings.button("btnCloseSettings").target().doClick());
		robot.waitForIdle();
	}

	private static String stateOfObfuscation(JTable table)
	{
		int row = findObfuscationRow(table);
		return row < 0 ? "" : String.valueOf(table.getValueAt(row, 2));
	}

	private static int findObfuscationRow(JTable table)
	{
		for (int row = 0; row < table.getRowCount(); row++)
		{
			Object id = table.getValueAt(row, 0);
			if (id != null && id.toString().toLowerCase().contains("obfuscation"))
			{
				return row;
			}
		}
		return -1;
	}
}
