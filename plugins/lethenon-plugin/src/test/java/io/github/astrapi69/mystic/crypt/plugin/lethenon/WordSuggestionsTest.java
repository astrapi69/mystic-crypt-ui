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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.lethenon.SeedPhrase;

/**
 * The words of the word list a field offers while a word is typed into it (#804), read through
 * lethenon's {@link SeedPhrase#wordsStartingWith}
 */
class WordSuggestionsTest
{

	@Test
	@DisplayName("the start of a word offers the words of the list that begin so, in the list's order")
	void theStartOfAWord_offersTheWordsThatBeginSo()
	{
		assertEquals(List.of("abandon"), WordSuggestions.offerFor("aban"));
		assertEquals(List.of("act", "action", "actor", "actress", "actual"),
			WordSuggestions.offerFor("act"));
	}

	@Test
	@DisplayName("what is typed is read as a word is read: any case, without spaces around it")
	void whatIsTyped_isReadAsAWord()
	{
		assertEquals(List.of("abandon"), WordSuggestions.offerFor("  ABAN "));
	}

	@ParameterizedTest(name = "\"{0}\" offers nothing")
	@ValueSource(strings = { "", " ", "a", "abandon", "xq", "aban don", "abandonx" })
	@DisplayName("nothing is offered for less than two letters, for a word already complete and alone, for no match and for two words")
	void nothingIsOffered_whenThereIsNothingToChoose(final String typed)
	{
		assertTrue(WordSuggestions.offerFor(typed).isEmpty());
	}

	@Test
	@DisplayName("two letters offer at most eight words, the first eight of the list")
	void twoLetters_offerAtMostEightWords()
	{
		List<String> all = SeedPhrase.wordsStartingWith("ab");
		assertTrue(all.size() > WordSuggestions.MAX, "the boundary needs more than eight matches");

		assertEquals(all.subList(0, WordSuggestions.MAX), WordSuggestions.offerFor("ab"));
	}
}
