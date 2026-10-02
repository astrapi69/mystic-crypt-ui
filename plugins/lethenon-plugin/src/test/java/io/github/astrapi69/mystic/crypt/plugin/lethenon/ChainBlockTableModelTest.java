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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The chain view's table shows the rows exactly as the replay produced them, one column per field,
 * and nothing in it can be edited - it is a view of a chain, not a way to change one.
 */
class ChainBlockTableModelTest
{

	private static final ChainBlockRow GENESIS = new ChainBlockRow(0L, "in the beginning #95",
		"302a300506032b6570", 0, 1_759_000_000_000L, 8);

	private static final ChainBlockRow SECOND = new ChainBlockRow(1L, "the second pun #3",
		"302a300506032b6571", 1, 1_759_000_120_000L, 9);

	@Test
	@DisplayName("one row per block, in the order the chain holds them")
	void rows_followTheChain()
	{
		ChainBlockTableModel model = new ChainBlockTableModel();

		model.setData(List.of(GENESIS, SECOND));

		assertEquals(2, model.getRowCount());
		assertEquals(0L, model.getValueAt(0, ChainBlockTableModel.HEIGHT));
		assertEquals(1L, model.getValueAt(1, ChainBlockTableModel.HEIGHT));
	}

	@ParameterizedTest(name = "column {0} shows {1}")
	@CsvSource({ "0, 1", "1, the second pun #3", "2, 302a300506032b6571", "3, 1",
			"4, 2025-09-27T19:08:40Z", "5, 9" })
	void eachColumn_showsItsField(final int column, final String shown)
	{
		ChainBlockTableModel model = new ChainBlockTableModel();
		model.setData(List.of(GENESIS, SECOND));

		assertEquals(shown, String.valueOf(model.getValueAt(1, column)));
	}

	@Test
	@DisplayName("six named columns, none of them editable")
	void columns_areNamedAndReadOnly()
	{
		ChainBlockTableModel model = new ChainBlockTableModel();
		model.setData(List.of(GENESIS));

		assertEquals(6, model.getColumnCount());
		assertEquals("Height", model.getColumnName(ChainBlockTableModel.HEIGHT));
		assertEquals("Pun", model.getColumnName(ChainBlockTableModel.PUN));
		assertEquals("Paid to", model.getColumnName(ChainBlockTableModel.PAID_TO));
		for (int column = 0; column < model.getColumnCount(); column++)
		{
			assertFalse(model.isCellEditable(0, column), "column " + column);
		}
	}
}
