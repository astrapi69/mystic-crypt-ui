/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.action.OpenExistingDatabaseAction;
import io.github.astrapi69.mystic.crypt.lock.IdleLockDecision;
import io.github.astrapi69.mystic.crypt.lock.IdleLockWatchdog;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * A locked vault does not stay decrypted forever (#242).
 * <p>
 * Locking keeps the model and the panel so unlocking can rebuild the view without reading and
 * decrypting the file again (#237), and nothing bounded that: a vault locked at five was still
 * decrypted in the process the next morning. Wiping it where it lies is not available - an entry's
 * title, user name, URL and notes are {@code String}s, and a String cannot be overwritten in Java -
 * so the only way the plaintext leaves memory is for the model to be dropped, which is what closing
 * does.
 * <p>
 * This measures the MEMORY, not the screen. {@code LockHidesVaultUiTest} deliberately does not
 * assert the model, because after locking the entries are still there - that is the fact this test
 * is about, from the other side: after the close they are not.
 */
class LockedVaultClosesItselfUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String ENTRY_TITLE = "still-in-memory-while-locked";

	@Test
	@DisplayName("the decrypted vault is still in memory while locked, and gone after the close")
	void aLockedVaultIsClosedOnceItHasBeenLockedLongEnough() throws IOException
	{
		File databaseFile = new File(tempHome, "locked-closes-itself.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, ENTRY_TITLE, "someone",
			TestPasswords.throwaway());
		application.saveDatabase();
		application.lockWorkspace();
		application.cancelUnlock();

		assertTrue(theModelStillHolds(ENTRY_TITLE),
			"precondition, and the whole reason this issue exists: locking hides the vault and "
				+ "keeps it decrypted");

		closeTheLockedVaultThroughTheWatchdog();

		assertNull(GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance()
			.getModelObject().getMasterPwFileModelBean()),
			"the vault is closed, not merely hidden again");
		assertFalse(theModelStillHolds(ENTRY_TITLE),
			"and its entries are out of the model. This is the assertion the issue asked for - a "
				+ "memory fact, not a screen fact");
		assertFalse(
			GuiActionRunner.execute(
				() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn()),
			"closing a locked vault must not sign the application back in");
		assertTrue(databaseFile.exists(), "and the file it came from is untouched");
	}

	@Test
	@DisplayName("locking writes pending changes, so the close afterwards loses nothing")
	void lockingSavesFirst_soTheTimedCloseCannotLoseChanges() throws IOException
	{
		File databaseFile = new File(tempHome, "locking-saves-first.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, ENTRY_TITLE, "someone",
			TestPasswords.throwaway());

		// deliberately NOT saved here: locking has to do it, while the master password is still in
		// memory. Afterwards there is nothing left to encrypt with
		application.lockWorkspace();
		application.cancelUnlock();

		assertFalse(
			GuiActionRunner.execute(
				() -> MysticCryptApplicationFrame.getInstance().getModelObject().isDirty()),
			"locking wrote the pending changes. Without that the timed close would have to choose "
				+ "between dropping them and never running, and both are bad answers");

		closeTheLockedVaultThroughTheWatchdog();
		ApplicationSteps reopened = openADatabaseFileInTheRunningApplication(databaseFile);
		reopened.showMainFrame();

		assertTrue(reopened.entryExistsWithTitle(ENTRY_TITLE),
			"and the entry added just before locking is in the file. A round trip, because a "
				+ "dirty flag says what the application believes, not what is on disk");
	}

	/**
	 * Opens a database in the application that is already running, through the entry #266 added.
	 * Not a second {@code signInWithExistingDatabase}: that launches another application, and this
	 * test is about the one whose vault was just closed
	 *
	 * @param databaseFile
	 *            the database to open
	 * @return steps for the signed-in application
	 */
	private ApplicationSteps openADatabaseFileInTheRunningApplication(final File databaseFile)
	{
		SwingUtilities.invokeLater(() -> new OpenExistingDatabaseAction("Open Database File")
			.actionPerformed(new ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		awaitSignInDialog().requireOkDisabled().checkMasterPassword()
			.typeMasterPassword(MASTER_PASSWORD).browseApplicationFile(databaseFile)
			.requireOkEnabled().okAndAwaitSignIn();
		return new ApplicationSteps(robot).awaitSignedIn();
	}

	/**
	 * Drives the real close against the real application frame, over a clock this test moves.
	 * <p>
	 * A watchdog of its own rather than the frame's: the frame's runs on the system clock, and
	 * waiting fifteen real minutes measures nothing the unit tests do not already measure. What
	 * this adds is the other half - that the close it decides on actually empties the model of this
	 * running application. It goes through the frame's own workspace adapter, so nothing here is a
	 * stand-in for the code under test
	 */
	private void closeTheLockedVaultThroughTheWatchdog()
	{
		AtomicLong clock = new AtomicLong();
		IdleLockWatchdog watchdog = new IdleLockWatchdog(
			MysticCryptApplicationFrame.getInstance().asLockableWorkspace(),
			() -> IdleLockDecision.OFF, () -> 15, clock::get);
		GuiActionRunner.execute(() -> {
			// the first check starts the locked clock, the second one finds it expired
			watchdog.checkNow();
			clock.set(TimeUnit.MINUTES.toMillis(15));
			watchdog.checkNow();
		});
		robot.waitForIdle();
	}

	/**
	 * Whether the application model still holds an entry with the given title, in either of the two
	 * places an entry lives - the map of a node's entries and the tree the nodes hang in
	 *
	 * @param title
	 *            the entry's title
	 * @return true if it is still in memory
	 */
	private static boolean theModelStillHolds(final String title)
	{
		return GuiActionRunner.execute(() -> {
			var model = MysticCryptApplicationFrame.getInstance().getModelObject();
			if (model.getDataOfNodes() != null && model.getDataOfNodes().values().stream()
				.filter(entries -> entries != null).flatMap(List::stream).anyMatch(
					entry -> entry != null && Arrays.equals(title.toCharArray(), entry.getTitle())))
			{
				return true;
			}
			if (model.getRootTreeAsMap() == null)
			{
				return false;
			}
			for (TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node : model
				.getRootTreeAsMap().values())
			{
				if (node == null || node.getValue() == null
					|| node.getValue().getDefaultContent() == null)
				{
					continue;
				}
				for (MysticCryptEntryModelBean entry : node.getValue().getDefaultContent())
				{
					if (entry != null && Arrays.equals(title.toCharArray(), entry.getTitle()))
					{
						return true;
					}
				}
			}
			return false;
		});
	}
}
