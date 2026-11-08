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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The line that says how current the overview's chain is (#831), read with a clock of the test's
 * own: 8 November 2026, 14:05 UTC
 */
class ChainCurrencyTest
{

	private static final Instant NOW = Instant.parse("2026-11-08T14:05:00Z");

	private static final ZoneId UTC = ZoneOffset.UTC;

	/**
	 * One case
	 *
	 * @param situation
	 *            what is the case, in words
	 * @param synced
	 *            when this program last synced, empty for never
	 * @param lastBlock
	 *            how long before now the last block was mined
	 * @param locale
	 *            the language
	 * @param expected
	 *            the line
	 */
	record Case(String situation, Optional<Instant> synced, Duration lastBlock, Locale locale,
		String expected)
	{
		@Override
		public String toString()
		{
			return situation;
		}
	}

	static Stream<Arguments> cases()
	{
		Optional<Instant> today = Optional.of(Instant.parse("2026-11-08T14:02:00Z"));
		return Stream.of(
			Arguments.of(new Case("the example of the decision, in English", today,
				Duration.ofMinutes(3), Locale.ENGLISH,
				"Synced: today 14:02, last block: 3 min ago")),
			Arguments.of(new Case("the example of the decision, in German", today,
				Duration.ofMinutes(3), Locale.GERMAN,
				"Abgeglichen: heute 14:02, letzter Block: vor 3 Min.")),
			Arguments.of(new Case("never synced by this program, a block of half a minute",
				Optional.empty(), Duration.ofSeconds(30), Locale.ENGLISH,
				"Synced: not by this program, last block: just now")),
			Arguments.of(new Case("synced yesterday, a block of 59 minutes",
				Optional.of(Instant.parse("2026-11-07T23:50:00Z")), Duration.ofMinutes(59),
				Locale.ENGLISH, "Synced: yesterday 23:50, last block: 59 min ago")),
			Arguments.of(new Case("synced days ago, a block of exactly an hour",
				Optional.of(Instant.parse("2026-11-05T09:00:00Z")), Duration.ofMinutes(60),
				Locale.ENGLISH, "Synced: Nov 5, 2026 09:00, last block: 1 h ago")),
			Arguments.of(new Case("a block of exactly a day", today, Duration.ofHours(24),
				Locale.ENGLISH, "Synced: today 14:02, last block: 1 day ago")),
			Arguments.of(new Case("a block of two days, in German", today, Duration.ofDays(2),
				Locale.GERMAN, "Abgeglichen: heute 14:02, letzter Block: vor 2 Tagen")),
			Arguments.of(new Case("a block ahead of the clock", today, Duration.ofMinutes(-5),
				Locale.ENGLISH, "Synced: today 14:02, last block: just now")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cases")
	void theLine_saysWhenSyncedAndHowOldTheLastBlockIs(final Case expected)
	{
		assertEquals(expected.expected(), ChainCurrency.line(expected.synced(),
			NOW.minus(expected.lastBlock()), NOW, UTC, expected.locale()));
	}
}
