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

import java.awt.Dialog;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.Robot;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.mystic.crypt.MenuId;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * Reusable steps for driving the signed-in application: menu actions (the main frame is never made
 * visible in tests, so menu items are triggered through their action, found by their stable
 * component name) and the KeePass import/export dialogs those actions open.
 * <p>
 * Same interaction conventions as {@link SignInDialogSteps}
 */
final class ApplicationSteps
{

	private final Robot robot;

	ApplicationSteps(Robot robot)
	{
		this.robot = robot;
	}

	/** Waits until the application model reports a completed sign-in */
	ApplicationSteps awaitSignedIn()
	{
		Pause.pause(new Condition("application model is signed in")
		{
			@Override
			public boolean test()
			{
				MysticCryptApplicationFrame applicationFrame = MysticCryptApplicationFrame
					.getInstance();
				return applicationFrame != null && applicationFrame.getModelObject() != null
					&& applicationFrame.getModelObject().isSignedIn()
					&& applicationFrame.getApplicationPanel() != null;
			}
		}, 15000);
		return this;
	}

	/**
	 * Opens the KeePass import dialog via the File menu, selects the given {@code .kdbx} file
	 * through the file chooser, enters the password, confirms with OK and closes the success
	 * message - the complete import use case as a user performs it
	 */
	ApplicationSteps importKeePassDatabase(File keePassFile, String password)
	{
		clickMenuItem(MenuId.IMPORT_KEEPASS.propertiesKey());

		DialogFixture importDialog = findDialogWithTitle("Import from KeePass");
		browseAndPickFile(importDialog, "btnFile", keePassFile);
		GuiActionRunner
			.execute(() -> importDialog.textBox("txtPassword").target().setText(password));
		robot.waitForIdle();
		UiTestSpeed.step();
		clickDialogButton(importDialog, "OK");

		dismissMessageDialog("Import successful");
		return this;
	}

	/**
	 * Opens the KeePass export dialog via the File menu, selects the given destination file through
	 * the file chooser, enters the password, confirms with OK and closes the success message - the
	 * complete export use case as a user performs it
	 */
	ApplicationSteps exportKeePassDatabase(File destinationFile, String password)
	{
		clickMenuItem(MenuId.EXPORT_KEEPASS.propertiesKey());

		DialogFixture exportDialog = findDialogWithTitle("Export to KeePass");
		browseAndPickFile(exportDialog, "btnFile", destinationFile);
		GuiActionRunner
			.execute(() -> exportDialog.textBox("txtPassword").target().setText(password));
		robot.waitForIdle();
		UiTestSpeed.step();
		clickDialogButton(exportDialog, "OK");

		dismissMessageDialog("Export successful");
		return this;
	}

	/**
	 * Makes the main application frame visible and returns a fixture for it. Needed for use cases
	 * that require real mouse interaction with components inside the frame (e.g. the database
	 * tree's context menu) - pure menu-action use cases work without ever showing the frame
	 */
	org.assertj.swing.fixture.FrameFixture showMainFrame()
	{
		MysticCryptApplicationFrame applicationFrame = MysticCryptApplicationFrame.getInstance();
		GuiActionRunner.execute(() -> {
			applicationFrame.setSize(1200, 800);
			applicationFrame.setVisible(true);
			applicationFrame.toFront();
		});
		robot.waitForIdle();
		UiTestSpeed.windowManagerSettle();
		return new org.assertj.swing.fixture.FrameFixture(robot, applicationFrame);
	}

	/**
	 * Adds a child node to the tree's root through the real user flow: right-click the root row,
	 * choose "add node..." from the context menu, type the name into the "New node" dialog and
	 * confirm with OK.
	 * <p>
	 * The right-click is dispatched as a synthetic {@link java.awt.event.MouseEvent} straight to
	 * the tree (same listener path as a real click) - an OS-level robot right-click proved
	 * unreliable on this shared, live desktop display
	 */
	ApplicationSteps addNodeToTreeRoot(org.assertj.swing.fixture.FrameFixture frame, String name)
	{
		rightClickTreeRow(frame, 0);
		chooseFromShowingPopup("add node...");

		DialogFixture newNodeDialog = findDialogWithTitle("New node");
		GuiActionRunner.execute(() -> newNodeDialog.textBox().target().setText(name));
		robot.waitForIdle();
		UiTestSpeed.step();
		clickDialogButton(newNodeDialog, "OK");
		awaitDialogClosed(newNodeDialog, "new-node dialog");
		return this;
	}

