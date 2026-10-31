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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;
import io.github.astrapi69.mystic.crypt.plugin.api.VaultAccessRefusedException;

/**
 * The wallets of the open vault, as the overview sees them (#680): the empty state, the three ways
 * to get a wallet into the vault, opening one, and what happens when the vault locks.
 */
class LethenonVaultTest
{

	private static final char[] PASSWORD = ThrowawayPasswords.next();

	@TempDir
	Path directory;

	private InMemoryVaultAccess access;

	private LethenonVault vault;

	@BeforeEach
	void aConnectedVault()
	{
		access = new InMemoryVaultAccess();
		vault = new LethenonVault();
		vault.connect(access);
	}

	@Test
	@DisplayName("a vault without a wallet shows none, which is the overview's empty state")
	void anEmptyVaultHasNoWallets()
	{
		assertTrue(vault.isAvailable());
		assertEquals(List.of(), vault.wallets());
	}

	@Test
	@DisplayName("a created wallet is stored in the vault and listed with its network and address; its words are not stored")
	void aCreatedWalletIsListed()
	{
		try (CreatedWallet created = vault.create("testnet", PASSWORD))
		{
			List<VaultWallet> wallets = vault.wallets();

			assertEquals(1, wallets.size());
			assertEquals("testnet", wallets.get(0).network());
			assertEquals("lethenon wallet (testnet)", wallets.get(0).title());
			assertEquals(created.entry().addressForCheck(), wallets.get(0).address());
			String words = new String(created.words());
			assertTrue(access.stored.stream().noneMatch(entry -> VaultEntryProperties
				.valuesOf(entry).values().stream().anyMatch(words::contains)),
				"no property of the stored entry holds the words");
		}
	}

	@Test
	@DisplayName("a taken-over wallet file and a wallet restored from its words are listed, the file stays")
	void takenOverAndRestoredWalletsAreListed() throws IOException
	{
		Wallet existing = Wallet.create();
		Path file = directory.resolve("my.lethwf");
		WalletFile.write(file, existing, PASSWORD);
		Wallet lost = Wallet.create();

		vault.takeOver(file, PASSWORD, "testnet");
		vault.restore(lost.phrase().toCharArray(), ThrowawayPasswords.next(), "mainnet");

		List<String> addresses = vault.wallets().stream().map(VaultWallet::address).toList();
		assertEquals(List.of(existing.address().toText(), lost.address().toText()), addresses);
		assertTrue(Files.exists(file), "the taken-over file is the user's to delete");
	}

	@Test
	@DisplayName("an opened wallet unseals with its password and reports a recorded address that does not fit")
	void opensAWalletWithItsPassword()
	{
		try (CreatedWallet created = vault.create("testnet", PASSWORD))
		{
			UUID id = vault.wallets().get(0).id();

			UnsealedWallet opened = vault.open(id, PASSWORD);

			assertEquals(created.entry().addressForCheck(), opened.wallet().address().toText());
			assertTrue(opened.addressWarning().isEmpty());
			assertThrows(SecurityException.class, () -> vault.open(id, ThrowawayPasswords.next()));
		}
	}

	@Test
	@DisplayName("an entry with the marker but without the sealed wallet is not listed as a wallet")
	void aMarkedEntryWithoutAttachmentIsNotAWallet()
	{
		MysticCryptEntryModelBean broken = MysticCryptEntryModelBean.builder().id(UUID.randomUUID())
			.build();
		VaultEntryProperties.replace(broken, Map.of(WalletVault.WALLET, "1"), Set.of());
		access.stored.add(broken);

		assertEquals(List.of(), vault.wallets());
	}

	@Test
	@DisplayName("when the vault locks the windows are told once, and the vault then lists nothing")
	void lockingTellsTheWindowsAndEmptiesTheList()
	{
		vault.create("testnet", PASSWORD).close();
		int[] told = { 0 };
		vault.whenClosing(() -> told[0]++);

		access.available = false;
		vault.vaultClosing();

		assertEquals(1, told[0]);
		assertFalse(vault.isAvailable());
		assertEquals(List.of(), vault.wallets());
	}

	@Test
	@DisplayName("before the host connects the plugin there is no vault, and creating is refused with the reason")
	void withoutAConnectionThereIsNoVault()
	{
		LethenonVault unconnected = new LethenonVault();

		assertFalse(unconnected.isAvailable());
		assertEquals(List.of(), unconnected.wallets());
		VaultAccessRefusedException refused = assertThrows(VaultAccessRefusedException.class,
			() -> unconnected.create("testnet", PASSWORD));
		assertTrue(refused.getMessage().contains("no vault"), refused.getMessage());
	}
}
