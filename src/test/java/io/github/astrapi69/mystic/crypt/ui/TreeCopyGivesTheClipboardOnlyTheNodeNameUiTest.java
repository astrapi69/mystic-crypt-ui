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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultMutableTreeNode;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * Ctrl+C on a tree node gives the clipboard the node's name and nothing else. It gave the node's
 * {@code toString}: Swing's {@code TreeTransferHandler} exports what {@code convertValueToText}
 * returns, whose default is {@code Object.toString()}, and the node's Lombok {@code toString}
 * carried every entry in the group with its password (#388). Measured in the running application:
 * 857 characters, the password in the form {@code [t, r, e, e, ...]}.
 * <p>
 * The copy is triggered the way the keystroke triggers it - the tree's {@code InputMap} maps
 * {@code ctrl C} to an action key, the {@code ActionMap} maps that key to the transfer handler's
 * copy action - rather than by a robot keypress, which under the harness's window manager did not
 * reach the tree.
 */
class TreeCopyGivesTheClipboardOnlyTheNodeNameUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String ENTRY_PASSWORD = "tree-copy-secret-9f3";

	@Test
	@DisplayName("Ctrl+C on a group node copies the group's name, not its entries")
	void copyingATreeNodeGivesTheClipboardTheNameOnly() throws Exception
	{
		File databaseFile = new File(tempHome, "tree-copy.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, "Probe Entry", "probe-user", ENTRY_PASSWORD);
		application.selectTreeRow(frame, 0);
		JTree tree = frame.tree().target();
		String nodeName = nameOfTheSelectedNode(tree);
		assertNotNull(nodeName, "the precondition: a named node is selected");

		String sentinel = "clipboard-before-ctrl-c";
		GuiActionRunner.execute(
			() -> io.github.astrapi69.awt.extension.ClipboardExtensions.copyToClipboard(sentinel));
		triggerTheCopyKeystrokeAction(tree);
		robot.waitForIdle();
		String clipboard = application.clipboardText();

		assertEquals(nodeName, clipboard,
			"the clipboard gets the node's name and nothing else - what the tree shows is what it "
				+ "copies");
		assertFalse(clipboard.contains(ENTRY_PASSWORD),
			"the entry's password reached the clipboard as text");
		assertFalse(clipboard.contains(Arrays.toString(ENTRY_PASSWORD.toCharArray())),
			"the entry's password reached the clipboard as a character array, which is how a "
				+ "Lombok toString prints a char[]");
		assertFalse(clipboard.contains("probe-user"), "the user name reached the clipboard");
	}

	private static String nameOfTheSelectedNode(final JTree tree)
	{
		return GuiActionRunner.execute(() -> {
			DefaultMutableTreeNode selected = (DefaultMutableTreeNode)tree.getSelectionPath()
				.getLastPathComponent();
			@SuppressWarnings("unchecked")
			BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node = (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>)selected
				.getUserObject();
			return node.getValue().getName();
		});
	}

	private static void triggerTheCopyKeystrokeAction(final JTree tree)
	{
		GuiActionRunner.execute(() -> {
			Object actionKey = tree.getInputMap(JComponent.WHEN_FOCUSED)
				.get(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
			Action copy = tree.getActionMap().get(actionKey);
			assertNotNull(copy, "ctrl C is bound on the tree, as it is on every Swing tree");
			copy.actionPerformed(
				new ActionEvent(tree, ActionEvent.ACTION_PERFORMED, String.valueOf(actionKey)));
		});
	}
}
