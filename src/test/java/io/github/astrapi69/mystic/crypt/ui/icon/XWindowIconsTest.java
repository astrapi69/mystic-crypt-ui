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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * How {@link XWindowIcons} finds a window by its title in the X server's window tree, as
 * {@code xwininfo -root -tree} prints it.
 * <p>
 * It used to ask {@code wmctrl -l}, which lists what the window manager keeps in
 * {@code _NET_CLIENT_LIST} and reads each window's title. On develop {@code ad2471d2} (run
 * 45821007085) that failed once with "BadWindow (invalid Window parameter)" while the application
 * started; on {@code 0d9309eb} (run 45830430466), after #1201 read the list again on a failure, it
 * failed for the whole ten seconds in three tests: the list held a window that no longer existed.
 * The window tree comes from the X server itself, which lists no window that is gone.
 */
class XWindowIconsTest
{

	private static final String TITLE = "Enter your credentials";

	private static final String TREE = """

		xwininfo: Window id: 0x3b5 (the root window) (has no name)

		  Root window id: 0x3b5 (the root window) (has no name)
		  Parent window id: 0x0 (none)
		     4 children:
		     0x400046 (has no name): ()  198x73+0+22  +1+23
		     0x400034 "Create your master key (2)": ("sun-awt-X11-XDialogPeer" "MysticCryptApplication")  198x73+0+22  +1+23
		     0x400007 "Enter your credentials": ("sun-awt-X11-XDialogPeer" "MysticCryptApplication")  198x73+0+22  +1+23
		        1 child:
		        0x400008 (has no name): ()  1x1+-1+-1  +0+22
		""";

	private static final IOException BAD_WINDOW = new IOException(
		"xwininfo -root -tree ended with exit code 1: X Error of failed request:  BadWindow (invalid Window parameter)");

	@Test
	@DisplayName("the window is found by its quoted title in the tree; a window without a name or with another title is not it")
	void theWindowIsFoundByItsTitleInTheTree()
	{
		assertEquals(Optional.of("0x400007"), XWindowIcons.windowIdIn(TREE, TITLE));
		assertEquals(Optional.of("0x400034"),
			XWindowIcons.windowIdIn(TREE, "Create your master key (2)"));
		assertEquals(Optional.empty(), XWindowIcons.windowIdIn(TREE, "Create your master key"));
		assertEquals(Optional.empty(), XWindowIcons.windowIdIn(TREE, "has no name"));
	}

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
			return TREE;
		}, 5_000);

		assertEquals("0x400007", windowId);
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
			() -> XWindowIcons.awaitWindowId("Create your master key", () -> TREE, 300));

		assertTrue(failure.getMessage().contains("no window titled 'Create your master key'"),
			failure.getMessage());
		assertTrue(failure.getMessage().contains(TITLE), "the listing: " + failure.getMessage());
	}
}
