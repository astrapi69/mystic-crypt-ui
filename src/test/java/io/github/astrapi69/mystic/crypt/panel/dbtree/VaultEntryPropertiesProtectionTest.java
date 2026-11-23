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
package io.github.astrapi69.mystic.crypt.panel.dbtree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.collection.pair.KeyValuePair;

/**
 * The switch of the property dialog marks a custom property as protected or takes the mark away
 * (#1017). The mark belongs to the name, as the entry's set of protected names holds it, so taking
 * it away from one property never unprotects another property of the same name that the user did
 * not touch, the rule {@link VaultEntryProperties#renamed} already follows
 */
class VaultEntryPropertiesProtectionTest
{

	record Case(String name, List<String> names, Set<String> protectedBefore, int changed,
		boolean protect, Set<String> protectedAfter) {
		@Override
		public String toString()
		{
			return name;
		}
	}

	static Stream<Case> cases()
	{
		return Stream.of(
			new Case("marking a plain property protects its name", List.of("pin", "note"), Set.of(),
				0, true, Set.of("pin")),
			new Case("taking the mark away unprotects the name", List.of("pin", "note"),
				Set.of("pin", "note"), 0, false, Set.of("note")),
			new Case("marking a protected property again changes nothing", List.of("pin"),
				Set.of("pin"), 0, true, Set.of("pin")),
			new Case("taking the mark of a plain property away changes nothing",
				List.of("pin", "note"), Set.of("pin"), 1, false, Set.of("pin")),
			new Case(
				"taking the mark away while another property of the same name carries it keeps the name protected",
				List.of("pin", "pin"), Set.of("pin"), 0, false, Set.of("pin")),
			new Case("an entry without a set of protected names gets one when a property is marked",
				List.of("pin"), null, 0, true, Set.of("pin")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cases")
	void theSwitchSetsTheMarkOfTheName(final Case example)
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();
		List<KeyValuePair<String, String>> properties = new ArrayList<>();
		example.names().forEach(name -> properties.add(new KeyValuePair<>(name, "a value")));
		entry.setProperties(properties);
		entry.setProtectedPropertyKeys(example.protectedBefore());

		VaultEntryProperties.setProtected(entry, properties.get(example.changed()),
			example.protect());

		assertEquals(example.protectedAfter(), VaultEntryProperties.protectedNamesOf(entry));
	}
}
