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
import java.util.Arrays;
import java.util.List;

import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.ChainState;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletScan;

/**
 * What a wallet holds in a chain file, worked out by replaying the chain - never asked of anybody
 * (lethenon's NoBalanceQueryTest is the rule behind that, and this class names no networking type).
 * <p>
 * No chain logic of its own: {@link io.github.astrapi69.lethenon.WalletFile#read} opens the wallet,
 * the chain goes through the same read-and-replay path as every other tool of this plugin, the
 * direct accounts are read from the replayed state, and {@link WalletScan#over} recognises the
 * payments to one-time destinations with the view key. Those are reported apart from the accounts:
 * they are spent by sweeping them first (lethenon#37).
 * <p>
 * The password is a {@code char[]} and is wiped before this method returns, whatever happens; it
 * appears in no message.
 */
public final class WalletBalanceSupport
{

	private WalletBalanceSupport()
	{
	}

	/**
	 * Replays the chain and reads what the wallet holds in it
	 *
	 * @param chainFile
	 *            the chain file
	 * @param walletFile
	 *            the wallet file lethenon's command line wrote
	 * @param password
	 *            the wallet file's password; overwritten with zeros before this method returns
	 * @return the spendable accounts and, apart from them, the one-time payments
	 * @throws IOException
	 *             when a file cannot be read
	 * @throws IllegalArgumentException
	 *             when a file is not named or not there, or the password does not open the wallet
	 * @throws io.github.astrapi69.lethenon.ChainRejected
	 *             when the chain does not verify, in which case there is no balance at all
	 */
	public static WalletBalance balance(final Path chainFile, final Path walletFile,
		final char[] password) throws IOException
	{
		try
		{
			Wallet wallet = LethenonWallets.open(walletFile, password);
			ChainReplaySupport.Replayed replayed = ChainReplaySupport.replayed(chainFile);
			ChainState state = replayed.replay().finalState();
			List<AccountBalance> accounts = Arrays.stream(SignatureSuite.values()).map(suite -> {
				Bytes account = wallet.spendKey(suite);
				return new AccountBalance(suite.identifier(), account.toString(),
					state.balanceOf(account));
			}).toList();
			WalletScan scan = WalletScan.over(replayed.chain(), wallet.address(),
				wallet.viewKeyPair().getPrivate());
			return new WalletBalance(accounts, scan.received().size(), scan.balance(),
				wallet.address().toText(), replayed.replay().describe());
		}
		finally
		{
			Arrays.fill(password, '\0');
		}
	}
}
