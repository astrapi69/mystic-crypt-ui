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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;

/**
 * Reads a window of this plugin the way a person does, by the names of its components: what a
 * field or a label shows, and which field, button or choice explains nothing (#604)
 */
final class Windows
{

	private Windows()
	{
	}

	/**
	 * @return what the field or label of that name shows
	 */
	static String textOf(final Container window, final String name)
	{
		Component found = component(window, name, Component.class);
		return found instanceof JTextComponent text ? text.getText() : ((JLabel)found).getText();
	}

	/**
	 * @return what every named field and label of the window shows
	 */
	static List<String> textsOf(final Container window)
	{
		return named(window).stream()
			.flatMap(component -> component instanceof JTextComponent text
				? Stream.of(text.getText())
				: component instanceof JLabel label ? Stream.of(label.getText()) : Stream.empty())
			.toList();
	}

	/**
	 * @return the named component, which the test fails without
	 */
	static <T> T component(final Container window, final String name, final Class<T> type)
	{
		T found = named(window).stream().filter(component -> name.equals(component.getName()))
			.map(type::cast).findFirst().orElse(null);
		assertNotNull(found, "the window has no component named " + name);
		return found;
	}

	/**
	 * @return the names of the fields, buttons and choices that have no tooltip (#604, rule 9)
	 */
	static List<String> withoutATooltip(final Container window)
	{
		List<String> silent = new ArrayList<>();
		for (JComponent component : named(window))
		{
			boolean interactive = component instanceof JTextComponent
				|| component instanceof AbstractButton || component instanceof JComboBox;
			String tooltip = component.getToolTipText();
			if (interactive && (tooltip == null || tooltip.isBlank()))
			{
				silent.add(component.getName());
			}
		}
		return silent;
	}

	/**
	 * @return every component of the window that has a name, nested ones included
	 */
	static List<JComponent> named(final Container window)
	{
		List<JComponent> found = new ArrayList<>();
		for (Component child : window.getComponents())
		{
			if (child instanceof JComponent component && component.getName() != null)
			{
				found.add(component);
			}
			if (child instanceof Container nested)
			{
				found.addAll(named(nested));
			}
		}
		return found;
	}
}
