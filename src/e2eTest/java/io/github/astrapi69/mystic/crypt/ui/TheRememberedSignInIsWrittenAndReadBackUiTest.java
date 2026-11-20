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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.gson.JsonStringToObjectExtensions;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.signin.MemoizedSigninModelBean;

/**
 * What a sign-in leaves in {@code memoizedSignin.json}, and that the next start reads it back.
 * <p>
 * The file is written through the file-worker and gson-extensions libraries and read through them
 * on the next start, so a library bump changes both halves at once. The other tests that sign in
 * assert that the file exists; this one reads its content and then restarts the application, which
 * is the part a user sees: the database signed in to last time is the one the dialog offers. The
 * database file lies in a directory whose name has umlauts, because a path with them is the case
 * where a charset decides what a later read finds.
 */
class TheRememberedSignInIsWrittenAndReadBackUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("a sign-in writes the database path into memoizedSignin.json as UTF-8, and the next start offers it")
	void aSignIn_isRemembered_andTheNextStartOffersTheSameDatabase() throws Exception
	{
		File directory = new File(tempHome, "Tresor für Jürgen");
		assertTrue(directory.mkdirs(), "precondition: the directory with umlauts exists");
		File databaseFile = new File(directory, "Passwörter.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		File memoizedSigninFile = new File(
			MysticCryptApplicationFrame.getInstance().getConfigurationDirectory(),
			MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME);
		MemoizedSigninModelBean written = JsonStringToObjectExtensions
			.toObject(readAsStrictUtf8(memoizedSigninFile), MemoizedSigninModelBean.class);
		assertEquals(databaseFile.getAbsolutePath(), written.getSelectedApplicationFilePath(),
			"the file holds the path of the database that was signed in to");
		assertTrue(written.getApplicationFilePaths().contains(databaseFile.getAbsolutePath()),
			"and the list of remembered databases holds it too: " + written);
		assertTrue(written.isWithMasterPw(), "and that a master password was used");

		shutdownApplication();
		launchApplication().requireSelectedApplicationFile(databaseFile.getAbsolutePath());
	}

	/** The bytes of the file as UTF-8, a byte that is not valid in it is an error */
	private static String readAsStrictUtf8(final File file) throws IOException
	{
		try
		{
			return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT)
				.decode(ByteBuffer.wrap(Files.readAllBytes(file.toPath()))).toString();
		}
		catch (final CharacterCodingException notUtf8)
		{
			throw new IOException(file + " is not valid UTF-8", notUtf8);
		}
	}
}
