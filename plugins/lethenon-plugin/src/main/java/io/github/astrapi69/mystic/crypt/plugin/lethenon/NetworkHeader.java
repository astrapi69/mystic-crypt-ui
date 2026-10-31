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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The network badge at the top right of a lethenon window (#697, draft 02): it shows the network of
 * the wallet the overview shows, and NETWORK UNKNOWN while there is none. The window's title stays
 * as it is: the host and the end-to-end suite find a tool window by its exact title
 */
final class NetworkHeader extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final JLabel lblNetworkBadge = badgeLabel();

	private final Locale locale;

	private final transient LethenonVault vault;

	private transient Runnable unfollow = () -> {
	};

	private NetworkHeader(final LethenonVault vault, final Locale locale)
	{
		super(new BorderLayout());
		this.vault = vault;
		this.locale = locale;
	}

	/**
	 * Puts the badge above a window's own panel
	 *
	 * @param tool
	 *            the window's panel
	 * @param vault
	 *            the plugin's view of the vault, whose wallet the badge follows
	 * @param locale
	 *            the language of the window
	 * @return the panel to put into the window
	 */
	static NetworkHeader around(final Component tool, final LethenonVault vault,
		final Locale locale)
	{
		NetworkHeader header = new NetworkHeader(vault, locale);
		JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
		row.add(header.lblNetworkBadge);
		header.add(row, BorderLayout.NORTH);
		header.add(tool, BorderLayout.CENTER);
		header.unfollow = follow(vault, header::show);
		return header;
	}

	/**
	 * Tells a listener the network of the session's wallet, at once and on every change
	 *
	 * @param vault
	 *            the plugin's view of the vault
	 * @param listener
	 *            told the network; unknown while there is no wallet
	 * @return what stops the telling, for a window that closes
	 */
	static Runnable follow(final LethenonVault vault, final Consumer<NetworkKind> listener)
	{
		listener.accept(kindOf(vault.chosen()));
		return vault.whenTheWalletChanges(wallet -> listener.accept(kindOf(wallet)));
	}

	@Override
	public void addNotify()
	{
		super.addNotify();
		unfollow.run();
		unfollow = follow(vault, this::show);
	}

	@Override
	public void removeNotify()
	{
		unfollow.run();
		super.removeNotify();
	}

	/**
	 * A label for a network badge, named so a test finds it
	 *
	 * @return the label
	 */
	static JLabel badgeLabel()
	{
		JLabel badge = new JLabel();
		badge.setName("lblNetworkBadge");
		badge.setFont(badge.getFont().deriveFont(Font.BOLD));
		return badge;
	}

	private static NetworkKind kindOf(final Optional<VaultWallet> wallet)
	{
		return wallet.map(chosen -> NetworkKind.of(chosen.network())).orElse(NetworkKind.UNKNOWN);
	}

	private void show(final NetworkKind kind)
	{
		kind.paint(lblNetworkBadge, locale);
	}
}
