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
package io.github.astrapi69.mystic.crypt.plugin.console;

import java.io.PrintStream;

import javax.swing.JInternalFrame;

/**
 * The one open console, and what it takes with it when it goes (#375).
 * <p>
 * The console captures {@code System.out} and {@code System.err}, so closing its window is not
 * enough: the streams kept pointing into the text area of a window nobody could see any more, and
 * everything ever written stayed reachable in it - vault paths among it, which is what the
 * measurement on #375 found. Locking is where this matters, because a locked workspace is one
 * whose content is supposed to be gone from the screen.
 */
public final class ConsoleSession
{

	private static JInternalFrame openFrame;

	private static ConsoleBuffer openBuffer;

	private static PrintStream systemOutBeforeTheConsole;

	private static PrintStream systemErrBeforeTheConsole;

	private ConsoleSession()
	{
	}

	/**
	 * Notes the console that was just opened, and the streams it replaced
	 *
	 * @param frame
	 *            the console window
	 * @param buffer
	 *            what the console shows
	 * @param systemOut
	 *            the standard output the console replaced
	 * @param systemErr
	 *            the standard error the console replaced
	 */
	static void opened(final JInternalFrame frame, final ConsoleBuffer buffer,
		final PrintStream systemOut, final PrintStream systemErr)
	{
		openFrame = frame;
		openBuffer = buffer;
		systemOutBeforeTheConsole = systemOut;
		systemErrBeforeTheConsole = systemErr;
	}

	/**
	 * Whether a console is open
	 *
	 * @return true while one is
	 */
	public static boolean isOpen()
	{
		return openFrame != null;
	}

	/**
	 * Gives the standard streams back, erases what the console holds, and forgets it. Called when
	 * the console window closes, whichever closed it - the user, or the lock taking the workspace
	 * away
	 */
	public static void ended()
	{
		if (systemOutBeforeTheConsole != null)
		{
			System.setOut(systemOutBeforeTheConsole);
		}
		if (systemErrBeforeTheConsole != null)
		{
			System.setErr(systemErrBeforeTheConsole);
		}
		// the root logger holds a handler bound to the console's stream; rebind it to the stream
		// that is current again, or every log line after this disappears into the closed console
		ConsoleLogRedirectSupport.redirectRootLoggingToCurrentSystemErr();
		if (openBuffer != null)
		{
			openBuffer.clear();
		}
		openFrame = null;
		openBuffer = null;
		systemOutBeforeTheConsole = null;
		systemErrBeforeTheConsole = null;
	}
}
