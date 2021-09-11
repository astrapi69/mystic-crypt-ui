/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */
package io.github.astrapi69.mystic.crypt.panels.privatekey;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.logging.Level;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

import io.github.astrapi69.swing.JMTextField;
import io.github.astrapi69.swing.dialog.DialogExtensions;
import io.github.astrapi69.swing.utils.AwtExtensions;
import lombok.Getter;
import lombok.extern.java.Log;
import io.github.astrapi69.create.FileFactory;
import io.github.astrapi69.crypto.algorithm.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypto.factories.KeyPairFactory;
import io.github.astrapi69.crypto.key.KeySize;
import io.github.astrapi69.crypto.key.PrivateKeyExtensions;
import io.github.astrapi69.crypto.key.writer.PrivateKeyWriter;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.Model;
import io.github.astrapi69.swing.adapters.DocumentListenerAdapter;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.combobox.model.EnumComboBoxModel;
import io.github.astrapi69.system.SystemFileExtensions;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;

@Log
@Getter
public class NewPrivateKeyPanel extends BasePanel<NewPrivateKeyModelBean>
{

	private javax.swing.JButton btnCancel;
	private javax.swing.JButton btnClear;
	private javax.swing.JButton btnDirectoryOfPrivateKey;
	private javax.swing.JButton btnGenerate;
	private javax.swing.JButton btnSave;
	private javax.swing.JComboBox<KeySize> cmbKeySize;
	private javax.swing.JLabel lblDirectoryOfPrivateKey;
	private javax.swing.JLabel lblFilenameOfPrivateKey;
	private javax.swing.JLabel lblKeySize;
	private javax.swing.JLabel lblPrivateKey;
	private javax.swing.JScrollPane scpPrivateKey;
	private javax.swing.JTextField txtDirectoryOfPrivateKey;
	private javax.swing.JTextField txtFilenameOfPrivateKey;
	private javax.swing.JTextArea txtPrivateKey;
	// ===
	// ===
	// ===
	private JFileChooser fileChooser;
	BtnSaveStateMachine btnSaveStateMachine;

	public NewPrivateKeyPanel()
	{
		this(BaseModel.<NewPrivateKeyModelBean> of(NewPrivateKeyModelBean.builder().build()));
	}

