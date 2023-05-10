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
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.astrapi69.mystic.crypt.panel.checksum;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;

import lombok.Getter;
import lombok.extern.java.Log;
import io.github.astrapi69.checksum.ChecksumExtensions;
import io.github.astrapi69.checksum.FileChecksumExtensions;
import io.github.astrapi69.crypt.api.algorithm.ChecksumAlgorithm;
import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.file.system.SystemFileExtensions;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.combobox.model.EnumComboBoxModel;
import io.github.astrapi69.swing.listener.document.EnableButtonBehavior;

@Getter
@Log
public class ChecksumPanel extends BasePanel<ChecksumBean>
{

	private JButton btnClearChecksumFile;
	private JButton btnClearOpenFile;
	private JButton btnCompare;
	private JButton btnOpenChecksumFile;
	private JButton btnOpenFile;
	private JLabel lblChecksumAlgorithm;
	private JLabel lblGeneratedChecksum;
	private JLabel lblOwnersChecksum;
	private JScrollPane srcGeneratedChecksum;
	private JScrollPane srcOwnersChecksum;
	private JTextField txtChecksumFile;
	private JTextField txtChecksumMatchResult;
	private JTextArea txtGeneratedChecksum;
	private JTextField txtOpenFile;
	private JTextArea txtOwnersChecksum;
	// manually changed
	private JComboBox<ChecksumAlgorithm> cbxChecksumAlgorithm;
	private JFileChooser fileChooser;

	/**
	 * Creates new {@link ChecksumPanel}
	 */
	public ChecksumPanel()
	{
		this(BaseModel.of(ChecksumBean.builder().build()));
	}

	/**
	 * Creates new form ChecksumPanel
	 */
	public ChecksumPanel(final IModel<ChecksumBean> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		txtOpenFile = new JTextField();
		btnOpenFile = new JButton();
		btnClearOpenFile = new JButton();
		lblGeneratedChecksum = new JLabel();
		srcGeneratedChecksum = new JScrollPane();
		txtGeneratedChecksum = new JTextArea();
		lblOwnersChecksum = new JLabel();
		txtChecksumFile = new JTextField();
		btnOpenChecksumFile = new JButton();
		btnClearChecksumFile = new JButton();
		srcOwnersChecksum = new JScrollPane();
		txtOwnersChecksum = new JTextArea();
		lblChecksumAlgorithm = new JLabel();
		cbxChecksumAlgorithm = new JComboBox<>();
		btnCompare = new JButton();
		txtChecksumMatchResult = new JTextField();

		// manually changed
		txtOpenFile.setEnabled(false);
		txtGeneratedChecksum.setEnabled(false);
		txtChecksumFile.setEnabled(false);

		btnOpenFile.addActionListener(this::onOpenFile);
		btnClearOpenFile.addActionListener(this::onClearOpenFile);
		EnableButtonBehavior.builder().buttonModel(btnClearOpenFile.getModel())
			.document(txtGeneratedChecksum.getDocument()).build();

		btnOpenFile.setText("Open File to check");
		btnClearOpenFile.setText("Clear");


		lblGeneratedChecksum.setText("Generated checksum");

		txtGeneratedChecksum.setColumns(20);
		txtGeneratedChecksum.setRows(3);
		srcGeneratedChecksum.setViewportView(txtGeneratedChecksum);

		lblOwnersChecksum.setText("Checksum from owner");

		txtOwnersChecksum.setColumns(20);
		txtOwnersChecksum.setRows(3);

		btnOpenChecksumFile.addActionListener(this::onOpenChecksumFile);
		btnClearChecksumFile.addActionListener(this::onClearChecksumFile);
		EnableButtonBehavior.builder().buttonModel(btnClearChecksumFile.getModel())
			.document(txtOwnersChecksum.getDocument()).build();

		btnOpenChecksumFile.setText("Open Checksum File");
		btnClearChecksumFile.setText("Clear");

		srcOwnersChecksum.setViewportView(txtOwnersChecksum);

		lblChecksumAlgorithm.setText("Checksum algorithm");

		cbxChecksumAlgorithm
			.setModel(new EnumComboBoxModel<>(ChecksumAlgorithm.class, ChecksumAlgorithm.MD5));
		cbxChecksumAlgorithm.addActionListener(this::onChangeChecksumAlgorithm);
		getModelObject().setSelectedAlgorithm(ChecksumAlgorithm.MD5);

		btnCompare.setText("Compare");
		btnCompare.addActionListener(this::onCompare);

		txtChecksumMatchResult.setText("Checksum Match Result");

		fileChooser = new JFileChooser(SystemFileExtensions.getUserDownloadsDir());
	}

