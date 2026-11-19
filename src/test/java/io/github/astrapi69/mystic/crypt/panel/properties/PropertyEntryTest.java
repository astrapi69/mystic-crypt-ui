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
package io.github.astrapi69.mystic.crypt.panel.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.collection.pair.KeyValuePair;

/**
 * The row type of the properties table since swing-table-components 4.0: a view of the entry's own
 * pair, never a copy of it
 */
class PropertyEntryTest
{

	@Test
	@DisplayName("a row shows the key and the value of the pair it views")
	void aRow_showsTheKeyAndTheValueOfThePair()
	{
		KeyValuePair<String, String> pair = new KeyValuePair<>("name", "value");
		PropertyEntry row = new PropertyEntry(pair);
		assertEquals("name", row.getKey());
		assertEquals("value", row.getValue());
		assertSame(pair, row.getPair());
	}

	@Test
	@DisplayName("a change of the pair is seen in the row, and setValue writes through to the pair")
	void aChangeOfThePair_isSeenInTheRow_andSetValueWritesThrough()
	{
		KeyValuePair<String, String> pair = new KeyValuePair<>("name", "value");
		PropertyEntry row = new PropertyEntry(pair);
		pair.setKey("renamed");
		assertEquals("renamed", row.getKey());
		assertEquals("value", row.setValue("other"));
		assertEquals("other", pair.getValue());
	}

	@Test
	@DisplayName("equality follows the contract of Map.Entry")
	void equality_followsTheContractOfMapEntry()
	{
		PropertyEntry row = new PropertyEntry(new KeyValuePair<>("name", "value"));
		Map.Entry<String, String> same = Map.entry("name", "value");
		assertEquals(row, same);
		assertEquals(same.hashCode(), row.hashCode());
		assertNotEquals(row, Map.entry("name", "other"));
	}

	@Test
	@DisplayName("a row without a pair is refused")
	void aRowWithoutAPair_isRefused()
	{
		assertThrows(NullPointerException.class, () -> new PropertyEntry(null));
	}
}
