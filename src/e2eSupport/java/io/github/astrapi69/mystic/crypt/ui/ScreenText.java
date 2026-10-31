/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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
package io.github.astrapi69.mystic.crypt.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.text.JTextComponent;

import org.assertj.swing.edt.GuiActionRunner;

/**
 * Reads what is actually readable on screen, so a test can assert that a locked vault's content is
 * not.
 * <p>
 * The vault panel being gone is one way for the content to be out of reach; a dialog that lists it
 * is a second door to the same content, and only looking at the text answers both. Shared rather
 * than copied because the automatic lock has to be held to the same assertion as the actions
 * (#305), and two copies of a screen scan drift the moment one of them learns about a new component
 * type.
 */
final class ScreenText
{

	private ScreenText()
	{
	}

	/**
	 * Whether none of the given secrets is readable in any showing window
	 *
	 * @param secrets
	 *            the strings that must not be on screen, typically an entry's title and password
	 * @return true if none of them is readable
	 */
	static boolean nothingOnScreenShows(final String... secrets)
	{
		List<String> onScreen = GuiActionRunner.execute(() -> {
			List<String> texts = new ArrayList<>();
			for (Window window : Window.getWindows())
			{
				if (window.isShowing())
				{
					collectText(window, texts);
				}
			}
			return texts;
		});
		return Arrays.stream(secrets)
			.noneMatch(secret -> onScreen.stream().anyMatch(text -> text.contains(secret)));
	}

	private static void collectText(final Component component, final List<String> texts)
	{
		switch (component)
		{
			case JLabel label -> texts.add(String.valueOf(label.getText()));
			case JTextComponent field -> texts.add(String.valueOf(field.getText()));
			case JTable table -> collectTableText(table, texts);
			case JTree tree -> collectTreeText(tree, texts);
			default ->
			{
			}
		}
		if (component instanceof Container container)
		{
			for (Component child : container.getComponents())
			{
				collectText(child, texts);
			}
		}
	}

	private static void collectTableText(final JTable table, final List<String> texts)
	{
		for (int row = 0; row < table.getRowCount(); row++)
		{
			for (int column = 0; column < table.getColumnCount(); column++)
			{
				texts.add(String.valueOf(table.getValueAt(row, column)));
			}
		}
	}

	private static void collectTreeText(final JTree tree, final List<String> texts)
	{
		for (int row = 0; row < tree.getRowCount(); row++)
		{
			texts.add(String.valueOf(tree.getPathForRow(row).getLastPathComponent()));
		}
	}
}
