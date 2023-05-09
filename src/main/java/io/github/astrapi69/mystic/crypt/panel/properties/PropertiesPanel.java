/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package io.github.astrapi69.mystic.crypt.panel.properties;

import java.awt.event.ActionEvent;
import java.io.Serial;

import lombok.Getter;
import net.miginfocom.swing.MigLayout;
import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.table.GenericJXTable;
import io.github.astrapi69.swing.table.model.properties.StringKeyValueTableModel;

@Getter
public class PropertiesPanel extends BasePanel<MysticCryptEntryModelBean>
{
	@Serial
	private static final long serialVersionUID = 1L;

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
		tableModel.addList(getModelObject().getProperties());
		tblProperties = new GenericJXTable<>(getTableModel());

		srcProperties.setViewportView(tblProperties);

		btnAdd.setText("Add Property");
		btnAdd.addActionListener(this::onAdd);
		btnEdit.addActionListener(this::onEdit);
		btnRemove.addActionListener(this::onRemove);

		btnRemove.setText("Remove");

		btnEdit.setText("Edit");
	}

	protected void onAdd(final ActionEvent actionEvent)
	{
		// new dialog appears and user can give new property
		// JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
		// JOptionPane.OK_CANCEL_OPTION);
	}

	protected void onRemove(final ActionEvent actionEvent)
	{

	}

	protected void onEdit(final ActionEvent actionEvent)
	{

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
