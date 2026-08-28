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
package io.github.astrapi69.mystic.crypt.plugin.obfuscation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.plugin.obfuscation.character.ObfuscationOperationModelBean;
import io.github.astrapi69.mystic.crypt.plugin.obfuscation.character.ObfuscationOperationRulePanel;
import io.github.astrapi69.mystic.crypt.plugin.obfuscation.simple.ObfuscationRulePanel;

/**
 * Layout regression guard for the two rule editors of the obfuscation tools: every text field stays
 * wide enough to be used when the window is narrower than the panel would like to be.
 * <p>
 * Both panels were laid out by a hand written {@link javax.swing.GroupLayout} that pinned every
 * field to 140 px in a row of four columns. They now use the shared tool window form, where a field
 * grows with the window and {@code ToolForm.sized} gives it a floor it is never laid out below, so
 * that a window one pixel too narrow does not make the fields vanish.
 */
class ObfuscationRulePanelsLayoutTest
{

	/** The width below which a text component is no longer usable */
	private static final int USABLE_WIDTH = 120;

	/** How much narrower than its preferred width the panel is squeezed */
	private static final int SQUEEZE = 120;

	/**
	 * The rule editors of both obfuscation tools, each with the name it is reported under
	 *
	 * @return the panels under test
	 */
	static Stream<Arguments> ruleEditors()
	{
		return Stream.of(
			Arguments.of("simple obfuscation rule editor",
				(Supplier<JPanel>)ObfuscationRulePanel::new),
			Arguments.of("operated obfuscation rule editor",
				(Supplier<JPanel>)() -> new ObfuscationOperationRulePanel(
					BaseModel.of(ObfuscationOperationModelBean.builder().build()))));
	}

	@ParameterizedTest(name = "no text field of the {0} collapses at its preferred width")
	@MethodSource("ruleEditors")
	void narrowestTextComponentStaysUsable_atThePreferredWidth(String name,
		Supplier<JPanel> panelFactory)
	{
		JPanel panel = panelFactory.get();
		layoutAt(panel, panel.getPreferredSize().width);

		JTextComponent narrowest = narrowestTextComponent(panel);
		assertTrue(narrowest.getWidth() >= USABLE_WIDTH,
			() -> describe(narrowest) + " of the " + name + " is only " + narrowest.getWidth()
				+ " px wide at the preferred width of " + panel.getWidth()
				+ " px, expected at least " + USABLE_WIDTH + " px");
	}

	@ParameterizedTest(name = "no text field of the {0} collapses when the window is 120 px narrower than the panel wants")
	@MethodSource("ruleEditors")
	void narrowestTextComponentStaysUsable_whenTheWindowIsNarrowerThanPreferred(String name,
		Supplier<JPanel> panelFactory)
	{
		JPanel panel = panelFactory.get();
		layoutAt(panel, panel.getPreferredSize().width - SQUEEZE);

		JTextComponent narrowest = narrowestTextComponent(panel);
		assertTrue(narrowest.getWidth() >= USABLE_WIDTH,
			() -> describe(narrowest) + " of the " + name + " is only " + narrowest.getWidth()
				+ " px wide when the panel is squeezed to " + panel.getWidth()
				+ " px, expected at least " + USABLE_WIDTH + " px");
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
	private static void layoutAt(JPanel panel, int width)
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
			.min((left, right) -> Integer.compare(left.getWidth(), right.getWidth())).orElseThrow();
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
