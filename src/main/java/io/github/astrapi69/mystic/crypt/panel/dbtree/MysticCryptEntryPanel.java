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

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;

import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import lombok.Getter;

import com.github.lgooddatepicker.components.CalendarPanel;
import com.github.lgooddatepicker.components.DatePicker;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.component.JMCheckBox;
import io.github.astrapi69.swing.component.JMPasswordField;
import io.github.astrapi69.swing.component.JMTextArea;
import io.github.astrapi69.swing.component.JMTextField;
import io.github.astrapi69.swing.component.ComponentExtensions;

@Getter
public class MysticCryptEntryPanel extends BasePanel<MysticCryptEntryModelBean>
{

	private JLabel lblEntryName;
	private JLabel lblHeader;
	private JLabel lblNotes;
	private JLabel lblPassword;
	private JLabel lblRepeat;
	private JLabel lblUrl;
	private JLabel lblUsername;
	private JScrollPane srcNotes;
	private JMTextField txtEntryName;
	private JMTextArea txtNotes;
	private JMPasswordField txtPassword;
	private JMPasswordField txtRepeat;
	private JMTextField txtUrl;
	private JMTextField txtUsername;

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

		lblHeader = new JLabel();
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

		lblHeader.setText("Create a new database entry");

		lblEntryName.setText("Entry name");

		lblUsername.setText("Username");

		lblPassword.setText("Password");

		lblRepeat.setText("Repeat");

		lblUrl.setText("Url");

		lblNotes.setText("Notes");

		txtPassword.setText("");

		txtNotes.setColumns(20);
		txtNotes.setRows(5);
		srcNotes.setViewportView(txtNotes);
		cbxExpirable.setText("Expires");

		cbxExpirable
			.setPropertyModel(LambdaModel.of(modelObject::isExpirable, modelObject::setExpirable));

		cbxExpirable.addActionListener(this::onChangeExpirable);

		if(getModelObject().isExpirable() && getModelObject().getExpires() != null) {
			txtExpires.setSelectedDate(getModelObject().getExpires());
		}
		ComponentExtensions.setComponentEnabled(txtExpires, getModelObject().isExpirable());
	}

	protected void onChangeExpirable(final ActionEvent actionEvent)
	{
		ComponentExtensions.setComponentEnabled(txtExpires, getModelObject().isExpirable());
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();

		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addComponent(lblEntryName, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18).addComponent(txtEntryName))
				.addGroup(layout.createSequentialGroup()
					.addComponent(lblUsername, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18).addComponent(txtUsername))
				.addGroup(layout.createSequentialGroup()
					.addComponent(lblPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18).addComponent(txtPassword))
				.addGroup(layout.createSequentialGroup()
					.addComponent(lblRepeat, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18).addComponent(txtRepeat))
				.addGroup(layout.createSequentialGroup()
					.addComponent(lblNotes, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18).addComponent(srcNotes))
				.addGroup(layout.createSequentialGroup()
					.addComponent(lblUrl, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18).addComponent(txtUrl))
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup()
						.addComponent(lblHeader, javax.swing.GroupLayout.PREFERRED_SIZE, 760,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addGap(0, 0, Short.MAX_VALUE))
					.addGroup(layout.createSequentialGroup().addComponent(cbxExpirable)
						.addGap(86, 86, 86).addComponent(txtExpires))))
				.addContainerGap()));
		layout
			.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
					.addComponent(lblHeader, javax.swing.GroupLayout.PREFERRED_SIZE, 30,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblEntryName, javax.swing.GroupLayout.PREFERRED_SIZE, 28,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(txtEntryName, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblUsername, javax.swing.GroupLayout.PREFERRED_SIZE, 28,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(txtUsername, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 28,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblRepeat, javax.swing.GroupLayout.PREFERRED_SIZE, 28,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(txtRepeat, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblUrl, javax.swing.GroupLayout.PREFERRED_SIZE, 28,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(txtUrl, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(lblNotes, javax.swing.GroupLayout.PREFERRED_SIZE, 28,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(srcNotes, javax.swing.GroupLayout.PREFERRED_SIZE, 280,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(txtExpires, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(cbxExpirable))
					.addContainerGap(23, Short.MAX_VALUE)));
	}
}
