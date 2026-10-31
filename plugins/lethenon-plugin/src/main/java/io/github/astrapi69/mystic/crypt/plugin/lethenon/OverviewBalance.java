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
import java.nio.file.Path;

import io.github.astrapi69.lethenon.Amount;
import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.ChainState;
import io.github.astrapi69.lethenon.PublishedAddress;

/**
 * The balance the overview shows without the wallet password (#700, decision record "The balance
 * in the overview"): the chain is replayed and both accounts are read with the public keys the
 * entry records. Every block is checked by the replay; nothing here computes a balance itself.
 */
final class OverviewBalance
{

	private OverviewBalance()
	{
	}

	/**
	 * The balance of both accounts of a wallet
	 *
	 * @param chainFile
	 *            the chain to read
	 * @param wallet
	 *            the wallet as the overview lists it
	 * @return both accounts' balances
	 * @throws IOException
	 *             if the chain cannot be read
	 */
	static AccountsBalance of(final Path chainFile, final VaultWallet wallet) throws IOException
	{
		ChainState state = ChainReplaySupport.replayed(chainFile).replay().finalState();
		Amount ed25519 = state.balanceOf(PublishedAddress.parse(wallet.address()).spendKey());
		Amount mlDsa = wallet.mlDsaAccount().isEmpty()
			? Amount.ZERO
			: state.balanceOf(Bytes.ofHex(wallet.mlDsaAccount()));
		return new AccountsBalance(ed25519, mlDsa);
	}
}
