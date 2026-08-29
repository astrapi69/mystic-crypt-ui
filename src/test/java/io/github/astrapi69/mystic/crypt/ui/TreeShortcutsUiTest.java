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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * End-to-end use case "work the tree from the keyboard": F2 opens the selected node for editing and
 * Delete asks before removing it, without ever touching the context menu. Driven through the real
 * tree, because what a key does depends on the focus and the selection, which no headless test can
 * stand in for.
 */
class TreeShortcutsUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("F2 opens the selected node for editing")
	void f2OpensTheSelectedNodeForEditing() throws IOException
	{
		File databaseFile = new File(tempHome, "tree-shortcuts-edit.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.addNodeToTreeRoot(frame, "NodeToRename");
		application.selectTreeNodeByNameAndFocus("NodeToRename");

		javax.swing.SwingUtilities.invokeLater(() -> application.pressOnTree(KeyEvent.VK_F2));

		DialogFixture editDialog = application.findDialogWithTitle("Edit node");
		assertEquals("NodeToRename",
			GuiActionRunner.execute(() -> editDialog.textBox().target().getText()),
			"the editing opened on a node other than the selected one");
		editDialog.close();
	}

	@Test
	@DisplayName("Delete asks before it removes the selected node")
	void deleteAsksBeforeItRemovesTheSelectedNode() throws IOException
	{
		File databaseFile = new File(tempHome, "tree-shortcuts-delete.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.addNodeToTreeRoot(frame, "NodeToDelete");
		application.selectTreeNodeByNameAndFocus("NodeToDelete");
		assertEquals("NodeToDelete", application.selectedTreeNodeName());

		javax.swing.SwingUtilities.invokeLater(() -> application.pressOnTree(KeyEvent.VK_DELETE));

		DialogFixture confirmation = application.findDialogWithTitle("Confirm deletion");
		confirmation.close();
		robot.waitForIdle();
		assertTrue(application.treeShowsARowNamed("NodeToDelete"),
			"cancelling the confirmation must leave the node where it was");
	}

}