	/**
	 * Renames the tree node with the given display name through the real user flow: right-click,
	 * "Edit node...", change the name in the "Edit node" dialog, OK
	 */
	ApplicationSteps editNodeName(org.assertj.swing.fixture.FrameFixture frame, String nodeName,
		String newName)
	{
		rightClickTreeNodeByName(frame, nodeName);
		chooseFromShowingPopup("Edit node...");

		DialogFixture editNodeDialog = findDialogWithTitle("Edit node");
		GuiActionRunner.execute(() -> editNodeDialog.textBox().target().setText(newName));
		robot.waitForIdle();
		UiTestSpeed.step();
		clickDialogButton(editNodeDialog, "OK");
		awaitDialogClosed(editNodeDialog, "edit-node dialog");
		return this;
	}

	/**
	 * Duplicates the tree node with the given display name through the real user flow: right-click,
	 * "Duplicate node...", type the duplicate's name into the "Name for duplicate" dialog, OK
	 */
	ApplicationSteps duplicateNode(org.assertj.swing.fixture.FrameFixture frame, String nodeName,
		String duplicateName)
	{
		rightClickTreeNodeByName(frame, nodeName);
		chooseFromShowingPopup("Duplicate node...");

		DialogFixture duplicateDialog = findDialogWithTitle("Name for duplicate");
		GuiActionRunner.execute(() -> duplicateDialog.textBox().target().setText(duplicateName));
		robot.waitForIdle();
		UiTestSpeed.step();
		clickDialogButton(duplicateDialog, "OK");
		awaitDialogClosed(duplicateDialog, "duplicate-node dialog");
		return this;
	}

	/**
	 * Deletes the tree node with the given display name through the real user flow: right-click,
	 * "delete", confirm the "Confirm deletion" dialog with OK
	 */
	ApplicationSteps deleteNode(org.assertj.swing.fixture.FrameFixture frame, String nodeName)
	{
		rightClickTreeNodeByName(frame, nodeName);
		chooseFromShowingPopup("delete");
		confirmOptionPaneDialog("Confirm deletion");
		return this;
	}

	/**
	 * Adds a password entry to the currently selected tree node through the real user flow:
	 * right-click the entries table, "add...", fill title, user name and password (with repeat) in
	 * the "New Crypt Entry" dialog, OK
	 */
	ApplicationSteps addEntry(org.assertj.swing.fixture.FrameFixture frame, String title,
		String userName, String password)
	{
		javax.swing.JTable table = frame.table().target();
		dispatchRightClick(table, 20, 10);
		chooseFromShowingPopup("add...");

		DialogFixture newEntryDialog = findDialogWithTitle("New Crypt Entry");
		GuiActionRunner.execute(() -> {
			newEntryDialog.textBox("txtEntryName").target().setText(title);
			newEntryDialog.textBox("txtUsername").target().setText(userName);
			newEntryDialog.textBox("txtPassword").target().setText(password);
			newEntryDialog.textBox("txtRepeat").target().setText(password);
		});
		robot.waitForIdle();
		UiTestSpeed.step();
		clickDialogButton(newEntryDialog, "OK");
		awaitDialogClosed(newEntryDialog, "new-crypt-entry dialog");
		return this;
	}

