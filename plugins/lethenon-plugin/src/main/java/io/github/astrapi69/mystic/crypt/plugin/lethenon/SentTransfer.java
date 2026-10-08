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

import io.github.astrapi69.lethenon.Amount;

/**
 * A transfer that was signed and now waits for the next block
 *
 * @param nonce
 *            the nonce the sending account signed it with
 * @param amount
 *            what it carries
 * @param recipientKind
 *            whether the recipient was an account key or a published address
 * @param recipient
 *            the recipient as it was typed: the account key, or the published address. For an
 *            address this is deliberately NOT the one-time destination the transfer carries -
 *            nobody but the recipient is to connect the two, and the sender's own screen is a place
 *            where that link would be written down
 * @param waiting
 *            how many transfers wait for the next block now, this one included: next to the chain
 *            file, or in the pool of the node it was handed to
 * @param node
 *            host:port of the node the transfer was handed to, empty when it waits next to the
 *            chain file
 */
public record SentTransfer(long nonce, Amount amount, RecipientKind recipientKind, String recipient,
	int waiting, String node) {
}
