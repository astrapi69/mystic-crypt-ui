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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;

import javax.swing.JTable;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.Test;

/**
 * Functional end-to-end test of the settings dialog: with a plugin installed, opening File ->
 * Settings must show the "Plugins" and "General" tabs, list the installed plugin in the plugins
 * table and let it be disabled (the table's state column then reflects the change)
 */
class SettingsUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "settings-pw-123";

	@Test
	void settingsDialogListsPluginsAndCanDisableThem() throws Exception
	{
		installPluginRequiringItBuilt(OBFUSCATION_ZIP);

		File databaseFile = new File(tempHome, "settings-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		DialogFixture settings = application.openSettingsDialog();

		String[] tabTitles = settings.tabbedPane("tabSettings").tabTitles();
		assertTrue(Arrays.asList(tabTitles).contains("Plugins"),
			"the settings dialog must have a Plugins tab");
		assertTrue(Arrays.asList(tabTitles).contains("General"),
			"the settings dialog must have a General tab");

		JTable table = settings.table("tblPlugins").target();
		int obfuscationRow = GuiActionRunner.execute(() -> findObfuscationRow(table));
		assertTrue(obfuscationRow >= 0, "the installed obfuscation plugin must be listed");

		// disable the obfuscation plugin through its button
		GuiActionRunner.execute(() -> {
			table.setRowSelectionInterval(obfuscationRow, obfuscationRow);
			settings.button("btnDisablePlugin").target().doClick();
		});
		robot.waitForIdle();

		String state = GuiActionRunner.execute(() -> {
			int row = findObfuscationRow(table);
			return row < 0 ? "" : String.valueOf(table.getValueAt(row, 2));
		});
		assertEquals("DISABLED", state,
			"disabling the plugin must update its state in the plugins table");

		GuiActionRunner.execute(() -> settings.button("btnCloseSettings").target().doClick());
		robot.waitForIdle();
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
