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
 * Creating a vault while another one is open closes the open one first (#279, #281).
 * <p>
 * Measured before the #279 fix, through the running application including a restart: the new vault
 * received the open vault's entries under its own master password, and a change made to the open
 * vault never reached its file - the flow replaced the save target and left the model alone. Both
 * halves are still pinned here as what must NOT happen again; what changed is how they are
 * prevented.
 * <p>
 * The #279 fix refused outright, deliberately and temporarily, because closing a vault was a state
 * this application could not reach while running - and inventing that path inside a single fix is
 * the improvised state move that produced #270. #281 built it, so this class now describes the
 * closing flow, as its previous version said it would.
 * <p>
 * A LOCKED vault is still refused rather than closed, and that is the #270 protection rather than
 * an oversight: its master password is not in memory, so there is nothing to write its pending
 * changes with. That case is measured in {@code LockRefusesANewVaultUiTest}.
 */
class SecondVaultWhileSignedInUiTest extends AbstractUiTest
{

	private static final String PW_A = TestPasswords.throwaway();

	private static final String REFUSAL_TITLE = "A database is already open";

	private static final String FILE_CHOOSER_TITLE = "Specify the database file to save";

	@Test
	@DisplayName("creating a vault while one is open closes it instead of refusing")
	void creatingASecondVaultClosesTheOpenOne() throws Exception
	{
		File fileA = new File(tempHome, "vault-a.mcrdb");
		createDatabaseFileHeadless(fileA, PW_A);

		ApplicationSteps application = signInWithExistingDatabase(fileA, PW_A);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, "EntryOfA", "user-a", "secret-of-a");
		application.saveDatabase();

		fireNewApplicationFileAction();
		awaitDialogTitled(FILE_CHOOSER_TITLE);

		assertTrue(dialogTitled(REFUSAL_TITLE) == null,
			"an UNLOCKED vault is closed rather than refused since #281 - the refusal was the "
				+ "shape available while this application could not close a vault at all");
		assertTrue(
			GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject()
				.getMasterPwFileModelBean() == null),
			"and it is closed BEFORE anything is created: the credentials of the vault that was "
				+ "open are gone by the time the file chooser is up");
		assertFalse(
			GuiActionRunner.execute(
				() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn()),
			"nothing may set the signed-in state along the way - that improvised move is #270");

		dismissDialog(FILE_CHOOSER_TITLE);
	}

	@Test
	@DisplayName("the vault that was closed keeps its content and its file")
	void theClosedVaultIsUntouched() throws Exception
	{
		File fileA = new File(tempHome, "untouched-a.mcrdb");
		File fileB = new File(tempHome, "vault-b.mcrdb");
		createDatabaseFileHeadless(fileA, PW_A);

		ApplicationSteps application = signInWithExistingDatabase(fileA, PW_A);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, "EntryOfA", "user-a", "secret-of-a");
		application.saveDatabase();
		long lengthAfterSaving = fileA.length();

		fireNewApplicationFileAction();
		awaitDialogTitled(FILE_CHOOSER_TITLE);
		dismissDialog(FILE_CHOOSER_TITLE);

		assertEquals(lengthAfterSaving, fileA.length(),
			"closing writes nothing by itself - the question about unsaved changes was answered "
				+ "before this, and there were none");
		assertFalse(fileB.exists(), "and nothing was created either, the chooser was dismissed");

		shutdownApplication();

		ApplicationSteps reopened = signInWithExistingDatabase(fileA, PW_A);
		assertTrue(reopened.entryExistsWithTitle("EntryOfA"),
			"measured through a restart, not through a file timestamp: the vault that was closed "
				+ "still holds its own entry. Writing one vault's entries into another vault's "
				+ "file is what this whole issue was about");
	}

	private static void fireNewApplicationFileAction()
	{
		SwingUtilities.invokeLater(() -> new NewApplicationFileAction("New Application")
			.actionPerformed(new ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
	}

	private static void awaitDialogTitled(final String title)
	{
		Pause.pause(new Condition("the dialog '" + title + "' is on screen")
		{
			@Override
			public boolean test()
			{
				return dialogTitled(title) != null;
			}
		}, TimeUnit.SECONDS.toMillis(15));
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
