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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

/**
 * End-to-end use case "duplicate a node": create a node with an entry inside, duplicate it via the
 * context menu's "Duplicate node..." - both nodes (and the entry in each) must exist, also after
 * save and restart
 */
class DuplicateNodeUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "duplicate-node-db-pw-123";
	private static final String NODE_NAME = "Original Node";
	private static final String DUPLICATE_NAME = "Duplicated Node";
	private static final String ENTRY_TITLE = "Entry Inside";

	@Test
	void duplicatedNodeWithEntrySurvivesReopen() throws Exception
	{
		File databaseFile = new File(tempHome, "duplicate-node-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		application.addNodeToTreeRoot(frame, NODE_NAME);
		application.selectTreeRowByName(frame, NODE_NAME);
		application.addEntry(frame, ENTRY_TITLE, "dup-user", "dup-secret-pw-1");

		application.duplicateNode(frame, NODE_NAME, DUPLICATE_NAME);
		assertTrue(application.treeContainsNodeStartingWith(NODE_NAME),
			"the original node must still exist after duplicating");
		assertTrue(application.treeContainsNodeStartingWith(DUPLICATE_NAME),
			"the duplicate node must exist under its own name");

		application.saveDatabase();
		shutdownApplication();

		ApplicationSteps reopened = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		assertTrue(reopened.treeContainsNodeStartingWith(NODE_NAME),
			"the original node must still be there after save and reopen");
		assertTrue(reopened.treeContainsNodeStartingWith(DUPLICATE_NAME),
			"the duplicate node must still be there after save and reopen");
		assertTrue(reopened.entryExistsWithTitle(ENTRY_TITLE),
			"the entry inside must still be there after save and reopen");
	}
}
