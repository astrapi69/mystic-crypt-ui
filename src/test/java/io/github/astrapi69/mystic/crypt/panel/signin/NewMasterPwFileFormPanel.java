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
package io.github.astrapi69.mystic.crypt.panel.signin;

/**
 *
 * @author astrapi69
 */
public class NewMasterPwFileFormPanel extends javax.swing.JPanel
{

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton btnApplicationFileChooser;
	private javax.swing.JButton btnCancel;
	private javax.swing.JButton btnCreateKeyFile;
	private javax.swing.JButton btnGeneratePw;
	private javax.swing.JButton btnHelp;
	private javax.swing.JButton btnKeyFileChooser;
	private javax.swing.JButton btnMasterPw;
	private javax.swing.JButton btnOk;
	private javax.swing.JCheckBox cbxKeyFile;
	private javax.swing.JCheckBox cbxMasterPw;
	private javax.swing.JComboBox<String> cmbKeyFile;
	private javax.swing.JLabel lblApplicationFile;
	private javax.swing.JLabel lblImageHeader;
	private javax.swing.JLabel lblRepeatPw;
	private javax.swing.JTextField txtApplicationFile;
	private javax.swing.JPasswordField txtMasterPw;
	private javax.swing.JPasswordField txtRepeatPw;

	/**
	 * Creates new form NewMasterPwFileFormPanel
	 */
	public NewMasterPwFileFormPanel()
	{
		initComponents();
	}

