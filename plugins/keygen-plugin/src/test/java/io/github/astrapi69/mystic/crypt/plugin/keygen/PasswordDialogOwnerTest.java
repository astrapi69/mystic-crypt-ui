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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Window;
import java.util.Optional;
import java.util.function.Predicate;

import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

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
 * which makes the window the panel sits in the owner.
 */
class PasswordDialogOwnerTest
{

	private static final String SHARED_OWNER_FRAME = "javax.swing.SwingUtilities$SharedOwnerFrame";

	private static final String PASSWORD_DIALOG_TITLE = "Enter a password";

	private static final String MISMATCH_DIALOG_TITLE = "Message";

	private JFrame frame;

	private GenerateKeysPanel panel;

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
	@DisplayName("the password dialog is owned by the shared owner frame, as it is today")
	void passwordDialog_isOwnedBySharedOwnerFrame_today() throws Exception
	{
		SwingUtilities.invokeLater(() -> panel.onSavePrivateKeyWithPassword(null));
		final Dialog dialog = awaitDialog(titled(PASSWORD_DIALOG_TITLE));

		final String ownerClass = onEventThread(() -> dialog.getOwner().getClass().getName());
		clickButton(dialog, "Cancel");

		assertEquals(SHARED_OWNER_FRAME, ownerClass);
	}

	@Test
	@DisplayName("the message about two different passwords is owned by the shared owner frame, as it is today")
	void mismatchMessage_isOwnedBySharedOwnerFrame_today() throws Exception
	{
		SwingUtilities.invokeLater(() -> panel.onSavePrivateKeyWithPassword(null));
		final Dialog passwordDialog = awaitDialog(titled(PASSWORD_DIALOG_TITLE));
		final PasswordPanel passwordPanel = onEventThread(
			() -> (PasswordPanel)find(passwordDialog, PasswordPanel.class::isInstance));
		onEventThread(() -> {
			passwordPanel.getTxtPassword().setText("one-" + System.nanoTime());
			passwordPanel.getTxtRepeatPassword().setText("other-" + System.nanoTime());
			return null;
		});
		clickButton(passwordDialog, "Set password");

		final Dialog message = awaitDialog(titled(MISMATCH_DIALOG_TITLE));
		final String ownerClass = onEventThread(() -> message.getOwner().getClass().getName());
		clickButton(message, "OK");

		assertEquals(SHARED_OWNER_FRAME, ownerClass);
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
