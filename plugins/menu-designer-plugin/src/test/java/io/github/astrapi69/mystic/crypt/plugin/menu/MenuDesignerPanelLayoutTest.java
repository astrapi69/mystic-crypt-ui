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
package io.github.astrapi69.mystic.crypt.plugin.menu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * Guards the xml editor of {@link MenuDesignerPanel} against a window that is narrower than the
 * panel wants.
 * <p>
 * The collapse this protects against is the one measured on the sibling panels: a text component in
 * a {@code GridBagLayout} column without {@code fill} and without {@code weightx} falls back to its
 * minimum width, which for a text component is close to zero, so the field does not shrink, it
 * disappears. This panel does not fall into that trap, because the editor sits in a scroll pane
 * that spans the whole width of the shared tool window layout and takes the height that is left.
 * Measured:
 * <ul>
 * <li>at the preferred width of 664 px the editor is 637 px wide</li>
 * <li>120 px below it, at 544 px, the editor is still 637 px wide, because the scroll pane states a
 * minimum width the layout does not go below</li>
 * </ul>
 * What was dishonest was the minimum. The scroll pane around the editor reported 22 x 22 px, the
 * bare minimum of an empty viewport, so every container that honours minimum sizes was free to
 * squeeze the editor away completely while the panel claimed nothing was wrong. It now states a
 * minimum that is still readable.
 */
@DisplayName("MenuDesignerPanel keeps its xml editor readable in a window narrower than it wants")
class MenuDesignerPanelLayoutTest
{

	/** The extent below which a text component is no longer usable */
	private static final int MINIMUM_USABLE_EXTENT = 120;

	/** How far below its preferred width the panel is squeezed for the measurement */
	private static final int SQUEEZE = 120;

	/** The height the panel is measured at, so the editor gets room in every case */
	private static final int MEASURED_HEIGHT = 400;

	@TempDir
	File configurationDirectory;

	@BeforeEach
	void keepTheEditorEmptyOnOpen() throws Exception
	{
		// the panel exports the live menu into the editor on open unless that is switched off; the
		// measurement runs without a running application frame, so it is switched off through the
		// plugin's own settings rather than worked around in the panel
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configurationDirectory.getAbsolutePath());
		PluginSettings.save(configurationDirectory, MenuDesignerSettingsContribution.PLUGIN_ID,
			new MenuDesignerSettingsContribution().getDefaults(),
			Map.of(MenuDesignerSettingsContribution.KEY_EXPORT_ON_OPEN, "false"));
	}

	@AfterEach
	void restoreTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	@Test
	@DisplayName("at the preferred width every text component is wide enough to read")
	void everyTextComponentStaysUsableAtThePreferredWidth()
	{
		MenuDesignerPanel panel = new MenuDesignerPanel();

		assertEveryTextComponentStaysUsable(panel, panel.getPreferredSize().width);
	}

	@Test
	@DisplayName("120 px below the preferred width every text component is still wide enough to read")
	void everyTextComponentStaysUsableBelowThePreferredWidth()
	{
		MenuDesignerPanel panel = new MenuDesignerPanel();

		assertEveryTextComponentStaysUsable(panel, panel.getPreferredSize().width - SQUEEZE);
	}

	@Test
	@DisplayName("the editor states a minimum size that is still readable, not the viewport's 22 px")
	void theEditorStatesAnHonestMinimumSize()
	{
		MenuDesignerPanel panel = new MenuDesignerPanel();

		Dimension minimum = editorScrollPaneOf(panel).getMinimumSize();
		assertTrue(minimum.width >= MINIMUM_USABLE_EXTENT,
			() -> "the editor reports a minimum width of " + minimum.width
				+ " px, expected at least " + MINIMUM_USABLE_EXTENT + " px");
		assertTrue(minimum.height >= MINIMUM_USABLE_EXTENT,
			() -> "the editor reports a minimum height of " + minimum.height
				+ " px, expected at least " + MINIMUM_USABLE_EXTENT + " px");
	}

	/**
	 * Lays the panel out at the given width and asserts that no text component in the whole
	 * component tree has shrunk below {@link #MINIMUM_USABLE_EXTENT}
	 *
	 * @param panel
	 *            the panel under measurement
	 * @param width
	 *            the width the panel is laid out at
	 */
	private static void assertEveryTextComponentStaysUsable(MenuDesignerPanel panel, int width)
	{
		panel.setSize(width, Math.max(MEASURED_HEIGHT, panel.getPreferredSize().height));
		layOutRecursively(panel);

		List<JTextComponent> textComponents = collectTextComponents(panel);
		assertFalse(textComponents.isEmpty(), "the panel holds no text component to measure");
		JTextComponent narrowest = narrowestOf(textComponents);
		assertTrue(narrowest.getWidth() >= MINIMUM_USABLE_EXTENT,
			() -> "at a panel width of " + width + " px the narrowest of the "
				+ textComponents.size() + " text components, '" + narrowest.getName() + "', is "
				+ narrowest.getWidth() + " px wide, expected at least " + MINIMUM_USABLE_EXTENT
				+ " px");
	}

	/**
	 * Lays out the given container and, after it, every container below it, so that the measured
	 * widths are the ones the components really get
	 *
	 * @param container
	 *            the container to lay out
	 */
	private static void layOutRecursively(Container container)
	{
		container.doLayout();
		for (Component child : container.getComponents())
		{
			if (child instanceof Container childContainer)
			{
				layOutRecursively(childContainer);
			}
		}
	}

	/**
	 * Collects every text component in the whole tree below the given container
	 *
	 * @param container
	 *            the root of the walked tree
	 * @return every text component below the container, in the order they were found
	 */
	private static List<JTextComponent> collectTextComponents(Container container)
	{
		List<JTextComponent> textComponents = new ArrayList<>();
		for (Component child : container.getComponents())
		{
			if (child instanceof JTextComponent textComponent)
			{
				textComponents.add(textComponent);
			}
			if (child instanceof Container childContainer)
			{
				textComponents.addAll(collectTextComponents(childContainer));
			}
		}
		return textComponents;
	}

	/**
	 * Finds the narrowest of the given text components
	 *
	 * @param textComponents
	 *            the measured text components, never empty
	 * @return the text component with the smallest width
	 */
	private static JTextComponent narrowestOf(List<JTextComponent> textComponents)
	{
		JTextComponent narrowest = textComponents.get(0);
		for (JTextComponent textComponent : textComponents)
		{
			if (textComponent.getWidth() < narrowest.getWidth())
			{
				narrowest = textComponent;
			}
		}
		return narrowest;
	}

	/**
	 * Finds the scroll pane that carries the xml editor
	 *
	 * @param panel
	 *            the panel under measurement
	 * @return the scroll pane around the xml editor
	 */
	private static JScrollPane editorScrollPaneOf(MenuDesignerPanel panel)
	{
		JTextComponent editor = collectTextComponents(panel).get(0);
		Container ancestor = editor.getParent();
		while (ancestor != null && !(ancestor instanceof JScrollPane))
		{
			ancestor = ancestor.getParent();
		}
		assertNotNull(ancestor, "the xml editor is not carried by a scroll pane");
		return (JScrollPane)ancestor;
	}
}
