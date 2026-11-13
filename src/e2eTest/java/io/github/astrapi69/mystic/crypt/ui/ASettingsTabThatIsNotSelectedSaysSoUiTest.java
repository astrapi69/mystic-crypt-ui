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
 */
class ASettingsTabThatIsNotSelectedSaysSoUiTest extends AbstractUiTest
{

	@Test
	@DisplayName("a click on a settings tab that changes nothing goes by without a word (#962, measured)")
	void aClickThatChangesNothingGoesBySilently() throws Exception
	{
		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().cancel();
		awaitApplicationInitialized();
		DialogFixture settings = new ApplicationSteps(robot).openSettingsDialog();
		settings.tabbedPane("tabSettings").selectTab("General");

		settings.tabbedPane("tabSettings").selectTab("Plugins");

		assertEquals("General", selectedTab(settings),
			"the disabled Plugins tab was clicked, General is still selected, and nothing said so");
	}

	private static String selectedTab(final DialogFixture settings)
	{
		JTabbedPane tabs = settings.tabbedPane("tabSettings").target();
		return GuiActionRunner.execute(() -> tabs.getTitleAt(tabs.getSelectedIndex()));
	}
}
