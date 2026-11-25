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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;
import io.github.astrapi69.mystic.crypt.plugin.api.VaultExport;

/**
 * The line the plugin gives the host before something leaves the vault (#810, decision 4 of
 * docs/decisions/lethenon-wallet-in-vault-2026-10-30.md): what a KDBX export holds of the wallets,
 * with the words named when a wallet keeps them, and what a saved wallet attachment is
 */
class LethenonVaultExportNoticeTest
{

	private static final String SEALED_ONLY = "The file contains sealed lethenon wallets. The wallet password is needed to use them; the 24 words are not in the file.";

	private static final String ATTACHMENT = "This file is a sealed lethenon wallet. The wallet password is needed to use it; the 24 words are not in it.";

	private InMemoryVaultAccess access;

	private LethenonVault vault;

	@BeforeEach
	void aConnectedVault()
	{
		access = new InMemoryVaultAccess();
		vault = new LethenonVault();
		vault.connect(access);
	}

	private void aWalletEntry(final boolean keepsItsWords)
	{
		Map<String, String> values = new LinkedHashMap<>();
		values.put(WalletVault.WALLET, "1");
		Set<String> protectedNames = new LinkedHashSet<>();
		if (keepsItsWords)
		{
			values.put(WalletInVault.MNEMONIC, "made up words that stand in for the twenty four");
			protectedNames.add(WalletInVault.MNEMONIC);
		}
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();
		VaultEntryProperties.replace(entry, values, protectedNames);
		access.add(entry);
	}

	@Test
	@DisplayName("an export of a vault without wallets gets no line")
	void aVaultWithoutWallets_givesNoLine()
	{
		assertEquals(Optional.empty(), vault.exportNotice(VaultExport.DATABASE, Locale.ENGLISH));
	}

	@Test
	@DisplayName("a vault that is not open gives no line, whatever it held")
	void aVaultThatIsNotOpen_givesNoLine()
	{
		aWalletEntry(true);
		access.available = false;

		assertEquals(Optional.empty(), vault.exportNotice(VaultExport.DATABASE, Locale.ENGLISH));
	}

	@Test
	@DisplayName("an export of wallets that do not keep their words says they are sealed and the words are not in the file")
	void walletsWithoutTheirWords_areNamedAsSealed()
	{
		aWalletEntry(false);
		aWalletEntry(false);

		assertEquals(Optional.of(SEALED_ONLY),
			vault.exportNotice(VaultExport.DATABASE, Locale.ENGLISH));
	}

