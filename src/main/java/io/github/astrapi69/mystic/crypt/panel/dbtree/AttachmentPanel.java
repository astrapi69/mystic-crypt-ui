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

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Optional;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.LayoutStyle;
import javax.swing.ListSelectionModel;

import io.github.astrapi69.mystic.crypt.panel.dbtree.button.state.ButtonGenericJXTableFileContentInfoStateMachine;
import lombok.Getter;
import io.github.astrapi69.file.create.FileContentInfo;
import io.github.astrapi69.file.write.WriteFileExtensions;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.table.GenericJXTable;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;

@Getter
public class AttachmentPanel extends BasePanel<MysticCryptEntryModelBean>
{
	private JButton btnAdd;
	private JButton btnRemove;
	private JButton btnSaveTo;
	private JScrollPane srcFiles;
	private GenericJXTable<FileContentInfo> tblFiles;

	private JFileChooser fileChooser;

	ButtonGenericJXTableFileContentInfoStateMachine btnRemoveStateMachine;

	ButtonGenericJXTableFileContentInfoStateMachine btnSaveToStateMachine;

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

	protected GenericJXTable<FileContentInfo> newJTable()
	{
		AttachmentTableModel tableModel = new AttachmentTableModel();
		return new GenericJXTable<>(tableModel);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
		fileChooser = new JFileChooser();

		srcFiles = new JScrollPane();
		AttachmentTableModel attachmentTableModel = new AttachmentTableModel();
		if (getModelObject().getResources() == null)
		{
			getModelObject().setResources(new ArrayList<>());
		}
		attachmentTableModel.setData(getModelObject().getResources());
		tblFiles = new GenericJXTable<>(attachmentTableModel);
		// set only single selection
		tblFiles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		btnAdd = new JButton();
		btnRemove = new JButton();
		btnSaveTo = new JButton();

		btnRemoveStateMachine = ButtonGenericJXTableFileContentInfoStateMachine.builder()
				.button(btnRemove)
			.component(tblFiles).build();
		btnRemoveStateMachine.onInitialize();

		btnSaveToStateMachine = ButtonGenericJXTableFileContentInfoStateMachine.builder().button(btnSaveTo)
			.component(tblFiles).build();
		btnSaveToStateMachine.onInitialize();

		ListSelectionModel selectionModel = tblFiles.getSelectionModel();
		selectionModel.addListSelectionListener(e -> {
			btnRemoveStateMachine.onTableSelection();
			btnSaveToStateMachine.onTableSelection();
		});


		srcFiles.setViewportView(tblFiles);

		btnAdd.setText("Add File");

		btnRemove.setText("Remove");

		btnSaveTo.setText("Save to");


		btnAdd.addActionListener(this::onAdd);
		btnRemove.addActionListener(this::onRemove);
		btnSaveTo.addActionListener(this::onSaveTo);
	}

	protected void onAdd(final ActionEvent actionEvent)
	{
		System.err.println("onAdd");
		final int returnVal = fileChooser.showSaveDialog(AttachmentPanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File newAttachment = fileChooser.getSelectedFile();
			FileContentInfo fileContentInfo = FileContentInfo.toFileContentInfo(newAttachment);
			getTblFiles().getGenericTableModel().add(fileContentInfo);
		}
	}

	protected void onRemove(final ActionEvent actionEvent)
	{
		System.err.println("onRemove");
		if (0 < tblFiles.getSelectedRows().length)
		{
			// confirm delete
			tblFiles.getGenericTableModel().removeAt(tblFiles.getSelectedRow());
		}
	}

	protected void onSaveTo(final ActionEvent actionEvent)
	{
		System.err.println("onSaveTo");
		Optional<FileContentInfo> singleSelectedRowData = tblFiles.getSingleSelectedRowData();
		if (singleSelectedRowData.isPresent())
		{
			FileContentInfo fileContentInfo = singleSelectedRowData.get();
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Specify a file to save");
			int userSelection = fileChooser.showSaveDialog(this);

			if (userSelection == JFileChooser.APPROVE_OPTION)
			{
				File fileToSave = fileChooser.getSelectedFile();
				RuntimeExceptionDecorator.decorate(() -> WriteFileExtensions
					.writeByteArrayToFile(fileToSave, fileContentInfo.getContent()));
			}
		}
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();

		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap()
				.addComponent(srcFiles, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(btnAdd, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
						Short.MAX_VALUE)
					.addComponent(btnRemove, GroupLayout.Alignment.TRAILING,
						GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
					.addComponent(btnSaveTo, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
						Short.MAX_VALUE))
				.addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
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
