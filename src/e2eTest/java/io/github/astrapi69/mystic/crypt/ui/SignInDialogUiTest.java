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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

/**
 * AssertJ-Swing regression test for the sign-in dialog's OK-button state machine: the OK button
 * must stay disabled until both a master password and an application file are provided, and must
 * enable once they are.
 * <p>
 * Builds on the flow proven by {@link CreateNewDatabaseUiTest} (the foundational first-database
 * test) and is composed from the same {@link SignInDialogSteps}; this test only checks button-state
 * transitions, so a dummy application file is enough - it ends with the Cancel button, never
 * actually signing in
 */
class SignInDialogUiTest extends AbstractUiTest
{

	@Test
	void okButtonEnablesOnlyAfterPasswordAndApplicationFileAreProvided() throws IOException
	{
		File dummyApplicationFile = new File(tempHome, "sign-in-ui-test.mcrdb");
		Files.write(dummyApplicationFile.toPath(),
			"not-a-real-mystic-crypt-database".getBytes(StandardCharsets.UTF_8));

		SignInDialogSteps signIn = launchApplication();

		signIn.requireOkDisabled().checkMasterPassword().typeMasterPassword("test-password")
			.requireOkDisabled().browseApplicationFile(dummyApplicationFile).requireOkEnabled()
			.cancel();
	}
}
