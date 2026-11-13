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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import javax.swing.JLabel;
import javax.swing.JTable;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * The "Plugin settings" tab explains the selected key (#886): every plugin writes a sentence per
 * key through {@code getDescription}, and the tab used to show keys and values only, so a user
 * editing {@code default.algorithm} or {@code buffer.max.lines} saw no word about what the value
 * does
 */
class PluginSettingDescriptionUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("selecting a setting shows the sentence its plugin wrote for it")
	void selectingASetting_showsItsDescription() throws Exception
	{
		installPluginRequiringItBuilt(OBFUSCATION_ZIP);

		File databaseFile = new File(tempHome, "setting-description-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		DialogFixture settings = application.openSettingsDialog();
		settings.tabbedPane("tabSettings").selectTab("Plugin settings");
		robot.waitForIdle();

		JTable table = settings.table("tblPluginSettings").target();
		assertTrue(GuiActionRunner.execute(() -> table.getRowCount()) > 0,
			"the installed plugin brings settings rows");
		GuiActionRunner.execute(() -> table.setRowSelectionInterval(0, 0));
		robot.waitForIdle();

		JLabel description = (JLabel)settings.label("lblPluginSettingDescription").target();
		String shown = GuiActionRunner.execute(() -> description.getText());
		assertFalse(shown == null || shown.isBlank(),
			"the sentence the plugin wrote for the selected key is shown below the table");

		GuiActionRunner.execute(() -> settings.button("btnCloseSettings").target().doClick());
		robot.waitForIdle();
	}
}
