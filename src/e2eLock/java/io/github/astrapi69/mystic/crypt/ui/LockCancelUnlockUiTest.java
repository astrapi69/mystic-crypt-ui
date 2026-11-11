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

import java.io.File;
import java.io.IOException;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Negative end-to-end case: cancelling the unlock dialog after locking the workspace must keep the
 * workspace locked - and leave a way back to the dialog (#889).
 * <p>
 * Pinned as the program behaves today for the way back: after Cancel the lock entry, the one entry
 * that brings the prompt back, is disabled, because the locked menu offers only the public entries
 * and the lock entry is not one of them.
 */
class LockCancelUnlockUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

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
	@DisplayName("after Cancel the lock entry is disabled today, so nothing brings the unlock prompt back")
	void afterCancel_theLockEntryIsDisabled_today() throws IOException
	{
		File databaseFile = new File(tempHome, "lock-cancel-way-back.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();

		application.lockWorkspace();
		application.cancelUnlock();

		assertFalse(application.lockEntryIsOffered(),
			"today the lock entry is disabled while the workspace is locked");
		assertEquals("Lock workspace",
			GuiActionRunner.execute(() -> application.lockEntry().getText()));
	}
}
