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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * The foundational end-to-end UI test: creates the very first mystic-crypt database exactly the way
 * a new user does - through the real sign-in dialog's "New..." flow - and signs in with it.
 * <p>
 * Covered flow: launch application, sign-in dialog appears, "New..." opens a save file chooser,
 * choosing the new {@code .mcrdb} file opens the "Create your master key" dialog, master password
 * is entered there, OK creates the encrypted database file on disk, and back in the sign-in dialog
 * the OK button must now be enabled (regression guard for the stale-model bug where it stayed
 * disabled) - clicking it signs in to the freshly created database.
 * <p>
 * Composed from the reusable {@link SignInDialogSteps}/{@link CreateMasterKeySteps}; later UI tests
 * build on the same steps, since without this flow no database exists to work with
 */
class CreateNewDatabaseUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	void createFirstDatabaseThroughNewFlowAndSignIn()
	{
		File newDatabaseFile = new File(tempHome, "my-first-database.mcrdb");

		SignInDialogSteps signIn = launchApplication();

		signIn.requireOkDisabled();

		CreateMasterKeySteps createMasterKey = signIn.startNewDatabase(newDatabaseFile);
		createMasterKey.requireApplicationFile(newDatabaseFile.getAbsolutePath())
			.requireOkDisabled().checkMasterPassword().typeMasterPasswordWithRepeat(MASTER_PASSWORD)
			.requireOkEnabled().okAndAwaitClose();

		assertTrue(newDatabaseFile.exists(), "the new database file must exist on disk");
		assertTrue(newDatabaseFile.length() > 0, "the new database file must not be empty");

		// back in the sign-in dialog: THE regression assertion - after the New... flow the OK
		// button must be enabled (it stayed disabled before the stale-model fix)
		signIn.requireOkEnabled().okAndAwaitSignIn();

		Pause.pause(new Condition("application model is signed in")
		{
			@Override
			public boolean test()
			{
				MysticCryptApplicationFrame applicationFrame = MysticCryptApplicationFrame
					.getInstance();
				return applicationFrame != null && applicationFrame.getModelObject() != null
					&& applicationFrame.getModelObject().isSignedIn();
			}
		}, 15000);

		File memoizedSigninFile = new File(
			MysticCryptApplicationFrame.getInstance().getConfigurationDirectory(),
			MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME);
		assertTrue(memoizedSigninFile.exists(),
			"successful sign-in must write the memoized sign-in file");
	}
}
