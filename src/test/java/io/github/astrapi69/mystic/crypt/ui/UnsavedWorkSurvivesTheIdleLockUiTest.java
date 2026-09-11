package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
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
import io.github.astrapi69.mystic.crypt.settings.MysticCryptSettings;

/**
 * The automatic lock does not decide about the user's data (#304).
 * <p>
 * This is the case #303 does not cover: a change that IS in the model and IS marked as unsaved,
 * with no dialog open anywhere. The user added an entry, did not save, and walked away. What must
 * not happen is either half of the old behaviour - the lock writing the change to the file because
 * a timer decided to, or the close that follows dropping the vault with the change in it.
 * <p>
 * Driven over a clock this test moves, through the frame's own workspace adapter, so the code under
 * test is the one the application runs.
 */
class UnsavedWorkSurvivesTheIdleLockUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String SAVED_ENTRY = "TheSavedOne";

	private static final String UNSAVED_ENTRY = "TheOneNobodySaved";

	@Test
	@DisplayName("an unsaved change is neither written nor dropped by the idle timeout")
	void anUnsavedChangeSurvivesLockedInMemoryAndReachesNoFile() throws Exception
	{
		File databaseFile = new File(tempHome, "unsaved-work.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, SAVED_ENTRY, "the-user", "the-password");
		application.saveDatabase();
		long lengthAfterSaving = databaseFile.length();
		long modifiedAfterSaving = databaseFile.lastModified();

		// the change nobody asked to save, and no dialog is open when it is made
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, UNSAVED_ENTRY, "another-user", "another-password");
		assertTrue(unsavedChangesAreReported(),
			"the precondition of this test: the model knows it is dirty before the timeout runs");

		theIdleTimeoutRuns();

		assertTrue(
			lengthAfterSaving == databaseFile.length()
				&& modifiedAfterSaving == databaseFile.lastModified(),
			"locking must not write. A timer deciding to commit a change the user had not "
				+ "decided about takes the choice away, and in a password manager writing is not "
				+ "a neutral act");
		assertTrue(theVaultIsStillOpen(),
			"and the close must leave a dirty vault alone: it stays locked until somebody "
				+ "unlocks it and decides");
		assertTrue(theModelStillHolds(UNSAVED_ENTRY),
			"so the unsaved entry is still in memory, waiting for that decision");
		assertEquals(false, signedIn(), "and the workspace is locked while it waits");
	}

	@Test
	@DisplayName("with the setting on, locking writes - the old behaviour, asked for once")
	void withTheSettingOnLockingWrites() throws Exception
	{
		File configurationDirectory = new File(tempHome, ".config/mystic-crypt-ui");
		configurationDirectory.mkdirs();
		MysticCryptSettings settings = new MysticCryptSettings();
		settings.setSaveWhenLocking(true);
		settings.save(configurationDirectory);

		File databaseFile = new File(tempHome, "save-on-lock.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, SAVED_ENTRY, "the-user", "the-password");
		application.saveDatabase();
		long lengthAfterSaving = databaseFile.length();
		long modifiedAfterSaving = databaseFile.lastModified();

		application.selectTreeRow(frame, 0);
		application.addEntry(frame, UNSAVED_ENTRY, "another-user", "another-password");

		theIdleTimeoutRuns();

		assertTrue(
			lengthAfterSaving != databaseFile.length()
				|| modifiedAfterSaving != databaseFile.lastModified(),
			"whoever asks for the old behaviour gets it: with the setting on, the lock writes "
				+ "the pending change");
	}

	@Test
	@DisplayName("the unlock prompt says that unsaved changes are waiting")
	void theUnlockPromptSaysWhatIsWaiting() throws Exception
	{
		File databaseFile = new File(tempHome, "unlock-hint.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, UNSAVED_ENTRY, "another-user", "another-password");

		application.lockWorkspace();

		assertTrue(theUnlockPromptShowsTheWaitingLine(),
			"otherwise the save-before-close question is the first and only hint the user ever "
				+ "gets that the lock kept something: one line turns 'not lost' into "
				+ "'deliberately kept'");
	}

	/**
	 * Whether the unlock prompt that locking put on screen carries the line about unsaved changes
	 */
	private boolean theUnlockPromptShowsTheWaitingLine()
	{
		return GuiActionRunner.execute(() -> {
			for (java.awt.Window window : java.awt.Window.getWindows())
			{
				if (!(window instanceof javax.swing.JDialog dialog) || !dialog.isShowing())
				{
					continue;
				}
				if (labelIsIn(dialog))
				{
					return true;
				}
			}
			return false;
		});
	}

	private static boolean labelIsIn(final java.awt.Container container)
	{
		for (java.awt.Component component : container.getComponents())
		{
			if ("lblUnsavedChangesWaiting".equals(component.getName()))
			{
				return true;
			}
			if (component instanceof java.awt.Container nested && labelIsIn(nested))
			{
				return true;
			}
		}
		return false;
	}

	@Test
	@DisplayName("ending the application while locked and dirty asks before discarding")
	void endingTheApplicationWhileLockedAndDirtyAsksBeforeDiscarding() throws Exception
	{
		File databaseFile = new File(tempHome, "exit-while-locked.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, SAVED_ENTRY, "the-user", "the-password");
		application.saveDatabase();
		long lengthAfterSaving = databaseFile.length();

		application.selectTreeRow(frame, 0);
		application.addEntry(frame, UNSAVED_ENTRY, "another-user", "another-password");
		application.lockWorkspace();
		application.cancelUnlock();

		javax.swing.SwingUtilities.invokeLater(() -> MysticCryptApplicationFrame.getInstance()
			.dispatchEvent(new java.awt.event.WindowEvent(MysticCryptApplicationFrame.getInstance(),
				java.awt.event.WindowEvent.WINDOW_CLOSING)));
		UiTestSpeed.windowManagerSettle();
		robot.waitForIdle();

		assertTrue(aDialogNamesDiscarding(),
			"locked means the master password is not in memory, so 'save' cannot work here. The "
				+ "user has to be told that ending now discards the change - offering a save that "
				+ "fails, or ending silently, are the two things this must not do");
		assertTrue(lengthAfterSaving == databaseFile.length(),
			"and nothing was written behind the question");
	}

	/** Whether a dialog on screen says that the unsaved changes would be discarded */
	private boolean aDialogNamesDiscarding()
	{
		return GuiActionRunner.execute(() -> {
			for (java.awt.Window window : java.awt.Window.getWindows())
			{
				if (window instanceof javax.swing.JDialog dialog && dialog.isShowing()
					&& dialog.getTitle() != null
					&& dialog.getTitle().toLowerCase().contains("discard"))
				{
					return true;
				}
			}
			return false;
		});
	}

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
			clock.set(TimeUnit.MINUTES.toMillis(30));
			watchdog.checkNow();
		});
		robot.waitForIdle();
	}

	private static boolean unsavedChangesAreReported()
	{
		return GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance()
			.asLockableWorkspace().hasUnsavedChanges());
	}

	private static boolean theVaultIsStillOpen()
	{
		return GuiActionRunner.execute(
			() -> MysticCryptApplicationFrame.getInstance().asLockableWorkspace().aVaultIsOpen());
	}

	private static boolean signedIn()
	{
		return GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn());
	}

	/**
	 * Whether the model in memory still holds an entry with the given title, in both of the places
	 * an entry lives - the map of a node's entries and the tree the nodes hang in
	 */
	private static boolean theModelStillHolds(final String title)
	{
		return GuiActionRunner.execute(() -> {
			var model = MysticCryptApplicationFrame.getInstance().getModelObject();
			if (model == null)
			{
				return false;
			}
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
			for (var node : model.getRootTreeAsMap().values())
			{
				if (node == null || node.getValue() == null
					|| node.getValue().getDefaultContent() == null)
				{
					continue;
				}
				for (var entry : node.getValue().getDefaultContent())
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
