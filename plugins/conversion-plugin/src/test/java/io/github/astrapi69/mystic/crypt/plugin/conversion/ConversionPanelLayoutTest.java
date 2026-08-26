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
package io.github.astrapi69.mystic.crypt.plugin.conversion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Keeps the panel usable in a window narrower than it wants.
 * <p>
 * The form was built with a {@link java.awt.GridBagLayout} that had neither a fill nor a weight on
 * the column holding the input fields, and a text field reports a minimum width of nearly nothing.
 * The cell stayed as wide as the button row below it while the field in it fell back to its own
 * minimum: at the preferred width of 549 px the source field measured 422 px, at 429 px, 120 px
 * below preferred, it measured 5 px and was gone from the screen.
 * <p>
 * The measurement walks the whole component tree and takes the narrowest text component, once
 * squeezed and once at the preferred width, so a fix that only holds up in one of the two is caught
 * as well.
 */
class ConversionPanelLayoutTest
{

	/** The width below which a text field is no longer usable */
	private static final int USABLE_WIDTH = 120;

	/** How much narrower than its preferred width the panel is squeezed for the measurement */
	private static final int SQUEEZE = 120;

	@Test
	@DisplayName("no text field collapses when the window is narrower than the panel wants")
	void everyTextComponentStaysUsableWhenTheWindowIsNarrowerThanThePanelWants()
	{
		ConversionPanel panel = new ConversionPanel();
		Dimension preferred = panel.getPreferredSize();

		layOut(panel, preferred.width - SQUEEZE, Math.max(400, preferred.height));

		JTextComponent narrowest = narrowestTextComponent(panel);
		assertTrue(narrowest.getWidth() >= USABLE_WIDTH,
			"squeezed to " + (preferred.width - SQUEEZE) + " px, the narrowest text component is "
				+ describe(narrowest) + ", which is below the usable " + USABLE_WIDTH + " px");
	}

	@Test
	@DisplayName("no text field collapses at the width the panel asks for")
	void everyTextComponentStaysUsableAtThePreferredWidth()
	{
		ConversionPanel panel = new ConversionPanel();
		Dimension preferred = panel.getPreferredSize();

		layOut(panel, preferred.width, Math.max(400, preferred.height));

		JTextComponent narrowest = narrowestTextComponent(panel);
		assertTrue(narrowest.getWidth() >= USABLE_WIDTH,
			"at the preferred width of " + preferred.width
				+ " px, the narrowest text component is " + describe(narrowest)
				+ ", which is below the usable " + USABLE_WIDTH + " px");
	}

	@Test
	@DisplayName("every text field asks for a minimum width it is still usable at")
	void everyTextComponentReportsAUsableMinimumWidth()
	{
		ConversionPanel panel = new ConversionPanel();

		for (JTextComponent textComponent : textComponents(panel))
		{
			assertTrue(textComponent.getMinimumSize().width >= USABLE_WIDTH,
				textComponent.getName() + " asks for a minimum width of only "
					+ textComponent.getMinimumSize().width + " px, so every layout that falls back"
					+ " to minimum widths makes it disappear");
		}
	}

	/**
	 * Gives the panel the asked-for size and lays out the whole tree below it, parents before
	 * children, so that every component has the bounds it would have on screen
	 *
	 * @param panel
	 *            the panel to lay out
	 * @param width
	 *            the width to give it
	 * @param height
	 *            the height to give it
	 */
	private static void layOut(ConversionPanel panel, int width, int height)
	{
		panel.setSize(width, height);
		panel.doLayout();
		layOutChildren(panel);
	}

	/**
	 * Lays out every container below the given one, parents before children
	 *
	 * @param container
	 *            the container whose children are laid out
	 */
	private static void layOutChildren(Container container)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof Container nested)
			{
				nested.doLayout();
				layOutChildren(nested);
			}
		}
	}

	/**
	 * Walks the whole component tree and answers the text component that came out narrowest
	 *
	 * @param panel
	 *            the panel to walk
	 * @return the narrowest text component below the panel
	 */
	private static JTextComponent narrowestTextComponent(ConversionPanel panel)
	{
		JTextComponent narrowest = null;
		for (JTextComponent textComponent : textComponents(panel))
		{
			if (narrowest == null || textComponent.getWidth() < narrowest.getWidth())
			{
				narrowest = textComponent;
			}
		}
		return narrowest;
	}

	/**
	 * Answers every text component below the given container
	 *
	 * @param container
	 *            the container to walk
	 * @return every text component below the container, in the order they were found
	 */
	private static List<JTextComponent> textComponents(Container container)
	{
		List<JTextComponent> collected = new ArrayList<>();
		collectTextComponents(container, collected);
		assertFalse(collected.isEmpty(),
			"the panel has to hold text components, otherwise this test proves nothing");
		return collected;
	}

	/**
	 * Collects every text component below the given container
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
			if (child instanceof Container nested)
			{
				collectTextComponents(nested, collected);
			}
		}
	}

	/**
	 * Names a component and its measured width, so a failure says which one collapsed
	 *
	 * @param component
	 *            the component to describe
	 * @return the name of the component and its width in pixels
	 */
	private static String describe(Component component)
	{
		return (component.getName() == null
			? component.getClass().getSimpleName()
			: component.getName()) + " at " + component.getWidth() + " px";
	}
}
