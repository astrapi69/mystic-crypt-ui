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
 * One block of an accepted chain, as the chain view shows it (lethenon#2, milestone 5).
 * <p>
 * Rows exist only for a chain that verified: {@link ChainReplaySupport#blocks} replays the whole
 * chain first and throws for a refused one, for the same reason {@link ChainReplayReport} does - a
 * block from a chain that did not verify is not a block anybody was paid by.
 *
 * @param height
 *            the block's height, the genesis block is zero
 * @param pun
 *            the pun the block was mined with
 * @param paidTo
 *            the account the block paid its reward to, as hexadecimal: at height zero the genesis
 *            holder, at every later height the miner (lethenon#23, lethenon#111)
 * @param transfers
 *            how many signed transfers the block carries
 * @param timestamp
 *            when the block was made, milliseconds since the epoch, as the block states it
 * @param difficulty
 *            how many leading zero bits the block's hash carries at least
 */
public record ChainBlockRow(long height, String pun, String paidTo, int transfers, long timestamp,
	int difficulty) {
}
