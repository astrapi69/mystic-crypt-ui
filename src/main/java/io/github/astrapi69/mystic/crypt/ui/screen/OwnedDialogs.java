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
package io.github.astrapi69.mystic.crypt.ui.screen;

import java.awt.Component;
import java.awt.Window;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import io.github.astrapi69.swing.dialog.DialogExtensions;
import io.github.astrapi69.swing.dialog.JOptionPaneExtensions;
import io.github.astrapi69.swing.listener.RequestFocusListener;

/**
 * Opens a dialog that belongs to the window of the component that asks for it, centred over that
 * window (#63).
 * <p>
 * A dialog without a parent belongs to Swing's shared, invisible owner frame and opens on the first
 * screen: with the application on a second monitor, every dialog of the database tree opened on the
 * first. Giving it a parent is not enough with swing-base-components 5.2.1, whose
 * {@code JOptionPaneExtensions.getSelectedOption} takes the parent and then centres the dialog on
 * the screen anyway ({@code dialog.setLocationRelativeTo(null)}). This class builds the same dialog
 * with the caller's window as owner and centres it there.
 * <p>
 * TODO(astrapi69/swing-base-components#29): use the library's method again once it centres over the
 * parent it is given
 */
public final class OwnedDialogs
{

	private OwnedDialogs()
	{
	}

	/**
	 * The window the given component is shown in: the component itself if it is a window
	 *
	 * @param caller
	 *            the component that asks for a dialog
	 * @return its window, or {@code null} if it is in none yet
	 */
	public static Window ownerOf(final Component caller)
	{
		if (caller == null)
		{
			return null;
		}
		if (caller instanceof Window window)
		{
			return window;
		}
		return SwingUtilities.getWindowAncestor(caller);
	}

	/**
	 * Shows the panel in a modal option dialog that belongs to the caller's window, centred over
	 * it, and returns the option the user chose
	 *
	 * @param caller
	 *            the component that asks, whose window owns the dialog
	 * @param panel
	 *            what the dialog shows
	 * @param messageType
	 *            the {@link JOptionPane} message type
	 * @param optionType
	 *            the {@link JOptionPane} option type
	 * @param title
	 *            the title of the dialog
	 * @param focusedComponent
	 *            the component of the panel that gets the focus, or {@code null}
	 * @return the option chosen, {@code -1} if the dialog was closed
	 */
	public static int selectedOption(final Component caller, final JPanel panel,
		final int messageType, final int optionType, final String title,
		final JComponent focusedComponent)
	{
		Window owner = ownerOf(caller);
		JOptionPane pane = new JOptionPane(panel, messageType, optionType);
		JDialog dialog = pane.createDialog(owner, title);
		if (focusedComponent != null)
		{
			dialog.addWindowFocusListener(new RequestFocusListener(focusedComponent));
		}
		dialog.pack();
		dialog.setLocationRelativeTo(owner);
		dialog.setVisible(true);
		dialog.dispose();
		return JOptionPaneExtensions.getSelectedOption(pane);
	}

	/**
	 * Asks a confirmation in a dialog that belongs to the caller's window, centred over it
	 *
	 * @param caller
	 *            the component that asks, whose window owns the dialog
	 * @param title
	 *            the title of the dialog
	 * @param message
	 *            the message, HTML without the html and body tags
	 * @param optionType
	 *            the {@link JOptionPane} option type
	 * @param messageType
	 *            the {@link JOptionPane} message type
	 * @return the option chosen
	 */
	public static int confirm(final Component caller, final String title, final String message,
		final int optionType, final int messageType)
	{
		return DialogExtensions.showConfirmDialog(ownerOf(caller), title, message, optionType,
			messageType, null);
	}
}
