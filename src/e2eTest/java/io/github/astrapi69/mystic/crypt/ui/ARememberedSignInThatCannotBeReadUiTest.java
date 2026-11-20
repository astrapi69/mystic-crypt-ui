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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * A {@code memoizedSignin.json} that cannot be read, a JSON cut short by a crash or another
 * program, does not stop the start, and it is kept rather than written over (#1082)
 */
class ARememberedSignInThatCannotBeReadUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	/** A remembered sign-in cut off in the middle of its first value */
	private static final byte[] CUT_SHORT = "{\"selectedApplicationFilePath\": \"/home/user/va"
		.getBytes(StandardCharsets.UTF_8);

	@Test
	@DisplayName("a remembered sign-in that cannot be read stops the start before the sign-in dialog (today, #1082)")
	void aRememberedSignInThatCannotBeRead_stopsTheStart() throws IOException
	{
		File databaseFile = new File(tempHome, "cut-short.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		writeTheRememberedSignIn(CUT_SHORT);

		IllegalStateException startEnded = assertThrows(IllegalStateException.class,
			this::launchApplication, "the JSON parser's exception ends the start thread");
		assertTrue(startEnded.getMessage().contains("start thread ended"),
			"the start thread ended before the sign-in dialog opened: " + startEnded.getMessage());
	}

	private File writeTheRememberedSignIn(final byte[] content) throws IOException
	{
		File configurationDirectory = new File(tempHome, ".config/mystic-crypt-ui");
		assertTrue(configurationDirectory.mkdirs() || configurationDirectory.isDirectory());
		File memoizedSigninFile = new File(configurationDirectory,
			MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME);
		Files.write(memoizedSigninFile.toPath(), content);
		return memoizedSigninFile;
	}
}
