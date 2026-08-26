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

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.text.JTextComponent;

/**
 * Measures how wide the text components of a panel really are once the panel is laid out at a
 * given size. A layout that only works at its preferred size is the defect this exists for:
 * GridBagLayout falls back to the minimum widths as soon as the container is narrower than the
 * grid wants, and a text component without an honest minimum shrinks to a few pixels there.
 */
final class PanelWidths
{

	private PanelWidths()
	{
	}

	/**
	 * Lays the whole component tree out at the given size and returns the width of its narrowest
	 * text component
	 *
	 * @param root
	 *            the panel to lay out
	 * @param width
	 *            the width the panel is given
	 * @param height
	 *            the height the panel is given
	 * @return the width in pixels of the narrowest {@link JTextComponent} below the panel
	 */
	static int narrowestTextComponent(JComponent root, int width, int height)
	{
		root.setSize(width, height);
		layoutTree(root);
		int narrowest = Integer.MAX_VALUE;
		for (JTextComponent textComponent : textComponentsOf(root))
		{
			narrowest = Math.min(narrowest, textComponent.getWidth());
		}
		return narrowest;
	}

	/**
	 * Describes every text component below the panel with its name and its current width, so a
	 * failing assertion names the component that vanished
	 *
	 * @param root
	 *            the panel that was laid out
	 * @return one line per text component
	 */
	static String widthReport(JComponent root)
	{
		StringBuilder report = new StringBuilder();
		for (JTextComponent textComponent : textComponentsOf(root))
		{
			report.append(textComponent.getName()).append('=').append(textComponent.getWidth())
				.append("px ");
		}
		return report.toString().trim();
	}

	/**
	 * Lays out a container and then, recursively, every container below it. A single
	 * {@code doLayout} on the panel only positions its own children; the children need their own
	 * pass before their widths can be read.
	 *
	 * @param component
	 *            the component whose subtree is laid out
	 */
	private static void layoutTree(Component component)
	{
		if (component instanceof Container container)
		{
			container.doLayout();
			for (Component child : container.getComponents())
			{
				layoutTree(child);
			}
		}
	}

	/**
	 * Collects every text component below the given component, in the order the tree holds them
	 *
	 * @param root
	 *            the component whose subtree is walked
	 * @return every {@link JTextComponent} below it
	 */
	static List<JTextComponent> textComponentsOf(Component root)
	{
		List<JTextComponent> found = new ArrayList<>();
		collect(root, found);
		return found;
	}

	private static void collect(Component component, List<JTextComponent> found)
	{
		if (component instanceof JTextComponent textComponent)
		{
			found.add(textComponent);
		}
		if (component instanceof Container container)
		{
			for (Component child : container.getComponents())
			{
				collect(child, found);
			}
		}
	}
}
