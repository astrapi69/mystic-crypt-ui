package de.alpharogroup.mystic.crypt.panels.certificate;

import de.alpharogroup.model.BaseModel;
import de.alpharogroup.model.api.Model;
import de.alpharogroup.swing.base.BasePanel;

import javax.swing.*;
import javax.swing.table.*;

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
        this(BaseModel.of());
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

        txtSerialNumber.setText("txtSerialNumber");

        lblIssuer.setText("Issuer:");

        lblSubject.setText("Subject:");

        txtIssuer.setText("txtIssuer");

        txtSubject.setText("txtSubject");

        lblNotBefore.setText("Not Before:");

        txtNotBefore.setText("txtNotBefore");

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

        txtNotAfter.setText("txtNotAfter");

        lblPublicKey.setText("Public Key:");

        txtPublicKey.setColumns(20);
        txtPublicKey.setRows(5);
        scpPublicKey.setViewportView(txtPublicKey);

        lblSignatureAlgorithm.setText("Signature Algorithm:");

        txtSignatureAlgorithm.setText("txtSignatureAlgorithm");

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

    private void onCreateIssuer(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onCreateIssuer
        // TODO add your handling code here:

    }//GEN-LAST:event_onCreateIssuer

    private void onGenerateSerialNumber(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onGenerateSerialNumber
        // TODO add your handling code here:
    }//GEN-LAST:event_onGenerateSerialNumber

    private void onCreateSubject(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onCreateSubject
        // TODO add your handling code here:
    }//GEN-LAST:event_onCreateSubject

    private void onAddExtension(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onAddExtension
        // TODO add your handling code here:
    }//GEN-LAST:event_onAddExtension


}
