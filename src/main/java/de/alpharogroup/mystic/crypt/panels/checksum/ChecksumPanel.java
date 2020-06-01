/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.alpharogroup.mystic.crypt.panels.checksum;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;

import de.alpharogroup.checksum.FileChecksumExtensions;
import de.alpharogroup.file.system.SystemFileExtensions;
import de.alpharogroup.model.BaseModel;
import de.alpharogroup.model.api.Model;
import de.alpharogroup.swing.base.BasePanel;
import de.alpharogroup.swing.combobox.model.EnumComboBoxModel;
import lombok.Getter;

@Getter
public class ChecksumPanel extends BasePanel<ChecksumBean>
{

	private javax.swing.JButton btnCompare;
	private javax.swing.JButton btnOpenFile;
	private javax.swing.JComboBox<ChecksumAlgorithm> cbxChecksumAlgorithm;
	private javax.swing.JLabel lblChecksumAlgorithm;
	private javax.swing.JLabel lblGeneratedChecksum;
	private javax.swing.JLabel lblOwnersChecksum;
	private javax.swing.JScrollPane srcGeneratedChecksum;
	private javax.swing.JScrollPane srcOwnersChecksum;
	private javax.swing.JTextField txtChecksumMatchResult;
	private javax.swing.JTextArea txtGeneratedChecksum;
	private javax.swing.JTextField txtOpenFile;
	private javax.swing.JTextArea txtOwnersChecksum;
	private JFileChooser fileChooser;

	/**
	 * Creates new {@link ChecksumPanel}
	 */
	public ChecksumPanel()
	{
		this(BaseModel.<ChecksumBean> of(ChecksumBean.builder().build()));
	}

	/**
	 * Creates new form ChecksumPanel
	 */
	public ChecksumPanel(final Model<ChecksumBean> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		txtOpenFile = new javax.swing.JTextField();
		btnOpenFile = new javax.swing.JButton();
		lblGeneratedChecksum = new javax.swing.JLabel();
		srcGeneratedChecksum = new javax.swing.JScrollPane();
		txtGeneratedChecksum = new javax.swing.JTextArea();
		lblOwnersChecksum = new javax.swing.JLabel();
		srcOwnersChecksum = new javax.swing.JScrollPane();
		txtOwnersChecksum = new javax.swing.JTextArea();
		lblChecksumAlgorithm = new javax.swing.JLabel();
		cbxChecksumAlgorithm = new javax.swing.JComboBox<>();
		btnCompare = new javax.swing.JButton();
		txtChecksumMatchResult = new javax.swing.JTextField();

		txtOpenFile.setEnabled(false);
		txtGeneratedChecksum.setEnabled(false);

		btnOpenFile.addActionListener(this::onOpenFile);

		btnOpenFile.setText("Open File");

		lblGeneratedChecksum.setText("Generated checksum");

		txtGeneratedChecksum.setColumns(20);
		txtGeneratedChecksum.setRows(5);
		srcGeneratedChecksum.setViewportView(txtGeneratedChecksum);

		lblOwnersChecksum.setText("Checksum from owner");

		txtOwnersChecksum.setColumns(20);
		txtOwnersChecksum.setRows(5);
		srcOwnersChecksum.setViewportView(txtOwnersChecksum);

		lblChecksumAlgorithm.setText("Checksum algorithm");

		cbxChecksumAlgorithm.setModel(new EnumComboBoxModel<>(ChecksumAlgorithm.class));
		cbxChecksumAlgorithm.setSelectedItem(ChecksumAlgorithm.MD5);
		cbxChecksumAlgorithm.addActionListener(this::onChangeChecksumAlgorithm);
		getModelObject().setSelectedAlgorithm(ChecksumAlgorithm.MD5);

		btnCompare.setText("Compare");
		btnCompare.addActionListener(this::onCompare);

		txtChecksumMatchResult.setText("Checksum Match Result");

		fileChooser = new JFileChooser(SystemFileExtensions.getUserHomeDir() + "/Downloads");
	}

	protected void onCompare(final ActionEvent actionEvent)
	{
		String ownersChecksumText = txtOwnersChecksum.getText();
		String generatedChecksumText = txtGeneratedChecksum.getText();
		if(ownersChecksumText.equals(generatedChecksumText)){
			txtChecksumMatchResult.setBackground(Color.GREEN);
			txtChecksumMatchResult.revalidate();
			txtChecksumMatchResult.setText("Match");
		} else {
			txtChecksumMatchResult.setBackground(Color.RED);
			txtChecksumMatchResult.revalidate();
			txtChecksumMatchResult.setText("No Match");

		}
	}

	@SuppressWarnings("unchecked")
	protected void onChangeChecksumAlgorithm(final ActionEvent actionEvent)
	{
		final JComboBox<ChecksumAlgorithm> cb = ((JComboBox<ChecksumAlgorithm>)actionEvent.getSource());
		final ChecksumAlgorithm selectedAlgorithm = (ChecksumAlgorithm)cb.getSelectedItem();
		getModelObject().setSelectedAlgorithm(selectedAlgorithm);
		calculateChecksum();
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
		if(getModelObject().getSelectedFile()!=null && getModelObject().getSelectedFile().exists()) {
			ChecksumAlgorithm selectedAlgorithm = getModelObject().getSelectedAlgorithm();
			try
			{
				String checksum = FileChecksumExtensions.getChecksum(getModelObject().getSelectedFile(), selectedAlgorithm);
				System.out.println(checksum);
				txtGeneratedChecksum.setText(checksum);
			}
			catch (NoSuchAlgorithmException | IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
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
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addGap(30, 30, 30).addGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(lblGeneratedChecksum, javax.swing.GroupLayout.DEFAULT_SIZE,
					javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addComponent(srcGeneratedChecksum, javax.swing.GroupLayout.DEFAULT_SIZE, 1142,
					Short.MAX_VALUE)
				.addComponent(srcOwnersChecksum)
				.addGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(lblChecksumAlgorithm, javax.swing.GroupLayout.PREFERRED_SIZE,
							199, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(cbxChecksumAlgorithm, javax.swing.GroupLayout.PREFERRED_SIZE,
							240, javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(39, 39, 39)
					.addComponent(btnCompare, javax.swing.GroupLayout.PREFERRED_SIZE, 180,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGap(45, 45, 45).addComponent(txtChecksumMatchResult))
				.addComponent(lblOwnersChecksum, javax.swing.GroupLayout.DEFAULT_SIZE,
					javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addGroup(layout.createSequentialGroup()
					.addComponent(txtOpenFile, javax.swing.GroupLayout.PREFERRED_SIZE, 850,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
						javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(btnOpenFile, javax.swing.GroupLayout.PREFERRED_SIZE, 240,
						javax.swing.GroupLayout.PREFERRED_SIZE)))
				.addGap(30, 30, 30)));
		layout
			.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addGap(30, 30, 30)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(btnOpenFile).addComponent(txtOpenFile,
							javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(33, 33, 33).addComponent(lblGeneratedChecksum).addGap(18, 18, 18)
					.addComponent(srcGeneratedChecksum, javax.swing.GroupLayout.PREFERRED_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18).addComponent(lblOwnersChecksum)
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(srcOwnersChecksum, javax.swing.GroupLayout.PREFERRED_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18).addComponent(lblChecksumAlgorithm).addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(cbxChecksumAlgorithm, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(btnCompare).addComponent(txtChecksumMatchResult,
							javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addContainerGap(26, Short.MAX_VALUE)));
	}

}
