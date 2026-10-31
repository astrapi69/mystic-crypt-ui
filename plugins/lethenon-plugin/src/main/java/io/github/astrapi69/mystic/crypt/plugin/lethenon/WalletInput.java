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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import io.github.astrapi69.lethenon.SeedPhrase;
import io.github.astrapi69.lethenon.Wallet;

/**
 * What the wallet window checks before it touches the vault (#680): the wallet password typed
 * twice, the 24 words of a restore, the wallet file of a take-over. Each refusal says what to do,
 * in the person's language; the words are checked by lethenon, never by this class.
 */
final class WalletInput
{

	private WalletInput()
	{
	}

	/**
	 * Checks a new wallet password and its repetition
	 *
	 * @param password
	 *            the password, may be null
	 * @param repeated
	 *            the same password once more, may be null
	 * @return the refusal, or empty when both are the same and not empty
	 */
	static Optional<Refusal> passwords(final char[] password, final char[] repeated)
	{
		if (password == null || password.length == 0)
		{
			return Optional.of(new Refusal("lethenon.wallet.form.refused.password.empty",
				"Type a wallet password: it seals the wallet in the vault."));
		}
		if (!Arrays.equals(password, repeated))
		{
			return Optional.of(new Refusal("lethenon.wallet.form.refused.password.different",
				"The two passwords are not the same. Type them again."));
		}
		return Optional.empty();
	}

	/**
	 * Checks the words of a restore: 24 of them, each in the word list, together a wallet
	 *
	 * @param typed
	 *            the words as typed, separated by any spaces or line breaks
	 * @return the refusal, or empty when the words give a wallet
	 */
	static Optional<Refusal> words(final String typed)
	{
		return wordsToCheck(typed).or(() -> givesAWallet(SeedPhrase.parse(typed))
			? Optional.empty()
			: Optional.of(new Refusal("lethenon.wallet.form.refused.words.checksum",
				"These 24 words do not give a wallet: a word may be in the wrong place. Compare "
					+ "them with your note, in order.")));
	}

	/**
	 * Checks the words of a backup check before they are compared with the wallet: 24 of them,
	 * each in the word list. A failed checksum is not refused here: for a check it is the answer
	 * "does not match", not a typing error (decision record, backup assistant 3)
	 *
	 * @param typed
	 *            the words as typed, separated by any spaces or line breaks
	 * @return the refusal, or empty when there are 24 known words
	 */
	static Optional<Refusal> wordsToCheck(final String typed)
	{
		List<String> words = SeedPhrase.parse(typed == null ? "" : typed);
		if (words.size() != BackupCheck.WORDS)
		{
			return Optional.of(new Refusal("lethenon.wallet.form.refused.words.count",
				"A wallet is restored from its 24 words, and you typed {0}.", words.size()));
		}
		return BackupCheck.firstUnknownWord(words)
			.map(unknown -> unknownWord(unknown.position(), unknown.word()));
	}

	/**
	 * Checks the answers of step 2 of the backup: the words at the asked positions, compared as
	 * lethenon reads a phrase, lower case and without the spaces around them (T2)
	 *
	 * @param words
	 *            the wallet's 24 words
	 * @param positions
	 *            the asked positions, counted from 1, in the order of the answers
	 * @param answers
	 *            what was typed, one answer per position
	 * @return the refusal for the first answer that is not a word of the list or not the right
	 *         word, or empty when all three are right
	 */
	static Optional<Refusal> answers(final List<String> words, final int[] positions,
		final String[] answers)
	{
		for (int index = 0; index < positions.length; index++)
		{
			int position = positions[index];
			String answer = normalised(answers[index]);
			if (!answer.isEmpty() && !SeedPhrase.isKnownWord(answer))
			{
				return Optional.of(unknownWord(position, answer));
			}
			if (!words.get(position - 1).equals(answer))
			{
				return Optional.of(new Refusal("lethenon.backup.question.wrong",
					"Word {0} is not right. Look at your note and try again.", position));
			}
		}
		return Optional.empty();
	}

	/** A word that is not in the word list, named with its position (T2) */
	private static Refusal unknownWord(final int position, final String word)
	{
		return new Refusal("lethenon.backup.question.unknown",
			"Word {0}, \"{1}\", is not in the word list. Check your note for a typing error.",
			position, word);
	}

	private static String normalised(final String answer)
	{
		return answer == null ? "" : answer.strip().toLowerCase(Locale.ROOT);
	}

	/**
	 * Checks that a wallet file was named
	 *
	 * @param walletFile
	 *            the path as typed or chosen
	 * @return the refusal, or empty when a file is named
	 */
	static Optional<Refusal> walletFile(final String walletFile)
	{
		return walletFile == null || walletFile.isBlank()
			? Optional.of(new Refusal("lethenon.wallet.form.refused.file",
				"Pick the wallet file first."))
			: Optional.empty();
	}

	private static boolean givesAWallet(final List<String> words)
	{
		try
		{
			Wallet.fromPhrase(String.join(" ", words));
			return true;
		}
		catch (final IllegalArgumentException failedChecksum)
		{
			// every word is known here, so the phrase's own checksum refused it: a word out of place
			return false;
		}
	}
}
