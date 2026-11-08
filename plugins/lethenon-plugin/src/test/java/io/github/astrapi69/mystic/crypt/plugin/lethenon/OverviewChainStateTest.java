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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * How current the overview's chain is (A5 of #804, draft 01, #831 part 2): when this program last
 * synced it, and how old its last block is
 */
class OverviewChainStateTest
{

	@TempDir
	Path directory;

	@TempDir
	Path configuration;

	private LethenonVault vault;

	private final Wallet wallet = Wallet.create();

	private final KeyPair payer = TransactionSigner.newKeyPair(SignatureSuite.ED25519);

	@BeforeEach
	void aVaultWithAWalletAndAChain() throws Exception
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configuration.toString());
		vault = new LethenonVault();
		vault.connect(new InMemoryVaultAccess());
		vault.restore(wallet.phrase().toCharArray(), ThrowawayPasswords.next(),
			WalletNetwork.TESTNET);
		Path chainFile = LethenonFixtures.written(directory.resolve("chain.lethenon"),
			LethenonFixtures.aChainPayingTheWallet(payer, wallet));
		LethenonSettingsContribution.remember(
			Map.of(LethenonSettingsContribution.KEY_CHAIN_FILE, chainFile.toString()));
	}

	@AfterEach
	void releaseTheConfiguration()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	@Test
	@DisplayName("with a chain the overview says neither when it was synced nor how old its last block is (pinned, #831)")
	void withAChain_theOverviewSaysNothingOfHowCurrentItIs()
	{
		LethenonWalletPanel panel = new LethenonWalletPanel(vault, Locale.ENGLISH);

		assertTrue(Windows.named(panel).stream()
			.noneMatch(component -> "txtChainState".equals(component.getName())),
			"a line that says how current the chain is (pinned, #831)");
	}
}
