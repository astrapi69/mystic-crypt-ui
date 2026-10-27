/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.plugin.obfuscation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.crypt.api.obfuscation.rule.Operation;
import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationOperationRule;
import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationRule;
import io.github.astrapi69.xstream.ObjectToXmlExtensions;

/**
 * The reader of an imported rule table builds exactly the types a rule table holds and refuses
 * every other one before it is built; a table the export wrote reads back as it was.
 */
class ObfuscationRuleTableXmlTest
{

	private static final String FOREIGN = "<" + NotARuleType.class.getName()
		+ "><text>not part of a rule table</text></" + NotARuleType.class.getName() + ">";

	@BeforeEach
	void resetTheCount()
	{
		NotARuleType.CREATED.set(0);
	}

	private static List<KeyValuePair<Character, ObfuscationRule<Character, Character>>> simpleTable()
	{
		List<KeyValuePair<Character, ObfuscationRule<Character, Character>>> table = new ArrayList<>();
		table.add(KeyValuePair.<Character, ObfuscationRule<Character, Character>> builder()
			.key('a').value(new ObfuscationRule<>('a', 'q')).build());
		table.add(KeyValuePair.<Character, ObfuscationRule<Character, Character>> builder()
			.key('ä').value(new ObfuscationRule<>('ä', '7')).build());
		return table;
	}

	private static List<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> operationTable()
	{
		Set<Integer> indexes = new LinkedHashSet<>(List.of(0, 3, 7));
		List<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> table = new ArrayList<>();
		table.add(KeyValuePair.<Character, ObfuscationOperationRule<Character, Character>> builder()
			.key('b')
			.value(new ObfuscationOperationRule<>('b', indexes, true, Optional.of('B'),
				Operation.UPPERCASE, 'x'))
			.build());
		table.add(KeyValuePair.<Character, ObfuscationOperationRule<Character, Character>> builder()
			.key('c')
			.value(new ObfuscationOperationRule<>('c', new LinkedHashSet<>(), false,
				Optional.empty(), Operation.NONE, 'y'))
			.build());
		return table;
	}

	@Test
	@DisplayName("a simple rule table the export wrote reads back as it was")
	void aSimpleTable_readsBack()
	{
		var table = simpleTable();

		assertEquals(table, ObfuscationRuleTableXml.toSimpleRules(ObjectToXmlExtensions.toXml(table)));
	}

	@Test
	@DisplayName("an operation rule table the export wrote reads back as it was, every field filled")
	void anOperationTable_readsBack()
	{
		var table = operationTable();

		assertEquals(table,
			ObfuscationRuleTableXml.toOperationRules(ObjectToXmlExtensions.toXml(table)));
	}

	@Test
	@DisplayName("a simple rule table holding a type of this project that no rule table holds is refused, and the type is never built")
	void aSimpleTableWithAForeignType_isRefused()
	{
		assertRefused(ObjectToXmlExtensions.toXml(simpleTable()), ObfuscationRuleTableXml::toSimpleRules);
	}

	@Test
	@DisplayName("an operation rule table holding a type of this project that no rule table holds is refused, and the type is never built")
	void anOperationTableWithAForeignType_isRefused()
	{
		assertRefused(ObjectToXmlExtensions.toXml(operationTable()),
			ObfuscationRuleTableXml::toOperationRules);
	}

	private static void assertRefused(final String table, final Function<String, ?> reader)
	{
		String withAForeignType = table.replaceFirst("(<[a-z-]*list[^>]*>)", "$1" + FOREIGN);
		assertTrue(withAForeignType.contains(NotARuleType.class.getName()),
			"the foreign type is in the XML: " + withAForeignType);

		RuleTableHoldsAForeignTypeException refusal = assertThrows(
			RuleTableHoldsAForeignTypeException.class, () -> reader.apply(withAForeignType));

		assertTrue(refusal.getMessage().contains(NotARuleType.class.getName()),
			"the refusal names the type: " + refusal.getMessage());
		assertEquals(0, NotARuleType.CREATED.get(), "instances of the foreign type built");
	}
}
