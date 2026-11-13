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
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JTable;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.settings.PluginSettingsPanel;

/**
 * The description an installed plugin gives for one of its settings reaches the row of that setting
 * in the Plugin settings tab (#886).
 * <p>
 * Every plugin explains its keys through {@code PluginSettingsContribution.getDescription}, and the
 * host never asked for the explanation: the table showed one general tooltip over every row. The
 * description is read from the plugin the application loaded, not written into this test, so the
 * test follows the plugin's own wording.
 */
class APluginSettingShowsItsDescriptionUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("the Plugin settings tab shows an installed plugin's description of a setting over its row (#886)")
	void theDescriptionShowsOverTheRowOfItsSetting() throws Exception
	{
		installPluginRequiringItBuilt(OBFUSCATION_ZIP);
		File databaseFile = new File(tempHome, "plugin-settings.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		DialogFixture settings = application.openSettingsDialog();
		application.selectSettingsTab(settings, "Plugin settings");
		settings.list("lstPluginSettings").selectItem("Obfuscation");
		robot.waitForIdle();

		PluginSettingsPanel panel = robot.finder().findByType(settings.target(),
			PluginSettingsPanel.class);
		JTable table = settings.table("tblPluginSettings").target();
		String key = GuiActionRunner.execute(() -> String.valueOf(table.getValueAt(0, 0)));
		String description = GuiActionRunner
			.execute(() -> panel.getSelectedContribution().getDescription(key));
		assertNotNull(description, "the obfuscation plugin describes its setting " + key);

		String tooltip = GuiActionRunner.execute(() -> {
			Rectangle cell = table.getCellRect(0, 1, true);
			return table.getToolTipText(
				new MouseEvent(table, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0,
					(int)cell.getCenterX(), (int)cell.getCenterY(), 0, false));
		});

		assertEquals(description, tooltip,
			"what the plugin wrote about " + key + " is what the user reads over its row");

		GuiActionRunner.execute(() -> settings.button("btnCloseSettings").target().doClick());
		robot.waitForIdle();
	}
}
