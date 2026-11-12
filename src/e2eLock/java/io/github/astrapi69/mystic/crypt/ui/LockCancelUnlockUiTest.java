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

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Negative end-to-end case: cancelling the unlock dialog after locking the workspace must keep the
 * workspace locked - and leave a way back to the dialog (#889).
 * <p>
 * The lock entry used to be disabled in the locked menu, since it is not a public entry, and it is
 * the one entry from the user's side that brings the prompt back: after Cancel, unsaved changes
 * could only be discarded. Locked, it now says "Unlock workspace..." and opens the prompt.
 */
class LockCancelUnlockUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String UNSAVED_TITLE = "Added before the lock";

	@Test
	void cancellingUnlockKeepsWorkspaceLocked() throws IOException
	{
		File databaseFile = new File(tempHome, "lock-cancel-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		application.lockWorkspace();
		application.cancelUnlock();

		assertFalse(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn(),
			"cancelling the unlock dialog must keep the workspace locked");
		// "locked" is more than the flag: cancelling used to leave the database view on screen
		// (#237)
		assertFalse(application.isInternalFrameShowing("Key database"),
			"cancelling must leave the database view off the screen as well");
	}

	@Test
	@DisplayName("after Cancel, Unlock workspace... brings the prompt back and the unsaved change can be saved")
	void afterCancel_unlockWorkspaceBringsThePromptBack() throws Exception
	{
		File databaseFile = new File(tempHome, "lock-cancel-way-back.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, UNSAVED_TITLE, "the-user", TestPasswords.throwaway());

		application.lockWorkspace();
		application.cancelUnlock();

		assertTrue(application.lockEntryIsOffered(),
			"locked, the lock entry is the way back to the unlock prompt");
		assertEquals("Unlock workspace...",
			GuiActionRunner.execute(() -> application.lockEntry().getText()));

		application.pressTheLockEntryAgain().unlockWorkspace(MASTER_PASSWORD);
		assertEquals("Lock workspace",
			GuiActionRunner.execute(() -> application.lockEntry().getText()),
			"signed in again, the entry locks");
		application.saveDatabase();
		shutdownApplication();

		ApplicationSteps reopened = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		assertTrue(reopened.entryExistsWithTitle(UNSAVED_TITLE),
			"the change made before the lock is in the file");
	}
}
