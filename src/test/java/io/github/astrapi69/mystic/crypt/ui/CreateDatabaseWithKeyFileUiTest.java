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
import java.util.concurrent.TimeUnit;

import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * End-to-end use case "create a database protected by password AND private key file" - the
 * strongest sign-in variant this application offers (and the one a security-conscious user picks):
 * through the "New..." flow, enable both master password and key file, generate an RSA private key
 * via "Create key file...", save it, create the database - then sign in with password plus key
 * file, restart, and sign in again the same way
 */
class CreateDatabaseWithKeyFileUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();
	private static final String KEY_FILE_NAME = "test-private-key.pem";

	@Test
	void createDatabaseWithPasswordAndKeyFileAndSignIn() throws Exception
	{
		File newDatabaseFile = new File(tempHome, "keyfile-database.mcrdb");

		SignInDialogSteps signIn = launchApplication();
		CreateMasterKeySteps createMasterKey = signIn.startNewDatabase(newDatabaseFile);
		createMasterKey.requireApplicationFile(newDatabaseFile.getAbsolutePath())
			.requireOkDisabled().checkMasterPassword().typeMasterPasswordWithRepeat(MASTER_PASSWORD)
			.checkKeyFile().createKeyFile(KEY_FILE_NAME).requireOkEnabled().okAndAwaitClose();

		assertTrue(newDatabaseFile.exists(), "the new database file must exist on disk");
		assertTrue(newDatabaseFile.length() > 0, "the new database file must not be empty");
		File keyFile = new File(
			MysticCryptApplicationFrame.getInstance().getConfigurationDirectory(), KEY_FILE_NAME);
		assertTrue(keyFile.exists(), "the generated private key file must exist on disk");
		assertTrue(keyFile.length() > 0, "the generated private key file must not be empty");

		// back in the sign-in dialog: sign in with password AND key file
		signIn.checkMasterPassword().typeMasterPassword(MASTER_PASSWORD).checkKeyFile()
			.browseKeyFile(keyFile).requireOkEnabled().okAndAwaitSignIn();
		awaitSignedIn();

		shutdownApplication();

		// returning user: sign in again with password plus key file
		SignInDialogSteps signInAgain = launchApplication();
		signInAgain.checkMasterPassword().typeMasterPassword(MASTER_PASSWORD).checkKeyFile()
			.browseKeyFile(keyFile).browseApplicationFile(newDatabaseFile).requireOkEnabled()
			.okAndAwaitSignIn();
		awaitSignedIn();
	}

	private void awaitSignedIn()
	{
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
		}, TimeUnit.SECONDS.toMillis(20));
	}
}
