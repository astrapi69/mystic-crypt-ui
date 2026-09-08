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

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.io.File;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MenuId;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * The settings dialog is offered without a vault - a theme, a language, a tooltip preference are
 * nobody's secret. Its Plugins tab is not: installing or enabling a plugin loads that plugin's code
 * into the running process, right away and without a restart (#232).
 * <p>
 * The whole tab rather than its buttons: a list of installed plugins without them is worth little,
 * and picking single widgets out of dialogs is a list that could never be finished. The tab asks
 * the predicate directly, which is what everything that is not a menu entry does.
 */
class PluginsTabNeedsAVaultUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("without a vault the Plugins tab is not usable, the other tabs are")
	void thePluginsTabIsDisabledWithoutAVault() throws IOException
	{
		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().cancel();
		awaitApplicationInitialized();

		openSettings();

		assertFalse(tabEnabled("Plugins"),
			"installing or enabling a plugin runs its code in this application, so it is not "
				+ "something offered before anyone has signed in");
		assertTrue(tabEnabled("General"),
			"a theme and a language are not a vault: the rest of the dialog stays usable");
		assertTrue(tabEnabled("Plugin settings"),
			"and so do the settings of plugins that are already installed");
	}

	@Test
	@DisplayName("signed in, the Plugins tab is usable again")
	void thePluginsTabIsEnabledWithAVault() throws IOException
	{
		File databaseFile = new File(tempHome, "plugins-tab-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		awaitApplicationInitialized();

		openSettings();

		assertTrue(tabEnabled("Plugins"),
			"with a vault open the tab is what it always was - the same predicate, the other answer");
	}

	private void openSettings()
	{
		JMenuItem settings = robot.finder().findByName(MenuId.SETTINGS.propertiesKey(),
			JMenuItem.class, false);
		SwingUtilities.invokeLater(settings::doClick);
		Pause.pause(new Condition("the settings dialog is on screen")
		{
			@Override
			public boolean test()
			{
				return settingsDialog() != null;
			}
		}, 15000);
	}

	private static JDialog settingsDialog()
	{
		return GuiActionRunner.execute(() -> {
			for (Window window : Window.getWindows())
			{
				if (window instanceof JDialog dialog && dialog.isShowing()
					&& "Settings".equals(dialog.getTitle()))
				{
					return dialog;
				}
			}
			return null;
		});
	}

	private static boolean tabEnabled(final String title)
	{
		return GuiActionRunner.execute(() -> {
			JTabbedPane tabs = findTabbedPane(settingsDialog());
			for (int index = 0; index < tabs.getTabCount(); index++)
			{
				if (title.equals(tabs.getTitleAt(index)))
				{
					return tabs.isEnabledAt(index);
				}
			}
			throw new IllegalStateException("no tab titled '" + title + "'");
		});
	}

	private static JTabbedPane findTabbedPane(final Container container)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof JTabbedPane tabbedPane)
			{
				return tabbedPane;
			}
			if (child instanceof Container nested)
			{
				JTabbedPane found = findTabbedPane(nested);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}
}
