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
package io.github.astrapi69.mystic.crypt.panels.certificate;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.Model;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.SpringBootSwingApplication;
import io.github.astrapi69.random.number.RandomBigIntegerFactory;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.dialog.factories.JDialogFactory;
import io.github.astrapi69.swing.listener.RequestFocusListener;
import lombok.Getter;

import javax.swing.*;
import javax.swing.table.*;
import java.math.BigInteger;

@Getter
public class NewCertificateInfoPanel extends BasePanel<CertificateInfo> {

    private JButton btnAddExtension;
    private JButton btnCreateIssuer;
    private JButton btnCreateSubject;
    private JButton btnGenerateSerialNumber;
    private JComboBox<String> cmbVersion;
    private JLabel lblExtensions;
    private JLabel lblIssuer;
    private JLabel lblNotAfter;
    private JLabel lblNotBefore;
    private JLabel lblPublicKey;
    private JLabel lblSerialNumber;
    private JLabel lblSignatureAlgorithm;
    private JLabel lblSubject;
    private JLabel lblVersion;
    private JScrollPane scpExtensions;
    private JScrollPane scpPublicKey;
    private JTable srcExtensions;
    private JTextField txtIssuer;
    private JTextField txtNotAfter;
    private JTextField txtNotBefore;
    private JTextArea txtPublicKey;
    private JTextField txtSerialNumber;
    private JTextField txtSignatureAlgorithm;
    private JTextField txtSubject;

    public NewCertificateInfoPanel() {
        this(BaseModel.<CertificateInfo>of(CertificateInfo.builder().build()));
    }

    public NewCertificateInfoPanel(final Model<CertificateInfo> model) {
        super(model);
    }

