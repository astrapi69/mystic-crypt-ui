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

import java.awt.Component;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JFileChooser;

/**
 * The two pieces of Swing every tool window of this plugin needs: a named button and a file chooser
 * that opens where the current file is. One place, so the windows cannot drift apart.
 */
final class LethenonSwing
{

	private LethenonSwing()
	{
	}

	/**
	 * A button with a name an end-to-end test can find, an action and a tooltip
	 *
	 * @param name
	 *            the component name
	 * @param text
	 *            the label
	 * @param listener
	 *            what a click does
	 * @param tooltip
	 *            the tooltip
	 * @return the button
	 */
	static JButton button(final String name, final String text, final ActionListener listener,
		final String tooltip)
	{
		JButton button = new JButton(text);
		button.setName(name);
		button.addActionListener(listener);
		button.setToolTipText(tooltip);
		return button;
	}

	/**
	 * Asks for a file, starting in the directory of the current one
	 *
	 * @param parent
	 *            the component the dialog belongs to
	 * @param current
	 *            the file named so far, may be blank
	 * @return the chosen file's absolute path, or empty when the dialog was cancelled
	 */
	static Optional<String> chooseFile(final Component parent, final String current)
	{
		JFileChooser chooser = new JFileChooser();
		if (current != null && !current.isBlank())
		{
			File chosenBefore = new File(current);
			chooser.setCurrentDirectory(
				chosenBefore.isDirectory() ? chosenBefore : chosenBefore.getParentFile());
		}
		if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION)
		{
			return Optional.empty();
		}
		return Optional.of(chooser.getSelectedFile().getAbsolutePath());
	}
}
