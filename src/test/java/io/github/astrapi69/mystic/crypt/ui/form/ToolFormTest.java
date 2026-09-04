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
package io.github.astrapi69.mystic.crypt.ui.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.miginfocom.swing.MigLayout;

/**
 * Every tool window is built from this class, so what it promises has to hold: a field that never
 * collapses, a text area that takes the height it is given, and two windows that agree with each
 * other because they were laid out by the same constants.
 */
class ToolFormTest
{

	@Test
	@DisplayName("the layout is the same for every window")
	void theLayoutIsShared()
	{
		MigLayout first = ToolForm.newLayout();
		MigLayout second = ToolForm.newLayout();

		assertEquals(first.getLayoutConstraints(), second.getLayoutConstraints());
		assertEquals(first.getColumnConstraints(), second.getColumnConstraints());
		assertEquals(ToolForm.COLUMNS, first.getColumnConstraints());
	}

	/**
	 * The reason this class exists: a text field reports a minimum width of nearly nothing, and a
	 * window one pixel too narrow then shows a sliver instead of a field
	 */
	@Test
	@DisplayName("a field is given a floor it is never laid out below")
	void aFieldNeverCollapses()
	{
		JTextField field = new JTextField();
		int beforeTheFloor = field.getMinimumSize().width;

		JTextField same = ToolForm.sized(field);

		assertSame(field, same, "the field itself is returned, so it can be used inline");
		assertEquals(ToolForm.MINIMUM_FIELD_WIDTH, field.getMinimumSize().width);
		assertTrue(beforeTheFloor < ToolForm.MINIMUM_FIELD_WIDTH,
			"this test would prove nothing if a field already had a usable minimum: was "
				+ beforeTheFloor);
	}

	/** A text area reports the minimum of its content, so the floor belongs on the scroll pane */
	@Test
	@DisplayName("a text area is wrapped in a scroll pane that carries the floor")
	void anAreaIsWrappedAndFloored()
	{
		JTextArea area = new JTextArea(3, 40);

		JScrollPane scrollPane = ToolForm.scrolled(area);

		assertSame(area, scrollPane.getViewport().getView());
		assertEquals(ToolForm.MINIMUM_FIELD_WIDTH, scrollPane.getMinimumSize().width);
		assertTrue(scrollPane.getMinimumSize().height >= 48,
			"an area squeezed to nothing is as useless as a field squeezed to nothing");
	}

	/** Buttons sit together rather than spreading across the window */
	@Test
	@DisplayName("a button row keeps its buttons together and in order")
	void buttonsStayTogether()
	{
		JButton first = new JButton("Encrypt");
		JButton second = new JButton("Decrypt");

		JPanel row = ToolForm.buttons(first, second);

		assertEquals(2, row.getComponentCount());
		assertSame(first, row.getComponent(0), "the buttons keep the order they are read in");
		assertSame(second, row.getComponent(1));
		assertTrue(row.getLayout() instanceof MigLayout);
	}

	/** A window that wants one row to grow more than another says so through the same door */
	@Test
	@DisplayName("row constraints can be given without losing the shared columns")
	void rowsCanBeDescribedExplicitly()
	{
		MigLayout layout = ToolForm.newLayout("[][grow 200][grow 100]");

		assertEquals(ToolForm.COLUMNS, layout.getColumnConstraints());
		assertEquals("[][grow 200][grow 100]", layout.getRowConstraints());
		assertNotNull(layout.getLayoutConstraints());
	}

	/**
	 * Three panels each wrapped an intro text in a plain {@code "<html>" + text + "</html>"} label,
	 * which does not reflow to the width its row is actually given without an explicit CSS width -
	 * it rendered as one long unwrapped line, running off to one side (#202). The replacement has
	 * to actually wrap, not just avoid the html tag.
	 */
	@Test
	@DisplayName("an intro reads as a wrapping, non-editable block of text, not a single unwrapped line")
	void introWrapsInsteadOfRunningOffToOneSide()
	{
		JTextArea intro = (JTextArea)ToolForm.intro("some explanatory text");

		assertEquals("some explanatory text", intro.getText());
		assertTrue(intro.getLineWrap(), "the text must wrap instead of running off to one side");
		assertTrue(intro.getWrapStyleWord(),
			"wrapping must break on word boundaries, not mid-word");
		assertFalse(intro.isEditable(), "an intro is not a field the user types into");
		assertFalse(intro.isFocusable(), "an intro is not a stop on the tab order");
	}

	/** The intro is visually set off from the rest of the form, not blending into a plain line */
	@Test
	@DisplayName("an intro is framed, not a bare line of text")
	void introIsFramed()
	{
		JTextArea intro = (JTextArea)ToolForm.intro("some explanatory text");

		assertNotNull(intro.getBorder(), "an intro must be visibly set off with a border");
	}
}
