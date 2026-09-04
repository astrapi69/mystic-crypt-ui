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
package io.github.astrapi69.mystic.crypt.plugin.kem;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

import javax.swing.JButton;
import javax.swing.JTextArea;

/**
 * A button that puts a text area's current content on the system clipboard.
 * <p>
 * Both panels of this plugin show long Base64/hex values (public keys, handshakes, shared
 * secrets, ciphertexts) a user has to move elsewhere by hand - selecting one correctly across a
 * wrapped, multi-line area is easy to get wrong, so every such area gets one of these next to it
 * (#184).
 */
final class CopyButtons
{

	private CopyButtons()
	{
	}

	/**
	 * Creates a copy button for the given text area
	 *
	 * @param name
	 *            the component name, for lookups in tests
	 * @param source
	 *            the area whose current text is copied
	 * @param tooltip
	 *            what the button explains itself with
	 * @return the button
	 */
	static JButton copyButton(String name, JTextArea source, String tooltip)
	{
		JButton button = new JButton(
			KemDemoMessages.getString("kemdemo.copy.button.label", "Copy"));
		button.setName(name);
		button.setToolTipText(tooltip);
		button.addActionListener(event -> Toolkit.getDefaultToolkit().getSystemClipboard()
			.setContents(new StringSelection(source.getText()), null));
		return button;
	}

}
