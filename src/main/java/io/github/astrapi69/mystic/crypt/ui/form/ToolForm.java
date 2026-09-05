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

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

/**
 * One shape for every tool window.
 * <p>
 * The windows grew one at a time and each brought its own layout: hand written GridBag constraints
 * in most plugins, MigLayout in the wizard, a flow layout here and there. They therefore disagreed
 * about everything a user notices - how much room is around a field, whether a label sits left or
 * right of it, whether a field grows with the window - and each one had to rediscover that a text
 * field with no weight collapses to nothing.
 * <p>
 * A tool window built from these constants looks like the one next to it: labels right aligned in a
 * narrow first column, fields filling a second column that takes the width, text areas taking the
 * height that is left, and a row of buttons under the thing it acts on.
 *
 * <pre>
 * setLayout(ToolForm.newLayout());
 * add(new JLabel("Passphrase:"));
 * add(pwdFile, ToolForm.FIELD);
 * add(ToolForm.scrolled(txtOutput), ToolForm.GROWING);
 * add(ToolForm.buttons(btnEncrypt, btnDecrypt), ToolForm.BUTTON_ROW);
 * </pre>
 */
public final class ToolForm
{

	/**
	 * How the two columns behave: a label column that stays narrow, a field column that takes the
	 * rest
	 */
	public static final String COLUMNS = "[right][grow,fill]";

	/** The gap around the form and between its rows, so two windows side by side agree */
	public static final String LAYOUT = "fillx, insets 12, gap 8 6, wrap 2";

	/** A field next to its label, growing with the window */
	public static final String FIELD = "growx";

	/**
	 * Something that has no label of its own and takes both columns.
	 * <p>
	 * The row is started explicitly. Spanning both columns from a row that is already half used
	 * puts the component in the second column instead of the first, and the width of the columns is
	 * then decided by whatever is widest in them - which is how changing a key size in one panel
	 * moved the panel below it several hundred pixels to the right.
	 */
	public static final String WIDE = "newline, span 2, growx, pushx";

	/** A text area that takes the height the window has left, in proportion to the others */
	public static final String GROWING = "newline, span 2, grow, push";

	/**
	 * A text area that fills the width it is given and keeps the height it is given a share of.
	 * <p>
	 * {@code wmin 0} is what makes it fill: without it the widest thing in the area - a line of a
	 * key, or the column count of the text area itself - becomes a lower bound for the column, and
	 * the area collapses to that width the first time the form is laid out again, which reads as
	 * the form jumping while it is being used.
	 */
	public static final String KEY_AREA = "grow, push, wmin 0";

	/**
	 * A row for {@link #intro(String)}, wide like {@link #WIDE} but with {@code wmin 0} - without
	 * it the intro's unwrapped-at-preferred-width measurement becomes a lower bound for the column,
	 * the same collapse {@link #KEY_AREA}'s javadoc already documents for a text area.
	 */
	public static final String INTRO_ROW = WIDE + ", wmin 0";

	/** A row of buttons under what they act on */
	public static final String BUTTON_ROW = "newline, span 2, align left, gaptop 2";

	/** A line that reports what happened, at the bottom */
	public static final String RESULT_LINE = "span 2, growx, gaptop 6";

	/** No field is ever laid out narrower than this, whatever the window does */
	public static final int MINIMUM_FIELD_WIDTH = 160;

	private ToolForm()
	{
	}

	/**
	 * The layout every tool window uses
	 *
	 * @return the layout
	 */
	public static MigLayout newLayout()
	{
		return new MigLayout(LAYOUT, COLUMNS);
	}

	/**
	 * The layout with rows described explicitly, for a window that wants one row to grow more than
	 * another
	 *
	 * @param rows
	 *            the row constraints, in MigLayout's notation
	 * @return the layout
	 */
	public static MigLayout newLayout(final String rows)
	{
		return new MigLayout(LAYOUT, COLUMNS, rows);
	}

	/**
	 * Gives a field a floor it is never laid out below, because a text field reports a minimum
	 * width of nearly nothing and then vanishes in a window one pixel too narrow
	 *
	 * @param field
	 *            the field
	 * @param <T>
	 *            the type of the field
	 * @return the same field
	 */
	public static <T extends JTextComponent> T sized(final T field)
	{
		field.setMinimumSize(new Dimension(MINIMUM_FIELD_WIDTH, field.getPreferredSize().height));
		return field;
	}

	/**
	 * Puts a text area in the scroll pane a tool window shows it in, with the same floor
	 *
	 * @param textArea
	 *            the area
	 * @return the scroll pane to add to the form
	 */
	public static JScrollPane scrolled(final JComponent textArea)
	{
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setMinimumSize(
			new Dimension(MINIMUM_FIELD_WIDTH, Math.max(48, scrollPane.getPreferredSize().height)));
		return scrollPane;
	}

	/**
	 * A short block of introductory text above a tool window's first field, explaining what the
	 * window does before the user starts filling it in.
	 * <p>
	 * A {@code JLabel} wrapped in {@code "<html>"} does not reflow to the width its row is actually
	 * given without an explicit CSS width - it renders as one long unwrapped line, running off to
	 * one side instead of wrapping like the rest of the window (#202). A non-editable, unfocusable
	 * {@link JTextArea} wraps correctly and is styled to read as a label, set off with a light
	 * border so it reads as a distinct note rather than blending into the form.
	 *
	 * @param text
	 *            the text to show
	 * @return the component to add with {@link #INTRO_ROW}
	 */
	public static JTextComponent intro(final String text)
	{
		JTextArea intro = new JTextArea(text);
		intro.setEditable(false);
		intro.setFocusable(false);
		intro.setLineWrap(true);
		intro.setWrapStyleWord(true);
		intro.setOpaque(false);
		intro.setFont(UIManager.getFont("Label.font"));
		Color borderColor = UIManager.getColor("Separator.foreground");
		intro.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(borderColor != null ? borderColor : Color.GRAY),
			BorderFactory.createEmptyBorder(8, 10, 8, 10)));
		return intro;
	}

	/**
	 * A row of buttons that sit together rather than spreading across the window
	 *
	 * @param buttons
	 *            the buttons, in the order they are read
	 * @return the row to add to the form
	 */
	public static JPanel buttons(final JButton... buttons)
	{
		JPanel row = new JPanel(new MigLayout("insets 0, gap 6", "[]", "[]"));
		row.setOpaque(false);
		for (JButton button : buttons)
		{
			row.add(button);
		}
		return row;
	}

}
