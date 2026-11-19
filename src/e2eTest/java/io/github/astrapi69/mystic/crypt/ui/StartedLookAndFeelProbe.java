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

import java.awt.Dialog;
import java.awt.Window;

import javax.swing.UIManager;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;

/**
 * What a test starts in a JVM of its own to learn which look and feel the application applies at
 * start: it starts the application the way {@code StartMysticCryptApplication.main} does, waits for
 * the sign-in dialog and prints {@code LOOK_AND_FEEL=<class name>} of the look that is active then.
 * A JVM of its own, so that no {@code UIManager} state of the test JVM can answer for the
 * application
 */
public final class StartedLookAndFeelProbe
{

	/** The prefix of the one line the probe prints on standard output */
	public static final String OUTPUT_PREFIX = "LOOK_AND_FEEL=";

	private static final long TIMEOUT_MILLIS = 60_000;

	private StartedLookAndFeelProbe()
	{
	}

	public static void main(final String[] args) throws Exception
	{
		Thread application = new Thread(MysticCryptApplicationFrame::new, "probe-start");
		application.setDaemon(true);
		application.start();
		long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
		while (!signInDialogShowing())
		{
			if (System.currentTimeMillis() > deadline)
			{
				System.err
					.println("the sign-in dialog did not show within " + TIMEOUT_MILLIS + " ms");
				System.exit(2);
			}
			Thread.sleep(100);
		}
		System.out.println(OUTPUT_PREFIX + UIManager.getLookAndFeel().getClass().getName());
		System.out.flush();
		System.exit(0);
	}

	private static boolean signInDialogShowing()
	{
		for (Window window : Window.getWindows())
		{
			if (window instanceof Dialog dialog
				&& "Enter your credentials".equals(dialog.getTitle()) && dialog.isShowing())
			{
				return true;
			}
		}
		return false;
	}
}
