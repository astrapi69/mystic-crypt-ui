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
package io.github.astrapi69.mystic.crypt.panel.pw;

import java.awt.event.ActionEvent;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.component.JMCheckBox;
import io.github.astrapi69.swing.component.JMSpinner;

/**
 *
 * @author astrapi69
 */
public class GeneratePasswordPanel extends BasePanel<GeneratePasswordModelBean>
{

	private javax.swing.JButton btnCancel;
	private javax.swing.JButton btnOk;
	private javax.swing.JCheckBox cbxBrackets;
	private javax.swing.JCheckBox cbxDigits;
	private javax.swing.JCheckBox cbxLowercase;
	private javax.swing.JCheckBox cbxMinus;
	private javax.swing.JCheckBox cbxMoreSpecial;
	private javax.swing.JCheckBox cbxPlus;
	private javax.swing.JCheckBox cbxSpecial;
	private javax.swing.JCheckBox cbxUnderscore;
	private javax.swing.JCheckBox cbxUppercase;
	private javax.swing.JCheckBox cbxWhitespace;
	private javax.swing.JLabel lblGeneratePwHeader;
	private javax.swing.JLabel lblPasswordLength;
	private javax.swing.JSpinner spnPasswordLength;
	// ===
	// ===
	// ===

	public GeneratePasswordPanel()
	{

		this(BaseModel.<GeneratePasswordModelBean> of(GeneratePasswordModelBean.builder().build()));

	}

	public GeneratePasswordPanel(final IModel<GeneratePasswordModelBean> model)
	{
		super(model);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		lblGeneratePwHeader = new javax.swing.JLabel();
		spnPasswordLength = new javax.swing.JSpinner();
		lblPasswordLength = new javax.swing.JLabel();
		cbxLowercase = new JMCheckBox();
		cbxUppercase = new JMCheckBox();
		cbxDigits = new JMCheckBox();
		cbxUnderscore = new JMCheckBox();
		cbxSpecial = new JMCheckBox();
		cbxMoreSpecial = new JMCheckBox();
		cbxBrackets = new JMCheckBox();
		cbxWhitespace = new JMCheckBox();
		cbxMinus = new JMCheckBox();
		cbxPlus = new JMCheckBox();
		btnCancel = new javax.swing.JButton();
		btnOk = new javax.swing.JButton();

		lblGeneratePwHeader.setText("Generate password by use of following characters");

		lblPasswordLength.setText("Password length");

		cbxLowercase.setText("Lower case (abc...)");

		cbxUppercase.setText("Upper case (ABC...)");

		cbxDigits.setText("Digits (123...)");

		cbxUnderscore.setText("Underscore (_)");

		cbxSpecial.setText("Special (#@$%^&*?!)");

		cbxMoreSpecial.setText("More special (°§=~.:,;µ|€²³^)");

		cbxBrackets.setText("Brackets ({}[]()...)");

		cbxWhitespace.setText("White space");

		btnCancel.setText("Cancel");

		btnOk.setText("Generate Password");

		cbxMinus.setText("Minus(-)");

		cbxPlus.setText("Plus (+)");
		// ===
		// ===
		// ===

		spnPasswordLength = new JMSpinner<Integer>();
		((JMSpinner)spnPasswordLength).setPropertyModel(LambdaModel
			.of(getModelObject()::getPasswordLength, getModelObject()::setPasswordLength));

		cbxLowercase = new JMCheckBox();
		((JMCheckBox)cbxLowercase).setPropertyModel(
			LambdaModel.of(getModelObject()::isLowercase, getModelObject()::setLowercase));
		cbxUppercase = new JMCheckBox();
		((JMCheckBox)cbxUppercase).setPropertyModel(
			LambdaModel.of(getModelObject()::isUppercase, getModelObject()::setUppercase));
		cbxDigits = new JMCheckBox();
		((JMCheckBox)cbxDigits).setPropertyModel(
			LambdaModel.of(getModelObject()::isDigits, getModelObject()::setDigits));
		cbxUnderscore = new JMCheckBox();
		((JMCheckBox)cbxUnderscore).setPropertyModel(
			LambdaModel.of(getModelObject()::isUnderscore, getModelObject()::setUnderscore));
		cbxSpecial = new JMCheckBox();
		((JMCheckBox)cbxSpecial).setPropertyModel(
			LambdaModel.of(getModelObject()::isSpecial, getModelObject()::setSpecial));
		cbxMoreSpecial = new JMCheckBox();
		((JMCheckBox)cbxMoreSpecial).setPropertyModel(
			LambdaModel.of(getModelObject()::isMoreSpecial, getModelObject()::setMoreSpecial));
		cbxBrackets = new JMCheckBox();
		((JMCheckBox)cbxBrackets).setPropertyModel(
			LambdaModel.of(getModelObject()::isBrackets, getModelObject()::setBrackets));
		cbxWhitespace = new JMCheckBox();
		((JMCheckBox)cbxWhitespace).setPropertyModel(
			LambdaModel.of(getModelObject()::isWhitespace, getModelObject()::setWhitespace));
		cbxMinus = new JMCheckBox();
		((JMCheckBox)cbxMinus).setPropertyModel(
			LambdaModel.of(getModelObject()::isMinus, getModelObject()::setMinus));
		cbxPlus = new JMCheckBox();
		((JMCheckBox)cbxPlus)
			.setPropertyModel(LambdaModel.of(getModelObject()::isPlus, getModelObject()::setPlus));

		cbxLowercase.setText("Lower case (abc...)");
		cbxUppercase.setText("Upper case (ABC...)");
		cbxDigits.setText("Digits (123...)");
		cbxUnderscore.setText("Underscore (_)");
		cbxSpecial.setText("Special (#@$%^&*?!)");
		cbxMoreSpecial.setText("More special (°§=~.:,;µ|€²³^)");
		cbxBrackets.setText("Brackets ({}[]()...)");
		cbxWhitespace.setText("White space");
		cbxMinus.setText("Minus(-)");
		cbxPlus.setText("Plus (+)");

		btnOk.addActionListener(this::onOk);
		btnCancel.addActionListener(this::onCancel);
	}

