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
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package io.github.astrapi69.mystic.crypt.panel.properties;

import java.awt.event.ActionEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import lombok.Getter;
import net.miginfocom.swing.MigLayout;
import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.button.state.GenericButtonGenericJXTableStateMachine;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.dialog.JOptionPaneExtensions;
import io.github.astrapi69.swing.table.GenericJXTable;
import io.github.astrapi69.swing.table.model.properties.StringKeyValueTableModel;

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
	private GenericJXTable<KeyValuePair<String, String>> tblProperties;
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
		tblProperties = new GenericJXTable<>(getTableModel());
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
		btnRemove.addActionListener(this::onRemove);

		btnRemove.setText("Remove");

		btnEdit.setText("Edit");
	}

	protected void onAdd(final ActionEvent actionEvent)
	{
		PropertiesNewEntryPanel panel = new PropertiesNewEntryPanel();
		int option = JOptionPaneExtensions.getSelectedOption(panel, JOptionPane.PLAIN_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION, null,
			Messages.getString("dialog.new.crypt.properties.title", "New Property Value."),
			panel.getTxtKey());
		if (option == JOptionPane.OK_OPTION)
		{
			KeyValuePair<String, String> newProperty = panel.getModelObject();
			PropertiesPanel.this.getModelObject().getProperties().add(newProperty);
			tableModel.add(newProperty);
		}
	}

	protected void onRemove(final ActionEvent actionEvent)
	{
		// Get selected property and remove it from table
		System.err.println("onRemove");
		if (0 < tblProperties.getSelectedRows().length)
		{
			// confirm delete
			tblProperties.getGenericTableModel().removeAt(tblProperties.getSelectedRow());
		}
	}

	protected void onEdit(final ActionEvent actionEvent)
	{
		System.err.println("onEdit");
		getTblProperties().getSingleSelectedRowData().ifPresent(tableEntry -> {
			showEditPropertyDialog(tableEntry);
		});
	}

	private void showEditPropertyDialog(KeyValuePair<String, String> tableEntry)
	{
		PropertiesNewEntryPanel panel = new PropertiesNewEntryPanel(BaseModel.of(tableEntry));
		int option = JOptionPaneExtensions.getSelectedOption(panel, JOptionPane.PLAIN_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION, null,
			Messages.getString("dialog.edit.crypt.properties.title", "Edit Property Value."),
			panel.getTxtKey());
		if (option == JOptionPane.OK_OPTION)
		{
			List<KeyValuePair<String, String>> data = getTblProperties().getGenericTableModel()
				.getData();
			int index = data.indexOf(tableEntry);
			data.remove(tableEntry);
			KeyValuePair<String, String> newProperty = panel.getModelObject();
			data.add(index, newProperty);
			getTblProperties().getGenericTableModel().fireTableDataChanged();
		}
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
