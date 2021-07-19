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

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.logging.Level;

import javax.swing.*;

import lombok.Getter;
import lombok.extern.java.Log;
import io.github.astrapi69.create.FileCreationState;
import io.github.astrapi69.create.FileFactory;
import io.github.astrapi69.crypto.algorithm.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypto.factories.KeyPairFactory;
import io.github.astrapi69.crypto.key.KeySize;
import io.github.astrapi69.crypto.key.PrivateKeyExtensions;
import io.github.astrapi69.crypto.key.writer.PrivateKeyWriter;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.Model;
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
    private javax.swing.JButton btnGenerate;
    private javax.swing.JButton btnSave;
    private javax.swing.JComboBox<KeySize> cmbKeySize;
    private javax.swing.JLabel lblKeySize;
    private javax.swing.JLabel lblPrivateKey;
    private javax.swing.JScrollPane scpPrivateKey;
    private javax.swing.JTextArea txtPrivateKey;
	// ===
	// ===
	// ===
	private JFileChooser fileChooser;

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

		txtPrivateKey.setColumns(20);
		txtPrivateKey.setRows(5);
		scpPrivateKey.setViewportView(txtPrivateKey);
		txtPrivateKey.getAccessibleContext().setAccessibleDescription("");
		txtPrivateKey.setEditable(false);

		cmbKeySize.setModel(new EnumComboBoxModel<>(KeySize.class));

		btnGenerate.setText("Generate key");

		lblPrivateKey.setText("Private key");

		lblKeySize.setText("Keysize");

		btnClear.setText("Clear key");

		btnCancel.setText("Cancel");

		btnSave.setText("Save private key");
		// ===
		// ===
		// ===

		cmbKeySize.setModel(new EnumComboBoxModel<>(KeySize.class));
		if (getModelObject().getKeySize() == null)
		{
			getModelObject().setKeySize(KeySize.KEYSIZE_2048);
		}
		cmbKeySize.setSelectedItem(getModelObject().getKeySize());

		cmbKeySize.addActionListener(actionEvent -> onChangeKeySize(actionEvent));

		btnGenerate.addActionListener(actionEvent -> onGenerate(actionEvent));
		btnClear.addActionListener(actionEvent -> onClear(actionEvent));

		btnSave.addActionListener(actionEvent -> onSave(actionEvent));
		btnSave.setEnabled(false);

		btnCancel.addActionListener(this::onCancel);

		fileChooser = new JFileChooser(SystemFileExtensions.getUserDownloadsDir());
	}

	protected void onCancel(ActionEvent actionEvent)
	{
		System.err.println("onCancel method action called");
	}

	protected void onSave(ActionEvent actionEvent)
	{
		final int returnVal = fileChooser.showSaveDialog(NewPrivateKeyPanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File selectedKeyFile = fileChooser.getSelectedFile();
			getModelObject().setPrivateKeyFile(selectedKeyFile);
			FileCreationState fileCreationState = RuntimeExceptionDecorator
				.decorate(() -> FileFactory.newFile(selectedKeyFile));
			RuntimeExceptionDecorator.decorate(() -> PrivateKeyWriter
				.writeInPemFormat(getModelObject().getPrivateKey(), selectedKeyFile));
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
			btnSave.setEnabled(true);
		}
		catch (final NoSuchAlgorithmException | NoSuchProviderException | IOException e)
		{
			// TODO show error dialog
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
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
		getModelObject().setKeySize(KeySize.KEYSIZE_2048);
		btnSave.setEnabled(false);
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
		layout
			.setHorizontalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup().addGroup(layout
						.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
						.addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout
							.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
							.addComponent(cmbKeySize, 0, javax.swing.GroupLayout.DEFAULT_SIZE,
								Short.MAX_VALUE)
							.addComponent(btnGenerate, javax.swing.GroupLayout.DEFAULT_SIZE, 206,
								Short.MAX_VALUE)))
						.addGroup(layout.createSequentialGroup().addGap(21, 21, 21).addComponent(
							lblKeySize, javax.swing.GroupLayout.PREFERRED_SIZE, 147,
							javax.swing.GroupLayout.PREFERRED_SIZE))
						.addGroup(layout.createSequentialGroup().addContainerGap().addComponent(
							btnClear, javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
						.addGap(18, 18, 18)
						.addGroup(layout
							.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
							.addGroup(layout.createSequentialGroup()
								.addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 190,
									javax.swing.GroupLayout.PREFERRED_SIZE)
								.addGap(18, 18, 18).addComponent(btnCancel,
									javax.swing.GroupLayout.PREFERRED_SIZE, 174,
									javax.swing.GroupLayout.PREFERRED_SIZE))
							.addGroup(layout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(scpPrivateKey, javax.swing.GroupLayout.PREFERRED_SIZE,
									480, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(lblPrivateKey, javax.swing.GroupLayout.PREFERRED_SIZE,
									147, javax.swing.GroupLayout.PREFERRED_SIZE)))
						.addContainerGap(57, Short.MAX_VALUE)));
		layout
			.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
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
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 57,
						Short.MAX_VALUE)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 41,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 41,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addContainerGap()));
	}

}
