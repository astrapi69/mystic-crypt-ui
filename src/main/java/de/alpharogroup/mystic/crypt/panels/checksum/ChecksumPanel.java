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
import java.util.Properties;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;

import de.alpharogroup.behaviors.EnableButtonBehavior;
import de.alpharogroup.checksum.FileChecksumExtensions;
import de.alpharogroup.file.read.ReadFileExtensions;
import de.alpharogroup.file.system.SystemFileExtensions;
import de.alpharogroup.model.BaseModel;
import de.alpharogroup.model.api.Model;
import de.alpharogroup.swing.base.BasePanel;
import de.alpharogroup.swing.combobox.model.EnumComboBoxModel;
import lombok.Getter;

@Getter
public class ChecksumPanel extends BasePanel<ChecksumBean>
{

	private javax.swing.JButton btnClearChecksumFile;
	private javax.swing.JButton btnClearOpenFile;
	private javax.swing.JButton btnCompare;
	private javax.swing.JButton btnOpenChecksumFile;
	private javax.swing.JButton btnOpenFile;
	private javax.swing.JLabel lblChecksumAlgorithm;
	private javax.swing.JLabel lblGeneratedChecksum;
	private javax.swing.JLabel lblOwnersChecksum;
	private javax.swing.JScrollPane srcGeneratedChecksum;
	private javax.swing.JScrollPane srcOwnersChecksum;
	private javax.swing.JTextField txtChecksumFile;
	private javax.swing.JTextField txtChecksumMatchResult;
	private javax.swing.JTextArea txtGeneratedChecksum;
	private javax.swing.JTextField txtOpenFile;
	private javax.swing.JTextArea txtOwnersChecksum;
	// manually changed
	private javax.swing.JComboBox<ChecksumAlgorithm> cbxChecksumAlgorithm;
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
		btnClearOpenFile = new javax.swing.JButton();
		lblGeneratedChecksum = new javax.swing.JLabel();
		srcGeneratedChecksum = new javax.swing.JScrollPane();
		txtGeneratedChecksum = new javax.swing.JTextArea();
		lblOwnersChecksum = new javax.swing.JLabel();
		txtChecksumFile = new javax.swing.JTextField();
		btnOpenChecksumFile = new javax.swing.JButton();
		btnClearChecksumFile = new javax.swing.JButton();
		srcOwnersChecksum = new javax.swing.JScrollPane();
		txtOwnersChecksum = new javax.swing.JTextArea();
		lblChecksumAlgorithm = new javax.swing.JLabel();
		cbxChecksumAlgorithm = new javax.swing.JComboBox<>();
		btnCompare = new javax.swing.JButton();
		txtChecksumMatchResult = new javax.swing.JTextField();

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

		cbxChecksumAlgorithm.setModel(new EnumComboBoxModel<>(ChecksumAlgorithm.class));
		cbxChecksumAlgorithm.setSelectedItem(ChecksumAlgorithm.MD5);
		cbxChecksumAlgorithm.addActionListener(this::onChangeChecksumAlgorithm);
		getModelObject().setSelectedAlgorithm(ChecksumAlgorithm.MD5);

		btnCompare.setText("Compare");
		btnCompare.addActionListener(this::onCompare);

		txtChecksumMatchResult.setText("Checksum Match Result");

