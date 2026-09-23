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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.TimeUnit;

import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MenuId;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * A save that cannot be written tells the user and keeps the changes (#424).
 * <p>
 * It did neither: the write threw out of the action on the event thread, where Swing printed it to
 * standard error and nothing reached the screen, and the dirty flag had already been cleared on the
 * way in. So the application showed a saved database, the file on disk was the previous version,
 * and File > Exit afterwards ended without the question that exists for exactly this - the changes
 * were gone. Measured while investigating #375: {@code dirty before=true after=false},
 * {@code dialogs after failing save: (none)}.
 * <p>
 * The failure is real rather than simulated: the vault's directory is made read-only, which is what
 * a share that went away, a full disk or somebody else's directory look like to the writer.
 */
class AFailedSaveKeepsTheChangesUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String ENTRY_TITLE = "not-lost-to-a-failed-save";

	private static final String FAILURE_TITLE = Messages.getString("dialog.save.failed.title",
		"The database could not be saved");

	private static final String CONFIRM_TITLE = Messages
		.getString("dialog.confirm.save.before.close.title", "Save Database Before Close.");

	@Test
	@DisplayName("a save that fails is reported, the changes stay unsaved, and ending still asks")
	void aFailedSaveIsReportedAndTheChangesStay() throws IOException
	{
		File vaultDirectory = new File(tempHome, "read-only-later");
		assertTrue(vaultDirectory.mkdirs());
		File databaseFile = new File(vaultDirectory, "failed-save.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, ENTRY_TITLE, "someone",
			TestPasswords.throwaway());
		long lengthBeforeTheAttempt = databaseFile.length();
		Files.setPosixFilePermissions(vaultDirectory.toPath(),
			PosixFilePermissions.fromString("r-xr-xr-x"));
		try
		{
			application.fireMenuItem(MenuId.SAVE_APPLICATION_FILE.propertiesKey());

			JDialog failure = awaitDialogTitled(FAILURE_TITLE, "the failed save is reported");
			assertNotNull(failure, "a save that did not happen has to say so");
			closeWithOk(failure);

			assertTrue(
				GuiActionRunner.execute(
					() -> MysticCryptApplicationFrame.getInstance().getModelObject().isDirty()),
				"the changes are still unsaved, because they were not saved");
			assertEquals(lengthBeforeTheAttempt, databaseFile.length(),
				"and the file on disk is the one from before the attempt");

			clickTheExitMenuItem();
			JDialog question = awaitDialogTitled(CONFIRM_TITLE,
				"ending asks about the unsaved changes");
			assertNotNull(question,
				"ending after a failed save asks, instead of discarding what could not be "
					+ "written");
			answerWith(question, JOptionPane.CANCEL_OPTION);
			assertTrue(
				GuiActionRunner
					.execute(() -> MysticCryptApplicationFrame.getInstance().isShowing()),
				"and Cancel keeps the application, with the entry still in it");
			assertTrue(application.entryExistsWithTitle(ENTRY_TITLE),
				"the entry that could not be written is still in the open vault");
		}
		finally
		{
			Files.setPosixFilePermissions(vaultDirectory.toPath(),
				PosixFilePermissions.fromString("rwxr-xr-x"));
		}
	}

	private void clickTheExitMenuItem()
	{
		JMenuItem exit = robot.finder().findByName("global.menu.file.exit", JMenuItem.class, false);
		SwingUtilities.invokeLater(exit::doClick);
	}

	private JDialog awaitDialogTitled(final String title, final String description)
	{
		Pause.pause(new Condition(description)
		{
			@Override
			public boolean test()
			{
				return dialogTitled(title) != null;
			}
		}, TimeUnit.SECONDS.toMillis(30));
		return dialogTitled(title);
	}

	private void closeWithOk(final JDialog dialog)
	{
		answerWith(dialog, JOptionPane.OK_OPTION);
	}

	private void answerWith(final JDialog dialog, final int option)
	{
		GuiActionRunner.execute(() -> {
			JOptionPane optionPane = (JOptionPane)robot.finder().findByType(dialog,
				JOptionPane.class);
			optionPane.setValue(option);
		});
		Pause.pause(new Condition("the dialog is gone")
		{
			@Override
			public boolean test()
			{
				return dialogTitled(dialog.getTitle()) == null;
			}
		}, TimeUnit.SECONDS.toMillis(15));
		robot.waitForIdle();
	}

	private static JDialog dialogTitled(final String title)
	{
		return GuiActionRunner.execute(() -> {
			for (Window window : Window.getWindows())
			{
				if (window instanceof JDialog dialog && dialog.isShowing()
					&& title.equals(dialog.getTitle()))
				{
					return dialog;
				}
			}
			return null;
		});
	}
}
