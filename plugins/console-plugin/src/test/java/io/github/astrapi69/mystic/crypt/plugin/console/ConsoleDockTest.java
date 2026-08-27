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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The console is docked into the application's desktop, so it is measured against that desktop.
 * <p>
 * It used to be measured against the screen: in a window taking a third of the screen the console
 * came out as a stamp in the bottom corner, and its width had nothing to do with the window it sat
 * in.
 */
class ConsoleDockTest
{

	/**
	 * The width is the desktop's, the height is the configured share of it, and it sits on the
	 * bottom edge
	 */
	@ParameterizedTest(name = "desktop {0}x{1}, divisor {2}")
	@CsvSource({ "1200, 900, 3", "800, 600, 4", "515, 1000, 3", "1920, 1080, 2" })
	@DisplayName("the console spans the desktop and sits on its bottom edge")
	void theConsoleIsMeasuredAgainstTheDesktop(int width, int height, int divisor)
	{
		Rectangle bounds = ConsoleDock.boundsIn(width, height, divisor);

		assertEquals(width, bounds.width, "the console must span the desktop");
		assertEquals(Math.max(ConsoleDock.MINIMUM_HEIGHT, height / divisor), bounds.height);
		assertEquals(height, bounds.y + bounds.height, "the console must sit on the bottom edge");
		assertEquals(0, bounds.x);
	}

	/**
	 * A large divisor once produced a console too short to read anything in
	 */
	@Test
	@DisplayName("a large divisor does not shrink it below what can be read")
	void itNeverBecomesUnreadable()
	{
		Rectangle bounds = ConsoleDock.boundsIn(1200, 900, 20);

		assertEquals(ConsoleDock.MINIMUM_HEIGHT, bounds.height);
		assertEquals(900, bounds.y + bounds.height);
	}

	/**
	 * And a desktop shorter than that minimum gets a console as tall as it is, rather than one
	 * hanging out of the bottom
	 */
	@Test
	@DisplayName("a desktop shorter than the minimum still gets a console that fits")
	void itFitsIntoAVerySmallDesktop()
	{
		Rectangle bounds = ConsoleDock.boundsIn(400, 80, 3);

		assertTrue(bounds.height <= 80, "the console hangs out of the desktop: " + bounds);
		assertEquals(0, bounds.y);
	}

	/**
	 * A divisor of zero comes from a hand-edited settings file and must not divide by zero
	 */
	@Test
	@DisplayName("a divisor of zero is not a crash")
	void aBrokenDivisorIsSurvived()
	{
		assertEquals(900, ConsoleDock.boundsIn(1200, 900, 0).height);
	}

	/**
	 * And the frame really receives those bounds
	 */
	@Test
	@DisplayName("the window is placed where the calculation says")
	void theWindowIsPlacedThere()
	{
		JDesktopPane desktopPane = new JDesktopPane();
		desktopPane.setSize(1000, 700);
		JInternalFrame console = new JInternalFrame("Console");

		ConsoleDock.dock(console, desktopPane, 4);

		assertEquals(new Rectangle(0, 525, 1000, 175), console.getBounds());
	}
}
