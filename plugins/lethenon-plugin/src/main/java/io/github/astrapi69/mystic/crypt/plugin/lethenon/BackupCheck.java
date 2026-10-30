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

import java.util.List;

import io.github.astrapi69.lethenon.PublishedAddress;
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
	 *             when the list does not have 24 words; the message names the count
	 */
	static boolean run(final List<String> typed, final WalletEntry entry)
	{
		if (typed.size() != WORDS)
		{
			throw new IllegalArgumentException("a backup has " + WORDS + " words, and this list has "
				+ typed.size());
		}
		Wallet derived;
		try
		{
			derived = Wallet.fromPhrase(String.join(" ", typed));
		}
		catch (IllegalArgumentException notAPhrase)
		{
			// an unknown word or a failed checksum is a backup that does not match, not an error
			return false;
		}
		// compared by value: the address's own text form is documented (lethenon#37), its toString is not
		return PublishedAddress.parse(entry.addressForCheck()).equals(derived.address());
	}
}
