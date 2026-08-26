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

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * End-to-end use case "sign in with a wrong password": the error dialog "Authentication with
 * Password" must appear and the application must NOT be signed in afterwards.
 * <p>
 * Documents current behavior: after dismissing the error the sign-in dialog is disposed anyway (the
 * dialog's onOk disposes unconditionally since the panel swallows the failure internally), so the
 * user cannot retry without restarting - candidate for a UX fix, at which point this test should
 * assert the dialog stays open instead
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
		signIn.requireOkDisabled().checkMasterPassword().typeMasterPassword("totally-wrong-pw")
			.browseApplicationFile(databaseFile).requireOkEnabled().clickOk();

		new ApplicationSteps(robot).dismissMessageDialog("Authentication with Password");

		assertFalse(
			MysticCryptApplicationFrame.getInstance() != null
				&& MysticCryptApplicationFrame.getInstance().getModelObject() != null
				&& MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn(),
			"a wrong password must never result in a signed-in application");
	}
}
