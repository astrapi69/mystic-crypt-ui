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
package io.github.astrapi69.mystic.crypt.panel.certificate;


import javax.swing.*;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.model.component.JMTextField;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;

/**
 * @author astrapi69
 */
@Getter
public class NewCertificateAttributesPanel extends BasePanel<DistinguishedNameInfoModel>
{
	private JLabel lblCommonName;
	private JLabel lblCountryCode;
	private JLabel lblLocation;
	private JLabel lblOrganization;
	private JLabel lblOrganizationUnit;
	private JLabel lblState;
	private JMTextField txtCommonName;
	private JMTextField txtCountryCode;
	private JMTextField txtLocation;
	private JMTextField txtOrganization;
	private JMTextField txtOrganizationUnit;
	private JMTextField txtState;

	public NewCertificateAttributesPanel()
	{
		this(BaseModel.of(DistinguishedNameInfoModel.builder().build()));
	}

	public NewCertificateAttributesPanel(final IModel<DistinguishedNameInfoModel> model)
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
		txtCommonName = new JMTextField();
		txtOrganization = new JMTextField();
		txtOrganizationUnit = new JMTextField();
		txtCountryCode = new JMTextField();
		txtState = new JMTextField();
		txtLocation = new JMTextField();

		DistinguishedNameInfoModel modelObject = getModelObject();

		txtCommonName.setPropertyModel(
			LambdaModel.of(modelObject::getCommonName, modelObject::setCommonName));
		txtOrganization.setPropertyModel(
			LambdaModel.of(modelObject::getOrganisation, modelObject::setOrganisation));
		txtOrganizationUnit.setPropertyModel(
			LambdaModel.of(modelObject::getOrganisationUnit, modelObject::setOrganisationUnit));
		txtCountryCode.setPropertyModel(
			LambdaModel.of(modelObject::getCountryCode, modelObject::setCountryCode));
		txtState.setPropertyModel(LambdaModel.of(modelObject::getState, modelObject::setState));
		txtLocation
			.setPropertyModel(LambdaModel.of(modelObject::getLocation, modelObject::setLocation));

		txtCommonName.setToolTipText(
			Messages.getString("wizard.certificate.attributes.tooltip.common.name"));
		txtOrganization.setToolTipText(
			Messages.getString("wizard.certificate.attributes.tooltip.organisation"));
		txtOrganizationUnit.setToolTipText(
			Messages.getString("wizard.certificate.attributes.tooltip.organisation.unit"));
		txtCountryCode.setToolTipText(
			Messages.getString("wizard.certificate.attributes.tooltip.country.code"));
		txtState.setToolTipText(Messages.getString("wizard.certificate.attributes.tooltip.state"));
		txtLocation
			.setToolTipText(Messages.getString("wizard.certificate.attributes.tooltip.location"));

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
		onInitializeMigLayout();
	}

	protected void onInitializeMigLayout()
	{
		MigLayout migLayout = new MigLayout("wrap 2", "[right][grow, fill]");

		setLayout(migLayout);

		add(lblCommonName);
		add(txtCommonName, "wrap");

		add(lblOrganization);
		add(txtOrganization, "wrap");

		add(lblOrganizationUnit);
		add(txtOrganizationUnit, "wrap");

		add(lblCountryCode);
		add(txtCountryCode, "wrap");

		add(lblState);
		add(txtState, "wrap");

		add(lblLocation);
		add(txtLocation, "wrap");
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
