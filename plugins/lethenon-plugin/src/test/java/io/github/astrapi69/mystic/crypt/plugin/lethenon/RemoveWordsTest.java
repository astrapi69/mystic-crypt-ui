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
import static io.github.astrapi69.mystic.crypt.plugin.lethenon.Windows.textOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.swing.AbstractButton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;
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
	@DisplayName("the overview of a wallet that keeps its words offers to remove them (#821)")
	void theOverview_offersTheRemoval_whenTheWordsAreKept()
	{
		LethenonWalletPanel panel = new LethenonWalletPanel(vault, Locale.ENGLISH);

		assertTrue(shows(panel, "btnRemoveWords"), "no removal of the words is offered");
		assertEquals("Remove words from vault",
			Windows.component(panel, "btnRemoveWords", AbstractButton.class).getText());
	}

	@Test
	@DisplayName("the confirmation asks and says what removing does and what stays in memory, before anything is removed (#821, #852)")
	void theConfirmation_saysWhatRemovingDoes()
	{
		LethenonWalletPanel panel = new LethenonWalletPanel(vault, Locale.ENGLISH);

		panel.onRemoveWords();

		assertEquals(WalletView.REMOVE_WORDS, panel.getModelObject().getView());
		assertEquals("Remove the words from the vault?",
			Windows.component(panel, "lblRemoveWordsTitle", javax.swing.JLabel.class).getText());
		assertEquals("The property lethenon.mnemonic is removed from the entry. The internal buffers "
			+ "owned by the plugin are overwritten. The opened vault may keep an immutable String "
			+ "copy in memory until it is garbage collected or the program exits. After the vault "
			+ "is saved, the words are not written to the vault file.",
			textOf(panel, "txtRemoveWordsEffect"));
		assertTrue(vault.keepsWords(id), "the words went before the confirmation");
	}

	@Test
	@DisplayName("confirming removes the property and nothing else, hides the action and says to save the vault (#821)")
	void confirming_removesTheWords_andNothingElse()
	{
		Map<String, String> before = new LinkedHashMap<>(storedProperties());
		LethenonWalletPanel panel = new LethenonWalletPanel(vault, Locale.ENGLISH);
		panel.onRemoveWords();

		panel.onRemoveWordsConfirmed();

		Map<String, String> expected = new LinkedHashMap<>(before);
		expected.remove(WalletInVault.MNEMONIC);
		assertEquals(expected, storedProperties(), "the entry lost more or less than the words");
		assertFalse(vault.keepsWords(id));
		assertEquals(WalletView.OVERVIEW, panel.getModelObject().getView());
		assertFalse(shows(panel, "btnRemoveWords"), "the action stays after the words are gone");
		assertEquals("The 24 words are removed from this wallet's entry. Save the vault, then they "
			+ "are gone from the file as well.", textOf(panel, "txtNotice"));
	}

	@Test
	@DisplayName("cancelling the confirmation keeps the words (#821)")
	void cancelling_keepsTheWords()
	{
		LethenonWalletPanel panel = new LethenonWalletPanel(vault, Locale.ENGLISH);
		panel.onRemoveWords();

		Windows.component(panel, "btnRemoveWordsCancelled", AbstractButton.class).doClick();

		assertTrue(vault.keepsWords(id));
		assertEquals(wallet.phrase(), storedProperties().get(WalletInVault.MNEMONIC));
		assertEquals(WalletView.OVERVIEW, panel.getModelObject().getView());
	}

	@Test
	@DisplayName("step 2 of a backup says the words are kept instead of offering the box, and leaves them as they are (#821)")
	void stepTwo_saysTheWordsAreKept_insteadOfTheBox()
	{
		LethenonWalletPanel panel = new LethenonWalletPanel(vault, Locale.ENGLISH);
		toStepTwo(panel);

		assertFalse(shows(panel, "chkStoreWords"), "the box is offered for words already kept");
		assertFalse(shows(panel, "lblStoreQuestion"));
		assertFalse(shows(panel, "txtStoreRisk"));
		assertTrue(shows(panel, "txtWordsKept"));
		assertEquals("Words are kept in the vault. Use \"Remove words from vault\" to delete them.",
			textOf(panel, "txtWordsKept"));
		assertFalse(panel.getBackupPanel().getModelObject().isStoreWords());
		assertEquals(wallet.phrase(), storedProperties().get(WalletInVault.MNEMONIC),
			"opening step 2 changed the words");
	}

	@Test
	@DisplayName("once the words are removed, step 2 offers the box again and no note (#821)")
	void stepTwo_offersTheBox_onceTheWordsAreRemoved()
	{
		vault.removeWords(id);
		LethenonWalletPanel panel = new LethenonWalletPanel(vault, Locale.ENGLISH);

		assertFalse(shows(panel, "btnRemoveWords"), "a removal is offered for words not kept");
		toStepTwo(panel);
		assertTrue(shows(panel, "chkStoreWords"), "the box is not offered");
		assertFalse(shows(panel, "txtWordsKept"), "a note says the words are kept");
	}

	private Map<String, String> storedProperties()
	{
		return VaultEntryProperties.valuesOf(access.stored.get(0));
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
