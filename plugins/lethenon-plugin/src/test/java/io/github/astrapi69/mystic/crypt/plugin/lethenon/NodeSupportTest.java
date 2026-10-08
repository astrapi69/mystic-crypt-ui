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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.Chain;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;
import io.github.astrapi69.lethenon.transport.Node;
import io.github.astrapi69.lethenon.transport.PeerAddress;
import io.github.astrapi69.lethenon.transport.Sync;

/**
 * The node behind the plugin's "Run a Node" window (#530 step 2), driven against the chain
 * library's own nodes on this machine: it serves its chain file, mines for a wallet when asked,
 * connects the configured peers, records the ones it cannot reach, and stops listening when it is
 * stopped. What cannot be a node is refused before anything listens.
 */
class NodeSupportTest
{

	private static final String PASSWORD = "correct horse battery staple";

	@TempDir
	Path directory;

	private Path chainFile;

	private List<BlockBody> chain;

	@BeforeEach
	void writeATestChain() throws IOException
	{
		chain = LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 3, Bytes.of(new byte[] { 9 }));
		chainFile = directory.resolve("chain.lethenon");
		new ChainFile(chainFile).write(chain);
	}

	@Test
	@DisplayName("a started node serves its chain file: another node takes the whole chain from it")
	void start_servesTheChainFile() throws Exception
	{
		try (RunningNode node = NodeSupport.start(order("0", ""), null))
		{
			assertTrue(node.port() > 0, "port 0 is any free one, and the node says which");
			assertEquals(2L, node.status().height());
			Path copy = directory.resolve("copy.lethenon");

			Sync.once(new ChainFile(copy), new PeerAddress("127.0.0.1", node.port()),
				Duration.ofSeconds(20));

			assertEquals(chain, new ChainFile(copy).require());
		}
	}

	@Test
	@DisplayName("a stopped node no longer listens")
	void close_stopsListening() throws Exception
	{
		RunningNode node = NodeSupport.start(order("0", ""), null);
		int port = node.port();

		node.close();

		assertThrows(IOException.class, () -> new Socket("127.0.0.1", port).close());
	}

	@Test
	@DisplayName("a mining node pays the wallet, writes its blocks to the chain file and counts them")
	void start_withMining_paysTheWallet() throws Exception
	{
		Wallet wallet = Wallet.create();
		Path walletFile = directory.resolve("miner.lethenon-wallet");
		WalletFile.write(walletFile, wallet, PASSWORD.toCharArray());
		char[] password = PASSWORD.toCharArray();

		try (RunningNode node = NodeSupport.start(
			new NodeOrder(chainFile, "0", "", true, walletFile, "a pun for the test"), password))
		{
			assertArrayEquals(new char[PASSWORD.length()], password,
				"the password is wiped once the wallet is open");
			await("a block is mined", () -> node.status().minedBlocks() >= 1);
		}

		List<BlockBody> written = new ChainFile(chainFile).require();
		assertTrue(written.size() > chain.size(), "the node writes what it adopts");
		assertEquals(wallet.spendKey(SignatureSuite.ED25519), written.getLast().beneficiary());
	}

	@Test
	@DisplayName("a configured peer is connected, and one that cannot be reached is among the refusals")
	void start_connectsThePeers_andRecordsTheOnesItCannotReach() throws Exception
	{
		int nobody = aPortNobodyListensOn();
		try (Node peer = Node.on(chain))
		{
			int peerPort = peer.listen(0);
			try (RunningNode node = NodeSupport
				.start(order("0", "127.0.0.1:" + peerPort + ", 127.0.0.1:" + nobody), null))
			{
				await("the peer is connected", () -> node.status().peers() == 1);
				List<String> refusals = node.status().refusals();
				assertTrue(refusals.stream().anyMatch(refusal -> refusal.contains(":" + nobody)),
					refusals.toString());
			}
		}
	}

	@Test
	@DisplayName("a port another program holds is refused, and nothing is left running")
	void start_refuses_aPortThatIsTaken() throws Exception
	{
		try (ServerSocket taken = new ServerSocket(0))
		{
			String port = String.valueOf(taken.getLocalPort());

			IOException refused = assertThrows(IOException.class,
				() -> NodeSupport.start(order(port, ""), null));

			assertTrue(refused.getMessage().contains(port), refused.getMessage());
		}
		try (RunningNode again = NodeSupport.start(order("0", ""), null))
		{
			assertEquals(2L, again.status().height(), "the chain file was left as it was");
		}
	}

	@Test
	@DisplayName("a main chain file is refused: nodes run on the test network only")
	void start_refuses_aMainChainFile() throws Exception
	{
		new ChainFile(chainFile)
			.write(LethenonFixtures.aChainOf(Chain.IDENTIFIER, 2, Bytes.of(new byte[] { 9 })));

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> NodeSupport.start(order("0", ""), null));

		assertTrue(refused.getMessage().contains(Chain.TEST_IDENTIFIER), refused.getMessage());
	}

	@Test
	@DisplayName("an empty chain file is not a chain a node can serve")
	void start_refuses_anEmptyChainFile() throws Exception
	{
		Files.write(chainFile, new byte[0]);

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> NodeSupport.start(order("0", ""), null));

		assertTrue(refused.getMessage().contains("is empty"), refused.getMessage());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("refusedBeforeAnythingListens")
	@DisplayName("what cannot be a node is refused before anything listens, saying what is missing")
	void start_refuses_beforeAnythingListens(final String what, final String chainFileName,
		final String port, final String peers, final boolean mine, final String expected)
	{
		Path named = chainFileName.isEmpty() ? Path.of("") : directory.resolve(chainFileName);
		char[] password = PASSWORD.toCharArray();

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> NodeSupport.start(new NodeOrder(named, port, peers, mine, Path.of(""), "pun"),
				password));

		assertTrue(refused.getMessage().contains(expected), refused.getMessage());
		assertArrayEquals(new char[PASSWORD.length()], password, "wiped all the same");
	}

	static Stream<Arguments> refusedBeforeAnythingListens()
	{
		return Stream.of(
			Arguments.of("no chain file named", "", "0", "", false, "no chain file was named"),
			Arguments.of("no chain file there", "missing.lethenon", "0", "", false,
				"there is no chain file at"),
			Arguments.of("a port that is not a number", "chain.lethenon", "eighteen", "", false,
				"the port is a number from 0 to 65535"),
			Arguments.of("a port above 65535", "chain.lethenon", "70000", "", false,
				"the port is a number from 0 to 65535"),
			Arguments.of("a peer that is not host:port", "chain.lethenon", "0", "localhost", false,
				"a peer is host:port"),
			Arguments.of("mining without a wallet", "chain.lethenon", "0", "", true,
				"no wallet file was named"));
	}

	private NodeOrder order(final String port, final String peers)
	{
		return new NodeOrder(chainFile, port, peers, false, Path.of(""), "pun");
	}

	private static int aPortNobodyListensOn() throws IOException
	{
		try (ServerSocket socket = new ServerSocket(0))
		{
			return socket.getLocalPort();
		}
	}

	private static void await(final String what, final BooleanSupplier condition)
		throws InterruptedException
	{
		long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
		while (!condition.getAsBoolean())
		{
			if (System.nanoTime() > deadline)
			{
				throw new AssertionError("waited twenty seconds for this, in vain: " + what);
			}
			Thread.sleep(20L);
		}
	}
}
