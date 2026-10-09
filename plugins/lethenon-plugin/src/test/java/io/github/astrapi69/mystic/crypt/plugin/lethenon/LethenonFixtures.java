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

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import io.github.astrapi69.lethenon.Amount;
import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.Blocks;
import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.Chain;
import io.github.astrapi69.lethenon.Destination;
import io.github.astrapi69.lethenon.DifficultyRule;
import io.github.astrapi69.lethenon.Genesis;
import io.github.astrapi69.lethenon.Mining;
import io.github.astrapi69.lethenon.OneTimeAddresses;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.SignedTransaction;
import io.github.astrapi69.lethenon.TransactionBody;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;

/**
 * Chains built with the chain library's own encoder and signer, for the tests of this plugin. A
 * committed fixture would freeze one version of the format and keep passing after it moved.
 */
final class LethenonFixtures
{

	/** When the chains of {@link #aChainOf} begin: in the past, so that no block is from the future */
	private static final long CHAIN_START = 1_759_000_000_000L;

	private LethenonFixtures()
	{
	}

	/**
	 * A chain mined the way a node mines it, every block from the chain library's own
	 * {@link Mining#nextBlock}, one target block time after the one before, so that the chain keeps
	 * the minimum difficulty
	 *
	 * @param chainIdentifier
	 *            the chain the genesis block starts; since lethenon 0.4.0 a chain under
	 *            {@link Chain#IDENTIFIER} is refused while the main chain has no anchor
	 *            (lethenon#161), so the tests that replay it use {@link Chain#TEST_IDENTIFIER}
	 * @param blocks
	 *            how many blocks, the genesis block included, at least one
	 * @param miner
	 *            the account every block pays
	 * @return the blocks, genesis first
	 */
	static List<BlockBody> aChainOf(final String chainIdentifier, final int blocks,
		final Bytes miner)
	{
		List<BlockBody> chain = new ArrayList<>();
		chain.add(Blocks.mine(Mining.nextBlock(chainIdentifier, List.of(), miner, List.of(),
			"in the beginning was the pun", CHAIN_START), 1_000_000L).orElseThrow());
		while (chain.size() < blocks)
		{
			chain.add(Blocks.mine(Mining.nextBlock(chain, miner, List.of(), "pun " + chain.size(),
				CHAIN_START + DifficultyRule.TARGET_BLOCK_MILLIS * chain.size()), 1_000_000L)
				.orElseThrow());
		}
		return List.copyOf(chain);
	}

	/**
	 * A test chain in the shape lethenon 0.4.0 accepts: block 0 pays the burn account
	 * (lethenon#148), block 1 pays the payer its reward, and block 2 carries the payer's two
	 * payments to the wallet: 3 LETH to its Ed25519 account and 5 LETH to a one-time destination of
	 * its published address
	 * @param payer
	 *            the miner of block 1 and the sender, an Ed25519 key pair
	 * @param wallet
	 *            the wallet that is paid
	 * @return the three blocks, genesis first
	 */
	static List<BlockBody> aChainPayingTheWallet(final KeyPair payer, final Wallet wallet)
	{
		Bytes payerKey = TransactionSigner.asBytes(payer.getPublic());
		List<BlockBody> chain = new ArrayList<>(List.of(Genesis.candidate(Chain.TEST_IDENTIFIER,
			"in the beginning was the pun", CHAIN_START)));
		chain.add(Blocks.mine(Mining.nextBlock(chain, payerKey, List.of(), "the payer's block",
			CHAIN_START + DifficultyRule.TARGET_BLOCK_MILLIS), 1_000_000L).orElseThrow());
		SignedTransaction direct = TransactionSigner.sign(
			new TransactionBody(Chain.TEST_IDENTIFIER, 0L, payerKey,
				Destination.direct(wallet.spendKey(SignatureSuite.ED25519)), Amount.ofLeth(3L),
				Amount.ZERO, "three, to the account"),
			SignatureSuite.ED25519, payer.getPrivate());
		SignedTransaction oneTime = TransactionSigner.sign(
			new TransactionBody(Chain.TEST_IDENTIFIER, 1L, payerKey,
				OneTimeAddresses.destinationFor(wallet.address(),
					OneTimeAddresses.newEphemeralKeyPair()),
				Amount.ofLeth(5L), Amount.ZERO, "five, to a one-time destination"),
			SignatureSuite.ED25519, payer.getPrivate());
		chain.add(Blocks.mine(Mining.nextBlock(chain, payerKey, List.of(direct, oneTime),
			"the third pun", CHAIN_START + 2 * DifficultyRule.TARGET_BLOCK_MILLIS), 1_000_000L)
			.orElseThrow());
		return List.copyOf(chain);
	}
}
