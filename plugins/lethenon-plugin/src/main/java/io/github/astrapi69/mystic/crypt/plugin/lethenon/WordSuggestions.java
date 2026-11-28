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

import io.github.astrapi69.lethenon.SeedPhrase;

/**
 * The words of the word list a field of the word grid offers while a word is typed into it (#804).
 * The list is lethenon's {@link SeedPhrase#wordsStartingWith}; this class decides when offering
 * them helps: from two letters on, since one letter matches up to a hundred words, at most
 * {@value #MAX} at a time, and not once the field holds a whole word that no other word continues.
 * BIP-39 makes the first four letters of a word unique, so four typed letters always narrow the
 * offer to one word.
 */
final class WordSuggestions
{

	/** The most words offered at once */
	static final int MAX = 8;

	private static final int FEWEST_LETTERS = 2;

	private WordSuggestions()
	{
	}

	/**
	 * The words to offer for what a field holds
	 *
	 * @param typed
	 *            the text of the field
	 * @return the words to offer, in the order of the word list; empty when there is nothing to
	 *         choose
	 */
	static List<String> offerFor(final String typed)
	{
		List<String> read = SeedPhrase.parse(typed);
		if (read.size() != 1 || read.getFirst().length() < FEWEST_LETTERS)
		{
			return List.of();
		}
		List<String> matches = SeedPhrase.wordsStartingWith(read.getFirst());
		if (matches.size() == 1 && matches.getFirst().equals(read.getFirst()))
		{
			return List.of();
		}
		return matches.subList(0, Math.min(MAX, matches.size()));
	}
}
