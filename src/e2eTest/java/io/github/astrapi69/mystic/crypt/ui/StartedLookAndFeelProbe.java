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
package io.github.astrapi69.mystic.crypt.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Window;

import javax.swing.AbstractButton;
import javax.swing.UIManager;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.StartupSignal;

/**
 * What a test starts in a JVM of its own to learn which look and feel the application applies at
 * start: it starts the application the way {@code StartMysticCryptApplication.main} does, cancels
 * the sign-in, waits until the application reports through {@link StartupSignal} that its start has
 * completed, and prints {@code LOOK_AND_FEEL=<class name>} of the look that is active then. Only
 * then, since the look of the sign-in dialog and the look of the frame are applied by two different
 * statements of the start. A JVM of its own, so that no {@code UIManager} state of the test JVM can
 * answer for the application. The signal must be switched on with
 * {@code -Dmystic.crypt.ui.startup.signal=true}
 */
public final class StartedLookAndFeelProbe
{

	/** The prefix of the one line the probe prints on standard output */
	public static final String OUTPUT_PREFIX = "LOOK_AND_FEEL=";

	private static final long TIMEOUT_MILLIS = 90_000;

	private StartedLookAndFeelProbe()
	{
	}

	public static void main(final String[] args) throws Exception
	{
		Thread application = new Thread(MysticCryptApplicationFrame::new, "probe-start");
		application.setDaemon(true);
		application.start();
		awaitCount("the sign-in dialog", StartupSignal::signInDialogsShown);
		EventQueue.invokeAndWait(StartedLookAndFeelProbe::cancelTheSignIn);
		awaitCount("the completed start", StartupSignal::startsCompleted);
		System.out.println(OUTPUT_PREFIX + UIManager.getLookAndFeel().getClass().getName());
		System.out.flush();
		System.exit(0);
	}

	private static void awaitCount(final String what, final java.util.function.IntSupplier count)
		throws InterruptedException
	{
		long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
		while (count.getAsInt() < 1)
		{
			if (System.currentTimeMillis() > deadline)
			{
				System.err.println("no " + what + " within " + TIMEOUT_MILLIS + " ms");
				System.exit(2);
			}
			Thread.sleep(100);
		}
	}

	private static void cancelTheSignIn()
	{
		for (Window window : Window.getWindows())
		{
			if (window instanceof Dialog dialog && dialog.isShowing())
			{
				AbstractButton cancel = buttonNamed(dialog, "btnCancel");
				if (cancel != null)
				{
					cancel.doClick();
					return;
				}
			}
		}
		System.err.println("the sign-in dialog has no button btnCancel");
		System.exit(3);
	}

	private static AbstractButton buttonNamed(final Container container, final String name)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof AbstractButton button && name.equals(button.getName()))
			{
				return button;
			}
			if (child instanceof Container inner)
			{
				AbstractButton found = buttonNamed(inner, name);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}
}
