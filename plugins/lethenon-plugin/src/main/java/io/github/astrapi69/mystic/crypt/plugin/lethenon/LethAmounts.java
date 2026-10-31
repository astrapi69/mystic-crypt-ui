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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

import io.github.astrapi69.lethenon.Amount;

/**
 * An amount as a person reads it (#604, rule 4): in the language of the window, thousands grouped,
 * every one of the eight decimals kept, and the unit named - {@code 2,369,457.85346351 LETH} in
 * English, {@code 2.369.457,85346351 LETH} in German. It is money, so no decimal is ever rounded
 * away.
 */
final class LethAmounts
{

	/** The decimals of a LETH, one lethe being the smallest amount */
	static final int DECIMALS = 8;

	private LethAmounts()
	{
	}

	/**
	 * Formats an amount
	 *
	 * @param amount
	 *            the amount
	 * @param locale
	 *            the language of the window
	 * @return the amount with its unit
	 */
	static String format(final Amount amount, final Locale locale)
	{
		NumberFormat number = NumberFormat.getNumberInstance(locale);
		number.setGroupingUsed(true);
		number.setMinimumFractionDigits(DECIMALS);
		number.setMaximumFractionDigits(DECIMALS);
		return number.format(BigDecimal.valueOf(amount.lethe(), DECIMALS)) + " LETH";
	}

	/**
	 * An amount as the overview shows it in large type: two decimals, rounded half up, the exact
	 * value stays beside it (draft 01, README defect G8)
	 *
	 * @param amount
	 *            the amount
	 * @param locale
	 *            the language of the window
	 * @return e.g. "12,345.68 LETH"
	 */
	static String rounded(final Amount amount, final Locale locale)
	{
		NumberFormat number = NumberFormat.getNumberInstance(locale);
		number.setGroupingUsed(true);
		number.setMinimumFractionDigits(2);
		number.setMaximumFractionDigits(2);
		number.setRoundingMode(RoundingMode.HALF_UP);
		return number.format(BigDecimal.valueOf(amount.lethe(), DECIMALS)) + " LETH";
	}
}
