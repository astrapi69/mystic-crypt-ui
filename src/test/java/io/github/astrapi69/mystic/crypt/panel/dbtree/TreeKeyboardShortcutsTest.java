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
package io.github.astrapi69.mystic.crypt.panel.dbtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.id.generate.LongIdGenerator;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * The keyboard shortcuts of the entry tree and the entries table: each key is bound, resolves the
 * SELECTED node - not a guessed one - and does nothing at all without a selection. The effects are
 * counted through the seam methods, because the real effects end in dialogs and in the application
 * frame, which a headless test must never reach.
 */
class TreeKeyboardShortcutsTest
{

	/** The panel with every seam method replaced by a counter */
	static class CountingPanel extends SecretKeyTreeWithContentPanel
	{
		final AtomicInteger deleted = new AtomicInteger();
		final AtomicInteger edited = new AtomicInteger();
		final AtomicInteger duplicated = new AtomicInteger();
		final AtomicInteger childAdded = new AtomicInteger();
		final AtomicReference<Integer> movedBy = new AtomicReference<>();
		final AtomicInteger usernameCopied = new AtomicInteger();
		final AtomicInteger passwordCopied = new AtomicInteger();
		final AtomicInteger entryDeleted = new AtomicInteger();

		CountingPanel(
			final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root)
		{
			super(BaseModel.of(root));
		}

		@Override
		protected void onDeleteSelectedTreeNode(final DefaultMutableTreeNode selectedTreeNode)
		{
			deleted.incrementAndGet();
		}

		@Override
		protected void onEditSelectedTreeNode(final DefaultMutableTreeNode selectedTreeNode)
		{
			edited.incrementAndGet();
		}

		@Override
		protected void onDuplicateSelectedTreeNode(final DefaultMutableTreeNode selectedTreeNode)
		{
			duplicated.incrementAndGet();
		}

		@Override
		protected void addChildTreeNode(final DefaultMutableTreeNode parentSwingNode)
		{
			childAdded.incrementAndGet();
		}

		@Override
		protected void onMoveSelectedTreeNode(final DefaultMutableTreeNode selectedTreeNode,
			final int offset)
		{
			movedBy.set(offset);
		}

		@Override
		protected void onCopyUsernameTableEntry()
		{
			usernameCopied.incrementAndGet();
		}

		@Override
		protected void onCopyPasswordTableEntry()
		{
			passwordCopied.incrementAndGet();
		}

		@Override
		protected void onDeleteTableEntry()
		{
			entryDeleted.incrementAndGet();
		}
	}

	private static CountingPanel panelWithASelectedNode()
	{
		CountingPanel panel = new CountingPanel(
			TestBaseTreeNodeFactory.initializeTestGenericTreeNodeElement(
				List.of(MysticCryptEntryModelBean.builder().title("an entry").build()),
				List.of(MysticCryptEntryModelBean.builder().title("another").build()),
				LongIdGenerator.of(0L)));
		DefaultMutableTreeNode root = (DefaultMutableTreeNode)panel.getTree().getModel().getRoot();
		DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode)root.getChildAt(0);
		panel.getTree().setSelectionPath(new TreePath(firstChild.getPath()));
		return panel;
	}

	private static void press(final JComponent component, final String keyStroke)
	{
		Object actionKey = component.getInputMap(JComponent.WHEN_FOCUSED)
			.get(KeyStroke.getKeyStroke(keyStroke));
		assertNotNull(actionKey, "the key '" + keyStroke + "' is not bound");
		Action action = component.getActionMap().get(actionKey);
		assertNotNull(action, "the key '" + keyStroke + "' is bound to a missing action");
		action.actionPerformed(null);
	}

	@Test
	@DisplayName("the tree keys act on the selected node")
	void theTreeKeysActOnTheSelectedNode()
	{
		CountingPanel panel = panelWithASelectedNode();

		press(panel.getTree(), "DELETE");
		press(panel.getTree(), "F2");
		press(panel.getTree(), "control K");
		press(panel.getTree(), "INSERT");
		press(panel.getTree(), "alt DOWN");

		assertEquals(1, panel.deleted.get(), "DELETE did not reach the deletion");
		assertEquals(1, panel.edited.get(), "F2 did not reach the editing");
		assertEquals(1, panel.duplicated.get(), "control K did not reach the duplication");
		assertEquals(1, panel.childAdded.get(), "INSERT did not reach adding a child");
		assertEquals(1, panel.movedBy.get(), "alt DOWN did not move down by one");

		press(panel.getTree(), "alt UP");
		assertEquals(-1, panel.movedBy.get(), "alt UP did not move up by one");
	}

	@Test
	@DisplayName("without a selected node the tree keys do nothing")
	void withoutASelectedNodeTheTreeKeysDoNothing()
	{
		CountingPanel panel = panelWithASelectedNode();
		panel.getTree().clearSelection();

		press(panel.getTree(), "DELETE");
		press(panel.getTree(), "F2");
		press(panel.getTree(), "control K");
		press(panel.getTree(), "INSERT");

		assertEquals(0, panel.deleted.get(), "DELETE acted although nothing is selected");
		assertEquals(0, panel.edited.get(), "F2 acted although nothing is selected");
		assertEquals(0, panel.duplicated.get(), "control K acted although nothing is selected");
		assertEquals(0, panel.childAdded.get(), "INSERT acted although nothing is selected");
	}

	@Test
	@DisplayName("the table keys act on the selected entry")
	void theTableKeysActOnTheSelectedEntry()
	{
		CountingPanel panel = panelWithASelectedNode();
		panel.getTblTreeEntryTable().setRowSelectionInterval(0, 0);

		press(panel.getTblTreeEntryTable(), "control B");
		press(panel.getTblTreeEntryTable(), "control C");
		press(panel.getTblTreeEntryTable(), "DELETE");

		assertEquals(1, panel.usernameCopied.get(), "control B did not copy the user name");
		assertEquals(1, panel.passwordCopied.get(), "control C did not copy the password");
		assertEquals(1, panel.entryDeleted.get(), "DELETE did not reach the entry deletion");
	}

	@Test
	@DisplayName("without a selected row the table keys do nothing, dialog included")
	void withoutASelectedRowTheTableKeysDoNothing()
	{
		CountingPanel panel = panelWithASelectedNode();
		panel.getTblTreeEntryTable().clearSelection();

		press(panel.getTblTreeEntryTable(), "DELETE");
		press(panel.getTblTreeEntryTable(), "control C");

		assertEquals(0, panel.entryDeleted.get(),
			"DELETE opened the deletion although no entry is selected");
		assertEquals(0, panel.passwordCopied.get(),
			"control C copied although no entry is selected");
	}

}
