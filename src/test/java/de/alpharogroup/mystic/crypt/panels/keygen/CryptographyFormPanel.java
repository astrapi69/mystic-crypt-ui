/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.alpharogroup.mystic.crypt.panels.keygen;

import javax.swing.DefaultComboBoxModel;

import de.alpharogroup.crypto.key.KeySize;

/**
 *
 * @author astrapi69
 */
public class CryptographyFormPanel extends javax.swing.JPanel {

    /**
     * Creates new form CryptographyPanel
     */
    public CryptographyFormPanel() {
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

        scpPrivateKey = new javax.swing.JScrollPane();
        txtPrivateKey = new javax.swing.JTextArea();
        cmbKeySize = new javax.swing.JComboBox<>();
        btnGenerate = new javax.swing.JButton();
        lblPrivateKey = new javax.swing.JLabel();
        lblKeySize = new javax.swing.JLabel();
        scpPublicKey = new javax.swing.JScrollPane();
        txtPublicKey = new javax.swing.JTextArea();
        lblPublicKey = new javax.swing.JLabel();
        btnClear = new javax.swing.JButton();
        btnSavePublicKey = new javax.swing.JButton();
        btnSavePrivateKey = new javax.swing.JButton();

        txtPrivateKey.setColumns(20);
        txtPrivateKey.setRows(5);
        scpPrivateKey.setViewportView(txtPrivateKey);
        txtPrivateKey.getAccessibleContext().setAccessibleDescription("");

        cmbKeySize.setModel(new DefaultComboBoxModel(KeySize.values()));
        cmbKeySize.setSelectedItem(KeySize.KEYSIZE_1024);

        btnGenerate.setText("Generate keys");

        lblPrivateKey.setText("Private key");

        lblKeySize.setText("Keysize");

        txtPublicKey.setColumns(20);
        txtPublicKey.setRows(5);
        scpPublicKey.setViewportView(txtPublicKey);

        lblPublicKey.setText("Public key");

        btnClear.setText("Clear keys");

        btnSavePublicKey.setText("Save public key");

        btnSavePrivateKey.setText("Save private key");

        final javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(cmbKeySize, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(btnGenerate, javax.swing.GroupLayout.DEFAULT_SIZE, 206, Short.MAX_VALUE)))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addComponent(lblKeySize, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(btnClear, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(scpPrivateKey, javax.swing.GroupLayout.PREFERRED_SIZE, 480, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblPrivateKey, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(542, 542, 542)
                        .addComponent(btnSavePrivateKey, javax.swing.GroupLayout.PREFERRED_SIZE, 174, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 56, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(lblPublicKey, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(scpPublicKey, javax.swing.GroupLayout.PREFERRED_SIZE, 480, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(btnSavePublicKey, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 174, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(28, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblPrivateKey, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblKeySize, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPublicKey, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(cmbKeySize, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnGenerate, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnClear, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(scpPrivateKey, javax.swing.GroupLayout.DEFAULT_SIZE, 265, Short.MAX_VALUE)
                    .addComponent(scpPublicKey))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 24, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSavePublicKey, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSavePrivateKey, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnGenerate;
    private javax.swing.JButton btnSavePrivateKey;
    private javax.swing.JButton btnSavePublicKey;
    private javax.swing.JComboBox<KeySize> cmbKeySize;
    private javax.swing.JLabel lblKeySize;
    private javax.swing.JLabel lblPrivateKey;
    private javax.swing.JLabel lblPublicKey;
    private javax.swing.JScrollPane scpPrivateKey;
    private javax.swing.JScrollPane scpPublicKey;
    private javax.swing.JTextArea txtPrivateKey;
    private javax.swing.JTextArea txtPublicKey;
    // End of variables declaration//GEN-END:variables
}