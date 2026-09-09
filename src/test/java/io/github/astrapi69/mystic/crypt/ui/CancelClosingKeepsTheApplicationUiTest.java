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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Cancelling the save-before-close question keeps the application open (#288).
 * <p>
 * The question is asked with Yes, No and Cancel, and the listener used to read all three the same
 * way: it took the answer, threw it away, and ended the application. Cancel and No therefore did
 * the same thing, so somebody reaching for Cancel to get back to their work lost every change since
 * the last save, and the window was gone before they could react.
 * <p>
 * {@code YES_NO_CANCEL_OPTION} offers three answers because there are three: save and go, do not
 * save and go, and <b>do not go</b>. The third is the only reason to offer Cancel rather than
 * {@code YES_NO_OPTION}; a button that silently means "No" reads as a way back and is not one.
 * <p>
 * WHAT THIS TEST CANNOT DO, and why that is the right shape: only the cancelled path is measured
 * here. The other two end in {@code System.exit(0)}, and a test that asserts the application really
 * exits takes the test JVM with it. Before the fix this test did exactly that - the run died rather
 * than failed, which is as loud a reproduction as this defect deserves.
 */
class CancelClosingKeepsTheApplicationUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	/**
	 * Read from the same place the dialog reads it, not typed again here: the properties file wins
	 * over the literal default in the code, and the two differ by a full stop
	 */
	private static final String CONFIRM_TITLE = Messages
		.getString("dialog.confirm.save.before.close.title", "Save Database Before Close.");

	private static final String ENTRY_TITLE = "not-lost-to-a-cancelled-close";

	@Test
	@DisplayName("cancelling the save question leaves the application and its changes alone")
	void cancellingTheSaveQuestionKeepsTheApplicationOpen() throws IOException
	{
		File databaseFile = new File(tempHome, "cancel-closing.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, ENTRY_TITLE, "someone",
			TestPasswords.throwaway());
		long lengthBeforeTheAttempt = databaseFile.length();

		attemptToCloseTheApplication();
		answerTheSaveQuestionWith(JOptionPane.CANCEL_OPTION);

		assertTrue(
			GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance().isShowing()),
			"Cancel means the application stays. Reading it as 'no' is what made the third button "
				+ "a lie");
		assertTrue(
			GuiActionRunner.execute(
				() -> MysticCryptApplicationFrame.getInstance().getModelObject().isDirty()),
			"and the unsaved changes are still unsaved, not silently dropped");
		assertTrue(application.entryExistsWithTitle(ENTRY_TITLE),
			"the entry that was never saved is still in the open vault");
		assertEquals(lengthBeforeTheAttempt, databaseFile.length(),
			"nothing was written either: Cancel is not a quiet Save");
	}

	@Test
	@DisplayName("dismissing the question with the window button counts as cancelling")
	void dismissingTheSaveQuestionKeepsTheApplicationOpen() throws IOException
	{
		File databaseFile = new File(tempHome, "dismiss-closing.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, ENTRY_TITLE, "someone",
			TestPasswords.throwaway());

		attemptToCloseTheApplication();
		dismissTheSaveQuestion();

		assertTrue(
			GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance().isShowing()),
			"a dialog dismissed the other way is not a 'no'. Reading it as one throws away the "
				+ "changes of somebody who only wanted the question to go away");
		assertTrue(GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject().isDirty()));
	}

	/**
	 * Asks the window to close the way the title bar's close button does, off the test thread: the
	 * listener puts a modal question up, and a test thread waiting for it never reaches the answer
	 */
	private void attemptToCloseTheApplication()
	{
		SwingUtilities.invokeLater(() -> {
			MysticCryptApplicationFrame frame = MysticCryptApplicationFrame.getInstance();
			frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
		});
		awaitTheSaveQuestion();
	}

	private void awaitTheSaveQuestion()
	{
		Pause.pause(new Condition("the save-before-close question is on screen")
		{
			@Override
			public boolean test()
			{
				return saveQuestionDialog() != null;
			}
		}, TimeUnit.SECONDS.toMillis(15));
	}

	/**
	 * Answers through the option pane's own API rather than by clicking: a synthetic click does not
	 * translate into the option value here, and the value is what the listener reads
	 *
	 * @param option
	 *            one of the {@link JOptionPane} option constants
	 */
	private void answerTheSaveQuestionWith(final int option)
	{
		GuiActionRunner.execute(() -> {
			JOptionPane optionPane = (JOptionPane)robot.finder().findByType(saveQuestionDialog(),
				JOptionPane.class);
			optionPane.setValue(option);
		});
		awaitTheSaveQuestionGone();
	}

	/** Closes the question the way its own window button does, without answering it */
	private void dismissTheSaveQuestion()
	{
		GuiActionRunner.execute(() -> {
			JDialog dialog = saveQuestionDialog();
			dialog.setVisible(false);
			dialog.dispose();
		});
		awaitTheSaveQuestionGone();
	}

	private void awaitTheSaveQuestionGone()
	{
		Pause.pause(new Condition("the save-before-close question is gone")
		{
			@Override
			public boolean test()
			{
				return saveQuestionDialog() == null;
			}
		}, TimeUnit.SECONDS.toMillis(15));
		robot.waitForIdle();
	}

	private static JDialog saveQuestionDialog()
	{
		return GuiActionRunner.execute(() -> {
			for (Window window : Window.getWindows())
			{
				if (window instanceof JDialog dialog && dialog.isShowing()
					&& CONFIRM_TITLE.equals(dialog.getTitle()))
				{
					return dialog;
				}
			}
			return null;
		});
	}
}
