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

import java.util.Arrays;

import io.github.astrapi69.mystic.crypt.secret.SecretBuffers;

/**
 * How a panel model replaces a secret it holds as {@code char[]}: the old buffer is overwritten,
 * unless a bound field handed the same content back in a new array
 */
final class SecretFields
{

	private SecretFields()
	{
	}

	/**
	 * The new buffer in place of the old one
	 *
	 * @param old
	 *            the buffer held so far
	 * @param replacement
	 *            the new buffer, null for none
	 * @return the buffer to hold now, never null
	 */
	static char[] replaced(final char[] old, final char[] replacement)
	{
		char[] kept = replacement == null ? new char[0] : replacement;
		if (old != kept && !Arrays.equals(old, kept))
		{
			SecretBuffers.wipe(old);
		}
		return kept;
	}
}
