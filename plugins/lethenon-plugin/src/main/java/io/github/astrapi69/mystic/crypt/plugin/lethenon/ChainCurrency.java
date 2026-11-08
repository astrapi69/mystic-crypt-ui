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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;

/**
 * How current the overview's chain is, in one line (#831, draft 01): when this program last synced
 * it, and how long ago its last block was mined, e.g. "Synced: today 14:02, last block: 3 min ago".
 * Both are said because they answer different questions: the first what this program did, the
 * second how old what the balance rests on is, whoever brought it here.
 */
final class ChainCurrency
{

	private ChainCurrency()
	{
	}

	/**
	 * The line
	 *
	 * @param synced
	 *            when this program last synced the chain file, empty when it never did
	 * @param lastBlock
	 *            when the chain's last block was mined
	 * @param now
	 *            the time the line is for
	 * @param zone
	 *            the zone the clock times are given in
	 * @param locale
	 *            the language of the window
	 * @return the line
	 */
	static String line(final Optional<Instant> synced, final Instant lastBlock, final Instant now,
		final ZoneId zone, final Locale locale)
	{
		String syncedPart = synced
			.map(at -> LethenonMessages.format("lethenon.wallet.chain.synced", "Synced: {0}",
				locale, when(at, now, zone, locale)))
			.orElseGet(() -> LethenonMessages.getString("lethenon.wallet.chain.synced.never",
				"Synced: not by this program", locale));
		return LethenonMessages.format("lethenon.wallet.chain.state", "{0}, last block: {1}",
			locale, syncedPart, age(lastBlock, now, locale));
	}

	/**
	 * A time as a person says it: today or yesterday with the clock time, otherwise the date
	 */
	static String when(final Instant at, final Instant now, final ZoneId zone, final Locale locale)
	{
		LocalDateTime time = LocalDateTime.ofInstant(at, zone);
		LocalDate today = LocalDate.ofInstant(now, zone);
		String clock = time.format(DateTimeFormatter.ofPattern("HH:mm", locale));
		if (time.toLocalDate().equals(today))
		{
			return LethenonMessages.format("lethenon.wallet.chain.today", "today {0}", locale,
				clock);
		}
		if (time.toLocalDate().equals(today.minusDays(1)))
		{
			return LethenonMessages.format("lethenon.wallet.chain.yesterday", "yesterday {0}",
				locale, clock);
		}
		return time.toLocalDate()
			.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) + " "
			+ clock;
	}

	/**
	 * How long ago something was: just now, minutes, hours, then days; a time ahead of the clock,
	 * from a miner's clock running fast, is just now
	 */
	static String age(final Instant then, final Instant now, final Locale locale)
	{
		Duration age = Duration.between(then, now);
		if (age.toMinutes() < 1)
		{
			return LethenonMessages.getString("lethenon.wallet.chain.age.now", "just now",
				locale);
		}
		if (age.toMinutes() < 60)
		{
			return LethenonMessages.format("lethenon.wallet.chain.age.minutes",
				"{0,number,integer} min ago", locale, age.toMinutes());
		}
		if (age.toHours() < 24)
		{
			return LethenonMessages.format("lethenon.wallet.chain.age.hours",
				"{0,number,integer} h ago", locale, age.toHours());
		}
		return LethenonMessages.format("lethenon.wallet.chain.age.days",
			"{0,choice,1#1 day|1<{0,number,integer} days} ago", locale, age.toDays());
	}
}
