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
package io.github.astrapi69.mystic.crypt.panel.dbtree;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.swing.base.BasePanel;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class AttachmentPanel extends BasePanel<MysticCryptEntryModelBean>
{
	private JButton btnAdd;
	private JButton btnRemove;
	private JButton btnSaveTo;
	private JScrollPane srcFiles;
	private JTable tblFiles;
	/**
	 * Creates new form NewAttachmentFormPanel
	 */
	public AttachmentPanel()
	{
		this(BaseModel.of(MysticCryptEntryModelBean.builder().build()));
	}

	public AttachmentPanel(final IModel<MysticCryptEntryModelBean> model)
	{
		super(model);
	}
	
	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
		srcFiles = new JScrollPane();
		tblFiles = new JTable();
		btnAdd = new JButton();
		btnRemove = new JButton();
		btnSaveTo = new JButton();

		tblFiles.setModel(new DefaultTableModel(
				new Object[][] { { null, null }, { null, null }, { null, null }, { null, null } },
				new String[] { "Title 1", "Title 2" }));
		srcFiles.setViewportView(tblFiles);

		btnAdd.setText("Add File");

		btnRemove.setText("Remove");

		btnSaveTo.setText("Save to");
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();

		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout
				.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
						.addComponent(srcFiles, GroupLayout.PREFERRED_SIZE,
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
								.addComponent(btnAdd, GroupLayout.DEFAULT_SIZE,
										GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(btnRemove, GroupLayout.Alignment.TRAILING,
										GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
								.addComponent(btnSaveTo, GroupLayout.DEFAULT_SIZE,
										GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
						.addContainerGap()));
		layout.setVerticalGroup(layout
				.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
						.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
								.addGroup(layout.createSequentialGroup().addComponent(btnAdd).addGap(18, 18, 18)
										.addComponent(btnRemove).addGap(18, 18, 18).addComponent(btnSaveTo))
								.addComponent(srcFiles, GroupLayout.PREFERRED_SIZE, 428,
										GroupLayout.PREFERRED_SIZE))
						.addContainerGap()));
	}
}