	static Stream<Arguments> walletsKeepingTheirWords()
	{
		return Stream.of(Arguments.of("one wallet, keeping its words", 1, 0),
			Arguments.of("one of two wallets keeping its words", 1, 1),
			Arguments.of("two of three wallets keeping their words", 2, 1));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("walletsKeepingTheirWords")
	@DisplayName("an export of wallets that keep their words names how many and what that means")
	void walletsKeepingTheirWords_areCounted(final String description, final int withWords,
		final int withoutWords)
	{
		for (int index = 0; index < withWords; index++)
		{
			aWalletEntry(true);
		}
		for (int index = 0; index < withoutWords; index++)
		{
			aWalletEntry(false);
		}

		assertEquals(Optional.of("The file contains sealed lethenon wallets, and the 24 words of "
			+ withWords
			+ " of them in a protected field: whoever can open the file has these wallets, also without the wallet password."),
			vault.exportNotice(VaultExport.DATABASE, Locale.ENGLISH));
	}

	@Test
	@DisplayName("a saved wallet attachment is named as one sealed wallet, also in a vault without others")
	void aSavedAttachment_isNamedAsASealedWallet()
	{
		assertEquals(Optional.of(ATTACHMENT),
			vault.exportNotice(VaultExport.ATTACHMENT, Locale.ENGLISH));
	}

	@Test
	@DisplayName("the lines are German for a German user")
	void theLinesAreGermanForAGermanUser()
	{
		aWalletEntry(true);

		assertEquals(Optional.of(
			"Die Datei enthält versiegelte lethenon-Wallets und von 1 davon die 24 Wörter in einem geschützten Feld: Wer die Datei öffnen kann, hat diese Wallets, auch ohne das Wallet-Passwort."),
			vault.exportNotice(VaultExport.DATABASE, Locale.GERMAN));
		assertEquals(Optional.of(
			"Diese Datei ist ein versiegeltes lethenon-Wallet. Zum Benutzen braucht es das Wallet-Passwort; die 24 Wörter sind nicht darin."),
			vault.exportNotice(VaultExport.ATTACHMENT, Locale.GERMAN));
	}

	/**
	 * A wallet entry as the export of chosen entries writes it: with its sealed wallet when the
	 * attachments were chosen, with its words when the custom fields were and it keeps them
	 */
	private static MysticCryptEntryModelBean exported(final boolean sealed, final boolean words)
	{
		Map<String, String> values = new LinkedHashMap<>();
		Set<String> protectedNames = new LinkedHashSet<>();
		if (words)
		{
			values.put(WalletVault.WALLET, "1");
			values.put(WalletInVault.MNEMONIC, "made up words that stand in for the twenty four");
			protectedNames.add(WalletInVault.MNEMONIC);
		}
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();
		VaultEntryProperties.replace(entry, values, protectedNames);
		if (sealed)
		{
			entry.setResources(List.of(FileContentInfo.builder().name(WalletVault.ATTACHMENT)
				.content(new byte[] { 1, 2, 3 }).build()));
		}
		return entry;
	}

	static Stream<Arguments> chosenEntries()
	{
		return Stream.of(
			Arguments.of("sealed wallets without their words", List.of(exported(true, false)),
				Optional.of(SEALED_ONLY)),
			Arguments.of("sealed wallets, two with their words",
				List.of(exported(true, true), exported(true, true), exported(true, false)),
				Optional.of("The file contains sealed lethenon wallets, and the 24 words of 2"
					+ " of them in a protected field: whoever can open the file has these wallets, also without the wallet password.")),
			Arguments.of("the words without the sealed wallets", List.of(exported(false, true)),
				Optional.of("The file contains the 24 words of 1 lethenon wallets in a protected"
					+ " field: whoever can open the file can restore these wallets.")),
			Arguments.of("neither the sealed wallets nor their words",
				List.of(exported(false, false)), Optional.empty()));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("chosenEntries")
	@DisplayName("the export of chosen entries names what of the wallets goes into the file (#1118)")
	void theExportOfChosenEntries_namesWhatOfTheWalletsGoesIn(final String description,
		final List<MysticCryptEntryModelBean> exported, final Optional<String> expected)
	{
		assertEquals(expected,
			vault.exportNotice(VaultExport.SELECTED_ENTRIES, exported, Locale.ENGLISH));
	}

	@Test
	@DisplayName("the export of chosen entries counts only those, not the other wallets of the vault (#1118)")
	void theExportOfChosenEntries_countsOnlyThose()
	{
		aWalletEntry(true);
		aWalletEntry(true);
		aWalletEntry(true);

		assertEquals(Optional.of("The file contains sealed lethenon wallets, and the 24 words of 1"
			+ " of them in a protected field: whoever can open the file has these wallets, also without the wallet password."),
			vault.exportNotice(VaultExport.SELECTED_ENTRIES, List.of(exported(true, true)),
				Locale.ENGLISH));
	}

	@Test
	@DisplayName("the line of the words without the sealed wallets is German for a German user (#1118)")
	void theWordsWithoutTheSealedWallets_areGermanForAGermanUser()
	{
		assertEquals(Optional.of(
			"Die Datei enthält von 1 lethenon-Wallets die 24 Wörter in einem geschützten Feld: Wer die Datei öffnen kann, kann diese Wallets wiederherstellen."),
			vault.exportNotice(VaultExport.SELECTED_ENTRIES, List.of(exported(false, true)),
				Locale.GERMAN));
	}
}
