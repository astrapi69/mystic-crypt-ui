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

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * Makes {@link java.util.logging.Logger} output follow wherever {@link System#err} currently
 * points, instead of the stream the JDK's default root handler captured once at bootstrap.
 * <p>
 * {@link ConsolePanel} redirects {@code System.out}/{@code System.err} to the console's text area
 * by reassigning those fields - but the JDK's default root {@link ConsoleHandler} captured the
 * original {@code System.err} once, when the {@code LogManager} first bootstrapped, long before
 * the console window ever opens. Reassigning the field does nothing for a handler that already
 * holds its own reference to the old stream, so every {@link Logger} call keeps writing to the
 * real terminal forever, never reaching the panel (#133).
 * <p>
 * The fix is not to mutate the existing handler ({@link java.util.logging.StreamHandler#setOutputStream}
 * flushes and closes whatever stream the handler is currently pointed at, which for the default
 * handler is the real {@code System.err} - not something to close as a side effect of a logging
 * fix). Instead, the existing root handlers are removed and a fresh {@link ConsoleHandler} is
 * added, which binds {@code System.err} at construction time rather than once at JVM bootstrap -
 * called after the redirect has already happened, it binds the redirected stream.
 */
final class ConsoleLogRedirectSupport
{

	private ConsoleLogRedirectSupport()
	{
	}

	/**
	 * Removes every handler currently on the root logger and adds a fresh {@link ConsoleHandler},
	 * which binds whatever {@link System#err} points to right now
	 */
	static void redirectRootLoggingToCurrentSystemErr()
	{
		Logger rootLogger = Logger.getLogger("");
		for (Handler handler : rootLogger.getHandlers())
		{
			rootLogger.removeHandler(handler);
		}
		rootLogger.addHandler(new ConsoleHandler());
	}

}
