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

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.button.state.GenericButtonGenericJXTableStateMachine;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.dialog.JOptionPaneExtensions;
import io.github.astrapi69.swing.table.GenericJTable;
import io.github.astrapi69.swing.table.model.properties.StringKeyValueTableModel;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;

@Getter
public class PropertiesPanel extends BasePanel<MysticCryptEntryModelBean>
{
	@Serial
	private static final long serialVersionUID = 1L;
	GenericButtonGenericJXTableStateMachine<KeyValuePair<String, String>> btnRemoveStateMachine;
	GenericButtonGenericJXTableStateMachine<KeyValuePair<String, String>> btnEditStateMachine;
	private javax.swing.JButton btnAdd;
	private javax.swing.JButton btnEdit;
	private javax.swing.JButton btnRemove;
	private javax.swing.JScrollPane srcProperties;
	private GenericJTable<KeyValuePair<String, String>> tblProperties;
	private StringKeyValueTableModel tableModel;

	public PropertiesPanel()
	{
		this(BaseModel.of(MysticCryptEntryModelBean.builder().build()));
	}

	public PropertiesPanel(final IModel<MysticCryptEntryModelBean> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		srcProperties = new javax.swing.JScrollPane();
		btnAdd = new javax.swing.JButton();
		btnRemove = new javax.swing.JButton();
		btnEdit = new javax.swing.JButton();

		tableModel = new StringKeyValueTableModel();
		List<KeyValuePair<String, String>> properties = getModelObject().getProperties() != null
			? getModelObject().getProperties()
			: new ArrayList<>();

		tableModel.addList(properties);
		tblProperties = new GenericJTable<>(getTableModel());
		// set only single selection
		tblProperties.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		srcProperties.setViewportView(tblProperties);


		btnRemoveStateMachine = GenericButtonGenericJXTableStateMachine
			.<KeyValuePair<String, String>> builder().button(btnRemove).component(tblProperties)
			.build();
		btnRemoveStateMachine.onInitialize();

		btnEditStateMachine = GenericButtonGenericJXTableStateMachine
			.<KeyValuePair<String, String>> builder().button(btnEdit).component(tblProperties)
			.build();
		btnEditStateMachine.onInitialize();

		ListSelectionModel selectionModel = tblProperties.getSelectionModel();
		selectionModel.addListSelectionListener(e -> {
			btnRemoveStateMachine.onTableSelection();
			btnEditStateMachine.onTableSelection();
		});

		btnAdd.setText("Add Property");
		btnAdd.addActionListener(this::onAdd);
		btnEdit.addActionListener(this::onEdit);
		tblProperties.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(final MouseEvent mouseEvent)
			{
				if (mouseEvent.getClickCount() == 2 && mouseEvent.getButton() == MouseEvent.BUTTON1)
				{
					onEditSelected();
				}
			}
		});
		btnRemove.addActionListener(this::onRemove);

		btnRemove.setText("Remove");

		btnEdit.setText("Edit");
	}

	protected void onAdd(final ActionEvent actionEvent)
	{
		PropertiesNewEntryPanel panel = new PropertiesNewEntryPanel();
		int option = askForProperty(panel,
			Messages.getString("dialog.new.crypt.properties.title", "New Property Value."));
		if (option == JOptionPane.OK_OPTION)
		{
			KeyValuePair<String, String> newProperty = panel.getModelObject();
			PropertiesPanel.this.getModelObject().getProperties().add(newProperty);
			tableModel.add(newProperty);
		}
	}

	protected void onRemove(final ActionEvent actionEvent)
	{
		getTblProperties().getSingleSelectedRowData().ifPresent(this::removeProperty);
	}

	/**
	 * Takes a property out of the table and out of the entry: the table holds a list of its own,
	 * and nothing reads it back into the entry (#808)
	 */
	private void removeProperty(final KeyValuePair<String, String> property)
	{
		getTblProperties().getGenericTableModel().remove(property);
		getModelObject().getProperties().remove(property);
	}

	protected void onEdit(final ActionEvent actionEvent)
	{
		onEditSelected();
	}

	/**
	 * Opens the selected property for editing. Pressing Edit and double clicking the row are the
	 * same thing, and a table row that answers a double click with nothing reads as broken.
	 */
	protected void onEditSelected()
	{
		getTblProperties().getSingleSelectedRowData().ifPresent(this::showEditPropertyDialog);
	}

	/**
	 * Lets the user change a property in a dialog that edits a copy of it, so that Cancel leaves
	 * the entry as it was: the fields write on every keystroke (#807). OK writes the copy into the
	 * row, and the row is the entry's own pair - the table copies the entry's list, not its
	 * elements
	 */
	private void showEditPropertyDialog(final KeyValuePair<String, String> tableEntry)
	{
		KeyValuePair<String, String> edited = new KeyValuePair<>(tableEntry.getKey(),
			tableEntry.getValue());
		PropertiesNewEntryPanel panel = new PropertiesNewEntryPanel(BaseModel.of(edited));
		int option = askForProperty(panel,
			Messages.getString("dialog.edit.crypt.properties.title", "Edit Property Value."));
		if (option == JOptionPane.OK_OPTION)
		{
			tableEntry.setKey(edited.getKey());
			tableEntry.setValue(edited.getValue());
			getTblProperties().getGenericTableModel().fireTableDataChanged();
		}
	}

	/**
	 * Shows the property dialog and waits for the answer: the one place the panel asks the user,
	 * for adding and for editing alike
	 *
	 * @param panel
	 *            the dialog's content
	 * @param title
	 *            the dialog's title
	 * @return {@link JOptionPane#OK_OPTION} or what else the dialog answered
	 */
	protected int askForProperty(final PropertiesNewEntryPanel panel, final String title)
	{
		return JOptionPaneExtensions.getSelectedOption(panel, JOptionPane.PLAIN_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION, null, title, panel.getTxtKey());
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
	}

	/**
	 * Initialize layout.
	 */
	protected void onInitializeMigLayout()
	{
		MigLayout layout = new MigLayout();
		this.setLayout(layout);
		add(srcProperties, "wrap");

	}

	protected void onInitializeGroupLayout()
	{
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap()
				.addComponent(srcProperties, javax.swing.GroupLayout.PREFERRED_SIZE,
					javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addComponent(btnAdd, javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(btnRemove, javax.swing.GroupLayout.Alignment.TRAILING,
						javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
					.addComponent(btnEdit, javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				.addContainerGap()));
		layout.setVerticalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
				.addContainerGap(42, Short.MAX_VALUE)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addComponent(srcProperties, javax.swing.GroupLayout.PREFERRED_SIZE, 172,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGroup(layout.createSequentialGroup().addComponent(btnAdd).addGap(18, 18, 18)
						.addComponent(btnRemove).addGap(18, 18, 18).addComponent(btnEdit)))
				.addContainerGap()));
	}

}
