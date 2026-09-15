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
package io.github.astrapi69.mystic.crypt.panel.signin;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A password field notifies its document listener on every keystroke, and every one of the three
 * panels that bind a master password field to this bean calls {@code getPassword()} on the field
 * and {@code setMasterPw} on the bean for each notification. {@link javax.swing.JPasswordField}
 * hands out a FRESH array on every {@code getPassword()} call, so typing an n-character password
 * called {@code setMasterPw} n times, and the array from every call before the last one was simply
 * replaced - left on the heap, never overwritten, the longest of them being the password minus its
 * final character (#351).
 */
class MasterPwFileModelBeanTest
{

	@Test
	@DisplayName("setMasterPw overwrites the array it replaces, not only the reference to it")
	void setMasterPw_wipesThePreviousValue_beforeReplacingIt()
	{
		MasterPwFileModelBean credentials = new MasterPwFileModelBean();
		char[] firstKeystroke = "s".toCharArray();
		char[] secondKeystroke = "se".toCharArray();
		credentials.setMasterPw(firstKeystroke);

		credentials.setMasterPw(secondKeystroke);

		assertArrayEquals(new char[firstKeystroke.length], firstKeystroke,
			"the array from the first call is what 'un-wiped copy per keystroke' means - it has "
				+ "to be zero-filled once nothing points at it as the current password any more, "
				+ "not merely dereferenced");
		assertArrayEquals(secondKeystroke, credentials.getMasterPw(),
			"and the replacement itself must arrive untouched - wiping the WRONG array would pass "
				+ "the assertion above by accident while corrupting the password actually in use");
	}

	@Test
	@DisplayName("replacing null does not throw")
	void setMasterPw_acceptsNull_whenThereWasNothingToWipe()
	{
		MasterPwFileModelBean credentials = new MasterPwFileModelBean();

		credentials.setMasterPw("first".toCharArray());
		credentials.setMasterPw(null);

		assertEquals(null, credentials.getMasterPw());
	}

	@Test
	@DisplayName("setting the same array back onto itself does not wipe it")
	void setMasterPw_doesNotWipe_whenTheNewValueIsTheOldOne()
	{
		MasterPwFileModelBean credentials = new MasterPwFileModelBean();
		char[] password = "unchanged".toCharArray();
		credentials.setMasterPw(password);

		credentials.setMasterPw(password);

		assertArrayEquals("unchanged".toCharArray(), credentials.getMasterPw(),
			"the array being set is the one already stored - wiping it first would wipe the very "
				+ "value being assigned, the self-assignment trap a naive 'wipe old, then store "
				+ "new' has to guard against explicitly");
	}
}
