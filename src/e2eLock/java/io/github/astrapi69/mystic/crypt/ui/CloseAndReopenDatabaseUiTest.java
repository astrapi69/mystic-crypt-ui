/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MenuId;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.action.CloseApplicationFileAction;
import io.github.astrapi69.mystic.crypt.action.OpenExistingDatabaseAction;
import io.github.astrapi69.mystic.crypt.lock.PublicAccess;

/**
 * Closing a vault while the application runs, and getting back into one afterwards (#281, #266).
 * <p>
 * Before this, "no vault open" was a state the application could not reach while running: the
 * save-if-dirty question lived inside the window-closing listener, so ending the application was
 * the only way to it. And after cancelling the sign-in every way into a vault was disabled, which
 * left Exit and a restart.
 * <p>
 * The round trip is the point rather than a nicety (quality-checks.md): a close path that empties
 * the model is easy to write and easy to get wrong in a way unit tests do not see, so this reopens
 * the same file through the real sign-in and looks for the entry that was in it.
 */
class CloseAndReopenDatabaseUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String ENTRY_TITLE = "survives-a-close";

	@Test
	@DisplayName("closing a vault empties the model and takes the view off the screen")
	void closingAVaultLeavesNothingOpen() throws IOException
	{
		File databaseFile = new File(tempHome, "close-empties.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, ENTRY_TITLE, "someone",
			TestPasswords.throwaway());
		application.saveDatabase();

		closeTheOpenVault();

		assertNull(
			GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject()
				.getMasterPwFileModelBean()),
			"the credentials are what says a vault is open at all, and after closing there is none");
		assertNull(
			GuiActionRunner
				.execute(() -> MysticCryptApplicationFrame.getInstance().getApplicationPanel()),
			"the panel is dropped rather than kept. Locking keeps it so unlocking can rebuild the "
				+ "view from it (#237); after closing there is nothing to rebuild, and a panel kept "
				+ "here is a decrypted vault left in memory with no way to reach it (#242)");
		assertFalse(
			GuiActionRunner.execute(
				() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn()),
			"closing must not leave the application pretending to be signed in");
		assertFalse(application.vaultIsOnScreen(), "and the vault is off the screen");
	}

	@Test
	@DisplayName("what was saved before closing is in the file when it is opened again")
	void aClosedVaultOpensAgainWithItsContent() throws IOException
	{
		File databaseFile = new File(tempHome, "close-and-reopen.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, ENTRY_TITLE, "someone",
			TestPasswords.throwaway());
		application.saveDatabase();
		closeTheOpenVault();

		ApplicationSteps reopened = openADatabaseFileInTheRunningApplication(databaseFile);
		reopened.showMainFrame();

		assertTrue(reopened.entryExistsWithTitle(ENTRY_TITLE),
			"the entry saved before the close has to be in the file that was reopened. A close "
				+ "path that empties the model is exactly the change a unit test can call green "
				+ "while the file it left behind is not the one it claims");
	}

	/**
	 * The whole way back in, through the entry #266 asked for: fire "Open Database...", answer the
	 * sign-in dialog it puts up, and wait until the application is in the vault again.
	 * <p>
	 * The action is fired off the test thread because the dialog it opens is modal - a test thread
	 * that waits for it to return never reaches the dialog it is waiting to answer
	 *
	 * @param databaseFile
	 *            the database to open
	 * @return steps for the signed-in application
	 */
	private ApplicationSteps openADatabaseFileInTheRunningApplication(final File databaseFile)
	{
		SwingUtilities.invokeLater(() -> new OpenExistingDatabaseAction("Open Database File")
			.actionPerformed(new ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		awaitSignInDialog().requireOkDisabled().checkMasterPassword()
			.typeMasterPassword(MASTER_PASSWORD).browseApplicationFile(databaseFile)
			.requireOkEnabled().okAndAwaitSignIn();
		return new ApplicationSteps(robot).awaitSignedIn();
	}

	@Test
	@DisplayName("with nothing open, opening a database file is offered and closing one is not")
	void thePublicStateOffersTheWayBackIn() throws Exception
	{
		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().cancel();
		awaitApplicationInitialized();

		assertTrue(menuItemIsEnabled(MenuId.OPEN_DATABASE_FILE.propertiesKey()),
			"cancelling the sign-in is not an error state, it is 'not right now'. From there "
				+ "opening a database is the one action that leads back into the application's "
				+ "purpose, and without it there was nothing left but Exit and a restart (#266)");
		assertTrue(PublicAccess.isPublicMenuId(MenuId.OPEN_DATABASE_FILE.propertiesKey()),
			"and it is offered because it is named public, not by accident");
		assertFalse(menuItemIsEnabled(MenuId.CLOSE_DATABASE.propertiesKey()),
			"with nothing open there is nothing to close");
	}

	/**
	 * Fires the close action and waits for the model to be empty. Fired through the action object
	 * rather than the menu item, so what is measured is the refusal in the action - the line that
	 * also holds for the keyboard shortcut (#284)
	 */
	private void closeTheOpenVault()
	{
		GuiActionRunner.execute(() -> new CloseApplicationFileAction("Close Database")
			.actionPerformed(new ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		robot.waitForIdle();
	}

	private static boolean menuItemIsEnabled(final String menuItemName)
	{
		return GuiActionRunner.execute(() -> {
			JMenuItem menuItem = findMenuItem(
				MysticCryptApplicationFrame.getInstance().getJMenuBar(), menuItemName);
			return menuItem != null && menuItem.isEnabled();
		});
	}

	private static JMenuItem findMenuItem(final javax.swing.MenuElement element,
		final String menuItemName)
	{
		for (javax.swing.MenuElement child : element.getSubElements())
		{
			if (child.getComponent()instanceof JMenuItem candidate
				&& menuItemName.equals(candidate.getName()))
			{
				return candidate;
			}
			JMenuItem found = findMenuItem(child, menuItemName);
			if (found != null)
			{
				return found;
			}
		}
		return null;
	}
}
