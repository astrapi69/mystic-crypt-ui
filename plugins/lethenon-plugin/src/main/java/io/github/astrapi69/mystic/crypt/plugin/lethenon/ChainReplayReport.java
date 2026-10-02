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
 * What a replay of a chain file verified: the counts and the chain's own one-line summary.
 * <p>
 * A report is only ever produced for a chain that was ACCEPTED. A chain that is refused does not
 * come back as a report with a flag in it - {@link ChainReplaySupport#verify} throws, and the panel
 * shows the reason - because a number read out of blocks that did not verify is the number a wallet
 * must never show.
 *
 * @param blocks
 *            how many blocks were replayed, genesis included
 * @param transactions
 *            how many transfers were applied
 * @param signatures
 *            how many signatures were checked
 * @param summary
 *            the chain library's own sentence about this replay, counts and supply included
 */
public record ChainReplayReport(long blocks, long transactions, long signatures, String summary)
{
}
