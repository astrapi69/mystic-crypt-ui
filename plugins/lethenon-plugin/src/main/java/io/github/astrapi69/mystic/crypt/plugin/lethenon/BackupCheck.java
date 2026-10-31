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

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import io.github.astrapi69.lethenon.PublishedAddress;
import io.github.astrapi69.lethenon.SeedPhrase;
import io.github.astrapi69.lethenon.Wallet;

/**
 * Checks a backup: derives the wallet's address from the typed 24 words and compares it with the
 * address of the wallet in the vault. Writes no file.
 */
final class BackupCheck
{

	/** The number of words a wallet phrase has */
	static final int WORDS = 24;

	private BackupCheck()
	{
	}

	/**
	 * Runs the check
	 *
	 * @param typed
	 *            the words the user typed, in order
	 * @param entry
	 *            the wallet's entry
	 * @return whether the typed words give the wallet in the vault
	 * @throws IllegalArgumentException
	 *             when the list does not have 24 words, naming the count, or holds a word that is
	 *             not in the BIP-39 list, naming the word and its position: a typing error, not a
	 *             backup that does not match
	 */
	static boolean run(final List<String> typed, final WalletEntry entry)
	{
		List<String> words = wordsOf(typed);
		if (words.size() != WORDS)
		{
			throw new IllegalArgumentException("a backup has " + WORDS + " words, and this list has "
				+ words.size());
		}
		requireKnownWords(words);
		Wallet derived;
		try
		{
			derived = Wallet.fromPhrase(String.join(" ", words));
		}
		catch (IllegalArgumentException notAPhrase)
		{
			// every word is known here: a failed checksum is a backup that does not match, not an error
			return false;
		}
		// compared by value: the address's own text form is documented (lethenon#37), its toString is not
		return PublishedAddress.parse(entry.addressForCheck()).equals(derived.address());
	}

	/**
	 * Runs the check and, when it matches, records the day in {@code lethenon.backupVerified}; a
	 * check that does not match leaves the recorded day as it was
	 *
	 * @param typed
	 *            the words the user typed, in order
	 * @param entry
	 *            the wallet's entry, changed only by a successful check
	 * @param today
	 *            the day of the check
	 * @return whether the typed words give the wallet in the vault
	 */
	static boolean checkAndRecord(final List<String> typed, final WalletEntry entry,
		final LocalDate today)
	{
		boolean matches = run(typed, entry);
		if (matches)
		{
			entry.setProperty(BackupReminder.VERIFIED, today.toString().toCharArray(), false);
		}
		return matches;
	}

	/**
	 * The words as lethenon reads a phrase: lower case, split at any space, empty and missing fields
	 * dropped. Only these go to {@link SeedPhrase#isKnownWord}, which does not take null
	 */
	private static List<String> wordsOf(final List<String> typed)
	{
		return SeedPhrase.parse(typed.stream().map(field -> field == null ? "" : field)
			.collect(Collectors.joining(" ")));
	}

	private static void requireKnownWords(final List<String> typed)
	{
		for (int position = 1; position <= typed.size(); position++)
		{
			String word = typed.get(position - 1);
			if (!SeedPhrase.isKnownWord(word))
			{
				throw new IllegalArgumentException("word " + position + ", '" + word
					+ "', is not a word of the BIP-39 list; check it in your note");
			}
		}
	}
}
