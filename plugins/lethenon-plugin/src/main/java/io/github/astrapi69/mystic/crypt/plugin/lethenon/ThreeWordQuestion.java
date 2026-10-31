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

import java.util.List;

/**
 * Asks for three words of a wallet's phrase, at positions chosen at random, to confirm that they were
 * written down. A wrong word is refused; the words are never stored here.
 */
final class ThreeWordQuestion
{

	private final List<String> words;

	private final int[] positions;

	private ThreeWordQuestion(final List<String> words, final int[] positions)
	{
		this.words = words;
		this.positions = positions.clone();
	}

	/**
	 * The question for the given positions, counted from 1
	 *
	 * @param words
	 *            the phrase, 24 words
	 * @param positions
	 *            three positions, from 1 to 24
	 * @return the question
	 */
	static ThreeWordQuestion ask(final List<String> words, final int[] positions)
	{
		return new ThreeWordQuestion(words, positions);
	}

	/**
	 * Checks the three answers
	 *
	 * @param answers
	 *            the words typed for the positions, in order
	 * @return whether every answer is the word at its position
	 */
	Result answer(final String... answers)
	{
		for (int i = 0; i < positions.length; i++)
		{
			if (!words.get(positions[i] - 1).equals(answers[i]))
			{
				return new Result(false);
			}
		}
		return new Result(true);
	}

	/** The outcome of one question */
	record Result(boolean correct)
	{
	}
}
