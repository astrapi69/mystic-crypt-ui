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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

import io.github.astrapi69.crypt.api.algorithm.HashAlgorithm;
import io.github.astrapi69.crypt.data.hash.HashExtensions;
import io.github.astrapi69.lethenon.PublishedAddress;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;

/**
 * A lethenon wallet in the vault (docs/decisions/lethenon-wallet-in-vault-2026-10-30.md): the
 * sealed wallet is the entry's attachment {@code wallet.lethwf}, the property
 * {@code lethenon.wallet} marks the entry, and the public address is recorded beside it. Every
 * action unseals in memory and keeps no unsealed wallet afterwards, so a locked vault holds none.
 */
final class WalletVault
{

	/** The marker property, holding the wallet format version */
	static final String WALLET = "lethenon.wallet";

	/** The wallet format version this plugin writes and reads */
	static final String FORMAT_VERSION = "1";

	/** The name of the attachment that holds the sealed wallet */
	static final String ATTACHMENT = "wallet.lethwf";

	/** The warning shown before an export, which the user confirms */
	static final String EXPORT_WARNING = "The exported file lies outside the vault. It is sealed: "
		+ "the wallet password opens it, and the 24 words are not in it.";

	private static final Set<String> NETWORKS = Set.of("testnet", "mainnet");

	private WalletVault()
	{
	}

	/**
	 * Creates a new wallet, sealed into a new entry
	 *
	 * @param network
	 *            "testnet" or "mainnet"
	 * @param password
	 *            the wallet password; not stored
	 * @return the entry and the words to show once
	 */
	static CreatedWallet create(final String network, final char[] password)
	{
		requireNetwork(network);
		Wallet wallet = Wallet.create();
		return new CreatedWallet(entryFor(wallet, WalletFile.toBytes(wallet, password), network),
			wallet.phrase().toCharArray());
	}

	/**
	 * Takes over an existing wallet file: its sealed bytes become the attachment, unchanged. The
	 * file itself is only read; whether it is deleted is the user's decision
	 *
	 * @param walletFile
	 *            the wallet file
	 * @param password
	 *            the password the file is sealed with
	 * @param network
	 *            "testnet" or "mainnet"
	 * @return the new entry
	 * @throws IOException
	 *             if the file cannot be read
	 */
	static WalletEntry takeOver(final Path walletFile, final char[] password, final String network)
		throws IOException
	{
		requireNetwork(network);
		byte[] sealed = Files.readAllBytes(walletFile);
		return entryFor(WalletFile.read(sealed, password), sealed, network);
	}

	/**
	 * Restores a wallet from its 24 words, sealed straight into a new entry
	 *
	 * @param words
	 *            the words, separated by spaces
	 * @param password
	 *            the new wallet password
	 * @param network
	 *            "testnet" or "mainnet"
	 * @return the new entry
	 */
	static WalletEntry restore(final char[] words, final char[] password, final String network)
	{
		requireNetwork(network);
		Wallet wallet = Wallet.fromPhrase(new String(words));
		return entryFor(wallet, WalletFile.toBytes(wallet, password), network);
	}

	/**
	 * Writes the sealed wallet to a file, only after the user confirmed the warning, and never over
	 * a file that is already there
	 *
	 * @param entry
	 *            the wallet's entry
	 * @param target
	 *            the file to write
	 * @param consent
	 *            whether the user confirmed {@link #EXPORT_WARNING}
	 * @return the SHA-256 of the file as written, read back, in hexadecimal
	 * @throws IOException
	 *             if the file exists or cannot be written or read back
	 */
	static String export(final WalletEntry entry, final Path target, final ExportConsent consent)
		throws IOException
	{
		if (consent != ExportConsent.GIVEN_AFTER_WARNING)
		{
			throw new IllegalStateException("nothing was exported to " + target
				+ ": the export was not confirmed after the warning. " + EXPORT_WARNING);
		}
		requireWallet(entry);
		Files.write(target, entry.sealed(), StandardOpenOption.CREATE_NEW);
		return sha256(Files.readAllBytes(target));
	}

