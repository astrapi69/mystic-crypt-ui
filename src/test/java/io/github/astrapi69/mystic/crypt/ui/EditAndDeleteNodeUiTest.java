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

/**
 * End-to-end use cases "rename a node" and "delete a node", chained the way a user works: create a
 * node, rename it via the context menu's "Edit node...", then delete it via "delete" plus the
 * confirmation dialog - each step verified in the model, the delete also after save and restart
 */
class EditAndDeleteNodeUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "edit-node-db-pw-123";
	private static final String ORIGINAL_NAME = "Node To Rename";
	private static final String RENAMED_NAME = "Renamed Node";

	@Test
	void nodeCanBeRenamedAndDeletedThroughContextMenu() throws Exception
	{
		File databaseFile = new File(tempHome, "edit-node-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		application.addNodeToTreeRoot(frame, ORIGINAL_NAME);
		assertTrue(application.treeContainsNodeStartingWith(ORIGINAL_NAME));

		application.editNodeName(frame, ORIGINAL_NAME, RENAMED_NAME);
		assertTrue(application.treeContainsNodeStartingWith(RENAMED_NAME),
			"the node must carry its new name after the rename");
		assertFalse(application.treeContainsNodeStartingWith(ORIGINAL_NAME),
			"the old name must be gone after the rename");

		application.deleteNode(frame, RENAMED_NAME);
		assertFalse(application.treeContainsNodeStartingWith(RENAMED_NAME),
			"the node must be gone right after the delete");

		application.saveDatabase();
		shutdownApplication();

		ApplicationSteps reopened = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		assertFalse(reopened.treeContainsNodeStartingWith(RENAMED_NAME),
			"the deleted node must not reappear after save and reopen");
	}
}
