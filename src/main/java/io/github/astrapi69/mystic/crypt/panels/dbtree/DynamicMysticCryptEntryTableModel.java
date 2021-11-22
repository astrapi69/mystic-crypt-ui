package io.github.astrapi69.mystic.crypt.panels.dbtree;

import io.github.astrapi69.swing.table.model.dynamic.DynamicTableColumnsModel;
import io.github.astrapi69.swing.table.model.dynamic.DynamicTableModel;
import lombok.NonNull;

public class DynamicMysticCryptEntryTableModel extends DynamicTableModel<MysticCryptEntryModelBean>
{
	public DynamicMysticCryptEntryTableModel(
		@NonNull DynamicTableColumnsModel<MysticCryptEntryModelBean> columnsModel)
	{
		super(columnsModel);
	}

	/**
	 * Returns the value for the cell at <code>columnIndex</code> and
	 * <code>rowIndex</code>.
	 *
	 * @param rowIndex    the row whose value is to be queried
	 * @param columnIndex the column whose value is to be queried
	 * @return the value Object at the specified cell
	 */
	@Override public Object getValueAt(int rowIndex, int columnIndex)
	{
		MysticCryptEntryModelBean mysticCryptEntryModelBean = getData().get(rowIndex);
		switch (columnIndex)
		{
			case 0 :
				return mysticCryptEntryModelBean.getTitle();
			case 1 :
				return mysticCryptEntryModelBean.getUserName();
			case 2 :
				return mysticCryptEntryModelBean.getPassword();
			case 3 :
				return mysticCryptEntryModelBean.getUrl();
			default :
				return null;
		}
	}
}
