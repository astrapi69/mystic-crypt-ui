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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * A {@code memoizedSignin.json} that is not UTF-8 still lets the application start and sign in.
 * <p>
 * A release that ran on a JDK before 18 wrote the file in the platform's charset, which is
 * Windows-1252 on Windows, so a remembered path with an umlaut is the byte 0xFC there, which is not
 * valid UTF-8. file-worker 20.0 replaced such a byte with U+FFFD on reading and the application
 * started; file-worker 21.0 reads strictly and throws a MalformedInputException, which
 * {@code showMasterPwDialog} passes on as a RuntimeException before the dialog is shown.
 */
class ARememberedSignInInAnOtherCharsetStillShowsTheSignInUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("a remembered sign-in written in Windows-1252 still shows the sign-in dialog, and signing in works")
	void aRememberedSignInInWindows1252_stillSignsIn() throws IOException
	{
		File databaseFile = new File(tempHome, "remembered.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		rememberTwoDatabasesInWindows1252(databaseFile);

		SignInDialogSteps signIn = launchApplication();
		signIn.checkMasterPassword().typeMasterPassword(MASTER_PASSWORD).requireOkEnabled()
			.okAndAwaitSignIn();
		new ApplicationSteps(robot).awaitSignedIn();
		assertTrue(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn());
	}

	private void rememberTwoDatabasesInWindows1252(final File databaseFile) throws IOException
	{
		File configurationDirectory = new File(tempHome, ".config/mystic-crypt-ui");
		assertTrue(configurationDirectory.mkdirs());
		String path = databaseFile.getAbsolutePath().replace("\\", "\\\\");
		String json = "{\"selectedApplicationFilePath\":\"" + path
			+ "\",\"applicationFilePaths\":[\"" + path + "\",\"/home/Jürgen/old.mcrdb\"],"
			+ "\"selectedKeyFilePath\":\"\",\"keyFilePaths\":[\"\"],\"withKeyFile\":false,"
			+ "\"withMasterPw\":true}";
		Files.write(
			new File(configurationDirectory,
				MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME).toPath(),
			json.getBytes(StandardCharsets.ISO_8859_1));
	}
}