	protected void onOk(ActionEvent actionEvent)
	{
		GeneratePasswordModelBean modelObject = getModelObject();
		modelObject.setPassword(GeneratePasswordExtensions.generatePassword(modelObject));
	}

	protected void onCancel(ActionEvent actionEvent)
	{
		System.err.println("onCancel method action called");
	}

	protected void onInitializeGroupLayout()
	{
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
					.addComponent(lblPasswordLength, javax.swing.GroupLayout.Alignment.LEADING,
						javax.swing.GroupLayout.PREFERRED_SIZE, 260,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addComponent(lblGeneratePwHeader, javax.swing.GroupLayout.PREFERRED_SIZE, 388,
						javax.swing.GroupLayout.PREFERRED_SIZE))
				.addGroup(layout
					.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
					.addGroup(layout.createSequentialGroup()
						.addComponent(btnOk, javax.swing.GroupLayout.PREFERRED_SIZE, 180,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addGap(18, 18, 18).addComponent(btnCancel,
							javax.swing.GroupLayout.PREFERRED_SIZE, 148,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGroup(javax.swing.GroupLayout.Alignment.LEADING,
						layout.createSequentialGroup()
							.addComponent(cbxMoreSpecial, javax.swing.GroupLayout.PREFERRED_SIZE,
								260, javax.swing.GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(cbxWhitespace, javax.swing.GroupLayout.PREFERRED_SIZE,
								260, javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGroup(javax.swing.GroupLayout.Alignment.LEADING,
						layout.createSequentialGroup()
							.addComponent(cbxSpecial, javax.swing.GroupLayout.PREFERRED_SIZE, 260,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(cbxBrackets, javax.swing.GroupLayout.PREFERRED_SIZE, 260,
								javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout
						.createSequentialGroup()
						.addComponent(cbxUppercase, javax.swing.GroupLayout.PREFERRED_SIZE, 260,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
							javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(cbxUnderscore, javax.swing.GroupLayout.PREFERRED_SIZE, 260,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout
						.createSequentialGroup()
						.addComponent(cbxLowercase, javax.swing.GroupLayout.PREFERRED_SIZE, 260,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addGap(48, 48, 48)
						.addGroup(layout
							.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
							.addComponent(spnPasswordLength, javax.swing.GroupLayout.PREFERRED_SIZE,
								100, javax.swing.GroupLayout.PREFERRED_SIZE)
							.addComponent(cbxDigits, javax.swing.GroupLayout.PREFERRED_SIZE, 260,
								javax.swing.GroupLayout.PREFERRED_SIZE)))
					.addGroup(layout.createSequentialGroup()
						.addComponent(cbxPlus, javax.swing.GroupLayout.PREFERRED_SIZE, 260,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
							javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(cbxMinus, javax.swing.GroupLayout.PREFERRED_SIZE, 260,
							javax.swing.GroupLayout.PREFERRED_SIZE))))
				.addContainerGap(22, Short.MAX_VALUE)));
		layout
			.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
					.addComponent(lblGeneratePwHeader, javax.swing.GroupLayout.PREFERRED_SIZE, 30,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 36,
						Short.MAX_VALUE)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(spnPasswordLength, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(lblPasswordLength, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(cbxDigits, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(cbxLowercase, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(cbxUnderscore, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(cbxUppercase, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(cbxBrackets, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(cbxSpecial, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(cbxWhitespace, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(cbxMoreSpecial, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(cbxMinus, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(cbxPlus, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(btnOk).addComponent(btnCancel))
					.addGap(15, 15, 15)));
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
	}


}
