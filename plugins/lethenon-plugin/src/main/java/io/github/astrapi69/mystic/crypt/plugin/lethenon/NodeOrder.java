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

/**
 * What a node is to be, as a person typed it into the node window: everything except the wallet's
 * password, which travels apart as a {@code char[]} so it can be wiped
 *
 * @param chainFile
 *            the chain file the node serves, on the test network; it belongs to the node while
 *            the node runs
 * @param port
 *            the TCP port the node listens on, as typed: 0 for any free one
 * @param peers
 *            host:port of the peers the node connects to, separated by spaces or commas; may be
 *            empty
 * @param mine
 *            whether the node mines
 * @param walletFile
 *            the wallet whose Ed25519 account the mined blocks pay; opened only when the node mines
 * @param pun
 *            the words mining varies
 */
public record NodeOrder(Path chainFile, String port, String peers, boolean mine, Path walletFile,
	String pun) {
}
