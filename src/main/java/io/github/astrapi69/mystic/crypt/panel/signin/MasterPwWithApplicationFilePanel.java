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
package io.github.astrapi69.mystic.crypt.panel.signin;

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;
import java.util.logging.Level;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

import lombok.Getter;
import lombok.extern.java.Log;
import io.github.astrapi69.browser.BrowserControlExtensions;
import io.github.astrapi69.file.search.PathFinder;
import io.github.astrapi69.file.system.SystemFileExtensions;
import io.github.astrapi69.gson.ObjectToJsonFileExtensions;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.net.url.URLExtensions;
import io.github.astrapi69.swing.component.JMCheckBox;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.combobox.model.StringMutableComboBoxModel;
import io.github.astrapi69.swing.dialog.help.HelpDialog;
import io.github.astrapi69.swing.listener.document.DocumentListenerAdapter;
import io.github.astrapi69.swing.panel.help.HelpModelBean;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;

/**
 * The class {@link MasterPwFilePanel}
 */
@Getter
@Log
public class MasterPwWithApplicationFilePanel extends BasePanel<MasterPwFileModelBean>
{

	private static final long serialVersionUID = 1L;
	BtnOkStateMachine btnOkStateMachine;
	StringMutableComboBoxModel cmbKeyFileModel;
	StringMutableComboBoxModel cmbApplicationFileModel;
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

	/**
	 * Instantiates a new {@link MasterPwWithApplicationFilePanel}
	 */
	public MasterPwWithApplicationFilePanel()
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
	public MasterPwWithApplicationFilePanel(final IModel<MasterPwFileModelBean> model)
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
		// allow pwfield to copy or cut

		cbxMasterPw = new JMCheckBox();
		((JMCheckBox)cbxMasterPw).setPropertyModel(
			LambdaModel.of(getModelObject()::isWithMasterPw, getModelObject()::setWithMasterPw));

		cbxKeyFile = new JMCheckBox();
		((JMCheckBox)cbxKeyFile).setPropertyModel(
			LambdaModel.of(getModelObject()::isWithKeyFile, getModelObject()::setWithKeyFile));

		txtMasterPw.putClientProperty("JPasswordField.cutCopyAllowed", true);

		cbxMasterPw.addActionListener(this::onCheckMasterPw);
		cbxKeyFile.addActionListener(this::onCheckKeyFile);
		btnMasterPw.addActionListener(this::onShowMasterPw);
		btnKeyFileChooser.addActionListener(this::onKeyFileChooser);
		btnOk.addActionListener(this::onOk);
		btnCancel.addActionListener(this::onCancel);

		MasterPwFileModelBean modelObject = getModelObject();
		btnOkStateMachine = BtnOkStateMachine.builder().component(btnOk).modelObject(modelObject)
			.currentState(BtnOkComponentStateEnum.DISABLED).build();
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
		String selectedKeyFilePath = modelObject.getSelectedKeyFilePath();

		cmbKeyFileModel = new StringMutableComboBoxModel(modelObject.getKeyFilePaths(),
			selectedKeyFilePath);
		cmbKeyFile.setModel(cmbKeyFileModel);
		cmbKeyFile.setSelectedItem(selectedKeyFilePath);
		if (selectedKeyFilePath != null && modelObject.getKeyFile() == null)
		{
			File kf = new File(selectedKeyFilePath);
			if (kf.exists())
			{
				modelObject.setKeyFile(kf);
			}
		}
		cmbKeyFile.addActionListener(this::onChangeCmbKeyFile);
		String selectedApplicationFilePath = modelObject.getSelectedApplicationFilePath();
		if (!modelObject.getApplicationFilePaths().contains(selectedApplicationFilePath))
		{
			modelObject.getApplicationFilePaths().add(selectedApplicationFilePath);
		}
		cmbApplicationFileModel = new StringMutableComboBoxModel(
			modelObject.getApplicationFilePaths(), selectedApplicationFilePath);
		cmbApplicationFile.setModel(cmbApplicationFileModel);
		cmbApplicationFile.setSelectedItem(selectedApplicationFilePath);
		if (selectedApplicationFilePath != null && modelObject.getApplicationFile() == null)
		{
			File saf = new File(selectedApplicationFilePath);
			if (saf.exists())
			{
				modelObject.setApplicationFile(saf);
			}
		}
		cmbApplicationFile.addActionListener(this::onChangeCmbApplicationFile);
		btnHelp.addActionListener(this::onHelp);

