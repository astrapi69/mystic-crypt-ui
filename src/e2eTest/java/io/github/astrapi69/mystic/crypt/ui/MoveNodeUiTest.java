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

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * End-to-end test of moving nodes in the tree: up and down among the siblings, under another node,
 * and that the new order survives saving and reopening the database.
 */
class MoveNodeUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

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
	 * The root is what the whole database hangs off: there can only be one of it, so it is not
	 * shown at all - what the user sees as the top level are its children. Nothing may promote a
	 * node next to it, which is why the first node of the top level cannot be moved up.
	 */
	@Test
	void theRootIsNotShownAndNothingCanBeMovedNextToIt() throws Exception
	{
		File databaseFile = new File(tempHome, "move-node-root.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		assertFalse(application.treeShowsARowNamed("root"),
			"the root must not be a row of its own; the top level is what hangs under it");
		assertEquals(List.of("mykeys"), application.treeTopLevelNames(),
			"what the tree shows on its top level are the children of the root");

		application.addNodeToTreeRoot(frame, "Second");
		assertFalse(application.treeContextMenuItemIsEnabled(frame, "mykeys", "Move up"),
			"the first node of the top level has nothing above it - a node next to the root would "
				+ "be a second root");
		assertTrue(application.treeContextMenuHasItem(frame, "mykeys", "Move to node..."),
			"moving under a sibling stays possible - what it must never offer is the level above, "
				+ "which SecretKeyTreeMoveTest pins down");
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
