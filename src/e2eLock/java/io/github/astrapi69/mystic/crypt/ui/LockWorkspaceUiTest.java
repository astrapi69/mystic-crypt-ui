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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.swing.enumeration.FrameMode;

/**
 * End-to-end use case "lock the workspace": locking must clear the signed-in state and hide the
 * content behind the desktop pane, and entering the correct master password in the unlock dialog
 * must restore the signed-in state and the content; a wrong password must keep the workspace locked
 */
class LockWorkspaceUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	void lockHidesContentAndUnlockRestoresItWithTheMasterPassword() throws IOException
	{
		File databaseFile = new File(tempHome, "lock-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		assertTrue(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn(),
			"precondition: signed in after opening the database");
		application.addNodeToTreeRoot(application.showMainFrame(), "SurvivesLock");
		assertTrue(application.vaultIsOnScreen(),
			"precondition: the vault is in front of the user before locking - without it the "
				+ "assertions after unlocking would pass for the wrong reason");

		application.lockWorkspace();
		assertFalse(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn(),
			"locking must clear the signed-in state");
		assertEquals(FrameMode.DESKTOP_PANE,
			MysticCryptApplicationFrame.getInstance().getFrameMode(),
			"locking must hide the content behind the desktop pane");
		// the frame mode alone said nothing about what is on screen: it was already DESKTOP_PANE
		// while the database view was sitting on that desktop, readable and operable (#237)
		assertFalse(application.isInternalFrameShowing("Key database"),
			"locking must take the database view off the screen, not just switch the mode");

		application.unlockWorkspace(MASTER_PASSWORD);
		assertTrue(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn(),
			"unlocking with the correct password must restore the signed-in state");
		assertEquals(FrameMode.APPLICATION_PANEL,
			MysticCryptApplicationFrame.getInstance().getFrameMode(),
			"unlocking must show the application content again");
		// the mode alone would wave through an empty screen, which is exactly what the fix for
		// #237 could have broken: the view is removed on locking and has to be built again here.
		// Three statements, and they are not the same one three times (#250): the window is back on
		// screen, the tree in it carries the row again, and the model behind it kept the node
		assertTrue(application.vaultIsOnScreen(),
			"unlocking must put the vault back on the screen");
		assertTrue(application.treeShowsARowNamed("SurvivesLock"),
			"and the tree in it must show the node again, not merely hold it in the model");
		assertTrue(application.treeContainsNodeStartingWith("SurvivesLock"),
			"and the model behind the view must still carry it");
	}

	@Test
	void wrongUnlockPasswordKeepsWorkspaceLockedThenCorrectOneUnlocks() throws IOException
	{
		File databaseFile = new File(tempHome, "lock-wrongpw-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		assertTrue(application.vaultIsOnScreen(),
			"precondition: the vault is in front of the user before locking");

		application.lockWorkspace();
		assertFalse(application.vaultIsOnScreen(), "locking takes it off the screen");
		application.enterUnlockPasswordExpectingFailure("definitely-wrong");
		assertFalse(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn(),
			"a wrong unlock password must keep the workspace locked");
		assertFalse(application.isInternalFrameShowing("Key database"),
			"and locked means the database view stays off the screen, not merely a false flag");

		application.unlockWorkspace(MASTER_PASSWORD);
		assertTrue(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn(),
			"the re-opened unlock dialog must accept the correct password");
		// the flag is what the user does not see. "Unlocked" means the vault is back in front of
		// them, and a fix that flips the flag without rebuilding the view would pass on the flag
		// alone (#250)
		assertTrue(application.vaultIsOnScreen(),
			"and accepting it must put the vault back on the screen");
	}
}
