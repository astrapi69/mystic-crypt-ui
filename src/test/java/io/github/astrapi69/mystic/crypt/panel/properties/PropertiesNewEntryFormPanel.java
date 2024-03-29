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
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package io.github.astrapi69.mystic.crypt.panel.properties;

/**
 *
 * @author astrapi69
 */
public class PropertiesNewEntryFormPanel extends javax.swing.JPanel
{

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JLabel lblCreateNewEntry;
	private javax.swing.JLabel lblKey;
	private javax.swing.JLabel lblValue;
	private javax.swing.JTextField txtKey;
	private javax.swing.JTextField txtValue;

	/**
	 * Creates new form PropertiesNewEntryFormPanel
	 */
	public PropertiesNewEntryFormPanel()
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

		lblKey = new javax.swing.JLabel();
		lblValue = new javax.swing.JLabel();
		txtKey = new javax.swing.JTextField();
		txtValue = new javax.swing.JTextField();
		lblCreateNewEntry = new javax.swing.JLabel();

		lblKey.setText("Key");

		lblValue.setText("Value");

		txtKey.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				txtKeyActionPerformed(evt);
			}
		});

		lblCreateNewEntry.setText("Create new entry");

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout
					.createParallelGroup(
						javax.swing.GroupLayout.Alignment.LEADING)
					.addGroup(
						layout.createSequentialGroup()
							.addGroup(layout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
								.addComponent(lblValue, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
									javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(lblKey, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
									javax.swing.GroupLayout.PREFERRED_SIZE))
							.addGap(18, 18, 18)
							.addGroup(layout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
								.addComponent(txtKey, javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(txtValue, javax.swing.GroupLayout.Alignment.LEADING,
									javax.swing.GroupLayout.DEFAULT_SIZE, 363, Short.MAX_VALUE))
							.addGap(20, 20, 20))
					.addGroup(layout.createSequentialGroup()
						.addComponent(lblCreateNewEntry, javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addGap(401, 401, 401)))));
		layout
			.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
					.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(lblCreateNewEntry).addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(txtKey, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(lblKey, javax.swing.GroupLayout.PREFERRED_SIZE, 34,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblValue, javax.swing.GroupLayout.PREFERRED_SIZE, 34,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(txtValue, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addContainerGap()));
	}// </editor-fold>//GEN-END:initComponents

	private void btnAddEntryActionPerformed(java.awt.event.ActionEvent evt)
	{// GEN-FIRST:event_btnAddEntryActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_btnAddEntryActionPerformed

	private void txtKeyActionPerformed(java.awt.event.ActionEvent evt)
	{// GEN-FIRST:event_txtKeyActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_txtKeyActionPerformed
		// End of variables declaration//GEN-END:variables
}
