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
package io.github.astrapi69.mystic.crypt.plugin.obfuscation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationRule;
import io.github.astrapi69.mystic.crypt.obfuscation.simple.SimpleObfuscatorExtensions;

/**
 * Headless proof that the simple obfuscation round-trips, exercising exactly the library calls the
 * fixed plugin panel performs. This guards the bug that was fixed while extracting the obfuscation
 * feature into this plugin: the simple variant's "Disentangle" used to re-obfuscate a stored key
 * and undo it (always returning the original key, ignoring the obfuscated text). The panel
 * disentangles the actual obfuscated text with the library's {@code disentangle(rules, text)},
 * which once reversed a replacement only when the replacement character was itself an original
 * character; the mystic-crypt the catalog names reverses every one, which this test pins (#897).
 * <p>
 * The operated variant's round-trip is position/index sensitive and is covered by the mystic-crypt
 * library's own obfuscation tests, not duplicated here
 */
class ObfuscationRoundTripTest
{

	@Test
	void simpleObfuscationRoundTripsThroughTheLibraryCallsTheFixedPanelUses()
	{
		BiMap<Character, ObfuscationRule<Character, Character>> rules = HashBiMap.create();
		rules.put('a', simpleRule('a', 'x'));
		rules.put('b', simpleRule('b', 'y'));
		rules.put('c', simpleRule('c', 'z'));

		String original = "cabbage";
		// what onEncrypt does
		String obfuscated = SimpleObfuscatorExtensions.obfuscateWith(rules, original);
		assertEquals("zxyyxge", obfuscated, "each mapped character must be replaced, others kept");

		// what onDecrypt does - disentangle the OBFUSCATED text with the library's own method
		String disentangled = SimpleObfuscatorExtensions.disentangle(rules, obfuscated);
		assertEquals(original, disentangled, "disentangle must invert obfuscate");
	}

	/**
	 * The library's own {@code disentangle(rules, text)} inverts {@code obfuscateWith} on the
	 * mystic-crypt the catalog names, including the plain substitution whose replacement is no
	 * original character - the case it once got wrong, and the reason the panel went through the
	 * character bi-map instead (#897). It agrees with {@code disentangleBiMap} on every case
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("ruleSets")
	void theLibrarysDisentangleInvertsObfuscate(final String description, final char[][] pairs,
		final String original)
	{
		BiMap<Character, ObfuscationRule<Character, Character>> rules = HashBiMap.create();
		for (char[] pair : pairs)
		{
			rules.put(pair[0], simpleRule(pair[0], pair[1]));
		}
		String obfuscated = SimpleObfuscatorExtensions.obfuscateWith(rules, original);

		assertEquals(original, SimpleObfuscatorExtensions.disentangle(rules, obfuscated),
			description + ": disentangle must invert obfuscate");
		assertEquals(
			SimpleObfuscatorExtensions.disentangleBiMap(
				SimpleObfuscatorExtensions.toCharacterBiMap(rules), obfuscated),
			SimpleObfuscatorExtensions.disentangle(rules, obfuscated),
			description + ": both library routes give the same text");
	}

	static Stream<Arguments> ruleSets()
	{
		return Stream.of(
			Arguments.of("a replacement that is no original character",
				new char[][] { { 'a', 'x' } }, "cab"),
			Arguments.of("two characters swapped", new char[][] { { 'a', 'b' }, { 'b', 'a' } },
				"abba"),
			// the text holds no unmapped "c": that one would collide with the replacement of "b",
			// and a substitution that is not one-to-one on the text cannot be undone by any route
			Arguments.of("a chain of replacements", new char[][] { { 'a', 'b' }, { 'b', 'c' } },
				"abab"),
			Arguments.of("unmapped characters stay",
				new char[][] { { 'a', 'x' }, { 'b', 'y' }, { 'c', 'z' } }, "cabbage, 42!"));
	}

	@Test
	void simpleDisentangleOfPlainTextWithoutRulesIsANoOp()
	{
		// the old code threw a NullPointerException when Disentangle was clicked before Obfuscate;
		// the fixed path just returns the input unchanged when there is nothing to disentangle
		BiMap<Character, ObfuscationRule<Character, Character>> emptyRules = HashBiMap.create();
		BiMap<Character, Character> characterBiMap = SimpleObfuscatorExtensions
			.toCharacterBiMap(emptyRules);
		assertEquals("nothing-to-do",
			SimpleObfuscatorExtensions.disentangleBiMap(characterBiMap, "nothing-to-do"));
	}

	private static ObfuscationRule<Character, Character> simpleRule(char character, char replaceWith)
	{
		return ObfuscationRule.<Character, Character> builder().character(character)
			.replaceWith(replaceWith).build();
	}
}
