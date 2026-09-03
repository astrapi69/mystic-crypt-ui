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

import javax.swing.*;
import javax.swing.event.DocumentEvent;

import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.file.system.SystemFileExtensions;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.listener.document.DocumentListenerAdapter;
import io.github.astrapi69.swing.model.component.JMCheckBox;
import io.github.astrapi69.swing.model.component.JMPasswordField;
import io.github.astrapi69.swing.model.component.JMTextField;
import lombok.Getter;

/**
 * The class {@link MasterPwFilePanel} asks for the master key of a database: a master password, a
 * key file, or both of them.
 * <p>
 * Every input component is bound to the {@link MasterPwFileModelBean} of this panel, so what was
 * typed or checked is readable from the model at any moment. The OK button and the enabled state it
 * is kept in ask the model, never a widget - that keeps the answer the same for a test, for a
 * dialog around this panel and for the panel itself.
 */
@Getter
public class MasterPwFilePanel extends BasePanel<MasterPwFileModelBean>
{

	private static final long serialVersionUID = 1L;

	private JButton btnCancel;
	private JButton btnHelp;
	private JButton btnKeyFileChooser;
	private JButton btnMasterPw;
	private JButton btnOk;
	private JMCheckBox cbxKeyFile;
	private JMCheckBox cbxMasterPw;
	private JLabel lblImageHeader;
	private JMTextField txtKeyFile;
	private JMPasswordField txtMasterPw;
	private JFileChooser fileChooser;

	/**
	 * Instantiates a new {@link MasterPwFilePanel}
	 */
	public MasterPwFilePanel()
	{
		this(BaseModel.<MasterPwFileModelBean> of(MasterPwFileModelBean.builder().build()));
	}

	/**
	 * Instantiates a new {@link MasterPwFilePanel}
	 *
	 * @param model
	 *            the model
	 */
	public MasterPwFilePanel(final IModel<MasterPwFileModelBean> model)
	{
		super(model);
	}

	protected void onCheckKeyFile(final ActionEvent actionEvent)
	{
		// the check box has written its state into the model already; writing it again keeps this
		// handler independent of the order in which a check box notifies its listeners
		getModelObject().setWithKeyFile(cbxKeyFile.isSelected());
		toggleKeyFileComponents(getModelObject().isWithKeyFile());
	}

	protected void toggleKeyFileComponents(boolean withKeyFile)
	{
		txtKeyFile.setEnabled(withKeyFile);
		if (!withKeyFile)
		{
			txtKeyFile.setText("");
			getModelObject().setKeyFileInfo(null);
		}
		btnKeyFileChooser.setEnabled(withKeyFile);
	}

	protected void toggleMasterPwComponents(boolean withMasterPw)
	{
		cbxMasterPw.setSelected(withMasterPw);
		txtMasterPw.setEnabled(withMasterPw);
		if (!withMasterPw)
		{
			txtMasterPw.setText("");
			getModelObject().setMasterPw(null);
		}
		btnMasterPw.setEnabled(withMasterPw);
		btnOk.getModel().setEnabled(getBtnOkEnabledState());
	}

