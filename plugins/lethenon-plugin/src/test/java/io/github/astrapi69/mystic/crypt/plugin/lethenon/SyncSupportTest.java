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
import io.github.astrapi69.lethenon.Chain;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.transport.Node;
import io.github.astrapi69.lethenon.transport.Sync;

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
			Sync.Synced synced = SyncSupport.sync(chainFile, local(node.listen(0)), "");

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
			Sync.Synced synced = SyncSupport.sync(chainFile, local(node.listen(0)), "  ");

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
			Sync.Synced synced = SyncSupport.sync(chainFile, local(node.listen(0)), null);

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
			Arguments.of("no node named", "refused.lethenon", " ", "", "no node was named"),
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
	@DisplayName("a main chain file is refused: nodes run on the test network only")
	void sync_refuses_aMainChainFile() throws Exception
	{
		Path chainFile = directory.resolve("main.lethenon");
		new ChainFile(chainFile).write(LethenonFixtures.aChainOf(Chain.IDENTIFIER, 2, miner));
		byte[] before = Files.readAllBytes(chainFile);

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> SyncSupport.sync(chainFile, local(aPortNobodyListensOn()), ""));

		assertTrue(refused.getMessage().contains(Chain.TEST_IDENTIFIER), refused.getMessage());
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
