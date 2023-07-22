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

import java.util.List;

import io.github.astrapi69.swing.table.model.BaseTableModel;
import io.github.astrapi69.swing.table.model.TableColumnsModel;
import io.github.astrapi69.swing.table.model.thread.ThreadsTableModel;

public class MysticCryptEntryTableModel extends BaseTableModel<MysticCryptEntryModelBean>
{

	public MysticCryptEntryTableModel()
	{ //@formatter:off
		this(TableColumnsModel.builder().columnNames(
			new String[] { "Title", "Username", "Url" })
			.canEdit(new boolean[] { false, false, false })
			.columnClasses(new Class<?>[] { String.class, String.class, String.class })
			.build());
		//@formatter:on
	}

	/**
	 * Instantiates a new {@link ThreadsTableModel} object.
	 *
	 * @param columnsModel
	 *            the column model
	 */
	public MysticCryptEntryTableModel(final TableColumnsModel columnsModel)
	{
		super(columnsModel);
	}

	/**
	 * Returns the value for the cell at <code>columnIndex</code> and <code>rowIndex</code>.
	 *
	 * @param rowIndex
	 *            the row whose value is to be queried
	 * @param columnIndex
	 *            the column whose value is to be queried
	 * @return the value Object at the specified cell
	 */
	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		List<MysticCryptEntryModelBean> data = getData();
		MysticCryptEntryModelBean mysticCryptEntryModelBean = data.get(rowIndex);
		switch (columnIndex)
		{
			case 0 :
				return mysticCryptEntryModelBean.getTitle();
			case 1 :
				return mysticCryptEntryModelBean.getUserName();
			case 2 :
				return mysticCryptEntryModelBean.getUrl();
			default :
				return null;
		}
	}
}
