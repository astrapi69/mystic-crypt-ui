/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.astrapi69.mystic.crypt.panels.pw;

/**
 *
 * @author astrapi69
 */
public class GeneratePasswordFormPanel extends javax.swing.JPanel {

    /**
     * Creates new form GeneratePasswordPanel
     */
    public GeneratePasswordFormPanel() {
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

        lblGeneratePwHeader = new javax.swing.JLabel();
        spnPasswordLength = new javax.swing.JSpinner();
        lblPasswordLength = new javax.swing.JLabel();
        cbxLowercase = new javax.swing.JCheckBox();
        cbxUppercase = new javax.swing.JCheckBox();
        cbxDigits = new javax.swing.JCheckBox();
        cbxUnderscore = new javax.swing.JCheckBox();
        cbxSpecial = new javax.swing.JCheckBox();
        cbxMoreSpecial = new javax.swing.JCheckBox();
        cbxBrackets = new javax.swing.JCheckBox();
        cbxWhitespace = new javax.swing.JCheckBox();
        btnCancel = new javax.swing.JButton();
        btnOk = new javax.swing.JButton();
        cbxMinus = new javax.swing.JCheckBox();
        cbxPlus = new javax.swing.JCheckBox();

        lblGeneratePwHeader.setText("Generate password by use of following characters");

        lblPasswordLength.setText("Password length");

        cbxLowercase.setText("Lower case (abc...)");

        cbxUppercase.setText("Upper case (ABC...)");
        cbxUppercase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbxUppercaseActionPerformed(evt);
            }
        });

        cbxDigits.setText("Digits (123...)");

        cbxUnderscore.setText("Underscore (_)");

        cbxSpecial.setText("Special (#@$%^&*?!)");
        cbxSpecial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbxSpecialActionPerformed(evt);
            }
        });

        cbxMoreSpecial.setText("More special (°§=~.:,;µ|€²³^)");
        cbxMoreSpecial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbxMoreSpecialActionPerformed(evt);
            }
        });

        cbxBrackets.setText("Brackets ({}[]()...)");
        cbxBrackets.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbxBracketsActionPerformed(evt);
            }
        });

        cbxWhitespace.setText("White space");
        cbxWhitespace.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbxWhitespaceActionPerformed(evt);
            }
        });

        btnCancel.setText("Cancel");

        btnOk.setText("Generate Password");

        cbxMinus.setText("Minus(-)");

        cbxPlus.setText("Plus (+)");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(lblPasswordLength, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblGeneratePwHeader, javax.swing.GroupLayout.PREFERRED_SIZE, 388, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(btnOk, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                            .addComponent(cbxMoreSpecial, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cbxWhitespace, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                            .addComponent(cbxSpecial, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cbxBrackets, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                            .addComponent(cbxUppercase, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cbxUnderscore, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                            .addComponent(cbxLowercase, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(48, 48, 48)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(spnPasswordLength, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(cbxDigits, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(cbxPlus, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cbxMinus, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(20, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblGeneratePwHeader, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 46, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spnPasswordLength, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPasswordLength, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cbxDigits, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbxLowercase, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cbxUnderscore, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbxUppercase, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cbxBrackets, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbxSpecial, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbxWhitespace, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbxMoreSpecial, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cbxMinus, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbxPlus, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnOk)
                    .addComponent(btnCancel))
                .addGap(15, 15, 15))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void cbxUppercaseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbxUppercaseActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cbxUppercaseActionPerformed

    private void cbxSpecialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbxSpecialActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cbxSpecialActionPerformed

    private void cbxMoreSpecialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbxMoreSpecialActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cbxMoreSpecialActionPerformed

    private void cbxBracketsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbxBracketsActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cbxBracketsActionPerformed

    private void cbxWhitespaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbxWhitespaceActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cbxWhitespaceActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
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
    // End of variables declaration//GEN-END:variables
}
