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

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import lombok.Getter;
import de.alpharogroup.file.read.ReadFileExtensions;
import de.alpharogroup.file.search.PathFinder;
import de.alpharogroup.file.system.SystemFileExtensions;
import de.alpharogroup.json.JsonFileToObjectExtensions;
import de.alpharogroup.json.JsonStringToObjectExtensions;
import de.alpharogroup.json.factory.ObjectMapperFactory;
import de.alpharogroup.model.BaseModel;
import de.alpharogroup.model.api.Model;
import io.github.astrapi69.crypto.algorithm.SunJCEAlgorithm;
import io.github.astrapi69.crypto.factories.CryptModelFactory;
import io.github.astrapi69.crypto.file.PBEFileDecryptor;
import io.github.astrapi69.crypto.key.PrivateKeyDecryptor;
import io.github.astrapi69.crypto.key.PrivateKeyGenericDecryptor;
import io.github.astrapi69.crypto.key.reader.EncryptedPrivateKeyReader;
import io.github.astrapi69.crypto.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypto.model.CryptModel;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.SpringBootSwingApplication;
import io.github.astrapi69.swing.adapters.DocumentListenerAdapter;
import io.github.astrapi69.swing.base.BasePanel;

/**
 * The class {@link MasterPwFilePanel}
 */
@Getter
public class MasterPwWithApplicationFilePanel extends BasePanel<MasterPwFileModelBean>
{

	private static final long serialVersionUID = 1L;

	private JButton btnApplicationFileChooser;
	private JButton btnCancel;
	private JButton btnHelp;
	private JButton btnKeyFileChooser;
	private JButton btnMasterPw;
	private JButton btnOk;
	private JCheckBox cbxKeyFile;
	private JCheckBox cbxMasterPw;
	private JLabel lblApplicationFile;
	private JLabel lblImageHeader;
	private JTextField txtApplicationFile;
	private JTextField txtKeyFile;
	private JPasswordField txtMasterPw;
	private JFileChooser fileChooser;

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

		lblImageHeader = new JLabel();
		cbxMasterPw = new JCheckBox();
		cbxKeyFile = new JCheckBox();
		txtMasterPw = new JPasswordField();
		btnMasterPw = new JButton();
		txtKeyFile = new JTextField();
		btnKeyFileChooser = new JButton();
		btnHelp = new JButton();
		btnOk = new JButton();
		btnCancel = new JButton();
		lblApplicationFile = new JLabel();
		txtApplicationFile = new JTextField();
		btnApplicationFileChooser = new JButton();
		toggleMasterPwComponents();
		toggleKeyFileComponents();

		setPreferredSize(new java.awt.Dimension(820, 380));

		lblImageHeader.setText("Enter Master Key");

		cbxMasterPw.setText("Master Password:");
		cbxMasterPw.addActionListener(this::onCheckMasterPw);

		cbxKeyFile.setText("Key File:");
		cbxKeyFile.addActionListener(this::onCheckKeyFile);

		txtMasterPw.setText("");
		txtMasterPw.setEnabled(false);
		txtMasterPw.getDocument().addDocumentListener(new DocumentListenerAdapter()
		{
			@Override
			public void onDocumentChanged(DocumentEvent e)
			{
				btnOk.getModel().setEnabled(getBtnOkEnabledState());
			}
		});

		btnMasterPw.setText("***");
		btnMasterPw.addActionListener(this::onShowMasterPw);

		txtKeyFile.setText("");
		txtKeyFile.setEnabled(false);

		btnKeyFileChooser.setText("File");
		btnKeyFileChooser.addActionListener(this::onKeyFileChooser);

		btnHelp.setText("Help");

		btnOk.setText("OK");
		btnOk.addActionListener(this::onOk);
		btnOk.getModel().setEnabled(getBtnOkEnabledState());

		btnCancel.setText("Cancel");
		btnCancel.addActionListener(this::onCancel);

		lblApplicationFile.setText("Application File");

		txtApplicationFile.setText(
			getModelObject().getAppDataFile() != null ? getModelObject().getAppDataFile() : "");
		txtApplicationFile.setEnabled(false);

