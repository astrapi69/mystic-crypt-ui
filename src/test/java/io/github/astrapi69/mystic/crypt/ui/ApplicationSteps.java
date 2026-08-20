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
