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

import java.io.File;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * "Create your master key" asks for the master password twice so that a typo is caught before the
 * database is written with it (#879): the sign-in that follows takes the characters from the
 * dialog, so the user never types the password again until the next start
 */
class RepeatedMasterPasswordUiTest extends AbstractUiTest
{

	@Test
	@DisplayName("pinned: a repeat that differs from the password leaves OK enabled (#879)")
	void aRepeatThatDiffers() throws Exception
	{
		File fresh = new File(tempHome, "a-new-one.mcrdb");

		SignInDialogSteps signIn = launchApplication();
		CreateMasterKeySteps createMasterKey = signIn.startNewDatabase(fresh);
		createMasterKey.checkMasterPassword()
			.typeMasterPassword(TestPasswords.throwaway(), TestPasswords.throwaway())
			.requireOkEnabled();
		createMasterKey.cancelAndAwaitClose();
	}
}
