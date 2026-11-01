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

import java.awt.Component;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;
import java.util.Locale;

import javax.swing.JPanel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.Amount;
import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.Replay;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * Mining, sweeping and the mining node with the wallet in the vault (#706): each window names the
 * wallet the overview shows instead of a wallet file, acts with it unsealed in memory, and
 * overwrites the password; the shared {@link WalletRow} shows the file field again while the
 * overview shows no wallet.
 */
class VaultWalletWindowsTest
{

	@TempDir
	Path configuration;

	@TempDir
	Path directory;

	private LethenonVault vault;

	private Wallet wallet;

	private char[] password;

	private Path chainFile;

	@BeforeEach
	void aVaultWalletAndAChainPayingIt() throws Exception
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configuration.toString());
		vault = new LethenonVault();
		vault.connect(new InMemoryVaultAccess());
		wallet = Wallet.create();
		password = ThrowawayPasswords.next();
		KeyPair payer = TransactionSigner.newKeyPair(SignatureSuite.ED25519);
		chainFile = LethenonFixtures.written(directory.resolve("chain.lethenon"),
			LethenonFixtures.aChainPayingTheWallet(payer, wallet));
		vault.restore(wallet.phrase().toCharArray(), password.clone(), WalletNetwork.TESTNET);
		vault.choose(vault.wallets().get(0));
	}

	@AfterEach
	void releaseTheConfiguration()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	private Amount ed25519Balance() throws Exception
	{
		List<BlockBody> chain = new ChainFile(chainFile).require();
		Replay replay = ChainReplaySupport.replay(chainFile, chain);
		return replay.finalState().balanceOf(wallet.spendKey(SignatureSuite.ED25519));
	}

	private static void assertTheVaultWalletIsNamed(final JPanel window)
	{
		assertEquals("lethenon wallet (testnet), from the vault", textOf(window, "txtFromWallet"));
		assertFalse(Windows.component(window, "txtWalletFile", Component.class).isVisible());
	}

	@Test
	@DisplayName("mining pays the block to the vault's wallet, and the password is overwritten")
	void mining_paysTheVaultWallet() throws Exception
	{
		LethenonMinePanel panel = new LethenonMinePanel(ChainConversionConsent.ASK, vault,
			Locale.ENGLISH);
		assertTheVaultWalletIsNamed(panel);
		panel.getModelObject().setChainFile(chainFile.toString());
		char[] typed = password.clone();
		panel.getModelObject().setPassword(typed);
		Amount before = ed25519Balance();

		panel.onMine();

		assertEquals(4, new ChainFile(chainFile).require().size(), textOf(panel, "lblResult"));
		assertTrue(ed25519Balance().compareTo(before) > 0, "the reward went to the vault's wallet");
		assertArrayEquals(new char[typed.length], typed);
	}

	@Test
	@DisplayName("sweeping takes the one-time payment over into the vault's wallet, and the password is overwritten")
	void sweeping_usesTheVaultWallet() throws Exception
	{
		LethenonSweepPanel panel = new LethenonSweepPanel(ChainConversionConsent.ASK, vault,
			Locale.ENGLISH);
		assertTheVaultWalletIsNamed(panel);
		panel.getModelObject().setChainFile(chainFile.toString());
		panel.getModelObject().setFee("0.1");
		char[] typed = password.clone();
		panel.getModelObject().setPassword(typed);

		panel.onSweep();

		assertEquals("4.90000000 LETH", textOf(panel, "txtSwept"));
		assertArrayEquals(new char[typed.length], typed);
	}

	@Test
	@DisplayName("the node window names the vault's wallet for the blocks it mines")
	void theNodeWindow_namesTheVaultWallet()
	{
		LethenonNodePanel panel = new LethenonNodePanel(ChainConversionConsent.ASK, vault,
			Locale.ENGLISH);

		assertTheVaultWalletIsNamed(panel);
	}

	@Test
	@DisplayName("while the overview shows no wallet, the windows show the wallet file field again")
	void withoutAVaultWallet_theFileFieldIsBack()
	{
		vault.choose(null);

		LethenonSweepPanel panel = new LethenonSweepPanel(ChainConversionConsent.ASK, vault,
			Locale.ENGLISH);

		assertTrue(Windows.component(panel, "txtWalletFile", Component.class).isVisible());
		assertFalse(Windows.component(panel, "txtFromWallet", Component.class).isVisible());
	}
}
