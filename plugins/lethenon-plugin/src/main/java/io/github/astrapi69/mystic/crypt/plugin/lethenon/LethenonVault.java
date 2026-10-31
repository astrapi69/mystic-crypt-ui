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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.plugin.api.VaultAccess;
import io.github.astrapi69.mystic.crypt.plugin.api.VaultAccessRefusedException;

/**
 * The lethenon wallets of the open vault, reached through the access the host hands this plugin
 * (#677): only entries marked {@code lethenon.wallet} are visible here. The windows ask this class,
 * and it tells them when the vault is about to lock or close; it keeps no unsealed wallet itself.
 */
final class LethenonVault
{

	private static final LethenonVault CURRENT = new LethenonVault();

	private final List<Runnable> closingListeners = new CopyOnWriteArrayList<>();

	private volatile VaultAccess access;

	/**
	 * The plugin's one view of the vault, connected by {@link LethenonVaultContribution}
	 *
	 * @return the current view
	 */
	static LethenonVault current()
	{
		return CURRENT;
	}

	/**
	 * Takes the access the host hands over
	 *
	 * @param access
	 *            the access bound to {@code lethenon.wallet}
	 */
	void connect(final VaultAccess access)
	{
		this.access = access;
	}

	/**
	 * @return whether a vault is open and unlocked
	 */
	boolean isAvailable()
	{
		VaultAccess current = access;
		return current != null && current.isAvailable();
	}

	/**
	 * The wallets of the open vault, in the vault's order
	 *
	 * @return the wallets, empty when there is no vault or no wallet in it
	 */
	List<VaultWallet> wallets()
	{
		if (!isAvailable())
		{
			return List.of();
		}
		return access.entries().stream().filter(entry -> WalletVault.isWallet(VaultWalletEntry.read(entry)))
			.map(LethenonVault::listed).toList();
	}

	/**
	 * Creates a new wallet in the vault
	 *
	 * @param network
	 *            "testnet" or "mainnet"
	 * @param password
	 *            the wallet password
	 * @return the created wallet, whose words the caller shows once and then closes
	 */
	CreatedWallet create(final String network, final char[] password)
	{
		VaultAccess target = requireAccess();
		CreatedWallet created = WalletVault.create(network, password);
		try
		{
			store(target, created.entry());
		}
		catch (final RuntimeException refused)
		{
			// the words of a wallet that never reached the vault are overwritten before the refusal
			// goes on
			created.close();
			throw refused;
		}
		return created;
	}

	/**
	 * Takes a wallet file over into the vault; the file is only read
	 *
	 * @param walletFile
	 *            the file
	 * @param password
	 *            its password
	 * @param network
	 *            "testnet" or "mainnet"
	 * @throws IOException
	 *             if the file cannot be read
	 */
	void takeOver(final Path walletFile, final char[] password, final String network)
		throws IOException
	{
		VaultAccess target = requireAccess();
		store(target, WalletVault.takeOver(walletFile, password, network));
	}

	/**
	 * Restores a wallet from its 24 words into the vault
	 *
	 * @param words
	 *            the words
	 * @param password
	 *            the new wallet password
	 * @param network
	 *            "testnet" or "mainnet"
	 */
	void restore(final char[] words, final char[] password, final String network)
	{
		VaultAccess target = requireAccess();
		store(target, WalletVault.restore(words, password, network));
	}

	/**
	 * Unseals a wallet of the vault in memory, for one action
	 *
	 * @param id
	 *            the wallet's entry identifier
	 * @param password
	 *            the wallet password
	 * @return the wallet, with the warning when its recorded address does not fit
	 */
	UnsealedWallet open(final UUID id, final char[] password)
	{
		VaultAccess source = requireAccess();
		MysticCryptEntryModelBean entry = source.entries().stream()
			.filter(candidate -> id.equals(candidate.getId())).findFirst()
			.orElseThrow(() -> new IllegalArgumentException(
				"the vault has no lethenon wallet with the identifier " + id));
		return WalletVault.unseal(VaultWalletEntry.read(entry), password);
	}

	/**
	 * Registers what a window does when the vault is about to lock or close
	 *
	 * @param listener
	 *            the window's reaction
	 */
	void whenClosing(final Runnable listener)
	{
		closingListeners.add(listener);
	}

	/** Tells every window that the vault is about to lock or close */
	void vaultClosing()
	{
		closingListeners.forEach(Runnable::run);
	}

	private VaultAccess requireAccess()
	{
		VaultAccess current = access;
		if (current == null)
		{
			throw new VaultAccessRefusedException(
				"no vault access: the host has not connected the lethenon plugin");
		}
		return current;
	}

	private static void store(final VaultAccess target, final WalletEntry wallet)
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();
		VaultWalletEntry.write(wallet, entry);
		target.add(entry);
	}

	private static VaultWallet listed(final MysticCryptEntryModelBean entry)
	{
		WalletEntry wallet = VaultWalletEntry.read(entry);
		String network = wallet.hasProperty(WalletEntry.NETWORK)
			? wallet.textOf(WalletEntry.NETWORK)
			: "";
		String title = entry.getTitle() == null ? "" : new String(entry.getTitle());
		return new VaultWallet(entry.getId(), title, network, wallet.addressForCheck());
	}
}
