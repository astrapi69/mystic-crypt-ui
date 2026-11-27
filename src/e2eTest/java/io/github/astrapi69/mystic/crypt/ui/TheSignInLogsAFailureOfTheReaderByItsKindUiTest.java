/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileReader;
import io.github.astrapi69.mystic.crypt.app.file.xml.PasswordVaultFormat;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwWithApplicationFilePanel;

/**
 * What the running application shows and logs when a database opens with the right password and the
 * reader fails on its root before it converts anything: the dialog of the sign-in names the kind of
 * failure, and the record of the reader and the record the sign-in panel writes for the refusal,
 * printed with every cause and suppressed exception, name the failure by its kind and type. The
 * panel writes its record only after the refusal's dialog is closed, so it is read here through the
 * application, not in a unit test.
 */
class TheSignInLogsAFailureOfTheReaderByItsKindUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	/** A root name whose escape is no hex number */
	private static final String A_ROOT_NAME_WITH_AN_ESCAPE_THAT_IS_NO_NUMBER = "<a_.zzzz/>";

	/** A text the message of the reader's failure carries for that root */
	private static final String TEXT_OF_THE_FAILURE = "zzzz";

	private final List<Logger> recordedLoggers = List.of(
		Logger.getLogger(ApplicationXmlFileReader.class.getName()),
		Logger.getLogger(MasterPwWithApplicationFilePanel.class.getName()));

	private final List<LogRecord> records = new ArrayList<>();

	private final Handler recorder = new Handler()
	{
		@Override
		public void publish(final LogRecord logRecord)
		{
			records.add(logRecord);
		}

		@Override
		public void flush()
		{
		}

		@Override
		public void close()
		{
		}
	};

	@BeforeEach
	void recordTheReaderAndTheSignIn()
	{
		recorder.setLevel(Level.ALL);
		recordedLoggers.forEach(logger -> logger.addHandler(recorder));
	}

	@AfterEach
	void stopRecording()
	{
		recordedLoggers.forEach(logger -> logger.removeHandler(recorder));
	}

	@Test
	@DisplayName("a root the reader fails on: the dialog and the records name the failure by its kind and type")
	void aRootTheReaderFailsOn_isNamedAndLoggedByItsKindAndType() throws Exception
	{
		File databaseFile = new File(tempHome, "a-root-the-reader-fails-on.mcrdb");
		Files.write(databaseFile.toPath(),
			PasswordVaultFormat.encrypt(A_ROOT_NAME_WITH_AN_ESCAPE_THAT_IS_NO_NUMBER.toCharArray(),
				MASTER_PASSWORD.toCharArray()));

		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().checkMasterPassword().typeMasterPassword(MASTER_PASSWORD)
			.browseApplicationFile(databaseFile).requireOkEnabled().clickOk();
		String shown = new ApplicationSteps(robot)
			.readAndDismissMessageDialog("Database not opened");
		robot.waitForIdle();

		assertTrue(shown.endsWith("<p>This database was decrypted, but its content could not be "
			+ "read: the reader failed with NumberFormatException. It was not opened, and the file is "
			+ "unchanged."), shown);
		assertTrue(records.stream().anyMatch(logRecord -> MasterPwWithApplicationFilePanel.class
			.getName().equals(logRecord.getLoggerName())), "the sign-in panel logged the refusal");
		for (LogRecord logRecord : records)
		{
			assertFalse(printed(logRecord).contains(TEXT_OF_THE_FAILURE),
				() -> "the record of " + logRecord.getLoggerName() + " at " + logRecord.getLevel());
		}

		// end the flow the way a user would who gives up: the dialog stays open after a
		// refusal, and a modal dialog left standing blocks the next test of this class (#251)
		signIn.cancel();
	}

	/**
	 * A record as a formatter can print it: its message, its parameters, and the stack trace of its
	 * throwable with every cause and every suppressed exception
	 */
	private static String printed(final LogRecord logRecord)
	{
		StringBuilder printed = new StringBuilder(String.valueOf(logRecord.getMessage()));
		if (logRecord.getParameters() != null)
		{
			for (Object parameter : logRecord.getParameters())
			{
				printed.append(' ').append(parameter);
			}
		}
		if (logRecord.getThrown() != null)
		{
			StringWriter stackTrace = new StringWriter();
			logRecord.getThrown().printStackTrace(new PrintWriter(stackTrace));
			printed.append(stackTrace);
		}
		return printed.toString();
	}
}
