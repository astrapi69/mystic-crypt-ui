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

import java.io.Serial;
import java.util.function.Predicate;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.swing.table.model.properties.StringKeyValueTableModel;

/**
 * The table model of the "Properties" tab. A value whose name the entry marks as protected - the
 * words the lethenon plugin keeps there, a custom field KeePass protected - is shown as
 * {@link #MASK}, never as itself; it is seen only in the edit dialog, on request. The mask is the
 * same for every value, so it does not give away the length either.
 * <p>
 * The masking is in {@link #getValueAt}, not in a renderer, because everything else that reads a
 * cell reads it there as well: copying a row, a sorter, an accessibility reader
 */
public class ProtectedPropertiesTableModel extends StringKeyValueTableModel
{

	/** What the table shows in place of a protected value */
	public static final String MASK = "********";

	@Serial
	private static final long serialVersionUID = 1L;

	private static final int VALUE_COLUMN = 1;

	private final transient Predicate<String> protectedName;

	/**
	 * Creates the table model
	 *
	 * @param protectedName
	 *            whether a property name is marked as protected, asked anew for every cell
	 */
	public ProtectedPropertiesTableModel(final Predicate<String> protectedName)
	{
		this.protectedName = protectedName;
	}

	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex)
	{
		if (columnIndex == VALUE_COLUMN && isProtected(getData().get(rowIndex)))
		{
			return MASK;
		}
		return super.getValueAt(rowIndex, columnIndex);
	}

	private boolean isProtected(final KeyValuePair<String, String> property)
	{
		return protectedName.test(property.getKey());
	}
}
