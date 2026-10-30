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

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.lethenon.Wallet;

/**
 * The wallet in the vault: the words are stored only on the user's explicit choice, read back only
 * for a check, and removed with their buffers overwritten. Decision:
 * docs/decisions/lethenon-wallet-in-vault-2026-10-30.md (options 1 and 2, the backup assistant).
 * Written before the code it describes; the names of the types below are the ones the code is to
 * have.
 */
class WalletInVaultTest
{

	/** One wallet's 24 words, made once for the class */
	private static final List<String> WORDS = List.of(Wallet.create().phrase().split(" "));

	@Test
	@DisplayName("the words are not stored unless the user chooses it")
	void theWordsAreNotStoredWithoutAnExplicitChoice()
	{
		WalletEntry entry = WalletEntry.newEntry("testnet");

		WalletInVault.store(entry, WORDS, WordsChoice.KEEP_OUT);

		assertFalse(entry.hasProperty("lethenon.mnemonic"),
			"the default is no words in the vault");
	}

	@Test
	@DisplayName("stored as a protected property only after the explicit choice")
	void theWordsAreStoredAsAProtectedPropertyAfterTheChoice()
	{
		WalletEntry entry = WalletEntry.newEntry("testnet");

		WalletInVault.store(entry, WORDS, WordsChoice.STORE_IN_VAULT);

		assertTrue(entry.hasProperty("lethenon.mnemonic"));
		assertTrue(entry.isProtected("lethenon.mnemonic"),
			"the KDBX export must carry the protection flag");
	}

	@Test
	@DisplayName("removing the words deletes the property and overwrites the buffers that held them")
	void removingTheWordsDeletesThemAndOverwritesTheBuffers()
	{
		WalletEntry entry = WalletEntry.newEntry("testnet");
		WalletInVault.store(entry, WORDS, WordsChoice.STORE_IN_VAULT);
		char[][] held = WalletInVault.lastBuffersHeldForTest();

		WalletInVault.remove(entry);

		assertFalse(entry.hasProperty("lethenon.mnemonic"));
		for (char[] buffer : held)
		{
			for (char c : buffer)
			{
				assertEquals('\0', c, "a buffer still holds a character of a word");
			}
		}
	}

	@Test
	@DisplayName("the three-word question refuses a wrong word and passes the right three")
	void theThreeWordQuestionRefusesAWrongWord()
	{
		ThreeWordQuestion question = ThreeWordQuestion.ask(WORDS, new int[] { 4, 11, 19 });

		assertTrue(question.answer(WORDS.get(3), WORDS.get(10), WORDS.get(18)).correct());
		assertFalse(question.answer(WORDS.get(3), "wrong", WORDS.get(18)).correct());
	}

	@Test
	@DisplayName("the backup check refuses a list that is not 24 words, and names the count")
	void theBackupCheckRefusesAShortListWithTheCount()
	{
		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> BackupCheck.run(WORDS.subList(0, 23), WalletEntry.newEntry("testnet")));

		assertTrue(refused.getMessage().contains("23"), refused.getMessage());
	}
}