	/** Selects the tree row (left click semantics) so entry operations target that node */
	ApplicationSteps selectTreeRow(org.assertj.swing.fixture.FrameFixture frame, int row)
	{
		javax.swing.JTree tree = frame.tree().target();
		GuiActionRunner.execute(() -> tree.setSelectionRow(row));
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	/** Selects the visible tree node with the given display name so entry operations target it */
	ApplicationSteps selectTreeRowByName(org.assertj.swing.fixture.FrameFixture frame,
		String nodeName)
	{
		javax.swing.JTree tree = frame.tree().target();
		GuiActionRunner.execute(() -> {
			tree.expandRow(0);
			javax.swing.tree.TreePath rowPath = findTreePathByName(tree, nodeName);
			tree.setSelectionPath(rowPath);
			// the entries table refreshes on a left CLICK, not on selection alone - dispatch one
			// on the selected row so the table shows this node's entries
			java.awt.Rectangle rowBounds = tree.getPathBounds(rowPath);
			long now = System.currentTimeMillis();
			int x = rowBounds.x + rowBounds.width / 2;
			int y = rowBounds.y + rowBounds.height / 2;
			tree.dispatchEvent(
				new java.awt.event.MouseEvent(tree, java.awt.event.MouseEvent.MOUSE_PRESSED, now,
					java.awt.event.InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false,
					java.awt.event.MouseEvent.BUTTON1));
			tree.dispatchEvent(
				new java.awt.event.MouseEvent(tree, java.awt.event.MouseEvent.MOUSE_RELEASED, now,
					java.awt.event.InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false,
					java.awt.event.MouseEvent.BUTTON1));
			tree.dispatchEvent(
				new java.awt.event.MouseEvent(tree, java.awt.event.MouseEvent.MOUSE_CLICKED, now,
					java.awt.event.InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false,
					java.awt.event.MouseEvent.BUTTON1));
		});
		robot.waitForIdle();
		// the tree's click listener defers single-click handling by the multi-click interval
		// before it refreshes the entries table - give that timer room to fire
		Pause.pause(700);
		UiTestSpeed.step();
		return this;
	}

	private static javax.swing.tree.TreePath findTreePathByName(javax.swing.JTree tree,
		String nodeName)
	{
		for (int row = 0; row < tree.getRowCount(); row++)
		{
			javax.swing.tree.TreePath rowPath = tree.getPathForRow(row);
			Object lastComponent = rowPath.getLastPathComponent();
			if (lastComponent instanceof javax.swing.tree.DefaultMutableTreeNode treeNode
				&& treeNode.getUserObject()instanceof BaseTreeNode<?, ?> baseTreeNode
				&& baseTreeNode.getValue()instanceof GenericTreeElement<?> treeElement
				&& nodeName.equals(treeElement.getName()))
			{
				return rowPath;
			}
		}
		throw new IllegalStateException("tree has no visible node named '" + nodeName + "'");
	}

	/**
	 * Selects the entries-table row whose title column matches the given title (the context-menu
	 * actions on entries operate on the selected rows, a right-click alone selects nothing)
	 */
	ApplicationSteps selectEntryRowByTitle(org.assertj.swing.fixture.FrameFixture frame,
		String title)
	{
		javax.swing.JTable table = frame.table().target();
		GuiActionRunner.execute(() -> {
			int row = findEntryRow(table, title);
			table.setRowSelectionInterval(row, row);
		});
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	/**
	 * Edits the selected entry through the real user flow: right-click the selected row, "edit...",
	 * change the title in the "Edit Crypt Entry" dialog, OK
	 */
	ApplicationSteps editSelectedEntryTitle(org.assertj.swing.fixture.FrameFixture frame,
		String newTitle)
	{
		rightClickSelectedTableRow(frame);
		chooseFromShowingPopup("edit...");

		DialogFixture editDialog = findDialogWithTitle("Edit Crypt Entry");
		GuiActionRunner
			.execute(() -> editDialog.textBox("txtEntryName").target().setText(newTitle));
		robot.waitForIdle();
		UiTestSpeed.step();
		clickDialogButton(editDialog, "OK");
		awaitDialogClosed(editDialog, "edit-crypt-entry dialog");
		return this;
	}

	/**
	 * Duplicates the selected entry through the real user flow: right-click, "duplicate...", type
	 * the duplicate's title into the "New title for duplicate" dialog, OK
	 */
	ApplicationSteps duplicateSelectedEntry(org.assertj.swing.fixture.FrameFixture frame,
		String duplicateTitle)
	{
		rightClickSelectedTableRow(frame);
		chooseFromShowingPopup("duplicate...");

		DialogFixture duplicateDialog = findDialogWithTitle("New title for duplicate");
		GuiActionRunner.execute(() -> duplicateDialog.textBox().target().setText(duplicateTitle));
		robot.waitForIdle();
		UiTestSpeed.step();
		clickDialogButton(duplicateDialog, "OK");
		awaitDialogClosed(duplicateDialog, "duplicate-entry dialog");
		return this;
	}

	/**
	 * Deletes the selected entries through the real user flow: right-click, "delete", confirm the
	 * "Confirm deletion" dialog
	 */
	ApplicationSteps deleteSelectedEntry(org.assertj.swing.fixture.FrameFixture frame)
	{
		rightClickSelectedTableRow(frame);
		chooseFromShowingPopup("delete");
		confirmOptionPaneDialog("Confirm deletion");
		return this;
	}

	/** Copies the selected entry's user name to the system clipboard via the context menu */
	ApplicationSteps copyUsernameOfSelectedEntry(org.assertj.swing.fixture.FrameFixture frame)
	{
		rightClickSelectedTableRow(frame);
		chooseFromShowingPopup("Copy Username");
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	/** Copies the selected entry's password to the system clipboard via the context menu */
	ApplicationSteps copyPasswordOfSelectedEntry(org.assertj.swing.fixture.FrameFixture frame)
	{
		rightClickSelectedTableRow(frame);
		chooseFromShowingPopup("Copy Password");
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	/** Reads the current plain-text content of the system clipboard */
	String clipboardText()
	{
		return GuiActionRunner.execute(() -> (String)java.awt.Toolkit.getDefaultToolkit()
			.getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor));
	}

	private void rightClickSelectedTableRow(org.assertj.swing.fixture.FrameFixture frame)
	{
		javax.swing.JTable table = frame.table().target();
		GuiActionRunner.execute(() -> {
			int row = table.getSelectedRow();
			if (row < 0)
			{
				throw new IllegalStateException("no table row is selected");
			}
			java.awt.Rectangle cellRect = table.getCellRect(row, 0, true);
			table.scrollRectToVisible(cellRect);
			dispatchRightClickEvents(table, cellRect.x + cellRect.width / 2,
				cellRect.y + cellRect.height / 2);
		});
		robot.waitForIdle();
	}

	@SuppressWarnings("unchecked")
	private static int findEntryRow(javax.swing.JTable table, String title)
	{
		io.github.astrapi69.swing.table.model.GenericTableModel<MysticCryptEntryModelBean> tableModel = (io.github.astrapi69.swing.table.model.GenericTableModel<MysticCryptEntryModelBean>)table
			.getModel();
		List<MysticCryptEntryModelBean> data = tableModel.getData();
		for (int index = 0; index < data.size(); index++)
		{
			if (title.equals(data.get(index).getTitle()))
			{
				return table.convertRowIndexToView(index);
			}
		}
		throw new IllegalStateException("entries table has no row with title '" + title + "'");
	}

	/** Confirms the option-pane dialog with the given title by setting OK_OPTION as its value */
	private void confirmOptionPaneDialog(String title)
	{
		DialogFixture confirmDialog = findDialogWithTitle(title);
		// answer through the option pane's own API: a synthetic click on the OK button did not
		// translate into OK_OPTION here - setValue(OK_OPTION) is what the button's listener sets
		GuiActionRunner.execute(() -> {
			javax.swing.JOptionPane optionPane = (javax.swing.JOptionPane)robot.finder()
				.findByType(confirmDialog.target(), javax.swing.JOptionPane.class);
			optionPane.setValue(javax.swing.JOptionPane.OK_OPTION);
		});
		UiTestSpeed.step();
		awaitDialogClosed(confirmDialog, "'" + title + "' dialog");
	}

	/** Debug helper: prints every node name in the model tree to stderr */
	void printTreeNames(String label)
	{
		GuiActionRunner.execute(() -> {
			BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = MysticCryptApplicationFrame
				.getInstance().getApplicationPanel().getSecretKeyTreeWithContentPanel()
				.getModelObject();
			root.traverse().forEach(node -> System.err.println("### " + label + " node: '"
				+ (node.getValue() != null ? node.getValue().getName() : null) + "'"));
		});
	}

	/** True if any tree node's entry list contains an entry with exactly the given title */
	boolean entryExistsWithTitle(String title)
	{
		return GuiActionRunner.execute(() -> {
			BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = MysticCryptApplicationFrame
				.getInstance().getApplicationPanel().getSecretKeyTreeWithContentPanel()
				.getModelObject();
			return root.traverse().stream().anyMatch(
				node -> node.getValue() != null && node.getValue().getDefaultContent() != null
					&& node.getValue().getDefaultContent().stream()
						.anyMatch(entry -> title.equals(entry.getTitle())));
		});
	}

	/**
	 * Dispatches a synthetic right-click to the center of the given visible tree row - an OS-level
	 * robot right-click proved unreliable on this shared, live desktop display
	 */
	private void rightClickTreeRow(org.assertj.swing.fixture.FrameFixture frame, int row)
	{
		javax.swing.JTree tree = frame.tree().target();
		GuiActionRunner.execute(() -> {
			// make sure the target row is expanded, laid out and visible before resolving its
			// bounds - a reload() swaps the tree model and can leave row geometry stale
			tree.expandRow(0);
			javax.swing.tree.TreePath rowPath = tree.getPathForRow(row);
			if (rowPath == null)
			{
				throw new IllegalStateException("tree has no visible row " + row);
			}
			rightClickTreePath(tree, rowPath);
		});
		robot.waitForIdle();
	}

	/**
	 * Finds the visible tree row whose node carries the given display name (expanding the root
	 * first) and dispatches a synthetic right-click on it - row indices are deliberately not part
	 * of the step API, since a fresh database already ships with default nodes
	 */
	private void rightClickTreeNodeByName(org.assertj.swing.fixture.FrameFixture frame,
		String nodeName)
	{
		javax.swing.JTree tree = frame.tree().target();
		GuiActionRunner.execute(() -> {
			tree.expandRow(0);
			for (int row = 0; row < tree.getRowCount(); row++)
			{
				javax.swing.tree.TreePath rowPath = tree.getPathForRow(row);
				Object lastComponent = rowPath.getLastPathComponent();
				if (lastComponent instanceof javax.swing.tree.DefaultMutableTreeNode treeNode
					&& treeNode.getUserObject()instanceof BaseTreeNode<?, ?> baseTreeNode
					&& baseTreeNode.getValue()instanceof GenericTreeElement<?> treeElement
					&& nodeName.equals(treeElement.getName()))
				{
					rightClickTreePath(tree, rowPath);
					return;
				}
			}
			throw new IllegalStateException("tree has no visible node named '" + nodeName + "'");
		});
		robot.waitForIdle();
	}

	private static void rightClickTreePath(javax.swing.JTree tree,
		javax.swing.tree.TreePath rowPath)
	{
		tree.scrollPathToVisible(rowPath);
		java.awt.Rectangle rowBounds = tree.getPathBounds(rowPath);
		dispatchRightClickEvents(tree, rowBounds.x + rowBounds.width / 2,
			rowBounds.y + rowBounds.height / 2);
	}

	private void dispatchRightClick(javax.swing.JComponent component, int x, int y)
	{
		GuiActionRunner.execute(() -> dispatchRightClickEvents(component, x, y));
		robot.waitForIdle();
	}

	private static void dispatchRightClickEvents(java.awt.Component component, int x, int y)
	{
		long now = System.currentTimeMillis();
		component.dispatchEvent(
			new java.awt.event.MouseEvent(component, java.awt.event.MouseEvent.MOUSE_PRESSED, now,
				java.awt.event.InputEvent.BUTTON3_DOWN_MASK, x, y, 1, true,
				java.awt.event.MouseEvent.BUTTON3));
		component.dispatchEvent(
			new java.awt.event.MouseEvent(component, java.awt.event.MouseEvent.MOUSE_RELEASED, now,
				java.awt.event.InputEvent.BUTTON3_DOWN_MASK, x, y, 1, true,
				java.awt.event.MouseEvent.BUTTON3));
		component.dispatchEvent(
			new java.awt.event.MouseEvent(component, java.awt.event.MouseEvent.MOUSE_CLICKED, now,
				java.awt.event.InputEvent.BUTTON3_DOWN_MASK, x, y, 1, true,
				java.awt.event.MouseEvent.BUTTON3));
	}

	/**
	 * Waits for the context menu item with the given text (the click listeners defer single-click
	 * handling by the desktop's multi-click interval, so the popup appears delayed) and fires it
	 */
	private void chooseFromShowingPopup(String menuItemText)
	{
		GenericTypeMatcher<JMenuItem> itemMatcher = new GenericTypeMatcher<JMenuItem>(
			JMenuItem.class, true)
		{
			@Override
			protected boolean isMatching(JMenuItem candidate)
			{
				return menuItemText.equals(candidate.getText());
			}
		};
		Pause.pause(new Condition("context menu with '" + menuItemText + "' is showing")
		{
			@Override
			public boolean test()
			{
				try
				{
					robot.finder().find(itemMatcher);
					return true;
				}
				catch (org.assertj.swing.exception.ComponentLookupException notYetShowing)
				{
					return false;
				}
			}
		}, 10000);
		JMenuItem menuItem = robot.finder().find(itemMatcher);
		UiTestSpeed.step();
		SwingUtilities.invokeLater(menuItem::doClick);
	}

	private void awaitDialogClosed(DialogFixture dialog, String description)
	{
		Pause.pause(new Condition(description + " is closed")
		{
			@Override
			public boolean test()
			{
				return !dialog.target().isShowing();
			}
		}, 10000);
	}

	/** Saves the open database via the File menu and waits until the model is no longer dirty */
	ApplicationSteps saveDatabase()
	{
		clickMenuItem(MenuId.SAVE_APPLICATION_FILE.propertiesKey());
		Pause.pause(new Condition("model is saved (no longer dirty)")
		{
			@Override
			public boolean test()
			{
				return !MysticCryptApplicationFrame.getInstance().getModelObject().isDirty();
			}
		}, 15000);
		UiTestSpeed.step();
		return this;
	}

	/** True if the signed-in database tree contains a node whose name starts with the prefix */
	boolean treeContainsNodeStartingWith(String namePrefix)
	{
		return GuiActionRunner.execute(() -> {
			BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = MysticCryptApplicationFrame
				.getInstance().getApplicationPanel().getSecretKeyTreeWithContentPanel()
				.getModelObject();
			return root.traverse().stream()
				.anyMatch(node -> node.getValue() != null && node.getValue().getName() != null
					&& node.getValue().getName().startsWith(namePrefix));
		});
	}

	/** Finds a menu item by its stable name and fires it (the menu bar is never shown in tests) */
	private void clickMenuItem(String menuItemName)
	{
		JMenuItem menuItem = robot.finder().findByName(menuItemName, JMenuItem.class, false);
		SwingUtilities.invokeLater(menuItem::doClick);
	}

	/** Clicks the named Browse button in the dialog and picks the given file in the chooser */
	private void browseAndPickFile(DialogFixture dialog, String browseButtonName, File file)
	{
		SwingUtilities.invokeLater(() -> dialog.button(browseButtonName).target().doClick());
		javax.swing.JFileChooser fileChooser = org.assertj.swing.finder.JFileChooserFinder
			.findFileChooser().withTimeout(10, TimeUnit.SECONDS).using(robot).target();
		UiTestSpeed.step();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(file);
			fileChooser.approveSelection();
		});
		robot.waitForIdle();
	}

	private void clickDialogButton(DialogFixture dialog, String buttonText)
	{
		SwingUtilities.invokeLater(
			() -> dialog.button(JButtonMatcher.withText(buttonText)).target().doClick());
		UiTestSpeed.step();
	}

	/** Waits for the modal message dialog with the given title and closes it via its OK button */
	void dismissMessageDialog(String title)
	{
		DialogFixture messageDialog = findDialogWithTitle(title);
		clickDialogButton(messageDialog, "OK");
		Pause.pause(new Condition("message dialog '" + title + "' is closed")
		{
			@Override
			public boolean test()
			{
				return !messageDialog.target().isShowing();
			}
		}, 10000);
	}

	private DialogFixture findDialogWithTitle(String title)
	{
		DialogFixture dialog = WindowFinder.findDialog(new GenericTypeMatcher<Dialog>(Dialog.class)
		{
			@Override
			protected boolean isMatching(Dialog candidate)
			{
				return title.equals(candidate.getTitle()) && candidate.isShowing();
			}
		}).withTimeout(10, TimeUnit.SECONDS).using(robot);
		UiTestSpeed.step();
		return dialog;
	}
}
