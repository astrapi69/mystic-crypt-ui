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

/**
 * The answer a unit test gives when a send asks which network its wallet is on: the test network. A
 * send from a wallet file asks that since #978, and the dialog of {@link SendConfirmation#ASK}
 * would wait for a click no unit test gives. The main network question is never answered: a test
 * that reaches it fails instead of signing.
 */
final class OnTheTestNetwork implements SendConfirmation
{
	private OnTheTestNetwork()
	{
	}

	/**
	 * A send window over the plugin's vault that answers the network question with the test network
	 *
	 * @return the window
	 */
	static LethenonSendPanel sendPanel()
	{
		return new LethenonSendPanel(ChainConversionConsent.ASK, LethenonVault.current(),
			new OnTheTestNetwork(), Locale.getDefault());
	}

	@Override
	public boolean confirmMainNetwork(final JComponent parent, final MainNetworkTransfer transfer)
	{
		throw new AssertionError("a send on the test network asks no main network question");
	}

	@Override
	public Optional<NetworkKind> askNetwork(final JComponent parent)
	{
		return Optional.of(NetworkKind.TEST);
	}
}
