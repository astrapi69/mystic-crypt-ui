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

import io.github.astrapi69.lethenon.ChainRejected;

/**
 * Turns what a window caught into the two sentences of draft 07 (#710), decided by the kind of the
 * failure, never by the words of its message: a wallet password that does not open the wallet, a
 * chain that does not verify, no chain at the place named; anything else gets the window's own
 * sentence, with its message under the details
 */
final class Failures
{

	private Failures()
	{
	}

	/**
	 * The failure as the window shows it
	 *
	 * @param thrown
	 *            what the window caught
	 * @param windowCause
	 *            the window's own sentence, for a failure of no known kind
	 * @param locale
	 *            the language of the window
	 * @return the failure
	 */
	static Failure of(final Throwable thrown, final Refusal windowCause, final Locale locale)
	{
		String details = thrown.getMessage() == null ? thrown.toString() : thrown.getMessage();
		if (isWrongPassword(thrown))
		{
			return new Failure(text("lethenon.failure.password.cause",
				"The wallet password is not right.", locale),
				text("lethenon.failure.password.action",
					"Type it again. It is the password of the wallet, not of the vault.", locale),
				details);
		}
		if (thrown instanceof ChainRejected)
		{
			return new Failure(text("lethenon.failure.chain.cause",
				"The chain is not in order; nothing in it can be relied on.", locale),
				text("lethenon.failure.chain.action", "Fetch the chain again from a node.", locale),
				details);
		}
		if (thrown instanceof NoChainFile noChain)
		{
			return new Failure(text("lethenon.failure.no.chain.cause",
				"There is no chain at this place yet.", locale),
				noChain.chainFile() + "\n" + text("lethenon.failure.no.chain.action",
					"Fetch the chain from a node, or choose another place.", locale),
				details);
		}
		return new Failure(windowCause.text(locale), text("lethenon.failure.other.action",
			"Look at the details below, then try again.", locale), details);
	}

	/**
	 * A password the wallet refused, whether it came through the vault or through a wallet file
	 * whose refusal carries the library's {@link SecurityException} as its cause
	 */
	private static boolean isWrongPassword(final Throwable thrown)
	{
		return thrown instanceof SecurityException
			|| thrown.getCause() instanceof SecurityException;
	}

	private static String text(final String key, final String defaultValue, final Locale locale)
	{
		return LethenonMessages.getString(key, defaultValue, locale);
	}
}
