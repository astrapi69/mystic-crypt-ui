/**
 * The MIT License
 * <p>
 * Copyright (C) 2015 Asterios Raptis
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.panels.signin;

import java.awt.event.ActionEvent;
import java.io.File;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.swing.*;
import javax.swing.event.DocumentEvent;

import lombok.Getter;
import io.github.astrapi69.crypto.algorithm.SunJCEAlgorithm;
import io.github.astrapi69.crypto.factories.CryptModelFactory;
import io.github.astrapi69.crypto.file.PBEFileDecryptor;
import io.github.astrapi69.crypto.key.PrivateKeyDecryptor;
import io.github.astrapi69.crypto.key.PrivateKeyGenericDecryptor;
import io.github.astrapi69.crypto.key.reader.EncryptedPrivateKeyReader;
import io.github.astrapi69.crypto.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypto.model.CryptModel;
import io.github.astrapi69.design.pattern.state.button.BtnOkComponentStateEnum;
import io.github.astrapi69.design.pattern.state.button.BtnOkStateMachine;
import io.github.astrapi69.json.JsonFileToObjectExtensions;
import io.github.astrapi69.json.JsonStringToObjectExtensions;
import io.github.astrapi69.json.factory.ObjectMapperFactory;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.Model;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.read.ReadFileExtensions;
import io.github.astrapi69.search.PathFinder;
import io.github.astrapi69.swing.adapters.DocumentListenerAdapter;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.system.SystemFileExtensions;

/**
 * The class {@link MasterPwFilePanel}
 */
@Getter
public class MasterPwWithApplicationFilePanel extends BasePanel<MasterPwFileModelBean>
{

	private static final long serialVersionUID = 1L;

	private javax.swing.JButton btnApplicationFileChooser;
	private javax.swing.JButton btnCancel;
	private javax.swing.JButton btnHelp;
	private javax.swing.JButton btnKeyFileChooser;
	private javax.swing.JButton btnMasterPw;
	private javax.swing.JButton btnOk;
	private javax.swing.JCheckBox cbxKeyFile;
	private javax.swing.JCheckBox cbxMasterPw;
	private javax.swing.JComboBox<String> cmbKeyFile;
	private javax.swing.JLabel lblApplicationFile;
	private javax.swing.JLabel lblImageHeader;
	private javax.swing.JTextField txtApplicationFile;
	private javax.swing.JPasswordField txtMasterPw;
	// ===
	// ===
	// ===
	private JFileChooser fileChooser;
	BtnOkStateMachine btnOkStateMachine;
	StringMutableComboBoxModel mutableComboBoxModel;

	/**
	 * Instantiates a new {@link MasterPwWithApplicationFilePanel}
	 */
	public MasterPwWithApplicationFilePanel()
	{
		this(BaseModel.<MasterPwFileModelBean> of(MasterPwFileModelBean.builder().build()));
	}

	/**
	 * Instantiates a new {@link MasterPwWithApplicationFilePanel}
	 *
	 * @param model
	 *            the model
	 */
	public MasterPwWithApplicationFilePanel(final Model<MasterPwFileModelBean> model)
	{
		super(model);
	}


	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		lblImageHeader = new javax.swing.JLabel();
		cbxMasterPw = new javax.swing.JCheckBox();
		cbxKeyFile = new javax.swing.JCheckBox();
		txtMasterPw = new javax.swing.JPasswordField();
		btnMasterPw = new javax.swing.JButton();
		btnKeyFileChooser = new javax.swing.JButton();
		btnHelp = new javax.swing.JButton();
		btnOk = new javax.swing.JButton();
		btnCancel = new javax.swing.JButton();
		lblApplicationFile = new javax.swing.JLabel();
		txtApplicationFile = new javax.swing.JTextField();
		btnApplicationFileChooser = new javax.swing.JButton();
		cmbKeyFile = new javax.swing.JComboBox<>();

		setPreferredSize(new java.awt.Dimension(820, 380));

		lblImageHeader.setText("Enter Master Key");

		cbxMasterPw.setText("Master Password:");

		cbxKeyFile.setText("Key File:");

		txtMasterPw.setText("jPasswordField1");

		btnMasterPw.setText("***");

		btnKeyFileChooser.setText("File");

		btnHelp.setText("Help");

		btnOk.setText("OK");

		btnCancel.setText("Cancel");

		lblApplicationFile.setText("Application File");

		btnApplicationFileChooser.setText("File");

