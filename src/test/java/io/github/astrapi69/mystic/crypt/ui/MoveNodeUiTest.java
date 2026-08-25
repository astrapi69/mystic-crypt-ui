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
import java.util.List;

import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

/**
 * End-to-end test of moving nodes in the tree: up and down among the siblings, under another node,
 * and that the new order survives saving and reopening the database.
 */
class MoveNodeUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "move-node-pw-123";

	@Test
	void movesNodesUpAndDownAndUnderAnotherNode() throws Exception
	{
		File databaseFile = new File(tempHome, "move-node-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		application.addNodeToTreeRoot(frame, "Alpha");
		application.addNodeToTreeRoot(frame, "Beta");
		application.addNodeToTreeRoot(frame, "Gamma");

		List<String> initial = application.treeRootChildNames();
		assertEquals(List.of("mykeys", "Alpha", "Beta", "Gamma"), initial,
			"the nodes start in the order they were added");

		// move up
		application.moveNodeUp(frame, "Gamma");
		assertEquals(List.of("mykeys", "Alpha", "Gamma", "Beta"), application.treeRootChildNames(),
			"'Move up' must swap the node with the one above it");

		// move down again
		application.moveNodeDown(frame, "Gamma");
		assertEquals(List.of("mykeys", "Alpha", "Beta", "Gamma"), application.treeRootChildNames(),
			"'Move down' must swap the node with the one below it");

		// the order survives a save and a reopen
		application.saveDatabase();
		shutdownApplication();
		ApplicationSteps reopened = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture reopenedFrame = reopened.showMainFrame();
		assertEquals(List.of("mykeys", "Alpha", "Beta", "Gamma"), reopened.treeRootChildNames(),
			"the order of the nodes must survive saving and reopening");

		// move a node under another one
		reopened.moveNodeUnder(reopenedFrame, "Gamma", "Alpha");
		assertEquals(List.of("mykeys", "Alpha", "Beta"), reopened.treeRootChildNames(),
			"the moved node must be gone from its old parent");
		assertEquals(List.of("Gamma"), reopened.treeChildNamesOf("Alpha"),
			"the moved node must be under its new parent");
	}

	/**
	 * The root is the one node that must stay where it is: there is exactly one of it, so it has no
	 * siblings to move among, nothing to be moved under, and it must not be removable - a second
	 * top level node would be a tree with two roots.
	 */
	@Test
	void theRootCanNeitherBeMovedNorDeleted() throws Exception
	{
		File databaseFile = new File(tempHome, "move-node-root.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		assertFalse(application.treeContextMenuHasItem(frame, "root", "Move up"),
			"the root has no siblings, so it must not offer to move up");
		assertFalse(application.treeContextMenuHasItem(frame, "root", "Move down"),
			"the root has no siblings, so it must not offer to move down");
		assertFalse(application.treeContextMenuHasItem(frame, "root", "Move to node..."),
			"there is nothing the root could be moved under");
		assertFalse(application.treeContextMenuHasItem(frame, "root", "delete"),
			"deleting the root would leave the tree without one");
		assertTrue(application.treeContextMenuHasItem(frame, "root", "add node..."),
			"the root must still take new children");
	}

	@Test
	void theFirstNodeCannotGoUpAndTheLastCannotGoDown() throws Exception
	{
		File databaseFile = new File(tempHome, "move-node-bounds.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		application.addNodeToTreeRoot(frame, "Last");

		assertFalse(application.treeContextMenuItemIsEnabled(frame, "mykeys", "Move up"),
			"the first node has nothing to move up past");
		assertTrue(application.treeContextMenuItemIsEnabled(frame, "mykeys", "Move down"),
			"the first node can still go down");
		assertFalse(application.treeContextMenuItemIsEnabled(frame, "Last", "Move down"),
			"the last node has nothing to move down past");
		assertTrue(application.treeContextMenuItemIsEnabled(frame, "Last", "Move up"),
			"the last node can still go up");
	}
}
