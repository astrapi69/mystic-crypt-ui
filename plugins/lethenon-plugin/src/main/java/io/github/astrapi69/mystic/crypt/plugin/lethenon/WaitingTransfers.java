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

import java.util.Collection;
import java.util.List;

import io.github.astrapi69.lethenon.Amount;
import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.SignedTransaction;

/**
 * The transfers of a wallet that wait for a block (#921): signed, handed to a node or written next
 * to the chain file, and not in a block yet. What they carry leaves the accounts with the next
 * block, fee included, the way lethenon's {@code Transfers} subtracts them before it signs the next
 * one.
 *
 * @param count
 *            how many transfers wait
 * @param amount
 *            what they take from the accounts, amounts and fees together
 */
record WaitingTransfers(int count, Amount amount) {

	/** No transfer waits */
	static final WaitingTransfers NONE = new WaitingTransfers(0, Amount.ZERO);

	/**
	 * The waiting transfers sent from the given accounts
	 *
	 * @param waiting
	 *            every transfer waiting for the next block, as {@code ChainFile.readPending} reads
	 *            them
	 * @param accounts
	 *            the wallet's accounts; a transfer from any other account is not counted
	 * @return the count and what they take, amount and fee
	 */
	static WaitingTransfers outgoing(final List<SignedTransaction> waiting,
		final Collection<Bytes> accounts)
	{
		int count = 0;
		Amount amount = Amount.ZERO;
		for (SignedTransaction transfer : waiting)
		{
			if (accounts.contains(transfer.body().sender()))
			{
				count++;
				amount = amount.plus(transfer.body().amount().plus(transfer.body().fee()));
			}
		}
		return new WaitingTransfers(count, amount);
	}
}
