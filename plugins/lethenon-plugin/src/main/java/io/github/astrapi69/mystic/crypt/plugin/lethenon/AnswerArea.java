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

import java.awt.Component;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 * The answer area of a window that runs an action (rule G3, draft 06, #816, #820). Its components
 * show only while the window holds an answer; until then one sentence says what will show there,
 * instead of empty fields. The details stay as they were opened or closed.
 * <p>
 * A failure view hides the whole area and gives it back as it was; the window calls
 * {@link #show(boolean, boolean)} after every result, so what comes back is put right. The window
 * grows to what it then shows (#835): it was packed while only the sentence stood here.
 */
final class AnswerArea
{

	/** The name of the sentence that stands in the place of the answer */
	static final String TO_COME = "txtAnswerToCome";

	private final List<Component> components;

	private final Component toCome;

	private final Component details;

	private AnswerArea(final List<Component> components, final Component details)
	{
		this.components = components;
		this.toCome = components.get(0);
		this.details = details;
	}

	/**
	 * The answer area of a form: the components added to it from the given index on, the first of
	 * them the sentence from {@link #toCome(String, String)}
	 *
	 * @param form
	 *            the window's form
	 * @param start
	 *            the index of the first component of the answer
	 * @param details
	 *            the panel of the details, shown only while they are opened
	 * @return the answer area
	 * @throws IllegalStateException
	 *             when the answer does not start with the sentence, naming what it starts with
	 */
	static AnswerArea of(final JPanel form, final int start, final Component details)
	{
		List<Component> components = List
			.copyOf(List.of(form.getComponents()).subList(start, form.getComponentCount()));
		if (components.isEmpty() || !TO_COME.equals(components.get(0).getName()))
		{
			throw new IllegalStateException("the answer of " + form.getClass().getSimpleName()
				+ " does not start with the sentence " + TO_COME + ", but with "
				+ (components.isEmpty() ? "nothing" : components.get(0).getName()));
		}
		return new AnswerArea(components, details);
	}

	/**
	 * The sentence in the place of the answer: read-only, wrapping, with its tooltip
	 *
	 * @param text
	 *            what will show there, in the window's language
	 * @param tooltip
	 *            the tooltip
	 * @return the sentence, named {@link #TO_COME}
	 */
	static JTextArea toCome(final String text, final String tooltip)
	{
		JTextArea sentence = LethenonWalletPanel.message(TO_COME, false);
		sentence.setText(text);
		sentence.setToolTipText(tooltip);
		return sentence;
	}

	/**
	 * @return the components of the area, the sentence first, for the window's failure view
	 */
	List<Component> components()
	{
		return components;
	}

	/**
	 * Shows the answer or the sentence in its place
	 *
	 * @param answered
	 *            whether the window holds an answer
	 * @param detailsShown
	 *            whether the details are opened
	 */
	void show(final boolean answered, final boolean detailsShown)
	{
		for (Component component : components)
		{
			component.setVisible(component == toCome
				? !answered
				: answered && (component != details || detailsShown));
		}
		WindowFit.growToShow(toCome);
	}
}
