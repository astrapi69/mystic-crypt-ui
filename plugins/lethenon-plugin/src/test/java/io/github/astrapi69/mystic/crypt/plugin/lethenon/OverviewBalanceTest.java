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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.Amount;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The balance in the overview (#700, decision record "The balance in the overview"): both accounts
 * from public keys on the entry, without the wallet password; the public ML-DSA-65 account key
 * written on every new entry, added to an older one when it is unsealed, and compared with the
 * wallet like the address; the one-time payments only with the password; the amounts with two
 * decimals and the exact value below.
 */
class OverviewBalanceTest
{

	@TempDir
	Path directory;

	@TempDir
	Path configuration;

	private InMemoryVaultAccess access;

	private LethenonVault vault;

	private char[] password;

	private Wallet wallet;

	private final KeyPair payer = TransactionSigner.newKeyPair(SignatureSuite.ED25519);

	@BeforeEach
	void aVaultWithTheWallet()
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configuration.toString());
		access = new InMemoryVaultAccess();
		vault = new LethenonVault();
		vault.connect(access);
		password = ThrowawayPasswords.next();
		wallet = Wallet.create();
	}

	@AfterEach
	void releaseTheConfiguration()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	private UUID restored()
	{
		return vault.restore(wallet.phrase().toCharArray(), password.clone(),
			WalletNetwork.TESTNET);
	}

	private Path aChainPayingTheWallet() throws Exception
	{
		return LethenonFixtures.written(directory.resolve("chain.lethenon"),
			LethenonFixtures.aChainPayingTheWallet(payer, wallet));
	}

	@Test
	@DisplayName("a new entry carries the wallet's public ML-DSA-65 account key, unprotected")
	void aNewEntry_carriesTheMlDsaAccountKey()
	{
		restored();

		MysticCryptEntryModelBean entry = access.stored.get(0);
		assertEquals(wallet.spendKey(SignatureSuite.ML_DSA_65).toString(),
			VaultEntryProperties.valuesOf(entry).get(WalletEntry.ACCOUNT_ML_DSA));
		assertFalse(VaultEntryProperties.protectedNamesOf(entry)
			.contains(WalletEntry.ACCOUNT_ML_DSA), "the key is public, like the address");
		assertEquals(wallet.spendKey(SignatureSuite.ML_DSA_65).toString(),
			vault.wallets().get(0).mlDsaAccount());
	}

	@Test
	@DisplayName("an older entry without the key gets it the next time it is unsealed")
	void anOlderEntry_getsTheKeyWhenUnsealed()
	{
		UUID id = restored();
		MysticCryptEntryModelBean entry = access.stored.get(0);
		Map<String, String> values = new java.util.HashMap<>(VaultEntryProperties.valuesOf(entry));
		values.remove(WalletEntry.ACCOUNT_ML_DSA);
		VaultEntryProperties.replace(entry, values, Set.of());
		assertEquals("", vault.wallets().get(0).mlDsaAccount());

		vault.open(id, password.clone());

		assertEquals(wallet.spendKey(SignatureSuite.ML_DSA_65).toString(),
			vault.wallets().get(0).mlDsaAccount());
	}

	@Test
	@DisplayName("a key on the entry that is not the wallet's is named when unsealed, and neither is changed")
	void aDifferingKey_isNamed()
	{
		UUID id = restored();
		String other = Wallet.create().spendKey(SignatureSuite.ML_DSA_65).toString();
		MysticCryptEntryModelBean entry = access.stored.get(0);
		Map<String, String> values = new java.util.HashMap<>(VaultEntryProperties.valuesOf(entry));
		values.put(WalletEntry.ACCOUNT_ML_DSA, other);
		VaultEntryProperties.replace(entry, values, Set.of());

		UnsealedWallet opened = vault.open(id, password.clone());

		assertTrue(opened.addressWarning().orElse("").contains("ML-DSA-65 account"),
			opened.addressWarning().toString());
		assertEquals(other, vault.wallets().get(0).mlDsaAccount(), "the property is not changed");
	}

	@Test
	@DisplayName("the balance from the public keys alone is the sum of both accounts the wallet itself counts")
	void theBalance_fromPublicKeys_isTheWalletsAccounts() throws Exception
	{
		restored();
		Path chain = aChainPayingTheWallet();

		AccountsBalance balance = OverviewBalance.of(chain, vault.wallets().get(0));

		assertEquals(Amount.ofLeth(3L), balance.total(),
			"3 LETH to the Ed25519 account, nothing to the ML-DSA-65 account");
	}

	@Test
	@DisplayName("the one-time payments need the wallet password; a wrong one is refused, the password is overwritten")
	void theIncomingPayments_needThePassword() throws Exception
	{
		UUID id = restored();
		Path chain = aChainPayingTheWallet();
		assertThrows(SecurityException.class,
			() -> vault.incomingPayments(id, ThrowawayPasswords.next(), chain));
		char[] typed = password.clone();

		IncomingPayments incoming = vault.incomingPayments(id, typed, chain);

		assertEquals(1, incoming.count());
		assertEquals(Amount.ofLeth(5L), incoming.total());
		assertArrayEquals(new char[typed.length], typed, "the password is overwritten");
	}

	@Test
	@DisplayName("the overview shows the balance with two decimals, the exact value, and that incoming payments are not checked")
	void theOverview_showsTheBalance() throws Exception
	{
		restored();
		Path chain = aChainPayingTheWallet();
		PluginSettings.save(configuration.toFile(), LethenonSettingsContribution.PLUGIN_ID,
			new LethenonSettingsContribution().getDefaults(),
			Map.of(LethenonSettingsContribution.KEY_CHAIN_FILE, chain.toString()));

		LethenonWalletPanel panel = new LethenonWalletPanel(vault, Locale.ENGLISH);

		assertEquals("3.00 LETH", textOf(panel, "lblBalance"));
		assertEquals("exactly 3.00000000 LETH, without unchecked incoming payments",
			textOf(panel, "txtBalanceExact"));

		panel.onCheckIncoming();
		panel.getModelObject().setPassword(password.clone());
		panel.onCheckIncomingConfirmed();

		assertEquals(WalletView.OVERVIEW, panel.getModelObject().getView());
		assertEquals("exactly 3.00000000 LETH; besides, 5.00 LETH came to your address in 1 payment "
			+ "and is not yet taken over", textOf(panel, "txtBalanceExact"));
	}

	@Test
	@DisplayName("without a chain file the overview says so instead of a balance")
	void withoutAChain_theOverviewSaysSo()
	{
		restored();

		LethenonWalletPanel panel = new LethenonWalletPanel(vault, Locale.ENGLISH);

		assertEquals("", textOf(panel, "lblBalance"));
		assertTrue(textOf(panel, "txtBalanceExact").startsWith("No chain yet"),
			textOf(panel, "txtBalanceExact"));
	}

	@Test
	@DisplayName("two decimals in the overview, in the language of the window")
	void twoDecimals_inTheLanguageOfTheWindow()
	{
		Amount amount = Amount.parseLeth("12345.6789");

		assertEquals("12,345.68 LETH", LethAmounts.rounded(amount, Locale.ENGLISH));
		assertEquals("12.345,68 LETH", LethAmounts.rounded(amount, Locale.GERMAN));
	}
}
