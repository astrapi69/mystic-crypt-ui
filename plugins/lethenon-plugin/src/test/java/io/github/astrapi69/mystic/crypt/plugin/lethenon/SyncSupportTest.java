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
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.CanonicalEncoding;
import io.github.astrapi69.lethenon.Chain;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.transport.Node;
import io.github.astrapi69.lethenon.transport.PeerAddress;
import io.github.astrapi69.lethenon.transport.Sync;
import io.github.astrapi69.lethenon.transport.TestNetworkSeeds;

/**
 * The sync behind the plugin's "Synchronise with a Node" window, against a real node of the chain
 * library on this machine rather than a mock: what the file takes is what the library's replay
 * verified, the file is written only when the chain grew, and what cannot be a sync is refused
 * before anything is sent.
 */
class SyncSupportTest
{

	/** A Tor version 3 onion address: 56 base32 characters before ".onion" */
	private static final String ONION = "a".repeat(56) + ".onion:18480";

	private final Bytes miner = Bytes.of(new byte[] { 9 });

	@TempDir
	Path directory;

	@Test
	@DisplayName("a chain file that does not exist yet takes the node's whole chain, genesis block included")
	void sync_takesTheNodesWholeChain_intoANewFile() throws Exception
	{
		List<BlockBody> served = LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 4, miner);
		Path chainFile = directory.resolve("new.lethenon");
		try (Node node = Node.on(served))
		{
			Sync.Synced synced = SyncSupport.sync(chainFile, local(node.listen(0)), "").synced();

			assertEquals(served, new ChainFile(chainFile).require());
			assertEquals(0L, synced.blocksBefore());
			assertEquals(4L, synced.blocksAfter());
			assertEquals(Chain.TEST_IDENTIFIER, synced.chainIdentifier());
		}
	}

	@Test
	@DisplayName("a chain file behind the node ends at the node's tip")
	void sync_bringsAFileBehind_upToTheTip() throws Exception
	{
		List<BlockBody> served = LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 5, miner);
		Path chainFile = directory.resolve("behind.lethenon");
		new ChainFile(chainFile).write(served.subList(0, 2));
		try (Node node = Node.on(served))
		{
			Sync.Synced synced = SyncSupport.sync(chainFile, local(node.listen(0)), "  ").synced();

			assertEquals(served, new ChainFile(chainFile).require());
			assertEquals(3L, synced.taken());
		}
	}

	@Test
	@DisplayName("a chain file at the node's tip is left exactly as it was")
	void sync_leavesAFileAtTheTip_asItWas() throws Exception
	{
		List<BlockBody> served = LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 3, miner);
		Path chainFile = directory.resolve("current.lethenon");
		new ChainFile(chainFile).write(served);
		byte[] before = Files.readAllBytes(chainFile);
		try (Node node = Node.on(served))
		{
			Sync.Synced synced = SyncSupport.sync(chainFile, local(node.listen(0)), null).synced();

			assertEquals(0L, synced.taken());
			assertArrayEquals(before, Files.readAllBytes(chainFile));
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("refusedBeforeAnythingIsSent")
	@DisplayName("what cannot be a sync is refused before anything is sent, saying what is missing")
	void sync_refuses_beforeAnythingIsSent(final String what, final String chainFileName,
		final String node, final String proxy, final String expected)
	{
		Path chainFile = chainFileName.isEmpty() ? Path.of("") : directory.resolve(chainFileName);

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> SyncSupport.sync(chainFile, node, proxy));

		assertTrue(refused.getMessage().contains(expected), refused.getMessage());
		assertFalse(Files.exists(directory.resolve("refused.lethenon")), "nothing was written");
	}

	static Stream<Arguments> refusedBeforeAnythingIsSent()
	{
		return Stream.of(
			Arguments.of("no chain file named", "", "127.0.0.1:18480", "",
				"no chain file was named"),
			Arguments.of("a node without a port", "refused.lethenon", "127.0.0.1", "",
				"a peer is host:port"),
			Arguments.of("a port above 65535", "refused.lethenon", "127.0.0.1:70000", "",
				"a port is 1 to 65535"),
			Arguments.of("an onion address without a proxy", "refused.lethenon", ONION, "",
				"reached only through Tor"),
			Arguments.of("a proxy that is not host:port", "refused.lethenon", ONION, "localhost",
				"the SOCKS proxy is host:port"));
	}

	@Test
	@DisplayName("a main chain file is refused with its name: nodes run on the test network only")
	void sync_refuses_aMainChainFile() throws Exception
	{
		Path chainFile = directory.resolve("main.lethenon");
		new ChainFile(chainFile).write(LethenonFixtures.aChainOf(Chain.IDENTIFIER, 2, miner));
		byte[] before = Files.readAllBytes(chainFile);

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> SyncSupport.sync(chainFile, local(aPortNobodyListensOn()), ""));

		assertTrue(refused.getMessage().startsWith(chainFile.toAbsolutePath().toString()),
			refused.getMessage());
		assertTrue(refused.getMessage().contains(Chain.TEST_IDENTIFIER), refused.getMessage());
		assertArrayEquals(before, Files.readAllBytes(chainFile));
	}

	@Test
	@DisplayName("a chain file an earlier lethenon wrote is refused with the file's name, before anything is sent, and stays as it was (#535)")
	void sync_refuses_aRetiredChain_namingTheFile() throws Exception
	{
		Path chainFile = directory.resolve("old.lethenon");
		Files.write(chainFile,
			CanonicalEncoding.encodeChain(LethenonFixtures.aChainUnderLethenonOne()));
		byte[] before = Files.readAllBytes(chainFile);

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> SyncSupport.sync(chainFile, local(aPortNobodyListensOn()), ""));

		assertTrue(refused.getMessage().startsWith(chainFile.toAbsolutePath().toString()),
			refused.getMessage());
		assertTrue(refused.getMessage().contains("was started under the rules before lethenon 0.4.0"),
			refused.getMessage());
		assertArrayEquals(before, Files.readAllBytes(chainFile));
	}

	@Test
	@DisplayName("a node that cannot be reached is named in the refusal, and nothing is written")
	void sync_refuses_aNodeThatCannotBeReached() throws Exception
	{
		Path chainFile = directory.resolve("unreached.lethenon");
		String node = local(aPortNobodyListensOn());

		IOException refused = assertThrows(IOException.class,
			() -> SyncSupport.sync(chainFile, node, ""));

		// the library's own sentence, which differs with the step that failed - for a new file the
		// first step is asking for the genesis block - and names the node either way
		assertTrue(refused.getMessage().contains(node), refused.getMessage());
		assertFalse(Files.exists(chainFile), "a sync that fails writes nothing");
	}

	@Test
	@DisplayName("no node named: a new chain file takes the chain from the first seed node that answers, and the report names it (#573)")
	void sync_withoutANode_takesTheChainFromTheFirstSeedThatAnswers() throws Exception
	{
		List<BlockBody> served = LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 3, miner);
		Path chainFile = directory.resolve("seeded.lethenon");
		try (Node seed = Node.on(served))
		{
			PeerAddress silent = new PeerAddress("127.0.0.1", aPortNobodyListensOn());
			PeerAddress answering = new PeerAddress("127.0.0.1", seed.listen(0));
			Seeds seeds = new Seeds(List.of(silent, answering));

			SyncSupport.SyncedFrom synced = SyncSupport.sync(chainFile, "  ", "", seeds);

			assertEquals(served, new ChainFile(chainFile).require());
			assertEquals(answering, synced.node());
			assertTrue(synced.seedNode());
			assertEquals(List.of(false), seeds.asked);
		}
	}

	@Test
	@DisplayName("no node named, a SOCKS proxy given: the seed nodes' onion addresses are asked for (#573)")
	void sync_withoutANode_throughAProxy_asksForTheOnionAddresses() throws Exception
	{
		Seeds seeds = new Seeds(List.of(new PeerAddress("127.0.0.1", aPortNobodyListensOn())));

		assertThrows(IOException.class, () -> SyncSupport.sync(directory.resolve("tor.lethenon"),
			"", local(aPortNobodyListensOn()), seeds));

		assertEquals(List.of(true), seeds.asked);
	}

	@Test
	@DisplayName("no node named and no seed node answers: every seed is named in the refusal, and nothing is written (#573)")
	void sync_withoutANode_noSeedAnswers_namesEverySeed() throws Exception
	{
		PeerAddress first = new PeerAddress("127.0.0.1", aPortNobodyListensOn());
		PeerAddress second = new PeerAddress("127.0.0.1", aPortNobodyListensOn());
		Path chainFile = directory.resolve("unseeded.lethenon");

		IOException refused = assertThrows(IOException.class,
			() -> SyncSupport.sync(chainFile, "", "", new Seeds(List.of(first, second))));

		assertTrue(refused.getMessage().contains("seed node"), refused.getMessage());
		assertTrue(refused.getMessage().contains(first.toString()), refused.getMessage());
		assertTrue(refused.getMessage().contains(second.toString()), refused.getMessage());
		assertFalse(Files.exists(chainFile), "a sync that fails writes nothing");
	}

	@Test
	@DisplayName("no node named on a chain of its own: refused before any seed node is dialled, and the file stays as it was (#573)")
	void sync_withoutANode_onAChainOfItsOwn_isRefusedBeforeAnyDial() throws Exception
	{
		Path chainFile = directory.resolve("own.lethenon");
		new ChainFile(chainFile).write(LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 2, miner));
		byte[] before = Files.readAllBytes(chainFile);

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> SyncSupport.sync(chainFile, "", "", SyncSupport.TEST_NETWORK_SEEDS));

		assertTrue(refused.getMessage().startsWith(chainFile.toAbsolutePath().toString()),
			refused.getMessage());
		assertTrue(refused.getMessage().contains("no node was named"), refused.getMessage());
		assertTrue(refused.getMessage().contains("seed nodes"), refused.getMessage());
		assertArrayEquals(before, Files.readAllBytes(chainFile));
	}

	@Test
	@DisplayName("the window's seed nodes are the library's: IP addresses directly, onion addresses through Tor (#573)")
	void theTestNetworkSeeds_areTheLibrarys()
	{
		assertEquals(TestNetworkSeeds.CLEARNET, SyncSupport.TEST_NETWORK_SEEDS.reachedThrough(false));
		assertEquals(TestNetworkSeeds.ONION, SyncSupport.TEST_NETWORK_SEEDS.reachedThrough(true));
		assertTrue(SyncSupport.TEST_NETWORK_SEEDS.serve(List.of()));
	}

	/** Seed nodes on this machine, serving whatever chain, which record what was asked of them */
	private static final class Seeds implements SyncSupport.SeedNodes
	{
		private final List<PeerAddress> addresses;

		private final List<Boolean> asked = new ArrayList<>();

		Seeds(final List<PeerAddress> addresses)
		{
			this.addresses = addresses;
		}

		@Override
		public boolean serve(final List<BlockBody> chain)
		{
			return true;
		}

		@Override
		public List<PeerAddress> reachedThrough(final boolean tor)
		{
			asked.add(tor);
			return addresses;
		}
	}

	private static String local(final int port)
	{
		return "127.0.0.1:" + port;
	}

	private static int aPortNobodyListensOn() throws IOException
	{
		try (ServerSocket socket = new ServerSocket(0))
		{
			return socket.getLocalPort();
		}
	}
}
