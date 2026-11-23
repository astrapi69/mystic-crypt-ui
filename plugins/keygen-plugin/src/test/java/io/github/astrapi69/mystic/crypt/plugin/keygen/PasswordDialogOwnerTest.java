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
package io.github.astrapi69.mystic.crypt.plugin.keygen;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The dialogs of the key generator belong to its window (#63).
 * <p>
 * The password dialog of "save the private key with a password" used to be shown with no parent, so
 * it belonged to the shared owner frame of the Swing runtime: no window to stay in front of, no
 * centre to open over, a window of its own in the task bar. The panel itself is the parent now,
 * which makes the window the panel sits in the owner and the centre of that window the place the
 * dialog opens over.
 * <p>
 * Where a dialog opens is read only once it has settled. The window manager of the test display
 * (fluxbox on Xvfb) can report a dialog that was just shown at another place for a few milliseconds
 * and then move it back: measured 2026-11-21 in 40 repetitions, the message appeared at (724, 656)
 * and moved to (362, 339), the centre of the window, a moment later. It can also move the main window
 * after it first appears. A reading taken as soon as the dialog shows measured that moment, 333 pixels
 * off on develop, while the dialog stood where it belongs.
 */
class PasswordDialogOwnerTest
{

	private static final int CENTRE_TOLERANCE_PIXELS = 15;

	/** How many readings in a row must find the same bounds before they count as settled. */
	private static final int SETTLED_READINGS = 4;

	private static final long READING_INTERVAL_MILLIS = 50;

	private static final long SETTLE_TIMEOUT_MILLIS = 5_000;

	private static final String ENTRY_DIALOG_TITLE = "Enter a password";

	private JFrame frame;

	private GenerateKeysPanel panel;

	private Robot robot;

	@BeforeEach
	void showThePanelInAWindow() throws Exception
	{
		panel = new GenerateKeysPanel();
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("key generator window");
			frame.setContentPane(panel);
			frame.setSize(900, 700);
			frame.setLocation(60, 40);
			frame.setVisible(true);
		});
		robot = new Robot();
		awaitSettled(frame);
	}

	@AfterEach
	void closeEverything() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> {
			for (Window window : Window.getWindows())
			{
				if (window instanceof Dialog && window.isShowing())
				{
					window.dispose();
				}
			}
			frame.dispose();
		});
	}

	@Test
	@DisplayName("the password dialog is owned by the window of the key generator and opens over its centre")
	void passwordDialog_isOwnedByTheKeyGeneratorWindow() throws Exception
	{
		SwingUtilities.invokeLater(() -> panel.onSavePrivateKeyWithPassword(null));
		final Dialog dialog = awaitDialog(titled(ENTRY_DIALOG_TITLE));

		final Window owner = onEventThread(dialog::getOwner);
		final int offCentre = offCentre(awaitSettled(dialog, frame));
		clickButton(dialog, "Cancel");

		assertSame(frame, owner);
		assertTrue(offCentre <= CENTRE_TOLERANCE_PIXELS,
			"the dialog is " + offCentre + " pixels off the centre of the window");
	}

	@Test
	@DisplayName("the message about two different passwords is owned by the window of the key generator and opens over its centre")
	void mismatchMessage_isOwnedByTheKeyGeneratorWindow() throws Exception
	{
		SwingUtilities.invokeLater(() -> panel.onSavePrivateKeyWithPassword(null));
		final Dialog passwordDialog = awaitDialog(titled(ENTRY_DIALOG_TITLE));
		final PasswordPanel passwordPanel = onEventThread(
			() -> (PasswordPanel)find(passwordDialog, PasswordPanel.class::isInstance));
		onEventThread(() -> {
			passwordPanel.getTxtPassword().setText("one-" + System.nanoTime());
			passwordPanel.getTxtRepeatPassword().setText("other-" + System.nanoTime());
			return null;
		});
		clickButton(passwordDialog, "Set password");

		final Dialog message = awaitDialog(titled(optionPaneText("OptionPane.messageDialogTitle")));
		final Window owner = onEventThread(message::getOwner);
		final int offCentre = offCentre(awaitSettled(message, frame));
		clickButton(message, optionPaneText("OptionPane.okButtonText"));

		assertSame(frame, owner);
		assertTrue(offCentre <= CENTRE_TOLERANCE_PIXELS,
			"the message is " + offCentre + " pixels off the centre of the window");
	}

	/**
	 * The text the look and feel gives a message dialog, in the default locale the dialog is built
	 * with, so the test finds it whatever language the machine runs in
	 *
	 * @param key
	 *            the key of the text in the defaults of {@link UIManager}
	 * @return the text
	 */
	private static String optionPaneText(final String key)
	{
		return UIManager.getString(key, Locale.getDefault());
	}

	/**
	 * How far the centre of the dialog lies from the centre of the window, the larger of the two axes
	 *
	 * @param settled
	 *            the bounds of the dialog and of the window, in this order, as
	 *            {@link #awaitSettled(Window...)} returns them
	 * @return the distance in pixels
	 */
	private static int offCentre(final Rectangle[] settled)
	{
		final Rectangle dialog = settled[0];
		final Rectangle window = settled[1];
		final int horizontal = Math.abs(
			dialog.x + dialog.width / 2 - (window.x + window.width / 2));
		final int vertical = Math.abs(
			dialog.y + dialog.height / 2 - (window.y + window.height / 2));
		return Math.max(horizontal, vertical);
	}

	/**
	 * Waits until the given windows are showing, the event queue is empty and their bounds have stayed
	 * the same over {@link #SETTLED_READINGS} readings in a row, and returns those bounds. A window that
	 * does not settle within {@link #SETTLE_TIMEOUT_MILLIS} fails the test, it is never measured
	 * anyway.
	 *
	 * @param windows
	 *            the windows to read
	 * @return their settled bounds, in the order given
	 * @throws Exception
	 *             if reading on the event thread fails or the wait is interrupted
	 */
	private Rectangle[] awaitSettled(final Window... windows) throws Exception
	{
		final long deadline = System.currentTimeMillis() + SETTLE_TIMEOUT_MILLIS;
		Rectangle[] last = null;
		int sameReadings = 0;
		while (System.currentTimeMillis() < deadline)
		{
			robot.waitForIdle();
			final Rectangle[] reading = onEventThread(() -> boundsWhileShowing(windows));
			sameReadings = reading != null && Arrays.equals(reading, last) ? sameReadings + 1 : 0;
			if (sameReadings + 1 >= SETTLED_READINGS)
			{
				return reading;
			}
			last = reading;
			Thread.sleep(READING_INTERVAL_MILLIS);
		}
		throw new AssertionError("the windows did not settle within " + SETTLE_TIMEOUT_MILLIS
			+ " ms, last bounds " + Arrays.toString(last));
	}

	private static Rectangle[] boundsWhileShowing(final Window... windows)
	{
		final Rectangle[] bounds = new Rectangle[windows.length];
		for (int index = 0; index < windows.length; index++)
		{
			if (!windows[index].isShowing())
			{
				return null;
			}
			bounds[index] = windows[index].getBounds();
		}
		return bounds;
	}

	private static Predicate<Dialog> titled(final String title)
	{
		return dialog -> title.equals(dialog.getTitle());
	}

	private static Dialog awaitDialog(final Predicate<Dialog> wanted) throws Exception
	{
		final long deadline = System.currentTimeMillis() + 15_000;
		while (System.currentTimeMillis() < deadline)
		{
			final Optional<Dialog> found = onEventThread(() -> {
				for (Window window : Window.getWindows())
				{
					if (window instanceof Dialog && window.isShowing()
						&& wanted.test((Dialog)window))
					{
						return Optional.of((Dialog)window);
					}
				}
				return Optional.<Dialog> empty();
			});
			if (found.isPresent())
			{
				return found.get();
			}
			Thread.sleep(50);
		}
		throw new AssertionError("the dialog did not appear within 15 seconds");
	}

	private static void clickButton(final Container root, final String text) throws Exception
	{
		SwingUtilities.invokeAndWait(() -> {
			final Component button = find(root,
				component -> component instanceof AbstractButton
					&& text.equals(((AbstractButton)component).getText()));
			assertNotNull(button, "no button " + text);
			((AbstractButton)button).doClick();
		});
	}

	private static Component find(final Container root, final Predicate<Component> wanted)
	{
		for (Component child : root.getComponents())
		{
			if (wanted.test(child))
			{
				return child;
			}
			if (child instanceof Container)
			{
				final Component inside = find((Container)child, wanted);
				if (inside != null)
				{
					return inside;
				}
			}
		}
		return null;
	}

	private static <T> T onEventThread(final java.util.concurrent.Callable<T> task)
		throws Exception
	{
		final Object[] holder = new Object[1];
		SwingUtilities.invokeAndWait(() -> {
			try
			{
				holder[0] = task.call();
			}
			catch (final Exception e)
			{
				throw new IllegalStateException(e);
			}
		});
		@SuppressWarnings("unchecked")
		final T value = (T)holder[0];
		return value;
	}
}
