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
import java.util.logging.Level;

import javax.crypto.Cipher;
import javax.swing.*;
import javax.swing.event.DocumentEvent;

import lombok.Getter;
import lombok.extern.java.Log;
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
@Log
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
	private javax.swing.JComboBox<String> cmbApplicationFile;
	private javax.swing.JComboBox<String> cmbKeyFile;
	private javax.swing.JLabel lblApplicationFile;
	private javax.swing.JLabel lblImageHeader;
	private javax.swing.JPasswordField txtMasterPw;
	// ===
	// ===
	// ===
	private JFileChooser fileChooser;
	BtnOkStateMachine btnOkStateMachine;
	StringMutableComboBoxModel cmbKeyFileModel;
	StringMutableComboBoxModel cmbApplicationFileModel;

	/**
	 * Instantiates a new {@link MasterPwWithApplicationFilePanel}
	 */
	public MasterPwWithApplicationFilePanel()
	{
		this(BaseModel.<MasterPwFileModelBean> of(MasterPwFileModelBean.builder()
			.minPasswordLength(6)
			.withKeyFile(false)
			.withMasterPw(false)
			.showMasterPw(false).build()));
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
		btnApplicationFileChooser = new javax.swing.JButton();
		cmbKeyFile = new javax.swing.JComboBox<>();
		cmbApplicationFile = new javax.swing.JComboBox<>();

		setPreferredSize(new java.awt.Dimension(880, 360));

		lblImageHeader.setText("Enter Master Key");

		cbxMasterPw.setText("Master Password:");

		cbxKeyFile.setText("Key File:");

		txtMasterPw.setText("");

		btnMasterPw.setText("***");

		btnKeyFileChooser.setText("Browse...");

		btnHelp.setText("Help");

		btnOk.setText("OK");

		btnCancel.setText("Cancel");

		lblApplicationFile.setText("Application File");

		btnApplicationFileChooser.setText("Browse...");

		cmbKeyFile.setModel(new javax.swing.DefaultComboBoxModel<>(
			new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

		cmbApplicationFile.setModel(new javax.swing.DefaultComboBoxModel<>(
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

		MasterPwFileModelBean modelObject = getModelObject();
		btnOkStateMachine = BtnOkStateMachine.builder().component(btnOk)
			.modelObject(modelObject)
			.current(BtnOkComponentStateEnum.DISABLED).build();
		txtMasterPw.setEnabled(false);
		txtMasterPw.getDocument().addDocumentListener(new DocumentListenerAdapter()
		{
			@Override
			public void onDocumentChanged(DocumentEvent e)
			{
				char[] password = txtMasterPw.getPassword();
				getModelObject().setMasterPw(password);
				DocumentExtensions.processDocumentLength(e, btnOkStateMachine);
			}
		});

		btnApplicationFileChooser.addActionListener(this::onApplicationFileChooser);

		File configDir = PathFinder.getRelativePath(SystemFileExtensions.getUserHomeDir(),
			MysticCryptApplicationFrame.DEFAULT_USER_CONFIGURATION_DIRECTORY_NAME,
			MysticCryptApplicationFrame.APPLICATION_NAME);
		fileChooser = new JFileChooser(configDir);

		cmbKeyFileModel = new StringMutableComboBoxModel(modelObject.getKeyFilePaths(),
			modelObject.getSelectedKeyFilePath());
		cmbKeyFile.setModel(cmbKeyFileModel);
		cmbKeyFile.addActionListener(this::onChangeCmbKeyFile);

		cmbApplicationFileModel = new StringMutableComboBoxModel(
			modelObject.getApplicationFilePaths(), modelObject.getSelectedKeyFilePath());
		cmbApplicationFile.setModel(cmbApplicationFileModel);
		cmbApplicationFile.addActionListener(this::onChangeCmbApplicationFile);

		toggleMasterPwComponents();
		toggleKeyFileComponents();
	}

	protected void onChangeCmbApplicationFile(final ActionEvent actionEvent)
	{
		Object item = cmbApplicationFile.getSelectedItem();
		String selectedApplicationFilePath = (String)item;
		getModelObject().setSelectedApplicationFilePath(selectedApplicationFilePath);
		if (selectedApplicationFilePath == "")
		{
			getModelObject().setApplicationFile(null);
			btnOkStateMachine.onApplicationFileAdded(btnOkStateMachine);
		}
		else
		{
			File selectedApplicationFile = new File(selectedApplicationFilePath);
			if (selectedApplicationFile != null && selectedApplicationFile.exists())
			{
				getModelObject().setApplicationFile(selectedApplicationFile);
				btnOkStateMachine.onApplicationFileAdded(btnOkStateMachine);
			}
			else if (selectedApplicationFile == null
				|| selectedApplicationFile != null && !selectedApplicationFile.exists())
			{
				getModelObject().setApplicationFile(null);
				cmbApplicationFileModel.removeElement(selectedApplicationFilePath);
				btnOkStateMachine.onApplicationFileAdded(btnOkStateMachine);
			}
		}
	}

	protected void onChangeCmbKeyFile(final ActionEvent actionEvent)
	{
		Object item = cmbKeyFile.getSelectedItem();
		String selectedKeyFilePath = (String)item;
		getModelObject().setSelectedKeyFilePath(selectedKeyFilePath);
		if (selectedKeyFilePath == "")
		{
			getModelObject().setKeyFile(null);
			btnOkStateMachine.onSetKeyFile(btnOkStateMachine);
		}
		else
		{
			File selectedKeyFile = new File(selectedKeyFilePath);
			if (selectedKeyFile != null && selectedKeyFile.exists())
			{
				getModelObject().setKeyFile(selectedKeyFile);
				btnOkStateMachine.onSetKeyFile(btnOkStateMachine);
			}
			else if (selectedKeyFile == null
				|| selectedKeyFile != null && !selectedKeyFile.exists())
			{
				getModelObject().setKeyFile(null);
				cmbKeyFileModel.removeElement(selectedKeyFilePath);
				btnOkStateMachine.onSetKeyFile(btnOkStateMachine);
			}
		}
	}

	protected void onInitializeGroupLayout()
	{


		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
				.addGroup(layout.createSequentialGroup()
					.addComponent(lblImageHeader, javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addGap(24, 24, 24))
				.addGroup(javax.swing.GroupLayout.Alignment.LEADING,
					layout.createSequentialGroup().addGroup(layout
						.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
						.addGroup(layout.createSequentialGroup()
							.addComponent(btnHelp, javax.swing.GroupLayout.PREFERRED_SIZE, 122,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(btnOk, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addGap(18, 18, 18).addComponent(btnCancel,
								javax.swing.GroupLayout.PREFERRED_SIZE, 141,
								javax.swing.GroupLayout.PREFERRED_SIZE))
						.addGroup(layout.createSequentialGroup().addGroup(layout
							.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
							.addComponent(cbxMasterPw, javax.swing.GroupLayout.Alignment.LEADING,
								javax.swing.GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE)
							.addComponent(lblApplicationFile,
								javax.swing.GroupLayout.Alignment.LEADING,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(cbxKeyFile, javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
							.addGap(18, 18, 18)
							.addGroup(layout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(txtMasterPw, javax.swing.GroupLayout.PREFERRED_SIZE,
									520, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(cmbApplicationFile,
									javax.swing.GroupLayout.PREFERRED_SIZE, 520,
									javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(cmbKeyFile, javax.swing.GroupLayout.PREFERRED_SIZE,
									520, javax.swing.GroupLayout.PREFERRED_SIZE))
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
							.addGroup(layout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(btnMasterPw, javax.swing.GroupLayout.PREFERRED_SIZE,
									102, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(btnApplicationFileChooser,
									javax.swing.GroupLayout.PREFERRED_SIZE, 102,
									javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(btnKeyFileChooser,
									javax.swing.GroupLayout.PREFERRED_SIZE, 102,
									javax.swing.GroupLayout.PREFERRED_SIZE))))
						.addContainerGap(36, Short.MAX_VALUE)))));
		layout.setVerticalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
					.addComponent(lblImageHeader, javax.swing.GroupLayout.PREFERRED_SIZE, 34,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGap(60, 60, 60))
				.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblApplicationFile, javax.swing.GroupLayout.PREFERRED_SIZE,
							35, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(cmbApplicationFile, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(btnApplicationFileChooser))
					.addGap(18, 18, 18)))
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
					.addComponent(txtMasterPw, javax.swing.GroupLayout.PREFERRED_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addComponent(btnMasterPw).addComponent(cbxMasterPw,
						javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
						Short.MAX_VALUE))
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
					.addComponent(cmbKeyFile, javax.swing.GroupLayout.PREFERRED_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addComponent(btnKeyFileChooser).addComponent(cbxKeyFile,
						javax.swing.GroupLayout.PREFERRED_SIZE, 35,
						javax.swing.GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 96,
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
		toggleKeyFileComponents();
	}

	protected void toggleKeyFileComponents()
	{
		boolean cbxKeyFileSelected = cbxKeyFile.isSelected();
		getModelObject().setWithKeyFile(cbxKeyFileSelected);
		cmbKeyFile.setEnabled(cbxKeyFileSelected);
		btnKeyFileChooser.setEnabled(cbxKeyFileSelected);
		btnOkStateMachine.onChangeWithKeyFile(btnOkStateMachine);
	}

	protected void toggleMasterPwComponents()
	{
		boolean cbxMasterPwSelected = cbxMasterPw.isSelected();
		txtMasterPw.setEnabled(cbxMasterPwSelected);
		btnMasterPw.setEnabled(cbxMasterPwSelected);
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
			String absolutePath = selectedApplicationFile.getAbsolutePath();
			cmbApplicationFileModel.addElement(absolutePath);
			cmbApplicationFileModel.setSelectedItem(absolutePath);
			getModelObject().setApplicationFile(selectedApplicationFile);
			toggleApplicationFileComponents();
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
			cmbKeyFileModel.addElement(absolutePath);
			cmbKeyFileModel.setSelectedItem(absolutePath);
			getModelObject().setSelectedKeyFilePath(absolutePath);
			getModelObject().setKeyFile(selectedKeyFile);
			btnOkStateMachine.onSetKeyFile(btnOkStateMachine);
		}
	}

	protected void onOk(ActionEvent actionEvent)
	{
		System.err.println("onOk method action called");
		ApplicationModelBean applicationModelBean;
		MasterPwFileModelBean modelObject = getModelObject();
		File applicationFile = modelObject.getApplicationFile();
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
				byte[] encryptedBytes = ReadFileExtensions.readFileToBytearray(applicationFile);
				String json = genericDecryptor.decrypt(encryptedBytes);
				applicationModelBean = JsonStringToObjectExtensions.toObject(json,
					ApplicationModelBean.class, ObjectMapperFactory.newObjectMapper());
				MysticCryptApplicationFrame.getInstance().setModelObject(applicationModelBean);
			}
			catch (Exception exception)
			{
				String title = "Authentication with Password or key file";
				String htmlMessage = "<html><body width='350'>" + "<h2>" + title + "</h2>"
					+ "<p> Password or key file or both are not valid" + "<p>"
					+ exception.getMessage();
				JOptionPane.showMessageDialog(this, htmlMessage, title, JOptionPane.ERROR_MESSAGE);
				log.log(Level.SEVERE, exception.getMessage(), exception);
			}
		}
		else if (modelObject.isWithMasterPw())
		{
			char[] password = getTxtMasterPw().getPassword();
			CryptModel<Cipher, String, String> pbeCryptModel = CryptModelFactory
				.newCryptModel(SunJCEAlgorithm.PBEWithMD5AndDES, new String(password));
			try
			{
				PBEFileDecryptor fileDecryptor = new PBEFileDecryptor(pbeCryptModel);
				File decrypt = fileDecryptor.decrypt(applicationFile);
				applicationModelBean = JsonFileToObjectExtensions.toObject(decrypt,
					ApplicationModelBean.class, ObjectMapperFactory.newObjectMapper());
				MysticCryptApplicationFrame.getInstance().setModelObject(applicationModelBean);
			}
			catch (Exception exception)
			{
				String title = "Authentication with Password";
				String htmlMessage = "<html><body width='350'>" + "<h2>" + title + "</h2>"
					+ "<p> Password is not valid" + "<p>" + exception.getMessage();
				JOptionPane.showMessageDialog(this, htmlMessage, title, JOptionPane.ERROR_MESSAGE);
				log.log(Level.SEVERE, exception.getMessage(), exception);
			}
		}
		else if (modelObject.isWithKeyFile())
		{
			File keyFile = modelObject.getKeyFile();
			try
			{
				PrivateKey privateKey = PrivateKeyReader.readPrivateKey(keyFile);

				PrivateKeyDecryptor decryptor;
				PrivateKeyGenericDecryptor<String> genericDecryptor;
				CryptModel<Cipher, PrivateKey, byte[]> decryptModel;

				decryptModel = CryptModel.<Cipher, PrivateKey, byte[]> builder().key(privateKey)
					.build();
				decryptor = new PrivateKeyDecryptor(decryptModel);
				genericDecryptor = new PrivateKeyGenericDecryptor<>(decryptor);
				byte[] encryptedBytes = ReadFileExtensions.readFileToBytearray(applicationFile);
				String json = genericDecryptor.decrypt(encryptedBytes);
				applicationModelBean = JsonStringToObjectExtensions.toObject(json,
					ApplicationModelBean.class, ObjectMapperFactory.newObjectMapper());
				MysticCryptApplicationFrame.getInstance().setModelObject(applicationModelBean);
			}
			catch (Exception exception)
			{
				String title = "Authentication with key file";
				String htmlMessage = "<html><body width='350'>" + "<h2>" + title + "</h2>"
					+ "<p> Key file is not valid" + "<p>" + exception.getMessage();
				JOptionPane.showMessageDialog(this, htmlMessage, title, JOptionPane.ERROR_MESSAGE);
				log.log(Level.SEVERE, exception.getMessage(), exception);
			}
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
