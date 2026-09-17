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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Window;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Where the caret sits in the sign-in dialog. The first thing anyone does here is type the master
 * password, and having to click the field first was the reported complaint.
 * <p>
 * What the window opens on is asked of the window itself rather than of the focus owner: this
 * harness raises and focuses the dialog after finding it, which puts the focus on the window and
 * would hide the answer.
 */
class SigninFocusUiTest extends AbstractUiTest
{

	private String focusedComponentName()
	{
		return GuiActionRunner.execute(() -> {
			Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager()
				.getFocusOwner();
			return focused == null ? "nothing" : String.valueOf(focused.getName());
		});
	}

	@Test
	@DisplayName("the dialog opens on the master password, and the caret follows it into the field")
	void theDialogOpensOnTheMasterPassword()
	{
		SignInDialogSteps signIn = launchApplication();
		robot.waitForIdle();

		// nothing was signed in before, so the master password is not switched on yet and the box
		// that switches it on is what the dialog points the keyboard at
		String initial = GuiActionRunner.execute(() -> {
			Window dialog = signIn.dialog().target();
			Component component = dialog.getFocusTraversalPolicy().getInitialComponent(dialog);
			return component == null ? "nothing" : String.valueOf(component.getName());
		});
		assertEquals("cbxMasterPw", initial,
			"the dialog does not point the keyboard at the master password");

		signIn.checkMasterPassword();
		robot.waitForIdle();

		assertEquals("txtMasterPw", focusedComponentName(),
			"switching the master password on left the caret outside the field");
	}

}
