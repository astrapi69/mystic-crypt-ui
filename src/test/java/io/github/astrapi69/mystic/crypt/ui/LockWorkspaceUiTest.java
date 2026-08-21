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
import io.github.astrapi69.swing.enumeration.FrameMode;

/**
 * End-to-end use case "lock the workspace": locking must clear the signed-in state and hide the
 * content behind the desktop pane, and entering the correct master password in the unlock dialog
 * must restore the signed-in state and the content; a wrong password must keep the workspace locked
 */
class LockWorkspaceUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "lock-pw-123";

	@Test
	void lockHidesContentAndUnlockRestoresItWithTheMasterPassword() throws IOException
	{
		File databaseFile = new File(tempHome, "lock-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		assertTrue(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn(),
			"precondition: signed in after opening the database");

		application.lockWorkspace();
		assertFalse(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn(),
			"locking must clear the signed-in state");
		assertEquals(FrameMode.DESKTOP_PANE,
			MysticCryptApplicationFrame.getInstance().getFrameMode(),
			"locking must hide the content behind the desktop pane");

		application.unlockWorkspace(MASTER_PASSWORD);
		assertTrue(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn(),
			"unlocking with the correct password must restore the signed-in state");
		assertEquals(FrameMode.APPLICATION_PANEL,
			MysticCryptApplicationFrame.getInstance().getFrameMode(),
			"unlocking must show the application content again");
	}

	@Test
	void wrongUnlockPasswordKeepsWorkspaceLockedThenCorrectOneUnlocks() throws IOException
	{
		File databaseFile = new File(tempHome, "lock-wrongpw-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		application.lockWorkspace();
		application.enterUnlockPasswordExpectingFailure("definitely-wrong");
		assertFalse(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn(),
			"a wrong unlock password must keep the workspace locked");

		application.unlockWorkspace(MASTER_PASSWORD);
		assertTrue(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn(),
			"the re-opened unlock dialog must accept the correct password");
	}
}
