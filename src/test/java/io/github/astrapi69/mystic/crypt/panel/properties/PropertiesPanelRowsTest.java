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
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;

/**
 * Which property a row of the "Properties" table stands for, once there are several and some look
 * alike. A row is a {@link PropertyEntry}, a view of the entry's own pair, and the table finds it
 * again by the equality of {@link java.util.Map.Entry}: the same key and value (#1047)
 */
class PropertiesPanelRowsTest
{

	@SafeVarargs
	private static MysticCryptEntryModelBean anEntryWith(
		final KeyValuePair<String, String>... pairs)
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();
		entry.setProperties(new ArrayList<>(List.of(pairs)));
		return entry;
	}

	private static PropertiesPanel answering(final MysticCryptEntryModelBean entry,
		final String typedKey, final String typedValue)
	{
		return new PropertiesPanel(BaseModel.of(entry))
		{
			@Override
			protected int askForProperty(final PropertiesNewEntryPanel panel, final String title)
			{
				panel.getTxtKey().setText(typedKey);
				panel.getTxtValue().setText(typedValue);
				return JOptionPane.OK_OPTION;
			}
		};
	}

	private static List<String> asText(final List<KeyValuePair<String, String>> properties)
	{
		return properties.stream().map(property -> property.getKey() + "=" + property.getValue())
			.toList();
	}

	@Test
	@DisplayName("OK in the dialog of the second row changes the second property and leaves the first")
	void okForTheSecondRow_changesOnlyTheSecondProperty()
	{
		MysticCryptEntryModelBean entry = anEntryWith(new KeyValuePair<>("first", "one"),
			new KeyValuePair<>("second", "two"));
		PropertiesPanel panel = answering(entry, "second", "changed");
		panel.getTblProperties().setRowSelectionInterval(1, 1);

		panel.onEditSelected();

		assertEquals(List.of("first=one", "second=changed"), asText(entry.getProperties()));
		assertEquals("changed", panel.getTblProperties().getValueAt(1, 1));
		assertEquals("one", panel.getTblProperties().getValueAt(0, 1));
	}

	@Test
	@DisplayName("Remove of the second of two identical properties leaves exactly one of them in the table and in the entry")
	void removeOfOneOfTwoIdenticalProperties_leavesOne()
	{
		MysticCryptEntryModelBean entry = anEntryWith(new KeyValuePair<>("pin", "4711"),
			new KeyValuePair<>("pin", "4711"), new KeyValuePair<>("other", "x"));
		PropertiesPanel panel = answering(entry, "", "");
		panel.getTblProperties().setRowSelectionInterval(1, 1);

		panel.onRemove(null);

		assertEquals(List.of("pin=4711", "other=x"), asText(entry.getProperties()));
		assertEquals(2, panel.getTblProperties().getRowCount());
		assertEquals("pin", panel.getTblProperties().getValueAt(0, 0));
		assertEquals("other", panel.getTblProperties().getValueAt(1, 0));
	}

	@Test
	@DisplayName("a row of the table is a view of the entry's own pair, not a copy of it")
	void aRow_isTheEntrysOwnPair()
	{
		KeyValuePair<String, String> own = new KeyValuePair<>("first", "one");
		MysticCryptEntryModelBean entry = anEntryWith(own);
		PropertiesPanel panel = new PropertiesPanel(BaseModel.of(entry));

		PropertyEntry row = (PropertyEntry)panel.getTblProperties().getGenericTableModel().getData()
			.get(0);

		assertSame(own, row.getPair());
	}
}
