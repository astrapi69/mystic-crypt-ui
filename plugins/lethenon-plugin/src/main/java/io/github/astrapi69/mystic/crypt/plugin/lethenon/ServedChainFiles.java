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

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.github.astrapi69.lethenon.transport.PeerAddress;

/**
 * The chain files that nodes started in this program serve, each with the port its node listens on
 * (#814). A node reads the transfers waiting in its chain file once, when it starts, and replaces
 * that list with its own pool at every block it takes and every transfer it admits; a transfer
 * another tool writes into the file meanwhile is therefore never mined, and the node's next block
 * deletes it. A transfer for a file named here goes to its node instead, through the handover of
 * {@code send --node} (#530).
 * <p>
 * A node outside this program, such as one the command line started, is not known here: lethenon
 * offers no way to ask whether a node serves a file (lethenon#299).
 */
final class ServedChainFiles
{

	/** The host a node of this program is reached at: it runs on this machine */
	static final String HOST = "127.0.0.1";

	private static final Map<Path, Integer> PORTS = new ConcurrentHashMap<>();

	private ServedChainFiles()
	{
	}

	/**
	 * Notes that a node of this program serves a chain file
	 *
	 * @param chainFile
	 *            the chain file the node serves
	 * @param port
	 *            the port it listens on
	 */
	static void serve(final Path chainFile, final int port)
	{
		PORTS.put(keyOf(chainFile), port);
	}

	/**
	 * Forgets a node that stopped; a node started since on the same file, on another port, stays
	 *
	 * @param chainFile
	 *            the chain file the node served
	 * @param port
	 *            the port it listened on
	 */
	static void release(final Path chainFile, final int port)
	{
		PORTS.remove(keyOf(chainFile), port);
	}

	/**
	 * The node of this program that serves a chain file
	 *
	 * @param chainFile
	 *            the chain file, as typed; it is compared as an absolute, normalized path
	 * @return the node's address, or empty when no node of this program serves the file
	 */
	static Optional<PeerAddress> nodeFor(final Path chainFile)
	{
		return Optional.ofNullable(PORTS.get(keyOf(chainFile)))
			.map(port -> new PeerAddress(HOST, port));
	}

	private static Path keyOf(final Path chainFile)
	{
		return chainFile.toAbsolutePath().normalize();
	}
}
