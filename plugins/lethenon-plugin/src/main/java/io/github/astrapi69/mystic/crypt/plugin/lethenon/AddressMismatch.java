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
 * The address an entry records against the address of the wallet in its attachment, when the two
 * differ (#716, texts T6 and T6a). The wallet's own address is the truth: payments to the recorded
 * one cannot be spent by this wallet. Nothing is changed without the person's confirmation.
 *
 * @param recorded
 *            the address the entry records
 * @param actual
 *            the address of the wallet in the attachment
 */
record AddressMismatch(String recorded, String actual)
{

	/**
	 * @param locale
	 *            the language
	 * @return the warning's title
	 */
	String title(final Locale locale)
	{
		return text("lethenon.address.mismatch.title",
			"The address in this entry does not belong to its wallet", locale);
	}

	/**
	 * @param locale
	 *            the language
	 * @return the sentence naming both addresses, whole
	 */
	String bothAddresses(final Locale locale)
	{
		return LethenonMessages.format("lethenon.address.mismatch.both",
			"The entry says: {0}. The wallet in the attachment has: {1}.", locale, recorded, actual);
	}

	/**
	 * @param locale
	 *            the language
	 * @return what the difference means for payments
	 */
	String consequence(final Locale locale)
	{
		return text("lethenon.address.mismatch.consequence",
			"Anyone who paid to the old address paid to an address that does not belong to this "
				+ "wallet. Find out where the difference comes from.",
			locale);
	}

	/**
	 * @param locale
	 *            the language
	 * @return the sentence that nothing was changed
	 */
	String nothingChanged(final Locale locale)
	{
		return text("lethenon.address.mismatch.nothing.changed", "Nothing was changed.", locale);
	}

	/**
	 * @param locale
	 *            the language
	 * @return the title of the confirmation before the address is replaced
	 */
	String confirmTitle(final Locale locale)
	{
		return text("lethenon.address.mismatch.confirm.title", "Replace the address in the entry?",
			locale);
	}

	/**
	 * @param locale
	 *            the language
	 * @return the confirmation, naming the old and the new address
	 */
	String confirmText(final Locale locale)
	{
		return LethenonMessages.format("lethenon.address.mismatch.confirm.text",
			"The address in the entry is replaced. Old: {0}. New, the wallet''s address: {1}. "
				+ "This wallet cannot spend payments made to the old address.",
			locale, recorded, actual);
	}

	private static String text(final String key, final String defaultValue, final Locale locale)
	{
		return LethenonMessages.getString(key, defaultValue, locale);
	}
}
