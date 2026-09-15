/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.clipboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Driven over a real system clipboard - the property under test IS what ends up there - but over a
 * clock this test moves, so nothing here waits out a real twenty seconds
 */
class ClipboardClearWatchdogTest
{

	@BeforeEach
	void requireAClipboard()
	{
		// headless CI can lack a system clipboard entirely; this suite is unit-level and does not
		// go through the #322 harness, so it skips rather than fails when one is not there
		Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
			"a system clipboard needs a display");
	}

	@AfterEach
	void clearTheRealClipboard()
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(""), null);
	}

	@Test
	@DisplayName("before the interval, the clipboard is untouched")
	void checkNow_doesNothing_beforeTheIntervalElapses()
	{
		AtomicLong clock = new AtomicLong();
		ClipboardClearWatchdog watchdog = new ClipboardClearWatchdog(() -> 20, clock::get);
		copyToClipboard("the-secret");

		watchdog.arm("the-secret".toCharArray());
		clock.set(19_000L);
		ClipboardClearWatchdog.Outcome outcome = watchdog.checkNow();

		assertEquals(ClipboardClearWatchdog.Outcome.NOTHING, outcome);
		assertEquals("the-secret", readClipboard());
	}

	@Test
	@DisplayName("once the interval passes, a matching clipboard is cleared")
	void checkNow_clears_whenTheClipboardStillHoldsWhatWasArmed()
	{
		AtomicLong clock = new AtomicLong();
		ClipboardClearWatchdog watchdog = new ClipboardClearWatchdog(() -> 20, clock::get);
		copyToClipboard("the-secret");

		watchdog.arm("the-secret".toCharArray());
		clock.set(20_000L);
		ClipboardClearWatchdog.Outcome outcome = watchdog.checkNow();

		assertEquals(ClipboardClearWatchdog.Outcome.CLEARED, outcome);
		assertEquals("", readClipboard());
	}

	@Test
	@DisplayName("a clipboard changed to something else in the meantime is left alone")
	void checkNow_skipsClearing_whenSomethingElseWasCopiedMeanwhile()
	{
		AtomicLong clock = new AtomicLong();
		ClipboardClearWatchdog watchdog = new ClipboardClearWatchdog(() -> 20, clock::get);
		copyToClipboard("the-secret");
		watchdog.arm("the-secret".toCharArray());
		// the user copied something unrelated before the interval was up
		copyToClipboard("a completely different thing the user copied later");

		clock.set(20_000L);
		ClipboardClearWatchdog.Outcome outcome = watchdog.checkNow();

		assertEquals(ClipboardClearWatchdog.Outcome.SKIPPED, outcome,
			"clearing unconditionally would destroy what the user just copied - that is the "
				+ "whole reason this compares before clearing (#352)");
		assertEquals("a completely different thing the user copied later", readClipboard(),
			"and the content the user actually wants must survive untouched");
	}

	@Test
	@DisplayName("0 never clears, however long it has been")
	void checkNow_neverClears_whenTheIntervalIsOff()
	{
		AtomicLong clock = new AtomicLong();
		ClipboardClearWatchdog watchdog = new ClipboardClearWatchdog(() -> 0, clock::get);
		copyToClipboard("the-secret");

		watchdog.arm("the-secret".toCharArray());
		clock.set(java.util.concurrent.TimeUnit.DAYS.toMillis(1));

		assertEquals(ClipboardClearWatchdog.Outcome.NOTHING, watchdog.checkNow());
		assertEquals("the-secret", readClipboard());
	}

	@Test
	@DisplayName("arming again before the first fire replaces what is remembered, not what is on "
		+ "the clipboard")
	void arm_replacesTheRememberedContent_ratherThanStacking()
	{
		AtomicLong clock = new AtomicLong();
		ClipboardClearWatchdog watchdog = new ClipboardClearWatchdog(() -> 20, clock::get);
		watchdog.arm("first-copy".toCharArray());
		clock.set(5_000L);
		copyToClipboard("second-copy");
		watchdog.arm("second-copy".toCharArray());

		clock.set(25_000L);
		ClipboardClearWatchdog.Outcome outcome = watchdog.checkNow();

		assertEquals(ClipboardClearWatchdog.Outcome.CLEARED, outcome,
			"the second arming is what is checked against - the watchdog does not still compare "
				+ "against the first, already-superseded copy");
		assertEquals("", readClipboard());
	}

	private static void copyToClipboard(final String text)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text),
			null);
	}

	private static String readClipboard()
	{
		try
		{
			return (String)Toolkit.getDefaultToolkit().getSystemClipboard()
				.getData(DataFlavor.stringFlavor);
		}
		catch (Exception unreadable)
		{
			return null;
		}
	}
}
