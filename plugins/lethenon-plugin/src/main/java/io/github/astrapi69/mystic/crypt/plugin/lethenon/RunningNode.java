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

import io.github.astrapi69.lethenon.transport.Miner;
import io.github.astrapi69.lethenon.transport.Node;

/**
 * A node of the chain library that {@link NodeSupport} started, with its miner when it mines.
 * Closing it stops the miner after the round in progress, then the node, which stops listening and
 * closes its connections; the chain file and the pool next to it hold what the node adopted.
 */
public final class RunningNode implements AutoCloseable
{

	private final Node node;

	/** The miner, or null when the node does not mine */
	private final Miner miner;

	private final int port;

	RunningNode(final Node node, final Miner miner, final int port)
	{
		this.node = node;
		this.miner = miner;
		this.port = port;
	}

	/**
	 * The port the node listens on, the one that was free when port 0 was asked for
	 *
	 * @return the TCP port
	 */
	public int port()
	{
		return port;
	}

	/**
	 * What the node holds now; every part is a snapshot the node hands out, so this can be called
	 * from any thread while the node runs
	 *
	 * @return the status
	 */
	public NodeStatus status()
	{
		return new NodeStatus(port, node.chain().size() - 1L, node.peers().size(),
			node.pending().size(), miner == null ? 0L : miner.minedBlocks(), node.refusals());
	}

	@Override
	public void close()
	{
		try
		{
			if (miner != null)
			{
				miner.close();
			}
		}
		finally
		{
			node.close();
		}
	}
}
