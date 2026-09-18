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
import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * End-to-end use case "open an existing database": the database file already exists on disk (the
 * situation of every returning user), the sign-in dialog is driven exactly as a user does - enable
 * master password, type it, browse to the file, OK - and the application must end up signed in with
 * the memoized sign-in file written
 */
class OpenExistingDatabaseUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	void signInToExistingDatabaseWithPassword() throws IOException
	{
		File databaseFile = new File(tempHome, "existing-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		File memoizedSigninFile = new File(
			MysticCryptApplicationFrame.getInstance().getConfigurationDirectory(),
			MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME);
		assertTrue(memoizedSigninFile.exists(),
			"successful sign-in must write the memoized sign-in file");
	}
}
