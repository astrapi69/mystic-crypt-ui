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
 * What the confirmation before sending on the main network shows (#704, text T5): every value as
 * the window formats it, the recipient whole, never shortened
 *
 * @param amount
 *            the amount sent
 * @param fee
 *            the fee
 * @param total
 *            amount and fee together
 * @param recipient
 *            the whole recipient as typed
 */
record MainNetworkTransfer(String amount, String fee, String total, String recipient)
{

	/**
	 * @param locale
	 *            the language of the window
	 * @return the title
	 */
	String title(final Locale locale)
	{
		return LethenonMessages.getString("lethenon.send.confirm.mainnet.title",
			"Confirm sending on the MAIN NETWORK", locale);
	}

	/**
	 * @param locale
	 *            the language of the window
	 * @return the main sentence, with the amount and the whole recipient
	 */
	String sentence(final Locale locale)
	{
		return LethenonMessages.format("lethenon.send.confirm.mainnet.sentence",
			"You are sending {0} on the MAIN NETWORK to {1}.", locale, amount, recipient);
	}

	/**
	 * @param locale
	 *            the language of the window
	 * @return the fee and the total
	 */
	String fee(final Locale locale)
	{
		return LethenonMessages.format("lethenon.send.confirm.mainnet.fee",
			"Fee: {0}. Together: {1}.", locale, fee, total);
	}

	/**
	 * @param locale
	 *            the language of the window
	 * @return that a transfer cannot be called back
	 */
	String irreversible(final Locale locale)
	{
		return LethenonMessages.getString("lethenon.send.confirm.mainnet.irreversible",
			"A transfer cannot be called back. Check the address character by character.", locale);
	}
}
