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
package io.github.astrapi69.mystic.crypt.ui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The geometry behind putting a window where the user is looking. A second monitor to the right of
 * the first one starts at x = 1920, so its centre is not near 960 - which is exactly the mistake
 * that put every dialog of this application on the first screen.
 */
class ScreenPlacementTest
{

	private static final Rectangle FIRST_SCREEN = new Rectangle(0, 0, 1920, 1080);

	private static final Rectangle SECOND_SCREEN = new Rectangle(1920, 0, 2560, 1440);

	@Test
	@DisplayName("a window is centred on the screen it is given, not on the first one")
	void aWindowIsCentredOnTheScreenItIsGiven()
	{
		Point placed = ScreenPlacement.centeredIn(SECOND_SCREEN, new Dimension(800, 600));

		assertEquals(new Point(1920 + (2560 - 800) / 2, (1440 - 600) / 2), placed,
			"the window did not land on the second screen");
	}

	@Test
	@DisplayName("centring on the first screen still puts the window in its middle")
	void centringOnTheFirstScreenStillPutsTheWindowInItsMiddle()
	{
		Point placed = ScreenPlacement.centeredIn(FIRST_SCREEN, new Dimension(920, 380));

		assertEquals(new Point((1920 - 920) / 2, (1080 - 380) / 2), placed);
	}

	@Test
	@DisplayName("a window larger than the screen keeps its top left corner reachable")
	void aWindowLargerThanTheScreenKeepsItsTopLeftCornerReachable()
	{
		Point placed = ScreenPlacement.centeredIn(FIRST_SCREEN, new Dimension(2400, 1600));

		assertEquals(new Point(0, 0), placed,
			"a window wider than the screen was pushed off it, title bar first");
	}

	@Test
	@DisplayName("the space the desktop reserves is not offered to a window")
	void theSpaceTheDesktopReservesIsNotOfferedToAWindow()
	{
		Rectangle usable = ScreenPlacement.withoutInsets(SECOND_SCREEN, new Insets(28, 0, 60, 0));

		assertEquals(new Rectangle(1920, 28, 2560, 1440 - 88), usable);
	}

	@Test
	@DisplayName("a window that fills a screen gets exactly the usable area")
	void aWindowThatFillsAScreenGetsExactlyTheUsableArea()
	{
		Rectangle usable = ScreenPlacement.withoutInsets(SECOND_SCREEN, new Insets(0, 0, 60, 0));

		assertEquals(new Dimension(2560, 1380), ScreenPlacement.fillingSizeOf(usable));
	}

}
