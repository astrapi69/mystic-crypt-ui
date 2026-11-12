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

import java.io.File;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;

/**
 * What the unlock prompt asks of a database that opens with a key file (#880).
 * <p>
 * PIN of the behaviour before the fix, green on it: a database with its key file alone cannot be
 * unlocked at all. The fix turns it.
 */
class UnlockAsksForTheFactorsOfOpeningUiTest extends AbstractUiTest
{

	private static final String KEY_FILE_NAME = "unlock-factors.pem";

	@Test
	@DisplayName("PIN: a key-file-only database cannot be unlocked, whatever is typed")
	void aKeyFileOnlyDatabase_cannotBeUnlocked() throws Exception
	{
		signInWithANewDatabase(null);
		ApplicationSteps application = new ApplicationSteps(robot).awaitSignedIn();

		application.lockWorkspace().enterUnlockPasswordExpectingFailure("anything at all");

		assertFalse(signedIn(), "today the prompt asks for a password this database does not have");
		application.cancelUnlock();
	}

	/**
	 * Creates a database with a key file, and a master password when one is given, and signs in
	 * with what it was created with
	 *
	 * @param password
	 *            the master password, or null for a database that opens with its key file alone
	 * @return the key file
	 */
	private File signInWithANewDatabase(final String password) throws Exception
	{
		File database = new File(tempHome, "unlock-factors.mcrdb");
		SignInDialogSteps signIn = launchApplication();
		CreateMasterKeySteps createMasterKey = signIn.startNewDatabase(database);
		if (password != null)
		{
			createMasterKey.checkMasterPassword().typeMasterPasswordWithRepeat(password);
		}
		createMasterKey.checkKeyFile().createKeyFile(KEY_FILE_NAME).okAndAwaitClose();
		File keyFile = new File(
			MysticCryptApplicationFrame.getInstance().getConfigurationDirectory(), KEY_FILE_NAME);
		if (password != null)
		{
			signIn.checkMasterPassword().typeMasterPassword(password);
		}
		else
		{
			signIn.uncheckMasterPassword();
		}
		signIn.checkKeyFile().browseKeyFile(keyFile).okAndAwaitSignIn();
		return keyFile;
	}

	private static boolean signedIn()
	{
		return GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn());
	}
}
