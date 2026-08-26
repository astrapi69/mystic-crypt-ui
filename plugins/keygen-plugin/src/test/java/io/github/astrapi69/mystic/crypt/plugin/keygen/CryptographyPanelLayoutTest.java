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
package io.github.astrapi69.mystic.crypt.plugin.keygen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * Layout regression test for {@link CryptographyPanel}.
 * <p>
 * Before the fix the panel pinned both key areas to a fixed width of 480 pixels and made the two
 * key columns non resizable, so it could not follow a window narrower than the width it wants: it
 * preferred 1280 pixels and never went below 1220, and the 60 pixels of give came from the gaps
 * alone. The key areas kept their 477 pixels at every width, which means a narrower window did not
 * shrink them, it cut the public key column and its buttons off the right edge.
 * <p>
 * Measured with this test, 120 pixels below the preferred width: 477 pixels before the fix (never
 * shrinking, clipped), 441 pixels after it; the smallest width the panel accepts went from 1220
 * down to 888 pixels.
 * <p>
 * The test lays the panel out without a display, walks the whole component tree and asserts that
 * every {@link JTextComponent} in it keeps a readable width, at the preferred width as well as
 * below it, so that the new resize behaviour cannot degenerate into collapsing text areas and a
 * fix that only works in one of the two states is caught too.
 */
@DisplayName("The key generation panel keeps its key areas readable in a narrow window")
class CryptographyPanelLayoutTest
{

	/**
	 * The width in pixels below which a text component is no longer usable.
	 */
	private static final int MINIMUM_READABLE_WIDTH = 120;

	/**
	 * The number of pixels the panel is squeezed below its preferred width.
	 */
	private static final int SQUEEZE = 120;

	@TempDir
	File configurationDirectory;

	@BeforeEach
	void useATemporaryConfigurationDirectory()
	{
		// the panel preselects the key size from the installed configuration directory; pointing it
		// at a temporary one keeps the test off the real settings of this machine
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configurationDirectory.getAbsolutePath());
	}

	@AfterEach
	void restoreTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	@Test
	@DisplayName("at its preferred width every text area is readable")
	void everyTextComponentIsReadableAtThePreferredWidth()
	{
		CryptographyPanel panel = new CryptographyPanel();
		Dimension preferredSize = panel.getPreferredSize();

		int narrowest = narrowestTextComponentWidth(panel, preferredSize.width,
			preferredSize.height);

		assertTrue(MINIMUM_READABLE_WIDTH <= narrowest, "the narrowest text component is "
			+ narrowest + " pixels wide at the preferred width of " + preferredSize.width);
	}

	@ParameterizedTest(name = "squeezed {0} pixels below the preferred width")
	@DisplayName("in a narrower window every text area stays readable")
	@ValueSource(ints = { SQUEEZE, 320, 520 })
	void everyTextComponentStaysReadableWhenTheWindowIsNarrower(int squeeze)
	{
		CryptographyPanel panel = new CryptographyPanel();
		Dimension preferredSize = panel.getPreferredSize();
		int squeezedWidth = preferredSize.width - squeeze;

		int narrowest = narrowestTextComponentWidth(panel, squeezedWidth,
			Math.max(400, preferredSize.height));

		assertTrue(MINIMUM_READABLE_WIDTH <= narrowest, "the narrowest text component is "
			+ narrowest + " pixels wide at a window width of " + squeezedWidth);
	}

	@Test
	@DisplayName("the panel accepts a window narrower than the width it prefers")
	void thePanelCanBeLaidOutNarrowerThanItPrefers()
	{
		CryptographyPanel panel = new CryptographyPanel();

		int preferredWidth = panel.getPreferredSize().width;
		int minimumWidth = panel.getMinimumSize().width;

		assertTrue(minimumWidth + SQUEEZE <= preferredWidth,
			"the panel prefers " + preferredWidth + " pixels and never goes below " + minimumWidth
				+ " pixels, so a narrower window cuts its key areas off instead of shrinking them");
	}

	@Test
	@DisplayName("at its minimum width every text area is still inside the panel")
	void everyTextComponentStaysInsideThePanelAtItsMinimumWidth()
	{
		CryptographyPanel panel = new CryptographyPanel();
		Dimension minimumSize = panel.getMinimumSize();

		panel.setSize(minimumSize.width, Math.max(400, minimumSize.height));
		layoutRecursively(panel);

		for (JTextComponent textComponent : collectTextComponents(panel))
		{
			int rightEdge = textComponent.getX() + textComponent.getWidth();
			assertTrue(rightEdge <= minimumSize.width,
				"the text component named " + textComponent.getName() + " ends at " + rightEdge
					+ " pixels, outside the panel width of " + minimumSize.width);
		}
	}

	/**
	 * Lays the given panel out at the given size and returns the width of the narrowest
	 * {@link JTextComponent} found anywhere in its component tree.
	 *
	 * @param panel
	 *            the panel to lay out
	 * @param width
	 *            the width in pixels to lay the panel out at
	 * @param height
	 *            the height in pixels to lay the panel out at
	 * @return the width in pixels of the narrowest text component, or {@link Integer#MAX_VALUE} if
	 *         the panel holds no text component at all
	 */
	private int narrowestTextComponentWidth(CryptographyPanel panel, int width, int height)
	{
		panel.setSize(width, height);
		layoutRecursively(panel);

		int narrowest = Integer.MAX_VALUE;
		for (JTextComponent textComponent : collectTextComponents(panel))
		{
			narrowest = Math.min(narrowest, textComponent.getWidth());
		}
		return narrowest;
	}

	/**
	 * Lays out the given component and, top down, every container below it. Without a display no
	 * validation happens on its own, so the layout has to be triggered on every level.
	 *
	 * @param component
	 *            the root of the component tree to lay out
	 */
	private void layoutRecursively(Component component)
	{
		if (component instanceof Container container)
		{
			container.doLayout();
			for (Component child : container.getComponents())
			{
				layoutRecursively(child);
			}
		}
	}

	/**
	 * Collects every {@link JTextComponent} in the component tree below the given component.
	 *
	 * @param component
	 *            the root of the component tree to walk
	 * @return all text components found, in the order they were encountered
	 */
	private List<JTextComponent> collectTextComponents(Component component)
	{
		List<JTextComponent> textComponents = new ArrayList<>();
		if (component instanceof JTextComponent textComponent)
		{
			textComponents.add(textComponent);
		}
		if (component instanceof Container container)
		{
			for (Component child : container.getComponents())
			{
				textComponents.addAll(collectTextComponents(child));
			}
		}
		return textComponents;
	}

}
