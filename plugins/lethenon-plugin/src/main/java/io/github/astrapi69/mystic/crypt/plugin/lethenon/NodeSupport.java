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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.transport.Miner;
import io.github.astrapi69.lethenon.transport.Node;
import io.github.astrapi69.lethenon.transport.PeerAddress;

/**
 * Starts a node of the chain library on a chain file (#530 step 2), what lethenon's {@code node}
 * command does without Tor: {@link Node#serving} serves the file and keeps the pool next to it,
 * listens on a port, connects the configured peers, and with mining on, a {@link Miner} mines on the
 * node's tip and pool for the wallet's Ed25519 account.
 * <p>
 * No network or chain logic of its own. What this class adds is what has to be decided before
 * anything listens: that the chain file holds a chain, which the plugin's other tools read the same
 * way ({@link ChainReplaySupport}), that the port and the peers are addresses, and, for mining, the
 * wallet. The password is a {@code char[]} and is wiped before this method returns, whatever
 * happens; it appears in no message.
 */
public final class NodeSupport
{

	private NodeSupport()
	{
	}

	/**
	 * Starts a node
	 *
	 * @param order
	 *            what the node is to be
	 * @param password
	 *            the wallet file's password when the node mines, otherwise ignored and may be null;
	 *            overwritten with zeros before this method returns
	 * @return the running node, which the caller closes
	 * @throws IOException
	 *             when the chain file cannot be read or the port cannot be opened; nothing then
	 *             runs
	 * @throws IllegalArgumentException
	 *             when no chain file holds a chain, the chain is not on the test network, the port
	 *             or a peer is not an address, or the wallet cannot be opened
	 * @throws io.github.astrapi69.lethenon.ChainRejected
	 *             when the chain does not verify
	 */
	public static RunningNode start(final NodeOrder order, final char[] password)
		throws IOException
	{
		try
		{
			ChainReplaySupport.replayed(order.chainFile());
			int port = portOf(order.port());
			List<PeerAddress> peers = peersOf(order.peers());
			Bytes beneficiary = order.mine()
				? LethenonWallets.open(order.walletFile(), password)
					.spendKey(SignatureSuite.ED25519)
				: null;
			return started(new ChainFile(order.chainFile()), port, peers, beneficiary,
				order.pun());
		}
		finally
		{
			wipe(password);
		}
	}

	/**
	 * Overwrites a password with zeros, for a caller that gives up before {@link #start} would
	 *
	 * @param password
	 *            the password; null is nothing to wipe
	 */
	static void wipe(final char[] password)
	{
		if (password != null)
		{
			Arrays.fill(password, '\0');
		}
	}

	/**
	 * Serves the file, listens, connects and mines; a node that fails on the way is closed again,
	 * so that nothing is left listening
	 */
	private static RunningNode started(final ChainFile chainFile, final int port,
		final List<PeerAddress> peers, final Bytes beneficiary, final String pun) throws IOException
	{
		Node node = Node.serving(chainFile);
		try
		{
			int listening = listen(node, port);
			node.connectAll(peers);
			Miner miner = beneficiary == null ? null : Miner.start(node, beneficiary, pun);
			return new RunningNode(node, miner, listening);
		}
		catch (IOException | RuntimeException failed)
		{
			node.close();
			throw failed;
		}
	}

	private static int listen(final Node node, final int port) throws IOException
	{
		try
		{
			return node.listen(port);
		}
		catch (IOException unbound)
		{
			throw new IOException("the node cannot listen on port " + port + ": "
				+ unbound.getMessage(), unbound);
		}
	}

	private static int portOf(final String text)
	{
		String form = "the port is a number from 0 to 65535, 0 for any free one, not '" + text
			+ "'";
		try
		{
			int port = Integer.parseInt(text.trim());
			if (port < 0 || port > 65535)
			{
				throw new IllegalArgumentException(form);
			}
			return port;
		}
		catch (NumberFormatException notANumber)
		{
			throw new IllegalArgumentException(form, notANumber);
		}
	}

	private static List<PeerAddress> peersOf(final String text)
	{
		if (text == null || text.isBlank())
		{
			return List.of();
		}
		return Arrays.stream(text.trim().split("[\\s,]+")).map(PeerAddress::parse).toList();
	}
}