		cmbKeyFile.setModel(new javax.swing.DefaultComboBoxModel<>(
			new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

		// ===
		// ===
		// ===
		cbxMasterPw.addActionListener(this::onCheckMasterPw);
		cbxKeyFile.addActionListener(this::onCheckKeyFile);
		btnMasterPw.addActionListener(this::onShowMasterPw);
		btnKeyFileChooser.addActionListener(this::onKeyFileChooser);
		btnOk.addActionListener(this::onOk);
		btnCancel.addActionListener(this::onCancel);

		btnOkStateMachine = BtnOkStateMachine.builder().component(btnOk)
			.current(BtnOkComponentStateEnum.DISABLED).build();
		txtMasterPw.setEnabled(false);
		txtMasterPw.getDocument().addDocumentListener(new DocumentListenerAdapter()
		{
			@Override
			public void onDocumentChanged(DocumentEvent e)
			{
				DocumentExtensions.processDocumentLength(e, btnOkStateMachine);
			}
		});

		txtApplicationFile.setText(
			getModelObject().getAppDataFile() != null ? getModelObject().getAppDataFile() : "");
		txtApplicationFile.setEnabled(false);

		btnApplicationFileChooser.setText("File");
		btnApplicationFileChooser.addActionListener(this::onApplicationFileChooser);

		File configDir = PathFinder.getRelativePath(SystemFileExtensions.getUserHomeDir(),
			MysticCryptApplicationFrame.DEFAULT_USER_CONFIGURATION_DIRECTORY_NAME,
			MysticCryptApplicationFrame.APPLICATION_NAME);
		fileChooser = new JFileChooser(configDir);

		mutableComboBoxModel = new StringMutableComboBoxModel(getModelObject().getKeyFilePaths(),
			getModelObject().getSelectedKeyFilePath());
		cmbKeyFile.setModel(mutableComboBoxModel);
		cmbKeyFile.addActionListener(this::onChangeCmbKeyFile);

		toggleMasterPwComponents();
		toggleKeyFileComponents();
	}

	protected void onChangeCmbKeyFile(final ActionEvent actionEvent)
	{
		Object item = cmbKeyFile.getSelectedItem();
		String selectedKeyFilePath = (String)item;
		if (selectedKeyFilePath == "")
		{
			getModelObject().setKeyFile(null);
		}
		else
		{
			File selectedKeyFile = new File(selectedKeyFilePath);
			if (selectedKeyFile != null && selectedKeyFile.exists())
			{
				getModelObject().setKeyFile(selectedKeyFile);
			}
			else if (selectedKeyFile == null
				|| selectedKeyFile != null && !selectedKeyFile.exists())
			{
				getModelObject().setKeyFile(null);
				mutableComboBoxModel.removeElement(selectedKeyFilePath);
			}
		}

	}

	protected void onInitializeGroupLayout()
	{
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout
					.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addGroup(
						layout.createSequentialGroup()
							.addComponent(btnHelp, javax.swing.GroupLayout.PREFERRED_SIZE, 122,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(
								btnOk, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addGap(18, 18, 18).addComponent(btnCancel,
								javax.swing.GroupLayout.PREFERRED_SIZE, 141,
								javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGroup(layout.createSequentialGroup()
						.addGroup(layout
							.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
							.addComponent(cbxMasterPw, javax.swing.GroupLayout.DEFAULT_SIZE, 170,
								Short.MAX_VALUE)
							.addComponent(cbxKeyFile, javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(
								lblApplicationFile, javax.swing.GroupLayout.Alignment.TRAILING,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(layout
							.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
							.addGroup(layout.createSequentialGroup()
								.addComponent(txtApplicationFile,
									javax.swing.GroupLayout.PREFERRED_SIZE, 520,
									javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(
									javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
								.addComponent(btnApplicationFileChooser,
									javax.swing.GroupLayout.PREFERRED_SIZE, 70,
									javax.swing.GroupLayout.PREFERRED_SIZE))
							.addGroup(layout.createSequentialGroup().addGroup(layout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(txtMasterPw, javax.swing.GroupLayout.PREFERRED_SIZE,
									520, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(cmbKeyFile, 0, javax.swing.GroupLayout.DEFAULT_SIZE,
									Short.MAX_VALUE))
								.addPreferredGap(
									javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
								.addGroup(layout
									.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING,
										false)
									.addComponent(btnMasterPw, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
									.addComponent(btnKeyFileChooser,
										javax.swing.GroupLayout.PREFERRED_SIZE, 70,
										javax.swing.GroupLayout.PREFERRED_SIZE)))))
					.addComponent(lblImageHeader, javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
					.addGap(24, 24, 24)));
		layout.setVerticalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addGap(57, 57, 57)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(txtApplicationFile, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(btnApplicationFileChooser)))
				.addGroup(layout.createSequentialGroup().addContainerGap().addComponent(
					lblImageHeader, javax.swing.GroupLayout.PREFERRED_SIZE, 34,
					javax.swing.GroupLayout.PREFERRED_SIZE))
				.addComponent(lblApplicationFile, javax.swing.GroupLayout.Alignment.TRAILING,
					javax.swing.GroupLayout.PREFERRED_SIZE, 35,
					javax.swing.GroupLayout.PREFERRED_SIZE))
				.addGap(18, 18, 18)
				.addGroup(layout
					.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
					.addComponent(cbxMasterPw, javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(txtMasterPw, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(btnMasterPw)))
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addComponent(cbxKeyFile, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(cmbKeyFile, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(btnKeyFileChooser)))
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 112,
					Short.MAX_VALUE)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
					.addComponent(btnHelp).addComponent(btnOk).addComponent(btnCancel))
				.addGap(42, 42, 42)));
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
	}


	protected void onCheckKeyFile(final ActionEvent actionEvent)
	{
		getModelObject().setWithKeyFile(!getModelObject().isWithKeyFile());
		toggleKeyFileComponents();
	}

	protected void toggleKeyFileComponents()
	{
		boolean cbxKeyFileSelected = cbxKeyFile.isSelected();
		cmbKeyFile.setEnabled(cbxKeyFileSelected);
		btnKeyFileChooser.setEnabled(cbxKeyFileSelected);
		btnOkStateMachine.setKeyFile(getModelObject().getKeyFile());
		btnOkStateMachine.setWithKeyFile(cbxKeyFileSelected);
		btnOkStateMachine.onChangeWithKeyFile(btnOkStateMachine);
	}

	protected void toggleMasterPwComponents()
	{
		boolean cbxMasterPwSelected = cbxMasterPw.isSelected();
		txtMasterPw.setEnabled(cbxMasterPwSelected);
		btnMasterPw.setEnabled(cbxMasterPwSelected);
		btnOkStateMachine.setWithMasterPassword(cbxMasterPwSelected);
		btnOkStateMachine.setPasswordLength(txtMasterPw.getDocument().getLength());
		btnOkStateMachine.onChangeWithMasterPassword(btnOkStateMachine);
	}

	protected void toggleApplicationFileComponents()
	{
		txtMasterPw.setEnabled(cbxMasterPw.isSelected());
		btnMasterPw.setEnabled(cbxKeyFile.isSelected());
		btnKeyFileChooser.setEnabled(cbxKeyFile.isSelected());
	}

	protected void onCheckMasterPw(final ActionEvent actionEvent)
	{
		Object source = actionEvent.getSource();
		if (source instanceof JCheckBox)
		{
			JCheckBox checkBox = (JCheckBox)source;
			getModelObject().setWithMasterPw(checkBox.isSelected());
		}
		toggleMasterPwComponents();
	}

	protected void onApplicationFileChooser(ActionEvent actionEvent)
	{
		System.err.println("onApplicationFileChooser method action called");
		final int returnVal = fileChooser.showSaveDialog(MasterPwWithApplicationFilePanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File selectedApplicationFile = fileChooser.getSelectedFile();
			getModelObject().setAppDataFile(selectedApplicationFile.getAbsolutePath());
			toggleApplicationFileComponents();
			txtApplicationFile.setText(getModelObject().getAppDataFile());
			btnOkStateMachine.setAppDataFile(getModelObject().getAppDataFile());
			btnOkStateMachine.onApplicationFileAdded(btnOkStateMachine);
		}
	}

	protected void onKeyFileChooser(ActionEvent actionEvent)
	{
		System.err.println("onKeyFileChooser method action called");
		final int returnVal = fileChooser.showSaveDialog(MasterPwWithApplicationFilePanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File selectedKeyFile = fileChooser.getSelectedFile();
			String absolutePath = selectedKeyFile.getAbsolutePath();
			mutableComboBoxModel.addElement(absolutePath);
			mutableComboBoxModel.setSelectedItem(absolutePath);
			getModelObject().setSelectedKeyFilePath(absolutePath);
			getModelObject().setKeyFile(selectedKeyFile);
			btnOkStateMachine.setKeyFile(selectedKeyFile);
			btnOkStateMachine.onSetKeyFile(btnOkStateMachine);
		}
	}

	protected void onOk(ActionEvent actionEvent)
	{
		System.err.println("onOk method action called");
		ApplicationModelBean applicationModelBean;
		MasterPwFileModelBean modelObject = getModelObject();
		String appDataFile = modelObject.getAppDataFile();
		File decrypt = null;
		try
		{
			if (cbxMasterPw.isSelected() && cbxKeyFile.isSelected())
			{
				char[] password = getTxtMasterPw().getPassword();
				File keyFile = modelObject.getKeyFile();
				PrivateKey privateKey;
				PrivateKeyDecryptor decryptor;
				PrivateKeyGenericDecryptor<String> genericDecryptor;
				CryptModel<Cipher, PrivateKey, byte[]> decryptModel;
				try
				{

					privateKey = EncryptedPrivateKeyReader.readPasswordProtectedPrivateKey(keyFile,
						String.valueOf(password));

					decryptModel = CryptModel.<Cipher, PrivateKey, byte[]> builder().key(privateKey)
						.build();
					decryptor = new PrivateKeyDecryptor(decryptModel);
					genericDecryptor = new PrivateKeyGenericDecryptor<>(decryptor);
					byte[] encryptedBytes = ReadFileExtensions
						.readFileToBytearray(new File(appDataFile));
					String json = genericDecryptor.decrypt(encryptedBytes);
					applicationModelBean = JsonStringToObjectExtensions.toObject(json,
						ApplicationModelBean.class, ObjectMapperFactory.newObjectMapper());
					MysticCryptApplicationFrame.getInstance().setModelObject(applicationModelBean);
				}
				catch (InvalidKeySpecException exception)
				{
					// TODO implement continues here...
					// Show dialog that password or key file is wrong
				}
			}
			else if (modelObject.isWithMasterPw())
			{
				char[] password = getTxtMasterPw().getPassword();
				CryptModel<Cipher, String, String> pbeCryptModel = CryptModelFactory
					.newCryptModel(SunJCEAlgorithm.PBEWithMD5AndDES, new String(password));
				PBEFileDecryptor fileDecryptor = new PBEFileDecryptor(pbeCryptModel);
				try
				{
					decrypt = fileDecryptor.decrypt(new File(appDataFile));
					applicationModelBean = JsonFileToObjectExtensions.toObject(decrypt,
						ApplicationModelBean.class, ObjectMapperFactory.newObjectMapper());
					MysticCryptApplicationFrame.getInstance().setModelObject(applicationModelBean);
				}
				catch (Exception exception)
				{
					// TODO
					// Show dialog that password is wrong
				}
			}
			else if (modelObject.isWithKeyFile())
			{
				File keyFile = modelObject.getKeyFile();
				PrivateKey privateKey = PrivateKeyReader.readPrivateKey(keyFile);

				PrivateKeyDecryptor decryptor;
				PrivateKeyGenericDecryptor<String> genericDecryptor;
				CryptModel<Cipher, PrivateKey, byte[]> decryptModel;

				decryptModel = CryptModel.<Cipher, PrivateKey, byte[]> builder().key(privateKey)
					.build();
				decryptor = new PrivateKeyDecryptor(decryptModel);
				genericDecryptor = new PrivateKeyGenericDecryptor<>(decryptor);
				byte[] encryptedBytes = ReadFileExtensions
					.readFileToBytearray(new File(appDataFile));
				try
				{
					String json = genericDecryptor.decrypt(encryptedBytes);
					applicationModelBean = JsonStringToObjectExtensions.toObject(json,
						ApplicationModelBean.class, ObjectMapperFactory.newObjectMapper());
					MysticCryptApplicationFrame.getInstance().setModelObject(applicationModelBean);
				}
				catch (Exception exception)
				{
					// TODO
					// Show dialog that key file is wrong
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	protected void onCancel(ActionEvent actionEvent)
	{
		System.err.println("onCancel method action called");
	}

	protected void onShowMasterPw(final ActionEvent actionEvent)
	{
		setEchoChar();
		getModelObject().setShowMasterPw(!getModelObject().isShowMasterPw());
	}

	private void setEchoChar()
	{
		if (getModelObject().isShowMasterPw())
		{
			getTxtMasterPw().setEchoChar('*');
		}
		else
		{
			char zero = 0;
			getTxtMasterPw().setEchoChar(zero);
		}
	}
}
