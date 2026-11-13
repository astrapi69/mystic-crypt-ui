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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JTabbedPane;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The step that picks a tab of the settings dialog says when the tab it clicked is not the one
 * selected (#962).
 * <p>
 * Once in CI the click on the General tab changed nothing: the Plugins tab stayed selected, and the
 * lookup of the view mode box that followed failed with a hierarchy dump that named neither tab.
 * That click could not be made to fail again here (30 rounds of choosing the view under CPU load, 0
 * failures), so the test uses a click that changes nothing for certain: the Plugins tab is disabled
 * while no database is open.
 * <p>
 * Now the step waits for the selection the click should have made, and fails with the title of the
 * selected tab when the click changed nothing.
 */
class ASettingsTabThatIsNotSelectedSaysSoUiTest extends AbstractUiTest
{

	@Test
	@DisplayName("a click on a settings tab that changes nothing fails with the tab that is selected (#962)")
	void aClickThatChangesNothingFailsWithTheSelectedTab() throws Exception
	{
		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().cancel();
		awaitApplicationInitialized();
		ApplicationSteps application = new ApplicationSteps(robot);
		DialogFixture settings = application.openSettingsDialog();
		application.selectSettingsTab(settings, "General");

		AssertionError failure = assertThrows(AssertionError.class,
			() -> application.selectSettingsTab(settings, "Plugins"));

		assertTrue(
			failure.getMessage().contains("'Plugins'")
				&& failure.getMessage().contains("the selected one is 'General'"),
			"the failure names the tab clicked and the tab selected: " + failure.getMessage());
		assertEquals("General", selectedTab(settings));
	}

	@Test
	@DisplayName("a click on a settings tab that takes it goes on with that tab selected (#962)")
	void aClickThatTakesTheTabGoesOn() throws Exception
	{
		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().cancel();
		awaitApplicationInitialized();
		ApplicationSteps application = new ApplicationSteps(robot);
		DialogFixture settings = application.openSettingsDialog();

		application.selectSettingsTab(settings, "Plugin settings");
		assertEquals("Plugin settings", selectedTab(settings));
		application.selectSettingsTab(settings, "General");
		assertEquals("General", selectedTab(settings));
	}

	private static String selectedTab(final DialogFixture settings)
	{
		JTabbedPane tabs = settings.tabbedPane("tabSettings").target();
		return GuiActionRunner.execute(() -> tabs.getTitleAt(tabs.getSelectedIndex()));
	}
}
