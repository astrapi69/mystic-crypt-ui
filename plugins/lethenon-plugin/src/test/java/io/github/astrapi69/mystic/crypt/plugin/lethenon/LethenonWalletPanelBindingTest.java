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

import static io.github.astrapi69.mystic.crypt.plugin.lethenon.Windows.textOf;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;

/**
 * The wallet window of the lethenon plugin (#680), read through its model and the names of its
 * components: the empty state "no wallet yet" with its three ways in, creating a wallet with its 24
 * words shown once, taking a wallet file over, restoring from the words, the overview of a wallet,
 * and what the window does when the vault locks.
 */
class LethenonWalletPanelBindingTest
{

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

	private LethenonWalletPanel window()
	{
		return new LethenonWalletPanel(vault, Locale.ENGLISH);
	}

	@Test
	@DisplayName("a vault without a wallet shows \"No wallet in the vault yet\" and the three ways to get one")
	void anEmptyVault_showsTheEmptyState()
	{
		LethenonWalletPanel panel = window();

		assertEquals(WalletView.EMPTY, panel.getModelObject().getView());
		assertEquals("No wallet in the vault yet", textOf(panel, "lblEmptyTitle"));
		assertEquals(List.of(), Windows.withoutATooltip(panel),
			"every field and button explains itself");
	}

	@Test
	@DisplayName("without an open vault the window says so and offers no wallet action")
	void withoutAVault_theWindowOffersNothing()
	{
		access.available = false;

		LethenonWalletPanel panel = window();

		assertEquals(WalletView.NO_VAULT, panel.getModelObject().getView());
		assertEquals("The vault is locked", textOf(panel, "lblNoVaultTitle"));
	}

	@Test
	@DisplayName("creating shows the 24 words once; after \"I have written them down\" the overview shows the wallet and the words are gone")
	void creating_showsTheWordsOnce_thenTheOverview()
	{
		LethenonWalletPanel panel = window();
		panel.onCreate();
		assertEquals(WalletView.CREATE, panel.getModelObject().getView());
		char[] password = ThrowawayPasswords.next();
		char[] typed = password.clone();
		panel.getModelObject().setPassword(typed);
		panel.getModelObject().setPasswordRepeated(password.clone());

		panel.onCreateConfirmed();

		assertEquals(WalletView.WORDS, panel.getModelObject().getView());
		assertArrayEquals(new char[typed.length], typed, "the typed password is overwritten");
		char[] words = panel.getModelObject().getShownWords();
		List<String> shown = new ArrayList<>();
		for (int position = 1; position <= 24; position++)
		{
			shown.add(textOf(panel, "lblWord" + position).replaceFirst("^\\d+\\. ", ""));
		}
		assertEquals(new String(words), String.join(" ", shown));
		String address = Wallet.fromPhrase(new String(words)).address().toText();

		panel.onWordsWrittenDown();

		assertEquals(WalletView.OVERVIEW, panel.getModelObject().getView());
		assertArrayEquals(new char[words.length], words, "the words are overwritten");
		assertTrue(textOf(panel, "lblWord1").isEmpty(), "no word is left on the screen");
		assertEquals(address, textOf(panel, "txtAddress"));
		assertEquals("Test network", textOf(panel, "txtNetwork"));
		assertEquals("lethenon wallet (testnet)", textOf(panel, "txtWalletTitle"));
	}

	@Test
	@DisplayName("cancelling while the words are shown creates no wallet")
	void cancellingTheWords_createsNoWallet()
	{
		LethenonWalletPanel panel = window();
		panel.onCreate();
		char[] password = ThrowawayPasswords.next();
		panel.getModelObject().setPassword(password.clone());
		panel.getModelObject().setPasswordRepeated(password.clone());
		panel.onCreateConfirmed();
		char[] words = panel.getModelObject().getShownWords();

		panel.onWordsCancelled();

		assertEquals(WalletView.EMPTY, panel.getModelObject().getView());
		assertEquals(List.of(), vault.wallets());
		assertArrayEquals(new char[words.length], words);
	}

	@Test
	@DisplayName("two different passwords are refused with a sentence, and nothing is created")
	void differentPasswords_areRefused()
	{
		LethenonWalletPanel panel = window();
		panel.onCreate();
		panel.getModelObject().setPassword(ThrowawayPasswords.next());
		panel.getModelObject().setPasswordRepeated(ThrowawayPasswords.next());

		panel.onCreateConfirmed();

		assertEquals(WalletView.CREATE, panel.getModelObject().getView());
		assertEquals("The two passwords are not the same. Type them again.",
			textOf(panel, "txtRefusal"));
		assertEquals(List.of(), vault.wallets());
	}

