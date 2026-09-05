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
package io.github.astrapi69.mystic.crypt.panel.dbtree;

import java.awt.event.ActionEvent;

import javax.swing.*;

import com.github.lgooddatepicker.components.CalendarPanel;
import com.github.lgooddatepicker.components.DatePicker;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.panel.pw.GeneratePasswordDialog;
import io.github.astrapi69.mystic.crypt.panel.pw.GeneratePasswordModelBean;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.component.ComponentExtensions;
import io.github.astrapi69.swing.model.component.JMCheckBox;
import io.github.astrapi69.swing.model.component.JMPasswordField;
import io.github.astrapi69.swing.model.component.JMTextArea;
import io.github.astrapi69.swing.model.component.JMTextField;
import lombok.Getter;

@Getter
public class MysticCryptEntryPanel extends BasePanel<MysticCryptEntryModelBean>
{

	IModel<GeneratePasswordModelBean> passwordModel;
	private javax.swing.JButton btnGeneratePassword;
	private javax.swing.JButton btnShowPassword;
	private javax.swing.JLabel lblEntryName;
	private javax.swing.JLabel lblNotes;
	private javax.swing.JLabel lblPassword;
	private javax.swing.JLabel lblRepeat;
	private javax.swing.JLabel lblUrl;
	private javax.swing.JLabel lblUsername;
	private javax.swing.JLabel lblCreated;
	private javax.swing.JLabel lblLastAccessed;
	private javax.swing.JLabel lblLastModified;
	private javax.swing.JLabel lblPreciseExpiry;
	private javax.swing.JScrollPane srcNotes;
	private JMTextField txtEntryName;
	private JMTextArea txtNotes;
	private JMPasswordField txtPassword;
	private JMPasswordField txtRepeat;
	private JMTextField txtUrl;
	private JMTextField txtUsername;
	private javax.swing.JTextField txtCreated;
	private javax.swing.JTextField txtLastAccessed;
	private javax.swing.JTextField txtLastModified;
	private javax.swing.JTextField txtPreciseExpiry;
	private JMCheckBox cbxExpirable;
	private CalendarPanel txtExpires;

	public MysticCryptEntryPanel()
	{
		this(BaseModel.of(MysticCryptEntryModelBean.builder().build()));
	}

