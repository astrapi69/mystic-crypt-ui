package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.lock.IdleLockWatchdog;

/**
 * The lock invariant, held against the AUTOMATIC lock instead of against an action (#305).
 * <p>
 * {@link LockInvariantUiTest} fires every action in the host's action package and asserts four
 * properties per action. The automatic lock is not in it and cannot be: {@code IdleLockWatchdog}
 * lives in the {@code lock} package and is no {@code AbstractAction}, so the completeness check
 * that catches a new action never sees it. This is the harness the issue asks for - the same shape,
 * the same four properties, a different trigger.
 * <p>
 * The timeout is fired rather than waited out, over a clock this test moves, so what runs is the
 * real watchdog against the real frame and not a stub of either.
 * <p>
 * The fifth property is the one the invariant does not have and this trigger needs: <b>nothing was
 * lost.</b> An action is something the user chose; the idle lock happens to somebody who walked
 * away, and what it must never do is decide about their unsaved work for them (#303, #304).
 */
class AutomaticLockHoldsTheInvariantUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String ENTRY_TITLE = "TheSecret";

	private static final String ENTRY_PASSWORD = "the-password";

	@Test
	@DisplayName("the idle lock leaves the workspace locked, the vault off screen and the file "
		+ "untouched")
	void theIdleTimeoutHoldsEveryPropertyTheActionsAreHeldTo() throws Exception
	{
		File databaseFile = new File(tempHome, "idle-lock-invariant.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, ENTRY_TITLE, "someone", ENTRY_PASSWORD);
		application.saveDatabase();
		long lengthBeforeTheTimeout = databaseFile.length();
		long modifiedBeforeTheTimeout = databaseFile.lastModified();
		assertTrue(application.vaultIsOnScreen(),
			"the precondition: a real vault is open in the running application. Without it this "
				+ "test would assert the locked state of nothing");

		theIdleTimeoutRuns();

		assertFalse(signedIn(),
			"the idle timeout must leave the workspace locked. Only entering the master password "
				+ "lifts that, whoever or whatever triggered the lock");
		assertFalse(application.vaultIsOnScreen(),
			"a lock that leaves the vault window standing is the bypass #237 was about, and a "
				+ "timer is no more entitled to it than a menu item");
		assertTrue(
			lengthBeforeTheTimeout == databaseFile.length()
				&& modifiedBeforeTheTimeout == databaseFile.lastModified(),
			"locking does not write. A timer committing a change the user had not decided about "
				+ "takes the decision away, and in a password manager writing is not neutral "
				+ "(#304)");
		assertTrue(ScreenText.nothingOnScreenShows(ENTRY_TITLE, ENTRY_PASSWORD),
			"and nothing left on screen still shows the entry - the panel being gone is one door, "
				+ "a dialog listing the content is another");
	}

	@Test
	@DisplayName("the idle lock keeps the entry that was in the vault")
	void theIdleTimeoutLosesNothing() throws Exception
	{
		File databaseFile = new File(tempHome, "idle-lock-loses-nothing.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, ENTRY_TITLE, "someone", ENTRY_PASSWORD);
		application.saveDatabase();

		theIdleTimeoutRuns();

		assertTrue(theModelStillHolds(ENTRY_TITLE),
			"the property the invariant does not have: an action is something the user chose, the "
				+ "idle lock happens to somebody who walked away. It may hide their work; it may "
				+ "not drop it");
		assertEquals(false, signedIn(), "and it is hidden behind the locked state while it waits");
	}

	/**
	 * Runs the real watchdog over a clock this test moves, far enough for the lock and not far
	 * enough for the close that follows it later
	 */
	private void theIdleTimeoutRuns()
	{
		AtomicLong clock = new AtomicLong();
		IdleLockWatchdog watchdog = new IdleLockWatchdog(
			MysticCryptApplicationFrame.getInstance().asLockableWorkspace(), () -> 15, () -> 15,
			clock::get);
		GuiActionRunner.execute(() -> {
			watchdog.checkNow();
			clock.set(TimeUnit.MINUTES.toMillis(15));
			watchdog.checkNow();
		});
		robot.waitForIdle();
	}

	private static boolean signedIn()
	{
		return GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn());
	}

	private static boolean theModelStillHolds(final String entryTitle)
	{
		return GuiActionRunner.execute(() -> {
			var dataOfNodes = MysticCryptApplicationFrame.getInstance().getModelObject()
				.getDataOfNodes();
			if (dataOfNodes != null && holds(dataOfNodes.values(), entryTitle))
			{
				return true;
			}
			var tree = MysticCryptApplicationFrame.getInstance().getModelObject()
				.getRootTreeAsMap();
			if (tree == null)
			{
				return false;
			}
			return tree.values().stream().filter(node -> node != null && node.getValue() != null)
				.anyMatch(node -> holds(List.of(node.getValue().getDefaultContent()), entryTitle));
		});
	}

	private static boolean holds(
		final java.util.Collection<List<io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean>> lists,
		final String entryTitle)
	{
		return lists.stream().filter(entries -> entries != null).flatMap(List::stream)
			.filter(entry -> entry != null && entry.getTitle() != null)
			.anyMatch(entry -> String.valueOf(entry.getTitle()).contains(entryTitle));
	}
}
