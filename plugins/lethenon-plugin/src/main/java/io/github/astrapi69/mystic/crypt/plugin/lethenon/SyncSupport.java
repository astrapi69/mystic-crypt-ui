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
import java.time.Duration;

import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.transport.Outbound;
import io.github.astrapi69.lethenon.transport.PeerAddress;
import io.github.astrapi69.lethenon.transport.Sync;

/**
 * Brings a chain file up to one node's tip through the chain library's {@link Sync#once}, and stops
 * (#530, lethenon#107).
 * <p>
 * No network or chain logic of its own: the library runs a node in memory that neither listens nor
 * discovers peers, takes from the one node only blocks its own replay verified, and writes the file
 * once, at the end, only when the chain grew. A file that does not exist yet starts from the node's
 * genesis block - trust on first use, which the window says. What this class adds is what has to
 * be decided before anything is sent: that a file and a node were named, and how the connection
 * leaves this machine.
 * <p>
 * It takes a {@link Path} and text and returns the library's record, so it is testable without a
 * display (architecture.md layer 2: no Swing types in the support layer).
 */
public final class SyncSupport
{

	/** How long a whole sync may take: the command line's default for {@code sync --within} */
	static final Duration WITHIN = Duration.ofSeconds(300);

	private SyncSupport()
	{
	}

	/**
	 * Brings the chain file up to the tip of the node at the given address
	 *
	 * @param chainFile
	 *            the chain file, on the test network; one that does not exist yet starts from the
	 *            node's genesis block
	 * @param node
	 *            host:port of a running lethenon node
	 * @param proxy
	 *            host:port of a SOCKS proxy the connections go through, for Tor usually
	 *            127.0.0.1:9050; blank for a direct connection
	 * @return what the sync did
	 * @throws IOException
	 *             when the file cannot be read or written, or the node cannot be reached, refuses,
	 *             breaks off or does not hand over its tip in time; the file is then as it was
	 * @throws IllegalArgumentException
	 *             when no file or no node was named, an address is not host:port, an onion address
	 *             has no proxy, or the chain file is not on the test network
	 * @throws io.github.astrapi69.lethenon.ChainRejected
	 *             when the chain file does not verify
	 */
	public static Sync.Synced sync(final Path chainFile, final String node, final String proxy)
		throws IOException
	{
		if (chainFile == null || chainFile.toString().isBlank())
		{
			throw new IllegalArgumentException("no chain file was named: pick the file to bring up "
				+ "to the node's tip - one that does not exist yet starts from the node's genesis "
				+ "block");
		}
		if (node == null || node.isBlank())
		{
			throw new IllegalArgumentException("no node was named: give its host and port, for "
				+ "example 127.0.0.1:18480");
		}
		PeerAddress address = PeerAddress.parse(node.trim());
		Outbound outbound = outbound(address, proxy);
		return Sync.once(new ChainFile(chainFile), address, WITHIN, outbound);
	}

	/**
	 * How the connections leave this machine. The library refuses an onion address without a proxy
	 * as well, but in the words of its command line, naming an option this window does not have
	 * (lethenon#135), so the refusal is made here first.
	 */
	private static Outbound outbound(final PeerAddress address, final String proxy)
	{
		if (proxy == null || proxy.isBlank())
		{
			if (address.isOnion())
			{
				throw new IllegalArgumentException("the onion address " + address + " is reached "
					+ "only through Tor: give Tor's SOCKS proxy, usually 127.0.0.1:9050");
			}
			return Outbound.DIRECT;
		}
		try
		{
			return Outbound.through(PeerAddress.parse(proxy.trim()));
		}
		catch (IllegalArgumentException notAnAddress)
		{
			throw new IllegalArgumentException("the SOCKS proxy is host:port, for Tor usually "
				+ "127.0.0.1:9050, not '" + proxy.trim() + "'", notAnAddress);
		}
	}
}
