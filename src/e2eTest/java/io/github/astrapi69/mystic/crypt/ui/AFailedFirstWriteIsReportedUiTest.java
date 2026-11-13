/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dialog;
import java.awt.Window;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * A first write in "Create your master key" that cannot be done (#944).
 * <p>
 * Measured on develop {@code 2b94c9f8}: the exception left OK on the event thread and reached only
 * standard error. The dialog stayed open with nothing on screen, and the file the new database was
 * to be written to was left with 0 bytes, so the next sign-in offered a database that is none. The
 * failure used here is a key file that went away between choosing it and OK, one of the ways a
 * first write fails besides a full disk or a directory that cannot be written.
 * <p>
 * Now a failed first write says so with the file and the reason, as a failed save does (#424), the
 * dialog stays open for the corrected input, and an empty file is removed. A file with content is
 * never removed.
 */
class AFailedFirstWriteIsReportedUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String OTHER_PASSWORD = TestPasswords.throwaway() + "-other";

	private static final String KEY_FILE_NAME = "goes-away.pem";

	/** The title of the dialog in which a new database gets its master key */
	private static final String CREATE_MASTER_KEY = "Create your master key";

	/** The title of the message that says the new database could not be written */
	private static final String CREATION_FAILED = Messages.getString("dialog.create.failed.title",
		"The database could not be created");

	@Test
	@DisplayName("A failed first write to a new file says why, keeps the dialog open and leaves no empty file (#944)")
	void aFailedFirstWriteToANewFile() throws Exception
	{
		File newDatabaseFile = new File(tempHome, "new-one.mcrdb");

		SignInDialogSteps signIn = launchApplication();
		CreateMasterKeySteps createMasterKey = signIn.startNewDatabase(newDatabaseFile);
		File keyFile = aPasswordAndAKeyFileThatGoesAway(createMasterKey);

		createMasterKey.clickOk();
		String message = awaitTheMessageAndCloseIt();

		assertTrue(message.contains(newDatabaseFile.getAbsolutePath()),
			"the message names the file the database was to be written to: " + message);
		assertTrue(message.contains(keyFile.getName()),
			"and the reason, here the key file that is not there: " + message);
		assertTrue(aDialogIsShowing(CREATE_MASTER_KEY),
			"the dialog stays open, so the input can be corrected");
		assertFalse(newDatabaseFile.exists(),
			"no empty file is left that the next sign-in would offer as a database");

		createMasterKey.createKeyFile(keyFile.getName()).requireOkEnabled().okAndAwaitClose();
		assertTrue(newDatabaseFile.length() > 0,
			"with the key file back, OK writes the database to the same file");
	}

	@Test
	@DisplayName("A failed first write onto an existing vault leaves that vault exactly as it was (#944)")
	void aFailedFirstWriteOntoAnExistingVault() throws Exception
	{
		File existing = new File(tempHome, "already-there.mcrdb");
		createDatabaseFileHeadless(existing, OTHER_PASSWORD);
		byte[] before = Files.readAllBytes(existing.toPath());

		SignInDialogSteps signIn = launchApplication();
		signIn.chooseFileForNewDatabase(existing);
		answerTheReplaceQuestionWithYes();
		CreateMasterKeySteps createMasterKey = awaitCreateMasterKey();
		aPasswordAndAKeyFileThatGoesAway(createMasterKey);

		createMasterKey.clickOk();
		String message = awaitTheMessageAndCloseIt();

		assertTrue(message.contains(existing.getAbsolutePath()), message);
		assertTrue(aDialogIsShowing(CREATE_MASTER_KEY),
			"the dialog stays open, so the input can be corrected");
		assertArrayEquals(before, Files.readAllBytes(existing.toPath()),
			"the vault that was there is untouched: the write failed before anything reached it, "
				+ "and a file with content is never what a failed creation removes");
	}

	/**
	 * Master password with its repeat, and a key file made in the dialog and deleted again before
	 * OK: the OK button was enabled while the file was there and stays so
	 *
	 * @return the key file, which no longer exists
	 */
	private File aPasswordAndAKeyFileThatGoesAway(final CreateMasterKeySteps createMasterKey)
		throws Exception
	{
		createMasterKey.checkMasterPassword().typeMasterPasswordWithRepeat(MASTER_PASSWORD)
			.checkKeyFile().createKeyFile(KEY_FILE_NAME).requireOkEnabled();
		File keyFile = new File(
			MysticCryptApplicationFrame.getInstance().getConfigurationDirectory(), KEY_FILE_NAME);
		Files.delete(keyFile.toPath());
		return keyFile;
	}

	private CreateMasterKeySteps awaitCreateMasterKey()
	{
		DialogFixture createMasterKeyDialog = WindowFinder
			.findDialog(new GenericTypeMatcher<Dialog>(Dialog.class)
			{
				@Override
				protected boolean isMatching(final Dialog candidate)
				{
					return CREATE_MASTER_KEY.equals(candidate.getTitle()) && candidate.isShowing();
				}
			}).withTimeout(10, TimeUnit.SECONDS).using(robot);
		return new CreateMasterKeySteps(robot, createMasterKeyDialog);
	}

	/** Whether a dialog with this title is on screen */
	private boolean aDialogIsShowing(final String title)
	{
		return GuiActionRunner.execute(() -> {
			for (Window window : Window.getWindows())
			{
				if (window instanceof JDialog dialog && dialog.isShowing()
					&& title.equals(dialog.getTitle()))
				{
					return true;
				}
			}
			return false;
		});
	}

	/**
	 * Waits for the message that the database could not be created, closes it with OK and returns
	 * its text
	 */
	private String awaitTheMessageAndCloseIt()
	{
		Pause.pause(new Condition("the message that the database could not be created")
		{
			@Override
			public boolean test()
			{
				return aDialogIsShowing(CREATION_FAILED);
			}
		}, TimeUnit.SECONDS.toMillis(15));
		String message = GuiActionRunner.execute(() -> {
			for (Window window : Window.getWindows())
			{
				if (window instanceof JDialog dialog && dialog.isShowing()
					&& CREATION_FAILED.equals(dialog.getTitle()))
				{
					JOptionPane optionPane = (JOptionPane)robot.finder().findByType(dialog,
						JOptionPane.class);
					String text = String.valueOf(optionPane.getMessage());
					optionPane.setValue(JOptionPane.OK_OPTION);
					return text;
				}
			}
			throw new AssertionError("the message went away before it could be read");
		});
		Pause.pause(new Condition("the message is closed")
		{
			@Override
			public boolean test()
			{
				return !aDialogIsShowing(CREATION_FAILED);
			}
		}, TimeUnit.SECONDS.toMillis(10));
		return message;
	}

	/** Answers the question about replacing the existing file with yes */
	private void answerTheReplaceQuestionWithYes()
	{
		GuiActionRunner.execute(() -> {
			for (Window window : Window.getWindows())
			{
				if (window instanceof JDialog dialog && dialog.isShowing()
					&& dialog.getTitle() != null
					&& dialog.getTitle().toLowerCase().contains("replace"))
				{
					JOptionPane optionPane = (JOptionPane)robot.finder().findByType(dialog,
						JOptionPane.class);
					optionPane.setValue(JOptionPane.YES_OPTION);
					return null;
				}
			}
			throw new AssertionError("no question about replacing the existing file");
		});
		UiTestSpeed.windowManagerSettle();
		robot.waitForIdle();
	}
}
