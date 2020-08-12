/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.alpharogroup.mystic.crypt.panels.certificate;

import de.alpharogroup.model.BaseModel;
import de.alpharogroup.model.api.Model;
import de.alpharogroup.swing.base.BasePanel;
import lombok.Getter;

/**
 * @author astrapi69
 */
@Getter
public class NewCertificateAttributesPanel extends BasePanel<CertificateAttributes> {
    private javax.swing.JLabel lblCommonName;
    private javax.swing.JLabel lblCountryCode;
    private javax.swing.JLabel lblLocation;
    private javax.swing.JLabel lblOrganization;
    private javax.swing.JLabel lblOrganizationUnit;
    private javax.swing.JLabel lblState;
    private javax.swing.JTextField txtCommonName;
    private javax.swing.JTextField txtCountryCode;
    private javax.swing.JTextField txtLocation;
    private javax.swing.JTextField txtOrganization;
    private javax.swing.JTextField txtOrganizationUnit;
    private javax.swing.JTextField txtState;
    public NewCertificateAttributesPanel() {
        this(BaseModel.of(CertificateAttributes.builder().build()));
    }

    public NewCertificateAttributesPanel(final Model<CertificateAttributes> model) {
        super(model);
    }

    @Override
    protected void onInitializeComponents()
    {
        super.onInitializeComponents();
        lblCommonName = new javax.swing.JLabel();
        lblOrganization = new javax.swing.JLabel();
        lblOrganizationUnit = new javax.swing.JLabel();
        lblCountryCode = new javax.swing.JLabel();
        lblState = new javax.swing.JLabel();
        lblLocation = new javax.swing.JLabel();
        txtCommonName = new javax.swing.JTextField();
        txtOrganization = new javax.swing.JTextField();
        txtOrganizationUnit = new javax.swing.JTextField();
        txtCountryCode = new javax.swing.JTextField();
        txtState = new javax.swing.JTextField();
        txtLocation = new javax.swing.JTextField();

        lblCommonName.setText("Common Name:");

        lblOrganization.setText("Organisation:");

        lblOrganizationUnit.setText("Organisation Unit:");

        lblCountryCode.setText("Country Code:");

        lblState.setText("State:");

        lblLocation.setText("Location:");
    }

    @Override protected void onInitializeLayout()
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
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(lblCommonName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(lblOrganization, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(lblOrganizationUnit, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                                        .addComponent(lblCountryCode, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(lblState, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(lblLocation, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(txtCommonName)
                                        .addComponent(txtOrganization)
                                        .addComponent(txtOrganizationUnit)
                                        .addComponent(txtCountryCode)
                                        .addComponent(txtState)
                                        .addComponent(txtLocation, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblCommonName)
                                        .addComponent(txtCommonName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblOrganization)
                                        .addComponent(txtOrganization, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblOrganizationUnit)
                                        .addComponent(txtOrganizationUnit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblCountryCode)
                                        .addComponent(txtCountryCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblState)
                                        .addComponent(txtState, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblLocation)
                                        .addComponent(txtLocation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }


}
