/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.astrapi69.mystic.crypt.panels.signin;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import io.github.astrapi69.create.FileCreationState;
import io.github.astrapi69.create.FileFactory;
import io.github.astrapi69.design.pattern.state.button.BtnOkComponentStateEnum;
import io.github.astrapi69.design.pattern.state.button.BtnOkStateMachine;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.Model;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.random.object.RandomStringFactory;
import io.github.astrapi69.search.PathFinder;
import io.github.astrapi69.swing.adapters.DocumentListenerAdapter;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.system.SystemFileExtensions;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;

/**
 *
 * @author astrapi69
 */
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
	StringMutableComboBoxModel cmbKeyFileModel;
	BtnOkStateMachine btnOkStateMachine;

	/**
	 * Creates new form NewMasterPwFileFormPanel
	 */
	public NewMasterPwFilePanel()
	{
		this(BaseModel
			.<MasterPwFileModelBean> of(MasterPwFileModelBean.builder().minPasswordLength(6)
				.withKeyFile(false).withMasterPw(false).showMasterPw(false).build()));
	}

	/**
	 * Instantiates a new {@link MasterPwWithApplicationFilePanel}
	 *
	 * @param model
	 *            the model
	 */
	public NewMasterPwFilePanel(final Model<MasterPwFileModelBean> model)
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

		cbxMasterPw.addActionListener(this::onCheckMasterPw);
		cbxKeyFile.addActionListener(this::onCheckKeyFile);
		btnOk.addActionListener(this::onOk);
		btnCancel.addActionListener(this::onCancel);
		final MasterPwFileModelBean modelObject = getModelObject();
		btnOkStateMachine = BtnOkStateMachine.builder().component(btnOk).modelObject(modelObject)
			.current(BtnOkComponentStateEnum.DISABLED).build();
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

		cmbKeyFileModel = new StringMutableComboBoxModel(modelObject.getKeyFilePaths());
		cmbKeyFile.setModel(cmbKeyFileModel);

		txtApplicationFile.setText(modelObject.getSelectedKeyFilePath() != null
			? modelObject.getSelectedKeyFilePath()
			: "");
		txtApplicationFile.setEnabled(false);
		btnApplicationFileChooser.addActionListener(this::onApplicationFileChooser);
		btnKeyFileChooser.addActionListener(this::onKeyFileChooser);

		File configDir = PathFinder.getRelativePath(SystemFileExtensions.getUserHomeDir(),
			MysticCryptApplicationFrame.DEFAULT_USER_CONFIGURATION_DIRECTORY_NAME,
			MysticCryptApplicationFrame.APPLICATION_NAME);
		fileChooser = new JFileChooser(configDir);

		cmbKeyFileModel = new StringMutableComboBoxModel(modelObject.getKeyFilePaths(),
			modelObject.getSelectedKeyFilePath());
		cmbKeyFile.setModel(cmbKeyFileModel);
		cmbKeyFile.addActionListener(this::onChangeCmbKeyFile);

		btnGeneratePw.addActionListener(this::onGeneratePassword);

		btnCreateKeyFile.addActionListener(this::onCreateKeyFile);

		toggleMasterPwComponents();
		toggleKeyFileComponents();
	}

	protected void onCreateKeyFile(final ActionEvent actionEvent)
	{
		// TODO create a key file dialog
	}

	protected void onGeneratePassword(final ActionEvent actionEvent)
	{
		String randomPassword = RandomStringFactory.newRandomLongString("abcdefghijklmnopqrstuvwxyz"
			+ "abcdefghijklmnopqrstuvwxyz".toUpperCase() + "0123456789", 15);
		txtMasterPw.setText(randomPassword);
		txtRepeatPw.setText(randomPassword);
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
			getModelObject().setApplicationFile(selectedApplicationFile);
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
			getModelObject().setKeyFile(selectedKeyFile);
			btnOkStateMachine.onSetKeyFile(btnOkStateMachine);
		}
	}

}