	public NewPrivateKeyPanel(final Model<NewPrivateKeyModelBean> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{

		scpPrivateKey = new javax.swing.JScrollPane();
		txtPrivateKey = new javax.swing.JTextArea();
		cmbKeySize = new javax.swing.JComboBox<>();
		btnGenerate = new javax.swing.JButton();
		lblPrivateKey = new javax.swing.JLabel();
		lblKeySize = new javax.swing.JLabel();
		btnClear = new javax.swing.JButton();
		btnCancel = new javax.swing.JButton();
		btnSave = new javax.swing.JButton();
		lblFilenameOfPrivateKey = new javax.swing.JLabel();
		txtFilenameOfPrivateKey = new javax.swing.JTextField();
		lblDirectoryOfPrivateKey = new javax.swing.JLabel();
		txtDirectoryOfPrivateKey = new javax.swing.JTextField();
		btnDirectoryOfPrivateKey = new javax.swing.JButton();

		txtPrivateKey.setColumns(20);
		txtPrivateKey.setRows(5);
		scpPrivateKey.setViewportView(txtPrivateKey);
		txtPrivateKey.getAccessibleContext().setAccessibleDescription("");

		btnGenerate.setText("Generate key");

		lblPrivateKey.setText("Private key");

		lblKeySize.setText("Keysize");

		btnClear.setText("Clear key");

		btnCancel.setText("Cancel");

		btnSave.setText("Save private key");

		lblFilenameOfPrivateKey.setText("File name for private key");

		lblDirectoryOfPrivateKey.setText("Directory to save the private key");

		btnDirectoryOfPrivateKey.setText("Browse...");
		// ===
		// ===
		// ===
		NewPrivateKeyModelBean modelObject = getModelObject();
		btnSaveStateMachine = BtnSaveStateMachine.builder().component(btnSave)
			.modelObject(modelObject).build();

		txtFilenameOfPrivateKey = new JMTextField();
		((JMTextField)txtFilenameOfPrivateKey).setPropertyModel(LambdaModel.of(
			modelObject::getFilenameOfPrivateKey, modelObject::setFilenameOfPrivateKey));
		txtFilenameOfPrivateKey.getDocument().addDocumentListener(new DocumentListenerAdapter()
		{
			@Override
			public void onDocumentChanged(DocumentEvent documentEvent)
			{
				String text = txtFilenameOfPrivateKey.getText();
				modelObject.setFilenameOfPrivateKey(text);
				btnSaveStateMachine.onChangeFilename();
			}
		});
		cmbKeySize.setModel(new EnumComboBoxModel<>(KeySize.class));
		if (modelObject.getKeySize() == null)
		{
			modelObject.setKeySize(KeySize.KEYSIZE_2048);
		}
		txtDirectoryOfPrivateKey.setEnabled(false);
		cmbKeySize.setSelectedItem(modelObject.getKeySize());

		cmbKeySize.addActionListener(actionEvent -> onChangeKeySize(actionEvent));

		btnGenerate.addActionListener(actionEvent -> onGenerate(actionEvent));
		btnClear.addActionListener(actionEvent -> onClear(actionEvent));

		btnSave.addActionListener(actionEvent -> onSave(actionEvent));
		btnSave.setEnabled(false);

		btnCancel.addActionListener(this::onCancel);
		btnDirectoryOfPrivateKey.addActionListener(this::onSelectedDirectoryOfPrivateKey);

		fileChooser = new JFileChooser(SystemFileExtensions.getUserDownloadsDir());
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		txtDirectoryOfPrivateKey.setText(SystemFileExtensions.getUserDownloadsDir().getAbsolutePath());
	}

	protected void onSelectedDirectoryOfPrivateKey(ActionEvent actionEvent)
	{
		final int returnVal = fileChooser.showSaveDialog(NewPrivateKeyPanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File privateKeyDirectory = fileChooser.getSelectedFile();
			getModelObject().setPrivateKeyDirectory(privateKeyDirectory);
			getTxtDirectoryOfPrivateKey().setText(privateKeyDirectory.getAbsolutePath());
			btnSaveStateMachine.onChangeDirectory();
		}
	}

	protected void onCancel(ActionEvent actionEvent)
	{
		System.err.println("onCancel method action called");
	}

	protected void onSave(ActionEvent actionEvent)
	{
		String filenameOfPrivateKey = getTxtFilenameOfPrivateKey().getText();
		File privateKeyDirectory = getModelObject().getPrivateKeyDirectory();
		File privateKeyFile = new File(privateKeyDirectory, filenameOfPrivateKey);
		if (privateKeyFile.exists())
		{
			String title = "File already exists";
			String message = "The file already exists. It will be overwritten if you confirm!";
			int option = JOptionPane.showConfirmDialog(this,
				message,
				title,
				JOptionPane.OK_CANCEL_OPTION);
			if (option == JOptionPane.CANCEL_OPTION)
			{
				return;
			}
		}
		RuntimeExceptionDecorator.decorate(() -> FileFactory.newFile(privateKeyFile));

		getModelObject().setPrivateKeyFile(privateKeyFile);
		RuntimeExceptionDecorator.decorate(() -> PrivateKeyWriter
			.writeInPemFormat(getModelObject().getPrivateKey(), privateKeyFile));
		Component rootJDialog = AwtExtensions.getRootJDialog(this);
		if(rootJDialog != null && rootJDialog instanceof JDialog){
			JDialog dialog = (JDialog)rootJDialog;
			dialog.dispose();
		}
	}

	/**
	 * Callback method that can be overwritten to provide specific action for the on generate.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onGenerate(final ActionEvent actionEvent)
	{
		final KeySize selected = (KeySize)getCmbKeySize().getSelectedItem();
		getTxtPrivateKey().setText("Generating private key...");
		try
		{
			final KeyPair keyPair = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA,
				selected.getKeySize());

			getModelObject().setPrivateKey(keyPair.getPrivate());

			final String privateKeyFormat = PrivateKeyExtensions
				.toPemFormat(getModelObject().getPrivateKey());


			getTxtPrivateKey().setText("");
			getTxtPrivateKey().setText(privateKeyFormat);
			btnSaveStateMachine.onGenerate();
		}
		catch (final NoSuchAlgorithmException | NoSuchProviderException | IOException exception)
		{
			DialogExtensions.showExceptionDialog(exception, this);
			log.log(Level.SEVERE, exception.getLocalizedMessage(), exception);
		}
	}

	/**
	 * Callback method that can be overwritten to provide specific action for the on clear.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onClear(final ActionEvent actionEvent)
	{
		getCmbKeySize().setSelectedItem(KeySize.KEYSIZE_2048);
		getTxtPrivateKey().setText("");
		getTxtDirectoryOfPrivateKey().setText("");
		getTxtFilenameOfPrivateKey().setText("");
		getModelObject().setKeySize(KeySize.KEYSIZE_2048);
		getModelObject().setFilenameOfPrivateKey("");
		getModelObject().setPrivateKey(null);
		getModelObject().setPrivateKeyDirectory(null);
		getModelObject().setPrivateKeyFile(null);
		btnSaveStateMachine.onClear();
	}

	/**
	 * Callback method that can be overwritten to provide specific action for the on change key
	 * size.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onChangeKeySize(final ActionEvent actionEvent)
	{
		final JComboBox<String> cb = (JComboBox<String>)actionEvent.getSource();
		final KeySize selected = (KeySize)cb.getSelectedItem();
		getModelObject().setKeySize(selected);
		btnSaveStateMachine.onChangeKeySize();
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
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout
				.createSequentialGroup().addContainerGap().addGroup(layout
					.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout
						.createSequentialGroup().addGroup(layout
							.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
							.addComponent(btnClear, javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(btnGenerate, javax.swing.GroupLayout.DEFAULT_SIZE, 240,
								Short.MAX_VALUE)
							.addComponent(cmbKeySize, 0, javax.swing.GroupLayout.DEFAULT_SIZE,
								Short.MAX_VALUE))
						.addGap(0, 0, Short.MAX_VALUE))
					.addGroup(layout.createSequentialGroup()
						.addGroup(
							layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(lblFilenameOfPrivateKey,
									javax.swing.GroupLayout.PREFERRED_SIZE, 240,
									javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(lblDirectoryOfPrivateKey,
									javax.swing.GroupLayout.PREFERRED_SIZE, 240,
									javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(lblKeySize, javax.swing.GroupLayout.PREFERRED_SIZE,
									240, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
							javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
				.addGroup(layout
					.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
					.addComponent(txtFilenameOfPrivateKey)
					.addComponent(lblPrivateKey, javax.swing.GroupLayout.PREFERRED_SIZE, 147,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addComponent(scpPrivateKey)
					.addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
						layout.createSequentialGroup().addGroup(layout
							.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
							.addComponent(txtDirectoryOfPrivateKey)
							.addGroup(layout.createSequentialGroup()
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
									273, Short.MAX_VALUE)
								.addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 190,
									javax.swing.GroupLayout.PREFERRED_SIZE)))
							.addGap(18, 18, 18)
							.addGroup(layout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING,
									false)
								.addComponent(btnCancel, javax.swing.GroupLayout.DEFAULT_SIZE, 185,
									Short.MAX_VALUE)
								.addComponent(btnDirectoryOfPrivateKey,
									javax.swing.GroupLayout.DEFAULT_SIZE,
									javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
				.addGap(20, 20, 20)));
		layout.setVerticalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addGap(26, 26, 26)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblPrivateKey, javax.swing.GroupLayout.PREFERRED_SIZE, 29,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(lblKeySize, javax.swing.GroupLayout.PREFERRED_SIZE, 29,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(layout.createSequentialGroup()
							.addComponent(cmbKeySize, javax.swing.GroupLayout.PREFERRED_SIZE, 43,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addGap(18, 18, 18)
							.addComponent(btnGenerate, javax.swing.GroupLayout.PREFERRED_SIZE, 41,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
							.addComponent(btnClear, javax.swing.GroupLayout.PREFERRED_SIZE, 41,
								javax.swing.GroupLayout.PREFERRED_SIZE))
						.addComponent(scpPrivateKey, javax.swing.GroupLayout.PREFERRED_SIZE, 265,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(19, 19, 19)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(txtFilenameOfPrivateKey,
							javax.swing.GroupLayout.PREFERRED_SIZE, 41,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(lblFilenameOfPrivateKey,
							javax.swing.GroupLayout.PREFERRED_SIZE, 36,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
					.addGroup(
						layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
							.addComponent(
								btnDirectoryOfPrivateKey, javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addGroup(layout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(lblDirectoryOfPrivateKey,
									javax.swing.GroupLayout.PREFERRED_SIZE, 36,
									javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(txtDirectoryOfPrivateKey,
									javax.swing.GroupLayout.PREFERRED_SIZE, 45,
									javax.swing.GroupLayout.PREFERRED_SIZE)))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 35,
						Short.MAX_VALUE)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 41,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 41,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addContainerGap()));
	}

}