	@Test
	@DisplayName("restoring names a mistyped word with its position, and restores the corrected words")
	void restoring_namesAMistypedWord_thenRestores()
	{
		Wallet lost = Wallet.create();
		List<String> words = new ArrayList<>(List.of(lost.phrase().split(" ")));
		words.set(11, "qqq");
		LethenonWalletPanel panel = window();
		panel.onRestore();
		char[] password = ThrowawayPasswords.next();
		panel.getModelObject().setPassword(password.clone());
		panel.getModelObject().setPasswordRepeated(password.clone());
		panel.getModelObject().setWords(String.join(" ", words));

		panel.onRestoreConfirmed();

		assertEquals(WalletView.RESTORE, panel.getModelObject().getView());
		assertEquals("Word 12, \"qqq\", is not in the word list. Check your note for a typing error.",
			textOf(panel, "txtRefusal"));

		panel.getModelObject().setWords(lost.phrase());
		panel.onRestoreConfirmed();

		assertEquals(WalletView.OVERVIEW, panel.getModelObject().getView());
		assertEquals(lost.address().toText(), textOf(panel, "txtAddress"));
		assertEquals("", panel.getModelObject().getWords(), "the typed words are cleared");
	}

	@Test
	@DisplayName("taking a wallet file over lists the wallet and leaves the file where it was")
	void takingAFileOver_listsTheWallet() throws Exception
	{
		Wallet existing = Wallet.create();
		Path file = directory.resolve("my.lethwf");
		char[] password = ThrowawayPasswords.next();
		WalletFile.write(file, existing, password.clone());
		LethenonWalletPanel panel = window();
		panel.onTakeOver();
		panel.getModelObject().setWalletFile(file.toString());
		panel.getModelObject().setPassword(password.clone());

		panel.onTakeOverConfirmed();

		assertEquals(WalletView.OVERVIEW, panel.getModelObject().getView());
		assertEquals(existing.address().toText(), textOf(panel, "txtAddress"));
		assertTrue(Files.exists(file));
		assertTrue(textOf(panel, "txtNotice").contains("my.lethwf"), textOf(panel, "txtNotice"));
	}

	@Test
	@DisplayName("a wrong password for a wallet file is refused with the file's name, and nothing is taken over")
	void aWrongPasswordForAFile_isRefused() throws Exception
	{
		Path file = directory.resolve("other.lethwf");
		WalletFile.write(file, Wallet.create(), ThrowawayPasswords.next());
		LethenonWalletPanel panel = window();
		panel.onTakeOver();
		panel.getModelObject().setWalletFile(file.toString());
		panel.getModelObject().setPassword(ThrowawayPasswords.next());

		panel.onTakeOverConfirmed();

		assertEquals(WalletView.TAKE_OVER, panel.getModelObject().getView());
		assertTrue(textOf(panel, "txtRefusal").contains("other.lethwf"),
			textOf(panel, "txtRefusal"));
		assertEquals(List.of(), vault.wallets());
	}

	@Test
	@DisplayName("when the vault locks while the words are shown, they are overwritten and the window shows the locked state")
	void lockingWhileTheWordsAreShown_overwritesThem()
	{
		LethenonWalletPanel panel = window();
		panel.onCreate();
		char[] password = ThrowawayPasswords.next();
		panel.getModelObject().setPassword(password.clone());
		panel.getModelObject().setPasswordRepeated(password.clone());
		panel.onCreateConfirmed();
		char[] words = panel.getModelObject().getShownWords();

		access.available = false;
		vault.vaultClosing();

		assertEquals(WalletView.NO_VAULT, panel.getModelObject().getView());
		assertArrayEquals(new char[words.length], words);
		assertTrue(textOf(panel, "lblWord1").isEmpty());
	}

	@Test
	@DisplayName("a vault with wallets opens on the overview of the first, and another one can be chosen")
	void aVaultWithWallets_showsTheOverview()
	{
		vault.restore(Wallet.create().phrase().toCharArray(), ThrowawayPasswords.next(), WalletNetwork.TESTNET);
		Wallet second = Wallet.create();
		vault.restore(second.phrase().toCharArray(), ThrowawayPasswords.next(), WalletNetwork.MAINNET);

		LethenonWalletPanel panel = window();

		assertEquals(WalletView.OVERVIEW, panel.getModelObject().getView());
		assertEquals(2, panel.getModelObject().getWallets().size());
		panel.choose(panel.getModelObject().getWallets().get(1));
		assertEquals(second.address().toText(), textOf(panel, "txtAddress"));
		assertEquals("Main network", textOf(panel, "txtNetwork"));
		assertFalse(textOf(panel, "txtWalletTitle").isEmpty());
	}
}
