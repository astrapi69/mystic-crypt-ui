package io.github.astrapi69.mystic.crypt.ui;

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

/**
 * An edit typed into an open entry dialog is work the user has done, and the automatic lock and
 * close have to treat it as such (#303).
 * <p>
 * The dialog binds its fields onto the entry that lives in the tree, so what is typed is already in
 * the model - but nothing sets the dirty flag, and every decision downstream reads that flag: the
 * lock saves nothing, and the close that follows fifteen minutes later sees a clean model, wipes
 * every entry and drops it. The user walked away mid-edit and came back to a vault without the
 * change.
 * <p>
 * Driven over a clock this test moves rather than fifteen real minutes, through the frame's own
 * workspace adapter, so the code under test is the real one.
 */
class OpenEditorSurvivesTheIdleLockUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String ORIGINAL_TITLE = "TheSecret";

	private static final String TYPED_TITLE = "TheSecretRenamedButNotConfirmed";

	@Test
	@DisplayName("an edit left open in the dialog survives the automatic lock and close")
	void anEditInAnOpenDialogIsNotLostToTheIdleTimeout() throws Exception
	{
		File databaseFile = new File(tempHome, "open-editor.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, ORIGINAL_TITLE, "the-user", "the-password");
		application.saveDatabase();

		application.selectEntryRowByTitle(frame, ORIGINAL_TITLE);
		application.openTheEditDialogAndType(frame, TYPED_TITLE);

		System.out.println("PROBE after typing: model holds typed="
			+ theModelStillHolds(TYPED_TITLE) + " original=" + theModelStillHolds(ORIGINAL_TITLE)
			+ " unsaved=" + GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance()
				.asLockableWorkspace().hasUnsavedChanges()));

		theIdleTimeoutRuns();

		System.out.println("PROBE after the timeout: model holds typed="
			+ theModelStillHolds(TYPED_TITLE) + " original=" + theModelStillHolds(ORIGINAL_TITLE)
			+ " vaultOpen=" + GuiActionRunner.execute(() -> MysticCryptApplicationFrame
				.getInstance().asLockableWorkspace().aVaultIsOpen()));

		assertTrue(theModelStillHolds(TYPED_TITLE) || theFileHolds(databaseFile, TYPED_TITLE),
			"the typed change has to be somewhere afterwards - in memory or on disk. It was in "
				+ "neither, which is the whole of #303: the dialog holds work the flag does not "
				+ "know about, so the lock saved nothing and the close discarded everything");
	}

	/**
	 * Runs the real lock and the real close over a clock this test moves: the first check starts
	 * the idle clock, the second finds the idle timeout expired, the third finds the locked vault
	 * old enough to close
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
			clock.set(TimeUnit.MINUTES.toMillis(30));
			watchdog.checkNow();
		});
		robot.waitForIdle();
	}

	/**
	 * Whether the model in memory still holds an entry with the given title, in both of the places
	 * an entry lives - the map of a node's entries and the tree the nodes hang in. Looking in only
	 * one of them reports a loss that did not happen
	 *
	 * @param title
	 *            the entry's title
	 * @return true if it is still in memory
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

	/** Whether the vault file on disk holds the given title, read back through a fresh sign-in */
	private boolean theFileHolds(final File databaseFile, final String title) throws Exception
	{
		shutdownApplication();
		ApplicationSteps reopened = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		return reopened.entryExistsWithTitle(title);
	}
}
