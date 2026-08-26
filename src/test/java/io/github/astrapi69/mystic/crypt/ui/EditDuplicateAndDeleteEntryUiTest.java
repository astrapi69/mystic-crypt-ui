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

import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * End-to-end use cases "edit an entry", "duplicate an entry" and "delete an entry", chained the way
 * a user works on the entries table: create an entry, rename it via "edit...", duplicate it via
 * "duplicate...", delete the duplicate via "delete" - each step verified in the model, and the
 * final state (edited entry present, duplicate gone) verified again after save and restart
 */
class EditDuplicateAndDeleteEntryUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();
	private static final String ORIGINAL_TITLE = "Mail Account";
	private static final String EDITED_TITLE = "Mail Account Work";
	private static final String DUPLICATE_TITLE = "Mail Account Private";

	@Test
	void entryCanBeEditedDuplicatedAndDeleted() throws Exception
	{
		File databaseFile = new File(tempHome, "entry-crud-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		application.selectTreeRow(frame, 0);
		application.addEntry(frame, ORIGINAL_TITLE, "mail-user", "mail-secret-pw-1");

		application.selectEntryRowByTitle(frame, ORIGINAL_TITLE);
		application.editSelectedEntryTitle(frame, EDITED_TITLE);
		assertTrue(application.entryExistsWithTitle(EDITED_TITLE),
			"the entry must carry its new title after the edit");
		assertFalse(application.entryExistsWithTitle(ORIGINAL_TITLE),
			"the old title must be gone after the edit");

		application.selectEntryRowByTitle(frame, EDITED_TITLE);
		application.duplicateSelectedEntry(frame, DUPLICATE_TITLE);
		assertTrue(application.entryExistsWithTitle(DUPLICATE_TITLE),
			"the duplicate must exist under its own title");
		assertTrue(application.entryExistsWithTitle(EDITED_TITLE),
			"the original entry must still exist after duplicating");

		application.selectEntryRowByTitle(frame, DUPLICATE_TITLE);
		application.deleteSelectedEntry(frame);
		assertFalse(application.entryExistsWithTitle(DUPLICATE_TITLE),
			"the duplicate must be gone right after the delete");

		application.saveDatabase();
		shutdownApplication();

		ApplicationSteps reopened = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		assertTrue(reopened.entryExistsWithTitle(EDITED_TITLE),
			"the edited entry must still be there after save and reopen");
		assertFalse(reopened.entryExistsWithTitle(DUPLICATE_TITLE),
			"the deleted duplicate must not reappear after save and reopen");
	}
}
