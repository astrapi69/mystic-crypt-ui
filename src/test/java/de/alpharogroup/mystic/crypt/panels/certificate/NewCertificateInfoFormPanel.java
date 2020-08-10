/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.alpharogroup.mystic.crypt.panels.certificate;

/**
 *
 * @author astrapi69
 */
public class NewCertificateInfoFormPanel extends javax.swing.JPanel {

    /**
     * Creates new form NewCertificateInfoFormPanel
     */
    public NewCertificateInfoFormPanel() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblVersion = new javax.swing.JLabel();
        cmbVersion = new javax.swing.JComboBox<>();
        lblSerialNumber = new javax.swing.JLabel();
        txtSerialNumber = new javax.swing.JTextField();
        lblIssuer = new javax.swing.JLabel();
        lblSubject = new javax.swing.JLabel();
        txtIssuer = new javax.swing.JTextField();
        txtSubject = new javax.swing.JTextField();
        lblNotBefore = new javax.swing.JLabel();
        txtNotBefore = new javax.swing.JTextField();
        lblNotAfter = new javax.swing.JLabel();
        btnCreateIssuer = new javax.swing.JButton();
        btnCreateSubject = new javax.swing.JButton();
        txtNotAfter = new javax.swing.JTextField();
        lblPublicKey = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtPublicKey = new javax.swing.JTextArea();
        lblSignatureAlgorithm = new javax.swing.JLabel();
        txtSignatureAlgorithm = new javax.swing.JTextField();
        lblExtensions = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblExtensions = new javax.swing.JTable();
        btnAddExtension = new javax.swing.JButton();
        btnGenerateSerialNumber = new javax.swing.JButton();

        lblVersion.setText("Version:");

        cmbVersion.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3" }));

        lblSerialNumber.setText("Serial Number:");

        txtSerialNumber.setText("txtSerialNumber");
        txtSerialNumber.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSerialNumberActionPerformed(evt);
            }
        });

        lblIssuer.setText("Issuer:");

        lblSubject.setText("Subject:");

        txtIssuer.setText("txtIssuer");
        txtIssuer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtIssuerActionPerformed(evt);
            }
        });

        txtSubject.setText("txtSubject");

        lblNotBefore.setText("Not Before:");

        txtNotBefore.setText("txtNotBefore");

        lblNotAfter.setText("Not After:");

        btnCreateIssuer.setText("Create Issuer");
        btnCreateIssuer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateIssuerActionPerformed(evt);
            }
        });

        btnCreateSubject.setText("Create Subj.");

        txtNotAfter.setText("txtNotAfter");

        lblPublicKey.setText("Public Key:");

        txtPublicKey.setColumns(20);
        txtPublicKey.setRows(5);
        jScrollPane1.setViewportView(txtPublicKey);

        lblSignatureAlgorithm.setText("Signature Algorithm:");

        txtSignatureAlgorithm.setText("txtSignatureAlgorithm");

        lblExtensions.setText("Extensions:");

        tblExtensions.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane2.setViewportView(tblExtensions);

        btnAddExtension.setText("Add");

        btnGenerateSerialNumber.setText("Generate");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 459, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(lblNotAfter, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblExtensions, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblSignatureAlgorithm, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                                .addComponent(lblPublicKey, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblVersion, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblSerialNumber, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGap(59, 59, 59)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(txtNotAfter, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(txtNotBefore, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(txtSubject, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(txtIssuer, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(cmbVersion, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(txtSerialNumber, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                .addComponent(txtSignatureAlgorithm, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE))
                            .addGap(0, 0, Short.MAX_VALUE))
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lblNotBefore, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(lblSubject, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(lblIssuer, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addGap(44, 44, 44)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnCreateIssuer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnCreateSubject, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnAddExtension, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnGenerateSerialNumber, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(55, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbVersion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblVersion))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblSerialNumber)
                    .addComponent(txtSerialNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnGenerateSerialNumber))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblIssuer)
                    .addComponent(txtIssuer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCreateIssuer))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblSubject)
                    .addComponent(txtSubject, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCreateSubject))
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblNotBefore)
                    .addComponent(txtNotBefore, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblNotAfter)
                    .addComponent(txtNotAfter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblPublicKey)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtSignatureAlgorithm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSignatureAlgorithm))
                .addGap(18, 18, 18)
                .addComponent(lblExtensions)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAddExtension))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnCreateIssuerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateIssuerActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnCreateIssuerActionPerformed

    private void txtSerialNumberActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSerialNumberActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtSerialNumberActionPerformed

    private void txtIssuerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtIssuerActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtIssuerActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddExtension;
    private javax.swing.JButton btnCreateIssuer;
    private javax.swing.JButton btnCreateSubject;
    private javax.swing.JButton btnGenerateSerialNumber;
    private javax.swing.JComboBox<String> cmbVersion;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblExtensions;
    private javax.swing.JLabel lblIssuer;
    private javax.swing.JLabel lblNotAfter;
    private javax.swing.JLabel lblNotBefore;
    private javax.swing.JLabel lblPublicKey;
    private javax.swing.JLabel lblSerialNumber;
    private javax.swing.JLabel lblSignatureAlgorithm;
    private javax.swing.JLabel lblSubject;
    private javax.swing.JLabel lblVersion;
    private javax.swing.JTable tblExtensions;
    private javax.swing.JTextField txtIssuer;
    private javax.swing.JTextField txtNotAfter;
    private javax.swing.JTextField txtNotBefore;
    private javax.swing.JTextArea txtPublicKey;
    private javax.swing.JTextField txtSerialNumber;
    private javax.swing.JTextField txtSignatureAlgorithm;
    private javax.swing.JTextField txtSubject;
    // End of variables declaration//GEN-END:variables
}
