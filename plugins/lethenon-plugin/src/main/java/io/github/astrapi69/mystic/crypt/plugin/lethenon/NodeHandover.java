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
import java.nio.file.Path;
import java.util.List;

import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.SignedTransaction;
import io.github.astrapi69.lethenon.transport.Handover;
import io.github.astrapi69.lethenon.transport.PeerAddress;

/**
 * Hands signed transfers to a running node that serves the chain file, and reads its pool
 * afterwards: {@link Handover#send} returns once the node has handled the frame, and the pool next
 * to the chain file then says whether it was admitted - what lethenon's {@code send --node} does
 * (ADR 0003). The send window hands over this way (#530), and the sweep does for a node of this
 * program (#823).
 */
final class NodeHandover
{

	private NodeHandover()
	{
	}

	/**
	 * Hands the transfers over one by one and checks that the node's pool carries every one
	 *
	 * @param node
	 *            the running node
	 * @param chain
	 *            the chain the transfers were signed against
	 * @param chainPath
	 *            the chain file the node is to serve, next to which its pool is read
	 * @param signed
	 *            the transfers, at least one
	 * @return how many transfers wait in the node's pool now, these included
	 * @throws IOException
	 *             when the node cannot be reached or refuses the handshake
	 * @throws IllegalStateException
	 *             when the node took them and its pool next to the chain file does not carry them
	 *             all: the node serves another chain file, or refused a transfer
	 */
	static int handOver(final PeerAddress node, final List<BlockBody> chain, final Path chainPath,
		final List<SignedTransaction> signed) throws IOException
	{
		for (SignedTransaction transfer : signed)
		{
			Handover.send(node, chain, transfer);
		}
		List<SignedTransaction> pool = new ChainFile(chainPath).readPending();
		if (!pool.containsAll(signed))
		{
			String which = signed.size() == 1
				? "transfer, and it is not"
				: "transfers, and not all of them are";
			throw new IllegalStateException("the node at " + node + " took the " + which + " in "
				+ chainPath + ".pending: a node that serves another chain file keeps its "
				+ "pool next to that file, and a node that refused a transfer names the reason "
				+ "among its refusals");
		}
		return pool.size();
	}
}
