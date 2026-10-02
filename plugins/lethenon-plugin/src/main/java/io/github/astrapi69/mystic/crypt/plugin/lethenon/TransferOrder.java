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

import io.github.astrapi69.lethenon.SignatureSuite;

/**
 * What a transfer is to be, as a person typed it into the send window: everything except the
 * wallet's password, which travels apart as a {@code char[]} so it can be wiped
 *
 * @param chainFile
 *            the chain file; the transfer waits next to it, in {@code <chain>.pending}
 * @param walletFile
 *            the wallet file that signs
 * @param suite
 *            the signature suite of the sending account
 * @param recipient
 *            the recipient's account key in hexadecimal, as every lethenon tool prints it
 * @param amount
 *            the amount in LETH, up to eight decimals, e.g. {@code 12.5}
 * @param fee
 *            the fee in LETH; blank means none
 * @param memo
 *            the text signed with the transfer; may be empty
 */
public record TransferOrder(Path chainFile, Path walletFile, SignatureSuite suite, String recipient,
	String amount, String fee, String memo) {
}
