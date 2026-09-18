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

import javax.swing.JTable;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Functional end-to-end test of the settings dialog's plugin management: a plugin can be disabled
 * and then enabled again through the UI, and its state in the plugins table reflects each change
 */
class SettingsEnablePluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

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
