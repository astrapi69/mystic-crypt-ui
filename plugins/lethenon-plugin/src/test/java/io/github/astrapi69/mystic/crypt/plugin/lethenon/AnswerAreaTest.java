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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The answer area of a window that runs an action (rule G3, draft 06, #820): the sentence until
 * there is an answer, the answer once there is one, the details only while they are opened
 */
class AnswerAreaTest
{

	private final JPanel form = new JPanel();

	private final JLabel input = new JLabel("an input above the answer");

	private final JTextArea toCome = AnswerArea.toCome("what will show here",
		"where the answer appears");

	private final JTextField answer = new JTextField();

	private final JPanel details = new JPanel();

	/**
	 * What the window holds, and what then shows
	 *
	 * @param state
	 *            the window's state, in words
	 * @param answered
	 *            whether the window holds an answer
	 * @param detailsShown
	 *            whether the details are opened
	 * @param sentenceShows
	 *            whether the sentence shows
	 * @param answerShows
	 *            whether the answer shows
	 * @param detailsShow
	 *            whether the details show
	 */
	record Visibility(String state, boolean answered, boolean detailsShown, boolean sentenceShows,
		boolean answerShows, boolean detailsShow)
	{
		@Override
		public String toString()
		{
			return state;
		}
	}

	static Stream<Arguments> states()
	{
		return Stream.of(
			Arguments.of(new Visibility("no answer yet", false, false, true, false, false)),
			Arguments.of(new Visibility("no answer, the details opened before it was cleared",
				false, true, true, false, false)),
			Arguments.of(new Visibility("an answer, the details closed", true, false, false, true,
				false)),
			Arguments.of(new Visibility("an answer, the details opened", true, true, false, true,
				true)));
	}

	private AnswerArea anAnswerAfterTheInput()
	{
		form.add(input);
		form.add(toCome);
		form.add(answer);
		form.add(details);
		return AnswerArea.of(form, 1, details);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("states")
	void show_putsTheSentenceInThePlaceOfTheAnswer_untilThereIsOne(final Visibility expected)
	{
		AnswerArea area = anAnswerAfterTheInput();

		area.show(expected.answered(), expected.detailsShown());

		assertEquals(expected.sentenceShows(), toCome.isVisible(), "the sentence");
		assertEquals(expected.answerShows(), answer.isVisible(), "the answer");
		assertEquals(expected.detailsShow(), details.isVisible(), "the details");
		assertTrue(input.isVisible(), "what stands above the answer is not part of it");
	}

	@Test
	void theArea_isWhatTheFormHoldsFromTheStart_theSentenceFirst()
	{
		AnswerArea area = anAnswerAfterTheInput();

		assertEquals(List.of(toCome, answer, details), area.components());
	}

	@Test
	void theSentence_isNamedReadOnlyAndExplainsItself()
	{
		assertEquals(AnswerArea.TO_COME, toCome.getName());
		assertEquals("what will show here", toCome.getText());
		assertEquals("where the answer appears", toCome.getToolTipText());
		assertFalse(toCome.isEditable());
		assertTrue(toCome.getLineWrap());
	}

	@Test
	void anAnswerWithoutTheSentence_isRefused_namingWhatItStartsWith()
	{
		answer.setName("txtAnswer");
		form.add(input);
		form.add(answer);

		IllegalStateException refused = assertThrows(IllegalStateException.class,
			() -> AnswerArea.of(form, 1, details));

		assertEquals("the answer of JPanel does not start with the sentence txtAnswerToCome, "
			+ "but with txtAnswer", refused.getMessage());
	}

	@Test
	void anEmptyAnswer_isRefused_namingNothing()
	{
		form.add(input);

		IllegalStateException refused = assertThrows(IllegalStateException.class,
			() -> AnswerArea.of(form, 1, details));

		assertEquals("the answer of JPanel does not start with the sentence txtAnswerToCome, "
			+ "but with nothing", refused.getMessage());
	}
}