	private void onOpenChecksumFile(ActionEvent actionEvent)
	{
		final int returnVal = fileChooser.showSaveDialog(ChecksumPanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File selectedChecksumFile = fileChooser.getSelectedFile();
			long length = selectedChecksumFile.length();
			if (length <= 128)
			{
				getModelObject().setSelectedChecksumFile(selectedChecksumFile);
				getModelObject().setSelectedChecksumFilename(selectedChecksumFile.getName());
				txtChecksumFile.setText(getModelObject().getSelectedChecksumFilename());
				try
				{
					String checksum = ReadFileExtensions.readFromFile(selectedChecksumFile).trim();
					System.out.println(checksum);
					txtOwnersChecksum.setText(checksum);
					txtOwnersChecksum.setEnabled(false);
					ChecksumAlgorithm checksumAlgorithmOfFile = ChecksumExtensions
						.resolveChecksumAlgorithm(checksum);
					cbxChecksumAlgorithm.setSelectedItem(checksumAlgorithmOfFile);
					this.revalidate();
				}
				catch (IOException e)
				{
					log.log(Level.SEVERE, e.getLocalizedMessage(), e);
				}
			}
			else
			{
				txtChecksumMatchResult.setText("Given checksum file is invalid");
				txtChecksumMatchResult.setBackground(new ColorUIResource(new Color(255, 0, 0)));
				txtChecksumMatchResult.revalidate();
			}

		}
	}

	protected void onClearChecksumFile(ActionEvent actionEvent)
	{
		getModelObject().setSelectedChecksumFile(null);
		getModelObject().setSelectedChecksumFilename("");
		txtChecksumFile.setText(getModelObject().getSelectedChecksumFilename());
		txtOwnersChecksum.setText("");
		txtOwnersChecksum.setEnabled(true);
	}

	protected void onCompare(final ActionEvent actionEvent)
	{
		String ownersChecksumText = txtOwnersChecksum.getText();
		String generatedChecksumText = txtGeneratedChecksum.getText();
		if (ownersChecksumText.equals(generatedChecksumText))
		{
			txtChecksumMatchResult.setText("Match");
			txtChecksumMatchResult.setBackground(new ColorUIResource(new Color(0, 255, 0)));
			txtChecksumMatchResult.revalidate();
		}
		else
		{
			txtChecksumMatchResult.setText("No Match");
			txtChecksumMatchResult.setBackground(new ColorUIResource(new Color(255, 0, 0)));
			txtChecksumMatchResult.revalidate();

		}
	}

	@SuppressWarnings("unchecked")
	protected void onChangeChecksumAlgorithm(final ActionEvent actionEvent)
	{
		final JComboBox<ChecksumAlgorithm> cb = ((JComboBox<ChecksumAlgorithm>)actionEvent
			.getSource());
		final ChecksumAlgorithm selectedAlgorithm = (ChecksumAlgorithm)cb.getSelectedItem();
		getModelObject().setSelectedAlgorithm(selectedAlgorithm);
		calculateChecksum();
	}

	protected void onClearOpenFile(ActionEvent actionEvent)
	{
		getModelObject().setSelectedFile(null);
		getModelObject().setSelectedFilename("");
		txtOpenFile.setText(getModelObject().getSelectedFilename());
		txtGeneratedChecksum.setText("");
	}

	protected void onOpenFile(final ActionEvent actionEvent)
	{
		final int returnVal = fileChooser.showSaveDialog(ChecksumPanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File selectedFile = fileChooser.getSelectedFile();
			getModelObject().setSelectedFile(selectedFile);
			getModelObject().setSelectedFilename(selectedFile.getName());
			txtOpenFile.setText(getModelObject().getSelectedFilename());
			calculateChecksum();
		}
	}

