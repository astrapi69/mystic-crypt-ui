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

import static io.github.astrapi69.mystic.crypt.plugin.lethenon.Windows.shows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * Removing the 24 words from the vault (#821, A9 of #804): an action of its own with its own
 * confirmation, shown only for a wallet whose entry keeps the words; the box in step 2 of a backup
 * stays "also store", with a note in its place when the words are already kept
 */
class RemoveWordsTest
{

	@TempDir
	Path configuration;

	private InMemoryVaultAccess access;

	private LethenonVault vault;

	private char[] password;

	private UUID id;

	private final Wallet wallet = Wallet.create();

	@BeforeEach
	void aVaultWithAWalletThatKeepsItsWords()
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configuration.toString());
		access = new InMemoryVaultAccess();
		vault = new LethenonVault();
		vault.connect(access);
		password = ThrowawayPasswords.next();
		id = vault.restore(wallet.phrase().toCharArray(), password.clone(), WalletNetwork.TESTNET);
		vault.storeWords(id, wallet.phrase().toCharArray(), WordsChoice.STORE_IN_VAULT);
	}

	@AfterEach
	void releaseTheConfiguration()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	@Test
	@DisplayName("the overview of a wallet that keeps its words offers no way to remove them (#821)")
	void theOverview_offersNoRemoval()
	{
		LethenonWalletPanel panel = new LethenonWalletPanel(vault, Locale.ENGLISH);

		assertTrue(Windows.named(panel).stream()
			.noneMatch(component -> "btnRemoveWords".equals(component.getName())),
			"a removal of the words is offered");
	}

	@Test
	@DisplayName("step 2 of a backup offers the box to store the words also when they are kept already (#821)")
	void stepTwo_offersTheBox_whenTheWordsAreKept()
	{
		LethenonWalletPanel panel = new LethenonWalletPanel(vault, Locale.ENGLISH);
		toStepTwo(panel);

		assertTrue(shows(panel, "chkStoreWords"), "the box is not offered");
		assertFalse(Windows.named(panel).stream()
			.anyMatch(component -> "txtWordsKept".equals(component.getName())),
			"a note says the words are kept");
	}

	private void toStepTwo(final LethenonWalletPanel panel)
	{
		panel.onBackup();
		LethenonBackupPanel backup = panel.getBackupPanel();
		backup.getModelObject().setPassword(password.clone());
		backup.onUnsealConfirmed();
		backup.onWrittenDown();
	}
}
