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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import io.github.astrapi69.swing.table.model.GenericTableModel;

/**
 * The table of the "Before you start" view (#540): one row per {@link LaunchCheck}, one column per
 * field. Nothing is editable - the table repeats a checklist, it does not change one.
 */
public class LaunchCheckTableModel extends GenericTableModel<LaunchCheck>
{

	private static final long serialVersionUID = 1L;

	/** The column of the activity */
	public static final int ACTIVITY = 0;

	/** The column of what MiCA asks of it */
	public static final int AUTHORISATION = 1;

	/** The column of the provision */
	public static final int PROVISION = 2;

	/** How many columns the table has */
	public static final int COLUMN_COUNT = 3;

	private final String[] columnNames = {
			LethenonMessages.getString(LaunchChecklist.KEY_PREFIX + "column.activity", "Activity"),
			LethenonMessages.getString(LaunchChecklist.KEY_PREFIX + "column.authorisation",
				"Authorisation under MiCA"),
			LethenonMessages.getString(LaunchChecklist.KEY_PREFIX + "column.provision",
				"Provision") };

	@Override
	public int getColumnCount()
	{
		return COLUMN_COUNT;
	}

	@Override
	public String getColumnName(final int column)
	{
		return columnNames[column];
	}

	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex)
	{
		LaunchCheck check = get(rowIndex);
		return switch (columnIndex)
		{
			case ACTIVITY -> check.activity();
			case AUTHORISATION -> check.authorisation();
			case PROVISION -> check.provision();
			default -> throw new IllegalArgumentException(
				"the checklist table has " + COLUMN_COUNT + " columns, not " + columnIndex);
		};
	}
}
