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
 * Funds at a one-time destination are counted and shown, but never added to what the accounts can
 * spend: each destination is an account of its own, and its money reaches the wallet's account only
 * through a sweep (lethenon#37) - which also shows the destination and the account together on the
 * chain. Adding them to a spendable total would hide that step and its cost.
 *
 * @param accounts
 *            the wallet's direct accounts, one per signature suite, which it can spend from
 * @param oneTimePayments
 *            how many payments arrived at one-time destinations of the wallet's address
 * @param oneTimeAmount
 *            what those payments hold together, spendable after a sweep
 * @param address
 *            the wallet's published address as text, {@code <view key>:<spend key>} - what a payer
 *            needs in order to pay a one-time destination of it
 * @param replaySummary
 *            the chain library's own sentence about the replay this balance comes from
 */
public record WalletBalance(List<AccountBalance> accounts, int oneTimePayments,
	Amount oneTimeAmount, String address, String replaySummary) {

	public WalletBalance {
		accounts = List.copyOf(accounts);
	}
}
