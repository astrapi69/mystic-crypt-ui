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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;

/**
 * What the "Properties" tab does to the entry it shows. The table holds a list of its own, so a
 * change reaches the entry only where the panel writes it there (#807, #808)
 */
class PropertiesPanelEditsTest
{

	private static MysticCryptEntryModelBean anEntryWithTwoProperties()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();
		entry.setProperties(new ArrayList<>(
			List.of(new KeyValuePair<>("first", "one"), new KeyValuePair<>("second", "two"))));
		return entry;
	}

	/**
	 * A panel whose dialog gets the given key and value typed into it and is then answered with the
	 * given option
	 */
	private static PropertiesPanel answering(final MysticCryptEntryModelBean entry,
		final String typedKey, final String typedValue, final int answer)
	{
		return new PropertiesPanel(BaseModel.of(entry))
		{
			@Override
			protected int askForProperty(final PropertiesNewEntryPanel panel, final String title)
			{
				panel.getTxtKey().setText(typedKey);
				panel.getTxtValue().setText(typedValue);
				return answer;
			}
		};
	}

	private static List<String> namesAndValues(final List<KeyValuePair<String, String>> properties)
	{
		return properties.stream().map(property -> property.getKey() + "=" + property.getValue())
			.toList();
	}

	private static List<String> inTheEntry(final MysticCryptEntryModelBean entry)
	{
		return namesAndValues(entry.getProperties());
	}

	private static List<String> inTheTable(final PropertiesPanel panel)
	{
		return namesAndValues(panel.getTblProperties().getGenericTableModel().getData());
	}

	@Test
	@DisplayName("Cancel in the property dialog leaves the entry and the table as they were (#807)")
	void cancelInThePropertyDialog_leavesTheEntryAsItWas()
	{
		MysticCryptEntryModelBean entry = anEntryWithTwoProperties();
		PropertiesPanel panel = answering(entry, "renamed", "typed", JOptionPane.CANCEL_OPTION);
		panel.getTblProperties().setRowSelectionInterval(0, 0);

		panel.onEditSelected();

		assertEquals(List.of("first=one", "second=two"), inTheEntry(entry));
		assertEquals(List.of("first=one", "second=two"), inTheTable(panel));
	}

	@Test
	@DisplayName("OK in the property dialog writes the new name and value into the entry and the table")
	void okInThePropertyDialog_writesTheChangeIntoTheEntryAndTheTable()
	{
		MysticCryptEntryModelBean entry = anEntryWithTwoProperties();
		PropertiesPanel panel = answering(entry, "renamed", "changed", JOptionPane.OK_OPTION);
		panel.getTblProperties().setRowSelectionInterval(0, 0);

		panel.onEditSelected();

		assertEquals(List.of("renamed=changed", "second=two"), inTheEntry(entry));
		assertEquals(List.of("renamed=changed", "second=two"), inTheTable(panel));
	}

	@Test
	@DisplayName("Remove takes the property out of the table and out of the entry (#808)")
	void remove_takesThePropertyOutOfTheEntry()
	{
		MysticCryptEntryModelBean entry = anEntryWithTwoProperties();
		PropertiesPanel panel = answering(entry, "", "", JOptionPane.CANCEL_OPTION);
		panel.getTblProperties().setRowSelectionInterval(1, 1);

		panel.onRemove(null);

		assertEquals(List.of("first=one"), inTheTable(panel));
		assertEquals(List.of("first=one"), inTheEntry(entry));
	}

	@Test
	@DisplayName("Remove with no row selected changes neither the table nor the entry")
	void removeWithNoRowSelected_changesNothing()
	{
		MysticCryptEntryModelBean entry = anEntryWithTwoProperties();
		PropertiesPanel panel = answering(entry, "", "", JOptionPane.CANCEL_OPTION);

		panel.onRemove(null);

		assertEquals(List.of("first=one", "second=two"), inTheTable(panel));
		assertEquals(List.of("first=one", "second=two"), inTheEntry(entry));
	}

	@Test
	@DisplayName("OK in the dialog of a new property adds it to the entry and the table")
	void okForANewProperty_addsItToTheEntryAndTheTable()
	{
		MysticCryptEntryModelBean entry = anEntryWithTwoProperties();
		PropertiesPanel panel = answering(entry, "third", "three", JOptionPane.OK_OPTION);

		panel.onAdd(null);

		assertEquals(List.of("first=one", "second=two", "third=three"), inTheEntry(entry));
		assertEquals(List.of("first=one", "second=two", "third=three"), inTheTable(panel));
	}

	@Test
	@DisplayName("Cancel in the dialog of a new property adds nothing")
	void cancelForANewProperty_addsNothing()
	{
		MysticCryptEntryModelBean entry = anEntryWithTwoProperties();
		PropertiesPanel panel = answering(entry, "third", "three", JOptionPane.CANCEL_OPTION);

		panel.onAdd(null);

		assertEquals(List.of("first=one", "second=two"), inTheEntry(entry));
		assertEquals(List.of("first=one", "second=two"), inTheTable(panel));
	}
}
