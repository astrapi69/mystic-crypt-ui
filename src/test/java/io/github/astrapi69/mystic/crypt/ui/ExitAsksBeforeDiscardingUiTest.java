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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Window;
import java.io.File;
import java.io.IOException;
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

import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * File > Exit with unsaved changes has to ask the same question the window's close button asks. It
 * did not: the menu item was wired to a library action whose whole body is {@code System.exit(0)},
 * so a user who chose Exit expecting the question every other ending asks lost every change since
 * the last save (#386).
 * <p>
 * The reproduction is the real menu item, clicked. Before the fix this test does not fail - it
 * dies: the JVM ends with exit code 0 and Gradle records the test as skipped, which is the failure
 * mode lessons-learned.md describes for #288. Read the XML counts, not the build result.
 */
class ExitAsksBeforeDiscardingUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String CONFIRM_TITLE = Messages
		.getString("dialog.confirm.save.before.close.title", "Save Database Before Close.");

	private static final String ENTRY_TITLE = "not-lost-to-the-exit-menu";

	@Test
	@DisplayName("File > Exit with unsaved changes asks, and Cancel keeps the application")
	void exitAsksAndCancelKeepsTheApplication() throws IOException
	{
		File databaseFile = new File(tempHome, "exit-asks.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, ENTRY_TITLE, "someone",
			TestPasswords.throwaway());
		long lengthBeforeTheAttempt = databaseFile.length();

		clickTheExitMenuItem();
		awaitTheSaveQuestion();
		answerTheSaveQuestionWith(JOptionPane.CANCEL_OPTION);

		assertTrue(
			GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance().isShowing()),
			"Cancel means the application stays, from the menu exactly as from the window button");
		assertTrue(
			GuiActionRunner.execute(
				() -> MysticCryptApplicationFrame.getInstance().getModelObject().isDirty()),
			"and the unsaved changes are still unsaved");
		assertTrue(application.entryExistsWithTitle(ENTRY_TITLE),
			"the entry that was never saved is still in the open vault");
		assertEquals(lengthBeforeTheAttempt, databaseFile.length(),
			"nothing was written either: Cancel is not a quiet Save");
	}

	private void clickTheExitMenuItem()
	{
		JMenuItem exit = robot.finder().findByName("global.menu.file.exit", JMenuItem.class, false);
		SwingUtilities.invokeLater(exit::doClick);
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

	private void answerTheSaveQuestionWith(final int option)
	{
		GuiActionRunner.execute(() -> {
			JOptionPane optionPane = (JOptionPane)robot.finder().findByType(saveQuestionDialog(),
				JOptionPane.class);
			optionPane.setValue(option);
		});
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
