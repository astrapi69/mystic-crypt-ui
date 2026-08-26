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
package io.github.astrapi69.mystic.crypt.plugin.sharing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JSpinner;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * Protects the {@link SecretSharingPanel} against a window that is narrower than the panel would
 * like: every text component has to stay wide enough to type into, instead of collapsing.
 * <p>
 * Before the fix the form was built with a {@code GridBagLayout} whose input column carried neither
 * {@code fill} nor {@code weightx}. As soon as the panel got less width than it asked for,
 * GridBagLayout fell back to the minimum sizes, and the minimum width a text component reports is
 * next to nothing. Measured on this panel, whose preferred width is 855 pixels:
 *
 * <pre>
 * at the preferred width 855: pwdSecret 312, txtSecretFile 378, txtShares 434, txtRebuilt 682,
 *                             txtRebuiltFile 378 - all of them usable
 * at 735, 120 below:          pwdSecret   5, txtSecretFile   5, txtShares 434, txtRebuilt   4,
 *                             txtRebuiltFile   5 - everything but the share area was gone
 * </pre>
 *
 * The share area survived only because it does not wrap and therefore keeps its own wide preferred
 * size; the fields the user types into were down to a few pixels, only their labels and buttons
 * were left. With the fix the narrowest text component at 735 is 547 pixels wide.
 * <p>
 * The spinners are measured separately: a spinner over the range 2 to 255 is usable at its own
 * preferred width and must not be stretched across the window, so it is held to its preferred width
 * rather than to the width a typed-into field needs.
 */
class SecretSharingPanelLayoutTest
{

	/** The width every text component of the panel has to keep, even in the narrowest layout */
	private static final int USABLE_WIDTH = 120;

	/** How much narrower than its preferred width the panel is measured at */
	private static final int SQUEEZE = 120;

	/** The text components of the panel, by the name each one was given */
	private static final List<String> TEXT_COMPONENT_NAMES = List.of("pwdSecret", "txtRebuilt",
		"txtRebuiltFile", "txtSecretFile", "txtShares");

	@TempDir
	File configurationDirectory;

	private SecretSharingPanel panel;

	@BeforeAll
	static void layoutWithoutADisplay()
	{
		System.setProperty("java.awt.headless", "true");
	}

	@BeforeEach
	void createThePanelWithATemporaryConfigurationDirectory()
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configurationDirectory.getAbsolutePath());
		panel = new SecretSharingPanel();
	}

	@AfterEach
	void restoreTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	/**
	 * The case the fix is about: the window is narrower than the panel wants, and every text
	 * component still has to be wide enough to work with
	 */
	@Test
	@DisplayName("a window 120 pixels narrower than the panel wants keeps every text component usable")
	void everyTextComponentStaysUsableWhenTheWindowIsNarrowerThanThePanelWants()
	{
		layoutAt(panel.getPreferredSize().width - SQUEEZE);

		JTextComponent narrowest = narrowestTextComponent();
		assertTrue(narrowest.getWidth() >= USABLE_WIDTH, () -> report(narrowest));
	}

	/**
	 * The counter case, so a fix that only works while the panel is squeezed is caught too: at the
	 * width the panel asks for, every text component is of course usable
	 */
	@Test
	@DisplayName("at the width the panel asks for every text component is usable as well")
	void everyTextComponentStaysUsableAtThePreferredWidth()
	{
		layoutAt(panel.getPreferredSize().width);

		JTextComponent narrowest = narrowestTextComponent();
		assertTrue(narrowest.getWidth() >= USABLE_WIDTH, () -> report(narrowest));
	}

	/**
	 * The spinners are not stretched across the window, but they are not allowed to collapse
	 * either: in the narrow layout they keep the width they ask for
	 */
	@Test
	@DisplayName("the spinners keep their own width when the window is narrower than the panel wants")
	void theSpinnersKeepTheirWidthWhenTheWindowIsNarrowerThanThePanelWants()
	{
		for (JSpinner spinner : componentsOfType(panel, JSpinner.class))
		{
			int wanted = spinner.getPreferredSize().width;
			layoutAt(panel.getPreferredSize().width - SQUEEZE);
			assertEquals(wanted, spinner.getWidth(),
				"the spinner '" + spinner.getName() + "' has to keep its width");
		}
	}

	/**
	 * Lays the panel out at the given width, the way a window of that width would, and passes the
	 * new size on to every child: a component that is never laid out reports a width of zero and
	 * would make the measurement above pass for the wrong reason
	 *
	 * @param width
	 *            the width to lay the panel out at
	 */
	private void layoutAt(int width)
	{
		panel.setSize(width, Math.max(400, panel.getPreferredSize().height));
		layoutRecursively(panel);
	}

	/**
	 * Lays out the given component and, below it, every container it holds
	 *
	 * @param component
	 *            the component to lay out
	 */
	private static void layoutRecursively(Component component)
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
	 * The narrowest of the panel's text components, the editors inside the spinners left aside
	 *
	 * @return the narrowest text component
	 */
	private JTextComponent narrowestTextComponent()
	{
		List<JTextComponent> textComponents = textComponents();
		assertEquals(TEXT_COMPONENT_NAMES,
			textComponents.stream().map(Component::getName).sorted().collect(Collectors.toList()),
			"the walk over the panel has to find every text component, otherwise it measures nothing");
		return textComponents.stream().min(Comparator.comparingInt(Component::getWidth))
			.orElseThrow();
	}

	/**
	 * Every text component of the panel except the editors inside the spinners, which carry a
	 * number of at most three digits and are measured on their own
	 *
	 * @return the text components
	 */
	private List<JTextComponent> textComponents()
	{
		List<JTextComponent> collected = new ArrayList<>();
		collectTextComponents(panel, false, collected);
		return collected;
	}

	private static void collectTextComponents(Component component, boolean insideSpinner,
		List<JTextComponent> collected)
	{
		if (component instanceof JTextComponent textComponent && !insideSpinner)
		{
			collected.add(textComponent);
		}
		if (component instanceof Container container)
		{
			boolean spinnerAbove = insideSpinner || component instanceof JSpinner;
			for (Component child : container.getComponents())
			{
				collectTextComponents(child, spinnerAbove, collected);
			}
		}
	}

	private static <T extends Component> List<T> componentsOfType(Container container,
		Class<T> type)
	{
		List<T> collected = new ArrayList<>();
		for (Component child : container.getComponents())
		{
			if (type.isInstance(child))
			{
				collected.add(type.cast(child));
			}
			else if (child instanceof Container childContainer)
			{
				collected.addAll(componentsOfType(childContainer, type));
			}
		}
		return collected;
	}

	/**
	 * Says what was measured, so a failure carries the numbers instead of only a boolean
	 *
	 * @param narrowest
	 *            the narrowest text component
	 * @return the message
	 */
	private String report(JTextComponent narrowest)
	{
		String measured = textComponents().stream()
			.map(component -> component.getName() + " " + component.getWidth() + " px")
			.collect(Collectors.joining(", "));
		return "at a panel width of " + panel.getWidth() + " (preferred "
			+ panel.getPreferredSize().width + ") the narrowest text component '"
			+ narrowest.getName() + "' is " + narrowest.getWidth() + " px wide, it has to keep at "
			+ "least " + USABLE_WIDTH + " px - measured: " + measured;
	}
}
