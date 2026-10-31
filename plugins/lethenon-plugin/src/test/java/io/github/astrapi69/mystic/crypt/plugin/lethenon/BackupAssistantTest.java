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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.lethenon.Wallet;

/**
 * The backup assistant beyond the single check (docs/decisions/lethenon-wallet-in-vault-2026-10-30.md,
 * "Backup assistant"): the three positions are random per request and never repeat the previous
 * request, a successful check records its date in {@code lethenon.backupVerified}, and a check
 * older than 180 days brings the reminder.
 */
class BackupAssistantTest
{

	private static final Wallet WALLET = Wallet.create();

	private static final List<String> WORDS = List.of(WALLET.phrase().split(" "));

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 31);

	@Test
	@DisplayName("over 50 requests the positions are three different ones from 1 to 24, never those of the request before")
	void positionsAreRandomPerRequestAndNeverRepeatTheLastOne()
	{
		QuestionPositions positions = new QuestionPositions(new SecureRandom()::nextInt);
		int[] previous = positions.next();
		List<int[]> drawn = new ArrayList<>();
		drawn.add(previous);

		for (int request = 1; request < 50; request++)
		{
			int[] current = positions.next();

			assertEquals(3, current.length);
			assertEquals(3, Arrays.stream(current).distinct().count(), Arrays.toString(current));
			assertTrue(Arrays.stream(current).allMatch(position -> 1 <= position && position <= 24),
				Arrays.toString(current));
			assertFalse(Arrays.equals(previous, current),
				"request " + request + " asks the same positions in the same order: "
					+ Arrays.toString(current));
			drawn.add(current);
			previous = current;
		}
		assertTrue(drawn.stream().map(Arrays::toString).distinct().count() > 1,
			"50 requests must not all ask the same positions");
	}

	@Test
	@DisplayName("a generator that draws the same positions twice still asks different ones the second time")
	void aRepeatedDrawIsDrawnAgain()
	{
		int[] sequence = { 3, 10, 18, 3, 10, 18, 0, 1, 2 };
		int[] next = { 0 };
		QuestionPositions positions = new QuestionPositions(
			bound -> sequence[next[0]++ % sequence.length]);

		int[] first = positions.next();
		int[] second = positions.next();

		assertNotEquals(Arrays.toString(first), Arrays.toString(second));
	}

	@Test
	@DisplayName("a successful check records today's date, a failed one leaves the date as it was")
	void aSuccessfulCheckRecordsItsDateAndAFailedOneChangesNothing()
	{
		WalletEntry entry = WalletEntry.newEntry("testnet");
		entry.setProperty(WalletEntry.ADDRESS, WALLET.address().toText().toCharArray(), false);
		List<String> wrong = new ArrayList<>(WORDS);
		wrong.set(0, WORDS.get(0).equals("abandon") ? "zoo" : "abandon");

		assertFalse(BackupCheck.checkAndRecord(wrong, entry, TODAY));
		assertFalse(entry.hasProperty(BackupReminder.VERIFIED), "a failed check records nothing");

		assertTrue(BackupCheck.checkAndRecord(WORDS, entry, TODAY));
		assertEquals("2026-10-31", entry.textOf(BackupReminder.VERIFIED));
		assertFalse(entry.isProtected(BackupReminder.VERIFIED), "a date is no secret");

		assertFalse(BackupCheck.checkAndRecord(wrong, entry, TODAY.plusDays(3)));
		assertEquals("2026-10-31", entry.textOf(BackupReminder.VERIFIED),
			"a later failed check keeps the date of the last successful one");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("checkDates")
	@DisplayName("the reminder comes after 180 days, the notice while no check was ever made")
	void theReminderFollowsTheDateOfTheLastCheck(final String when, final String recorded,
		final BackupState expected)
	{
		WalletEntry entry = WalletEntry.newEntry("testnet");
		if (recorded != null)
		{
			entry.setProperty(BackupReminder.VERIFIED, recorded.toCharArray(), false);
		}

		assertEquals(expected, BackupReminder.stateOf(entry, TODAY));
	}

	static Stream<Arguments> checkDates()
	{
		return Stream.of(Arguments.of("never checked", null, BackupState.NOT_CHECKED),
			Arguments.of("checked today", "2026-10-31", BackupState.CURRENT),
			Arguments.of("checked 180 days ago", TODAY.minusDays(180).toString(),
				BackupState.CURRENT),
			Arguments.of("checked 181 days ago", TODAY.minusDays(181).toString(), BackupState.DUE),
			Arguments.of("a date that is no date", "last spring", BackupState.NOT_CHECKED));
	}

	@Test
	@DisplayName("a new successful check after the reminder ends it")
	void aNewCheckEndsTheReminder()
	{
		WalletEntry entry = WalletEntry.newEntry("testnet");
		entry.setProperty(WalletEntry.ADDRESS, WALLET.address().toText().toCharArray(), false);
		entry.setProperty(BackupReminder.VERIFIED, TODAY.minusDays(200).toString().toCharArray(),
			false);
		assertEquals(BackupState.DUE, BackupReminder.stateOf(entry, TODAY));

		assertTrue(BackupCheck.checkAndRecord(WORDS, entry, TODAY));

		assertEquals(BackupState.CURRENT, BackupReminder.stateOf(entry, TODAY));
	}

	@Test
	@DisplayName("the question built from drawn positions asks the words at those positions")
	void theDrawnQuestionAsksTheWordsAtItsPositions()
	{
		int[] drawn = new QuestionPositions(new SecureRandom()::nextInt).next();

		ThreeWordQuestion question = ThreeWordQuestion.ask(WORDS, drawn);

		String[] right = IntStream.of(drawn).mapToObj(position -> WORDS.get(position - 1))
			.toArray(String[]::new);
		assertTrue(question.answer(right).correct());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("misspelledWords")
	@DisplayName("a word that is not in the list is refused, named with its position")
	void anUnknownWordIsRefusedWithItsPosition(final String where, final int position,
		final String misspelled)
	{
		WalletEntry entry = WalletEntry.newEntry("testnet");
		entry.setProperty(WalletEntry.ADDRESS, WALLET.address().toText().toCharArray(), false);
		List<String> typed = new ArrayList<>(WORDS);
		typed.set(position - 1, misspelled);

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> BackupCheck.checkAndRecord(typed, entry, TODAY));

		assertTrue(refused.getMessage().contains("word " + position), refused.getMessage());
		assertTrue(refused.getMessage().contains("'" + misspelled + "'"), refused.getMessage());
		assertFalse(entry.hasProperty(BackupReminder.VERIFIED), "a refused check records nothing");
	}

	static Stream<Arguments> misspelledWords()
	{
		return Stream.of(Arguments.of("the first word", 1, "abandn"),
			Arguments.of("a word in the middle", 12, "lethenon"),
			Arguments.of("the last word", 24, "zooo"));
	}

	@Test
	@DisplayName("words typed in capitals count as the words they are")
	void wordsInCapitalsAreTheSameWords()
	{
		WalletEntry entry = WalletEntry.newEntry("testnet");
		entry.setProperty(WalletEntry.ADDRESS, WALLET.address().toText().toCharArray(), false);
		List<String> typed = WORDS.stream().map(String::toUpperCase).toList();

		assertTrue(BackupCheck.checkAndRecord(typed, entry, TODAY));
	}
}
