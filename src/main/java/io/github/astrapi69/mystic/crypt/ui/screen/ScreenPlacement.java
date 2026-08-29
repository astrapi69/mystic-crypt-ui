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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

/**
 * Puts a window on the screen the user is actually looking at.
 * <p>
 * Centring on "the screen" means the first one as far as swing is concerned, which on a desk with
 * two or three monitors is regularly not the one the application is on: the dialog appears on the
 * left-hand monitor while the window that opened it sits on the right. Everything here works from
 * the screen of a component that is already where the user is - the parent window, or failing that
 * the pointer.
 * <p>
 * The geometry is kept apart from the toolkit on purpose, so it can be checked without a display.
 */
public final class ScreenPlacement
{

	private ScreenPlacement()
	{
	}

	/**
	 * The part of the given screen a window may use: its bounds without the space the desktop keeps
	 * for itself, a task bar or a dock.
	 *
	 * @param configuration
	 *            the screen, or null for the default one
	 * @return the usable area, in the coordinates of the whole desktop
	 */
	public static Rectangle usableBoundsOf(final GraphicsConfiguration configuration)
	{
		GraphicsConfiguration screen = configuration != null
			? configuration
			: GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
				.getDefaultConfiguration();
		Rectangle bounds = screen.getBounds();
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(screen);
		return withoutInsets(bounds, insets);
	}

	/**
	 * Takes what the desktop reserves off the given bounds
	 *
	 * @param bounds
	 *            the whole screen
	 * @param insets
	 *            what the desktop keeps for itself on each side
	 * @return what is left for a window
	 */
	public static Rectangle withoutInsets(final Rectangle bounds, final Insets insets)
	{
		return new Rectangle(bounds.x + insets.left, bounds.y + insets.top,
			bounds.width - insets.left - insets.right, bounds.height - insets.top - insets.bottom);
	}

	/**
	 * Where a window of the given size sits when it is centred in the given area.
	 * <p>
	 * A window larger than the area is put at its top left corner rather than half off it: a title
	 * bar that cannot be reached is worse than a window that overflows to the right.
	 *
	 * @param usableBounds
	 *            the area to centre in
	 * @param windowSize
	 *            the size of the window
	 * @return the top left corner the window is to be placed at
	 */
	public static Point centeredIn(final Rectangle usableBounds, final Dimension windowSize)
	{
		int x = usableBounds.x + Math.max(0, (usableBounds.width - windowSize.width) / 2);
		int y = usableBounds.y + Math.max(0, (usableBounds.height - windowSize.height) / 2);
		return new Point(x, y);
	}

	/**
	 * The size a window is given when it is to fill the given area
	 *
	 * @param usableBounds
	 *            the area to fill
	 * @return the size of the window
	 */
	public static Dimension fillingSizeOf(final Rectangle usableBounds)
	{
		return new Dimension(usableBounds.width, usableBounds.height);
	}

	/**
	 * The screen the given component is on, the screen the pointer is on when the component is not
	 * on one yet, and the default screen when there is no pointer either.
	 *
	 * @param reference
	 *            the component that says where the user is, may be null
	 * @return the screen to place on, never null
	 */
	public static GraphicsConfiguration screenOf(final Component reference)
	{
		if (reference != null && reference.getGraphicsConfiguration() != null)
		{
			return reference.getGraphicsConfiguration();
		}
		return screenUnderThePointer();
	}

	/**
	 * The screen the pointer is on, or the default screen when that cannot be told
	 *
	 * @return the screen, never null
	 */
	public static GraphicsConfiguration screenUnderThePointer()
	{
		GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		PointerInfo pointer = MouseInfo.getPointerInfo();
		if (pointer != null)
		{
			Point location = pointer.getLocation();
			for (GraphicsDevice device : environment.getScreenDevices())
			{
				GraphicsConfiguration configuration = device.getDefaultConfiguration();
				if (configuration.getBounds().contains(location))
				{
					return configuration;
				}
			}
		}
		return environment.getDefaultScreenDevice().getDefaultConfiguration();
	}

	/**
	 * Centres the given window on the screen the given component is on. This is what every dialog
	 * in this application uses instead of centring on the first screen.
	 *
	 * @param window
	 *            the window to place
	 * @param reference
	 *            the component that says which screen that is, usually the window that opens the
	 *            dialog; may be null, then the pointer decides
	 */
	public static void centerOnScreenOf(final Window window, final Component reference)
	{
		Rectangle usableBounds = usableBoundsOf(screenOf(reference));
		window.setLocation(centeredIn(usableBounds, window.getSize()));
	}

	/**
	 * Gives the given window the whole usable area of the screen the given component is on
	 *
	 * @param window
	 *            the window to place and size
	 * @param reference
	 *            the component that says which screen that is; may be null, then the pointer
	 *            decides
	 */
	public static void fillScreenOf(final Window window, final Component reference)
	{
		fillScreen(window, screenOf(reference));
	}

	/**
	 * Gives the given window the whole usable area of the given screen
	 *
	 * @param window
	 *            the window to place and size
	 * @param screen
	 *            the screen it is to fill; null falls back to the screen under the pointer
	 */
	public static void fillScreen(final Window window, final GraphicsConfiguration screen)
	{
		Rectangle usableBounds = usableBoundsOf(screen != null ? screen : screenUnderThePointer());
		window.setSize(fillingSizeOf(usableBounds));
		window.setLocation(usableBounds.x, usableBounds.y);
	}

}
