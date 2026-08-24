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

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

/**
 * End-to-end test of the "Enter submits the sign-in" convenience: with a valid master password and
 * application file provided, pressing Enter in the master-password field signs in just like
 * clicking OK. Complements the OK-button state-machine coverage in {@link SignInDialogUiTest}.
 */
class SignInEnterSubmitUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "enter-submit-e2e-pw-123";

	@Test
	void pressingEnterInTheMasterPasswordFieldSignsIn() throws Exception
	{
		File databaseFile = new File(tempHome, "enter-submit-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		SignInDialogSteps signIn = launchApplication();
		signIn.requireOkDisabled().checkMasterPassword().typeMasterPassword(MASTER_PASSWORD)
			.browseApplicationFile(databaseFile).requireOkEnabled()
			.enterInMasterPasswordAndAwaitSignIn();

		// the sign-in dialog closed on Enter; awaitSignedIn returning proves the Enter key signed
		// in
		FrameFixture frame = new ApplicationSteps(robot).awaitSignedIn().showMainFrame();
		assertTrue(GuiActionRunner.execute(() -> frame.target().isShowing()),
			"after Enter-submit sign-in the main application frame must be showing");
	}
}
