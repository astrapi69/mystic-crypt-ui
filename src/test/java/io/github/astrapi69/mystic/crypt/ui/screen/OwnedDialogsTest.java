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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.Window;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link OwnedDialogs} gives a dialog the window of the component that asks for it (#63)
 */
class OwnedDialogsTest
{

	private JFrame frame;

	@AfterEach
	void disposeTheFrame() throws Exception
	{
		if (frame != null)
		{
			SwingUtilities.invokeAndWait(frame::dispose);
		}
	}

	@Test
	@DisplayName("the owner is the window the component is shown in, the window itself, or none")
	void theOwner_isTheWindowOfTheCaller() throws Exception
	{
		JPanel inside = new JPanel();
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("caller");
			frame.add(inside);
			frame.pack();
		});

		assertSame(frame, OwnedDialogs.ownerOf(inside), "a component in a window");
		assertSame(frame, OwnedDialogs.ownerOf(frame), "a window");
		assertNull(OwnedDialogs.ownerOf(new JPanel()), "a component in no window yet");
		assertNull(OwnedDialogs.ownerOf(null), "no component");
	}

	@Test
	@DisplayName("the option dialog belongs to the caller's window and returns the option chosen")
	void theOptionDialog_belongsToTheCallersWindow() throws Exception
	{
		JPanel caller = new JPanel();
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("caller");
			frame.add(caller);
			frame.setBounds(100, 100, 400, 300);
			frame.setVisible(true);
		});
		AtomicInteger chosen = new AtomicInteger(Integer.MIN_VALUE);
		SwingUtilities.invokeLater(() -> chosen.set(OwnedDialogs.selectedOption(caller,
			new JPanel(), JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, "asked", null)));

		JDialog dialog = awaitDialog("asked");
		assertSame(frame, dialog.getOwner(), "the dialog belongs to the caller's window");
		SwingUtilities.invokeAndWait(() -> optionPaneOf(dialog).setValue(JOptionPane.OK_OPTION));
		SwingUtilities.invokeAndWait(() -> {
		});

		assertEquals(JOptionPane.OK_OPTION, chosen.get());
	}

	private static JDialog awaitDialog(final String title) throws Exception
	{
		for (int attempt = 0; attempt < 100; attempt++)
		{
			JDialog[] found = new JDialog[1];
			SwingUtilities.invokeAndWait(() -> found[0] = Arrays.stream(Window.getWindows())
				.filter(JDialog.class::isInstance).map(JDialog.class::cast)
				.filter(dialog -> title.equals(dialog.getTitle()) && dialog.isShowing()).findFirst()
				.orElse(null));
			if (found[0] != null)
			{
				return found[0];
			}
			Thread.sleep(50);
		}
		throw new AssertionError("no dialog '" + title + "' opened");
	}

	/** JOptionPane.createDialog puts the pane into the dialog's content pane, alone */
	private static JOptionPane optionPaneOf(final JDialog dialog)
	{
		return (JOptionPane)dialog.getContentPane().getComponent(0);
	}
}
