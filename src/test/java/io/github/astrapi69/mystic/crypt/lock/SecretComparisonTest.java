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
package io.github.astrapi69.mystic.crypt.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The comparison of one typed secret with another: the same answers as {@code Arrays.equals}, in
 * time that does not depend on where the two differ (#916)
 */
class SecretComparisonTest
{

	static Stream<Arguments> pairs()
	{
		return Stream.of(
			Arguments.of("the same characters", chars("abc-123"), chars("abc-123"), true),
			Arguments.of("the first character differs", chars("xbc-123"), chars("abc-123"), false),
			Arguments.of("the last character differs", chars("abc-12x"), chars("abc-123"), false),
			Arguments.of("one is longer", chars("abc-1234"), chars("abc-123"), false),
			Arguments.of("one is a prefix of the other", chars("abc"), chars("abc-123"), false),
			Arguments.of("both empty", new char[0], new char[0], true),
			Arguments.of("one empty", new char[0], chars("a"), false),
			Arguments.of("both missing", null, null, true),
			Arguments.of("the first missing", null, chars("a"), false),
			Arguments.of("the second missing", chars("a"), null, false),
			Arguments.of("missing and empty", null, new char[0], false));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("pairs")
	@DisplayName("answers what Arrays.equals answers")
	void answersWhatArraysEqualsAnswers(final String name, final char[] first, final char[] second,
		final boolean expected)
	{
		assertEquals(expected, SecretComparison.sameCharacters(first, second));
		assertEquals(java.util.Arrays.equals(first, second),
			SecretComparison.sameCharacters(first, second), "the meaning of Arrays.equals is kept");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("pairs")
	@DisplayName("leaves both arrays as they were")
	void leavesBothArraysAsTheyWere(final String name, final char[] first, final char[] second,
		final boolean expected)
	{
		char[] firstBefore = first == null ? null : first.clone();
		char[] secondBefore = second == null ? null : second.clone();

		SecretComparison.sameCharacters(first, second);

		assertEquals(java.util.Arrays.toString(firstBefore), java.util.Arrays.toString(first));
		assertEquals(java.util.Arrays.toString(secondBefore), java.util.Arrays.toString(second));
	}

	private static char[] chars(final String text)
	{
		return text.toCharArray();
	}
}
