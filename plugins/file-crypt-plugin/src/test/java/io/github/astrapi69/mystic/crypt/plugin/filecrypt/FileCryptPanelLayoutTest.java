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
package io.github.astrapi69.mystic.crypt.plugin.filecrypt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Protects the file crypt panel against a window that is narrower than the panel wants to be.
 * <p>
 * Before the fix the entry column of both tabs was laid out with neither
 * {@link java.awt.GridBagConstraints#fill} nor a {@code weightx}, and no text component had a
 * minimum width of its own. {@link java.awt.GridBagLayout} lays a container out at the minimum
 * widths as soon as it is narrower than the grid wants, and a text component reports a minimum
 * width of almost nothing, so the fields did not shrink, they disappeared: the panel wants 713
 * pixels, and at 593 pixels the two text areas measured 4 pixels and every entry field 5 pixels.
 * <p>
 * Both tabs are laid out with the shared tool window form now, whose field column grows with the
 * window and whose text components carry a minimum width of their own: the panel wants 753 pixels,
 * and at 633 pixels the narrowest text component is 406 pixels wide.
 * <p>
 * The two tests below measure the same panel twice: squeezed, where the old layout collapsed, and
 * at its preferred width, so that a fix which only works while squeezed is caught as well.
 */
class FileCryptPanelLayoutTest
{

	static
	{
		System.setProperty("java.awt.headless", "true");
	}

	/** How much narrower than its preferred width the panel is squeezed for the measurement */
	private static final int SQUEEZE = 120;

	/** The width below which a text component is no longer usable */
	private static final int USABLE_WIDTH = 120;

	@Test
	@DisplayName("no text field or text area collapses when the window is 120 pixels too narrow")
	void textComponents_stayUsablyWide_whenTheWindowIsNarrowerThanPreferred()
	{
		FileCryptPanel panel = new FileCryptPanel();
		int squeezedWidth = panel.getPreferredSize().width - SQUEEZE;

		List<JTextComponent> textComponents = layOutAndCollectTextComponents(panel, squeezedWidth);

		assertUsableWidths(textComponents, squeezedWidth);
	}

	@Test
	@DisplayName("no text field or text area collapses at the preferred width either")
	void textComponents_stayUsablyWide_whenTheWindowHasThePreferredWidth()
	{
		FileCryptPanel panel = new FileCryptPanel();
		int preferredWidth = panel.getPreferredSize().width;

		List<JTextComponent> textComponents = layOutAndCollectTextComponents(panel, preferredWidth);

		assertUsableWidths(textComponents, preferredWidth);
	}

	/**
	 * Asserts that every collected text component is at least {@link #USABLE_WIDTH} pixels wide and
	 * names the narrowest one with its measurement when it is not
	 *
	 * @param textComponents
	 *            the text components of the panel, already laid out
	 * @param width
	 *            the width the panel was laid out at, for the failure message
	 */
	private static void assertUsableWidths(List<JTextComponent> textComponents, int width)
	{
		assertFalse(textComponents.isEmpty(), "the panel has text components to measure");
		JTextComponent narrowest = textComponents.stream()
			.min(Comparator.comparingInt(Component::getWidth)).orElseThrow();
		assertTrue(narrowest.getWidth() >= USABLE_WIDTH,
			"at " + width + " pixels wide the narrowest text component is " + name(narrowest)
				+ " with " + narrowest.getWidth() + " pixels, expected at least " + USABLE_WIDTH);
	}

	/**
	 * Lays the panel out at the given width, the way the window manager would, and returns every
	 * text component underneath it
	 *
	 * @param panel
	 *            the panel under test
	 * @param width
	 *            the width to lay the panel out at
	 * @return every {@link JTextComponent} in the panel's component tree, with its laid out bounds
	 */
	private static List<JTextComponent> layOutAndCollectTextComponents(FileCryptPanel panel,
		int width)
	{
		panel.setSize(width, Math.max(400, panel.getPreferredSize().height));
		layOutDeeply(panel);
		List<JTextComponent> textComponents = new ArrayList<>();
		collectTextComponents(panel, textComponents);
		return textComponents;
	}

	/**
	 * Lays out the container and then every container underneath it, so that the components deep in
	 * the tree have their real bounds and not the ones they were born with
	 *
	 * @param container
	 *            the container to lay out
	 */
	private static void layOutDeeply(Container container)
	{
		container.doLayout();
		for (Component child : container.getComponents())
		{
			if (child instanceof Container childContainer)
			{
				layOutDeeply(childContainer);
			}
		}
	}

	/**
	 * Collects every text component in the container's tree into the given list
	 *
	 * @param container
	 *            the container to walk
	 * @param collected
	 *            the list the found text components are added to
	 */
	private static void collectTextComponents(Container container, List<JTextComponent> collected)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof JTextComponent textComponent)
			{
				collected.add(textComponent);
			}
			if (child instanceof Container childContainer)
			{
				collectTextComponents(childContainer, collected);
			}
		}
	}

	/**
	 * The name a component was given, or its class name when it has none
	 *
	 * @param component
	 *            the component to name
	 * @return the component's name for a failure message
	 */
	private static String name(Component component)
	{
		return component.getName() != null
			? component.getName()
			: component.getClass().getSimpleName();
	}
}
