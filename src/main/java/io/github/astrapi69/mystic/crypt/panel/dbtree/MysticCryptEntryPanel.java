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

import javax.swing.*;

import io.github.astrapi69.swing.component.JMTextField;
import lombok.Getter;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.swing.base.BasePanel;

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
	private JTextField txtEntryName;
	private JTextArea txtNotes;
	private JPasswordField txtPassword;
	private JPasswordField txtRepeat;
	private JTextField txtUrl;
	private JTextField txtUsername;

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
		txtPassword = new JPasswordField();
		txtRepeat = new JPasswordField();
		txtUrl = new JTextField();
		srcNotes = new JScrollPane();
		txtNotes = new JTextArea();

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
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();

		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout
				.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addComponent(lblEntryName, GroupLayout.PREFERRED_SIZE, 140,
						GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18).addComponent(txtEntryName))
				.addGroup(layout.createSequentialGroup()
					.addComponent(lblUsername, GroupLayout.PREFERRED_SIZE, 140,
						GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18).addComponent(txtUsername))
				.addGroup(layout.createSequentialGroup()
					.addComponent(lblPassword, GroupLayout.PREFERRED_SIZE, 140,
						GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18).addComponent(txtPassword))
				.addGroup(layout.createSequentialGroup()
					.addComponent(lblRepeat, GroupLayout.PREFERRED_SIZE, 140,
						GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18).addComponent(txtRepeat))
				.addGroup(layout.createSequentialGroup()
					.addComponent(lblNotes, GroupLayout.PREFERRED_SIZE, 140,
						GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18).addComponent(srcNotes))
				.addGroup(layout.createSequentialGroup()
					.addComponent(lblHeader, GroupLayout.PREFERRED_SIZE, 760,
						GroupLayout.PREFERRED_SIZE)
					.addGap(0, 0, Short.MAX_VALUE))
				.addGroup(layout.createSequentialGroup()
					.addComponent(lblUrl, GroupLayout.PREFERRED_SIZE, 140,
						GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18).addComponent(txtUrl)))
				.addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap()
				.addComponent(lblHeader, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(lblEntryName, GroupLayout.PREFERRED_SIZE, 28,
						GroupLayout.PREFERRED_SIZE)
					.addComponent(txtEntryName, GroupLayout.PREFERRED_SIZE,
						GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(lblUsername, GroupLayout.PREFERRED_SIZE, 28,
						GroupLayout.PREFERRED_SIZE)
					.addComponent(txtUsername, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE))
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(lblPassword, GroupLayout.PREFERRED_SIZE, 28,
						GroupLayout.PREFERRED_SIZE)
					.addComponent(txtPassword, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE))
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(lblRepeat, GroupLayout.PREFERRED_SIZE, 28,
						GroupLayout.PREFERRED_SIZE)
					.addComponent(txtRepeat, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE))
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(lblUrl, GroupLayout.PREFERRED_SIZE, 28,
						GroupLayout.PREFERRED_SIZE)
					.addComponent(txtUrl, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE))
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(lblNotes, GroupLayout.PREFERRED_SIZE, 28,
						GroupLayout.PREFERRED_SIZE)
					.addComponent(srcNotes, GroupLayout.PREFERRED_SIZE, 280,
						GroupLayout.PREFERRED_SIZE))
				.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
	}
}
