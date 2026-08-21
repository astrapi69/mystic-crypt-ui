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

import org.junit.jupiter.api.Test;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationRule;
import io.github.astrapi69.mystic.crypt.obfuscation.simple.SimpleObfuscatorExtensions;

/**
 * Headless proof that the simple obfuscation round-trips, exercising exactly the library calls the
 * fixed plugin panel performs. This guards the bug that was fixed while extracting the obfuscation
 * feature into this plugin: the simple variant's "Disentangle" used to re-obfuscate a stored key
 * and undo it (always returning the original key, ignoring the obfuscated text), and even the
 * library convenience {@code disentangle(rules, text)} only reverses a replacement when the
 * replacement character is itself an original character. The fixed panel disentangles the actual
 * obfuscated text through the character bi-map's inverse, which is what this test verifies.
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

		// what the fixed onDecrypt does - disentangle the OBFUSCATED text via the inverse bi-map
		BiMap<Character, Character> characterBiMap = SimpleObfuscatorExtensions
			.toCharacterBiMap(rules);
		String disentangled = SimpleObfuscatorExtensions.disentangleBiMap(characterBiMap,
			obfuscated);
		assertEquals(original, disentangled, "disentangle must invert obfuscate");
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
