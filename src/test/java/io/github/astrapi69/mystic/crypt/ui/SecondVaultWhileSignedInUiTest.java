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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.action.NewApplicationFileAction;

/**
 * Creating a vault while another one is open is refused (#279).
 * <p>
 * Measured before the fix, through the running application including a restart: the new vault
 * received the open vault's entries under its own master password, and a change made to the open
 * vault never reached its file - the flow replaced the save target and left the model alone. Both
 * halves are pinned here as what must NOT happen again.
 * <p>
 * The refusal is the shape this application can hold today. Closing a vault is a state it cannot
 * reach while running (#281), so "close the open one first" cannot be built inside this fix without
 * inventing that path here - which is the improvised state move that produced #270. When #281 is
 * done, these tests describe the closing flow instead of the refusal, and this class is rewritten
 * rather than replaced.
 */
class SecondVaultWhileSignedInUiTest extends AbstractUiTest
{

	private static final String PW_A = TestPasswords.throwaway();

	private static final String REFUSAL_TITLE = "A database is already open";

	@Test
	@DisplayName("creating a vault while one is open is refused, and says what to do")
	void creatingASecondVaultIsRefusedWhileOneIsOpen() throws Exception
	{
		File fileA = new File(tempHome, "vault-a.mcrdb");
		File fileB = new File(tempHome, "vault-b.mcrdb");
		createDatabaseFileHeadless(fileA, PW_A);

		ApplicationSteps application = signInWithExistingDatabase(fileA, PW_A);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, "EntryOfA", "user-a", "secret-of-a");
		application.saveDatabase();

		SwingUtilities.invokeLater(() -> new NewApplicationFileAction("New Application")
			.actionPerformed(new ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		Pause.pause(new Condition("the refusal is on screen")
		{
			@Override
			public boolean test()
			{
				return dialogTitled(REFUSAL_TITLE) != null;
			}
		}, TimeUnit.SECONDS.toMillis(15));

		assertTrue(dialogTitled(REFUSAL_TITLE) != null,
			"the refusal has to be visible - a silent one repeats the defect this application "
				+ "already had in Lock workspace");
		assertTrue(dialogTitled("Specify the database file to save") == null,
			"and it comes before the file chooser, so nothing reaches disk");
		assertFalse(fileB.exists(), "no second vault file is created");
	}

	@Test
	@DisplayName("the open vault keeps its content and its file after the refusal")
	void theOpenVaultIsUntouchedByTheRefusal() throws Exception
	{
		File fileA = new File(tempHome, "untouched-a.mcrdb");
		createDatabaseFileHeadless(fileA, PW_A);

		ApplicationSteps application = signInWithExistingDatabase(fileA, PW_A);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, "EntryOfA", "user-a", "secret-of-a");
		application.saveDatabase();
		long lengthAfterSaving = fileA.length();
		File fileOnScreenBefore = application.applicationFileOnScreen();

		SwingUtilities.invokeLater(() -> new NewApplicationFileAction("New Application")
			.actionPerformed(new ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		Pause.pause(new Condition("the refusal is on screen")
		{
			@Override
			public boolean test()
			{
				return dialogTitled(REFUSAL_TITLE) != null;
			}
		}, TimeUnit.SECONDS.toMillis(15));

		assertEquals(fileOnScreenBefore, application.applicationFileOnScreen(),
			"the save target must not move - that move is what wrote the open vault's entries "
				+ "into a different file before this fix");
		assertTrue(application.entryExistsWithTitle("EntryOfA"),
			"and the open vault's content stays where it was");
		assertEquals(lengthAfterSaving, fileA.length(), "its file is not rewritten either");

		dismissDialog(REFUSAL_TITLE);
		shutdownApplication();

		ApplicationSteps reopened = signInWithExistingDatabase(fileA, PW_A);
		assertTrue(reopened.entryExistsWithTitle("EntryOfA"),
			"measured through a restart, not through a file timestamp: the vault still holds "
				+ "its own entry");
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

	private static void dismissDialog(final String title)
	{
		GuiActionRunner.execute(() -> {
			JDialog dialog = dialogTitled(title);
			if (dialog != null)
			{
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
	}
}
