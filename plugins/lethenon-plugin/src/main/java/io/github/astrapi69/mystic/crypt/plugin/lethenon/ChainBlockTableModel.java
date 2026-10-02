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

import java.time.Instant;

import io.github.astrapi69.swing.table.model.GenericTableModel;

/**
 * The chain view's table: one row per block of an accepted chain, one column per field of
 * {@link ChainBlockRow}. Nothing is editable - the table shows a chain, it does not change one.
 */
public class ChainBlockTableModel extends GenericTableModel<ChainBlockRow>
{

	private static final long serialVersionUID = 1L;

	/** The column of the height */
	public static final int HEIGHT = 0;

	/** The column of the pun */
	public static final int PUN = 1;

	/** The column of the account the block paid */
	public static final int PAID_TO = 2;

	/** The column of the number of transfers */
	public static final int TRANSFERS = 3;

	/** The column of the block's own timestamp */
	public static final int TIME = 4;

	/** The column of the difficulty */
	public static final int DIFFICULTY = 5;

	private final String[] columnNames = {
			LethenonMessages.getString("lethenon.chain.column.height", "Height"),
			LethenonMessages.getString("lethenon.chain.column.pun", "Pun"),
			LethenonMessages.getString("lethenon.chain.column.paid.to", "Paid to"),
			LethenonMessages.getString("lethenon.chain.column.transfers", "Transfers"),
			LethenonMessages.getString("lethenon.chain.column.time", "Time (UTC)"),
			LethenonMessages.getString("lethenon.chain.column.difficulty", "Difficulty") };

	@Override
	public int getColumnCount()
	{
		return columnNames.length;
	}

	@Override
	public String getColumnName(final int column)
	{
		return columnNames[column];
	}

	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex)
	{
		ChainBlockRow block = get(rowIndex);
		return switch (columnIndex)
		{
			case HEIGHT -> block.height();
			case PUN -> block.pun();
			case PAID_TO -> block.paidTo();
			case TRANSFERS -> block.transfers();
			case TIME -> Instant.ofEpochMilli(block.timestamp()).toString();
			case DIFFICULTY -> block.difficulty();
			default -> throw new IllegalArgumentException(
				"the chain table has " + columnNames.length + " columns, not " + columnIndex);
		};
	}
}
