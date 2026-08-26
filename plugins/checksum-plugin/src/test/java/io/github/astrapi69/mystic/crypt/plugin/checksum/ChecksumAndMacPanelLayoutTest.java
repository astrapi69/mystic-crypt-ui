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
package io.github.astrapi69.mystic.crypt.plugin.checksum;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * Proof that the {@link ChecksumAndMacPanel} stays operable in a window that is narrower than the
 * panel would like to be.
 * <p>
 * Before the fix the panel built its grid without a fill and without a weight on the column that
 * holds the inputs. A {@link JTextComponent} reports a minimum width of nearly zero, so as soon as
 * the window fell below the width the grid wanted, the fields did not shrink, they disappeared. The
 * panel asks for 735 pixels; at that width its text components measured between 268 and 576 pixels,
 * at 615 pixels they measured 4 to 5 pixels each - the text areas 4, every field 5.
 * <p>
 * Both widths are measured here, so a fix that only helps when the window is squeezed - or only
 * when it is not - fails.
 */
@DisplayName("the checksum panel keeps its text fields usable in a narrow window")
class ChecksumAndMacPanelLayoutTest
{

	/** The width a text component has to keep to be usable at all */
	private static final int USABLE_WIDTH = 120;

	/** How far below its preferred width the panel is measured */
	private static final int NARROWER_BY = 120;

	@TempDir
	File configurationDirectory;

	@BeforeEach
	void useATemporaryConfigurationDirectory()
	{
		// the panel starts with the configured digest; a temporary directory keeps the test off
		// the real settings of this machine
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configurationDirectory.getAbsolutePath());
	}

	@AfterEach
	void restoreTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	@Test
	@DisplayName("every text component is usable at the width the panel asks for")
	void everyTextComponentIsWideEnoughAtThePreferredWidth()
	{
		ChecksumAndMacPanel panel = new ChecksumAndMacPanel();
		Dimension preferredSize = panel.getPreferredSize();

		assertUsable(layoutAt(panel, preferredSize.width, preferredSize.height),
			"at the preferred width of " + preferredSize.width);
	}

	@Test
	@DisplayName("every text component survives a window " + NARROWER_BY
		+ " pixels below the preferred width")
	void everyTextComponentSurvivesAWindowNarrowerThanThePanelWants()
	{
		ChecksumAndMacPanel panel = new ChecksumAndMacPanel();
		Dimension preferredSize = panel.getPreferredSize();

		assertUsable(
			layoutAt(panel, preferredSize.width - NARROWER_BY, Math.max(400, preferredSize.height)),
			"at " + NARROWER_BY + " pixels below the preferred width of " + preferredSize.width);
	}

	/**
	 * Fails unless the narrowest of the measured text components is still usable
	 *
	 * @param measured
	 *            every text component of the panel with the width it was laid out to
	 * @param where
	 *            the width the panel was measured at, for the failure message
	 */
	private static void assertUsable(List<Component> measured, String where)
	{
		assertFalse(measured.isEmpty(), "no text component was found in the panel at all");
		Component narrowest = narrowestOf(measured);
		assertTrue(USABLE_WIDTH <= narrowest.getWidth(),
			() -> "the narrowest text component " + where + " is " + narrowest.getName() + " with "
				+ narrowest.getWidth() + " pixels, which is below the " + USABLE_WIDTH
				+ " pixels a field needs to be usable" + report(measured));
	}

	/**
	 * Lays the panel and everything below it out at the given size and collects the text components
	 *
	 * @param panel
	 *            the panel to lay out
	 * @param width
	 *            the width the panel is given
	 * @param height
	 *            the height the panel is given
	 * @return every text component of the panel, with the width the layout gave it
	 */
	private static List<Component> layoutAt(ChecksumAndMacPanel panel, int width, int height)
	{
		panel.setSize(width, height);
		layoutDeeply(panel);
		List<Component> textComponents = new ArrayList<>();
		collectTextComponents(panel, textComponents);
		return textComponents;
	}

	/**
	 * Lays out a container and, after it, every container below it - a component only knows its
	 * width once its parent has laid it out
	 *
	 * @param component
	 *            the root of the tree to lay out
	 */
	private static void layoutDeeply(Component component)
	{
		if (component instanceof Container container)
		{
			container.doLayout();
			for (Component child : container.getComponents())
			{
				layoutDeeply(child);
			}
		}
	}

	private static void collectTextComponents(Container root, List<Component> collected)
	{
		for (Component component : root.getComponents())
		{
			if (component instanceof JTextComponent)
			{
				collected.add(component);
			}
			if (component instanceof Container container)
			{
				collectTextComponents(container, collected);
			}
		}
	}

	private static Component narrowestOf(List<Component> components)
	{
		Component narrowest = components.get(0);
		for (Component component : components)
		{
			if (component.getWidth() < narrowest.getWidth())
			{
				narrowest = component;
			}
		}
		return narrowest;
	}

	private static String report(List<Component> components)
	{
		StringBuilder report = new StringBuilder();
		for (Component component : components)
		{
			report.append(System.lineSeparator()).append("  ").append(component.getName())
				.append(": ").append(component.getWidth()).append(" px");
		}
		return report.toString();
	}
}
