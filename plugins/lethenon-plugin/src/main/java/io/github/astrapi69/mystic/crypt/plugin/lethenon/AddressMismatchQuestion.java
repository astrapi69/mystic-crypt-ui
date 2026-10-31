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

import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * What the person answers when an entry's address is not the address of its wallet (#716, T6 and
 * T6a): "Close" by default, which changes nothing; a replacement only after a second question that
 * names the old and the new address
 */
interface AddressMismatchQuestion
{

	/** What the person answered */
	enum Answer
	{
		/** keep both addresses as they are */
		CLOSE,
		/** replace the entry's address with the wallet's, confirmed */
		REPLACE
	}

	/** No window to ask in: nothing is changed */
	AddressMismatchQuestion NOBODY = mismatch -> Answer.CLOSE;

	/** Asks with dialogs on the event thread, in the language the application started in */
	AddressMismatchQuestion ASK = mismatch -> {
		AtomicReference<Answer> answer = new AtomicReference<>(Answer.CLOSE);
		Runnable asking = () -> answer.set(Dialogs.ask(mismatch, Locale.getDefault()));
		if (SwingUtilities.isEventDispatchThread())
		{
			asking.run();
			return answer.get();
		}
		try
		{
			SwingUtilities.invokeAndWait(asking);
		}
		catch (InterruptedException interrupted)
		{
			Thread.currentThread().interrupt();
		}
		catch (InvocationTargetException failed)
		{
			throw new IllegalStateException("the address warning could not be shown", failed);
		}
		return answer.get();
	};

	/**
	 * Asks what to do about the difference
	 *
	 * @param mismatch
	 *            both addresses
	 * @return the answer; anything but a confirmed replacement is {@link Answer#CLOSE}
	 */
	Answer ask(AddressMismatch mismatch);

	/** The two dialogs of T6 and T6a */
	final class Dialogs
	{

		private Dialogs()
		{
		}

		static Answer ask(final AddressMismatch mismatch, final Locale locale)
		{
			String close = text("lethenon.address.mismatch.close", "Close", locale);
			String use = text("lethenon.address.mismatch.use", "Use the wallet's address", locale);
			Window parent = parent();
			String warning = mismatch.bothAddresses(locale) + "\n\n" + mismatch.consequence(locale)
				+ "\n\n" + mismatch.nothingChanged(locale);
			int chosen = JOptionPane.showOptionDialog(parent, warning, mismatch.title(locale),
				JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
				new Object[] { close, use }, close);
			if (chosen != 1)
			{
				return Answer.CLOSE;
			}
			String cancel = text("lethenon.address.mismatch.confirm.cancel", "Cancel", locale);
			String replace = text("lethenon.address.mismatch.confirm.replace", "Replace", locale);
			int confirmed = JOptionPane.showOptionDialog(parent, mismatch.confirmText(locale),
				mismatch.confirmTitle(locale), JOptionPane.DEFAULT_OPTION,
				JOptionPane.WARNING_MESSAGE, null, new Object[] { cancel, replace }, cancel);
			return confirmed == 1 ? Answer.REPLACE : Answer.CLOSE;
		}

		/**
		 * The window both dialogs belong to, chosen once before the first, so the confirmation
		 * does not take the warning that just closed as its owner: the active window while it
		 * shows, else the first frame that shows
		 */
		private static Window parent()
		{
			Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
			if (active != null && active.isShowing())
			{
				return active;
			}
			return Arrays.stream(Frame.getFrames()).filter(Frame::isShowing).findFirst()
				.orElse(null);
		}

		private static String text(final String key, final String defaultValue,
			final Locale locale)
		{
			return LethenonMessages.getString(key, defaultValue, locale);
		}
	}
}