		fileChooser = new JFileChooser(SystemFileExtensions.getUserHomeDir() + "/Downloads");
	}

	private void onOpenChecksumFile(ActionEvent actionEvent)
	{
		final int returnVal = fileChooser.showSaveDialog(ChecksumPanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File selectedChecksumFile = fileChooser.getSelectedFile();
			long length = selectedChecksumFile.length();
			if(length <= 128) {
				getModelObject().setSelectedChecksumFile(selectedChecksumFile);
				getModelObject().setSelectedChecksumFilename(selectedChecksumFile.getName());
				txtChecksumFile.setText(getModelObject().getSelectedChecksumFilename());
				try
				{
					String checksum = ReadFileExtensions.readFromFile(selectedChecksumFile).trim();
					System.out.println(checksum);
					txtOwnersChecksum.setText(checksum);
					txtOwnersChecksum.setEnabled(false);
					// TODO set checksum algorithm
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			} else {
				txtChecksumMatchResult.setText("Given checksum file is invalid");
				txtChecksumMatchResult.setBackground(new ColorUIResource(new Color(255, 0, 0)));
				txtChecksumMatchResult.revalidate();
			}

		}
	}

	private void onClearChecksumFile(ActionEvent actionEvent)
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
		if(ownersChecksumText.equals(generatedChecksumText)){
			txtChecksumMatchResult.setText("Match");
			txtChecksumMatchResult.setBackground(new ColorUIResource(new Color(0, 255, 0)));
			txtChecksumMatchResult.revalidate();
		} else {
			txtChecksumMatchResult.setText("No Match");
			txtChecksumMatchResult.setBackground(new ColorUIResource(new Color(255, 0, 0)));
			txtChecksumMatchResult.revalidate();

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

	protected void onClearOpenFile(ActionEvent actionEvent) {
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
		layout.setHorizontalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(layout.createSequentialGroup()
								.addGap(30, 30, 30)
								.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
										.addGroup(layout.createSequentialGroup()
												.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
														.addComponent(lblGeneratedChecksum, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
														.addComponent(srcGeneratedChecksum, javax.swing.GroupLayout.DEFAULT_SIZE, 1142, Short.MAX_VALUE)
														.addComponent(lblOwnersChecksum, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
														.addGroup(layout.createSequentialGroup()
																.addComponent(txtOpenFile, javax.swing.GroupLayout.PREFERRED_SIZE, 780, javax.swing.GroupLayout.PREFERRED_SIZE)
																.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																.addComponent(btnOpenFile, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
																.addGap(18, 18, 18)
																.addComponent(btnClearOpenFile)))
												.addGap(30, 30, 30))
										.addGroup(layout.createSequentialGroup()
												.addComponent(lblChecksumAlgorithm, javax.swing.GroupLayout.PREFERRED_SIZE, 199, javax.swing.GroupLayout.PREFERRED_SIZE)
												.addGap(0, 0, Short.MAX_VALUE))
										.addGroup(layout.createSequentialGroup()
												.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
														.addGroup(layout.createSequentialGroup()
																.addComponent(cbxChecksumAlgorithm, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
																.addGap(31, 31, 31)
																.addComponent(btnCompare, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
																.addGap(57, 57, 57)
																.addComponent(txtChecksumMatchResult))
														.addComponent(srcOwnersChecksum)
														.addGroup(layout.createSequentialGroup()
																.addComponent(txtChecksumFile, javax.swing.GroupLayout.PREFERRED_SIZE, 780, javax.swing.GroupLayout.PREFERRED_SIZE)
																.addGap(57, 57, 57)
																.addComponent(btnOpenChecksumFile, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
																.addGap(18, 18, 18)
																.addComponent(btnClearChecksumFile)))
												.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
		);
		layout.setVerticalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(layout.createSequentialGroup()
								.addGap(30, 30, 30)
								.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(btnOpenFile)
										.addComponent(txtOpenFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
										.addComponent(btnClearOpenFile))
								.addGap(18, 18, 18)
								.addComponent(lblGeneratedChecksum)
								.addGap(26, 26, 26)
								.addComponent(srcGeneratedChecksum, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addGap(18, 18, 18)
								.addComponent(lblOwnersChecksum)
								.addGap(18, 18, 18)
								.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
										.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(btnOpenChecksumFile)
												.addComponent(btnClearChecksumFile))
										.addComponent(txtChecksumFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
								.addGap(18, 18, 18)
								.addComponent(srcOwnersChecksum, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addGap(18, 18, 18)
								.addComponent(lblChecksumAlgorithm)
								.addGap(18, 18, 18)
								.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(cbxChecksumAlgorithm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
										.addComponent(btnCompare)
										.addComponent(txtChecksumMatchResult, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
								.addContainerGap(40, Short.MAX_VALUE))
		);
	}

}
