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
package io.github.astrapi69.mystic.crypt.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.mystic.crypt.menu.PluginMenuOrder.Entry;
import io.github.astrapi69.swing.menu.enumeration.Anchor;

/**
 * No {@code JMenu}, no {@code DesktopMenu}, no display - just the ordering decision, so every
 * branch here is a real mutation-testing target (see {@code gradle/mutation-testing.gradle}).
 * Whether {@code DesktopMenu} actually builds the {@code JMenu} tree this order describes is a
 * different, Swing-level concern, covered separately.
 */
class PluginMenuOrderTest
{

	private static List<String> orderedNames(final List<String> namesAsFound)
	{
		List<Entry> entries = namesAsFound.stream().map(name -> new Entry(name, Anchor.LAST, null))
			.toList();
		return PluginMenuOrder.orderedNames(entries);
	}

	static Stream<Arguments> unanchoredOrdering()
	{
		return Stream.of(
			Arguments.of("a list already in the caller's chosen order",
				Arrays.asList("Certificate", "Checksum", "Key Generation", "Obfuscation")),
			Arguments.of("a single entry", Arrays.asList("Solo")),
			Arguments.of("an odd case mix, unchanged either way",
				Arrays.asList("cherry", "Banana", "apple")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("unanchoredOrdering")
	@DisplayName("with no anchor declared, entries keep the exact order they were given in")
	void withNoAnchorDeclaredEntriesKeepTheOrderTheyWereGivenIn(final String scenario,
		final List<String> asGiven)
	{
		// alphabetical order, or any other order, is decided by the caller before this class ever
		// sees the names - this pins that, with no anchor involved, nothing here reorders them
		assertEquals(asGiven, orderedNames(asGiven), scenario);
	}

	static Stream<Arguments> anchorResolution()
	{
		return Stream.of(
			Arguments.of("AFTER lands right after the named entry",
				new Entry("Zebra", Anchor.AFTER, "Checksum"),
				List.of("Checksum", "Zebra", "Obfuscation")),
			Arguments.of("BEFORE lands right before the named entry",
				new Entry("Aardvark", Anchor.BEFORE, "Obfuscation"),
				List.of("Checksum", "Aardvark", "Obfuscation")),
			Arguments.of("FIRST overrides the alphabetical position",
				new Entry("Zebra", Anchor.FIRST, null),
				List.of("Zebra", "Checksum", "Obfuscation")),
			Arguments.of("a target that is not in the list does not break the order",
				new Entry("Zebra", Anchor.AFTER, "Not Installed"),
				List.of("Checksum", "Obfuscation", "Zebra")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("anchorResolution")
	@DisplayName("an entry anchored relative to a fixed pair of siblings lands where it asked to")
	void anAnchoredEntryLandsWhereItAskedTo(final String scenario, final Entry anchored,
		final List<String> expected)
	{
		List<Entry> entries = List.of(new Entry("Checksum", Anchor.LAST, null),
			new Entry("Obfuscation", Anchor.LAST, null), anchored);

		assertEquals(expected, PluginMenuOrder.orderedNames(entries), scenario);
	}

	@Test
	@DisplayName("an empty list orders to an empty list")
	void anEmptyListOrdersToAnEmptyList()
	{
		assertEquals(List.of(), PluginMenuOrder.orderedNames(List.of()));
	}

}
