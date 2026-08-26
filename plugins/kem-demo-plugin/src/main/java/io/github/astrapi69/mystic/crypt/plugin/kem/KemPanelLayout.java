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
package io.github.astrapi69.mystic.crypt.plugin.kem;

import java.awt.Dimension;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * The layout rule the two panels of this plugin share: a text area keeps an honest minimum width.
 * <p>
 * Both panels lay their text areas out in a GridBagLayout, and GridBagLayout falls back to the
 * minimum widths as soon as the container is narrower than the grid wants. A text area reports
 * a minimum width of nearly nothing, so without a minimum of its own it does not shrink, it
 * vanishes.
 */
final class KemPanelLayout
{

	/** The width below which a text area is not usable any more */
	static final int MINIMUM_TEXT_WIDTH = 240;

	private KemPanelLayout()
	{
	}

	/**
	 * Wraps a text area in a scroll pane that carries the minimum width. The minimum belongs on the
	 * scroll pane and not on the area: the area is the view inside the viewport, and the layout
	 * manager only ever asks the scroll pane.
	 *
	 * @param textArea
	 *            the area to wrap
	 * @return the scroll pane around it, with a usable minimum width
	 */
	static JScrollPane scrolled(JTextArea textArea)
	{
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setMinimumSize(
			new Dimension(MINIMUM_TEXT_WIDTH, scrollPane.getPreferredSize().height));
		return scrollPane;
	}
}
