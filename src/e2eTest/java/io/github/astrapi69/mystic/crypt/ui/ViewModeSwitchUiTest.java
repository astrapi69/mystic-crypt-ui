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
import java.io.IOException;

import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.settings.MysticCryptSettings;
import io.github.astrapi69.swing.enumeration.FrameMode;

/**
 * End-to-end use case "choose the view": the view is a setting now, not a menu item. Choosing it in
 * the settings dialog puts the frame into that view at once, and the application opens in it the
 * next time - which is the whole point of moving it out of the menu.
 */
class ViewModeSwitchUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("the view chosen in the settings is the view the frame is in")
	void theViewChosenInTheSettingsIsTheViewTheFrameIsIn() throws IOException
	{
		File databaseFile = new File(tempHome, "viewmode-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		application.chooseViewMode(FrameMode.DESKTOP_PANE);
		assertEquals(FrameMode.DESKTOP_PANE,
			MysticCryptApplicationFrame.getInstance().getFrameMode(),
			"choosing the desktop view in the settings did not put the frame into it");

		application.chooseViewMode(FrameMode.APPLICATION_PANEL);
		assertEquals(FrameMode.APPLICATION_PANEL,
			MysticCryptApplicationFrame.getInstance().getFrameMode(),
			"choosing the panel view in the settings did not put the frame into it");
	}

	@Test
	@DisplayName("the chosen view is the view the application opens in next time")
	void theChosenViewIsTheViewTheApplicationOpensInNextTime() throws Exception
	{
		File databaseFile = new File(tempHome, "viewmode-restart.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		signInWithExistingDatabase(databaseFile, MASTER_PASSWORD)
			.chooseViewMode(FrameMode.DESKTOP_PANE);
		File configurationDirectory = new File(tempHome, ".config/mystic-crypt-ui");
		assertEquals(FrameMode.DESKTOP_PANE,
			MysticCryptSettings.load(configurationDirectory).getViewMode(),
			"closing the settings dialog did not write the chosen view down");

		shutdownApplication();
		signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		assertEquals(FrameMode.DESKTOP_PANE,
			MysticCryptApplicationFrame.getInstance().getFrameMode(),
			"the application did not open in the view that was chosen before");
	}

	@Test
	@DisplayName("opening a plugin tool keeps the database view reachable, and closing Settings restores Panel view")
	void openingAPluginToolKeepsTheDatabaseViewReachableAndClosingSettingsRestoresPanelView()
		throws IOException
	{
		installPluginRequiringItBuilt(CHECKSUM_ZIP);
		File databaseFile = new File(tempHome, "viewmode-stuck-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.openPluginTool("Verify Checksum", "Verify Checksum");
		assertEquals(FrameMode.DESKTOP_PANE,
			MysticCryptApplicationFrame.getInstance().getFrameMode(),
			"opening a plugin tool switches the frame to Desktop mode to have somewhere to put "
				+ "its window - the premise this regression test builds on (#132)");
		assertTrue(application.isInternalFrameShowing("Key database"),
			"a plugin switching the frame to Desktop mode must not make the database view "
				+ "disappear - it has to keep showing as its own internal frame there (#132)");

		DialogFixture settings = application.openSettingsDialog();
		settings.button("btnCloseSettings").click();

		assertEquals(FrameMode.APPLICATION_PANEL,
			MysticCryptApplicationFrame.getInstance().getFrameMode(),
			"closing Settings without touching the view combo must still restore Panel view - the "
				+ "persisted setting never changed, so the combo fires no event, and this is the "
				+ "only way back (#132)");
	}

}
