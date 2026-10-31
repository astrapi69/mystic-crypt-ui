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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryAttachments;

/**
 * The wallet's properties survive the move into the vault's entry model and back, with their
 * protection flag: the words are protected, the network is not.
 */
class VaultWalletEntryTest
{

	@Test
	@DisplayName("a protected words property keeps its protection in the vault's entry model")
	void theProtectionOfTheWordsSurvivesTheMoveIntoTheEntry()
	{
		WalletEntry wallet = WalletEntry.empty();
		WalletInVault.store(wallet, List.of("abandon", "zoo"), WordsChoice.STORE_IN_VAULT);
		wallet.setProperty(WalletEntry.NETWORK, "testnet".toCharArray(), false);
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();

		VaultWalletEntry.write(wallet, entry);
		WalletEntry back = VaultWalletEntry.read(entry);

		assertTrue(back.hasProperty(WalletInVault.MNEMONIC));
		assertTrue(back.isProtected(WalletInVault.MNEMONIC), "the words must stay protected");
		assertFalse(back.isProtected(WalletEntry.NETWORK), "the network is a plain hint");
		assertEquals("testnet", back.textOf(WalletEntry.NETWORK));
	}

	@Test
	@DisplayName("the sealed wallet travels as the attachment wallet.lethwf, beside the entry's other attachments")
	void theSealedWalletTravelsAsItsOwnAttachment()
	{
		char[] password = "wallet password".toCharArray();
		try (CreatedWallet created = WalletVault.create("testnet", password))
		{
			MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();
			entry.getResources().add(FileContentInfo.builder().name("receipt.pdf")
				.content(new byte[] { 1, 2, 3 }).build());

			VaultWalletEntry.write(created.entry(), entry);
			WalletEntry back = VaultWalletEntry.read(entry);

			assertArrayEquals(created.entry().sealed(),
				VaultEntryAttachments.contentOf(entry, WalletVault.ATTACHMENT).orElseThrow());
			assertArrayEquals(created.entry().sealed(), back.sealed());
			assertTrue(WalletVault.isWallet(back));
			assertEquals(2, entry.getResources().size(), "the other attachment stays");
			assertEquals("lethenon wallet (testnet)", new String(entry.getTitle()),
				"people read the title, the plugin reads the marker");
		}
	}
}
