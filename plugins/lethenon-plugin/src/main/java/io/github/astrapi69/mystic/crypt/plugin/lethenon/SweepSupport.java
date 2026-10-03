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

import io.github.astrapi69.lethenon.Amount;
import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.SignedTransaction;
import io.github.astrapi69.lethenon.Sweeps;
import io.github.astrapi69.lethenon.Wallet;

/**
 * Moves what was paid to a wallet's one-time destinations onto its own Ed25519 account and leaves
 * the transfers waiting for the next block, next to the chain file in {@code <chain>.pending} -
 * what lethenon's {@code sweep} does on the command line (lethenon#37).
 * <p>
 * No sweeping loop of its own: {@link Sweeps#prepare} recognises the destinations, leaves out the
 * ones already emptied by a waiting transfer and the ones that hold no more than the fee, and signs
 * one transfer per destination with that destination's own nonce and one-time key. This class reads
 * the chain the way every tool of this plugin reads it and writes what came back with
 * {@link ChainFile}, the chain library's own format for waiting transfers. When there is nothing to
 * sweep, nothing is written.
 * <p>
 * What sweeping costs is not this class's to decide, and the window that calls it says it before
 * anything is signed: each transfer names a destination as its sender and the account as its
 * recipient, so whoever reads the chain afterwards knows they belong together. Receiving was
 * unlinkable; spending is the moment that ends.
 * <p>
 * The password is a {@code char[]} and is wiped before this method returns, whatever happens; it
 * appears in no message.
 */
public final class SweepSupport
{

	private SweepSupport()
	{
	}

	/**
	 * Signs one transfer per one-time destination worth sweeping and appends them to the transfers
	 * waiting for the next block
	 *
	 * @param order
	 *            what the sweep is to be
	 * @param password
	 *            the wallet file's password; overwritten with zeros before this method returns
	 * @return what was signed; zero transfers when there was nothing worth sweeping
	 * @throws IOException
	 *             when a file cannot be read or written
	 * @throws IllegalArgumentException
	 *             when a file is missing, the password does not open the wallet, or the fee is not
	 *             one the chain can take
	 * @throws io.github.astrapi69.lethenon.ChainRejected
	 *             when the chain does not verify
	 */
	public static SweptPayments sweep(final SweepOrder order, final char[] password)
		throws IOException
	{
		try
		{
			Amount fee = order.fee().isBlank() ? Amount.ZERO : Amount.parseLeth(order.fee().trim());
			Wallet wallet = LethenonWallets.open(order.walletFile(), password);
			List<BlockBody> chain = ChainReplaySupport.replayed(order.chainFile()).chain();
			ChainFile chainFile = new ChainFile(order.chainFile());
			List<SignedTransaction> waiting = new ArrayList<>(chainFile.readPending());
			List<SignedTransaction> sweep = Sweeps.prepare(wallet, chain, waiting, fee,
				order.memo());
			String account = wallet.spendKey(SignatureSuite.ED25519).toString();
			if (sweep.isEmpty())
			{
				return new SweptPayments(0, Amount.ZERO, account, waiting.size());
			}
			waiting.addAll(sweep);
			chainFile.writePending(waiting);
			return new SweptPayments(sweep.size(), totalOf(sweep), account, waiting.size());
		}
		finally
		{
			Arrays.fill(password, '\0');
		}
	}

	private static Amount totalOf(final List<SignedTransaction> sweep)
	{
		Amount total = Amount.ZERO;
		for (SignedTransaction transfer : sweep)
		{
			total = total.plus(transfer.body().amount());
		}
		return total;
	}
}
