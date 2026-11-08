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
package io.github.astrapi69.mystic.crypt.ui;

import java.io.File;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import io.github.astrapi69.lethenon.Amount;
import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.Blocks;
import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.Chain;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.Destination;
import io.github.astrapi69.lethenon.Genesis;
import io.github.astrapi69.lethenon.Mining;
import io.github.astrapi69.lethenon.OneTimeAddresses;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.SignedTransaction;
import io.github.astrapi69.lethenon.TransactionBody;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;

/**
 * Test chains the end-to-end tests of the lethenon plugin share, built with the chain library
 * itself
 */
final class LethenonTestChains
{

	private LethenonTestChains()
	{
	}

	/**
	 * A test chain in the shape lethenon 0.4.0 accepts, on which a payer mines block 1 and then
	 * pays the wallet 3 LETH to its Ed25519 account and 5 LETH to a one-time destination of its
	 * published address in block 2, written as lethenon 0.6.0 keeps a chain, in a database
	 *
	 * @param wallet
	 *            the wallet that is paid
	 * @param directory
	 *            where the chain file is written
	 * @return the chain file, {@code paid.lethenon}
	 * @throws Exception
	 *             if the chain cannot be signed, mined or written
	 */
	static File payingTheWallet(final Wallet wallet, final File directory) throws Exception
	{
		KeyPair payer = TransactionSigner.newKeyPair(SignatureSuite.ED25519);
		Bytes payerKey = TransactionSigner.asBytes(payer.getPublic());
		List<BlockBody> chain = new ArrayList<>(List.of(Genesis.candidate(Chain.TEST_IDENTIFIER,
			"in the beginning was the pun", 1_759_000_000_000L)));
		chain.add(Blocks.mine(
			Mining.nextBlock(chain, payerKey, List.of(), "the payer's block", 1_759_000_120_000L),
			1_000_000L).orElseThrow());
		SignedTransaction direct = TransactionSigner
			.sign(
				new TransactionBody(Chain.TEST_IDENTIFIER, 0L, payerKey,
					Destination.direct(wallet.spendKey(SignatureSuite.ED25519)), Amount.ofLeth(3L),
					Amount.ZERO, "three, to the account"),
				SignatureSuite.ED25519, payer.getPrivate());
		SignedTransaction oneTime = TransactionSigner
			.sign(new TransactionBody(Chain.TEST_IDENTIFIER, 1L, payerKey,
				OneTimeAddresses.destinationFor(wallet.address(),
					OneTimeAddresses.newEphemeralKeyPair()),
				Amount.ofLeth(5L), Amount.ZERO, "five, to a one-time destination"),
				SignatureSuite.ED25519, payer.getPrivate());
		chain.add(Blocks.mine(Mining.nextBlock(chain, payerKey, List.of(direct, oneTime),
			"the second pun", 1_759_000_240_000L), 1_000_000L).orElseThrow());
		File chainFile = new File(directory, "paid.lethenon");
		new ChainFile(chainFile.toPath()).write(chain);
		return chainFile;
	}
}
