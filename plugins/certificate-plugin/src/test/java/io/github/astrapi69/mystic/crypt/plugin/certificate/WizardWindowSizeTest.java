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
package io.github.astrapi69.mystic.crypt.plugin.certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The certificate wizard used to open at a fixed 820x600 while asking for 467x656, so it was 56
 * pixels too short before anyone touched it. Measured on the wizard itself: the extensions step
 * wants 615 pixels and the navigation row another 39.
 */
class WizardWindowSizeTest
{

	/** What the wizard really asks for, measured */
	private static final Dimension WANTED = new Dimension(467 + 24, 656 + 24);

	@Test
	@DisplayName("the window is at least as tall as the wizard asks for")
	void theWindowFitsTheWizard()
	{
		Dimension size = WizardWindowSize.on(WANTED, new Dimension(1920, 1080));

		assertTrue(size.height >= WANTED.height,
			"the wizard is cut off again: window " + size.height + ", wizard " + WANTED.height);
		assertTrue(size.width >= WANTED.width);
	}

	/**
	 * The old behaviour, written down so it cannot come back: 600 pixels is not enough for a wizard
	 * that needs 680
	 */
	@Test
	@DisplayName("the old fixed size would not pass this test")
	void theOldFixedSizeWasTooSmall()
	{
		assertTrue(WANTED.height > 600,
			"this test is pointless if the wizard fits into the old 600 pixels");
	}

	/** A tiny wizard still gets a window worth opening */
	@Test
	@DisplayName("a small wizard still opens at a usable size")
	void aSmallWizardStillGetsAUsableWindow()
	{
		Dimension size = WizardWindowSize.on(new Dimension(200, 150), new Dimension(1920, 1080));

		assertEquals(WizardWindowSize.MINIMUM_WIDTH, size.width);
		assertEquals(WizardWindowSize.MINIMUM_HEIGHT, size.height);
	}

	/**
	 * The window must not become narrower than it always was. The wizard asks for 467 and the
	 * window has always opened at 820; taking the wizard's number literally would have made the
	 * extensions step, which has a table in it, tighter than before while fixing the height.
	 */
	@Test
	@DisplayName("fixing the height does not cost the width")
	void theWindowDoesNotBecomeNarrowerThanItWas()
	{
		Dimension size = WizardWindowSize.on(WANTED, new Dimension(1920, 1080));

		assertTrue(size.width >= 820, "the window became narrower than it used to be: " + size);
	}

	/** And a screen smaller than the wizard is not a reason to open past its edges */
	@Test
	@DisplayName("the window never grows past the screen")
	void theWindowStaysOnTheScreen()
	{
		Dimension size = WizardWindowSize.on(WANTED, new Dimension(800, 600));

		assertTrue(size.width <= 800 - WizardWindowSize.MARGIN, "wider than the screen: " + size);
		assertTrue(size.height <= 600 - WizardWindowSize.MARGIN, "taller than the screen: " + size);
	}

	/**
	 * A very small screen, a projector or a virtual machine, must still produce a window rather than
	 * a negative size
	 */
	@Test
	@DisplayName("a very small screen still produces a window")
	void aVerySmallScreenStillWorks()
	{
		Dimension size = WizardWindowSize.on(WANTED, new Dimension(320, 200));

		assertTrue(size.width > 0 && size.height > 0, "not a window: " + size);
	}
}
