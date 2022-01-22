/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package io.github.astrapi69.mystic.crypt.panels.dbtree;

/**
 *
 * @author astrapi69
 */
public class AttachementFormPanel extends javax.swing.JPanel
{

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton btnAdd;
	private javax.swing.JButton btnRemove;
	private javax.swing.JButton btnSaveTo;
	private javax.swing.JScrollPane srcFiles;
	private javax.swing.JTable tblFiles;
	/**
	 * Creates new form AttachementFormPanel
	 */
	public AttachementFormPanel()
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

		srcFiles = new javax.swing.JScrollPane();
		tblFiles = new javax.swing.JTable();
		btnAdd = new javax.swing.JButton();
		btnRemove = new javax.swing.JButton();
		btnSaveTo = new javax.swing.JButton();

		tblFiles.setModel(new javax.swing.table.DefaultTableModel(
			new Object[][] { { null, null, null, null }, { null, null, null, null },
					{ null, null, null, null }, { null, null, null, null } },
			new String[] { "Title 1", "Title 2", "Title 3", "Title 4" }));
		srcFiles.setViewportView(tblFiles);

		btnAdd.setText("Add File");
		btnAdd.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				btnAddActionPerformed(evt);
			}
		});

		btnRemove.setText("Remove");

		btnSaveTo.setText("Save to");

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap()
				.addComponent(srcFiles, javax.swing.GroupLayout.PREFERRED_SIZE,
					javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addComponent(btnAdd, javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(btnRemove, javax.swing.GroupLayout.Alignment.TRAILING,
						javax.swing.GroupLayout.DEFAULT_SIZE, 138, Short.MAX_VALUE)
					.addComponent(btnSaveTo, javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				.addContainerGap()));
		layout.setVerticalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
				.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup().addComponent(btnAdd).addGap(18, 18, 18)
						.addComponent(btnRemove).addGap(18, 18, 18).addComponent(btnSaveTo))
					.addComponent(srcFiles, javax.swing.GroupLayout.PREFERRED_SIZE, 428,
						javax.swing.GroupLayout.PREFERRED_SIZE))
				.addContainerGap()));
	}// </editor-fold>//GEN-END:initComponents

	private void btnAddActionPerformed(java.awt.event.ActionEvent evt)
	{// GEN-FIRST:event_btnAddActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_btnAddActionPerformed
	// End of variables declaration//GEN-END:variables
}