    @Override
    protected void onInitializeComponents()
    {
        super.onInitializeComponents();

        lblVersion = new JLabel();
        cmbVersion = new JComboBox<>();
        lblSerialNumber = new JLabel();
        txtSerialNumber = new JTextField();
        lblIssuer = new JLabel();
        lblSubject = new JLabel();
        txtIssuer = new JTextField();
        txtSubject = new JTextField();
        lblNotBefore = new JLabel();
        txtNotBefore = new JTextField();
        lblNotAfter = new JLabel();
        btnCreateIssuer = new JButton();
        btnCreateSubject = new JButton();
        txtNotAfter = new JTextField();
        lblPublicKey = new JLabel();
        scpPublicKey = new JScrollPane();
        txtPublicKey = new JTextArea();
        lblSignatureAlgorithm = new JLabel();
        txtSignatureAlgorithm = new JTextField();
        lblExtensions = new JLabel();
        scpExtensions = new JScrollPane();
        srcExtensions = new JTable();
        btnAddExtension = new JButton();
        btnGenerateSerialNumber = new JButton();

        lblVersion.setText("Version:");

        cmbVersion.setModel(new DefaultComboBoxModel<>(new String[] { "1", "2", "3" }));

        lblSerialNumber.setText("Serial Number:");

        txtSerialNumber.setText("");

        lblIssuer.setText("Issuer:");

        lblSubject.setText("Subject:");

        txtIssuer.setText("");

        txtSubject.setText("");

        lblNotBefore.setText("Not Before:");

        txtNotBefore.setText("");

        lblNotAfter.setText("Not After:");

        btnCreateIssuer.setText("Create Issuer");
        btnCreateIssuer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onCreateIssuer(evt);
            }
        });

        btnCreateSubject.setText("Create Subj.");
        btnCreateSubject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onCreateSubject(evt);
            }
        });

        txtNotAfter.setText("");

        lblPublicKey.setText("Public Key:");

        txtPublicKey.setColumns(20);
        txtPublicKey.setRows(5);
        scpPublicKey.setViewportView(txtPublicKey);

        lblSignatureAlgorithm.setText("Signature Algorithm:");

        txtSignatureAlgorithm.setText("");

        lblExtensions.setText("Extensions:");

        srcExtensions.setModel(new DefaultTableModel(
                new Object [][] {
                        {null, null}
                },
                new String [] {
                        "Key", "Value"
                }
        ) {
            Class[] types = new Class [] {
                    java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        scpExtensions.setViewportView(srcExtensions);

        btnAddExtension.setText("Add");
        btnAddExtension.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onAddExtension(evt);
            }
        });

        btnGenerateSerialNumber.setText("Generate");
        btnGenerateSerialNumber.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onGenerateSerialNumber(evt);
            }
        });

        // Manually added:

        // enable when functionality is given...
        btnAddExtension.setEnabled(false);

    }

    @Override protected void onInitializeLayout()
    {
        super.onInitializeLayout();
        onInitializeGroupLayout();
    }

    protected void onInitializeGroupLayout()
    {
        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(scpExtensions, GroupLayout.PREFERRED_SIZE, 459, GroupLayout.PREFERRED_SIZE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(lblNotAfter, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(lblExtensions, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(lblSignatureAlgorithm, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                                                        .addComponent(lblPublicKey, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(lblVersion, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(lblSerialNumber, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                .addGap(59, 59, 59)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                                        .addComponent(txtNotAfter, GroupLayout.Alignment.LEADING)
                                                        .addComponent(txtNotBefore, GroupLayout.Alignment.LEADING)
                                                        .addComponent(txtSubject, GroupLayout.Alignment.LEADING)
                                                        .addComponent(txtIssuer, GroupLayout.Alignment.LEADING)
                                                        .addComponent(cmbVersion, GroupLayout.Alignment.LEADING, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(txtSerialNumber, GroupLayout.Alignment.LEADING)
                                                        .addComponent(scpPublicKey, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                                        .addComponent(txtSignatureAlgorithm, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)))
                                        .addComponent(lblNotBefore, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblSubject, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblIssuer, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(btnCreateIssuer, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btnCreateSubject, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btnAddExtension, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btnGenerateSerialNumber, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap(55, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(cmbVersion, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblVersion))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(btnGenerateSerialNumber, GroupLayout.Alignment.TRAILING)
                                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(lblSerialNumber)
                                                .addComponent(txtSerialNumber, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(btnCreateIssuer, GroupLayout.Alignment.TRAILING)
                                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(lblIssuer)
                                                .addComponent(txtIssuer, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblSubject)
                                        .addComponent(txtSubject, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnCreateSubject))
                                .addGap(15, 15, 15)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblNotBefore)
                                        .addComponent(txtNotBefore, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblNotAfter)
                                        .addComponent(txtNotAfter, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(lblPublicKey)
                                        .addComponent(scpPublicKey, GroupLayout.PREFERRED_SIZE, 220, GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(txtSignatureAlgorithm, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblSignatureAlgorithm))
                                .addGap(18, 18, 18)
                                .addComponent(lblExtensions)
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(scpExtensions, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnAddExtension))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }

    protected void onCreateIssuer(java.awt.event.ActionEvent evt) {
        NewCertificateAttributesPanel panel = new NewCertificateAttributesPanel();

        JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);

        JDialog dialog = JDialogFactory.newJDialog(MysticCryptApplicationFrame.getInstance(),
                optionPane, "Create issuer");
        dialog.addWindowFocusListener(new RequestFocusListener(panel.getTxtCommonName()));
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        if (optionPane.getValue().equals(JOptionPane.OK_OPTION))
        {
            panel.getModelObject().setCommonName(panel.getTxtCommonName().getText());
            panel.getModelObject().setCountryCode(panel.getTxtCountryCode().getText());
            panel.getModelObject().setLocation(panel.getTxtLocation().getText());
            panel.getModelObject().setOrganisation(panel.getTxtOrganization().getText());
            panel.getModelObject().setOrganisationUnit(panel.getTxtOrganizationUnit().getText());
            panel.getModelObject().setState(panel.getTxtState().getText());
            String issuer = panel.getModelObject().toRepresentableString();
            getModelObject().setIssuer(issuer);
            getTxtIssuer().setText(issuer);
        }

    }

    protected void onGenerateSerialNumber(java.awt.event.ActionEvent evt) {
        BigInteger serialNumber = RandomBigIntegerFactory.randomSerialNumber();
        getModelObject().setSerialNumber(serialNumber);
        getTxtSerialNumber().setText(serialNumber.toString());
    }

    protected void onCreateSubject(java.awt.event.ActionEvent evt) {
        NewCertificateAttributesPanel panel = new NewCertificateAttributesPanel();

        JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);

        JDialog dialog = JDialogFactory.newJDialog(MysticCryptApplicationFrame.getInstance(),
                optionPane, "Create subject");
        dialog.addWindowFocusListener(new RequestFocusListener(panel.getTxtCommonName()));
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        if (optionPane.getValue().equals(JOptionPane.OK_OPTION))
        {
            panel.getModelObject().setCommonName(panel.getTxtCommonName().getText());
            panel.getModelObject().setCountryCode(panel.getTxtCountryCode().getText());
            panel.getModelObject().setLocation(panel.getTxtLocation().getText());
            panel.getModelObject().setOrganisation(panel.getTxtOrganization().getText());
            panel.getModelObject().setOrganisationUnit(panel.getTxtOrganizationUnit().getText());
            panel.getModelObject().setState(panel.getTxtState().getText());
            String subject = panel.getModelObject().toRepresentableString();
            getModelObject().setSubject(subject);
            getTxtSubject().setText(subject);
        }
    }

    protected void onAddExtension(java.awt.event.ActionEvent evt) {

    }

}
