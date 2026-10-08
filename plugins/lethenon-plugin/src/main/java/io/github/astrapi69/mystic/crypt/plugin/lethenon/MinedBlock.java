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

/**
 * A block that was mined and written to the chain file
 *
 * @param chainIdentifier
 *            the chain it belongs to, e.g. {@code lethenon-test-1}
 * @param height
 *            its height; 0 for a genesis block
 * @param pun
 *            the pun that met the difficulty, the counter mining appended included
 * @param transfers
 *            how many waiting transfers it carries
 * @param beneficiary
 *            whom it pays, the miner's account key in hexadecimal
 * @param replaySummary
 *            the chain library's own sentence about the replay of the extended chain
 */
public record MinedBlock(String chainIdentifier, long height, String pun, int transfers,
	String beneficiary, String replaySummary) {
}
