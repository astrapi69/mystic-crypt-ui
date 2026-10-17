/*
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

import java.awt.Font;
import java.util.Locale;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;

import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.model.LambdaModel;
import org.danekja.java.util.function.serializable.SerializableSupplier;
import io.github.astrapi69.swing.model.component.JMTextArea;
import io.github.astrapi69.swing.model.component.JMTextField;

/**
 * The parts every window of this plugin answers with (#604): the answer in one sentence in a
 * larger font, read-only fields that show the model and never write into it, a Copy button that
 * copies the whole value a field may show shortened, and the texts of what was checked, which
 * every window that replays a chain puts behind "Show details".
 */
final class AnswerFields
{

	/** How much larger the answer is than the text around it */
	private static final float ANSWER_SCALE = 1.5f;

	private AnswerFields()
	{
	}

	/**
	 * The area the answer is written in: wrapping, read-only, without a frame, bold and larger than
	 * the text around it, named {@code txtAnswer}
	 *
	 * @param tooltip
	 *            what the answer is, in plain words
	 * @return the area
	 */
	static JMTextArea answerArea(final String tooltip)
	{
		JMTextArea area = new JMTextArea(2, 40);
		area.setName("txtAnswer");
		area.setEditable(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setOpaque(false);
		area.setBorder(BorderFactory.createEmptyBorder());
		Font base = new JLabel().getFont();
		area.setFont(base.deriveFont(Font.BOLD, base.getSize2D() * ANSWER_SCALE));
		area.setToolTipText(tooltip);
		return area;
	}

	/**
	 * A field that shows a value and cannot be typed into
	 *
	 * @param name
	 *            the component's name, which the tests find it by
	 * @param tooltip
	 *            what is in it and what to do with it, in plain words
	 * @return the field
	 */
	static JMTextField readOnly(final String name, final String tooltip)
	{
		JMTextField field = new JMTextField(34);
		field.setName(name);
		field.setEditable(false);
		field.setToolTipText(tooltip);
		return field;
	}

	/**
	 * Binds a read-only field to what the model holds; the field never writes back
	 *
	 * @param field
	 *            the field
	 * @param shown
	 *            what it shows
	 */
	static void show(final JMTextField field, final SerializableSupplier<String> shown)
	{
		field.setPropertyModel(LambdaModel.of(shown, AnswerFields::shownOnly));
	}

	/**
	 * Binds the answer area to what the model holds; the area never writes back
	 *
	 * @param area
	 *            the area
	 * @param shown
	 *            what it shows
	 */
	static void show(final JMTextArea area, final SerializableSupplier<String> shown)
	{
		area.setPropertyModel(LambdaModel.of(shown, AnswerFields::shownOnly));
	}

	/**
	 * Puts a text into a read-only field and shows it from its beginning, so a value wider than the
	 * field starts where it starts rather than where the caret was left
	 *
	 * @param field
	 *            the field
	 * @param text
	 *            the text
	 */
	static void setText(final JMTextField field, final String text)
	{
		field.setText(text);
		field.setCaretPosition(0);
	}

	/**
	 * A Copy button, which copies the whole value: what a field shows may be shortened
	 *
	 * @param name
	 *            the button's name
	 * @param value
	 *            the whole value, read when the button is pressed
	 * @param tooltip
	 *            what is copied and what for
	 * @param locale
	 *            the language of the window
	 * @return the button
	 */
	static JButton copyButton(final String name, final Supplier<String> value,
		final String tooltip, final Locale locale)
	{
		return LethenonSwing.button(name,
			LethenonMessages.getString("lethenon.answer.button.copy", "Copy", locale),
			event -> LethenonSwing.copy(value.get()), tooltip);
	}

	/**
	 * The text of the button that opens or closes the details
	 *
	 * @param shown
	 *            whether the details are shown now
	 * @param locale
	 *            the language of the window
	 * @return "Show details" or "Hide details"
	 */
	static String detailsButtonText(final boolean shown, final Locale locale)
	{
		return shown
			? LethenonMessages.getString("lethenon.answer.button.details.hide", "Hide details",
				locale)
			: LethenonMessages.getString("lethenon.answer.button.details.show", "Show details",
				locale);
	}

	/**
	 * What a replay checked, in words: "3 blocks, 2 transfers, 2 signatures"
	 *
	 * @param replay
	 *            the replay
	 * @param locale
	 *            the language of the window
	 * @return the text
	 */
	static String chainChecked(final ChainReplayReport replay, final Locale locale)
	{
		return LethenonMessages.format("lethenon.answer.chain.checked",
			"{0,choice,0#no blocks|1#1 block|1<{0,number,integer} blocks}, "
				+ "{1,choice,0#no transfers|1#1 transfer|1<{1,number,integer} transfers}, "
				+ "{2,choice,0#no signatures|1#1 signature|1<{2,number,integer} signatures}",
			locale, replay.blocks(), replay.transactions(), replay.signatures());
	}

	/**
	 * The label of the field that shows what a replay checked
	 *
	 * @param locale
	 *            the language of the window
	 * @return the label
	 */
	static JLabel chainCheckedLabel(final Locale locale)
	{
		return new JLabel(
			LethenonMessages.getString("lethenon.answer.label.chain.checked", "Chain checked:", locale));
	}

	/**
	 * The tooltip of the field that shows what a replay checked
	 *
	 * @param locale
	 *            the language of the window
	 * @return the tooltip
	 */
	static String chainCheckedTooltip(final Locale locale)
	{
		return LethenonMessages.getString("lethenon.answer.tooltip.chain.checked",
			"every block, transfer and signature of the chain was checked here; nothing was taken "
				+ "on trust",
			locale);
	}

	/**
	 * The label of the field that shows all the money on the chain
	 *
	 * @param locale
	 *            the language of the window
	 * @return the label
	 */
	static JLabel supplyLabel(final Locale locale)
	{
		return new JLabel(LethenonMessages.getString("lethenon.answer.label.supply",
			"All the money there is:", locale));
	}

	/**
	 * The tooltip of the field that shows all the money on the chain
	 *
	 * @param locale
	 *            the language of the window
	 * @return the tooltip
	 */
	static String supplyTooltip(final Locale locale)
	{
		return LethenonMessages.getString("lethenon.answer.tooltip.supply",
			"all the LETH on this chain, everybody's together; it grows only by the small reward "
				+ "each new block adds",
			locale);
	}

	/**
	 * The name a person reads for a signature suite: "Ed25519", "ML-DSA-65"
	 *
	 * @param suite
	 *            the suite
	 * @return its name
	 */
	static String suiteName(final SignatureSuite suite)
	{
		return suite == SignatureSuite.ED25519 ? "Ed25519"
			: suite.identifier().toUpperCase(Locale.ROOT);
	}

	/**
	 * What a read-only field writes back into the model: nothing. The field shows the model and
	 * never writes into it, so a shortened value cannot replace the whole one Copy copies
	 *
	 * @param shown
	 *            the text the field shows
	 */
	private static void shownOnly(final String shown)
	{
		// a read-only field's text is derived from the model, never the other way round
	}
}
