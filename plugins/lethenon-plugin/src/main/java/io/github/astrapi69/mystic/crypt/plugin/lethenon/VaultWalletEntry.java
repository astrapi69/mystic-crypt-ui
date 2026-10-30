package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;

/**
 * Moves a wallet's properties between a vault entry and the wallet tools' view of it. The host's
 * model holds property values as text, and the wallet tools keep them in buffers of their own where
 * they can (see {@link WalletEntry}). The protection flag travels with the names, so the KDBX export
 * keeps it. The host's {@link VaultEntryProperties} does the reading and the writing.
 */
final class VaultWalletEntry
{

	private VaultWalletEntry()
	{
	}

	/**
	 * The wallet view of a vault entry
	 *
	 * @param entry
	 *            the vault's entry
	 * @return the wallet's properties, each with its protection flag
	 */
	static WalletEntry read(final MysticCryptEntryModelBean entry)
	{
		Map<String, String> values = VaultEntryProperties.valuesOf(entry);
		Set<String> protectedNames = VaultEntryProperties.protectedNamesOf(entry);
		WalletEntry wallet = WalletEntry.empty();
		for (Map.Entry<String, String> value : values.entrySet())
		{
			wallet.setProperty(value.getKey(), value.getValue().toCharArray(),
				protectedNames.contains(value.getKey()));
		}
		return wallet;
	}

	/**
	 * Writes the wallet's properties into a vault entry, replacing the entry's custom properties
	 *
	 * @param wallet
	 *            the wallet's properties
	 * @param entry
	 *            the vault's entry, changed in place
	 */
	static void write(final WalletEntry wallet, final MysticCryptEntryModelBean entry)
	{
		Map<String, String> values = new LinkedHashMap<>();
		Set<String> protectedNames = new LinkedHashSet<>();
		for (String name : wallet.propertyNames())
		{
			values.put(name, wallet.textOf(name));
			if (wallet.isProtected(name))
			{
				protectedNames.add(name);
			}
		}
		VaultEntryProperties.replace(entry, values, protectedNames);
	}
}
