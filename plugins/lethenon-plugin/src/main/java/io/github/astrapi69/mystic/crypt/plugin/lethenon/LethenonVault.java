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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

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
	 *            the network the wallet is meant for
	 * @param password
	 *            the wallet password
	 * @return the created wallet, whose words the caller shows once and then closes
	 */
	NewWallet create(final WalletNetwork network, final char[] password)
	{
		VaultAccess target = requireAccess();
		CreatedWallet created = WalletVault.create(network.hint(), password);
		try
		{
			return new NewWallet(store(target, created.entry()), created.words());
		}
		catch (final RuntimeException refused)
		{
			// the words of a wallet that never reached the vault are overwritten before the refusal
			// goes on
			created.close();
			throw refused;
		}
	}

	/**
	 * Takes back a creation the user cancelled while the words were shown: the entry leaves the
	 * vault and the words are overwritten
	 *
	 * @param created
	 *            the wallet {@link #create} returned
	 */
	void discard(final NewWallet created)
	{
		try (created)
		{
			requireAccess().remove(created.id());
		}
	}

	/**
	 * Takes a wallet file over into the vault; the file is only read
	 *
	 * @param walletFile
	 *            the file
	 * @param password
	 *            its password
	 * @param network
	 *            the network the wallet is meant for
	 * @return the identifier of the wallet's new entry
	 * @throws IOException
	 *             if the file cannot be read
	 */
	UUID takeOver(final Path walletFile, final char[] password, final WalletNetwork network)
		throws IOException
	{
		VaultAccess target = requireAccess();
		return store(target, WalletVault.takeOver(walletFile, password, network.hint()));
	}

	/**
	 * Restores a wallet from its 24 words into the vault
	 *
	 * @param words
	 *            the words
	 * @param password
	 *            the new wallet password
	 * @param network
	 *            the network the wallet is meant for
	 * @return the identifier of the wallet's new entry
	 */
	UUID restore(final char[] words, final char[] password, final WalletNetwork network)
	{
		VaultAccess target = requireAccess();
		return store(target, WalletVault.restore(words, password, network.hint()));
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
		return WalletVault.unseal(VaultWalletEntry.read(entryOf(requireAccess(), id)), password);
	}

	/**
	 * The 24 words of a wallet again, for the backup on request: the wallet is unsealed in memory
	 * with its password and nothing is written
	 *
	 * @param id
	 *            the wallet's entry identifier
	 * @param password
	 *            the wallet password
	 * @return the words, separated by spaces; the caller overwrites them
	 */
	char[] wordsOf(final UUID id, final char[] password)
	{
		return open(id, password).wallet().phrase().toCharArray();
	}

	/**
	 * Stores the words as a protected field of the wallet's entry, only when the person chose to
	 *
	 * @param id
	 *            the wallet's entry identifier
	 * @param words
	 *            the 24 words, separated by spaces
	 * @param choice
	 *            the person's choice; keeping them out changes nothing
	 */
	void storeWords(final UUID id, final char[] words, final WordsChoice choice)
	{
		if (choice != WordsChoice.STORE_IN_VAULT)
		{
			return;
		}
		change(id, wallet -> WalletInVault.store(wallet, List.of(new String(words).split(" ")),
			choice));
	}

	/**
	 * Checks typed words against the wallet's address; a match records today in
	 * {@code lethenon.backupVerified}, a mismatch changes nothing
	 *
	 * @param id
	 *            the wallet's entry identifier
	 * @param typed
	 *            the words as typed
	 * @param today
	 *            the day of the check
	 * @return whether the words give the wallet's address
	 * @throws IllegalArgumentException
	 *             when the words are not 24 or one is not in the word list, naming it
	 */
	boolean checkBackup(final UUID id, final List<String> typed, final LocalDate today)
	{
		WalletEntry wallet = VaultWalletEntry.read(entryOf(requireAccess(), id));
		boolean matches = BackupCheck.checkAndRecord(typed, wallet, today);
		if (matches)
		{
			change(id, entry -> entry.setProperty(BackupReminder.VERIFIED,
				today.toString().toCharArray(), false));
		}
		return matches;
	}

	/**
	 * What the overview says about the wallet's backup
	 *
	 * @param id
	 *            the wallet's entry identifier
	 * @param today
	 *            the day to compare the last check with
	 * @return not checked, current or due
	 */
	BackupState backupStateOf(final UUID id, final LocalDate today)
	{
		return BackupReminder.stateOf(VaultWalletEntry.read(entryOf(requireAccess(), id)), today);
	}

	/**
	 * Writes the sealed wallet file to a place the person picked
	 *
	 * @param id
	 *            the wallet's entry identifier
	 * @param target
	 *            the file to write; an existing one is never overwritten
	 * @param consent
	 *            whether the person confirmed the warning
	 * @return the SHA-256 of the file as read back, in hexadecimal
	 * @throws IOException
	 *             if the file exists or cannot be written or read back
	 */
	String export(final UUID id, final Path target, final ExportConsent consent)
		throws IOException
	{
		return WalletVault.export(VaultWalletEntry.read(entryOf(requireAccess(), id)), target,
			consent);
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

	private static MysticCryptEntryModelBean entryOf(final VaultAccess source, final UUID id)
	{
		return source.entries().stream().filter(candidate -> id.equals(candidate.getId()))
			.findFirst().orElseThrow(() -> new IllegalArgumentException(
				"the vault has no lethenon wallet with the identifier " + id));
	}

	/** Reads the wallet's entry, lets the change work on it, and hands it back to the vault */
	private void change(final UUID id, final Consumer<WalletEntry> change)
	{
		VaultAccess target = requireAccess();
		MysticCryptEntryModelBean entry = entryOf(target, id);
		WalletEntry wallet = VaultWalletEntry.read(entry);
		change.accept(wallet);
		VaultWalletEntry.write(wallet, entry);
		target.change(entry);
	}

	private static UUID store(final VaultAccess target, final WalletEntry wallet)
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().id(UUID.randomUUID())
			.build();
		VaultWalletEntry.write(wallet, entry);
		target.add(entry);
		return entry.getId();
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
