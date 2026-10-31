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
		List<String> words = SeedPhrase.parse(typed == null ? "" : typed);
		if (words.size() != BackupCheck.WORDS)
		{
			return Optional.of(new Refusal("lethenon.wallet.form.refused.words.count",
				"A wallet is restored from its 24 words, and you typed {0}.", words.size()));
		}
		Optional<BackupCheck.UnknownWord> unknown = BackupCheck.firstUnknownWord(words);
		if (unknown.isPresent())
		{
			return Optional.of(new Refusal("lethenon.backup.question.unknown",
				"Word {0}, \"{1}\", is not in the word list. Check your note for a typing error.",
				unknown.get().position(), unknown.get().word()));
		}
		return givesAWallet(words) ? Optional.empty()
			: Optional.of(new Refusal("lethenon.wallet.form.refused.words.checksum",
				"These 24 words do not give a wallet: a word may be in the wrong place. Compare "
					+ "them with your note, in order."));
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
