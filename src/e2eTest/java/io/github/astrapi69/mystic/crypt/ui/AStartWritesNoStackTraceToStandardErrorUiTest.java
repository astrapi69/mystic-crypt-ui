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

import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * A start of the application, up to the sign-in dialog and through the sign-in, writes no exception
 * and no stack trace to standard error and reports no uncaught exception on any thread, the event
 * dispatch thread included. A stack trace in the log of an ordinary start hides the one that
 * matters when something really breaks, and it is a defect of its own (#1245).
 */
class AStartWritesNoStackTraceToStandardErrorUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	void startAndSignIn_writesNoStackTraceToStandardError_whenTheVaultIsOpened() throws Exception
	{
		File databaseFile = new File(tempHome, "quiet-start-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		try (StartupWatch watch = StartupWatch.begin())
		{
			signInWithExistingDatabase(databaseFile, MASTER_PASSWORD).showMainFrame();
			awaitApplicationInitialized();
			robot.waitForIdle();

			String standardError = watch.standardError();
			assertTrue(watch.uncaught().isEmpty(),
				"uncaught exceptions during the start: " + watch.uncaught());
			assertTrue(
				!standardError.contains("Exception") && !standardError.contains("Error")
					&& !standardError.contains("\tat "),
				"standard error of the start holds an exception or a stack trace:\n"
					+ standardError);
		}
	}
}
