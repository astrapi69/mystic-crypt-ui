/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.panel.signin;

/**
 *
 * @author astrapi69
 */
public class MasterPwFileFormPanel extends javax.swing.JPanel
{

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton btnCancel;
	private javax.swing.JButton btnHelp;
	private javax.swing.JButton btnKeyFileChooser;
	private javax.swing.JButton btnMasterPw;
	private javax.swing.JButton btnOk;
	private javax.swing.JCheckBox cbxKeyFile;
	private javax.swing.JCheckBox cbxMasterPw;
	private javax.swing.JComboBox<String> cmbKeyFile;
	private javax.swing.JLabel lblImageHeader;
	private javax.swing.JPasswordField txtMasterPw;

	// End of variables declaration//GEN-END:variables
	/**
	 * Creates new form MasterPwFileFormPanel
	 */
	public MasterPwFileFormPanel()
	{
		initComponents();
	}

	private void cbxMasterPwActionPerformed(java.awt.event.ActionEvent evt)
	{// GEN-FIRST:event_cbxMasterPwActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_cbxMasterPwActionPerformed

	/**
	 * This method is called from within the constructor to initialize the form. WARNING: Do NOT
	 * modify this code. The content of this method is always regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents()
	{

		lblImageHeader = new javax.swing.JLabel();
		cbxMasterPw = new javax.swing.JCheckBox();
		cbxKeyFile = new javax.swing.JCheckBox();
		txtMasterPw = new javax.swing.JPasswordField();
		btnMasterPw = new javax.swing.JButton();
		btnKeyFileChooser = new javax.swing.JButton();
		btnHelp = new javax.swing.JButton();
		btnOk = new javax.swing.JButton();
		btnCancel = new javax.swing.JButton();
		cmbKeyFile = new javax.swing.JComboBox<>();

		setPreferredSize(new java.awt.Dimension(820, 380));

		lblImageHeader.setText("Enter Master Key");

		cbxMasterPw.setText("Master Password:");
		cbxMasterPw.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				cbxMasterPwActionPerformed(evt);
			}
		});

		cbxKeyFile.setText("Key File:");

		txtMasterPw.setText("jPasswordField1");

		btnMasterPw.setText("***");

		btnKeyFileChooser.setText("Browse...");
		btnKeyFileChooser.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				btnKeyFileChooserActionPerformed(evt);
			}
		});

		btnHelp.setText("Help");

		btnOk.setText("OK");

		btnCancel.setText("Cancel");

		cmbKeyFile.setModel(new javax.swing.DefaultComboBoxModel<>(
			new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(lblImageHeader, javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
							layout.createSequentialGroup()
								.addComponent(btnHelp, javax.swing.GroupLayout.PREFERRED_SIZE, 122,
									javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
									493, Short.MAX_VALUE)
								.addComponent(btnOk, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
									javax.swing.GroupLayout.PREFERRED_SIZE)
								.addGap(30, 30, 30).addComponent(btnCancel,
									javax.swing.GroupLayout.PREFERRED_SIZE, 141,
									javax.swing.GroupLayout.PREFERRED_SIZE))
						.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout
							.createSequentialGroup()
							.addGroup(layout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING,
									false)
								.addComponent(cbxMasterPw, javax.swing.GroupLayout.DEFAULT_SIZE,
									201, Short.MAX_VALUE)
								.addComponent(cbxKeyFile, javax.swing.GroupLayout.DEFAULT_SIZE,
									javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
							.addGroup(layout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING,
									false)
								.addComponent(txtMasterPw, javax.swing.GroupLayout.DEFAULT_SIZE,
									625, Short.MAX_VALUE)
								.addComponent(cmbKeyFile, 0, javax.swing.GroupLayout.DEFAULT_SIZE,
									Short.MAX_VALUE))
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
							.addGroup(layout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(btnKeyFileChooser,
									javax.swing.GroupLayout.DEFAULT_SIZE,
									javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(btnMasterPw, javax.swing.GroupLayout.DEFAULT_SIZE,
									javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
					.addContainerGap()));
		layout
			.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
					.addComponent(lblImageHeader, javax.swing.GroupLayout.PREFERRED_SIZE, 80,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(txtMasterPw, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(btnMasterPw).addComponent(cbxMasterPw))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(btnKeyFileChooser)
						.addComponent(cmbKeyFile, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(cbxKeyFile))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 87,
						Short.MAX_VALUE)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(btnHelp).addComponent(btnOk).addComponent(btnCancel))
					.addGap(42, 42, 42)));
	}// </editor-fold>//GEN-END:initComponents

	private void btnKeyFileChooserActionPerformed(java.awt.event.ActionEvent evt)
	{// GEN-FIRST:event_btnKeyFileChooserActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_btnKeyFileChooserActionPerformed
}
