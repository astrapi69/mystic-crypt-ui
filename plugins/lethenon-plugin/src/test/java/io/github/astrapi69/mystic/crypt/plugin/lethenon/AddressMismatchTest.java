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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;

/**
 * The warning when an entry's recorded address is not the address of the wallet in its attachment
 * (#716, T6 and T6a, ADR lethenon-wallet-in-vault): asked on every unseal, "Close" changes nothing,
 * only a confirmed replacement changes the entry, and never silently.
 */
class AddressMismatchTest
{

	private static final char[] PASSWORD = ThrowawayPasswords.next();

	private InMemoryVaultAccess access;

	private LethenonVault vault;

	private NewWallet created;

	private String foreign;

	private final List<AddressMismatch> asked = new ArrayList<>();

	@BeforeEach
	void aWalletWhoseEntryRecordsAForeignAddress()
	{
		access = new InMemoryVaultAccess();
		vault = new LethenonVault();
		vault.connect(access);
		created = vault.create(WalletNetwork.TESTNET, PASSWORD);
		foreign = Wallet.create().address().toText();
		recordOnTheEntry(foreign);
	}

	@AfterEach
	void closeTheWords()
	{
		created.close();
	}

	@Test
	@DisplayName("an unseal asks with both addresses, and \"Close\" changes nothing")
	void close_changesNothing()
	{
		vault.askWhenTheAddressDiffers(mismatch -> {
			asked.add(mismatch);
			return AddressMismatchQuestion.Answer.CLOSE;
		});

		UnsealedWallet opened = vault.open(created.id(), PASSWORD);

		assertEquals(1, asked.size());
		assertEquals(foreign, asked.get(0).recorded());
		assertEquals(opened.wallet().address().toText(), asked.get(0).actual());
		assertEquals(foreign, vault.wallets().get(0).address(), "nothing was changed");
	}

	@Test
	@DisplayName("a confirmed replacement writes the wallet's address on the entry and tells the windows")
	void replace_writesTheWalletsAddress()
	{
		vault.choose(vault.wallets().get(0));
		List<VaultWallet> told = new ArrayList<>();
		vault.whenTheWalletChanges(wallet -> wallet.ifPresent(told::add));
		vault.askWhenTheAddressDiffers(mismatch -> AddressMismatchQuestion.Answer.REPLACE);

		UnsealedWallet opened = vault.open(created.id(), PASSWORD);

		String actual = opened.wallet().address().toText();
		assertEquals(actual, vault.wallets().get(0).address());
		assertEquals(actual, vault.chosen().orElseThrow().address(),
			"the overview shows the wallet's address now");
		assertEquals(actual, told.get(told.size() - 1).address());
	}

	@Test
	@DisplayName("the overview shows the wallet's address once it was replaced, wherever the unseal happened")
	void theOverview_followsAReplacement()
	{
		LethenonWalletPanel overview = new LethenonWalletPanel(vault, Locale.ENGLISH);
		assertEquals(foreign, Windows.textOf(overview, "txtAddress"));
		vault.askWhenTheAddressDiffers(mismatch -> AddressMismatchQuestion.Answer.REPLACE);

		String actual = vault.open(created.id(), PASSWORD).wallet().address().toText();

		assertEquals(actual, Windows.textOf(overview, "txtAddress"));
	}

	@Test
	@DisplayName("no question when the recorded address is the wallet's")
	void aFittingAddress_asksNothing()
	{
		recordOnTheEntry(Wallet.fromPhrase(new String(created.words())).address().toText());
		vault.askWhenTheAddressDiffers(mismatch -> {
			asked.add(mismatch);
			return AddressMismatchQuestion.Answer.REPLACE;
		});

		vault.open(created.id(), PASSWORD);

		assertTrue(asked.isEmpty(), "asked: " + asked);
	}

	@Test
	@DisplayName("without a question, as in a window-less use, the vault changes nothing")
	void withoutAQuestion_nothingIsChanged()
	{
		vault.open(created.id(), PASSWORD);

		assertEquals(foreign, vault.wallets().get(0).address());
	}

	@Test
	@DisplayName("a replacement is refused when the entry no longer records the address that was shown")
	void aReplacement_isRefusedWhenTheEntryChanged()
	{
		UUID id = created.id();
		String actual = Wallet.fromPhrase(new String(created.words())).address().toText();
		AddressMismatch shown = new AddressMismatch(foreign, actual);
		String other = Wallet.create().address().toText();
		recordOnTheEntry(other);

		IllegalStateException refused = assertThrows(IllegalStateException.class,
			() -> vault.useTheWalletsAddress(id, shown));

		assertTrue(refused.getMessage().contains(foreign), refused.getMessage());
		assertEquals(other, vault.wallets().get(0).address(), "nothing was replaced");
	}

	@Test
	@DisplayName("the texts of T6 and T6a, in German and English, name both addresses")
	void theTexts()
	{
		AddressMismatch mismatch = new AddressMismatch("OLD", "NEW");

		assertEquals("Die Adresse in diesem Eintrag gehört nicht zu seiner Wallet",
			mismatch.title(Locale.GERMAN));
		assertEquals("Im Eintrag steht: OLD. Die Wallet im Anhang hat: NEW.",
			mismatch.bothAddresses(Locale.GERMAN));
		assertEquals("Anyone who paid to the old address paid to an address that does not belong "
			+ "to this wallet. Find out where the difference comes from.",
			mismatch.consequence(Locale.ENGLISH));
		assertEquals("Es wurde nichts geändert.", mismatch.nothingChanged(Locale.GERMAN));
		assertEquals("Adresse im Eintrag ersetzen?", mismatch.confirmTitle(Locale.GERMAN));
		assertEquals("The address in the entry is replaced. Old: OLD. New, the wallet's address: "
			+ "NEW. This wallet cannot spend payments made to the old address.",
			mismatch.confirmText(Locale.ENGLISH));
	}

	private void recordOnTheEntry(final String address)
	{
		MysticCryptEntryModelBean entry = access.entries().get(0);
		WalletEntry wallet = VaultWalletEntry.read(entry);
		wallet.setProperty(WalletEntry.ADDRESS, address.toCharArray(), false);
		VaultWalletEntry.write(wallet, entry);
		access.change(entry);
	}
}
