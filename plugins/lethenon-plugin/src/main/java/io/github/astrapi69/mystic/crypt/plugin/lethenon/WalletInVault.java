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

/**
 * The 24 words in the vault, on the user's explicit choice only. The words are stored as the
 * protected property {@code lethenon.mnemonic}; removing them overwrites the buffer that held them.
 */
final class WalletInVault
{

	/** The property that holds the words, protected so that the KDBX export keeps the flag */
	static final String MNEMONIC = "lethenon.mnemonic";

	private WalletInVault()
	{
	}

	/**
	 * Stores the words only when the user chose to. Any other choice leaves the entry as it is.
	 *
	 * @param entry
	 *            the wallet's entry
	 * @param words
	 *            the 24 words
	 * @param choice
	 *            the user's choice; the default keeps the words out
	 */
	static void store(final WalletEntry entry, final List<String> words, final WordsChoice choice)
	{
		if (choice != WordsChoice.STORE_IN_VAULT)
		{
			return;
		}
		entry.setProperty(MNEMONIC, String.join(" ", words).toCharArray(), true);
	}

	/**
	 * Removes the stored words and overwrites their buffer
	 *
	 * @param entry
	 *            the wallet's entry
	 */
	static void remove(final WalletEntry entry)
	{
		entry.removeProperty(MNEMONIC);
	}
}