	public MysticCryptEntryPanel(final IModel<MysticCryptEntryModelBean> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		lblEntryName = new JLabel();
		lblUsername = new JLabel();
		lblPassword = new JLabel();
		lblRepeat = new JLabel();
		lblUrl = new JLabel();
		lblNotes = new JLabel();
		txtEntryName = new JMTextField();
		txtUsername = new JMTextField();
		txtPassword = new JMPasswordField();
		txtRepeat = new JMPasswordField();
		txtUrl = new JMTextField();
		srcNotes = new JScrollPane();
		txtNotes = new JMTextArea();

		cbxExpirable = new JMCheckBox();
		txtExpires = new CalendarPanel(new DatePicker());

		lblCreated = new JLabel();
		lblLastAccessed = new JLabel();
		lblLastModified = new JLabel();
		lblPreciseExpiry = new JLabel();
		txtCreated = new javax.swing.JTextField();
		txtLastAccessed = new javax.swing.JTextField();
		txtLastModified = new javax.swing.JTextField();
		txtPreciseExpiry = new javax.swing.JTextField();

		btnShowPassword = new javax.swing.JButton();
		btnGeneratePassword = new javax.swing.JButton();

		// stable component names for UI tests (AssertJ-Swing lookups by name)
		txtEntryName.setName("txtEntryName");
		txtUsername.setName("txtUsername");
		txtPassword.setName("txtPassword");
		txtRepeat.setName("txtRepeat");
		txtUrl.setName("txtUrl");
		txtNotes.setName("txtNotes");
		cbxExpirable.setName("cbxExpirable");
		btnShowPassword.setName("btnShowPassword");
		btnGeneratePassword.setName("btnGeneratePassword");
		txtCreated.setName("txtCreated");
		txtLastAccessed.setName("txtLastAccessed");
		txtLastModified.setName("txtLastModified");
		txtPreciseExpiry.setName("txtPreciseExpiry");

		MysticCryptEntryModelBean modelObject = getModelObject();
		// bind with model
		txtEntryName.setPropertyModel(LambdaModel.of(modelObject::getTitle, modelObject::setTitle));
		txtUsername
			.setPropertyModel(LambdaModel.of(modelObject::getUserName, modelObject::setUserName));
		txtPassword
			.setPropertyModel(LambdaModel.of(modelObject::getPassword, modelObject::setPassword));
		txtRepeat.setPropertyModel(LambdaModel.of(modelObject::getRepeat, modelObject::setRepeat));
		txtUrl.setPropertyModel(LambdaModel.of(modelObject::getUrl, modelObject::setUrl));
		txtNotes.setPropertyModel(LambdaModel.of(modelObject::getNotes, modelObject::setNotes));

		lblEntryName.setText("Entry name");

		lblUsername.setText("Username");

		lblPassword.setText("Password");

		lblRepeat.setText("Repeat");

		lblUrl.setText("Url");

		lblNotes.setText("Notes");

		txtNotes.setColumns(20);
		txtNotes.setRows(5);
		srcNotes.setViewportView(txtNotes);
		cbxExpirable.setText("Expires");

		btnShowPassword.setText("***");
		btnShowPassword.setToolTipText(Messages.getString("entry.tooltip.show.password",
			"shows or hides the typed password and its repetition"));

		btnShowPassword.addActionListener(this::onShowPassword);

		btnGeneratePassword.setText("Generate");
		btnGeneratePassword.addActionListener(this::onGeneratePassword);

		cbxExpirable
			.setPropertyModel(LambdaModel.of(modelObject::isExpirable, modelObject::setExpirable));
		cbxExpirable.setToolTipText(Messages.getString("entry.tooltip.expirable",
			"whether this entry has an expiry date - ticking it enables the date below"));
		txtExpires.setToolTipText(Messages.getString("entry.tooltip.expires",
			"the date this entry expires, only used while Expires is ticked"));

		cbxExpirable.addActionListener(this::onChangeExpirable);

		lblCreated.setText(Messages.getString("entry.label.created", "Created"));
		lblLastAccessed.setText(Messages.getString("entry.label.last.accessed", "Last accessed"));
		lblLastModified.setText(Messages.getString("entry.label.last.modified", "Last modified"));
		lblPreciseExpiry
			.setText(Messages.getString("entry.label.precise.expiry", "Precise expiry"));

		// these four are displays of imported facts, not fields the user fills: they are fed once
		// from the model and never write back, so they are deliberately NOT model backed
		// components.
		// A JMTextField here would claim a binding it cannot honour - a property model whose setter
		// does nothing - and ModelBinding rightly reports exactly that (see PanelsAreBoundTest)
		txtCreated.setText(EntryTimestampFormatter.format(modelObject.getCreationTime()));
		txtLastAccessed.setText(EntryTimestampFormatter.format(modelObject.getLastAccessTime()));
		txtLastModified
			.setText(EntryTimestampFormatter.format(modelObject.getLastModificationTime()));
		txtPreciseExpiry
			.setText(EntryTimestampFormatter.format(modelObject.getPreciseExpiryTime()));

		txtCreated.setEditable(false);
		txtLastAccessed.setEditable(false);
		txtLastModified.setEditable(false);
		txtPreciseExpiry.setEditable(false);

		txtCreated.setToolTipText(Messages.getString("entry.tooltip.created",
			"when this entry was created, if known (e.g. imported from a KeePass database)"));
		txtLastAccessed.setToolTipText(Messages.getString("entry.tooltip.last.accessed",
			"when this entry was last accessed, if known (e.g. imported from a KeePass database)"));
		txtLastModified.setToolTipText(Messages.getString("entry.tooltip.last.modified",
			"when this entry was last modified, if known (e.g. imported from a KeePass database)"));
		txtPreciseExpiry.setToolTipText(Messages.getString("entry.tooltip.precise.expiry",
			"the exact expiry timestamp from the imported source, including time of day and offset "
				+ "- the Expires date above only has day precision"));

		if (getModelObject().isExpirable() && getModelObject().getExpires() != null)
		{
			txtExpires.setSelectedDate(getModelObject().getExpires());
		}
		ComponentExtensions.setComponentEnabled(txtExpires, getModelObject().isExpirable());
	}

