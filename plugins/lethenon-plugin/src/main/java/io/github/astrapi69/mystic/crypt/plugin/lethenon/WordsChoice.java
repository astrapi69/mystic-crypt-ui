package io.github.astrapi69.mystic.crypt.plugin.lethenon;

/**
 * Whether the 24 words of a wallet are stored in the vault as well. The default is to keep them out;
 * storing them is the user's explicit choice (docs/decisions/lethenon-wallet-in-vault-2026-10-30.md).
 */
enum WordsChoice
{
	/** The words are not stored; the user keeps them on paper or offline. The default. */
	KEEP_OUT,

	/** The user chose to store the words in the vault, with the warning shown beforehand. */
	STORE_IN_VAULT
}
