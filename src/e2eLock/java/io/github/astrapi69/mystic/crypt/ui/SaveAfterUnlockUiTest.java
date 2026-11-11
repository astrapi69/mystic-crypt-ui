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

import java.io.File;

import javax.swing.JMenuItem;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MenuId;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * A workspace that comes back from the lock with unsaved changes can save them (#913).
 * <p>
 * The unlock put the menu into the state of a freshly opened, unchanged vault, so Save was disabled
 * while the changes from before the lock were still unsaved, until something was edited again. Now
 * the unlock offers Save when there is something to save, and only then.
 */
class SaveAfterUnlockUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String UNSAVED_TITLE = "Added before the lock";

	@Test
	@DisplayName("after lock and unlock, Save is offered for the change from before the lock, and it lands in the file")
	void afterTheUnlock_theUnsavedChangeCanBeSaved() throws Exception
	{
		File databaseFile = new File(tempHome, "save-after-unlock.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, UNSAVED_TITLE, "the-user", TestPasswords.throwaway());
		assertTrue(saveIsOffered(), "precondition: the change can be saved before the lock");

		application.lockWorkspace();
		application.unlockWorkspace(MASTER_PASSWORD);

		assertTrue(theDatabaseIsMarkedChanged(), "the change is still unsaved after the unlock");
		assertTrue(saveIsOffered(), "Save is offered for it");

		application.saveDatabase();
		shutdownApplication();
		ApplicationSteps reopened = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		assertTrue(reopened.entryExistsWithTitle(UNSAVED_TITLE),
			"the change made before the lock is in the file");
	}

	@Test
	@DisplayName("after lock and unlock without a change, Save stays disabled as after opening")
	void afterTheUnlock_withoutAChange_saveStaysDisabled() throws Exception
	{
		File databaseFile = new File(tempHome, "nothing-to-save-after-unlock.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();

		application.lockWorkspace();
		application.unlockWorkspace(MASTER_PASSWORD);

		assertFalse(theDatabaseIsMarkedChanged(), "precondition: nothing to save");
		assertFalse(saveIsOffered(), "with nothing changed there is nothing to save");
	}

	private boolean saveIsOffered()
	{
		JMenuItem save = robot.finder().findByName(MenuId.SAVE_APPLICATION_FILE.propertiesKey(),
			JMenuItem.class, false);
		return GuiActionRunner.execute(save::isEnabled);
	}

	private static boolean theDatabaseIsMarkedChanged()
	{
		return GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject().isDirty());
	}
}
