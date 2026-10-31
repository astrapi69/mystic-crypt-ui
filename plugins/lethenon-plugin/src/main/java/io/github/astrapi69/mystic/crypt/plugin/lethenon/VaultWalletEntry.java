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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryAttachments;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;

/**
 * Moves a wallet's properties between a vault entry and the wallet tools' view of it. The host's
 * model holds property values as text, and the wallet tools keep them in buffers of their own where
 * they can (see {@link WalletEntry}). The protection flag travels with the names, so the KDBX export
 * keeps it. The sealed wallet travels as the attachment {@code wallet.lethwf}, and a wallet entry
 * gets the title people read, {@code lethenon wallet (testnet)} or {@code (mainnet)}; the plugin
 * itself reads the marker, never the title. The host's {@link VaultEntryProperties} and
 * {@link VaultEntryAttachments} do the reading and the writing.
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
		wallet.setSealed(VaultEntryAttachments.contentOf(entry, WalletVault.ATTACHMENT).orElse(null));
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
		if (wallet.sealed() != null)
		{
			VaultEntryAttachments.put(entry, WalletVault.ATTACHMENT, wallet.sealed());
		}
		if (WalletVault.isWallet(wallet))
		{
			entry.setTitle(titleOf(wallet).toCharArray());
		}
	}

	private static String titleOf(final WalletEntry wallet)
	{
		return wallet.hasProperty(WalletEntry.NETWORK)
			? "lethenon wallet (" + wallet.textOf(WalletEntry.NETWORK) + ")"
			: "lethenon wallet";
	}
}
