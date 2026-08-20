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
 * End-to-end use case "add a node to the database tree": right-click the root node in the visible
 * main frame, choose "add node..." from the context menu, name it, confirm - then save, restart and
 * sign in again: the node must still be there.
 * <p>
 * The restart half of this test guards the persistence path of tree edits (the live tree and the
 * persisted rootTreeAsMap must stay in sync)
 */
class AddNodeUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "add-node-db-pw-123";
	private static final String NEW_NODE_NAME = "My New Test Node";

	@Test
	void addedNodeShowsUpInTreeAndSurvivesReopen() throws Exception
	{
		File databaseFile = new File(tempHome, "add-node-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		application.addNodeToTreeRoot(frame, NEW_NODE_NAME);
		assertTrue(application.treeContainsNodeStartingWith(NEW_NODE_NAME),
			"the new node must show up in the tree right after adding it");

		application.saveDatabase();
		shutdownApplication();

		ApplicationSteps reopened = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		assertTrue(reopened.treeContainsNodeStartingWith(NEW_NODE_NAME),
			"the added node must still be in the tree after save and reopen");
	}
}
