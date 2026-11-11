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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The model behind the 24 fields of {@link WordGrid} (#804): one slot per position, and the phrase
 * the slots give together, which is what a restore and a backup check read
 */
class TypedWordsTest
{

	private final TypedWords words = new TypedWords();

	@Test
	@DisplayName("a fresh model has 24 empty positions and an empty phrase")
	void aFreshModel_isEmpty()
	{
		for (int position = 1; position <= 24; position++)
		{
			assertEquals("", words.get(position));
		}
		assertEquals("", words.phrase());
	}

	@Test
	@DisplayName("the phrase is the typed positions in order, empty ones left out")
	void thePhrase_isThePositionsInOrder_withoutTheEmptyOnes()
	{
		words.set(24, "zoo");
		words.set(1, "abandon");
		words.set(2, "  ");

		assertEquals("abandon zoo", words.phrase());
	}

	@Test
	@DisplayName("a missing value is an empty position")
	void aMissingValue_isAnEmptyPosition()
	{
		words.set(3, "abandon");

		words.set(3, null);

		assertEquals("", words.get(3));
	}

	@ParameterizedTest(name = "position {0}")
	@ValueSource(ints = { 0, 25, -1 })
	@DisplayName("a position outside 1 to 24 is refused, naming it")
	void aPositionOutsideTheGrid_isRefused(final int position)
	{
		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> words.set(position, "abandon"));
		assertTrue(refused.getMessage().contains(String.valueOf(position)), refused.getMessage());
		assertThrows(IllegalArgumentException.class, () -> words.get(position));
	}

	@Test
	@DisplayName("spreading puts word 1 at position 1 and so on, and empties the positions after the last word")
	void spreading_putsEachWordAtItsPosition()
	{
		words.set(24, "zoo");
		List<String> two = List.of("abandon", "ability");

		words.spread(two);

		assertEquals("abandon", words.get(1));
		assertEquals("ability", words.get(2));
		assertEquals("", words.get(24), "what was there before is replaced");
		assertEquals("abandon ability", words.phrase());
	}

	@Test
	@DisplayName("spreading more than 24 words keeps the ones that do not fit at position 24")
	void spreadingMoreThanTwentyFour_keepsTheRestAtTheLastPosition()
	{
		List<String> twentySix = new ArrayList<>(
			IntStream.rangeClosed(1, 24).mapToObj(position -> "abandon").toList());
		twentySix.add("ability");
		twentySix.add("zoo");

		words.spread(twentySix);

		assertEquals("abandon ability zoo", words.get(24));
		assertEquals(String.join(" ", twentySix), words.phrase());
	}

	@Test
	@DisplayName("clearing empties every position")
	void clear_emptiesEveryPosition()
	{
		words.set(1, "abandon");
		words.set(24, "zoo");

		words.clear();

		assertEquals("", words.phrase());
		assertEquals("", words.get(24));
	}
}
