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

import java.util.Locale;

/**
 * The network a wallet of the vault is meant for, recorded as the hint {@code lethenon.network} on
 * its entry (docs/decisions/lethenon-wallet-in-vault-2026-10-30.md): the wallet itself has no
 * network
 */
public enum WalletNetwork
{

	/** the test network, where LETH has no monetary value */
	TESTNET("testnet", "lethenon.wallet.network.testnet", "Test network"),

	/** the main network */
	MAINNET("mainnet", "lethenon.wallet.network.mainnet", "Main network");

	private final String hint;

	private final String key;

	private final String defaultName;

	WalletNetwork(final String hint, final String key, final String defaultName)
	{
		this.hint = hint;
		this.key = key;
		this.defaultName = defaultName;
	}

	/**
	 * @return the value of {@code lethenon.network}, "testnet" or "mainnet"
	 */
	public String hint()
	{
		return hint;
	}

	/**
	 * The name a person reads
	 *
	 * @param locale
	 *            the language of the window
	 * @return e.g. "Test network"
	 */
	public String displayName(final Locale locale)
	{
		return LethenonMessages.getString(key, defaultName, locale);
	}

	/**
	 * The network an entry's hint names
	 *
	 * @param hint
	 *            the value of {@code lethenon.network}, may be empty
	 * @param locale
	 *            the language of the window
	 * @return its name, or "Network unknown" for a missing or unknown hint
	 */
	static String nameOf(final String hint, final Locale locale)
	{
		for (WalletNetwork network : values())
		{
			if (network.hint.equals(hint))
			{
				return network.displayName(locale);
			}
		}
		return LethenonMessages.getString("lethenon.wallet.network.unknown", "Network unknown",
			locale);
	}
}
