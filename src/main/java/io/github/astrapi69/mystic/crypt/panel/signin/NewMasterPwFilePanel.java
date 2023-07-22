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
package io.github.astrapi69.mystic.crypt.panel.signin;

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import org.apache.commons.lang3.StringUtils;

import io.github.astrapi69.browser.BrowserControlExtensions;
import io.github.astrapi69.crypt.data.model.KeyModel;
import io.github.astrapi69.file.create.FileCreationState;
import io.github.astrapi69.file.create.FileFactory;
import io.github.astrapi69.file.create.FileInfo;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileFactory;
import io.github.astrapi69.mystic.crypt.panel.privatekey.NewPrivateKeyFileDialog;
import io.github.astrapi69.mystic.crypt.panel.privatekey.NewPrivateKeyModelBean;
import io.github.astrapi69.mystic.crypt.panel.pw.GeneratePasswordDialog;
import io.github.astrapi69.mystic.crypt.panel.pw.GeneratePasswordModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.button.state.ok.BtnOkComponentStateEnum;
import io.github.astrapi69.mystic.crypt.panel.signin.button.state.ok.BtnOkStateMachine;
import io.github.astrapi69.net.url.URLExtensions;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.model.combobox.GenericMutableComboBoxModel;
import io.github.astrapi69.swing.component.JMTextField;
import io.github.astrapi69.swing.dialog.help.HelpDialog;
import io.github.astrapi69.swing.listener.document.DocumentListenerAdapter;
import io.github.astrapi69.swing.panel.help.HelpModelBean;
import io.github.astrapi69.swing.util.ClipboardExtensions;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewMasterPwFilePanel extends BasePanel<MasterPwFileModelBean>
{

	private javax.swing.JButton btnApplicationFileChooser;
	private javax.swing.JButton btnCancel;
	private javax.swing.JButton btnCreateKeyFile;
	private javax.swing.JButton btnGeneratePw;
	private javax.swing.JButton btnHelp;
	private javax.swing.JButton btnKeyFileChooser;
	private javax.swing.JButton btnMasterPw;
	private javax.swing.JButton btnOk;
	private javax.swing.JCheckBox cbxKeyFile;
	private javax.swing.JCheckBox cbxMasterPw;
	private javax.swing.JComboBox<String> cmbKeyFile;
	private javax.swing.JLabel lblApplicationFile;
	private javax.swing.JLabel lblImageHeader;
	private javax.swing.JLabel lblRepeatPw;
	private javax.swing.JTextField txtApplicationFile;
	private javax.swing.JPasswordField txtMasterPw;
	private javax.swing.JPasswordField txtRepeatPw;
	// ===
	// ===
	// ===
	JFileChooser fileChooser;
	GenericMutableComboBoxModel<String> cmbKeyFileModel;
	BtnOkStateMachine btnOkStateMachine;
	IModel<GeneratePasswordModelBean> passwordModel;
	NewPrivateKeyModelBean privateKeyModelBean;

	/**
	 * Creates new form NewMasterPwFileFormPanel
	 */
	public NewMasterPwFilePanel()
	{
		this(BaseModel.of(MasterPwFileModelBean.builder().minPasswordLength(6).withKeyFile(false)
			.withMasterPw(false).showMasterPw(false).build()));
	}

	/**
	 * Instantiates a new {@link MasterPwWithApplicationFilePanel}
	 *
	 * @param model
	 *            the model
	 */
	public NewMasterPwFilePanel(final IModel<MasterPwFileModelBean> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
	}

	@Override
	protected void onInitializeComponents()
	{
		btnKeyFileChooser = new javax.swing.JButton();
		btnHelp = new javax.swing.JButton();
		btnOk = new javax.swing.JButton();
		btnCancel = new javax.swing.JButton();
		lblImageHeader = new javax.swing.JLabel();
		cbxMasterPw = new javax.swing.JCheckBox();
		cbxKeyFile = new javax.swing.JCheckBox();
		txtRepeatPw = new javax.swing.JPasswordField();
		btnMasterPw = new javax.swing.JButton();
		cmbKeyFile = new javax.swing.JComboBox<>();
		lblRepeatPw = new javax.swing.JLabel();
		txtMasterPw = new javax.swing.JPasswordField();
		btnCreateKeyFile = new javax.swing.JButton();
		lblApplicationFile = new javax.swing.JLabel();
		txtApplicationFile = new javax.swing.JTextField();
		btnApplicationFileChooser = new javax.swing.JButton();
		btnGeneratePw = new javax.swing.JButton();

		btnKeyFileChooser.setText("Browse..");

		btnHelp.setText("Help");

		btnOk.setText("OK");

		btnCancel.setText("Cancel");

		lblImageHeader.setText("Create Master Key");

		cbxMasterPw.setText("Master Password:");

		cbxKeyFile.setText("Key File:");

		txtRepeatPw.setText("");

		btnMasterPw.setText("***");

		lblRepeatPw.setText("Repeat Password:");

		txtMasterPw.setText("");

		btnCreateKeyFile.setText("Create key file...");

		lblApplicationFile.setText("Application File");

		btnApplicationFileChooser.setText("Browse...");

		btnGeneratePw.setText("Generate");
		// ===
		// ===
		// ===
		setPreferredSize(new java.awt.Dimension(840, 520));
		// allow JPasswordField to copy or cut
		txtMasterPw.putClientProperty("JPasswordField.cutCopyAllowed", true);
		txtRepeatPw.putClientProperty("JPasswordField.cutCopyAllowed", true);

		cbxMasterPw.addActionListener(this::onCheckMasterPw);
		cbxKeyFile.addActionListener(this::onCheckKeyFile);
		btnOk.addActionListener(this::onOk);
		btnCancel.addActionListener(this::onCancel);
		final MasterPwFileModelBean modelObject = getModelObject();
		btnOkStateMachine = BtnOkStateMachine.builder().component(btnOk).modelObject(modelObject)
			.currentState(BtnOkComponentStateEnum.DISABLED).build();
		txtMasterPw.setEnabled(false);
		txtMasterPw.getDocument().addDocumentListener(new DocumentListenerAdapter()
		{
			@Override
			public void onDocumentChanged(DocumentEvent documentEvent)
			{
				char[] password = txtMasterPw.getPassword();
				modelObject.setMasterPw(password);
				btnOkStateMachine.onChangeMasterPasswordLength(btnOkStateMachine);
			}
		});
		btnMasterPw.addActionListener(this::onShowMasterPw);

		cmbKeyFileModel = new GenericMutableComboBoxModel<String>(modelObject.getKeyFilePaths());
		cmbKeyFile.setModel(cmbKeyFileModel);
		IModel<String> selectedApplicationFilePathModel = LambdaModel.of(
			getModelObject()::getSelectedApplicationFilePath,
			getModelObject()::setSelectedApplicationFilePath);
		txtApplicationFile = new JMTextField();
		((JMTextField)txtApplicationFile).setPropertyModel(selectedApplicationFilePathModel);
		txtApplicationFile.setEnabled(false);
		btnApplicationFileChooser.addActionListener(this::onApplicationFileChooser);
		btnKeyFileChooser.addActionListener(this::onKeyFileChooser);

		File configDir = MysticCryptApplicationFrame.getInstance().getConfigurationDirectory();
		fileChooser = new JFileChooser(configDir);

		cmbKeyFileModel = new GenericMutableComboBoxModel<>(modelObject.getKeyFilePaths(),
			modelObject.getSelectedKeyFilePath());
		cmbKeyFile.setModel(cmbKeyFileModel);
		cmbKeyFile.addActionListener(this::onChangeCmbKeyFile);

		btnGeneratePw.addActionListener(this::onGeneratePassword);

		btnCreateKeyFile.addActionListener(this::onCreateKeyFile);
		btnHelp.addActionListener(this::onHelp);

		toggleMasterPwComponents();
		toggleKeyFileComponents();
	}

	protected void onHelp(ActionEvent actionEvent)
	{
		String helpLink = "https://github.com/astrapi69/mystic-crypt-ui/wiki/"
			+ "Help:-create-new-mystic-crypt-database";
		URL helpUrl = RuntimeExceptionDecorator.decorate(() -> new URL(helpLink));
		if (URLExtensions.isReachable(helpUrl))
		{
			BrowserControlExtensions.displayURLonStandardBrowser(this, helpLink);
		}
		else
		{
			HelpModelBean helpModelBean = HelpModelBean.builder()
				.title("Help for create new mystic-crypt database")
				.content("For create a new mystic-crypt database and encrypt your data \n"
					+ "you will need to define first the application file.\n\n"
					+ "Than you have to set your master key that consist of a password or a private key\n"
					+ "or a combination of both.\n"
					+ "If you want only with a password check the appropriate checkbox and leave \n"
					+ "the private key checkbox unchecked and set the password. "
					+ "\nIf you want only with a private key check the appropriate checkbox and leave\n"
					+ "the password checkbox unchecked and set the private key.\n"
					+ "\nIf you want a combination of both check both checkboxes \n"
					+ "and set the password and private key.\n\n"
					+ "After that you can save the mystic-crypt database by clicking ok.\n"
					+ "For all option above remember to copy the password or a private key or both \n"
					+ "and backup to save place. The same applies for the mystic-crypt database file.")
				.build();
			IModel<HelpModelBean> helpModel = BaseModel.of(helpModelBean);
			HelpDialog helpDialog = new HelpDialog(MysticCryptApplicationFrame.getInstance(),
				"Help for sign in to the your database", true, helpModel);
			helpDialog.setSize(800, 300);
			helpDialog.setVisible(true);
		}
	}

	protected void onCreateKeyFile(final ActionEvent actionEvent)
	{
		privateKeyModelBean = NewPrivateKeyModelBean.builder().build();
		NewPrivateKeyFileDialog dialog = new NewPrivateKeyFileDialog(
			MysticCryptApplicationFrame.getInstance(), "Create new private key", true,
			BaseModel.of(privateKeyModelBean))
		{
			@Override
			protected void onSave()
			{
				KeyModel privateKey = privateKeyModelBean.getPrivateKeyInfo();
				NewMasterPwFilePanel.this.getModelObject().setPrivateKeyInfo(privateKey);
				NewMasterPwFilePanel.this.getModelObject()
					.setKeyFileInfo(FileInfo.toFileInfo(privateKeyModelBean.getPrivateKeyFile()));
				NewMasterPwFilePanel.this.cmbKeyFileModel
					.addElement(privateKeyModelBean.getPrivateKeyFile().getAbsolutePath());
				NewMasterPwFilePanel.this.cmbKeyFileModel
					.setSelectedItem(privateKeyModelBean.getPrivateKeyFile().getAbsolutePath());
				super.onSave();
			}
		};
		dialog.setSize(950, 560);
		dialog.setVisible(true);
	}

	protected void onGeneratePassword(final ActionEvent actionEvent)
	{
		passwordModel = BaseModel.of(GeneratePasswordModelBean.builder().passwordLength(20)
			.lowercase(true).uppercase(true).digits(true).special(true).build());
		GeneratePasswordDialog dialog = new GeneratePasswordDialog(
			MysticCryptApplicationFrame.getInstance(), "Generate Password", true, passwordModel)
		{
			@Override
			protected void onOk()
			{
				char[] password = passwordModel.getObject().getPassword();
				NewMasterPwFilePanel.this.getModelObject().setMasterPw(password);
				txtMasterPw.setText(String.valueOf(password));
				txtRepeatPw.setText(String.valueOf(password));
				ClipboardExtensions.copyToClipboard(String.valueOf(password));
				super.onOk();
			}
		};
		dialog.setSize(600, 420);
		dialog.setVisible(true);
	}


	protected void onChangeCmbKeyFile(final ActionEvent actionEvent)
	{
		Object item = cmbKeyFile.getSelectedItem();
		String selectedKeyFilePath = (String)item;
		getModelObject().setSelectedKeyFilePath(selectedKeyFilePath);
		if (StringUtils.isEmpty(selectedKeyFilePath))
		{
			getModelObject().setKeyFileInfo(null);
			btnOkStateMachine.onSetKeyFile(btnOkStateMachine);
		}
		else
		{
			File selectedKeyFile = new File(selectedKeyFilePath);
			if (selectedKeyFile.exists())
			{
				getModelObject().setKeyFileInfo(FileInfo.toFileInfo(selectedKeyFile));
				btnOkStateMachine.onSetKeyFile(btnOkStateMachine);
			}
			if (!selectedKeyFile.exists())
			{
				getModelObject().setKeyFileInfo(null);
				cmbKeyFileModel.removeElement(selectedKeyFilePath);
				btnOkStateMachine.onSetKeyFile(btnOkStateMachine);
			}
		}
	}


	protected void onInitializeGroupLayout()
	{
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout
			.setHorizontalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout
						.createParallelGroup(
							javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
							layout.createSequentialGroup()
								.addGroup(
									layout
										.createParallelGroup(
											javax.swing.GroupLayout.Alignment.LEADING)
										.addComponent(lblImageHeader,
											javax.swing.GroupLayout.DEFAULT_SIZE,
											javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addGroup(layout.createSequentialGroup().addGroup(layout
											.createParallelGroup(
												javax.swing.GroupLayout.Alignment.LEADING)
											.addGroup(
												layout.createSequentialGroup().addGap(8, 516,
													Short.MAX_VALUE).addComponent(btnCreateKeyFile,
														javax.swing.GroupLayout.PREFERRED_SIZE, 163,
														javax.swing.GroupLayout.PREFERRED_SIZE)
													.addGap(18, 18, 18).addComponent(
														btnKeyFileChooser,
														javax.swing.GroupLayout.PREFERRED_SIZE, 111,
														javax.swing.GroupLayout.PREFERRED_SIZE))
											.addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
												layout
													.createSequentialGroup().addGap(0, 212,
														Short.MAX_VALUE)
													.addComponent(cmbKeyFile,
														javax.swing.GroupLayout.PREFERRED_SIZE, 596,
														javax.swing.GroupLayout.PREFERRED_SIZE))
											.addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
												layout.createSequentialGroup().addGroup(layout
													.createParallelGroup(
														javax.swing.GroupLayout.Alignment.LEADING)
													.addGroup(layout.createSequentialGroup()
														.addGap(12, 12, 12)
														.addGroup(layout.createParallelGroup(
															javax.swing.GroupLayout.Alignment.LEADING)
															.addComponent(cbxMasterPw,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																169,
																javax.swing.GroupLayout.PREFERRED_SIZE)
															.addComponent(lblApplicationFile,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																169,
																javax.swing.GroupLayout.PREFERRED_SIZE)
															.addComponent(cbxKeyFile,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																161,
																javax.swing.GroupLayout.PREFERRED_SIZE)))
													.addComponent(
														lblRepeatPw,
														javax.swing.GroupLayout.Alignment.TRAILING,
														javax.swing.GroupLayout.PREFERRED_SIZE, 134,
														javax.swing.GroupLayout.PREFERRED_SIZE))
													.addPreferredGap(
														javax.swing.LayoutStyle.ComponentPlacement.RELATED,
														31, Short.MAX_VALUE)
													.addGroup(layout.createParallelGroup(
														javax.swing.GroupLayout.Alignment.LEADING)
														.addComponent(txtApplicationFile,
															javax.swing.GroupLayout.Alignment.TRAILING,
															javax.swing.GroupLayout.PREFERRED_SIZE,
															483,
															javax.swing.GroupLayout.PREFERRED_SIZE)
														.addComponent(txtMasterPw,
															javax.swing.GroupLayout.Alignment.TRAILING,
															javax.swing.GroupLayout.PREFERRED_SIZE,
															483,
															javax.swing.GroupLayout.PREFERRED_SIZE)
														.addComponent(
															txtRepeatPw,
															javax.swing.GroupLayout.Alignment.TRAILING,
															javax.swing.GroupLayout.PREFERRED_SIZE,
															483,
															javax.swing.GroupLayout.PREFERRED_SIZE))
													.addPreferredGap(
														javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
													.addGroup(layout.createParallelGroup(
														javax.swing.GroupLayout.Alignment.TRAILING,
														false)
														.addComponent(btnGeneratePw,
															javax.swing.GroupLayout.DEFAULT_SIZE,
															101, Short.MAX_VALUE)
														.addComponent(btnMasterPw,
															javax.swing.GroupLayout.DEFAULT_SIZE,
															javax.swing.GroupLayout.DEFAULT_SIZE,
															Short.MAX_VALUE)
														.addComponent(btnApplicationFileChooser,
															javax.swing.GroupLayout.DEFAULT_SIZE,
															javax.swing.GroupLayout.DEFAULT_SIZE,
															Short.MAX_VALUE))))
											.addGap(12, 12, 12)))
								.addContainerGap())
						.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout
							.createSequentialGroup()
							.addComponent(btnHelp, javax.swing.GroupLayout.PREFERRED_SIZE, 122,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(btnOk, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
							.addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 141,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addGap(22, 22, 22)))));
		layout
			.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
					.addComponent(lblImageHeader, javax.swing.GroupLayout.PREFERRED_SIZE, 80,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(txtApplicationFile, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(btnApplicationFileChooser).addComponent(lblApplicationFile,
							javax.swing.GroupLayout.PREFERRED_SIZE, 35,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(10, 10, 10)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(btnMasterPw)
						.addComponent(txtMasterPw, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(cbxMasterPw))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblRepeatPw)
						.addComponent(txtRepeatPw, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(btnGeneratePw))
					.addGap(52, 52, 52)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(cmbKeyFile, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(cbxKeyFile))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(btnKeyFileChooser).addComponent(btnCreateKeyFile))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 81,
						Short.MAX_VALUE)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(btnOk).addComponent(btnCancel).addComponent(btnHelp))
					.addGap(42, 42, 42)));
	}

	private void setEchoChar()
	{
		if (getModelObject().isShowMasterPw())
		{
			getTxtMasterPw().setEchoChar('*');
			getTxtRepeatPw().setEchoChar('*');
		}
		else
		{
			char zero = 0;
			getTxtMasterPw().setEchoChar(zero);
			getTxtRepeatPw().setEchoChar(zero);
		}
	}

	protected void onShowMasterPw(final ActionEvent actionEvent)
	{
		setEchoChar();
		getModelObject().setShowMasterPw(!getModelObject().isShowMasterPw());
	}

	protected void onOk(ActionEvent actionEvent)
	{
		MasterPwFileModelBean modelObject = getModelObject();
		SignInType signInType = SignInType.toSignInType(modelObject);
		if (SignInType.PASSWORD_AND_PRIVATE_KEY.equals(signInType))
		{
			ApplicationXmlFileFactory.newApplicationFileWithPasswordAndPrivateKey(modelObject);
		}
		else if (SignInType.PRIVATE_KEY.equals(signInType))
		{
			ApplicationXmlFileFactory.newApplicationFileWithPrivateKey(modelObject);
		}
		else if (SignInType.PASSWORD.equals(signInType))
		{
			ApplicationXmlFileFactory.newApplicationFileWithPassword(modelObject);
		}
	}

	protected void onCancel(ActionEvent actionEvent)
	{
		System.err.println("onCancel method action called");
	}


	protected void onCheckKeyFile(final ActionEvent actionEvent)
	{
		toggleKeyFileComponents();
	}

	protected void toggleKeyFileComponents()
	{
		boolean cbxKeyFileSelected = cbxKeyFile.isSelected();
		getModelObject().setWithKeyFile(cbxKeyFileSelected);
		btnKeyFileChooser.setEnabled(cbxKeyFileSelected);
		btnCreateKeyFile.setEnabled(cbxKeyFileSelected);
		cmbKeyFile.setEnabled(cbxKeyFileSelected);
		btnOkStateMachine.onSetKeyFile(btnOkStateMachine);
	}

	protected void toggleMasterPwComponents()
	{
		boolean cbxMasterPwSelected = cbxMasterPw.isSelected();
		txtMasterPw.setEnabled(cbxMasterPwSelected);
		txtRepeatPw.setEnabled(cbxMasterPwSelected);
		btnMasterPw.setEnabled(cbxMasterPwSelected);
		btnGeneratePw.setEnabled(cbxMasterPwSelected);
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
		final int returnVal = fileChooser.showSaveDialog(NewMasterPwFilePanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File selectedApplicationFile = fileChooser.getSelectedFile();
			FileCreationState fileCreationState = RuntimeExceptionDecorator
				.decorate(() -> FileFactory.newFile(selectedApplicationFile));
			String absolutePath = selectedApplicationFile.getAbsolutePath();
			getModelObject().setApplicationFileInfo(FileInfo.toFileInfo(selectedApplicationFile));
			getModelObject().setSelectedApplicationFilePath(absolutePath);
			txtApplicationFile.setText(absolutePath);
			btnOkStateMachine.onApplicationFileAdded(btnOkStateMachine);
			toggleApplicationFileComponents();
		}
	}

	protected void onKeyFileChooser(ActionEvent actionEvent)
	{
		System.err.println("onKeyFileChooser method action called");
		final int returnVal = fileChooser.showSaveDialog(NewMasterPwFilePanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File selectedKeyFile = fileChooser.getSelectedFile();
			String absolutePath = selectedKeyFile.getAbsolutePath();
			cmbKeyFileModel.addElement(absolutePath);
			cmbKeyFileModel.setSelectedItem(absolutePath);
			getModelObject().setSelectedKeyFilePath(absolutePath);
			getModelObject().setKeyFileInfo(FileInfo.toFileInfo(selectedKeyFile));
			btnOkStateMachine.onSetKeyFile(btnOkStateMachine);
		}
	}

}
