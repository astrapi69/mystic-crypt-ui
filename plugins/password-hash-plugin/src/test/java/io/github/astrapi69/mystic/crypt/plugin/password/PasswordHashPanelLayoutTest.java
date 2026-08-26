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
package io.github.astrapi69.mystic.crypt.plugin.password;

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
 * Layout regression guard for {@link PasswordHashPanel}: every text component of the panel stays
 * wide enough to be used when the window is narrower than the panel would like to be.
 * <p>
 * Before the fix the panel built its {@link java.awt.GridBagLayout} without {@code fill} and
 * without {@code weightx} on the input column. GridBagLayout then falls back to the minimum widths
 * as soon as the container is narrower than the grid wants, and a text field reports a minimum
 * width of nearly nothing, so the fields did not shrink, they disappeared:
 * <ul>
 * <li>at the preferred width of 566 px: password field 268 px, hash area 443 px, correct</li>
 * <li>at 500 px: password field 5 px, hash area 22 px, unusable</li>
 * <li>120 px below the preferred width: narrowest text component 0 px</li>
 * </ul>
 */
class PasswordHashPanelLayoutTest
{

	/** The width below which a text component is no longer usable */
	private static final int USABLE_WIDTH = 120;

	/** How much narrower than its preferred width the panel is squeezed */
	private static final int SQUEEZE = 120;

	@TempDir
	File configurationDirectory;

	@BeforeEach
	void useATemporaryConfigurationDirectory()
	{
		// the panel reads its start algorithm through the installed configuration directory;
		// pointing it at a temporary one keeps the test off the real settings of this machine
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configurationDirectory.getAbsolutePath());
	}

	@AfterEach
	void restoreTheConfigurationDirectory()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	@Test
	@DisplayName("no text field of the password hash panel collapses when the window is 120 px narrower than the panel wants")
	void narrowestTextComponentStaysUsable_whenTheWindowIsNarrowerThanPreferred()
	{
		PasswordHashPanel panel = new PasswordHashPanel();
		layoutAt(panel, panel.getPreferredSize().width - SQUEEZE);

		JTextComponent narrowest = narrowestTextComponent(panel);
		assertTrue(narrowest.getWidth() >= USABLE_WIDTH,
			() -> describe(narrowest) + " is only " + narrowest.getWidth()
				+ " px wide when the panel is squeezed to " + panel.getWidth()
				+ " px, expected at least " + USABLE_WIDTH + " px");
	}

	@Test
	@DisplayName("no text field of the password hash panel collapses at the panel's preferred width")
	void narrowestTextComponentStaysUsable_atThePreferredWidth()
	{
		PasswordHashPanel panel = new PasswordHashPanel();
		layoutAt(panel, panel.getPreferredSize().width);

		JTextComponent narrowest = narrowestTextComponent(panel);
		assertTrue(narrowest.getWidth() >= USABLE_WIDTH,
			() -> describe(narrowest) + " is only " + narrowest.getWidth()
				+ " px wide at the preferred width of " + panel.getWidth() + " px, expected at least "
				+ USABLE_WIDTH + " px");
	}

	/**
	 * Sizes the panel to the given width and lays out the whole component tree, so that the width
	 * every text component really gets can be read off it
	 *
	 * @param panel
	 *            the panel under test
	 * @param width
	 *            the width the panel is given
	 */
	private static void layoutAt(PasswordHashPanel panel, int width)
	{
		panel.setSize(width, Math.max(400, panel.getPreferredSize().height));
		layoutRecursively(panel);
	}

	/**
	 * Lays out the given container and, below it, every container it holds
	 *
	 * @param container
	 *            the container to lay out
	 */
	private static void layoutRecursively(Container container)
	{
		container.doLayout();
		for (Component child : container.getComponents())
		{
			if (child instanceof Container childContainer)
			{
				layoutRecursively(childContainer);
			}
		}
	}

	/**
	 * The text component of the tree below the given container that came out narrowest
	 *
	 * @param container
	 *            the root of the component tree to walk
	 * @return the narrowest text component
	 */
	private static JTextComponent narrowestTextComponent(Container container)
	{
		List<JTextComponent> textComponents = new ArrayList<>();
		collectTextComponents(container, textComponents);
		assertTrue(!textComponents.isEmpty(),
			"the panel holds no text component, the measurement would prove nothing");
		return textComponents.stream()
			.min((left, right) -> Integer.compare(left.getWidth(), right.getWidth()))
			.orElseThrow();
	}

	/**
	 * Collects every text component of the tree below the given container
	 *
	 * @param container
	 *            the root of the component tree to walk
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
	 * A description of the given component that names it, so a failure says which field collapsed
	 *
	 * @param component
	 *            the component to describe
	 * @return the name of the component, its class name when it carries no name
	 */
	private static String describe(Component component)
	{
		return component.getName() != null
			? component.getName()
			: component.getClass().getSimpleName();
	}
}
