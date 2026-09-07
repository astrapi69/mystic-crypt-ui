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

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.io.IOException;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Reproduces #237. Locking the workspace is the gesture someone makes when they step away from the
 * machine, so the property that matters is that nothing of the vault is on screen afterwards.
 * <p>
 * The existing lock tests do not pin that property. LockWorkspaceUiTest says "locking must hide the
 * content behind the desktop pane" and then asserts the frame MODE
 * ({@code assertEquals(FrameMode.DESKTOP_PANE, ...getFrameMode())}); LockCancelUnlockUiTest asserts
 * the {@code isSignedIn()} flag. Both stay green while the "Key database" frame, with the tree and
 * the entry panel, is still showing - which is what {@code switchToDesktopPane} puts back on the
 * desktop at MysticCryptApplicationFrame:527-530.
 */
class LockHidesVaultUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	/**
	 * Defect A: locking switches the frame mode but leaves the vault on screen, because
	 * LockWorkspaceAction:69 calls switchToDesktopPane, which re-opens the database frame at
	 * MysticCryptApplicationFrame:527-530
	 */
	@Test
	@DisplayName("A: locking the workspace takes the vault off the screen")
	void lockingTakesTheVaultOffTheScreen() throws IOException
	{
		File databaseFile = new File(tempHome, "lock-hides-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.addNodeToTreeRoot(frame, "SecretNodeA");

		application.lockWorkspace();

		assertFalse(application.isInternalFrameShowing("Key database"),
			"after locking, no frame carrying the vault may still be on screen - someone walking up "
				+ "to the machine must not be able to read the entries");
	}

	/**
	 * Defect B: cancelling the unlock prompt leaves the state reached by A. Whether this is a cause
	 * of its own or only a consequence is what this test decides: if A is fixed and this still
	 * fails, cancelling has its own path back into the visible state
	 */
	@Test
	@DisplayName("B: cancelling the unlock prompt leaves the vault off the screen")
	void cancellingUnlockLeavesTheVaultOffTheScreen() throws IOException
	{
		File databaseFile = new File(tempHome, "lock-cancel-hides-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.addNodeToTreeRoot(frame, "SecretNodeB");

		application.lockWorkspace();
		application.cancelUnlock();

		assertFalse(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn(),
			"precondition: cancelling keeps the workspace locked");
		assertFalse(application.isInternalFrameShowing("Key database"),
			"after cancelling the unlock prompt the vault must still be off the screen");
	}

	/**
	 * Question 1: was the vault merely visible after locking, or operable? The entry table is what
	 * carries the actions - a right click on it builds the context menu with "Copy Password"
	 * (SecretKeyTreeWithContentPanel:1207-1218, shown on getTblTreeEntryTable() at :1265, copying
	 * at :1360-1364). onEnableByPublic walks the menu bar and the toolbar
	 * (DesktopMenu:633,649-650), so a JPopupMenu built on the table is never disabled by locking:
	 * while that table is reachable, the action is triggerable.
	 * <p>
	 * The positive control in the same test is what makes the second assertion mean something: the
	 * identical lookup finds the table while unlocked. A lookup that never worked, or one
	 * swallowing an exception, fails there instead of passing silently.
	 * <p>
	 * What this test checks: that the component carrying the entry actions is reachable while
	 * unlocked and gone while locked. What it deliberately does NOT check: that a right click
	 * builds the menu and that "Copy Password" is actually fired - the entry table has no stable
	 * component name (setName is used only for four menu items,
	 * SecretKeyTreeWithContentPanel:470-513), the menu is rebuilt on every right click (:1265), and
	 * ApplicationSteps has no popup helper, so executing the click would mean building test
	 * infrastructure first. Tracked separately; do not read this test as proof that the action
	 * itself was fired.
	 * <p>
	 * Not asserted through the model: {@code treeContainsNodeStartingWith} reads
	 * {@code getApplicationPanel().getModelObject()}, a field that is never cleared, so it answers
	 * a question about memory rather than about reachability - that belongs to the memory hygiene
	 * issue, not here. The model may legitimately keep the content while locked, since unlocking
	 * builds the view from it again.
	 */
	@Test
	@DisplayName("the table that carries the entry actions is reachable unlocked and gone when locked")
	void theEntryActionsAreNotTriggerableWhileLocked() throws IOException
	{
		File databaseFile = new File(tempHome, "lock-reachable-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.addNodeToTreeRoot(frame, "SecretNodeC");

		assertTrue(entryTableIsReachable(),
			"positive control: while unlocked the entry table must be reachable, otherwise the "
				+ "assertion below would pass for the wrong reason");

		application.lockWorkspace();

		assertFalse(entryTableIsReachable(),
			"while locked the entry table must be gone - as long as it is reachable, its context "
				+ "menu builds and Copy Password stays triggerable");
	}

	/**
	 * Whether the entry table is in the showing component hierarchy of the application frame. Read
	 * on the event dispatch thread, and returning false rather than throwing when it is not there,
	 * so that a missing table is a result and not an error the test could swallow
	 *
	 * @return true if the table that carries the entry context menu is on screen
	 */
	private static boolean entryTableIsReachable()
	{
		return GuiActionRunner.execute(() -> {
			MysticCryptApplicationFrame frame = MysticCryptApplicationFrame.getInstance();
			return frame != null && containsShowingEntryTable(frame.getContentPane());
		});
	}

	private static boolean containsShowingEntryTable(final Component component)
	{
		if (component instanceof javax.swing.JTable table && table.isShowing())
		{
			return true;
		}
		if (component instanceof Container container)
		{
			for (Component child : container.getComponents())
			{
				if (containsShowingEntryTable(child))
				{
					return true;
				}
			}
		}
		return false;
	}
}
