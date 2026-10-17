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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A value too long to read is shown by its beginning, its end and its length; anything up to
 * {@value LongValues#READABLE} characters is shown as it is (#604, rule 3). The Copy button next
 * to it copies the whole value, which the panel tests check.
 */
class LongValuesTest
{

	@Test
	@DisplayName("a value of 100 characters is shown as it is")
	void aReadableValue_isShownAsItIs()
	{
		String hundred = "a".repeat(LongValues.READABLE);

		assertEquals(hundred, LongValues.shortened(hundred, Locale.ENGLISH));
		assertEquals("", LongValues.shortened("", Locale.ENGLISH));
	}

	@Test
	@DisplayName("a value of 101 characters is shortened, with its length")
	void oneCharacterMore_isShortened()
	{
		String value = "0123456789ab" + "x".repeat(81) + "cdef0123";

		assertEquals("0123456789ab…cdef0123 (101 characters)",
			LongValues.shortened(value, Locale.ENGLISH));
	}

	@Test
	@DisplayName("an ML-DSA-65 account shows its beginning, its end and its length, grouped")
	void anMlDsaAccount_isShortened() 
	{
		String account = "308207b2300b" + "0".repeat(3884) + "bfb098ae";

		assertEquals("308207b2300b…bfb098ae (3,904 characters)",
			LongValues.shortened(account, Locale.ENGLISH));
		assertEquals("308207b2300b…bfb098ae (3.904 Zeichen)",
			LongValues.shortened(account, Locale.GERMAN));
	}
}
