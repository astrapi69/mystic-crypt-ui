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

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;

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
 * selected tab when the click changed nothing. Since the lost click came back once in CI after
 * #1074 and #1131 and could not be reproduced here (70 rounds), the failure also says where the
 * dialog was placed before the click, and where the dialog, the tab and the pointer were and which
 * window was active right after it and when the step gave up.
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
	@DisplayName("a click on a settings tab that changes nothing says where the dialog, the tab and the pointer were (#962)")
	void aClickThatChangesNothingSaysWhereItWent() throws Exception
	{
		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().cancel();
		awaitApplicationInitialized();
		ApplicationSteps application = new ApplicationSteps(robot);
		DialogFixture settings = application.openSettingsDialog();
		application.selectSettingsTab(settings, "General");

		AssertionError failure = assertThrows(AssertionError.class,
			() -> application.selectSettingsTab(settings, "Plugins"));

		Rectangle dialog = UiTestSpeed.awaitSettled(settings.target())[0];
		Rectangle pluginsTab = tabOnScreen(settings, "Plugins");
		Point pointer = MouseInfo.getPointerInfo().getLocation();
		assertTrue(pluginsTab.contains(pointer),
			"the premise: the robot clicked on the Plugins tab " + where(pluginsTab)
				+ ", and the pointer rests at " + where(pointer));
		String message = failure.getMessage();
		assertTrue(message.contains("placed at " + where(dialog) + " before the click"),
			"the failure names where the dialog was placed: " + message);
		assertTrue(
			message.contains("right after the click: dialog 'dlgSettings' at " + where(dialog)
				+ ", tab 'Plugins' at " + where(pluginsTab) + ", pointer at " + where(pointer)
				+ ", active window 'dlgSettings'"),
			"the failure names the dialog, the tab, the pointer and the active window: " + message);
		assertTrue(message.contains("after waiting: dialog 'dlgSettings' at " + where(dialog)),
			"the failure reads them again when the step gives up: " + message);
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

	private static Rectangle tabOnScreen(final DialogFixture settings, final String title)
	{
		JTabbedPane tabs = settings.tabbedPane("tabSettings").target();
		return GuiActionRunner.execute(() -> {
			Rectangle tab = tabs.getUI().getTabBounds(tabs, tabs.indexOfTab(title));
			Point origin = tabs.getLocationOnScreen();
			return new Rectangle(origin.x + tab.x, origin.y + tab.y, tab.width, tab.height);
		});
	}

	private static String where(final Rectangle bounds)
	{
		return "(" + bounds.x + "," + bounds.y + ") " + bounds.width + "x" + bounds.height;
	}

	private static String where(final Point point)
	{
		return "(" + point.x + "," + point.y + ")";
	}

	private static String selectedTab(final DialogFixture settings)
	{
		JTabbedPane tabs = settings.tabbedPane("tabSettings").target();
		return GuiActionRunner.execute(() -> tabs.getTitleAt(tabs.getSelectedIndex()));
	}
}