		btnApplicationFileChooser.setText("File");
		btnApplicationFileChooser.addActionListener(this::onApplicationFileChooser);
		File configDir = PathFinder.getRelativePath(SystemFileExtensions.getUserHomeDir(),
			MysticCryptApplicationFrame.DEFAULT_USER_CONFIGURATION_DIRECTORY_NAME,
			MysticCryptApplicationFrame.APPLICATION_NAME);
		fileChooser = new JFileChooser(configDir);
	}

	protected void onInitializeGroupLayout()
	{
		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
					.addGroup(layout.createSequentialGroup()
						.addComponent(btnHelp, GroupLayout.PREFERRED_SIZE, 122,
							GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
							GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(btnOk, GroupLayout.PREFERRED_SIZE, 140,
							GroupLayout.PREFERRED_SIZE)
						.addGap(18, 18, 18).addComponent(btnCancel, GroupLayout.PREFERRED_SIZE, 141,
							GroupLayout.PREFERRED_SIZE))
					.addGroup(layout.createSequentialGroup().addGroup(layout
						.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(cbxMasterPw, GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)
						.addComponent(cbxKeyFile, GroupLayout.DEFAULT_SIZE,
							GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(lblApplicationFile, GroupLayout.Alignment.TRAILING,
							GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(
							layout.createParallelGroup(GroupLayout.Alignment.LEADING)
								.addGroup(layout.createSequentialGroup()
									.addComponent(txtApplicationFile, GroupLayout.PREFERRED_SIZE,
										520, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
									.addComponent(btnApplicationFileChooser,
										GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE))
								.addGroup(layout.createSequentialGroup()
									.addGroup(layout
										.createParallelGroup(GroupLayout.Alignment.LEADING, false)
										.addComponent(txtMasterPw)
										.addComponent(txtKeyFile, GroupLayout.PREFERRED_SIZE, 520,
											GroupLayout.PREFERRED_SIZE))
									.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
									.addGroup(layout
										.createParallelGroup(GroupLayout.Alignment.LEADING, false)
										.addComponent(btnMasterPw, GroupLayout.DEFAULT_SIZE,
											GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addComponent(btnKeyFileChooser, GroupLayout.PREFERRED_SIZE,
											70, GroupLayout.PREFERRED_SIZE)))))
					.addComponent(lblImageHeader, GroupLayout.DEFAULT_SIZE,
						GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				.addGap(24, 24, 24)));
		layout
			.setVerticalGroup(
				layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addGroup(layout.createSequentialGroup().addGap(57, 57, 57)
								.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
									.addComponent(txtApplicationFile, GroupLayout.PREFERRED_SIZE,
										GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
									.addComponent(btnApplicationFileChooser)))
							.addGroup(layout.createSequentialGroup().addContainerGap().addComponent(
								lblImageHeader, GroupLayout.PREFERRED_SIZE, 34,
								GroupLayout.PREFERRED_SIZE))
							.addComponent(lblApplicationFile, GroupLayout.Alignment.TRAILING,
								GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE))
						.addGap(18, 18, 18)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
							.addComponent(cbxMasterPw, GroupLayout.DEFAULT_SIZE,
								GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
								.addComponent(txtMasterPw, GroupLayout.PREFERRED_SIZE,
									GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(btnMasterPw)))
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
							.addComponent(txtKeyFile, GroupLayout.PREFERRED_SIZE,
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addComponent(btnKeyFileChooser).addComponent(cbxKeyFile,
								GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 124,
							Short.MAX_VALUE)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
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
		btnKeyFileChooser.setEnabled(cbxKeyFile.isSelected());
		btnOk.getModel().setEnabled(getBtnOkEnabledState());
	}

	protected void toggleMasterPwComponents()
	{
		txtMasterPw.setEnabled(cbxMasterPw.isSelected());
		btnMasterPw.setEnabled(cbxMasterPw.isSelected());
		btnOk.getModel().setEnabled(getBtnOkEnabledState());
	}

	protected void toggleApplicationFileComponents()
	{
		txtMasterPw.setEnabled(cbxMasterPw.isSelected());
		btnMasterPw.setEnabled(cbxKeyFile.isSelected());
		btnKeyFileChooser.setEnabled(cbxKeyFile.isSelected());
		btnOk.getModel().setEnabled(getBtnOkEnabledState());
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
			boolean okEnabledState = getBtnOkEnabledState();
			btnOk.getModel().setEnabled(okEnabledState);
		}
	}

	protected void onKeyFileChooser(ActionEvent actionEvent)
	{
		System.err.println("onKeyFileChooser method action called");
		final int returnVal = fileChooser.showSaveDialog(MasterPwWithApplicationFilePanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File selectedKeyFile = fileChooser.getSelectedFile();
			getModelObject().setKeyFile(selectedKeyFile);
			btnOk.getModel().setEnabled(getBtnOkEnabledState());
			txtKeyFile.setText(getModelObject().getKeyFile().getName());
		}
	}

	protected boolean getBtnOkEnabledState()
	{
		boolean result = true;
		MasterPwFileModelBean modelObject = getModelObject();
		if (modelObject.getAppDataFile() == null)
		{
			return false;
		}
		if (modelObject.isWithMasterPw() && modelObject.isWithKeyFile()
			&& !(0 < txtMasterPw.getDocument().getLength() && modelObject.getKeyFile() != null))
		{
			return false;
		}
		if (!modelObject.isWithMasterPw() && !modelObject.isWithKeyFile())
		{
			return false;
		}
		if (modelObject.isWithMasterPw() && !modelObject.isWithKeyFile()
			&& txtMasterPw.getDocument().getLength() == 0)
		{
			return false;
		}
		if (!modelObject.isWithMasterPw() && modelObject.isWithKeyFile()
			&& modelObject.getKeyFile() == null)
		{
			return false;
		}
		return result;
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
				try{

					privateKey = EncryptedPrivateKeyReader
						.readPasswordProtectedPrivateKey(keyFile, String.valueOf(password));

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
				} catch (InvalidKeySpecException exception){
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
				try {
					decrypt = fileDecryptor.decrypt(new File(appDataFile));
					applicationModelBean = JsonFileToObjectExtensions.toObject(decrypt,
						ApplicationModelBean.class, ObjectMapperFactory.newObjectMapper());
					MysticCryptApplicationFrame.getInstance().setModelObject(applicationModelBean);
				} catch (Exception exception){
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
				} catch (Exception exception) {
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
