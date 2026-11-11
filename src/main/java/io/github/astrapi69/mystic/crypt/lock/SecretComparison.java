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
package io.github.astrapi69.mystic.crypt.lock;

/**
 * Compares one typed secret with another in time that does not depend on where they differ, and
 * without copying either (#916, as #879 asked for Repeat Password).
 * <p>
 * {@code java.util.Arrays.equals} returns at the first character that differs, so the time it takes
 * says how much of a guess was right. Bouncy Castle's {@code constantTimeAreEqual} looks at every
 * character; it answers {@code false} for a null array, and this keeps the meaning of
 * {@code Arrays.equals} instead: two missing secrets are the same, a missing and a present one are
 * not, which is what the callers relied on
 */
public final class SecretComparison
{

	private SecretComparison()
	{
	}

	/**
	 * Whether the two arrays hold the same characters
	 *
	 * @param first
	 *            one secret, may be null
	 * @param second
	 *            the other, may be null
	 * @return true if both are null, or both hold the same characters
	 */
	public static boolean sameCharacters(final char[] first, final char[] second)
	{
		if (first == null || second == null)
		{
			return first == second;
		}
		return org.bouncycastle.util.Arrays.constantTimeAreEqual(first, second);
	}
}