	protected void onCheckMasterPw(final ActionEvent actionEvent)
	{
		// the check box has written its state into the model already; writing it again keeps this
		// handler independent of the order in which a check box notifies its listeners
		getModelObject().setWithMasterPw(cbxMasterPw.isSelected());
		toggleMasterPwComponents(getModelObject().isWithMasterPw());
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
		lblImageHeader = new JLabel();
		cbxMasterPw = new JMCheckBox();
		cbxKeyFile = new JMCheckBox();
		txtMasterPw = new JMPasswordField();
		btnMasterPw = new JButton();
		txtKeyFile = new JMTextField();
		btnKeyFileChooser = new JButton();
		btnHelp = new JButton();
		btnOk = new JButton();
		btnCancel = new JButton();
		bindComponents();
		toggleMasterPwComponents(getModelObject().isWithMasterPw());
		toggleKeyFileComponents(getModelObject().isWithKeyFile());

		setPreferredSize(new java.awt.Dimension(820, 380));

		lblImageHeader.setText("Enter Master Key");

		cbxMasterPw.setText("Master Password:");
		cbxMasterPw.addActionListener(this::onCheckMasterPw);

		cbxKeyFile.setText("Key File:");
		cbxKeyFile.addActionListener(this::onCheckKeyFile);

		txtMasterPw.setText("");
		txtMasterPw.getDocument().addDocumentListener(new DocumentListenerAdapter()
		{
			@Override
			public void onDocumentChanged(DocumentEvent e)
			{
				// the model first, the enabled state from the model afterwards: a document
				// notifies its listeners in reverse registration order, so this listener runs
				// before the one the password field keeps for its own property model
				getModelObject().setMasterPw(txtMasterPw.getPassword());
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

		fileChooser = new JFileChooser(SystemFileExtensions.getUserDownloadsDir());

		cbxMasterPw.setToolTipText(Messages.getString("signin.tooltip.master.password.checkbox",
			"unlock using a master password"));
		cbxKeyFile.setToolTipText(
			Messages.getString("signin.tooltip.key.file.checkbox", "unlock using a key file"));
		txtMasterPw.setToolTipText(
			Messages.getString("signin.tooltip.master.password", "the master password"));
		btnMasterPw.setToolTipText(Messages.getString("signin.tooltip.show.hide.master.password",
			"show or hide the typed password"));
		txtKeyFile.setToolTipText(
			Messages.getString("signin.tooltip.key.file", "the key file chosen to unlock with"));
		btnKeyFileChooser.setToolTipText(
			Messages.getString("signin.tooltip.key.file.chooser", "choose the key file"));
	}

	/**
	 * Binds every input component to the model bean of this panel, so that each edit lands in the
	 * model and the model is what the buttons read - the components carry the values the model
	 * already holds
	 */
	protected void bindComponents()
	{
		cbxMasterPw.setPropertyModel(LambdaModel.of(() -> getModelObject().isWithMasterPw(),
			(Boolean withMasterPw) -> getModelObject().setWithMasterPw(withMasterPw)));
		cbxKeyFile.setPropertyModel(LambdaModel.of(() -> getModelObject().isWithKeyFile(),
			(Boolean withKeyFile) -> getModelObject().setWithKeyFile(withKeyFile)));
		txtMasterPw.setPropertyModel(LambdaModel.of(() -> getModelObject().getMasterPw(),
			(char[] masterPw) -> getModelObject().setMasterPw(masterPw)));
		txtKeyFile.setPropertyModel(LambdaModel.of(() -> getModelObject().getSelectedKeyFilePath(),
			(String keyFilePath) -> getModelObject().setSelectedKeyFilePath(keyFilePath)));
	}

	protected void onKeyFileChooser(ActionEvent actionEvent)
	{
		final int returnVal = fileChooser.showSaveDialog(MasterPwFilePanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File selectedKeyFile = fileChooser.getSelectedFile();
			getModelObject().setKeyFileInfo(FileInfo.toFileInfo(selectedKeyFile));
			btnOk.getModel().setEnabled(getBtnOkEnabledState());
			txtKeyFile.setText(getModelObject().getKeyFileInfo().getName());
		}
	}

	/**
	 * Gets the enabled state the OK button has to be in for the master key the model currently
	 * holds
	 *
	 * @return true if the master key is complete, otherwise false
	 */
	protected boolean getBtnOkEnabledState()
	{
		MasterPwFileModelBean modelObject = getModelObject();
		char[] masterPw = modelObject.getMasterPw();
		int masterPwLength = masterPw != null ? masterPw.length : 0;
		if (modelObject.isWithMasterPw() && modelObject.isWithKeyFile()
			&& !(0 < masterPwLength && modelObject.getKeyFileInfo() != null))
		{
			return false;
		}
		if (!modelObject.isWithMasterPw() && !modelObject.isWithKeyFile())
		{
			return false;
		}
		if (modelObject.isWithMasterPw() && !modelObject.isWithKeyFile() && masterPwLength == 0)
		{
			return false;
		}
		if (!modelObject.isWithMasterPw() && modelObject.isWithKeyFile()
			&& modelObject.getKeyFileInfo() == null)
		{
			return false;
		}
		return true;
	}

	protected void onOk(ActionEvent actionEvent)
	{
	}

	protected void onCancel(ActionEvent actionEvent)
	{
	}

	protected void onInitializeGroupLayout()
	{
		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout
				.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(lblImageHeader, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
					Short.MAX_VALUE)
				.addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
						.addGroup(layout.createSequentialGroup()
							.addComponent(btnHelp, GroupLayout.PREFERRED_SIZE, 122,
								GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
								GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(btnOk, GroupLayout.PREFERRED_SIZE, 140,
								GroupLayout.PREFERRED_SIZE)
							.addGap(18, 18, 18).addComponent(btnCancel, GroupLayout.PREFERRED_SIZE,
								141, GroupLayout.PREFERRED_SIZE))
						.addGroup(layout.createSequentialGroup().addGroup(layout
							.createParallelGroup(GroupLayout.Alignment.LEADING, false)
							.addComponent(cbxMasterPw, GroupLayout.DEFAULT_SIZE, 201,
								Short.MAX_VALUE)
							.addComponent(cbxKeyFile, GroupLayout.DEFAULT_SIZE,
								GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
							.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
							.addGroup(layout
								.createParallelGroup(GroupLayout.Alignment.LEADING, false)
								.addComponent(txtMasterPw).addComponent(txtKeyFile,
									GroupLayout.PREFERRED_SIZE, 520, GroupLayout.PREFERRED_SIZE))
							.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(
								layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
									.addComponent(btnMasterPw, GroupLayout.DEFAULT_SIZE,
										GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
									.addComponent(btnKeyFileChooser, GroupLayout.PREFERRED_SIZE, 70,
										GroupLayout.PREFERRED_SIZE))))
					.addGap(12, 12, 12)))
				.addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap()
				.addComponent(
					lblImageHeader, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup().addComponent(cbxMasterPw)
						.addGap(18, 18, 18).addComponent(cbxKeyFile))
					.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
							.addComponent(txtMasterPw, GroupLayout.PREFERRED_SIZE,
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addComponent(btnMasterPw))
						.addGap(18, 18, 18)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
							.addComponent(txtKeyFile, GroupLayout.PREFERRED_SIZE,
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addComponent(btnKeyFileChooser))))
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 128, Short.MAX_VALUE)
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
