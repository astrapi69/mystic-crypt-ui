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

import javax.swing.UIManager;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.formdev.flatlaf.FlatDarkLaf;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * End-to-end use case "choose the look": the look chosen in the settings dialog is the look the
 * application is in at once and the one it starts with next time. There is no language setting
 * since #885, so the look is all that is chosen there.
 */
class LookAndFeelSurvivesRestartUiTest extends AbstractUiTest
{

	@Test
	@DisplayName("the look chosen in the settings is still active after a restart")
	void theLookChosenInTheSettingsIsStillActiveAfterARestart() throws Exception
	{
		String masterPassword = TestPasswords.throwaway();
		File databaseFile = new File(tempHome, "look-restart.mcrdb");
		createDatabaseFileHeadless(databaseFile, masterPassword);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, masterPassword);

		DialogFixture settings = application.openSettingsDialog();
		application.selectSettingsTab(settings, "General");
		GuiActionRunner.execute(
			() -> settings.comboBox("cmbLookAndFeel").target().setSelectedItem("FlatLaf Dark"));
		robot.waitForIdle();
		settings.button("btnCloseSettings").click();
		robot.waitForIdle();

		shutdownApplication();
		launchApplication();

		String activeLookAndFeelClass = GuiActionRunner
			.execute(() -> UIManager.getLookAndFeel().getClass().getName());
		assertEquals(FlatDarkLaf.class.getName(), activeLookAndFeelClass,
			"the look chosen in the settings was not the look after the restart");
	}

}
