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

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * Ending the application has to overwrite the decrypted vault before the JVM goes, the way closing
 * the vault does. It did not: both ways of ending ran straight into {@code System.exit(0)}, and the
 * JVM releases its heap without overwriting it - so the one moment the decision record names as the
 * end of the vault's life in memory was the moment it was left exactly as it was (#387).
 * <p>
 * The exit itself is injected, because a test cannot assert anything after {@code System.exit}.
 * What is held across the ending is the entry's own password array, taken out of the live model,
 * and the assertion is that it is zero-filled - not that some reference became null
 * (quality-checks.md).
 */
class EndingTheApplicationErasesTheVaultUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String ENTRY_PASSWORD = TestPasswords.throwaway();

	private static final String CONFIRM_TITLE = Messages
		.getString("dialog.confirm.save.before.close.title", "Save Database Before Close.");

	@Test
	@DisplayName("ending with unsaved changes discarded overwrites the entry's password before exit")
	void endingOverwritesThePasswordBeforeTheExit() throws IOException
	{
		File databaseFile = new File(tempHome, "ending-erases.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, "held-across-the-end", "someone",
			ENTRY_PASSWORD);
		char[] password = passwordOfTheFirstEntry();
		assertTrue(new String(password).equals(ENTRY_PASSWORD),
			"the precondition: the array held is the live one, and it holds the password");

		AtomicBoolean exitReached = new AtomicBoolean(false);
		AtomicBoolean ended = new AtomicBoolean(false);
		SwingUtilities.invokeLater(() -> ended.set(MysticCryptApplicationFrame.getInstance()
			.endTheApplication(() -> exitReached.set(true))));
		awaitTheSaveQuestion();
		answerTheSaveQuestionWith(JOptionPane.NO_OPTION);
		Pause.pause(new Condition("the ending has run through to the exit")
		{
			@Override
			public boolean test()
			{
				return exitReached.get();
			}
		}, TimeUnit.SECONDS.toMillis(15));

		assertTrue(ended.get(), "answering No ends the application");
		assertTrue(isZeroFilled(password),
			"the password array must be overwritten BEFORE the exit is reached - the JVM releases "
				+ "memory, it does not clear it, and a core dump or a swap file keeps what was there");
		assertFalse(
			GuiActionRunner.execute(
				() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn()),
			"the vault is closed on the way out, not only the JVM ended");
	}

	@Test
	@DisplayName("ending with a clean vault asks nothing and still overwrites it")
	void endingACleanVaultOverwritesItWithoutAsking() throws IOException
	{
		File databaseFile = new File(tempHome, "ending-clean.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0).addEntry(frame, "saved-then-ended", "someone",
			ENTRY_PASSWORD);
		application.saveDatabase();
		char[] password = passwordOfTheFirstEntry();

		AtomicBoolean exitReached = new AtomicBoolean(false);
		boolean ended = GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance()
			.endTheApplication(() -> exitReached.set(true)));

		assertTrue(ended && exitReached.get(), "a clean vault ends without a question");
		assertTrue(isZeroFilled(password),
			"clean or not, the decrypted vault does not outlive the application in memory");
	}

	private static char[] passwordOfTheFirstEntry()
	{
		return GuiActionRunner.execute(() -> {
			for (TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node : MysticCryptApplicationFrame
				.getInstance().getModelObject().getRootTreeAsMap().values())
			{
				List<MysticCryptEntryModelBean> entries = node.getValue().getDefaultContent();
				if (entries != null && !entries.isEmpty())
				{
					return entries.get(0).getPassword();
				}
			}
			throw new IllegalStateException("no entry in the open vault");
		});
	}

	private static boolean isZeroFilled(final char[] buffer)
	{
		for (char c : buffer)
		{
			if (c != 0)
			{
				return false;
			}
		}
		return true;
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
