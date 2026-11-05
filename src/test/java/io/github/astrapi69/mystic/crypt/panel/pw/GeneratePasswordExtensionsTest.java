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
package io.github.astrapi69.mystic.crypt.panel.pw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The password generator keeps its character sets and its length when it moves from randomizer to
 * SecureRandom (#658): each option alone draws only from its set, and reaches all of it
 */
class GeneratePasswordExtensionsTest
{

	private static final int DRAWS = 600;

	static Stream<Arguments> options()
	{
		return Stream.of(
			Arguments.of("lowercase", GeneratePasswordModelBean.builder().lowercase(true).build(),
				"abcdefghijklmnopqrstuvwxyz"),
			Arguments.of("uppercase", GeneratePasswordModelBean.builder().uppercase(true).build(),
				"ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
			Arguments.of("digits", GeneratePasswordModelBean.builder().digits(true).build(),
				"0123456789"),
			Arguments.of("underscore", GeneratePasswordModelBean.builder().underscore(true).build(),
				"_"),
			Arguments.of("special", GeneratePasswordModelBean.builder().special(true).build(),
				"#@$%^&*?!"),
			Arguments.of("more special",
				GeneratePasswordModelBean.builder().moreSpecial(true).build(), "°§=~.:,;µ|€²³^"),
			Arguments.of("brackets", GeneratePasswordModelBean.builder().brackets(true).build(),
				"(){}[]<>"),
			Arguments.of("whitespace", GeneratePasswordModelBean.builder().whitespace(true).build(),
				" "),
			Arguments.of("minus", GeneratePasswordModelBean.builder().minus(true).build(), "-"),
			Arguments.of("plus", GeneratePasswordModelBean.builder().plus(true).build(), "+"));
	}

	/**
	 * Every other set beside the whitespace option. The space used to be lost whenever it was the
	 * last character of the alphabet, that is whenever neither minus nor plus was chosen, because
	 * the alphabet was trimmed (#670)
	 */
	static Stream<Arguments> setsBesideWhitespace()
	{
		return Stream.of(
			Arguments.of("lowercase", GeneratePasswordModelBean.builder().lowercase(true).build()),
			Arguments.of("uppercase", GeneratePasswordModelBean.builder().uppercase(true).build()),
			Arguments.of("digits", GeneratePasswordModelBean.builder().digits(true).build()),
			Arguments.of("underscore",
				GeneratePasswordModelBean.builder().underscore(true).build()),
			Arguments.of("special", GeneratePasswordModelBean.builder().special(true).build()),
			Arguments.of("more special",
				GeneratePasswordModelBean.builder().moreSpecial(true).build()),
			Arguments.of("brackets", GeneratePasswordModelBean.builder().brackets(true).build()),
			Arguments.of("minus", GeneratePasswordModelBean.builder().minus(true).build()),
			Arguments.of("plus", GeneratePasswordModelBean.builder().plus(true).build()));
	}

	@ParameterizedTest(name = "whitespace with {0} draws spaces")
	@MethodSource("setsBesideWhitespace")
	@DisplayName("the whitespace option draws spaces together with any other set")
	void generatePassword_drawsSpaces_whenWhitespaceIsChosenWithAnotherSet(final String option,
		final GeneratePasswordModelBean chosen)
	{
		GeneratePasswordModelBean model = chosen.toBuilder().whitespace(true).passwordLength(DRAWS)
			.build();

		String password = new String(GeneratePasswordExtensions.generatePassword(model));

		assertEquals(DRAWS, password.length());
		assertTrue(password.indexOf(' ') >= 0, "whitespace with " + option + ": " + DRAWS
			+ " draws without a single space: " + password);
	}

	@Test
	@DisplayName("no set chosen is reported as such, not as an empty alphabet deep in the draw")
	void generatePassword_refuses_whenNoSetIsChosen()
	{
		GeneratePasswordModelBean model = GeneratePasswordModelBean.builder().passwordLength(16)
			.build();

		assertFalse(GeneratePasswordExtensions.isAnySetChosen(model));
		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> GeneratePasswordExtensions.generatePassword(model));
		assertEquals("no character set is chosen to generate a password from",
			refused.getMessage());
	}

	@ParameterizedTest(name = "{0} alone is a chosen set")
	@MethodSource("options")
	@DisplayName("each option alone counts as a chosen set, the whitespace option included")
	void isAnySetChosen_isTrue_forEachOptionAlone(final String option,
		final GeneratePasswordModelBean chosen, final String expected)
	{
		assertTrue(GeneratePasswordExtensions.isAnySetChosen(chosen), option);
	}

	@ParameterizedTest(name = "{0} alone draws from \"{2}\"")
	@MethodSource("options")
	@DisplayName("each option alone draws only from its set, and reaches every character of it")
	void generatePassword_drawsFromTheChosenSet(final String option,
		final GeneratePasswordModelBean chosen, final String expected)
	{
		GeneratePasswordModelBean model = chosen.toBuilder().passwordLength(DRAWS).build();

		String password = new String(GeneratePasswordExtensions.generatePassword(model));

		assertEquals(DRAWS, password.length());
		assertTrue(password.chars().allMatch(c -> expected.indexOf(c) >= 0),
			option + ": only " + expected + ", got " + password);
		assertTrue(expected.chars().allMatch(c -> password.indexOf(c) >= 0),
			option + ": " + DRAWS + " draws reach every character of " + expected);
	}

	@Test
	@DisplayName("two options together give a password of the asked length from both sets")
	void generatePassword_combinesTheChosenSets()
	{
		GeneratePasswordModelBean model = GeneratePasswordModelBean.builder().passwordLength(16)
			.lowercase(true).digits(true).build();

		String password = new String(GeneratePasswordExtensions.generatePassword(model));

		assertEquals(16, password.length());
		assertTrue(password.chars()
			.allMatch(c -> Character.isDigit(c) || Character.isLowerCase(c) && c < 128), password);
	}
}
