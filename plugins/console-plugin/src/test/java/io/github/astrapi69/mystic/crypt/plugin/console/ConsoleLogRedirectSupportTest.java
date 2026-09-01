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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link ConsoleLogRedirectSupport} has to make {@link java.util.logging.Logger} output follow
 * wherever {@link System#err} currently points - the JDK's default root handler captures
 * {@code System.err} once, at {@code LogManager} bootstrap, so a plain field reassignment (what
 * {@code ConsolePanel} does) never reaches it on its own
 */
class ConsoleLogRedirectSupportTest
{

	private final Logger rootLogger = Logger.getLogger("");
	private Handler[] originalHandlers;
	private PrintStream originalErr;

	@BeforeEach
	void captureOriginalState()
	{
		originalHandlers = rootLogger.getHandlers();
		originalErr = System.err;
	}

	@AfterEach
	void restoreOriginalState()
	{
		System.setErr(originalErr);
		for (Handler handler : rootLogger.getHandlers())
		{
			rootLogger.removeHandler(handler);
		}
		for (Handler handler : originalHandlers)
		{
			rootLogger.addHandler(handler);
		}
	}

	@Test
	void aLoggerCallBeforeRedirectingDoesNotReachTheNewStream()
	{
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));

		Logger.getLogger("some.test.logger").severe("before-redirect-marker");

		assertFalse(captured.toString(StandardCharsets.UTF_8).contains("before-redirect-marker"),
			"without the redirect, the default handler must keep writing to the stream it "
				+ "captured at bootstrap, not the reassigned System.err - the premise this fix "
				+ "builds on (#133)");
	}

	@Test
	void aLoggerCallAfterRedirectingReachesTheCurrentSystemErr()
	{
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));

		ConsoleLogRedirectSupport.redirectRootLoggingToCurrentSystemErr();
		Logger.getLogger("some.test.logger").severe("after-redirect-marker");

		assertTrue(captured.toString(StandardCharsets.UTF_8).contains("after-redirect-marker"),
			"a logger call after redirecting must reach the current System.err, not the stream "
				+ "the default handler captured at JVM bootstrap (#133)");
	}

}
