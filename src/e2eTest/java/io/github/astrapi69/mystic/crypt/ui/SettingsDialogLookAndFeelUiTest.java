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

import java.io.File;

import javax.swing.JMenuItem;
import javax.swing.UIManager;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.formdev.flatlaf.FlatDarkLaf;

import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.settings.FlatLafTheme;
import io.github.astrapi69.mystic.crypt.settings.MysticCryptSettings;

/**
 * The settings dialog and the look the screen shows: after a look is chosen in View > Look and
 * Feel, the dialog must show that look, and closing it must not write another one down
 */
class SettingsDialogLookAndFeelUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("after a look is chosen in the menu, the settings dialog shows it and keeps it")
	void afterALookIsChosenInTheMenu_theSettingsDialogShowsItAndKeepsIt() throws Exception
	{
		File databaseFile = new File(tempHome, "look-dialog-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		JMenuItem darkItem = robot.finder().findByName(FlatLafTheme.DARK.menuItemName(),
			JMenuItem.class, false);
		GuiActionRunner.execute(() -> darkItem.doClick());
		robot.waitForIdle();
		assertEquals(FlatDarkLaf.class, UIManager.getLookAndFeel().getClass(),
			"the menu did not switch the screen to FlatLaf Dark");

		DialogFixture settings = application.openSettingsDialog();
		application.selectSettingsTab(settings, "General");
		Object shown = GuiActionRunner
			.execute(() -> settings.comboBox("cmbLookAndFeel").target().getSelectedItem());
		settings.button("btnCloseSettings").click();
		robot.waitForIdle();

		File configurationDirectory = new File(tempHome, ".config/mystic-crypt-ui");
		assertEquals("FlatLaf Dark", shown,
			"the dialog shows another look than the one on the screen");
		assertEquals("FlatLaf Dark",
			MysticCryptSettings.load(configurationDirectory).getLookAndFeel(),
			"closing the dialog wrote the old look back");
	}
}
