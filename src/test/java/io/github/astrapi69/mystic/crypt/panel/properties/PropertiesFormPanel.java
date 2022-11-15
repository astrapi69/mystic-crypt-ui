/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package io.github.astrapi69.mystic.crypt.panel.properties;

/**
 *
 * @author astrapi69
 */
public class PropertiesFormPanel extends javax.swing.JPanel
{

	/**
	 * Creates new form PropertiesFormPanel
	 */
	public PropertiesFormPanel()
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

		srcProperties = new javax.swing.JScrollPane();
		tblProperties = new javax.swing.JTable();
		btnAdd = new javax.swing.JButton();
		btnRemove = new javax.swing.JButton();
		btnEdit = new javax.swing.JButton();

		tblProperties.setModel(new javax.swing.table.DefaultTableModel(
			new Object[][] { { null, null }, { null, null }, { null, null }, { null, null } },
			new String[] { "Key", "Value" }));
		srcProperties.setViewportView(tblProperties);

		btnAdd.setText("Add Property");
		btnAdd.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				btnAddActionPerformed(evt);
			}
		});

		btnRemove.setText("Remove");

		btnEdit.setText("Edit");

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap()
				.addComponent(srcProperties, javax.swing.GroupLayout.PREFERRED_SIZE,
					javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addComponent(btnAdd, javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(btnRemove, javax.swing.GroupLayout.Alignment.TRAILING,
						javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
					.addComponent(btnEdit, javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				.addContainerGap()));
		layout.setVerticalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
				.addContainerGap(42, Short.MAX_VALUE)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addComponent(srcProperties, javax.swing.GroupLayout.PREFERRED_SIZE, 172,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGroup(layout.createSequentialGroup().addComponent(btnAdd).addGap(18, 18, 18)
						.addComponent(btnRemove).addGap(18, 18, 18).addComponent(btnEdit)))
				.addContainerGap()));
	}// </editor-fold>//GEN-END:initComponents

	private void btnAddActionPerformed(java.awt.event.ActionEvent evt)
	{// GEN-FIRST:event_btnAddActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_btnAddActionPerformed


	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton btnAdd;
	private javax.swing.JButton btnEdit;
	private javax.swing.JButton btnRemove;
	private javax.swing.JScrollPane srcProperties;
	private javax.swing.JTable tblProperties;
	// End of variables declaration//GEN-END:variables
}
