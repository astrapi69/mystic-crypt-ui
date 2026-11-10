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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.lethenon.Wallet;

/**
 * What the wallet window refuses before it touches the vault (#680): two passwords that differ, an
 * empty one, a list of words that is not 24 words, a word that is not in the list - named with its
 * position - and 24 known words that give no wallet. Each refusal is a sentence the window shows in
 * the person's language.
 */
class WalletInputTest
{

	private static final Locale ENGLISH = Locale.ENGLISH;

	@Test
	@DisplayName("the same password twice is accepted")
	void theSamePasswordTwice_isAccepted()
	{
		char[] password = ThrowawayPasswords.next();

		assertEquals(Optional.empty(), WalletInput.passwords(password, password.clone()));
	}

	static Stream<Arguments> refusedPasswords()
	{
		char[] one = ThrowawayPasswords.next();
		char[] other = ThrowawayPasswords.next();
		return Stream.of(
			Arguments.of("two different passwords", one, other,
				"The two passwords are not the same. Type them again."),
			Arguments.of("an empty password", new char[0], new char[0],
				"Type a wallet password: it seals the wallet in the vault."),
			Arguments.of("no password at all", null, null,
				"Type a wallet password: it seals the wallet in the vault."),
			Arguments.of("the first a prefix of the second", Arrays.copyOf(one, one.length - 1), one,
				"The two passwords are not the same. Type them again."));
	}

	@ParameterizedTest(name = "{0} is refused")
	@MethodSource("refusedPasswords")
	@DisplayName("passwords that cannot seal a wallet are refused with a sentence")
	void refusedPasswords_areNamed(final String caseName, final char[] password,
		final char[] repeated, final String expected)
	{
		assertEquals(expected, WalletInput.passwords(password, repeated).orElseThrow().text(ENGLISH));
	}

	@Test
	@DisplayName("the 24 words of a wallet are accepted, whatever the spaces and the case")
	void theWordsOfAWallet_areAccepted()
	{
		String phrase = Wallet.create().phrase();

		assertEquals(Optional.empty(), WalletInput.words(phrase));
		assertEquals(Optional.empty(),
			WalletInput.words("  " + phrase.toUpperCase(Locale.ROOT).replace(" ", "\n  ") + " "));
	}

	static Stream<Arguments> refusedWords()
	{
		List<String> words = new ArrayList<>(List.of(Wallet.create().phrase().split(" ")));
		List<String> typo = new ArrayList<>(words);
		typo.set(6, "abandonn");
		List<String> swapped = swappedUntilTheChecksumFails(words);
		return Stream.of(
			Arguments.of("nothing", "", "A wallet is restored from its 24 words, and you typed 0."),
			Arguments.of("23 words", String.join(" ", words.subList(0, 23)),
				"A wallet is restored from its 24 words, and you typed 23."),
			Arguments.of("25 words", String.join(" ", words) + " abandon",
				"A wallet is restored from its 24 words, and you typed 25."),
			Arguments.of("a typing error in word 7", String.join(" ", typo),
				"Word 7, \"abandonn\", is not in the word list. Check your note for a typing error."),
			Arguments.of("two words swapped", String.join(" ", swapped),
				"These 24 words do not give a wallet: a word may be in the wrong place. Compare them "
					+ "with your note, in order."));
	}

