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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.DesktopMenu;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * End-to-end regression test for the best-practice menu layout: after sign-in the menu bar must
 * read File | View | ... | Help - Help stays last, the former top-level "Look and Feel" menu is
 * gone from the bar and now lives under a "View" menu together with the View Mode submenu. Guards
 * the reorganizeMenus() rework, locale-independently (checks menu identity, not localized text).
 */
class MenuStructureUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	void helpIsLastAndLookAndFeelLivesUnderView() throws Exception
	{
		File databaseFile = new File(tempHome, "menu-structure-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();

		GuiActionRunner.execute(() -> {
			DesktopMenu menu = (DesktopMenu)MysticCryptApplicationFrame.getInstance().getMenu();
			JMenuBar menubar = menu.getMenubar();
			JMenu helpMenu = menu.getHelpMenu();
			JMenu lookAndFeelMenu = menu.getLookAndFeelMenu();

			assertNotNull(helpMenu, "there must be a Help menu");
			assertSame(helpMenu, menubar.getMenu(menubar.getMenuCount() - 1),
				"Help must be the last top-level menu");

			assertTrue(menubar.getComponentIndex(lookAndFeelMenu) < 0,
				"Look and Feel must no longer be a top-level menu");

			JMenu viewMenu = null;
			for (int i = 0; i < menubar.getMenuCount(); i++)
			{
				if ("global.menu.view".equals(menubar.getMenu(i).getName()))
				{
					viewMenu = menubar.getMenu(i);
					break;
				}
			}
			assertNotNull(viewMenu, "there must be a View menu");

			boolean viewContainsLookAndFeel = false;
			for (Component child : viewMenu.getMenuComponents())
			{
				if (child == lookAndFeelMenu)
				{
					viewContainsLookAndFeel = true;
					break;
				}
			}
			assertTrue(viewContainsLookAndFeel,
				"the View menu must contain the Look and Feel menu");
			return null;
		});
	}
}
