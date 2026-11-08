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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;

/**
 * Grows the window a component is in to the size its content asks for (#835). A tool window of the
 * host is packed when it opens, while a window that runs an action shows only the sentence in the
 * place of its answer (#820); what appears later - the answer, a failure, the details - would
 * otherwise lie below its visible part. The window never shrinks, so a size the person chose stays,
 * and an internal window grows no further than the desktop it sits on.
 */
final class WindowFit
{

	private WindowFit()
	{
	}

	/**
	 * Grows the window around a component to what its content asks for
	 *
	 * @param inside
	 *            a component of the window; outside any window nothing happens
	 */
	static void growToShow(final Component inside)
	{
		Container window = windowOf(inside);
		if (window == null)
		{
			return;
		}
		Dimension wanted = window.getPreferredSize();
		Dimension now = window.getSize();
		int width = Math.max(now.width, wanted.width);
		int height = Math.max(now.height, wanted.height);
		Container desktop = window.getParent();
		if (window instanceof JInternalFrame && desktop != null && desktop.getHeight() > 0)
		{
			width = Math.min(width, Math.max(now.width, desktop.getWidth() - window.getX()));
			height = Math.min(height, Math.max(now.height, desktop.getHeight() - window.getY()));
		}
		if (width != now.width || height != now.height)
		{
			window.setSize(width, height);
		}
	}

	private static Container windowOf(final Component inside)
	{
		Container frame = (Container)SwingUtilities.getAncestorOfClass(JInternalFrame.class,
			inside);
		return frame != null ? frame : SwingUtilities.getWindowAncestor(inside);
	}
}