	/**
	 * Words of the list in an order the checksum refuses are refused with a sentence that says what
	 * to look for, in both languages of the window (#804, draft 05)
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("theChecksumSentence")
	void aFailedChecksum_saysWhatToLookFor(final Locale locale, final String expected)
	{
		List<String> swapped = swappedUntilTheChecksumFails(
			List.of(Wallet.create().phrase().split(" ")));

		assertEquals(expected, WalletInput.words(String.join(" ", swapped)).orElseThrow().text(locale));
	}

	static Stream<Arguments> theChecksumSentence()
	{
		return Stream.of(Arguments.of(Locale.ENGLISH,
			"These 24 words do not give a wallet: a word may be in the wrong place. Compare them "
				+ "with your note, in order."),
			Arguments.of(Locale.GERMAN,
				"Diese 24 Wörter ergeben keine Wallet: vielleicht steht ein Wort an der falschen "
					+ "Stelle. Vergleiche sie der Reihe nach mit deiner Notiz."));
	}

	@ParameterizedTest(name = "{0} is refused")
	@MethodSource("refusedWords")
	@DisplayName("words that give no wallet are refused with a sentence that says what to check")
	void refusedWords_areNamed(final String caseName, final String typed, final String expected)
	{
		assertEquals(expected, WalletInput.words(typed).orElseThrow().text(ENGLISH));
	}

	@Test
	@DisplayName("the German sentence names the same word and position")
	void theGermanSentence_namesTheWordAndItsPosition()
	{
		List<String> words = new ArrayList<>(List.of(Wallet.create().phrase().split(" ")));
		words.set(0, "zzz");

		assertEquals("Wort 1, \"zzz\", gibt es in der Wortliste nicht. Sieh in deiner Notiz nach, "
			+ "vielleicht hast du dich vertippt.",
			WalletInput.words(String.join(" ", words)).orElseThrow().text(Locale.GERMAN));
	}

	@Test
	@DisplayName("a wallet file is named before it is taken over")
	void aWalletFile_isNamed()
	{
		assertEquals("Pick the wallet file first.",
			WalletInput.walletFile(" ").orElseThrow().text(ENGLISH));
		assertTrue(WalletInput.walletFile("/home/me/my.lethwf").isEmpty());
	}

	@Test
	@DisplayName("the three words from the note are accepted, whatever the case and the spaces around them")
	void theThreeRightWords_areAccepted()
	{
		List<String> words = List.of(Wallet.create().phrase().split(" "));
		int[] positions = { 4, 11, 19 };

		assertEquals(Optional.empty(), WalletInput.answers(words, positions,
			new String[] { " " + words.get(3).toUpperCase(Locale.ROOT), words.get(10),
				words.get(18) + " " }));
	}

	static Stream<Arguments> refusedAnswers()
	{
		List<String> words = new ArrayList<>(List.of(Wallet.create().phrase().split(" ")));
		String wrongButKnown = words.get(10).equals("abandon") ? "ability" : "abandon";
		return Stream.of(
			Arguments.of("a known word at the wrong position", words,
				new String[] { words.get(3), wrongButKnown, words.get(18) },
				"Word 11 is not right. Look at your note and try again."),
			Arguments.of("a word that is not in the list", words,
				new String[] { "qqq", words.get(10), words.get(18) },
				"Word 4, \"qqq\", is not in the word list. Check your note for a typing error."),
			Arguments.of("an empty answer", words, new String[] { words.get(3), "", words.get(18) },
				"Word 11 is not right. Look at your note and try again."));
	}

	@ParameterizedTest(name = "{0} is refused")
	@MethodSource("refusedAnswers")
	@DisplayName("a wrong answer names its position, an unknown word names itself and its position (T2)")
	void wrongAnswers_areNamed(final String caseName, final List<String> words,
		final String[] answers, final String expected)
	{
		assertEquals(expected,
			WalletInput.answers(words, new int[] { 4, 11, 19 }, answers).orElseThrow().text(ENGLISH));
	}

	static Stream<Arguments> numberedPhrases()
	{
		List<String> words = List.of(Wallet.create().phrase().split(" "));
		return Stream.of(
			Arguments.of("numbered with a dot, as the backup sheet shows them",
				numbered(words, "%d. ")),
			Arguments.of("numbered with a bracket", numbered(words, "%d) ")),
			Arguments.of("numbered bare and zero-padded", numbered(words, "%02d ")),
			Arguments.of("numbered with a colon", numbered(words, "%d: ")));
	}

	/**
	 * A phrase pasted as people paste it from the backup sheet is accepted (#802): lethenon 0.8.1's
	 * {@code SeedPhrase.parse} drops a number in front of every word (lethenon#286). With 0.8.0 the
	 * numbers counted as words of their own, and the restore refused the 24 words as 48
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("numberedPhrases")
	@DisplayName("a phrase pasted with the numbers of a backup sheet is accepted (#802)")
	void aNumberedPhrase_isAccepted(final String caseName, final String typed)
	{
		assertEquals(Optional.empty(), WalletInput.words(typed));
	}

	/** Commas separate words since lethenon 0.8.1 (#802); 0.8.0 left them on the words */
	@Test
	@DisplayName("a phrase pasted with commas is accepted (#802)")
	void aCommaSeparatedPhrase_isAccepted()
	{
		List<String> words = List.of(Wallet.create().phrase().split(" "));

		assertEquals(Optional.empty(), WalletInput.words(String.join(", ", words)));
	}

	/**
	 * Non-breaking spaces separate words since lethenon 0.8.1 (#802); for 0.8.0 they were no
	 * spaces, and the whole phrase counted as one word
	 */
	@Test
	@DisplayName("a phrase pasted with non-breaking spaces is accepted (#802)")
	void aNonBreakingSpacePhrase_isAccepted()
	{
		String phrase = Wallet.create().phrase();

		assertEquals(Optional.empty(), WalletInput.words(phrase.replace(' ', ' ')));
	}

	/**
	 * An answer of the backup check pasted with its number in front, as a note copied from the
	 * backup sheet carries one, is the word it names (#802); 0.8.0 refused it as unknown
	 */
	@Test
	@DisplayName("an answer pasted with its number is the right word (#802)")
	void anAnswerPastedWithItsNumber_isAccepted()
	{
		List<String> words = List.of(Wallet.create().phrase().split(" "));

		assertEquals(Optional.empty(),
			WalletInput.answers(words, new int[] { 7 }, new String[] { "7. " + words.get(6) }));
	}

	/** The words with a number in front of each, as the backup sheet shows them */
	private static String numbered(final List<String> words, final String format)
	{
		StringBuilder phrase = new StringBuilder();
		for (int position = 1; position <= words.size(); position++)
		{
			phrase.append(String.format(Locale.ROOT, format, position))
				.append(words.get(position - 1)).append(' ');
		}
		return phrase.toString().strip();
	}

	/**
	 * Swaps neighbouring words until the phrase's checksum fails; a swap can keep it valid by
	 * chance, one in 256, so the test does not depend on the first one
	 */
	private static List<String> swappedUntilTheChecksumFails(final List<String> words)
	{
		for (int first = 0; first < words.size() - 1; first++)
		{
			List<String> swapped = new ArrayList<>(words);
			swapped.set(first, words.get(first + 1));
			swapped.set(first + 1, words.get(first));
			if (!swapped.equals(words) && !givesAWallet(swapped))
			{
				return swapped;
			}
		}
		throw new IllegalStateException("no swap broke the checksum of " + words.size() + " words");
	}

	private static boolean givesAWallet(final List<String> words)
	{
		try
		{
			Wallet.fromPhrase(String.join(" ", words));
			return true;
		}
		catch (IllegalArgumentException notAPhrase)
		{
			return false;
		}
	}
}