	protected void onShowPassword(final ActionEvent actionEvent)
	{
		setEchoChar();
		getModelObject().setShowPassword(!getModelObject().isShowPassword());
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
				MysticCryptEntryPanel.this.getModelObject().setPassword(password);
				txtPassword.setText(String.valueOf(password));
				txtRepeat.setText(String.valueOf(password));
				super.onOk();
			}
		};
		dialog.setSize(600, 420);
		dialog.setVisible(true);
	}

	private void setEchoChar()
	{
		if (getModelObject().isShowPassword())
		{
			getTxtPassword().setEchoChar('*');
			getTxtRepeat().setEchoChar('*');
		}
		else
		{
			char zero = 0;
			getTxtPassword().setEchoChar(zero);
			getTxtRepeat().setEchoChar(zero);
		}
	}

	protected void onChangeExpirable(final ActionEvent actionEvent)
	{
		ComponentExtensions.setComponentEnabled(txtExpires, getModelObject().isExpirable());
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
				.addGroup(layout
					.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
					.addComponent(lblUrl, javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(lblRepeat, javax.swing.GroupLayout.Alignment.LEADING,
						javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
						Short.MAX_VALUE)
					.addComponent(lblPassword, javax.swing.GroupLayout.Alignment.LEADING,
						javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
						Short.MAX_VALUE)
					.addComponent(lblUsername, javax.swing.GroupLayout.Alignment.LEADING,
						javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
						Short.MAX_VALUE)
					.addComponent(lblEntryName, javax.swing.GroupLayout.Alignment.LEADING,
						javax.swing.GroupLayout.DEFAULT_SIZE, 155, Short.MAX_VALUE))
				.addComponent(lblNotes, javax.swing.GroupLayout.DEFAULT_SIZE,
					javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addComponent(cbxExpirable, javax.swing.GroupLayout.DEFAULT_SIZE, 161,
					Short.MAX_VALUE)
				.addComponent(lblPreciseExpiry, javax.swing.GroupLayout.DEFAULT_SIZE,
					javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addComponent(lblCreated, javax.swing.GroupLayout.DEFAULT_SIZE,
					javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addComponent(lblLastAccessed, javax.swing.GroupLayout.DEFAULT_SIZE,
					javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addComponent(lblLastModified, javax.swing.GroupLayout.DEFAULT_SIZE,
					javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				.addGap(18, 18, 18)
				.addGroup(layout
					.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
					.addComponent(txtEntryName).addComponent(txtUsername)
					.addGroup(layout.createSequentialGroup()
						.addGroup(layout
							.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
							.addComponent(txtRepeat, javax.swing.GroupLayout.Alignment.LEADING,
								javax.swing.GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE)
							.addComponent(txtPassword, javax.swing.GroupLayout.Alignment.LEADING))
						.addGap(18, 18, 18)
						.addGroup(layout
							.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
							.addComponent(btnShowPassword, javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(btnGeneratePassword, javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
					.addComponent(txtUrl)
					.addComponent(srcNotes, javax.swing.GroupLayout.DEFAULT_SIZE, 530,
						Short.MAX_VALUE)
					.addComponent(txtExpires).addComponent(txtPreciseExpiry)
					.addComponent(txtCreated).addComponent(txtLastAccessed)
					.addComponent(txtLastModified))
				.addContainerGap(25, Short.MAX_VALUE)));
		layout
			.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addGap(29, 29, 29)
					.addGroup(layout
						.createParallelGroup(
							javax.swing.GroupLayout.Alignment.TRAILING)
						.addGroup(
							layout.createSequentialGroup()
								.addGroup(
									layout
										.createParallelGroup(
											javax.swing.GroupLayout.Alignment.TRAILING)
										.addGroup(layout.createSequentialGroup().addGroup(layout
											.createParallelGroup(
												javax.swing.GroupLayout.Alignment.BASELINE)
											.addComponent(lblEntryName).addComponent(txtEntryName,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE))
											.addGap(18, 18, 18).addComponent(lblUsername))
										.addComponent(txtUsername,
											javax.swing.GroupLayout.PREFERRED_SIZE,
											javax.swing.GroupLayout.DEFAULT_SIZE,
											javax.swing.GroupLayout.PREFERRED_SIZE))
								.addGap(18, 18, 18)
								.addGroup(layout
									.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
									.addComponent(lblPassword)
									.addComponent(txtPassword,
										javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.PREFERRED_SIZE)
									.addComponent(btnShowPassword))
								.addGap(21, 21, 21).addComponent(lblRepeat))
						.addGroup(
							layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(txtRepeat, javax.swing.GroupLayout.PREFERRED_SIZE,
									javax.swing.GroupLayout.DEFAULT_SIZE,
									javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(btnGeneratePassword)))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblUrl).addComponent(txtUrl,
							javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(lblNotes).addComponent(srcNotes,
							javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(21, 21, 21)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(cbxExpirable).addComponent(txtExpires,
							javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblPreciseExpiry).addComponent(txtPreciseExpiry,
							javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblCreated).addComponent(txtCreated,
							javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblLastAccessed).addComponent(txtLastAccessed,
							javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblLastModified).addComponent(txtLastModified,
							javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addContainerGap(20, Short.MAX_VALUE)));
	}
}
