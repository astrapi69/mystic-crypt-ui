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
import java.util.List;
import java.util.stream.Collectors;

/**
 * The 24 words of a restore or a backup check as they are typed, one position per field of
 * {@link WordGrid} (#804), and the phrase they give together, which is what the restore and the
 * check read.
 * <p>
 * The positions are Strings, because text fields hold them; they are emptied after every restore
 * and every check, and the decision record names that limit.
 */
final class TypedWords
{

	private final String[] positions = new String[BackupCheck.WORDS];

	/** Instantiates a model whose 24 positions are empty */
	TypedWords()
	{
		clear();
	}

	/**
	 * Gets what is typed at a position
	 *
	 * @param position
	 *            the position, counted from 1
	 * @return what is typed there, empty when nothing is
	 */
	String get(final int position)
	{
		return positions[index(position)];
	}

	/**
	 * Sets what is typed at a position
	 *
	 * @param position
	 *            the position, counted from 1
	 * @param word
	 *            what is typed there; a missing value is an empty position
	 */
	void set(final int position, final String word)
	{
		positions[index(position)] = word == null ? "" : word;
	}

	/**
	 * The phrase the positions give, as a restore and a check read it
	 *
	 * @return the typed positions in order, separated by one space; an empty position adds nothing
	 */
	String phrase()
	{
		return Arrays.stream(positions).map(String::strip).filter(word -> !word.isEmpty())
			.collect(Collectors.joining(" "));
	}

	/**
	 * Puts the words of a phrase at their positions, word 1 at position 1, and empties the
	 * positions after the last word. Words that do not fit stay together at position 24, so
	 * nothing of the phrase is lost and the count check can name how many there were
	 *
	 * @param words
	 *            the words, as {@link io.github.astrapi69.lethenon.SeedPhrase#parse} reads a phrase
	 */
	void spread(final List<String> words)
	{
		for (int position = 1; position <= positions.length; position++)
		{
			set(position, wordAt(words, position));
		}
	}

	/** Empties every position */
	void clear()
	{
		Arrays.fill(positions, "");
	}

	private static String wordAt(final List<String> words, final int position)
	{
		if (words.size() < position)
		{
			return "";
		}
		if (position < BackupCheck.WORDS)
		{
			return words.get(position - 1);
		}
		return String.join(" ", words.subList(position - 1, words.size()));
	}

	private static int index(final int position)
	{
		if (position < 1 || position > BackupCheck.WORDS)
		{
			throw new IllegalArgumentException(
				"a phrase has the positions 1 to " + BackupCheck.WORDS + ", not " + position);
		}
		return position - 1;
	}
}
