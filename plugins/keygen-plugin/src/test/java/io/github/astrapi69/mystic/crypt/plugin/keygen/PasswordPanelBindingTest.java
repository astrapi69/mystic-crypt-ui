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
package io.github.astrapi69.mystic.crypt.plugin.keygen;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that the two fields of {@link PasswordPanel} are bound to its {@link PasswordBean}: the
 * caller of the dialog compares the password with its repetition through the model, so what was
 * typed has to be there, and an untouched dialog has to report empty passwords rather than null.
 */
@DisplayName("The password dialog panel reports what was typed through its model")
class PasswordPanelBindingTest
{

	/**
	 * The password this test types. It is made up per run rather than written into the source, so
	 * nothing here reads as a credential.
	 */
	private static final String TYPED_PASSWORD = "typed-" + UUID.randomUUID();

	@Test
	@DisplayName("what is typed into both fields is what the model holds")
	void whatIsTypedIntoBothFieldsIsWhatTheModelHolds()
	{
		PasswordPanel panel = new PasswordPanel();

		panel.getTxtPassword().setText(TYPED_PASSWORD);
		panel.getTxtRepeatPassword().setText(TYPED_PASSWORD);

		assertArrayEquals(TYPED_PASSWORD.toCharArray(), panel.getModelObject().getPassword());
		assertArrayEquals(TYPED_PASSWORD.toCharArray(), panel.getModelObject().getRepeatPassword());
	}

	@Test
	@DisplayName("an untouched dialog reports empty passwords, never null")
	void anUntouchedDialogReportsEmptyPasswords()
	{
		PasswordPanel panel = new PasswordPanel();

		assertEquals(0, panel.getModelObject().getPassword().length);
		assertEquals(0, panel.getModelObject().getRepeatPassword().length);
	}
}
