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
package io.github.astrapi69.mystic.crypt.panels.certificate;

import javax.swing.*;

import lombok.Getter;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.Model;
import io.github.astrapi69.swing.base.BasePanel;

/**
 * @author astrapi69
 */
@Getter
public class NewCertificateAttributesPanel extends BasePanel<CertificateAttributes>
{
	private JLabel lblCommonName;
	private JLabel lblCountryCode;
	private JLabel lblLocation;
	private JLabel lblOrganization;
	private JLabel lblOrganizationUnit;
	private JLabel lblState;
	private JTextField txtCommonName;
	private JTextField txtCountryCode;
	private JTextField txtLocation;
	private JTextField txtOrganization;
	private JTextField txtOrganizationUnit;
	private JTextField txtState;

	public NewCertificateAttributesPanel()
	{
		this(BaseModel.of(CertificateAttributes.builder().build()));
	}

	public NewCertificateAttributesPanel(final Model<CertificateAttributes> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
		lblCommonName = new JLabel();
		lblOrganization = new JLabel();
		lblOrganizationUnit = new JLabel();
		lblCountryCode = new JLabel();
		lblState = new JLabel();
		lblLocation = new JLabel();
		txtCommonName = new JTextField();
		txtOrganization = new JTextField();
		txtOrganizationUnit = new JTextField();
		txtCountryCode = new JTextField();
		txtState = new JTextField();
		txtLocation = new JTextField();

		lblCommonName.setText("Common Name:");

		lblOrganization.setText("Organisation:");

		lblOrganizationUnit.setText("Organisation Unit:");

		lblCountryCode.setText("Country Code:");

		lblState.setText("State:");

		lblLocation.setText("Location:");
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
	}

	protected void onInitializeGroupLayout()
	{
		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout
				.createParallelGroup(GroupLayout.Alignment.LEADING, false)
				.addComponent(lblCommonName, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
					Short.MAX_VALUE)
				.addComponent(lblOrganization, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
					Short.MAX_VALUE)
				.addComponent(lblOrganizationUnit, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
				.addComponent(lblCountryCode, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
					Short.MAX_VALUE)
				.addComponent(lblState, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
					Short.MAX_VALUE)
				.addComponent(lblLocation, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
					Short.MAX_VALUE))
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
					.addComponent(txtCommonName).addComponent(txtOrganization)
					.addComponent(txtOrganizationUnit).addComponent(txtCountryCode)
					.addComponent(txtState)
					.addComponent(txtLocation, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE))
				.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		layout
			.setVerticalGroup(
				layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup().addContainerGap()
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
							.addComponent(lblCommonName)
							.addComponent(txtCommonName, GroupLayout.PREFERRED_SIZE,
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addGap(18, 18, 18)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
							.addComponent(lblOrganization)
							.addComponent(txtOrganization, GroupLayout.PREFERRED_SIZE,
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addGap(18, 18, 18)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
							.addComponent(lblOrganizationUnit)
							.addComponent(txtOrganizationUnit, GroupLayout.PREFERRED_SIZE,
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addGap(18, 18, 18)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
							.addComponent(lblCountryCode)
							.addComponent(txtCountryCode, GroupLayout.PREFERRED_SIZE,
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addGap(18, 18, 18)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
							.addComponent(lblState)
							.addComponent(txtState, GroupLayout.PREFERRED_SIZE,
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addGap(18, 18, 18)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
							.addComponent(lblLocation).addComponent(txtLocation,
								GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE))
						.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
	}


}
