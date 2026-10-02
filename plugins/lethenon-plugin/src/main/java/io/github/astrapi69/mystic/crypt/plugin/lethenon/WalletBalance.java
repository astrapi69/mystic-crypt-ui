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

import java.util.List;

import io.github.astrapi69.lethenon.Amount;

/**
 * What a wallet holds in a replayed chain, the spendable part and the part it cannot move yet kept
 * apart.
 * <p>
 * Funds at a one-time destination are counted and shown, but never as spendable: lethenon#21 is
 * open, and until it is closed nobody can move them - not the payee either. Adding them to a
 * spendable total would show money the wallet cannot use, which is the one thing a balance must not
 * do.
 *
 * @param accounts
 *            the wallet's direct accounts, one per signature suite, which it can spend from
 * @param oneTimePayments
 *            how many payments arrived at one-time destinations of the wallet's address
 * @param oneTimeAmount
 *            what those payments hold together, NOT spendable until lethenon#21 is resolved
 * @param replaySummary
 *            the chain library's own sentence about the replay this balance comes from
 */
public record WalletBalance(List<AccountBalance> accounts, int oneTimePayments,
	Amount oneTimeAmount, String replaySummary) {

	public WalletBalance {
		accounts = List.copyOf(accounts);
	}
}
