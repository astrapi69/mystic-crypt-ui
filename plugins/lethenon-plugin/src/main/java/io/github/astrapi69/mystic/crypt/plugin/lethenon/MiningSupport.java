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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.Blocks;
import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.DifficultyRule;
import io.github.astrapi69.lethenon.Mining;
import io.github.astrapi69.lethenon.Replay;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.SignedTransaction;

/**
 * Mines the next block with a pun and writes it to the chain file, carrying the transfers that wait
 * next to it - what lethenon's {@code mine} does on the command line (lethenon#2, milestone 5).
 * <p>
 * No chain logic of its own: {@link Mining#nextBlock} sets the difficulty the rule requires and a
 * timestamp the median-time rule accepts, {@link Blocks#mine} varies the pun until the block meets
 * the difficulty, and the extended chain is replayed with {@link Replay#verify} BEFORE anything is
 * written. A chain that does not verify, a waiting transfer the chain would refuse, or a pun that
 * runs out of attempts leaves both files exactly as they were. Where there is no chain file yet,
 * the block mined is its genesis block, and it pays the miner.
 * <p>
 * The block pays the wallet's Ed25519 account, as on the command line. The password is a
 * {@code char[]} and is wiped before this method returns, whatever happens; it appears in no
 * message.
 */
public final class MiningSupport
{

	private MiningSupport()
	{
	}

	/**
	 * Mines the next block and writes it, emptying the waiting transfers into it
	 *
	 * @param order
	 *            what to mine
	 * @param password
	 *            the wallet file's password; overwritten with zeros before this method returns
	 * @param now
	 *            the current time, milliseconds since the epoch
	 * @return the mined block
	 * @throws IOException
	 *             when a file cannot be read or written
	 * @throws IllegalArgumentException
	 *             when the wallet file is missing or the password does not open it
	 * @throws IllegalStateException
	 *             when no variation of the pun met the difficulty within the attempts
	 * @throws io.github.astrapi69.lethenon.ChainRejected
	 *             when the chain, or the chain with the new block, does not verify
	 */
	public static MinedBlock mine(final MiningOrder order, final char[] password, final long now)
		throws IOException
	{
		try
		{
			Bytes beneficiary = LethenonWallets.open(order.walletFile(), password)
				.spendKey(SignatureSuite.ED25519);
			ChainFile chainFile = new ChainFile(order.chainFile());
			List<BlockBody> chain = chainFile.read();
			List<SignedTransaction> waiting = chain.isEmpty() ? List.of() : chainFile.readPending();
			if (!chain.isEmpty())
			{
				Replay.verify(chain);
			}
			BlockBody mined = Blocks
				.mine(Mining.nextBlock(chain, beneficiary, waiting, order.pun(), now),
					order.attempts())
				.orElseThrow(() -> new IllegalStateException("no variation of the pun reached "
					+ "difficulty " + DifficultyRule.requiredFor(chain) + " within "
					+ order.attempts() + " attempts; mine again, or with other words"));
			List<BlockBody> extended = new ArrayList<>(chain);
			extended.add(mined);
			Replay replay = Replay.verify(extended);
			chainFile.write(extended);
			chainFile.writePending(List.of());
			return new MinedBlock(mined.height(), mined.pun(), waiting.size(),
				beneficiary.toString(), replay.describe());
		}
		finally
		{
			Arrays.fill(password, '\0');
		}
	}
}
