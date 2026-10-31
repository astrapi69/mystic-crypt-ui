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

import java.awt.Color;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

/**
 * Which network a wallet is on, as a person sees it (#697, draft 02): the test network, the main
 * network, or unknown when the vault entry names none. The hint {@code lethenon.network} decides;
 * nothing is guessed from a chain or an address.
 */
public enum NetworkKind
{

	/** the test network: LETH has no monetary value here */
	TEST("lethenon.network.badge.test", "TESTNET", "lethenon.network.risk.test",
		"Test network, LETH has no monetary value.", new Color(0xfff4d6), new Color(0xc98a00),
		new Color(0x8a5a00)),

	/** the main network: LETH has real value here */
	MAIN("lethenon.network.badge.main", "MAIN NETWORK", "lethenon.network.risk.main",
		"Main network: LETH has real value here, and a transfer cannot be called back.",
		new Color(0xfdecec), new Color(0xb71c1c), new Color(0x8e1515)),

	/** the entry names no network, or one this plugin does not know */
	UNKNOWN("lethenon.network.badge.unknown", "NETWORK UNKNOWN", "lethenon.network.risk.unknown",
		"The entry names no network. Check it before you send.", new Color(0xeef0f3),
		new Color(0x9aa3b2), new Color(0x5b6270));

	private final String badgeKey;

	private final String defaultBadge;

	private final String riskKey;

	private final String defaultRisk;

	private final Color background;

	private final Color border;

	private final Color foreground;

	NetworkKind(final String badgeKey, final String defaultBadge, final String riskKey,
		final String defaultRisk, final Color background, final Color border,
		final Color foreground)
	{
		this.badgeKey = badgeKey;
		this.defaultBadge = defaultBadge;
		this.riskKey = riskKey;
		this.defaultRisk = defaultRisk;
		this.background = background;
		this.border = border;
		this.foreground = foreground;
	}

	/**
	 * The network a hint names
	 *
	 * @param hint
	 *            the value of {@code lethenon.network}, may be empty or null
	 * @return test, main, or unknown for anything else
	 */
	public static NetworkKind of(final String hint)
	{
		if (WalletNetwork.TESTNET.hint().equals(hint))
		{
			return TEST;
		}
		return WalletNetwork.MAINNET.hint().equals(hint) ? MAIN : UNKNOWN;
	}

	/**
	 * @param locale
	 *            the language of the window
	 * @return the badge's text, e.g. "TESTNET"
	 */
	public String badge(final Locale locale)
	{
		return LethenonMessages.getString(badgeKey, defaultBadge, locale);
	}

	/**
	 * @param locale
	 *            the language of the window
	 * @return the short risk note the overview shows
	 */
	public String riskNote(final Locale locale)
	{
		return LethenonMessages.getString(riskKey, defaultRisk, locale);
	}

	/**
	 * Gives a label this network's badge: its text, its colours and its frame
	 *
	 * @param label
	 *            the badge label
	 * @param locale
	 *            the language of the window
	 */
	void paint(final JLabel label, final Locale locale)
	{
		label.setText(badge(locale));
		label.setOpaque(true);
		label.setBackground(background);
		label.setForeground(foreground);
		label.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(border, 2), BorderFactory.createEmptyBorder(3, 10, 3, 10)));
	}
}
