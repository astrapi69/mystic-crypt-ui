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
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package io.github.astrapi69.mystic.crypt.panel.properties;

import lombok.Getter;
import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.component.JMTextField;

@Getter
public class PropertiesNewEntryPanel extends BasePanel<KeyValuePair<String, String>>
{

	private javax.swing.JLabel lblCreateNewEntry;
	private javax.swing.JLabel lblKey;
	private javax.swing.JLabel lblValue;
	private JMTextField txtKey;
	private JMTextField txtValue;

	public PropertiesNewEntryPanel()
	{
		this(BaseModel.of(KeyValuePair.<String, String> builder().build()));
	}

	public PropertiesNewEntryPanel(final IModel<KeyValuePair<String, String>> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
		lblKey = new javax.swing.JLabel();
		lblValue = new javax.swing.JLabel();
		txtKey = new JMTextField();
		txtValue = new JMTextField();
		lblCreateNewEntry = new javax.swing.JLabel();

		lblKey.setText("Key");

		lblValue.setText("Value");

		lblCreateNewEntry.setText("Create new entry");
		// bind with model
		KeyValuePair<String, String> modelObject = getModelObject();
		txtKey.setPropertyModel(LambdaModel.of(modelObject::getKey, modelObject::setKey));
		txtValue.setPropertyModel(LambdaModel.of(modelObject::getValue, modelObject::setValue));
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
	}

	protected void onInitializeGroupLayout()
	{
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout
					.createParallelGroup(
						javax.swing.GroupLayout.Alignment.LEADING)
					.addGroup(
						layout.createSequentialGroup()
							.addGroup(layout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
								.addComponent(lblValue, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
									javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(lblKey, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
									javax.swing.GroupLayout.PREFERRED_SIZE))
							.addGap(18, 18, 18)
							.addGroup(layout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
								.addComponent(txtKey, javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(txtValue, javax.swing.GroupLayout.Alignment.LEADING,
									javax.swing.GroupLayout.DEFAULT_SIZE, 363, Short.MAX_VALUE))
							.addGap(20, 20, 20))
					.addGroup(layout.createSequentialGroup()
						.addComponent(lblCreateNewEntry, javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addGap(401, 401, 401)))));
		layout
			.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
					.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(lblCreateNewEntry).addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(txtKey, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(lblKey, javax.swing.GroupLayout.PREFERRED_SIZE, 34,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblValue, javax.swing.GroupLayout.PREFERRED_SIZE, 34,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(txtValue, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addContainerGap()));
	}

}
