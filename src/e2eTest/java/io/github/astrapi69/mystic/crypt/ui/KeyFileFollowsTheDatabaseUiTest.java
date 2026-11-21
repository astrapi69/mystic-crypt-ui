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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * The key file and the factors the sign-in dialog offers for the database that is chosen (#997).
 * <p>
 * Databases are created through "New...", each is signed in to once, and after a restart the dialog
 * offers the one signed in to last. Choosing another database in it offers what that database was
 * opened with, and a database the remembered sign-in knows nothing about leaves the fields as they
 * are.
 */
class KeyFileFollowsTheDatabaseUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("choosing the first database offers its own key file, not the one of the database signed in to last")
	void choosingAnotherDatabase_offersTheKeyFileThatDatabaseWasOpenedWith() throws Exception
	{
		File first = new File(tempHome, "first.mcrdb");
		File second = new File(tempHome, "second.mcrdb");
		File firstKey = createWithKeyFileAndSignIn(first, "first-key.pem");
		shutdownApplication();
		createWithKeyFileAndSignIn(second, "second-key.pem");
		shutdownApplication();

		SignInDialogSteps signIn = launchApplication();
		signIn.browseApplicationFile(first);

		assertEquals(firstKey.getAbsolutePath(), signIn.selectedKeyFile());
		assertTrue(signIn.keyFileIsChecked() && signIn.masterPasswordIsChecked(),
			"both factors, as the database was opened");
	}

	@Test
	@DisplayName("each database offers its factors, and one the remembered sign-in does not know leaves the fields alone")
	void choosingDatabases_setsTheFieldsFromTheirEntries_andLeavesThemForAnUnknownOne()
		throws Exception
	{
		File withKey = new File(tempHome, "with-key.mcrdb");
		File passwordOnly = new File(tempHome, "password-only.mcrdb");
		File neverSignedIn = new File(tempHome, "never-signed-in.mcrdb");
		createDatabaseFileHeadless(neverSignedIn, MASTER_PASSWORD);
		File key = createWithKeyFileAndSignIn(withKey, "with-key.pem");
		shutdownApplication();
		createWithPasswordAloneAndSignIn(passwordOnly);
		shutdownApplication();

		SignInDialogSteps signIn = launchApplication();
		assertFalse(signIn.keyFileIsChecked(), "the database signed in to last used no key file");
		signIn.browseApplicationFile(withKey);
		assertEquals(key.getAbsolutePath(), signIn.selectedKeyFile());
		assertTrue(signIn.keyFileIsChecked() && signIn.masterPasswordIsChecked());

		signIn.browseApplicationFile(neverSignedIn);
		assertEquals(key.getAbsolutePath(), signIn.selectedKeyFile(), "no entry, field alone");
		assertTrue(signIn.keyFileIsChecked() && signIn.masterPasswordIsChecked());

		signIn.browseApplicationFile(passwordOnly);
		assertFalse(signIn.keyFileIsChecked(), "this database was opened with the password alone");
		assertTrue(signIn.masterPasswordIsChecked());
		assertTrue(signIn.selectedKeyFile() == null || signIn.selectedKeyFile().isEmpty(),
			"and no key file is offered for it: " + signIn.selectedKeyFile());
	}

	@Test
	@DisplayName("choosing a database in the dropdown offers its key file as Browse... does")
	void choosingDatabasesInTheDropdown_offersTheirKeyFiles() throws Exception
	{
		File first = new File(tempHome, "dropdown-first.mcrdb");
		File second = new File(tempHome, "dropdown-second.mcrdb");
		File firstKey = createWithKeyFileAndSignIn(first, "dropdown-first-key.pem");
		shutdownApplication();
		File secondKey = createWithKeyFileAndSignIn(second, "dropdown-second-key.pem");
		shutdownApplication();

		SignInDialogSteps signIn = launchApplication();
		// both are listed once they have been browsed to; the list a "New..." run remembers holds
		// only the database it created
		signIn.browseApplicationFile(first).browseApplicationFile(second);
		signIn.chooseApplicationFileInDropdown(first);
		assertEquals(firstKey.getAbsolutePath(), signIn.selectedKeyFile());
		signIn.chooseApplicationFileInDropdown(second);
		assertEquals(secondKey.getAbsolutePath(), signIn.selectedKeyFile());
		signIn.chooseApplicationFileInDropdown(first);
		assertEquals(firstKey.getAbsolutePath(), signIn.selectedKeyFile(), "and back again");
	}

	private File createWithKeyFileAndSignIn(final File database, final String keyFileName)
	{
		SignInDialogSteps signIn = launchApplication();
		signIn.startNewDatabase(database).checkMasterPassword()
			.typeMasterPasswordWithRepeat(MASTER_PASSWORD).checkKeyFile().createKeyFile(keyFileName)
			.requireOkEnabled().okAndAwaitClose();
		signIn.checkMasterPassword().typeMasterPassword(MASTER_PASSWORD).requireOkEnabled()
			.okAndAwaitSignIn();
		new ApplicationSteps(robot).awaitSignedIn();
		return new File(MysticCryptApplicationFrame.getInstance().getConfigurationDirectory(),
			keyFileName);
	}

	private void createWithPasswordAloneAndSignIn(final File database)
	{
		SignInDialogSteps signIn = launchApplication();
		signIn.startNewDatabase(database).checkMasterPassword()
			.typeMasterPasswordWithRepeat(MASTER_PASSWORD).requireOkEnabled().okAndAwaitClose();
		signIn.checkMasterPassword().uncheckKeyFile().typeMasterPassword(MASTER_PASSWORD)
			.requireOkEnabled().okAndAwaitSignIn();
		new ApplicationSteps(robot).awaitSignedIn();
	}
}
