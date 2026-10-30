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
