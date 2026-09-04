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

import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.mystic.crypt.MenuId;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.base.BaseMenuId;
import io.github.astrapi69.swing.enumeration.FrameMode;
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
	 * Drives the KeePass import dialog like {@link #importKeePassDatabase} but additionally
	 * supplies a key file: it enables the "Key File" checkbox and browses to the given key file, so
	 * the import builds a password + key-file {@code KdbxCreds}.
	 *
	 * @param keePassFile
	 *            the {@code .kdbx} file to import
	 * @param password
	 *            the database password
	 * @param keyFile
	 *            the key file protecting the database
	 * @return this
	 */
	ApplicationSteps importKeePassDatabaseWithKeyFile(File keePassFile, String password,
		File keyFile)
	{
		clickMenuItem(MenuId.IMPORT_KEEPASS.propertiesKey());

		DialogFixture importDialog = findDialogWithTitle("Import from KeePass");
		browseAndPickFile(importDialog, "btnFile", keePassFile);
		GuiActionRunner
			.execute(() -> importDialog.textBox("txtPassword").target().setText(password));
		// doClick (not setSelected) so the checkbox's listener fires and enables the key-file
		// browser
		GuiActionRunner.execute(() -> importDialog.checkBox("cbxKeyFile").target().doClick());
		robot.waitForIdle();
		browseAndPickFile(importDialog, "btnKeyFile", keyFile);
		robot.waitForIdle();
		UiTestSpeed.step();
		clickDialogButton(importDialog, "OK");

		dismissMessageDialog("Import successful");
		return this;
	}

	/**
	 * Drives the KeePass import dialog exactly like {@link #importKeePassDatabase} but with a wrong
	 * password, and closes the resulting "Import failed" error dialog - the negative use case
	 */
	ApplicationSteps importKeePassDatabaseExpectingFailure(File keePassFile, String wrongPassword)
	{
		clickMenuItem(MenuId.IMPORT_KEEPASS.propertiesKey());

		DialogFixture importDialog = findDialogWithTitle("Import from KeePass");
		browseAndPickFile(importDialog, "btnFile", keePassFile);
		GuiActionRunner
			.execute(() -> importDialog.textBox("txtPassword").target().setText(wrongPassword));
		robot.waitForIdle();
		UiTestSpeed.step();
		clickDialogButton(importDialog, "OK");

		dismissMessageDialog("Import failed");
		return this;
	}

	/** Opens the KeePass import dialog via the File menu and cancels it - nothing is imported */
	ApplicationSteps importKeePassCancel()
	{
		clickMenuItem(MenuId.IMPORT_KEEPASS.propertiesKey());
		// cancel through the option pane's value (locale-independent: the button reads "Abbrechen"
		// under the app's German locale, not "Cancel")
		cancelOptionPaneDialog("Import from KeePass");
		return this;
	}

	/** Chooses the panel view in the settings and waits until the frame is in it */
	ApplicationSteps switchToPanelMode()
	{
		chooseViewMode(FrameMode.APPLICATION_PANEL);
		return this;
	}

	/** Chooses the desktop view in the settings and waits until the frame is in it */
	ApplicationSteps switchToDesktopMode()
	{
		chooseViewMode(FrameMode.DESKTOP_PANE);
		return this;
	}

	/**
	 * Picks the given view in the settings dialog and closes it, which is what saves the choice and
	 * puts the frame into that view
	 *
	 * @param viewMode
	 *            the view to choose
	 */
	ApplicationSteps chooseViewMode(final FrameMode viewMode)
	{
		DialogFixture settings = openSettingsDialog();
		settings.tabbedPane("tabSettings").selectTab("General");
		GuiActionRunner
			.execute(() -> settings.comboBox("cmbViewMode").target().setSelectedItem(viewMode));
		robot.waitForIdle();
		settings.button("btnCloseSettings").click();
		awaitFrameMode(viewMode);
		return this;
	}

	private void awaitFrameMode(FrameMode expected)
	{
		Pause.pause(new Condition("frame is in " + expected + " mode")
		{
			@Override
			public boolean test()
			{
				return MysticCryptApplicationFrame.getInstance().getFrameMode() == expected;
			}
		}, 10000);
		UiTestSpeed.step();
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

	/** Opens the KeePass export dialog via the File menu and cancels it - nothing is exported */
	ApplicationSteps exportKeePassCancel()
	{
		clickMenuItem(MenuId.EXPORT_KEEPASS.propertiesKey());
		cancelOptionPaneDialog("Export to KeePass");
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
		rightClickBelowTheTreeNodes(frame);
		chooseFromShowingPopup("add node...");

		DialogFixture newNodeDialog = findDialogWithTitle("New node");
		GuiActionRunner.execute(() -> newNodeDialog.textBox().target().setText(name));
		robot.waitForIdle();
		UiTestSpeed.step();
		clickDialogButton(newNodeDialog, "OK");
		awaitDialogClosed(newNodeDialog, "new-node dialog");
		return this;
	}

	/** Opens the "New node" dialog and closes it without confirming - no node is added */
	ApplicationSteps addNodeButCancel(org.assertj.swing.fixture.FrameFixture frame)
	{
		rightClickBelowTheTreeNodes(frame);
		chooseFromShowingPopup("add node...");
		disposeDialog("New node");
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
	 * Moves the tree node with the given display name one position up among its siblings, through
	 * the real user flow: right-click, "Move up"
	 */
	ApplicationSteps moveNodeUp(org.assertj.swing.fixture.FrameFixture frame, String nodeName)
	{
		rightClickTreeNodeByName(frame, nodeName);
		chooseFromShowingPopup("Move up");
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	/** Moves the tree node one position down among its siblings */
	ApplicationSteps moveNodeDown(org.assertj.swing.fixture.FrameFixture frame, String nodeName)
	{
		rightClickTreeNodeByName(frame, nodeName);
		chooseFromShowingPopup("Move down");
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	/**
	 * Whether the context menu of the given node offers the entry with the given text, and whether
	 * it is enabled - the popup is closed again afterwards
	 */
	boolean treeContextMenuItemIsEnabled(org.assertj.swing.fixture.FrameFixture frame,
		String nodeName, String menuItemText)
	{
		rightClickTreeNodeByName(frame, nodeName);
		GenericTypeMatcher<JMenuItem> itemMatcher = new GenericTypeMatcher<JMenuItem>(
			JMenuItem.class, true)
		{
			@Override
			protected boolean isMatching(JMenuItem candidate)
			{
				return menuItemText.equals(candidate.getText());
			}
		};
		// the popup appears delayed, exactly like it does for chooseFromShowingPopup
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
		boolean enabled = GuiActionRunner.execute(menuItem::isEnabled);
		// close the popup again, so the next right-click starts from a clean state
		GuiActionRunner.execute(() -> {
			java.awt.Container parent = menuItem.getParent();
			if (parent instanceof javax.swing.JPopupMenu popupMenu)
			{
				popupMenu.setVisible(false);
			}
		});
		robot.waitForIdle();
		UiTestSpeed.step();
		return enabled;
	}

	/**
	 * Whether the context menu of the given node offers an entry with the given text at all - the
	 * popup is closed again afterwards
	 */
	boolean treeContextMenuHasItem(org.assertj.swing.fixture.FrameFixture frame, String nodeName,
		String menuItemText)
	{
		rightClickTreeNodeByName(frame, nodeName);
		GenericTypeMatcher<JMenuItem> itemMatcher = new GenericTypeMatcher<JMenuItem>(
			JMenuItem.class, true)
		{
			@Override
			protected boolean isMatching(JMenuItem candidate)
			{
				return menuItemText.equals(candidate.getText());
			}
		};
		// wait for the popup itself, not for the entry: the entry is what is being asked about
		Pause.pause(new Condition("context menu of '" + nodeName + "' is showing")
		{
			@Override
			public boolean test()
			{
				return !robot.finder().findAll(new GenericTypeMatcher<javax.swing.JPopupMenu>(
					javax.swing.JPopupMenu.class, true)
				{
					@Override
					protected boolean isMatching(javax.swing.JPopupMenu candidate)
					{
						return true;
					}
				}).isEmpty();
			}
		}, 10000);
		boolean present;
		try
		{
			robot.finder().find(itemMatcher);
			present = true;
		}
		catch (org.assertj.swing.exception.ComponentLookupException notThere)
		{
			present = false;
		}
		GuiActionRunner.execute(() -> robot.finder().findAll(
			new GenericTypeMatcher<javax.swing.JPopupMenu>(javax.swing.JPopupMenu.class, true)
			{
				@Override
				protected boolean isMatching(javax.swing.JPopupMenu candidate)
				{
					return true;
				}
			}).forEach(popupMenu -> popupMenu.setVisible(false)));
		robot.waitForIdle();
		UiTestSpeed.step();
		return present;
	}

	/**
	 * Moves the tree node with the given display name under another node, through the real user
	 * flow: right-click, "Move to node...", pick the target whose path ends with the given name, OK
	 */
	ApplicationSteps moveNodeUnder(org.assertj.swing.fixture.FrameFixture frame, String nodeName,
		String targetNodeName)
	{
		rightClickTreeNodeByName(frame, nodeName);
		chooseFromShowingPopup("Move to node...");

		DialogFixture moveDialog = findDialogWithTitle("Move node");
		GuiActionRunner.execute(() -> {
			javax.swing.JComboBox<?> chooser = moveDialog.comboBox("cmbMoveTarget").target();
			for (int index = 0; index < chooser.getItemCount(); index++)
			{
				Object item = chooser.getItemAt(index);
				if (String.valueOf(item).contains("displayValue=" + targetNodeName + ","))
				{
					chooser.setSelectedIndex(index);
					return;
				}
			}
			throw new IllegalStateException("no move target named '" + targetNodeName + "' among "
				+ chooser.getItemCount() + " targets");
		});
		robot.waitForIdle();
		UiTestSpeed.step();
		clickDialogButton(moveDialog, "OK");
		awaitDialogClosed(moveDialog, "move-node dialog");
		return this;
	}

	/**
	 * Selects the tree row with the given display name and puts the keyboard focus on the tree, so
	 * that the next key press lands where the user would have put it
	 *
	 * @param name
	 *            the display name of the row
	 */
	ApplicationSteps selectTreeNodeByNameAndFocus(final String name)
	{
		GuiActionRunner.execute(() -> {
			javax.swing.JTree tree = MysticCryptApplicationFrame.getInstance().getApplicationPanel()
				.getSecretKeyTreeWithContentPanel().getTree();
			for (int row = 0; row < tree.getRowCount(); row++)
			{
				javax.swing.tree.TreePath path = tree.getPathForRow(row);
				if (path
					.getLastPathComponent()instanceof javax.swing.tree.DefaultMutableTreeNode node
					&& node.getUserObject()instanceof BaseTreeNode<?, ?> treeNode
					&& name.equals(String.valueOf(treeNode.getDisplayValue())))
				{
					tree.setSelectionPath(path);
					tree.requestFocusInWindow();
					return;
				}
			}
		});
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	/**
	 * Presses the given key on the tree, the way a user does with the tree focused
	 *
	 * @param keyCode
	 *            the key to press
	 */
	ApplicationSteps pressOnTree(final int keyCode)
	{
		GuiActionRunner.execute(() -> {
			javax.swing.JTree tree = MysticCryptApplicationFrame.getInstance().getApplicationPanel()
				.getSecretKeyTreeWithContentPanel().getTree();
			Object actionKey = tree.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
				.get(javax.swing.KeyStroke.getKeyStroke(keyCode, 0));
			javax.swing.Action action = actionKey == null
				? null
				: tree.getActionMap().get(actionKey);
			if (action != null)
			{
				action.actionPerformed(null);
			}
		});
		robot.waitForIdle();
		return this;
	}

	/** Whether the tree shows a row with the given display name */
	boolean treeShowsARowNamed(String name)
	{
		return GuiActionRunner.execute(() -> {
			javax.swing.JTree tree = MysticCryptApplicationFrame.getInstance().getApplicationPanel()
				.getSecretKeyTreeWithContentPanel().getTree();
			for (int row = 0; row < tree.getRowCount(); row++)
			{
				Object last = tree.getPathForRow(row).getLastPathComponent();
				if (last instanceof javax.swing.tree.DefaultMutableTreeNode node
					&& String.valueOf(node.getUserObject()).contains("displayValue=" + name + ","))
				{
					return true;
				}
			}
			return false;
		});
	}

	/** The display names of the rows the tree shows on its top level */
	List<String> treeTopLevelNames()
	{
		return GuiActionRunner.execute(() -> {
			javax.swing.JTree tree = MysticCryptApplicationFrame.getInstance().getApplicationPanel()
				.getSecretKeyTreeWithContentPanel().getTree();
			javax.swing.tree.TreeModel model = tree.getModel();
			Object root = model.getRoot();
			List<String> names = new java.util.ArrayList<>();
			for (int index = 0; index < model.getChildCount(root); index++)
			{
				Object child = model.getChild(root, index);
				if (child instanceof javax.swing.tree.DefaultMutableTreeNode node
					&& node.getUserObject()instanceof BaseTreeNode<?, ?> treeNode)
				{
					names.add(String.valueOf(treeNode.getDisplayValue()));
				}
			}
			return names;
		});
	}

	/** The display names of the children of the tree root, in the order the tree shows them */
	List<String> treeRootChildNames()
	{
		return GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance()
			.getApplicationPanel().getSecretKeyTreeWithContentPanel().getModelObject().getChildren()
			.stream().map(child -> String.valueOf(child.getDisplayValue())).toList());
	}

	/** The display names of the children of the node with the given name */
	List<String> treeChildNamesOf(String nodeName)
	{
		return GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getApplicationPanel()
				.getSecretKeyTreeWithContentPanel().getModelObject().traverse().stream()
				.filter(node -> nodeName.equals(String.valueOf(node.getDisplayValue()))).findFirst()
				.map(node -> node.getChildren().stream()
					.map(child -> String.valueOf(child.getDisplayValue())).toList())
				.orElseThrow(() -> new IllegalStateException("no node named '" + nodeName + "'")));
	}

	/**
	 * Opens the "Edit node" dialog for the node and closes it without confirming - name unchanged
	 */
	ApplicationSteps editNodeButCancel(org.assertj.swing.fixture.FrameFixture frame,
		String nodeName)
	{
		rightClickTreeNodeByName(frame, nodeName);
		chooseFromShowingPopup("Edit node...");
		disposeDialog("Edit node");
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

	/** Opens the "Name for duplicate" dialog and closes it without confirming - no duplicate */
	ApplicationSteps duplicateNodeButCancel(org.assertj.swing.fixture.FrameFixture frame,
		String nodeName)
	{
		rightClickTreeNodeByName(frame, nodeName);
		chooseFromShowingPopup("Duplicate node...");
		disposeDialog("Name for duplicate");
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
	 * Starts deleting the tree node but cancels the "Confirm deletion" dialog - the node must
	 * survive
	 */
	ApplicationSteps deleteNodeButCancel(org.assertj.swing.fixture.FrameFixture frame,
		String nodeName)
	{
		rightClickTreeNodeByName(frame, nodeName);
		chooseFromShowingPopup("delete");
		cancelOptionPaneDialog("Confirm deletion");
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

	/**
	 * Opens the "New Crypt Entry" dialog, types a title but closes without confirming - no entry
	 */
	ApplicationSteps addEntryButCancel(org.assertj.swing.fixture.FrameFixture frame, String title)
	{
		javax.swing.JTable table = frame.table().target();
		dispatchRightClick(table, 20, 10);
		chooseFromShowingPopup("add...");

		DialogFixture newEntryDialog = findDialogWithTitle("New Crypt Entry");
		GuiActionRunner
			.execute(() -> newEntryDialog.textBox("txtEntryName").target().setText(title));
		robot.waitForIdle();
		UiTestSpeed.step();
		disposeDialog("New Crypt Entry");
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
			for (int row = 0; row < tree.getRowCount(); row++)
			{
				tree.expandRow(row);
			}
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

	/**
	 * Starts deleting the selected entry but cancels the "Confirm deletion" dialog - the entry must
	 * survive
	 */
	ApplicationSteps deleteSelectedEntryButCancel(org.assertj.swing.fixture.FrameFixture frame)
	{
		rightClickSelectedTableRow(frame);
		chooseFromShowingPopup("delete");
		cancelOptionPaneDialog("Confirm deletion");
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

	/** Cancels the option-pane dialog with the given title by setting CANCEL_OPTION as its value */
	private void cancelOptionPaneDialog(String title)
	{
		DialogFixture confirmDialog = findDialogWithTitle(title);
		GuiActionRunner.execute(() -> {
			javax.swing.JOptionPane optionPane = (javax.swing.JOptionPane)robot.finder()
				.findByType(confirmDialog.target(), javax.swing.JOptionPane.class);
			optionPane.setValue(javax.swing.JOptionPane.CANCEL_OPTION);
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
	/**
	 * Right-clicks the empty area below the nodes. The tree hides its root, so this is the place
	 * the application offers "add node..." for the top level
	 */
	private void rightClickBelowTheTreeNodes(org.assertj.swing.fixture.FrameFixture frame)
	{
		javax.swing.JTree tree = frame.tree().target();
		GuiActionRunner.execute(() -> {
			int belowLastRow = tree.getRowCount() == 0
				? 4
				: tree.getRowBounds(tree.getRowCount() - 1).y
					+ tree.getRowBounds(tree.getRowCount() - 1).height + 8;
			long now = System.currentTimeMillis();
			tree.dispatchEvent(
				new java.awt.event.MouseEvent(tree, java.awt.event.MouseEvent.MOUSE_CLICKED, now,
					java.awt.event.InputEvent.BUTTON3_DOWN_MASK, 8, belowLastRow, 1, true,
					java.awt.event.MouseEvent.BUTTON3));
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
			for (int row = 0; row < tree.getRowCount(); row++)
			{
				tree.expandRow(row);
			}
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

	/** Closes the dialog with the given title without confirming it (disposes it) */
	private void disposeDialog(String title)
	{
		DialogFixture dialog = findDialogWithTitle(title);
		GuiActionRunner.execute(() -> dialog.target().dispose());
		awaitDialogClosed(dialog, "'" + title + "' dialog");
	}

	/** The number of nodes in the signed-in database tree (root included) */
	int treeNodeCount()
	{
		return GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getApplicationPanel()
				.getSecretKeyTreeWithContentPanel().getModelObject().traverse().size());
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

	/**
	 * Saves the open database to a new file via the File menu's "Save As": fires the menu item,
	 * picks the given target in the file chooser and waits until it has been written to disk
	 */
	ApplicationSteps saveAsDatabase(File target)
	{
		clickMenuItem(MenuId.SAVE_AS_APPLICATION_FILE.propertiesKey());
		javax.swing.JFileChooser fileChooser = org.assertj.swing.finder.JFileChooserFinder
			.findFileChooser().withTimeout(10, TimeUnit.SECONDS).using(robot).target();
		UiTestSpeed.step();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(target);
			fileChooser.approveSelection();
		});
		Pause.pause(new Condition("save-as target file '" + target.getName() + "' is written")
		{
			@Override
			public boolean test()
			{
				return target.exists() && target.length() > 0;
			}
		}, 15000);
		UiTestSpeed.step();
		return this;
	}

	/** Fires "Save As" via the File menu but cancels the file chooser - nothing is written */
	ApplicationSteps saveAsCancel()
	{
		clickMenuItem(MenuId.SAVE_AS_APPLICATION_FILE.propertiesKey());
		javax.swing.JFileChooser fileChooser = org.assertj.swing.finder.JFileChooserFinder
			.findFileChooser().withTimeout(10, TimeUnit.SECONDS).using(robot).target();
		UiTestSpeed.step();
		SwingUtilities.invokeLater(fileChooser::cancelSelection);
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	/** Locks the workspace via the File menu and waits until the model is no longer signed in */
	ApplicationSteps lockWorkspace()
	{
		clickMenuItem(MenuId.LOCK_WORKSPACE.propertiesKey());
		Pause.pause(new Condition("workspace is locked (model no longer signed in)")
		{
			@Override
			public boolean test()
			{
				return !MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn();
			}
		}, 10000);
		UiTestSpeed.step();
		return this;
	}

	/**
	 * Enters the given password into the "Unlock workspace" dialog, confirms and waits until the
	 * model is signed in again
	 */
	ApplicationSteps unlockWorkspace(String password)
	{
		DialogFixture unlockDialog = findDialogWithTitle("Unlock workspace");
		GuiActionRunner
			.execute(() -> unlockDialog.textBox("txtUnlockPassword").target().setText(password));
		robot.waitForIdle();
		UiTestSpeed.step();
		clickDialogButton(unlockDialog, "OK");
		Pause.pause(new Condition("workspace is unlocked (model signed in)")
		{
			@Override
			public boolean test()
			{
				return MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn();
			}
		}, 10000);
		// the unlock sets the signed-in flag before switching the frame back to the application
		// panel - wait for the EDT to finish that switch before callers assert on the frame mode
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	/**
	 * Enters a wrong password into the "Unlock workspace" dialog, confirms and closes the resulting
	 * "Unlock failed" error dialog - the workspace stays locked
	 */
	ApplicationSteps enterUnlockPasswordExpectingFailure(String wrongPassword)
	{
		DialogFixture unlockDialog = findDialogWithTitle("Unlock workspace");
		GuiActionRunner.execute(
			() -> unlockDialog.textBox("txtUnlockPassword").target().setText(wrongPassword));
		robot.waitForIdle();
		UiTestSpeed.step();
		clickDialogButton(unlockDialog, "OK");
		dismissMessageDialog("Unlock failed");
		return this;
	}

	/** Cancels the "Unlock workspace" dialog - the workspace stays locked */
	ApplicationSteps cancelUnlock()
	{
		// cancel through the option pane's value (locale-independent: the button reads "Abbrechen"
		// under the app's German locale, not "Cancel")
		cancelOptionPaneDialog("Unlock workspace");
		return this;
	}

	/** Opens the settings dialog via the File menu and returns a fixture for it */
	DialogFixture openSettingsDialog()
	{
		clickMenuItem(MenuId.SETTINGS.propertiesKey());
		return findDialogWithTitle("Settings");
	}

	/**
	 * Opens the certificate wizard via the certificate plugin's "Create Certificate..." menu item
	 * (matched by text, like the other plugin tools) and returns a fixture for its dialog
	 */
	DialogFixture openCertificateWizard()
	{
		JMenuItem menuItem = robot.finder()
			.find(new GenericTypeMatcher<JMenuItem>(JMenuItem.class, false)
			{
				@Override
				protected boolean isMatching(JMenuItem candidate)
				{
					return !(candidate instanceof javax.swing.JMenu)
						&& "Create Certificate...".equals(candidate.getText());
				}
			});
		SwingUtilities.invokeLater(menuItem::doClick);
		return findDialogWithTitle("Create Certificate");
	}

	/**
	 * Opens the conversion wizard via the conversion plugin's "Convert Key/Certificate..." menu
	 * item (matched by text, like the other plugin tools) and returns a fixture for its dialog
	 */
	DialogFixture openConversionWizard()
	{
		JMenuItem menuItem = robot.finder()
			.find(new GenericTypeMatcher<JMenuItem>(JMenuItem.class, false)
			{
				@Override
				protected boolean isMatching(JMenuItem candidate)
				{
					return !(candidate instanceof javax.swing.JMenu)
						&& "Convert Key/Certificate...".equals(candidate.getText());
				}
			});
		SwingUtilities.invokeLater(menuItem::doClick);
		return findDialogWithTitle("Convert Key or Certificate File");
	}

	/** Clicks the Help menu's Donate item, which shows a popup of donation targets */
	void clickDonateMenuItem()
	{
		clickMenuItem(BaseMenuId.HELP_DONATE.propertiesKey());
	}

	/** Opens the Help menu's info/about dialog and returns a fixture for the shown dialog */
	DialogFixture openHelpInfoDialog()
	{
		clickMenuItem(BaseMenuId.HELP_INFO.propertiesKey());
		DialogFixture dialog = WindowFinder.findDialog(new GenericTypeMatcher<Dialog>(Dialog.class)
		{
			@Override
			protected boolean isMatching(Dialog candidate)
			{
				return candidate.isShowing();
			}
		}).withTimeout(10, TimeUnit.SECONDS).using(robot);
		UiTestSpeed.step();
		return dialog;
	}

	/** Opens the Search dialog via the File menu, enters the term and confirms with OK */
	ApplicationSteps searchFor(String term)
	{
		clickMenuItem(MenuId.SEARCH.propertiesKey());
		DialogFixture searchDialog = findDialogWithTitle("Search");
		GuiActionRunner.execute(() -> searchDialog.textBox("txtSearch").target().setText(term));
		robot.waitForIdle();
		UiTestSpeed.step();
		clickDialogButton(searchDialog, "OK");
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	/**
	 * Opens the Search dialog, enters a term that matches nothing and closes the resulting "No
	 * match" info dialog
	 */
	ApplicationSteps searchExpectingNoMatch(String term)
	{
		clickMenuItem(MenuId.SEARCH.propertiesKey());
		DialogFixture searchDialog = findDialogWithTitle("Search");
		GuiActionRunner.execute(() -> searchDialog.textBox("txtSearch").target().setText(term));
		robot.waitForIdle();
		UiTestSpeed.step();
		clickDialogButton(searchDialog, "OK");
		dismissMessageDialog("No match");
		return this;
	}

	/**
	 * The display name of the currently selected tree node, or {@code null} if nothing is selected
	 */
	String selectedTreeNodeName()
	{
		return GuiActionRunner.execute(() -> {
			javax.swing.JTree tree = MysticCryptApplicationFrame.getInstance().getApplicationPanel()
				.getSecretKeyTreeWithContentPanel().getTree();
			javax.swing.tree.TreePath path = tree.getSelectionPath();
			if (path == null)
			{
				return null;
			}
			Object lastComponent = path.getLastPathComponent();
			if (lastComponent instanceof javax.swing.tree.DefaultMutableTreeNode treeNode
				&& treeNode.getUserObject()instanceof BaseTreeNode<?, ?> baseTreeNode
				&& baseTreeNode.getValue()instanceof GenericTreeElement<?> treeElement)
			{
				return treeElement.getName();
			}
			return null;
		});
	}

	/** The file the signed-in application believes it has open, as its model holds it */
	File applicationFileOnScreen()
	{
		return GuiActionRunner.execute(() -> FileInfo.toFile(MysticCryptApplicationFrame
			.getInstance().getModelObject().getMasterPwFileModelBean().getApplicationFileInfo()));
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

	/**
	 * Fires the menu item with the given stable name and waits for the internal frame with the
	 * given title to appear on the desktop pane
	 */
	ApplicationSteps openInternalFrameViaMenu(String menuItemName, String internalFrameTitle)
	{
		clickMenuItem(menuItemName);
		Pause.pause(new Condition("internal frame '" + internalFrameTitle + "' is open")
		{
			@Override
			public boolean test()
			{
				return findInternalFrameByTitle(internalFrameTitle) != null;
			}
		}, 10000);
		UiTestSpeed.step();
		return this;
	}

	/**
	 * Opens a private key through the File menu's "Open private key": the menu action directly
	 * shows a file chooser; picking the given PEM file opens the "Private key view" internal frame
	 */
	ApplicationSteps openPrivateKeyViaMenu(File privateKeyPemFile)
	{
		clickMenuItem(MenuId.OPEN_PRIVATE_KEY.propertiesKey());
		javax.swing.JFileChooser fileChooser = org.assertj.swing.finder.JFileChooserFinder
			.findFileChooser().withTimeout(10, TimeUnit.SECONDS).using(robot).target();
		UiTestSpeed.step();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(privateKeyPemFile);
			fileChooser.approveSelection();
		});
		Pause.pause(new Condition("internal frame 'Private key view' is open")
		{
			@Override
			public boolean test()
			{
				return findInternalFrameByTitle("Private key view") != null;
			}
		}, 15000);
		UiTestSpeed.step();
		return this;
	}

	/**
	 * Fires the Plugins-menu item with the given text (plugin menu items have no stable name, so
	 * they are matched by text) and waits for the internal frame with the given title to appear -
	 * the entry point for a plugin's end-to-end test
	 */
	ApplicationSteps openPluginTool(String menuItemText, String internalFrameTitle)
	{
		JMenuItem menuItem = robot.finder()
			.find(new GenericTypeMatcher<JMenuItem>(JMenuItem.class, false)
			{
				@Override
				protected boolean isMatching(JMenuItem candidate)
				{
					// exclude JMenu: a single-item plugin's submenu header can share the item's
					// text (e.g. "Console"), and only the leaf item opens the tool
					return !(candidate instanceof javax.swing.JMenu)
						&& menuItemText.equals(candidate.getText());
				}
			});
		SwingUtilities.invokeLater(menuItem::doClick);
		Pause.pause(new Condition("plugin internal frame '" + internalFrameTitle + "' is open")
		{
			@Override
			public boolean test()
			{
				return findInternalFrameByTitle(internalFrameTitle) != null;
			}
		}, 10000);
		UiTestSpeed.step();
		return this;
	}

	/**
	 * Closes the internal frame with the given title (so equally titled frames stay unambiguous)
	 */
	ApplicationSteps closeInternalFrame(String internalFrameTitle)
	{
		GuiActionRunner.execute(() -> {
			javax.swing.JInternalFrame internalFrame = findInternalFrameByTitle(internalFrameTitle);
			if (internalFrame != null)
			{
				internalFrame.dispose();
			}
		});
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	/** True if an internal frame with the given title is currently open on the desktop pane */
	boolean isInternalFrameShowing(String title)
	{
		return GuiActionRunner.execute(() -> findInternalFrameByTitle(title) != null);
	}

	/**
	 * The internal frame with the given title, for a test that needs to inspect it directly (its
	 * bounds, for instance) rather than just knowing it is there
	 */
	javax.swing.JInternalFrame internalFrame(String title)
	{
		return GuiActionRunner.execute(() -> findInternalFrameByTitle(title));
	}

	private static javax.swing.JInternalFrame findInternalFrameByTitle(String title)
	{
		MysticCryptApplicationFrame applicationFrame = MysticCryptApplicationFrame.getInstance();
		if (applicationFrame == null || applicationFrame.getDesktopPanePanel() == null)
		{
			return null;
		}
		for (javax.swing.JInternalFrame internalFrame : applicationFrame.getDesktopPanePanel()
			.getDesktopPane().getAllFrames())
		{
			if (title.equals(internalFrame.getTitle()) && internalFrame.isVisible())
			{
				return internalFrame;
			}
		}
		return null;
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

	DialogFixture findDialogWithTitle(String title)
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
