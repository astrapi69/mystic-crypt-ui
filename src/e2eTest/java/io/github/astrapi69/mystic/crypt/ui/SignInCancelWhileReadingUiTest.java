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

import java.io.File;

import javax.swing.JButton;
import javax.swing.JLabel;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * End-to-end test of the sign-in that reads the database off the event dispatch thread (#1053):
 * while the key is derived the dialog says that it opens the database and takes no second OK, and
 * Cancel then leaves the dialog open with nothing read. The next OK signs in.
 */
class SignInCancelWhileReadingUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	/** The name of the thread the sign-in reads on, from MasterPwWithApplicationFilePanel */
	private static final String READ_THREAD = "sign-in-read";

	@Test
	@DisplayName("Cancel while the database is read leaves the dialog open with nothing read, and OK then signs in (#1053)")
	void cancelWhileTheDatabaseIsRead_leavesTheDialogOpenWithNothingRead() throws Exception
	{
		File databaseFile = new File(tempHome, "cancel-while-reading.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().checkMasterPassword().typeMasterPassword(MASTER_PASSWORD)
			.browseApplicationFile(databaseFile).requireOkEnabled();
		DialogFixture dialog = signIn.dialog();
		JButton ok = GuiActionRunner.execute(() -> dialog.button("btnOk").target());
		JButton cancel = GuiActionRunner.execute(() -> dialog.button("btnCancel").target());
		JLabel working = GuiActionRunner.execute(() -> dialog.label("lblSignInWorking").target());

		// both presses in one step of the event dispatch thread: the key derivation, which takes
		// hundreds of milliseconds, cannot be over between them
		String whileReading = GuiActionRunner.execute(() -> {
			ok.doClick();
			String shown = working.getText() + (ok.isEnabled() ? " | OK took presses" : "");
			cancel.doClick();
			return shown;
		});
		awaitTheCancelledRead();

		assertEquals(
			Messages.getString("signin.reading",
				"Opening the database: the key is being derived from the password ..."),
			whileReading);
		assertTrue(signIn.dialogIsShowing(), "Cancel during the read closed the dialog");
		assertFalse(
			GuiActionRunner.execute(
				() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn()),
			"the cancelled read signed in");
		assertTrue(GuiActionRunner.execute(ok::isEnabled), "OK takes no press after the cancel");
		assertEquals(" ", GuiActionRunner.execute(working::getText),
			"the dialog still says that it opens the database");

		signIn.okAndAwaitSignIn();
		new ApplicationSteps(robot).awaitSignedIn();
	}

	/**
	 * Waits until the read that was cancelled has ended and the event queue has handled what it
	 * posted, so whatever it brought would be on screen by now if it were not dropped
	 */
	private void awaitTheCancelledRead() throws InterruptedException
	{
		for (Thread thread : Thread.getAllStackTraces().keySet())
		{
			if (READ_THREAD.equals(thread.getName()))
			{
				thread.join(60_000);
				assertFalse(thread.isAlive(), "the cancelled read did not end within a minute");
			}
		}
		robot.waitForIdle();
	}
}
