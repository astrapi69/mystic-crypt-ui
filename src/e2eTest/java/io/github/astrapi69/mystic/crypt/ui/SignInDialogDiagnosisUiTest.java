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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The diagnosis of a failed sign-in dialog lookup (#1070) read off a real sign-in dialog, so that
 * the reflection into the robot's window filter is shown to reach it: a diagnosis that silently
 * answered "false" for everything would look the same on the day it is needed.
 */
class SignInDialogDiagnosisUiTest extends AbstractUiTest
{

	@Test
	@DisplayName("the diagnosis of a showing sign-in dialog names its owner, the main window, and the window events of the launch")
	void theDiagnosisNamesTheOwnerAndTheWindowEvents()
	{
		launchApplication();

		String diagnosis = signInDialogDiagnosis();

		assertTrue(diagnosis.contains("MasterPwFileDialog@") && diagnosis.contains("showing=true"),
			diagnosis);
		assertTrue(diagnosis.contains(
			"owner: io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame@"), diagnosis);
		assertTrue(diagnosis.contains("this is test method number "), diagnosis);
		assertTrue(diagnosis.contains("launchApplication begun"), diagnosis);
		assertTrue(
			diagnosis.contains("WINDOW_OPENED") && diagnosis.contains("Enter your credentials"),
			diagnosis);
	}
}