		toggleMasterPwComponents();
		toggleKeyFileComponents();
	}

	protected void onHelp(ActionEvent actionEvent)
	{
		String helpLink = "https://github.com/astrapi69/mystic-crypt-ui/wiki/"
			+ "Help:-open-existing-mystic-crypt-database";
		URL helpUrl = RuntimeExceptionDecorator.decorate(() -> new URL(helpLink));
		if (URLExtensions.isReachable(helpUrl))
		{
			BrowserControlExtensions.displayURLonStandardBrowser(this, helpLink);
		}
		else
		{
			HelpModelBean helpModelBean = HelpModelBean.builder().title("Help for authentication")
				.content("Your mystic-crypt database is an encrypted file.\n\n"
					+ "For open you will need your master key.\n"
					+ "You master key consist either from a password or a private key or "
					+ "both.\n\n"
					+ "The master key is set by the creation of the mystic-crypt database.\n"
					+ "If you desided by creation only with password then enter only your password\n"
					+ "If you desided by creation only with private key then select only the private key\n"
					+ "If you desided by creation a combination of password and private key enter \n"
					+ "your password and select your private key\n"
					+ "After you set the master key you can open your mystic-crypt database by clicking ok.")
				.build();
			IModel<HelpModelBean> helpModel = BaseModel.of(helpModelBean);
			HelpDialog helpDialog = new HelpDialog(MysticCryptApplicationFrame.getInstance(),
				"Help for sign in to the your database", true, helpModel);
			helpDialog.setSize(800, 300);
			helpDialog.setVisible(true);
		}
	}

	protected void onChangeCmbApplicationFile(final ActionEvent actionEvent)
	{
		Object item = cmbApplicationFile.getSelectedItem();
		String selectedApplicationFilePath = (String)item;
		getModelObject().setSelectedApplicationFilePath(selectedApplicationFilePath);
		if (selectedApplicationFilePath != null && selectedApplicationFilePath.isEmpty())
		{
			getModelObject().setApplicationFile(null);
			btnOkStateMachine.onApplicationFileAdded(btnOkStateMachine);
		}
		else
		{
			File selectedApplicationFile = new File(selectedApplicationFilePath);
			if (selectedApplicationFile.exists())
			{
				getModelObject().setApplicationFile(selectedApplicationFile);
				if (!getModelObject().getApplicationFilePaths()
					.contains(selectedApplicationFile.getAbsolutePath()))
				{
					getModelObject().getApplicationFilePaths()
						.add(selectedApplicationFile.getAbsolutePath());
				}
				btnOkStateMachine.onApplicationFileAdded(btnOkStateMachine);
			}
			else if (!selectedApplicationFile.exists())
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
		if (selectedKeyFilePath.isEmpty())
		{
			getModelObject().setKeyFile(null);
			btnOkStateMachine.onSetKeyFile(btnOkStateMachine);
		}
		else
		{
			File selectedKeyFile = new File(selectedKeyFilePath);
			if (selectedKeyFile.exists())
			{
				getModelObject().setKeyFile(selectedKeyFile);
				if (!getModelObject().getKeyFilePaths().contains(selectedKeyFile.getAbsolutePath()))
				{
					getModelObject().getKeyFilePaths().add(selectedKeyFile.getAbsolutePath());
				}
				btnOkStateMachine.onSetKeyFile(btnOkStateMachine);
			}
			else if (!selectedKeyFile.exists())
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
		try
		{
			MasterPwFileModelBean modelObject = getModelObject();
			ApplicationModelBean applicationModelBean = ApplicationFileReader.read(modelObject);
			MysticCryptApplicationFrame.getInstance().setModelObject(applicationModelBean);
			MasterPwFileModelBean masterPwFileModelBean = applicationModelBean
				.getMasterPwFileModelBean();
			MemoizedSigninModelBean memoizedSigninModelBean = masterPwFileModelBean
				.toMemoizedSigninModelBean(modelObject);
			File memoizedSigninFile = new File(
				MysticCryptApplicationFrame.getInstance().getConfigurationDirectory(),
				MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME);
			ObjectToJsonFileExtensions.toJsonFile(memoizedSigninModelBean, memoizedSigninFile);
		}
		catch (Exception exception)
		{
			String exceptionMessage = exception.getMessage();
			if (exceptionMessage.contains("::"))
			{
				String[] split = exceptionMessage.split("::");
				String title = split[0];
				String htmlMessage = split[1];
				JOptionPane.showMessageDialog(this, htmlMessage, title, JOptionPane.ERROR_MESSAGE);
				log.log(Level.SEVERE, exception.getMessage(), exception);
			}
			else
			{
				String title = exceptionMessage;
				String localizedMessage = exception.getLocalizedMessage();
				JOptionPane.showMessageDialog(this, localizedMessage, title,
					JOptionPane.ERROR_MESSAGE);
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
