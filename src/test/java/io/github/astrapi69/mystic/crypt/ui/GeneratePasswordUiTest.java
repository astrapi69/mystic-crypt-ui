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

import java.awt.Dialog;
import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.Test;

/**
 * End-to-end use case "generate a master password": in the "Create your master key" dialog the
 * Generate button opens the password generator; confirming it must fill both password fields with
 * the same generated password and enable the OK button
 */
class GeneratePasswordUiTest extends AbstractUiTest
{

	@Test
	void generateFillsBothPasswordFieldsAndEnablesOk()
	{
		File newDatabaseFile = new File(tempHome, "generated-pw-database.mcrdb");

		SignInDialogSteps signIn = launchApplication();
		CreateMasterKeySteps createMasterKey = signIn.startNewDatabase(newDatabaseFile);
		createMasterKey.requireOkDisabled().checkMasterPassword();

		// Generate opens the modal password-generator dialog
		SwingUtilities
			.invokeLater(() -> createMasterKey.dialog().button("btnGeneratePw").target().doClick());
		DialogFixture generatorDialog = WindowFinder
			.findDialog(new GenericTypeMatcher<Dialog>(Dialog.class)
			{
				@Override
				protected boolean isMatching(Dialog candidate)
				{
					return "Generate Password".equals(candidate.getTitle())
						&& candidate.isShowing();
				}
			}).withTimeout(10, TimeUnit.SECONDS).using(robot);
		UiTestSpeed.step();

		SwingUtilities.invokeLater(() -> generatorDialog
			.button(JButtonMatcher.withText("Generate Password")).target().doClick());
		Pause.pause(new Condition("password generator dialog is closed")
		{
			@Override
			public boolean test()
			{
				return !generatorDialog.target().isShowing();
			}
		}, 10000);
		UiTestSpeed.step();

		char[] masterPassword = GuiActionRunner
			.execute(() -> ((javax.swing.JPasswordField)createMasterKey.dialog()
				.textBox("txtMasterPw").target()).getPassword());
		char[] repeatedPassword = GuiActionRunner
			.execute(() -> ((javax.swing.JPasswordField)createMasterKey.dialog()
				.textBox("txtRepeatPw").target()).getPassword());
		assertTrue(masterPassword.length >= 6,
			"the generated master password must be filled in and reasonably long");
		assertTrue(java.util.Arrays.equals(masterPassword, repeatedPassword),
			"both password fields must contain the same generated password");

		createMasterKey.requireOkEnabled();
		createMasterKey.cancelAndAwaitClose();
		signIn.cancel();
	}
}
