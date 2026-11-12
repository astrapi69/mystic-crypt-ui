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

import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * New... picks a first file, Browse... in "Create your master key" changes it: the database is
 * written to the browsed file, and the sign-in dialog must then show and open exactly that file -
 * not the one picked first, which stays empty (#890)
 */
class BrowsedDatabaseFileUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	void theSignInOpensTheFileTheCreateDialogWrote()
	{
		File firstPicked = new File(tempHome, "first-picked.mcrdb");
		File browsedInstead = new File(tempHome, "browsed-instead.mcrdb");

		SignInDialogSteps signIn = launchApplication();

		CreateMasterKeySteps createMasterKey = signIn.startNewDatabase(firstPicked);
		createMasterKey.browseApplicationFile(browsedInstead)
			.requireApplicationFile(browsedInstead.getAbsolutePath()).checkMasterPassword()
			.typeMasterPasswordWithRepeat(MASTER_PASSWORD).requireOkEnabled().okAndAwaitClose();

		assertTrue(browsedInstead.exists() && browsedInstead.length() > 0,
			"the database must be written to the browsed file");

		signIn.requireSelectedApplicationFile(browsedInstead.getAbsolutePath());
		signIn.requireOkEnabled().okAndAwaitSignIn();

		Pause.pause(new Condition("application model is signed in")
		{
			@Override
			public boolean test()
			{
				MysticCryptApplicationFrame applicationFrame = MysticCryptApplicationFrame
					.getInstance();
				return applicationFrame != null && applicationFrame.getModelObject() != null
					&& applicationFrame.getModelObject().isSignedIn();
			}
		}, 15000);
	}
}