	/**
	 * This method is called from within the constructor to initialize the form. WARNING: Do NOT
	 * modify this code. The content of this method is always regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents()
	{

		btnKeyFileChooser = new javax.swing.JButton();
		btnHelp = new javax.swing.JButton();
		btnOk = new javax.swing.JButton();
		btnCancel = new javax.swing.JButton();
		lblImageHeader = new javax.swing.JLabel();
		cbxMasterPw = new javax.swing.JCheckBox();
		cbxKeyFile = new javax.swing.JCheckBox();
		txtRepeatPw = new javax.swing.JPasswordField();
		btnMasterPw = new javax.swing.JButton();
		cmbKeyFile = new javax.swing.JComboBox<>();
		lblRepeatPw = new javax.swing.JLabel();
		txtMasterPw = new javax.swing.JPasswordField();
		btnCreateKeyFile = new javax.swing.JButton();
		lblApplicationFile = new javax.swing.JLabel();
		txtApplicationFile = new javax.swing.JTextField();
		btnApplicationFileChooser = new javax.swing.JButton();
		btnGeneratePw = new javax.swing.JButton();

		btnKeyFileChooser.setText("Browse..");

		btnHelp.setText("Help");

		btnOk.setText("OK");

		btnCancel.setText("Cancel");

		lblImageHeader.setText("Create Master Key");

		cbxMasterPw.setText("Master Password:");
		cbxMasterPw.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				cbxMasterPwActionPerformed(evt);
			}
		});

		cbxKeyFile.setText("Key File:");

		txtRepeatPw.setText("jPasswordField1");

		btnMasterPw.setText("***");

		cmbKeyFile.setModel(new javax.swing.DefaultComboBoxModel<>(
			new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

		lblRepeatPw.setText("Repeat Password:");

		txtMasterPw.setText("jPasswordField1");

		btnCreateKeyFile.setText("Create key file...");

		lblApplicationFile.setText("Application File");

		btnApplicationFileChooser.setText("Browse...");

		btnGeneratePw.setText("Generate");

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout
			.setHorizontalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout
						.createParallelGroup(
							javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
							layout.createSequentialGroup()
								.addGroup(
									layout
										.createParallelGroup(
											javax.swing.GroupLayout.Alignment.LEADING)
										.addComponent(lblImageHeader,
											javax.swing.GroupLayout.DEFAULT_SIZE,
											javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addGroup(layout.createSequentialGroup().addGroup(layout
											.createParallelGroup(
												javax.swing.GroupLayout.Alignment.LEADING)
											.addGroup(
												layout.createSequentialGroup().addGap(8, 516,
													Short.MAX_VALUE).addComponent(btnCreateKeyFile,
														javax.swing.GroupLayout.PREFERRED_SIZE, 163,
														javax.swing.GroupLayout.PREFERRED_SIZE)
													.addGap(18, 18, 18).addComponent(
														btnKeyFileChooser,
														javax.swing.GroupLayout.PREFERRED_SIZE, 111,
														javax.swing.GroupLayout.PREFERRED_SIZE))
											.addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
												layout
													.createSequentialGroup().addGap(0, 212,
														Short.MAX_VALUE)
													.addComponent(cmbKeyFile,
														javax.swing.GroupLayout.PREFERRED_SIZE, 596,
														javax.swing.GroupLayout.PREFERRED_SIZE))
											.addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
												layout.createSequentialGroup().addGroup(layout
													.createParallelGroup(
														javax.swing.GroupLayout.Alignment.LEADING)
													.addGroup(layout.createSequentialGroup()
														.addGap(12, 12, 12)
														.addGroup(layout.createParallelGroup(
															javax.swing.GroupLayout.Alignment.LEADING)
															.addComponent(cbxMasterPw,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																169,
																javax.swing.GroupLayout.PREFERRED_SIZE)
															.addComponent(lblApplicationFile,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																169,
																javax.swing.GroupLayout.PREFERRED_SIZE)
															.addComponent(cbxKeyFile,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																161,
																javax.swing.GroupLayout.PREFERRED_SIZE)))
													.addComponent(
														lblRepeatPw,
														javax.swing.GroupLayout.Alignment.TRAILING,
														javax.swing.GroupLayout.PREFERRED_SIZE, 134,
														javax.swing.GroupLayout.PREFERRED_SIZE))
													.addPreferredGap(
														javax.swing.LayoutStyle.ComponentPlacement.RELATED,
														31, Short.MAX_VALUE)
													.addGroup(layout.createParallelGroup(
														javax.swing.GroupLayout.Alignment.LEADING)
														.addComponent(txtApplicationFile,
															javax.swing.GroupLayout.Alignment.TRAILING,
															javax.swing.GroupLayout.PREFERRED_SIZE,
															483,
															javax.swing.GroupLayout.PREFERRED_SIZE)
														.addComponent(txtMasterPw,
															javax.swing.GroupLayout.Alignment.TRAILING,
															javax.swing.GroupLayout.PREFERRED_SIZE,
															483,
															javax.swing.GroupLayout.PREFERRED_SIZE)
														.addComponent(
															txtRepeatPw,
															javax.swing.GroupLayout.Alignment.TRAILING,
															javax.swing.GroupLayout.PREFERRED_SIZE,
															483,
															javax.swing.GroupLayout.PREFERRED_SIZE))
													.addPreferredGap(
														javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
													.addGroup(layout.createParallelGroup(
														javax.swing.GroupLayout.Alignment.TRAILING,
														false)
														.addComponent(btnGeneratePw,
															javax.swing.GroupLayout.DEFAULT_SIZE,
															101, Short.MAX_VALUE)
														.addComponent(btnMasterPw,
															javax.swing.GroupLayout.DEFAULT_SIZE,
															javax.swing.GroupLayout.DEFAULT_SIZE,
															Short.MAX_VALUE)
														.addComponent(btnApplicationFileChooser,
															javax.swing.GroupLayout.DEFAULT_SIZE,
															javax.swing.GroupLayout.DEFAULT_SIZE,
															Short.MAX_VALUE))))
											.addGap(12, 12, 12)))
								.addContainerGap())
						.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout
							.createSequentialGroup()
							.addComponent(btnHelp, javax.swing.GroupLayout.PREFERRED_SIZE, 122,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(btnOk, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
							.addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 141,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addGap(22, 22, 22)))));
		layout
			.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
					.addComponent(lblImageHeader, javax.swing.GroupLayout.PREFERRED_SIZE, 80,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(txtApplicationFile, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(btnApplicationFileChooser).addComponent(lblApplicationFile,
							javax.swing.GroupLayout.PREFERRED_SIZE, 35,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(10, 10, 10)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(btnMasterPw)
						.addComponent(txtMasterPw, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(cbxMasterPw))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblRepeatPw)
						.addComponent(txtRepeatPw, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(btnGeneratePw))
					.addGap(52, 52, 52)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(cmbKeyFile, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(cbxKeyFile))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(btnKeyFileChooser).addComponent(btnCreateKeyFile))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 81,
						Short.MAX_VALUE)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(btnOk).addComponent(btnCancel).addComponent(btnHelp))
					.addGap(42, 42, 42)));
	}// </editor-fold>//GEN-END:initComponents

	private void cbxMasterPwActionPerformed(java.awt.event.ActionEvent evt)
	{// GEN-FIRST:event_cbxMasterPwActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_cbxMasterPwActionPerformed
		// End of variables declaration//GEN-END:variables
}
