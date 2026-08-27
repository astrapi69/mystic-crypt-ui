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

import java.awt.Rectangle;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

/**
 * Works out where the console sits.
 * <p>
 * It used to be sized from the screen, which is the wrong measurement: the console lives inside the
 * application's desktop, not on the desktop of the operating system. In a window that took a third
 * of the screen the console was a stamp in the corner, and on a second monitor it was worse.
 * <p>
 * It is docked along the bottom of the desktop, across its full width, taking the share of the
 * height the settings ask for, and it never becomes so small that nothing can be read in it.
 */
public final class ConsoleDock
{

	/** The console is never docked shorter than this, whatever the divisor says */
	public static final int MINIMUM_HEIGHT = 120;

	private ConsoleDock()
	{
	}

	/**
	 * The bounds the console takes inside a desktop of the given size
	 *
	 * @param desktopWidth
	 *            the width of the desktop the console is docked into
	 * @param desktopHeight
	 *            the height of that desktop
	 * @param heightDivisor
	 *            the share of the height the console takes, from the settings
	 * @return the bounds to give the console
	 */
	public static Rectangle boundsIn(final int desktopWidth, final int desktopHeight,
		final int heightDivisor)
	{
		int divisor = heightDivisor > 0 ? heightDivisor : 1;
		int height = Math.max(MINIMUM_HEIGHT, desktopHeight / divisor);
		// a desktop shorter than the minimum gets a console as tall as it is, rather than one that
		// hangs out of the bottom
		height = Math.min(height, Math.max(1, desktopHeight));
		return new Rectangle(0, Math.max(0, desktopHeight - height), Math.max(1, desktopWidth),
			height);
	}

	/**
	 * Docks the console along the bottom of the desktop it is in
	 *
	 * @param internalFrame
	 *            the console window
	 * @param desktopPane
	 *            the desktop it is docked into
	 * @param heightDivisor
	 *            the share of the height the console takes, from the settings
	 */
	public static void dock(final JInternalFrame internalFrame, final JDesktopPane desktopPane,
		final int heightDivisor)
	{
		if (internalFrame == null || desktopPane == null)
		{
			return;
		}
		internalFrame
			.setBounds(boundsIn(desktopPane.getWidth(), desktopPane.getHeight(), heightDivisor));
	}
}
