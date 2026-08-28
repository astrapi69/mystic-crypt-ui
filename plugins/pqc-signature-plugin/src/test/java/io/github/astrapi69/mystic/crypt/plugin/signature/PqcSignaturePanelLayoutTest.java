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
package io.github.astrapi69.mystic.crypt.plugin.signature;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
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
 * Guards the panel against a window that is narrower than the panel would like to be.
 * <p>
 * Before the fix the input column of the GridBagLayout had neither a fill nor a weight. As soon
 * as the container was narrower than the grid wanted, GridBagLayout fell back to the minimum
 * widths of that column, and a text component reports a minimum width of nearly zero: the
 * fields did not shrink, they vanished. Measured on this panel: at its preferred width the four
 * file fields were 444 pixels wide and the three text areas 660 pixels, which is right, but 120
 * pixels below the preferred width the fields were 5 pixels and the areas 4 pixels wide. With
 * the horizontal fill and the weight on the input column the same measurement gives 543 pixels
 * for the fields and 525 pixels for the areas.
 * <p>
 * The panel is laid out with the shared {@code ToolForm} since then, whose field column grows the
 * same way; the numbers above are kept as the record of the bug this test guards against.
 */
@DisplayName("the signature panel keeps every text component readable in a narrow window")
class PqcSignaturePanelLayoutTest
{

	/** The width every text component has to keep, even in the narrowest layout */
	private static final int USABLE_WIDTH = 120;

	/** How far below its preferred width the panel is squeezed for the measurement */
	private static final int SQUEEZE = 120;

	@TempDir
	File workingDirectory;

	private PqcSignaturePanel panel;

	@BeforeEach
	void createThePanelWithATemporaryConfigurationDirectory()
	{
		// the panel starts with the configured algorithm and message; a temporary configuration
		// directory keeps the test off the real settings of this machine
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			workingDirectory.getAbsolutePath());
		panel = new PqcSignaturePanel();
	}

	@AfterEach
	void restoreTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	/**
	 * The case from the bug report: the window is narrower than the panel wants, so GridBagLayout
	 * falls back to the minimum widths of the column
	 */
	@Test
	@DisplayName("squeezed 120 pixels below its preferred width, no text component collapses")
	void everyTextComponentStaysUsableWhenThePanelIsSqueezed()
	{
		int narrowest = narrowestTextComponentWidth(panel.getPreferredSize().width - SQUEEZE);

		assertTrue(narrowest >= USABLE_WIDTH, "squeezed " + SQUEEZE
			+ " pixels below the preferred width the narrowest text component is " + narrowest
			+ " pixels wide, at least " + USABLE_WIDTH + " are needed:" + widthsPerComponent());
	}

	/**
	 * A fix that only works while the panel is squeezed would be no fix, so the same measurement
	 * is taken at the width the panel asks for
	 */
	@Test
	@DisplayName("at its preferred width, no text component collapses either")
	void everyTextComponentStaysUsableAtThePreferredWidth()
	{
		int narrowest = narrowestTextComponentWidth(panel.getPreferredSize().width);

		assertTrue(narrowest >= USABLE_WIDTH,
			"at the preferred width the narrowest text component is " + narrowest
				+ " pixels wide, at least " + USABLE_WIDTH + " are needed:" + widthsPerComponent());
	}

	/**
	 * Lays the panel out at the given width and measures its narrowest text component
	 *
	 * @param width
	 *            the width the panel is laid out at
	 * @return the width in pixels of the narrowest text component of the whole panel
	 */
	private int narrowestTextComponentWidth(final int width)
	{
		panel.setSize(width, Math.max(400, panel.getPreferredSize().height));
		layoutRecursively(panel);
		int narrowest = Integer.MAX_VALUE;
		for (JTextComponent textComponent : textComponents(panel, new ArrayList<>()))
		{
			narrowest = Math.min(narrowest, textComponent.getWidth());
		}
		return narrowest;
	}

	/**
	 * Every text component with the width it was given, for a failure message that names the
	 * component that collapsed
	 *
	 * @return one line per text component of the panel
	 */
	private String widthsPerComponent()
	{
		StringBuilder widths = new StringBuilder();
		for (JTextComponent textComponent : textComponents(panel, new ArrayList<>()))
		{
			widths.append(System.lineSeparator()).append("  ").append(textComponent.getName())
				.append(": ").append(textComponent.getWidth()).append(" px");
		}
		return widths.toString();
	}

	/**
	 * Lays out the container and every container below it, because a container only sizes its own
	 * children and the text areas sit two levels down inside their scroll panes
	 *
	 * @param container
	 *            the container to lay out together with everything below it
	 */
	private static void layoutRecursively(final Container container)
	{
		container.doLayout();
		for (Component child : container.getComponents())
		{
			if (child instanceof Container)
			{
				layoutRecursively((Container)child);
			}
		}
	}

	/**
	 * Collects every text component of the whole component tree
	 *
	 * @param container
	 *            the container to walk
	 * @param collected
	 *            the list the found text components are added to
	 * @return the list that was passed in
	 */
	private static List<JTextComponent> textComponents(final Container container,
		final List<JTextComponent> collected)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof JTextComponent)
			{
				collected.add((JTextComponent)child);
			}
			if (child instanceof Container)
			{
				textComponents((Container)child, collected);
			}
		}
		return collected;
	}
}
