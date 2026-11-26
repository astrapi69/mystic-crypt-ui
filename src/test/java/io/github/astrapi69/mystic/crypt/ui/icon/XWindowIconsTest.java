/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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
package io.github.astrapi69.mystic.crypt.ui.icon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * How {@link XWindowIcons} finds a window by its title in what {@code wmctrl -l} lists. On develop
 * {@code ad2471d2} (run 45821007085) {@code wmctrl -l} ended with exit code 1 and "X Error of
 * failed request: BadWindow (invalid Window parameter)" while the application was starting: a
 * window it had listed went away before it read the window's title, and the lookup failed at once
 * instead of reading the list again.
 */
class XWindowIconsTest
{

	private static final String TITLE = "Enter your credentials";

	private static final String LISTING = "0x00400007  0 vm " + TITLE + "\n";

	private static final IOException BAD_WINDOW = new IOException(
		"wmctrl -l ended with exit code 1: X Error of failed request:  BadWindow (invalid Window parameter)");

	@Test
	@DisplayName("a listing that fails because a window went away is read again, and the window is found")
	void aFailedListingIsReadAgain() throws Exception
	{
		AtomicInteger reads = new AtomicInteger();

		String windowId = XWindowIcons.awaitWindowId(TITLE, () -> {
			if (reads.incrementAndGet() == 1)
			{
				throw BAD_WINDOW;
			}
			return LISTING;
		}, 5_000);

		assertEquals("0x00400007", windowId);
		assertEquals(2, reads.get(), "one failed read, one that found the window");
	}

	@Test
	@DisplayName("a listing that keeps failing ends at the deadline and names the last failure")
	void aListingThatKeepsFailingNamesItsFailure()
	{
		IllegalStateException failure = assertThrows(IllegalStateException.class,
			() -> XWindowIcons.awaitWindowId(TITLE, () -> {
				throw BAD_WINDOW;
			}, 300));

		assertTrue(failure.getMessage().contains("BadWindow"), failure.getMessage());
		assertSame(BAD_WINDOW, failure.getCause());
	}

	@Test
	@DisplayName("a window that never appears ends at the deadline and shows what was listed")
	void aWindowThatNeverAppearsShowsTheListing()
	{
		IllegalStateException failure = assertThrows(IllegalStateException.class,
			() -> XWindowIcons.awaitWindowId("Create your master key", () -> LISTING, 300));

		assertTrue(failure.getMessage().contains("no window titled 'Create your master key'"),
			failure.getMessage());
		assertTrue(failure.getMessage().contains(TITLE), "the listing: " + failure.getMessage());
	}
}
