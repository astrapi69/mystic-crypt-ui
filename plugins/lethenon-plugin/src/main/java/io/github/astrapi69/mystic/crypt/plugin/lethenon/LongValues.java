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

import java.util.Locale;

/**
 * A value too long to read, shown by its beginning, its end and its length (#604, rule 3): an
 * ML-DSA-65 account is about 3,900 hexadecimal characters, which nobody reads and every field
 * scrolls. The window puts a Copy button beside it, and that button always copies the whole value.
 */
final class LongValues
{

	/** The longest value shown as it is; a published address, 177 characters, is longer */
	static final int READABLE = 100;

	/** The characters shown from the beginning of a long value */
	static final int BEGINNING = 12;

	/** The characters shown from the end of a long value */
	static final int END = 8;

	private LongValues()
	{
	}

	/**
	 * Shortens a value that is too long to read
	 *
	 * @param value
	 *            the value
	 * @param locale
	 *            the language of the window, for the word and the grouping of the length
	 * @return the value itself up to {@link #READABLE} characters, its beginning, its end and its
	 *         length beyond
	 */
	static String shortened(final String value, final Locale locale)
	{
		if (value.length() <= READABLE)
		{
			return value;
		}
		return LethenonMessages.format("lethenon.value.shortened",
			"{0}\u2026{1} ({2,number,integer} characters)", locale,
			value.substring(0, BEGINNING), value.substring(value.length() - END), value.length());
	}
}
