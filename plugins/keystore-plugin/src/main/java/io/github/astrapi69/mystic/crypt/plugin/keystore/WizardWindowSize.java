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
package io.github.astrapi69.mystic.crypt.plugin.keystore;

import java.awt.Dimension;

/**
 * Works out how large the "Create Key Store..." wizard window opens.
 * <p>
 * Mirrors the conversion plugin's own {@code WizardWindowSize} (that one is owned by the conversion
 * plugin and shared with nothing else, so this is a separate, plugin-local copy rather than a shared
 * dependency - the creation wizard is a small form spread over three steps, of a size comparable to
 * the conversion wizard's own three steps, so the same floor is appropriate here too).
 * <p>
 * The window takes what the wizard asks for, never less than something usable and never more than
 * the screen it opens on.
 */
public final class WizardWindowSize
{

	/** The minimum width the window opens at, whatever the wizard itself asks for */
	public static final int MINIMUM_WIDTH = 520;

	/** The minimum height the window opens at, whatever the wizard itself asks for */
	public static final int MINIMUM_HEIGHT = 360;

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