	private void calculateChecksum()
	{
		if (getModelObject().getSelectedFile() != null
			&& getModelObject().getSelectedFile().exists())
		{
			ChecksumAlgorithm selectedAlgorithm = getModelObject().getSelectedAlgorithm();
			try
			{
				String checksum = FileChecksumExtensions
					.getChecksum(getModelObject().getSelectedFile(), selectedAlgorithm);
				System.out.println(checksum);
				txtGeneratedChecksum.setText(checksum);
			}
			catch (NoSuchAlgorithmException | IOException e)
			{
				log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}

	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
	}

	protected void onInitializeGroupLayout()
	{
		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addGap(30, 30, 30)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addComponent(lblGeneratedChecksum, GroupLayout.DEFAULT_SIZE,
								GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(srcGeneratedChecksum, GroupLayout.DEFAULT_SIZE, 1142,
								Short.MAX_VALUE)
							.addComponent(lblOwnersChecksum, GroupLayout.DEFAULT_SIZE,
								GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addGroup(layout.createSequentialGroup()
								.addComponent(txtOpenFile, GroupLayout.PREFERRED_SIZE, 780,
									GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
									GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(btnOpenFile, GroupLayout.PREFERRED_SIZE, 240,
									GroupLayout.PREFERRED_SIZE)
								.addGap(18, 18, 18).addComponent(btnClearOpenFile)))
						.addGap(30, 30, 30))
					.addGroup(layout.createSequentialGroup()
						.addComponent(lblChecksumAlgorithm, GroupLayout.PREFERRED_SIZE, 199,
							GroupLayout.PREFERRED_SIZE)
						.addGap(0, 0, Short.MAX_VALUE))
					.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
							.addGroup(layout.createSequentialGroup()
								.addComponent(cbxChecksumAlgorithm, GroupLayout.PREFERRED_SIZE, 240,
									GroupLayout.PREFERRED_SIZE)
								.addGap(31, 31, 31)
								.addComponent(btnCompare, GroupLayout.PREFERRED_SIZE, 180,
									GroupLayout.PREFERRED_SIZE)
								.addGap(57, 57, 57).addComponent(txtChecksumMatchResult))
							.addComponent(srcOwnersChecksum)
							.addGroup(layout.createSequentialGroup()
								.addComponent(txtChecksumFile, GroupLayout.PREFERRED_SIZE, 780,
									GroupLayout.PREFERRED_SIZE)
								.addGap(57, 57, 57)
								.addComponent(btnOpenChecksumFile, GroupLayout.PREFERRED_SIZE, 240,
									GroupLayout.PREFERRED_SIZE)
								.addGap(18, 18, 18).addComponent(btnClearChecksumFile)))
						.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addGap(30, 30, 30)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(btnOpenFile)
					.addComponent(txtOpenFile, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE)
					.addComponent(btnClearOpenFile))
				.addGap(18, 18, 18).addComponent(lblGeneratedChecksum).addGap(26, 26, 26)
				.addComponent(srcGeneratedChecksum, GroupLayout.PREFERRED_SIZE, 58,
					GroupLayout.PREFERRED_SIZE)
				.addGap(18, 18, 18).addComponent(lblOwnersChecksum).addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(btnOpenChecksumFile).addComponent(btnClearChecksumFile))
					.addComponent(txtChecksumFile, GroupLayout.PREFERRED_SIZE,
						GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addGap(18, 18, 18)
				.addComponent(srcOwnersChecksum, GroupLayout.PREFERRED_SIZE, 60,
					GroupLayout.PREFERRED_SIZE)
				.addGap(18, 18, 18).addComponent(lblChecksumAlgorithm).addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(cbxChecksumAlgorithm, GroupLayout.PREFERRED_SIZE,
						GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addComponent(btnCompare).addComponent(txtChecksumMatchResult,
						GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE))
				.addContainerGap(40, Short.MAX_VALUE)));
	}

}
