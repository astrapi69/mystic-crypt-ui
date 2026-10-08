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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.Chain;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.Destination;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.SignedTransaction;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;
import io.github.astrapi69.lethenon.transport.Node;

/**
 * Sending through a node (#530 step 3), what lethenon's {@code send --node} does: the transfer is
 * handed to a running node that serves the chain file, instead of being written next to it, and the
 * node's pool is read back afterwards, because only the pool says whether the node admitted it. The
 * nodes here are the chain library's own, on this machine.
 */
class TransferThroughANodeTest
{

	private static final String PASSWORD = "correct horse battery staple";

	@TempDir
	Path directory;

	private Path chainFile;

	private Path walletFile;

	private Bytes recipient;

	@BeforeEach
	void writeATestChainThatPaysTheWallet() throws Exception
	{
		Wallet wallet = Wallet.create();
		walletFile = directory.resolve("wallet.lethenon-wallet");
		WalletFile.write(walletFile, wallet, PASSWORD.toCharArray());
		chainFile = directory.resolve("chain.lethenon");
		new ChainFile(chainFile).write(LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 2,
			wallet.spendKey(SignatureSuite.ED25519)));
		recipient = TransactionSigner
			.asBytes(TransactionSigner.newKeyPair(SignatureSuite.ED25519).getPublic());
	}

	@Test
	@DisplayName("a transfer handed to the node that serves the chain file waits in that node's pool")
	void send_handsTheTransfer_toTheNodeServingTheChainFile() throws Exception
	{
		try (Node node = Node.serving(new ChainFile(chainFile)))
		{
			String address = "127.0.0.1:" + node.listen(0);

			SentTransfer sent = TransferSupport.send(order(address), PASSWORD.toCharArray());

			assertEquals(address, sent.node());
			assertEquals(1, sent.waiting());
			List<SignedTransaction> pool = node.pending();
			assertEquals(1, pool.size(), "the node admitted the transfer");
			assertEquals(Destination.direct(recipient), pool.getFirst().body().recipient());
			assertEquals(pool, new ChainFile(chainFile).readPending(),
				"the node keeps its pool next to the chain file it serves, where every tool reads it");
		}
	}

	@Test
	@DisplayName("a node that cannot be reached is named, and nothing is left waiting")
	void send_throughANodeThatCannotBeReached_leavesNothingWaiting() throws Exception
	{
		String address = "127.0.0.1:" + aPortNobodyListensOn();

		IOException refused = assertThrows(IOException.class,
			() -> TransferSupport.send(order(address), PASSWORD.toCharArray()));

		assertTrue(refused.getMessage().contains(address), refused.getMessage());
		assertTrue(new ChainFile(chainFile).readPending().isEmpty(),
			"a transfer for a node is never written next to the chain file instead");
	}

	@Test
	@DisplayName("a node that serves another chain file takes the transfer there, and the refusal says where to look")
	void send_throughANodeOfAnotherChainFile_saysWhereTheTransferWent() throws Exception
	{
		Path otherFile = directory.resolve("other.lethenon");
		Files.copy(chainFile, otherFile);
		try (Node node = Node.serving(new ChainFile(otherFile)))
		{
			String address = "127.0.0.1:" + node.listen(0);

			IllegalStateException refused = assertThrows(IllegalStateException.class,
				() -> TransferSupport.send(order(address), PASSWORD.toCharArray()));

			assertTrue(refused.getMessage().contains(chainFile + ".pending"), refused.getMessage());
			assertEquals(1, node.pending().size(), "the other node did admit it");
		}
	}

	@Test
	@DisplayName("a node that is not host:port is refused, and the password is wiped all the same")
	void send_refuses_aNodeThatIsNotAnAddress()
	{
		char[] password = PASSWORD.toCharArray();

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> TransferSupport.send(order("localhost"), password));

		assertTrue(refused.getMessage().contains("a peer is host:port"), refused.getMessage());
		assertArrayEquals(new char[PASSWORD.length()], password);
	}

	private TransferOrder order(final String node)
	{
		return new TransferOrder(chainFile, walletFile, SignatureSuite.ED25519,
			RecipientKind.ACCOUNT_KEY, recipient.toString(), "3", "0", "through the node", node);
	}

	private static int aPortNobodyListensOn() throws IOException
	{
		try (ServerSocket socket = new ServerSocket(0))
		{
			return socket.getLocalPort();
		}
	}
}
