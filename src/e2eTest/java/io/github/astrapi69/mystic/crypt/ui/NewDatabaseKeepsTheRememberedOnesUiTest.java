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

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.gson.JsonStringToObjectExtensions;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Creating a database with "New..." in the sign-in dialog, then signing in to it, keeps the
 * databases the sign-in remembered before: the dialog's list offers them at the next start.
 */
class NewDatabaseKeepsTheRememberedOnesUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("after a sign-in to a database made with New..., the remembered list of databases holds only the new one")
	void aSignInToANewDatabase_forgetsTheRememberedOnes() throws Exception
	{
		File earlier = new File(tempHome, "remembered-earlier.mcrdb");
		createDatabaseFileHeadless(earlier, MASTER_PASSWORD);
		signInWithExistingDatabase(earlier, MASTER_PASSWORD);
		File remembered = new File(
			MysticCryptApplicationFrame.getInstance().getConfigurationDirectory(),
			MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME);
		assertTrue(Files.readString(remembered.toPath()).contains(earlier.getName()),
			"the premise: the first sign-in remembered its database");
		shutdownApplication();

		File created = new File(tempHome, "made-with-new.mcrdb");
		SignInDialogSteps signIn = launchApplication();
		signIn.startNewDatabase(created).checkMasterPassword()
			.typeMasterPasswordWithRepeat(MASTER_PASSWORD).requireOkEnabled().okAndAwaitClose();
		signIn.checkMasterPassword().typeMasterPassword(MASTER_PASSWORD).requireOkEnabled()
			.okAndAwaitSignIn();
		new ApplicationSteps(robot).awaitSignedIn();

		Map<?, ?> written = JsonStringToObjectExtensions
			.toObject(Files.readString(remembered.toPath()), Map.class);
		List<?> databases = (List<?>)written.get("applicationFilePaths");
		assertTrue(databases.contains(created.getAbsolutePath()),
			"the new database is in the list: " + databases);
		assertFalse(databases.contains(earlier.getAbsolutePath()),
			"and the one remembered before is gone from it: " + databases);
	}
}
