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

import java.io.File;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * The key file the sign-in dialog offers for the database that is chosen (#997).
 * <p>
 * Two databases are created through "New...", each with a key file of its own, and each is signed
 * in to once. After a restart the dialog offers the database signed in to last.
 */
class KeyFileFollowsTheDatabaseUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("pinned on develop (#997): choosing the first database offers the key file of the second one")
	void choosingAnotherDatabase_keepsTheKeyFileOfTheDatabaseSignedInToLast() throws Exception
	{
		File first = new File(tempHome, "first.mcrdb");
		File second = new File(tempHome, "second.mcrdb");
		createAndSignIn(first, "first-key.pem");
		shutdownApplication();
		File secondKey = createAndSignIn(second, "second-key.pem");
		shutdownApplication();

		SignInDialogSteps signIn = launchApplication();
		signIn.browseApplicationFile(first);

		assertEquals(secondKey.getAbsolutePath(), signIn.selectedKeyFile(),
			"today the key file of the database signed in to last stays in the field");
	}

	private File createAndSignIn(final File database, final String keyFileName)
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
}
