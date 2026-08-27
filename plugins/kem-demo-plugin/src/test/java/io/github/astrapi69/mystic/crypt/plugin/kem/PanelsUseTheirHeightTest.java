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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A window that is made bigger has to show more, or there was no reason to make it bigger.
 * <p>
 * Measured before this was fixed, at 780x900: the demo showed a ciphertext 48 pixels high and two
 * secrets at 33, and the exchange showed eight areas of 54 pixels with some 450 pixels of empty
 * space below them. The rows carried no vertical weight, so the height went nowhere.
 */
class PanelsUseTheirHeightTest
{

	private static void layOut(Container container, int width, int height)
	{
		container.setSize(width, height);
		container.doLayout();
		layOutChildren(container);
	}

	private static void layOutChildren(Container container)
	{
		for (Component component : container.getComponents())
		{
			if (component instanceof Container child)
			{
				child.doLayout();
				layOutChildren(child);
			}
		}
	}

	private static List<JScrollPane> areasOf(Container container)
	{
		List<JScrollPane> found = new ArrayList<>();
		for (Component component : container.getComponents())
		{
			if (component instanceof JScrollPane pane
				&& pane.getViewport().getView() instanceof javax.swing.text.JTextComponent)
			{
				found.add(pane);
			}
			if (component instanceof Container child)
			{
				found.addAll(areasOf(child));
			}
		}
		return found;
	}

	private static int totalHeightOf(JPanel panel, int width, int height)
	{
		layOut(panel, width, height);
		return areasOf(panel).stream().mapToInt(Component::getHeight).sum();
	}

	/**
	 * The demo: twice the height shows more of the ciphertext and the secrets, not the same three
	 * boxes with more grey around them
	 */
	@Test
	@DisplayName("the demo shows more when the window is taller")
	void theDemoUsesTheHeightItIsGiven()
	{
		int atNormalHeight = totalHeightOf(new KemDemoPanel(), 780, 400);
		int atDoubleHeight = totalHeightOf(new KemDemoPanel(), 780, 900);

		assertTrue(atDoubleHeight > atNormalHeight + 200,
			"the areas did not grow with the window: " + atNormalHeight + " px at 400, "
				+ atDoubleHeight + " px at 900");
	}

	/**
	 * And the exchange, whose fields are the ones people actually read and paste
	 */
	@Test
	@DisplayName("the exchange shows more when the window is taller")
	void theExchangeUsesTheHeightItIsGiven()
	{
		int atNormalHeight = totalHeightOf(new KeyExchangePanel(), 780, 500);
		int atDoubleHeight = totalHeightOf(new KeyExchangePanel(), 780, 1000);

		assertTrue(atDoubleHeight > atNormalHeight + 200,
			"the areas did not grow with the window: " + atNormalHeight + " px at 500, "
				+ atDoubleHeight + " px at 1000");
	}

	/**
	 * The other half of the bargain: a window too short for everything must not put a field out of
	 * reach, so each tab sits in something that scrolls
	 */
	@Test
	@DisplayName("a window too short to hold everything scrolls instead of cutting it off")
	void nothingIsPutOutOfReach()
	{
		KeyExchangePanel panel = new KeyExchangePanel();
		layOut(panel, 500, 260);

		List<JScrollPane> scrollPanes = new ArrayList<>();
		collectScrollPanes(panel, scrollPanes);
		assertTrue(
			scrollPanes.stream()
				.anyMatch(pane -> !(pane.getViewport()
					.getView() instanceof javax.swing.text.JTextComponent)),
			"the tabs are not in anything that can scroll, so a short window hides the last field");
	}

	private static void collectScrollPanes(Container container, List<JScrollPane> found)
	{
		for (Component component : container.getComponents())
		{
			if (component instanceof JScrollPane pane)
			{
				found.add(pane);
			}
			if (component instanceof Container child)
			{
				collectScrollPanes(child, found);
			}
		}
	}
}
