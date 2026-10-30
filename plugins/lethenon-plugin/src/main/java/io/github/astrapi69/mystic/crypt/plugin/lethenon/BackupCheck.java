package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import java.util.List;

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
		Wallet derived = Wallet.fromPhrase(String.join(" ", typed));
		return derived.address().toString().equals(entry.addressForCheck());
	}
}
