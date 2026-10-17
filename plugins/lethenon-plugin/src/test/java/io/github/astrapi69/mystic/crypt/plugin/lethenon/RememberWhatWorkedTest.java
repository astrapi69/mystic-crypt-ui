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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.Chain;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;
import io.github.astrapi69.lethenon.transport.Node;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The windows remember the chain file, the wallet file, the node and the SOCKS proxy once they have
 * worked, so that they are typed once (#605): a window that succeeded leaves its values in the
 * plugin's settings, a window that failed leaves them as they were, and a new window starts with
 * them. A password is never remembered, in no form: it is in none of the files the settings write.
 */
class RememberWhatWorkedTest
{

	private static final String PASSWORD = "correct horse battery staple";

	@TempDir
	File configurationDirectory;

	@TempDir
	Path directory;

	private Path chainFile;

	private Path walletFile;

	@BeforeEach
	void writeAChainAndAWallet() throws Exception
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configurationDirectory.getAbsolutePath());
		Wallet wallet = Wallet.create();
		walletFile = directory.resolve("home.wallet");
		WalletFile.write(walletFile, wallet, PASSWORD.toCharArray());
		KeyPair payer = TransactionSigner.newKeyPair(SignatureSuite.ED25519);
		chainFile = directory.resolve("chain.lethenon");
		LethenonFixtures.written(chainFile, LethenonFixtures.aChainPayingTheWallet(payer, wallet));
	}

	@AfterEach
	void releaseTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	@Test
	@DisplayName("a balance that was read leaves its chain and its wallet in the settings")
	void aBalance_remembersTheChainAndTheWallet()
	{
		LethenonBalancePanel panel = new LethenonBalancePanel();
		panel.getModelObject().setChainFile(chainFile.toString());
		panel.getModelObject().setWalletFile(walletFile.toString());
		panel.getModelObject().setPassword(PASSWORD.toCharArray());

		panel.onShow();

		assertEquals(chainFile.toString(), LethenonSettingsContribution.chainFile());
		assertEquals(walletFile.toString(), LethenonSettingsContribution.walletFile());
	}

	@Test
	@DisplayName("a wrong password leaves what worked before as it was")
	void aFailure_leavesTheRememberedValues()
	{
		LethenonSettingsContribution.remember(Map.of(
			LethenonSettingsContribution.KEY_CHAIN_FILE, "/tmp/worked.lethenon",
			LethenonSettingsContribution.KEY_WALLET_FILE, "/tmp/worked.wallet"));
		LethenonBalancePanel panel = new LethenonBalancePanel();
		panel.getModelObject().setChainFile(chainFile.toString());
		panel.getModelObject().setWalletFile(directory.resolve("typo.wallet").toString());
		panel.getModelObject().setPassword("not the password".toCharArray());

		panel.onShow();

		assertEquals("/tmp/worked.lethenon", LethenonSettingsContribution.chainFile());
		assertEquals("/tmp/worked.wallet", LethenonSettingsContribution.walletFile(),
			"a typo that failed does not replace a value that worked");
	}

	@Test
	@DisplayName("a mined block leaves its chain and its wallet in the settings")
	void aBlock_remembersTheChainAndTheWallet()
	{
		LethenonMinePanel panel = new LethenonMinePanel();
		panel.getModelObject().setChainFile(chainFile.toString());
		panel.getModelObject().setWalletFile(walletFile.toString());
		panel.getModelObject().setPassword(PASSWORD.toCharArray());

		panel.onMine();

		assertEquals(chainFile.toString(), LethenonSettingsContribution.chainFile());
		assertEquals(walletFile.toString(), LethenonSettingsContribution.walletFile());
	}

	@Test
	@DisplayName("a signed transfer leaves its chain and its wallet; no node was used, so none is remembered")
	void aTransfer_remembersTheChainAndTheWallet()
	{
		LethenonSendPanel panel = new LethenonSendPanel();
		panel.getModelObject().setChainFile(chainFile.toString());
		panel.getModelObject().setWalletFile(walletFile.toString());
		panel.getModelObject().setPassword(PASSWORD.toCharArray());
		panel.getModelObject().setRecipient(TransactionSigner
			.asBytes(TransactionSigner.newKeyPair(SignatureSuite.ED25519).getPublic()).toString());
		panel.getModelObject().setAmount("1");

		panel.onSend();

		assertEquals("the transfer was signed and waits for the next block",
			panel.getModelObject().getResultText());
		assertEquals(chainFile.toString(), LethenonSettingsContribution.chainFile());
		assertEquals(walletFile.toString(), LethenonSettingsContribution.walletFile());
		assertEquals("", LethenonSettingsContribution.node());
	}

	@Test
	@DisplayName("a verified chain leaves its chain file in the settings")
	void aVerifiedChain_remembersTheChain()
	{
		LethenonChainPanel panel = new LethenonChainPanel();
		panel.getModelObject().setChainFile(chainFile.toString());

		panel.onVerify();

		assertEquals(chainFile.toString(), LethenonSettingsContribution.chainFile());
	}

	@Test
	@DisplayName("a sync through a node and a SOCKS proxy leaves the chain, the node and the proxy")
	void aSync_remembersTheChainTheNodeAndTheProxy() throws Exception
	{
		List<BlockBody> served = LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 3,
			Bytes.of(new byte[] { 9 }));
		Path synced = directory.resolve("synced.lethenon");
		try (Node node = Node.on(served); Socks5Relay proxy = new Socks5Relay())
		{
			String nodeAddress = "127.0.0.1:" + node.listen(0);
			String proxyAddress = "127.0.0.1:" + proxy.port();
			LethenonSyncPanel panel = new LethenonSyncPanel();
			panel.getModelObject().setChainFile(synced.toString());
			panel.getModelObject().setNode(nodeAddress);
			panel.getModelObject().setProxy(proxyAddress);

			panel.onSync();
			await("the sync has ended", () -> !panel.getModelObject().isSynchronising());

			assertEquals("the chain file is at the tip of the node now",
				panel.getModelObject().getResultText());
			assertTrue(proxy.relayed() > 0, "the sync went through the proxy");
			assertEquals(synced.toString(), LethenonSettingsContribution.chainFile());
			assertEquals(nodeAddress, LethenonSettingsContribution.node());
			assertEquals(proxyAddress, LethenonSettingsContribution.proxy());
		}
	}

	@Test
	@DisplayName("a refused sync leaves the node and the proxy that worked before")
	void aRefusedSync_leavesTheRememberedValues() throws Exception
	{
		LethenonSettingsContribution.remember(Map.of(LethenonSettingsContribution.KEY_NODE,
			"203.0.113.5:18480", LethenonSettingsContribution.KEY_PROXY, "127.0.0.1:9050"));
		Path own = directory.resolve("own.lethenon");
		new ChainFile(own).write(
			LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 2, Bytes.of(new byte[] { 9 })));
		LethenonSyncPanel panel = new LethenonSyncPanel();
		panel.getModelObject().setChainFile(own.toString());
		panel.getModelObject().setNode("127.0.0.1:" + closedPort());
		panel.getModelObject().setProxy("");

		panel.onSync();
		await("the sync has ended", () -> !panel.getModelObject().isSynchronising());

		assertTrue(panel.getModelObject().getResultText().startsWith(
			"the chain file was not synchronised"), panel.getModelObject().getResultText());
		assertEquals("203.0.113.5:18480", LethenonSettingsContribution.node());
		assertEquals("127.0.0.1:9050", LethenonSettingsContribution.proxy());
		assertEquals("", LethenonSettingsContribution.chainFile());
	}

	@Test
	@DisplayName("a started node leaves its chain file; it did not mine, so no wallet is remembered")
	void aNode_remembersItsChain() throws Exception
	{
		Path served = directory.resolve("served.lethenon");
		new ChainFile(served).write(
			LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 3, Bytes.of(new byte[] { 9 })));
		LethenonNodePanel panel = new LethenonNodePanel();
		panel.getModelObject().setChainFile(served.toString());
		panel.getModelObject().setPort("0");
		try
		{
			panel.onStart();
			await("the node runs", () -> panel.getModelObject().isRunning());

			assertEquals(served.toString(), LethenonSettingsContribution.chainFile());
			assertEquals("", LethenonSettingsContribution.walletFile());
		}
		finally
		{
			panel.onStop();
			await("the node has stopped", () -> !panel.getModelObject().isRunning());
		}
	}

	@Test
	@DisplayName("a new window starts with what was remembered")
	void aNewWindow_startsWithTheRememberedValues()
	{
		LethenonSettingsContribution.remember(Map.of(
			LethenonSettingsContribution.KEY_CHAIN_FILE, "/tmp/chain.lethenon",
			LethenonSettingsContribution.KEY_WALLET_FILE, "/tmp/home.wallet",
			LethenonSettingsContribution.KEY_NODE, "203.0.113.5:18480",
			LethenonSettingsContribution.KEY_PROXY, "127.0.0.1:9050"));

		LethenonBalancePanelModel balance = new LethenonBalancePanel().getModelObject();
		LethenonMinePanelModel mine = new LethenonMinePanel().getModelObject();
		LethenonSendPanelModel send = new LethenonSendPanel().getModelObject();
		LethenonSweepPanelModel sweep = new LethenonSweepPanel().getModelObject();
		LethenonSyncPanelModel sync = new LethenonSyncPanel().getModelObject();
		LethenonNodePanelModel node = new LethenonNodePanel().getModelObject();

		assertEquals(List.of("/tmp/chain.lethenon", "/tmp/home.wallet"),
			List.of(balance.getChainFile(), balance.getWalletFile()));
		assertEquals(List.of("/tmp/chain.lethenon", "/tmp/home.wallet"),
			List.of(mine.getChainFile(), mine.getWalletFile()));
		assertEquals(List.of("/tmp/chain.lethenon", "/tmp/home.wallet", "203.0.113.5:18480"),
			List.of(send.getChainFile(), send.getWalletFile(), send.getNode()));
		assertEquals(List.of("/tmp/chain.lethenon", "/tmp/home.wallet"),
			List.of(sweep.getChainFile(), sweep.getWalletFile()));
		assertEquals(List.of("/tmp/chain.lethenon", "203.0.113.5:18480", "127.0.0.1:9050"),
			List.of(sync.getChainFile(), sync.getNode(), sync.getProxy()));
		assertEquals(List.of("/tmp/chain.lethenon", "/tmp/home.wallet"),
			List.of(node.getChainFile(), node.getWalletFile()));
	}

	@Test
	@DisplayName("the password is in none of the files the settings write")
	void thePassword_isInNoSettingsFile() throws Exception
	{
		LethenonBalancePanel balance = new LethenonBalancePanel();
		balance.getModelObject().setChainFile(chainFile.toString());
		balance.getModelObject().setWalletFile(walletFile.toString());
		balance.getModelObject().setPassword(PASSWORD.toCharArray());
		balance.onShow();
		LethenonMinePanel mine = new LethenonMinePanel();
		mine.getModelObject().setChainFile(chainFile.toString());
		mine.getModelObject().setWalletFile(walletFile.toString());
		mine.getModelObject().setPassword(PASSWORD.toCharArray());
		mine.onMine();

		List<Path> written;
		try (Stream<Path> files = Files.walk(configurationDirectory.toPath()))
		{
			written = files.filter(Files::isRegularFile).toList();
		}
		assertFalse(written.isEmpty(), "the settings were written, so there is something to read");
		for (Path file : written)
		{
			String content = Files.readString(file, StandardCharsets.ISO_8859_1);
			assertFalse(content.contains(PASSWORD), file + " holds the password");
			assertFalse(content.contains("correct"), file + " holds a part of the password");
		}
		assertTrue(Files.readString(written.getFirst(), StandardCharsets.ISO_8859_1)
			.contains(walletFile.getFileName().toString()), "what worked is what was written");
	}

	private static int closedPort() throws IOException
	{
		try (ServerSocket socket = new ServerSocket(0))
		{
			return socket.getLocalPort();
		}
	}

	private static void await(final String what, final BooleanSupplier condition)
		throws InterruptedException
	{
		long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
		while (!condition.getAsBoolean())
		{
			if (System.nanoTime() > deadline)
			{
				throw new AssertionError("waited thirty seconds for: " + what);
			}
			Thread.sleep(20L);
		}
	}

	/**
	 * The smallest SOCKS5 proxy a sync can go through: no authentication, CONNECT to an IPv4
	 * address or a host name, then bytes in both directions. It counts the connections it relayed.
	 */
	private static final class Socks5Relay implements AutoCloseable
	{

		private final ServerSocket server = new ServerSocket(0);

		private volatile int relayed;

		Socks5Relay() throws IOException
		{
			Thread.ofVirtual().start(this::accept);
		}

		int port()
		{
			return server.getLocalPort();
		}

		int relayed()
		{
			return relayed;
		}

		private void accept()
		{
			while (!server.isClosed())
			{
				try
				{
					Socket client = server.accept();
					Thread.ofVirtual().start(() -> relay(client));
				}
				catch (IOException closed)
				{
					return;
				}
			}
		}

		private void relay(final Socket client)
		{
			try (client)
			{
				DataInputStream in = new DataInputStream(client.getInputStream());
				OutputStream out = client.getOutputStream();
				in.readUnsignedByte();
				in.readFully(new byte[in.readUnsignedByte()]);
				out.write(new byte[] { 5, 0 });
				in.readUnsignedByte();
				in.readUnsignedByte();
				in.readUnsignedByte();
				int type = in.readUnsignedByte();
				String host;
				if (type == 1)
				{
					byte[] address = new byte[4];
					in.readFully(address);
					host = (address[0] & 0xff) + "." + (address[1] & 0xff) + "."
						+ (address[2] & 0xff) + "." + (address[3] & 0xff);
				}
				else
				{
					byte[] name = new byte[in.readUnsignedByte()];
					in.readFully(name);
					host = new String(name, StandardCharsets.US_ASCII);
				}
				int port = in.readUnsignedShort();
				try (Socket target = new Socket(host, port))
				{
					out.write(new byte[] { 5, 0, 0, 1, 0, 0, 0, 0, 0, 0 });
					relayed++;
					Thread back = Thread.ofVirtual()
						.start(() -> pipe(target, client));
					pipe(client, target);
					back.join();
				}
			}
			catch (IOException | InterruptedException ended)
			{
				// the connection ended, which is how every relayed connection ends
			}
		}

		private static void pipe(final Socket from, final Socket to)
		{
			try
			{
				InputStream in = from.getInputStream();
				in.transferTo(to.getOutputStream());
				to.shutdownOutput();
			}
			catch (IOException ended)
			{
				// one side closed
			}
		}

		@Override
		public void close() throws IOException
		{
			server.close();
		}
	}
}
