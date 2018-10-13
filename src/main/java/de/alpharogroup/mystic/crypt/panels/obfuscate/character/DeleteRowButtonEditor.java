package de.alpharogroup.mystic.crypt.panels.obfuscate.character;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import de.alpharogroup.swing.table.model.GenericTableModel;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * The class {@link DeleteRowButtonEditor}
 * @deprecated use instead the same name class from project swing-components when new release is out
 */
public class DeleteRowButtonEditor extends DefaultCellEditor
{
	/**
	 * The listener interface for receiving deleteRowButton events. The class that is interested in
	 * processing a deleteRowButton event implements this interface, and the object created with
	 * that class is registered with a component using the component's
	 * <code>addDeleteRowButtonListener<code> method. When the deleteRowButton event occurs, that
	 * object's appropriate method is invoked.
	 *
	 * @see DeleteRowButtonEvent
	 */
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@Setter
	class DeleteRowButtonListener implements ActionListener
	{

		int row;
		JTable table;

		@Override
		public void actionPerformed(ActionEvent event)
		{
			if (table.getRowCount() > 0)
			{
				TableModel tableModel = table.getModel();
				GenericTableModel<?> model = (GenericTableModel<?>)tableModel;
				model.removeAt(this.row);
				DeleteRowButtonEditor.this.cancelCellEditing();
			}
		}
	}

	private static final long serialVersionUID = 1L;

	/** The button. */
	protected JButton button;

	/** The delete row button listener. */
	private final DeleteRowButtonListener deleteRowButtonListener;

	/**
	 * Instantiates a new {@link DeleteRowButtonEditor}
	 *
	 * @param checkBox
	 *            the check box
	 */
	public DeleteRowButtonEditor(JCheckBox checkBox)
	{
		super(checkBox);
		deleteRowButtonListener = new DeleteRowButtonListener();
		button = new JButton();
		button.setOpaque(true);
		button.addActionListener(deleteRowButtonListener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
		int row, int column)
	{
		deleteRowButtonListener.setRow(row);
		deleteRowButtonListener.setTable(table);

		button.setText(value == null ? "" : "Delete");
		return button;
	}

}