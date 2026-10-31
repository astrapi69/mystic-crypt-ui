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

import java.util.Arrays;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

/**
 * Draws the three positions the three-word question asks, from 1 to 24, all different and in
 * ascending order. A request never asks the same positions as the request before it: a draw that
 * repeats them is drawn again.
 */
final class QuestionPositions
{

	/** How many words the question asks */
	static final int ASKED = 3;

	private final IntUnaryOperator below;

	private int[] previous = new int[0];

	/**
	 * Positions drawn with the given source of numbers
	 *
	 * @param below
	 *            gives a number from 0 up to, not including, its argument; a secure random's
	 *            {@code nextInt}
	 */
	QuestionPositions(final IntUnaryOperator below)
	{
		this.below = below;
	}

	/**
	 * The positions for the next request
	 *
	 * @return three different positions from 1 to 24, ascending, not those of the last request
	 */
	int[] next()
	{
		int[] drawn = draw();
		while (Arrays.equals(drawn, previous))
		{
			drawn = draw();
		}
		previous = drawn;
		return drawn.clone();
	}

	private int[] draw()
	{
		return IntStream.generate(() -> below.applyAsInt(BackupCheck.WORDS) + 1)
			.distinct().limit(ASKED).sorted().toArray();
	}
}