	/**
	 * Unseals the wallet in memory and compares its address with the one the entry records
	 *
	 * @param entry
	 *            the wallet's entry; not changed
	 * @param password
	 *            the wallet password
	 * @return the wallet, with a warning when the addresses differ
	 */
	static UnsealedWallet unseal(final WalletEntry entry, final char[] password)
	{
		requireWallet(entry);
		Wallet wallet = WalletFile.read(entry.sealed(), password);
		Optional<String> warning = addressWarning(entry.addressForCheck(), wallet.address());
		Optional<String> accountWarning = accountWarning(entry, wallet);
		Optional<AddressMismatch> mismatch = warning
			.map(differs -> new AddressMismatch(entry.addressForCheck(), wallet.address().toText()));
		return new UnsealedWallet(wallet, warning.isPresent() && accountWarning.isPresent()
			? Optional.of(warning.get() + " " + accountWarning.get())
			: warning.or(() -> accountWarning), mismatch);
	}

	/**
	 * The public key of a wallet's ML-DSA-65 account, as the entry records it
	 *
	 * @param wallet
	 *            the wallet
	 * @return the key in lethenon's text form
	 */
	static String mlDsaAccountOf(final Wallet wallet)
	{
		return wallet.spendKey(SignatureSuite.ML_DSA_65).toString();
	}

	/**
	 * Compares the recorded ML-DSA-65 account key with the wallet's, like the address: a missing
	 * key is no warning, an older entry simply has none yet (#700)
	 */
	private static Optional<String> accountWarning(final WalletEntry entry, final Wallet wallet)
	{
		if (!entry.hasProperty(WalletEntry.ACCOUNT_ML_DSA))
		{
			return Optional.empty();
		}
		return entry.textOf(WalletEntry.ACCOUNT_ML_DSA).equals(mlDsaAccountOf(wallet))
			? Optional.empty()
			: Optional.of("The ML-DSA-65 account key recorded on this entry is not the key of the "
				+ "wallet in its attachment. Neither was changed.");
	}

	/**
	 * Whether an entry is a wallet of this format: the marker and the attachment are both there
	 *
	 * @param entry
	 *            the entry
	 * @return true for a wallet entry
	 */
	static boolean isWallet(final WalletEntry entry)
	{
		return entry.hasProperty(WALLET) && FORMAT_VERSION.equals(entry.textOf(WALLET))
			&& entry.sealed() != null;
	}

	private static WalletEntry entryFor(final Wallet wallet, final byte[] sealed,
		final String network)
	{
		WalletEntry entry = WalletEntry.newEntry(network);
		entry.setProperty(WALLET, FORMAT_VERSION.toCharArray(), false);
		entry.setProperty(WalletEntry.ADDRESS, wallet.address().toText().toCharArray(), false);
		entry.setProperty(WalletEntry.ACCOUNT_ML_DSA, mlDsaAccountOf(wallet).toCharArray(), false);
		entry.setSealed(sealed);
		return entry;
	}

	private static Optional<String> addressWarning(final String recorded,
		final PublishedAddress actual)
	{
		if (recorded.isEmpty() || isTheAddressOf(recorded, actual))
		{
			return Optional.empty();
		}
		return Optional.of(mismatch(recorded, actual));
	}

	private static boolean isTheAddressOf(final String recorded, final PublishedAddress actual)
	{
		try
		{
			return PublishedAddress.parse(recorded).equals(actual);
		}
		catch (final IllegalArgumentException notAnAddress)
		{
			// text that is no address at all cannot be this wallet's address: a mismatch, shown
			return false;
		}
	}

	private static String mismatch(final String recorded, final PublishedAddress actual)
	{
		return "The address recorded on this entry, " + recorded
			+ ", is not the address of the wallet in its attachment, " + actual.toText()
			+ ". Neither was changed.";
	}

	private static void requireNetwork(final String network)
	{
		if (!NETWORKS.contains(network))
		{
			throw new IllegalArgumentException(
				"the network is testnet or mainnet, and this is '" + network + "'");
		}
	}

	private static void requireWallet(final WalletEntry entry)
	{
		if (!isWallet(entry))
		{
			throw new IllegalArgumentException("this entry is not a lethenon wallet: it needs the "
				+ "property " + WALLET + " = " + FORMAT_VERSION + " and the attachment "
				+ ATTACHMENT);
		}
	}

	private static String sha256(final byte[] bytes)
	{
		try
		{
			return HexFormat.of().formatHex(HashExtensions.hash(bytes, HashAlgorithm.SHA_256));
		}
		catch (final NoSuchAlgorithmException brokenInstallation)
		{
			throw new IllegalStateException("this Java installation has no SHA-256",
				brokenInstallation);
		}
	}
}
