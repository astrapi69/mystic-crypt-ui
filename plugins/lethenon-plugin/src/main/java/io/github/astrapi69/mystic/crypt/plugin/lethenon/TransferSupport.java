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
import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.Destination;
import io.github.astrapi69.lethenon.OneTimeAddresses;
import io.github.astrapi69.lethenon.PublishedAddress;
import io.github.astrapi69.lethenon.SignedTransaction;
import io.github.astrapi69.lethenon.Transfers;
import io.github.astrapi69.lethenon.Wallet;

/**
 * Signs a transfer to an account key or to a published address and leaves it waiting for the next
 * block, next to the chain file in {@code <chain>.pending} - what lethenon's {@code send --to} and
 * {@code send --to-address} do on the command line (lethenon#2, milestone 5).
 * <p>
 * A published address is paid at a one-time destination: {@link OneTimeAddresses#destinationFor}
 * derives it from the address and a key pair made for this payment alone, so two payments to one
 * address land on keys with nothing visibly in common. The destination is never handed back to the
 * caller: {@link SentTransfer} names the address the person typed, because the sender's own screen
 * is a place where the link between the two would be written down.
 * <p>
 * No chain logic of its own: the chain is read and replayed the way every tool of this plugin reads
 * it, and {@link Transfers#prepare} takes the nonce and the balance from the replayed chain and the
 * transfers already waiting, so a second transfer before the next block gets the next nonce and
 * cannot spend the same money twice. The waiting transfers are read and written with
 * {@link ChainFile}, the chain library's own format for them. A transfer the account cannot cover,
 * or one the chain could not take, is refused before anything is written.
 * <p>
 * The password is a {@code char[]} and is wiped before this method returns, whatever happens; it
 * appears in no message.
 */
public final class TransferSupport
{

	private TransferSupport()
	{
	}

	/**
	 * Signs a transfer and appends it to the transfers waiting for the next block
	 *
	 * @param order
	 *            what the transfer is to be
	 * @param password
	 *            the wallet file's password; overwritten with zeros before this method returns
	 * @return the signed transfer
	 * @throws IOException
	 *             when a file cannot be read or written
	 * @throws IllegalArgumentException
	 *             when a file is missing, the password does not open the wallet, an input is not
	 *             one the chain can take, or the account does not cover the amount and the fee
	 * @throws io.github.astrapi69.lethenon.ChainRejected
	 *             when the chain does not verify
	 */
	public static SentTransfer send(final TransferOrder order, final char[] password)
		throws IOException
	{
		try
		{
			Destination recipient = destinationOf(order.recipientKind(), order.recipient());
			Amount amount = Amount.parseLeth(order.amount().trim());
			Amount fee = order.fee().isBlank() ? Amount.ZERO : Amount.parseLeth(order.fee().trim());
			Wallet sender = LethenonWallets.open(order.walletFile(), password);
			List<BlockBody> chain = ChainReplaySupport.replayed(order.chainFile()).chain();
			ChainFile chainFile = new ChainFile(order.chainFile());
			List<SignedTransaction> waiting = new ArrayList<>(chainFile.readPending());
			SignedTransaction signed = Transfers.prepare(sender, order.suite(), chain, waiting,
				recipient, amount, fee, order.memo());
			waiting.add(signed);
			chainFile.writePending(waiting);
			return new SentTransfer(signed.body().nonce(), amount, order.recipientKind(),
				order.recipient().trim(), waiting.size());
		}
		finally
		{
			Arrays.fill(password, '\0');
		}
	}

	private static Destination destinationOf(final RecipientKind kind, final String recipient)
	{
		return switch (kind)
		{
			case ACCOUNT_KEY -> Destination.direct(recipientOf(recipient));
			case PUBLISHED_ADDRESS -> oneTimeDestinationOf(recipient);
		};
	}

	/**
	 * Derives the one-time destination of a published address. Only the shape is checked by
	 * {@link PublishedAddress#parse}; whether the halves are keys is answered by the derivation, so
	 * both refusals are caught here and name what was typed
	 */
	private static Destination oneTimeDestinationOf(final String recipient)
	{
		if (recipient == null || recipient.isBlank())
		{
			throw new IllegalArgumentException("no recipient was named: enter the published "
				+ "address, its view key and its spend key in hexadecimal separated by '"
				+ PublishedAddress.SEPARATOR + "', as lethenon prints it after 'address (publish "
				+ "this):'");
		}
		try
		{
			return OneTimeAddresses.destinationFor(PublishedAddress.parse(recipient.trim()),
				OneTimeAddresses.newEphemeralKeyPair());
		}
		catch (IllegalArgumentException notAnAddress)
		{
			throw new IllegalArgumentException("the recipient '" + recipient.trim()
				+ "' is not a published address: " + notAnAddress.getMessage(), notAnAddress);
		}
	}

	private static Bytes recipientOf(final String recipient)
	{
		if (recipient == null || recipient.isBlank())
		{
			throw new IllegalArgumentException("no recipient was named: enter the recipient's "
				+ "account key in hexadecimal, as every lethenon tool prints it");
		}
		try
		{
			return Bytes.ofHex(recipient.trim());
		}
		catch (IllegalArgumentException notHex)
		{
			throw new IllegalArgumentException("the recipient '" + recipient.trim()
				+ "' is not an account key in hexadecimal: " + notHex.getMessage(), notHex);
		}
	}
}
