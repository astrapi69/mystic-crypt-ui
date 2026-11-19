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
import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * End-to-end use case "sign in with a wrong password": the error dialog "Authentication with
 * Password" must appear, the application must NOT be signed in, and the sign-in dialog must stay
 * open so the password can be corrected.
 * <p>
 * The last part is what #251 fixed. Before it, the dialog disposed itself unconditionally after a
 * failed attempt, and since the main frame is only shown after a successful sign-in, dismissing the
 * error left NO window on screen at all - measured, not inferred: "windows showing: (none)". From
 * the outside the application had vanished after a typo, while its process kept running.
 */
class WrongPasswordSignInUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	void wrongPasswordShowsErrorAndDoesNotSignIn() throws IOException
	{
		File databaseFile = new File(tempHome, "wrong-pw-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().checkMasterPassword()
			.typeMasterPassword(TestPasswords.throwaway()).browseApplicationFile(databaseFile)
			.requireOkEnabled().clickOk();

		String shown = new ApplicationSteps(robot)
			.readAndDismissMessageDialog("Authentication with Password");

		assertTrue(shown.endsWith("<p>Wrong master password."),
			"the user reads the unlock prompt's words: " + shown);
		assertNoCipherText(shown);
		assertFalse(
			MysticCryptApplicationFrame.getInstance() != null
				&& MysticCryptApplicationFrame.getInstance().getModelObject() != null
				&& MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn(),
			"a wrong password must never result in a signed-in application");

		// end the flow the way a user would who gives up: since #251 the dialog stays open after a
		// failed attempt, and a modal dialog left standing blocks the application thread, which the
		// next test in this class inherits (forkEvery=1 forks per class, not per method)
		signIn.cancel();
	}

	@Test
	@DisplayName("a wrong key file shows a message, not the cipher's error text (#1005)")
	void wrongKeyFileShowsAMessageNotTheCipherText() throws Exception
	{
		File databaseFile = new File(tempHome, "wrong-key-database.mcrdb");
		File keyFile = new File(tempHome, "right-key.pem");
		createDatabaseFileWithKeyFileHeadless(databaseFile, MASTER_PASSWORD, keyFile);
		File foreignKeyFile = new File(tempHome, "foreign-key.pem");
		io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter.writeInPemFormat(
			io.github.astrapi69.crypt.data.factory.KeyPairFactory.newKeyPair("RSA").getPrivate(),
			foreignKeyFile);

		SignInDialogSteps signIn = launchApplication();
		signIn.checkMasterPassword().typeMasterPassword(MASTER_PASSWORD).checkKeyFile()
			.browseKeyFile(foreignKeyFile).browseApplicationFile(databaseFile).requireOkEnabled()
			.clickOk();

		String shown = new ApplicationSteps(robot)
			.readAndDismissMessageDialog("Authentication with Password or key file");

		assertTrue(shown.endsWith("<p>The master password or the key file is wrong."),
			"the user reads the unlock prompt's words: " + shown);
		assertNoCipherText(shown);
		signIn.cancel();
	}

	private static void assertNoCipherText(final String shown)
	{
		for (String cipherText : new String[] { "Padding error", "pad block", "Exception",
				"could not decrypt" })
		{
			assertFalse(shown.contains(cipherText), "no exception text is shown: " + shown);
		}
	}

	@Test
	@DisplayName("a wrong password leaves the sign-in dialog open, and the corrected one signs in")
	void wrongPasswordLeavesTheDialogOpenForAnotherAttempt() throws IOException
	{
		File databaseFile = new File(tempHome, "retry-after-wrong-pw-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().checkMasterPassword()
			.typeMasterPassword(TestPasswords.throwaway()).browseApplicationFile(databaseFile)
			.requireOkEnabled().clickOk();
		new ApplicationSteps(robot).dismissMessageDialog("Authentication with Password");

		assertTrue(signIn.dialogIsShowing(),
			"after a failed attempt the sign-in dialog must still be there - it is the only window "
				+ "the application has at that point, and disposing it leaves nothing on screen");

		signIn.typeMasterPassword(MASTER_PASSWORD);
		signIn.okAndAwaitSignIn();

		assertTrue(MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn(),
			"correcting the password in the same dialog must sign in, without a restart");
	}
}
