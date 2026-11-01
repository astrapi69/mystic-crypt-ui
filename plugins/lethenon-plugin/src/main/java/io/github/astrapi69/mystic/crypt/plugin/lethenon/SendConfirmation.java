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
import java.util.Optional;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

/**
 * Asks the person before a send that needs it (#704): the confirmation T5 on the main network, and
 * which network a wallet is on when its entry names none (draft 02). Cancel is the default of both.
 */
interface SendConfirmation
{

	/** Asks with dialogs, in the language the application started in */
	SendConfirmation ASK = new SendConfirmation()
	{
		@Override
		public boolean confirmMainNetwork(final JComponent parent,
			final MainNetworkTransfer transfer)
		{
			Locale locale = Locale.getDefault();
			String cancel = text("lethenon.send.confirm.mainnet.cancel", "Cancel", locale);
			String send = text("lethenon.send.confirm.mainnet.send", "Send", locale);
			String message = transfer.sentence(locale) + "\n" + transfer.fee(locale) + "\n\n"
				+ transfer.irreversible(locale);
			int chosen = JOptionPane.showOptionDialog(parent, message, transfer.title(locale),
				JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
				new Object[] { cancel, send }, cancel);
			return chosen == 1;
		}

		@Override
		public Optional<NetworkKind> askNetwork(final JComponent parent)
		{
			Locale locale = Locale.getDefault();
			String cancel = text("lethenon.send.network.cancel", "Cancel", locale);
			String test = NetworkKind.TEST.badge(locale);
			String main = NetworkKind.MAIN.badge(locale);
			String message = NetworkKind.UNKNOWN.riskNote(locale) + "\n\n"
				+ text("lethenon.send.network.question", "Which network is this wallet on?", locale);
			int chosen = JOptionPane.showOptionDialog(parent, message,
				text("lethenon.send.network.title", "Which network?", locale),
				JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
				new Object[] { cancel, test, main }, cancel);
			return switch (chosen)
			{
				case 1 -> Optional.of(NetworkKind.TEST);
				case 2 -> Optional.of(NetworkKind.MAIN);
				default -> Optional.empty();
			};
		}

		private String text(final String key, final String defaultValue, final Locale locale)
		{
			return LethenonMessages.getString(key, defaultValue, locale);
		}
	};

	/**
	 * Asks whether to send on the main network, with T5
	 *
	 * @param parent
	 *            the window
	 * @param transfer
	 *            what is about to be sent
	 * @return true only when the person chose Send
	 */
	boolean confirmMainNetwork(JComponent parent, MainNetworkTransfer transfer);

	/**
	 * Asks which network a wallet is on whose entry names none
	 *
	 * @param parent
	 *            the window
	 * @return the network chosen, or empty when the person cancelled
	 */
	Optional<NetworkKind> askNetwork(JComponent parent);
}
