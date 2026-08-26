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

import java.awt.Dimension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the layout of {@link KeyExchangePanel} against a window that is narrower than the panel
 * wants. Both tabs are measured: the tabbed pane gives every tab the same content bounds, so one
 * layout pass covers the receiving and the sending side.
 * <p>
 * Before the fix the one column both tabs are built from was filled but carried no
 * {@code weightx}, so the fields never followed the window: at the preferred width of 614 pixels
 * each area was 594 pixels wide, 120 pixels narrower, at 494, they fell back to whatever the widest
 * button row of their tab needs - 349 pixels on the receiving side, 285 on the sending side - and
 * left the rest of the window empty. With the weight and a minimum width on the scroll panes they
 * are 474 pixels there.
 */
class KeyExchangePanelLayoutTest
{

	/** How much narrower than its preferred width the panel is squeezed for the measurement */
	private static final int SQUEEZE = 120;

	/** The width below which a text component is not usable any more */
	private static final int USABLE_WIDTH = 120;

	@Test
	@DisplayName("every text area of both tabs stays usable in a window 120 pixels too narrow")
	void everyTextAreaStaysUsableWhenTheWindowIsTooNarrow()
	{
		KeyExchangePanel panel = new KeyExchangePanel();
		Dimension preferred = panel.getPreferredSize();

		int narrowest = PanelWidths.narrowestTextComponent(panel, preferred.width - SQUEEZE,
			Math.max(400, preferred.height));

		assertTrue(narrowest >= USABLE_WIDTH, "squeezed to " + (preferred.width - SQUEEZE)
			+ "px the narrowest text component is " + narrowest + "px: "
			+ PanelWidths.widthReport(panel));
	}

	@Test
	@DisplayName("every text area of both tabs is usable at the width the panel asks for")
	void everyTextAreaIsUsableAtThePreferredWidth()
	{
		KeyExchangePanel panel = new KeyExchangePanel();
		Dimension preferred = panel.getPreferredSize();

		int narrowest = PanelWidths.narrowestTextComponent(panel, preferred.width,
			Math.max(400, preferred.height));

		assertTrue(narrowest >= USABLE_WIDTH,
			"at its preferred width of " + preferred.width + "px the narrowest text component is "
				+ narrowest + "px: " + PanelWidths.widthReport(panel));
	}

	@Test
	@DisplayName("every text area of both tabs is usable at the panel's smallest width")
	void everyTextAreaIsUsableAtTheMinimumSizeOfThePanel()
	{
		KeyExchangePanel panel = new KeyExchangePanel();
		Dimension minimum = panel.getMinimumSize();

		int narrowest = PanelWidths.narrowestTextComponent(panel, minimum.width,
			Math.max(400, minimum.height));

		assertTrue(narrowest >= USABLE_WIDTH,
			"at its minimum width of " + minimum.width + "px the narrowest text component is "
				+ narrowest + "px: " + PanelWidths.widthReport(panel));
	}
}
