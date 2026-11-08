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

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.swing.JButton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.transport.Node;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The overview without a chain (A6 of #804, draft 06, #827): what it shows in the place of the
 * balance, and how the chain is fetched from there
 */
class FetchTheChainTest
{

	@TempDir
	Path configuration;

	@TempDir
	Path directory;

	private LethenonVault vault;

	private final Wallet wallet = Wallet.create();

	private final KeyPair payer = TransactionSigner.newKeyPair(SignatureSuite.ED25519);

	@BeforeEach
	void aVaultWithAWallet()
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configuration.toString());
		vault = new LethenonVault();
		vault.connect(new InMemoryVaultAccess());
		vault.restore(wallet.phrase().toCharArray(), ThrowawayPasswords.next(),
			WalletNetwork.TESTNET);
	}

	@AfterEach
	void releaseTheConfiguration()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	@Test
	@DisplayName("without a chain the overview shows \"No chain yet\" with \"Fetch the chain\" in the place of the balance, and nothing to check (#827)")
	void withoutAChain_theOverviewShowsTheEmptyStateOfDraft06()
	{
		LethenonWalletPanel panel = new LethenonWalletPanel(vault, Locale.ENGLISH);

		assertTrue(shows(panel, "btnFetchChain"), "no button fetches the chain");
		assertEquals("No chain yet", textOf(panel, "lblNoChainTitle"));
		assertEquals("No node in the settings: the seed nodes of the test network are asked.",
			textOf(panel, "txtFetchNode"));
		assertFalse(shows(panel, "lblBalance"), "an empty balance shows");
		assertFalse(shows(panel, "btnCheckIncoming"), "incoming payments without a chain to check");
	}

	@Test
	@DisplayName("the empty state names the node of the settings it fetches from (#827)")
	void theEmptyState_namesTheNodeOfTheSettings()
	{
		LethenonSettingsContribution.remember(
			Map.of(LethenonSettingsContribution.KEY_NODE, "127.0.0.1:18480"));

		LethenonWalletPanel panel = new LethenonWalletPanel(vault, Locale.ENGLISH);

		assertEquals("Node from the settings: 127.0.0.1:18480", textOf(panel, "txtFetchNode"));
	}

	@Test
	@DisplayName("\"Fetch the chain\" writes the configured chain file from the node of the settings, remembers it and shows the balance (#827)")
	void fetching_intoTheConfiguredFile_showsTheBalance() throws Exception
	{
		Path chainFile = directory.resolve("not-yet.lethenon");
		try (Node node = Node.on(LethenonFixtures.aChainPayingTheWallet(payer, wallet)))
		{
			String address = "127.0.0.1:" + node.listen(0);
			LethenonSettingsContribution.remember(
				Map.of(LethenonSettingsContribution.KEY_CHAIN_FILE, chainFile.toString(),
					LethenonSettingsContribution.KEY_NODE, address));
			LethenonWalletPanel panel = new LethenonWalletPanel(vault, Locale.ENGLISH);

			panel.onFetchChain();

			assertEquals(3, new ChainFile(chainFile).require().size(), "the chain was written");
			assertEquals("3.00 LETH", textOf(panel, "lblBalance"));
			assertTrue(shows(panel, "btnCheckIncoming"));
			assertFalse(shows(panel, "btnFetchChain"), "the empty state stays after the fetch");
			assertEquals(address, LethenonSettingsContribution.node());
			assertEquals(chainFile.toString(), LethenonSettingsContribution.chainFile());
		}
	}

	@Test
	@DisplayName("with no chain file configured, \"Fetch the chain\" asks where to keep it and writes it there (#827)")
	void fetching_withNoChainFileConfigured_writesWhereThePersonChose() throws Exception
	{
		File chosen = directory.resolve("chosen.lethenon").toFile();
		try (Node node = Node.on(LethenonFixtures.aChainPayingTheWallet(payer, wallet)))
		{
			LethenonSettingsContribution.remember(
				Map.of(LethenonSettingsContribution.KEY_NODE, "127.0.0.1:" + node.listen(0)));
			LethenonWalletPanel panel = choosing(Optional.of(chosen));

			panel.onFetchChain();

			assertTrue(chosen.isFile(), "the chain was written where the person chose");
			assertEquals(chosen.toString(), LethenonSettingsContribution.chainFile());
			assertEquals("3.00 LETH", textOf(panel, "lblBalance"));
		}
	}

	@Test
	@DisplayName("the chooser proposes lethenon-<network>.lethenon in the home folder (#827)")
	void theChooser_proposesTheNetworkInTheName()
	{
		LethenonWalletPanel panel = new LethenonWalletPanel(vault, Locale.ENGLISH);

		assertEquals(new File(System.getProperty("user.home"), "lethenon-testnet.lethenon"),
			panel.proposedChainFile());
	}

	@Test
	@DisplayName("cancelling the chooser fetches nothing and remembers nothing (#827)")
	void cancellingTheChooser_fetchesNothing()
	{
		LethenonWalletPanel panel = choosing(Optional.empty());

		panel.onFetchChain();

		assertEquals("", LethenonSettingsContribution.chainFile());
		assertTrue(shows(panel, "btnFetchChain"), "the empty state stays");
		assertEquals("", textOf(panel, "txtRefusal"));
	}

	@Test
	@DisplayName("a node that cannot be reached is named in the refusal, nothing is written or remembered, and the empty state stays (#827)")
	void aNodeThatCannotBeReached_saysWhy() throws Exception
	{
		Path chainFile = directory.resolve("not-yet.lethenon");
		String address = "127.0.0.1:" + aPortNobodyListensOn();
		LethenonSettingsContribution.remember(
			Map.of(LethenonSettingsContribution.KEY_CHAIN_FILE, chainFile.toString(),
				LethenonSettingsContribution.KEY_NODE, address));
		LethenonWalletPanel panel = new LethenonWalletPanel(vault, Locale.ENGLISH);

		panel.onFetchChain();

		assertTrue(textOf(panel, "txtRefusal").startsWith("The chain was not fetched: "),
			textOf(panel, "txtRefusal"));
		assertTrue(textOf(panel, "txtRefusal").contains(address), textOf(panel, "txtRefusal"));
		assertFalse(Files.exists(chainFile), "a failed fetch leaves no file behind");
		assertTrue(shows(panel, "btnFetchChain"), "the empty state stays");
		assertTrue(fetchButton(panel).isEnabled(), "the button can be pressed again");
	}

	private LethenonWalletPanel choosing(final Optional<File> answer)
	{
		return new LethenonWalletPanel(vault, Locale.ENGLISH)
		{
			private static final long serialVersionUID = 1L;

			@Override
			Optional<File> chooseChainTarget()
			{
				return answer;
			}
		};
	}

	private static JButton fetchButton(final LethenonWalletPanel panel)
	{
		return Windows.component(panel, "btnFetchChain", JButton.class);
	}

	private static int aPortNobodyListensOn() throws IOException
	{
		try (ServerSocket socket = new ServerSocket(0))
		{
			return socket.getLocalPort();
		}
	}
}
