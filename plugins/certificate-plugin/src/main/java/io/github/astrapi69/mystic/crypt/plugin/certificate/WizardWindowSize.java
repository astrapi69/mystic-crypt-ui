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

import java.awt.Dimension;

/**
 * Works out how large the certificate wizard window opens.
 * <p>
 * It used to open at a fixed 820x600 while the wizard asks for 467x656, so the window was 56 pixels
 * too short from the first moment: the extensions step alone wants 615 pixels and the navigation
 * row another 39, and what did not fit was simply cut off at the bottom.
 * <p>
 * The window now takes what the wizard asks for, never less than something usable and never more
 * than the screen it opens on.
 */
public final class WizardWindowSize
{

	/**
	 * The width the window has always opened at. The wizard asks for less, but the extensions step
	 * has a table in it and the room was never the problem: only the height was.
	 */
	public static final int MINIMUM_WIDTH = 820;

	/** The same for the height, which is where it was cut off */
	public static final int MINIMUM_HEIGHT = 520;

	/** How much of the screen is left free around the window */
	public static final int MARGIN = 80;

	private WizardWindowSize()
	{
	}

	/**
	 * The size the window opens at
	 *
	 * @param wanted
	 *            what the wizard asks for
	 * @param screen
	 *            the screen it opens on
	 * @return the size to give the window
	 */
	public static Dimension on(final Dimension wanted, final Dimension screen)
	{
		int width = Math.max(MINIMUM_WIDTH, wanted.width);
		int height = Math.max(MINIMUM_HEIGHT, wanted.height);
		// a window larger than the screen hides its own buttons, which is what the scroll pane
		// around the wizard is there for
		width = Math.min(width, Math.max(MINIMUM_WIDTH / 2, screen.width - MARGIN));
		height = Math.min(height, Math.max(MINIMUM_HEIGHT / 2, screen.height - MARGIN));
		return new Dimension(width, height);
	}
}
