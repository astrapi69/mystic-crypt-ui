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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;

/**
 * The backup assistant's work on the vault (#690): the words again from the wallet password, the
 * words stored only on the person's choice, a backup check that records its day only when it
 * matches, the state the overview shows, and the export of the sealed file. Every change reaches
 * the vault through {@code VaultAccess.change}.
 */
class LethenonVaultBackupTest
{

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 31);

	@TempDir
	Path directory;

	private InMemoryVaultAccess access;

	private LethenonVault vault;

	private char[] password;

	private Wallet wallet;

	private UUID id;

	@BeforeEach
	void aVaultWithOneWallet()
	{
		access = new InMemoryVaultAccess();
		vault = new LethenonVault();
		vault.connect(access);
		password = ThrowawayPasswords.next();
		wallet = Wallet.create();
		id = vault.restore(wallet.phrase().toCharArray(), password.clone(), WalletNetwork.TESTNET);
	}

	private Map<String, String> storedProperties()
	{
		return VaultEntryProperties.valuesOf(access.stored.get(0));
	}

	@Test
	@DisplayName("the wallet password gives the 24 words again, a wrong one gives nothing")
	void theWordsAgain_needTheWalletPassword()
	{
		assertArrayEquals(wallet.phrase().toCharArray(), vault.wordsOf(id, password.clone()));
		assertThrows(SecurityException.class, () -> vault.wordsOf(id, ThrowawayPasswords.next()));
	}

	@Test
	@DisplayName("the words are stored, protected, only when the person chose to; keeping them out stores nothing")
	void theWords_areStoredOnlyOnChoice()
	{
		vault.storeWords(id, wallet.phrase().toCharArray(), WordsChoice.KEEP_OUT);
		assertFalse(storedProperties().containsKey(WalletInVault.MNEMONIC));

		vault.storeWords(id, wallet.phrase().toCharArray(), WordsChoice.STORE_IN_VAULT);

		assertEquals(wallet.phrase(), storedProperties().get(WalletInVault.MNEMONIC));
		assertTrue(VaultEntryProperties.protectedNamesOf(access.stored.get(0))
			.contains(WalletInVault.MNEMONIC), "the words are a protected field");
		assertTrue(vault.wallets().get(0).address().equals(wallet.address().toText()),
			"the wallet is still the same wallet");
	}

	@Test
	@DisplayName("a matching check records the day and the state is current; a check that does not match changes nothing")
	void aCheck_recordsItsDayOnlyWhenItMatches()
	{
		assertEquals(BackupState.NOT_CHECKED, vault.backupStateOf(id, TODAY));
		List<String> other = List.of(Wallet.create().phrase().split(" "));

		assertFalse(vault.checkBackup(id, other, TODAY));
		assertEquals(BackupState.NOT_CHECKED, vault.backupStateOf(id, TODAY));

		assertTrue(vault.checkBackup(id, List.of(wallet.phrase().split(" ")), TODAY));
		assertEquals(TODAY.toString(), storedProperties().get(BackupReminder.VERIFIED));
		assertEquals(BackupState.CURRENT, vault.backupStateOf(id, TODAY.plusDays(180)));
		assertEquals(BackupState.DUE, vault.backupStateOf(id, TODAY.plusDays(181)));
	}

	@Test
	@DisplayName("a check with a word that is not in the list is refused with the word and its position, and records nothing")
	void aCheckWithAnUnknownWord_isRefused()
	{
		List<String> typed = new ArrayList<>(List.of(wallet.phrase().split(" ")));
		typed.set(4, "zzz");

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> vault.checkBackup(id, typed, TODAY));

		assertTrue(refused.getMessage().contains("word 5, 'zzz'"), refused.getMessage());
		assertFalse(storedProperties().containsKey(BackupReminder.VERIFIED));
	}

	@Test
	@DisplayName("the export writes the sealed file once, after the warning, and returns its SHA-256 as read back")
	void theExport_writesTheSealedFileOnce() throws Exception
	{
		Path target = directory.resolve("stick.lethwf");

		assertThrows(IllegalStateException.class,
			() -> vault.export(id, target, ExportConsent.NOT_GIVEN));
		assertFalse(Files.exists(target), "nothing is written without the confirmation");

		String checksum = vault.export(id, target, ExportConsent.GIVEN_AFTER_WARNING);

		assertEquals(sha256(Files.readAllBytes(target)), checksum);
		assertEquals(wallet.address(), WalletFile.read(target, password.clone()).address(),
			"the file is the sealed wallet, opened by its password");
		assertThrows(FileAlreadyExistsException.class,
			() -> vault.export(id, target, ExportConsent.GIVEN_AFTER_WARNING));
	}

	private static String sha256(final byte[] bytes) throws Exception
	{
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
	}

	@Test
	@DisplayName("an identifier the vault does not have is refused with that identifier")
	void anUnknownIdentifier_isRefused() throws IOException
	{
		UUID unknown = UUID.randomUUID();

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> vault.backupStateOf(unknown, TODAY));
		assertTrue(refused.getMessage().contains(unknown.toString()), refused.getMessage());
	}
}
