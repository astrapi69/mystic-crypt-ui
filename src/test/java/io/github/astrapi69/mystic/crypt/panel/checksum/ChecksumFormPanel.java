/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package io.github.astrapi69.mystic.crypt.panel.checksum;

/**
 *
 * @author astrapi69
 */
public class ChecksumFormPanel extends javax.swing.JPanel
{

	/**
	 * Creates new form NewJPanel
	 */
	public ChecksumFormPanel()
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

		txtOpenFile = new javax.swing.JTextField();
		btnOpenFile = new javax.swing.JButton();
		btnClearOpenFile = new javax.swing.JButton();
		lblGeneratedChecksum = new javax.swing.JLabel();
		srcGeneratedChecksum = new javax.swing.JScrollPane();
		txtGeneratedChecksum = new javax.swing.JTextArea();
		lblOwnersChecksum = new javax.swing.JLabel();
		txtChecksumFile = new javax.swing.JTextField();
		btnOpenChecksumFile = new javax.swing.JButton();
		btnClearChecksumFile = new javax.swing.JButton();
		srcOwnersChecksum = new javax.swing.JScrollPane();
		txtOwnersChecksum = new javax.swing.JTextArea();
		lblChecksumAlgorithm = new javax.swing.JLabel();
		cbxChecksumAlgorithm = new javax.swing.JComboBox<>();
		btnCompare = new javax.swing.JButton();
		txtChecksumMatchResult = new javax.swing.JTextField();

		txtOpenFile.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				txtOpenFileActionPerformed(evt);
			}
		});

		btnOpenFile.setText("Open File to check");

		btnClearOpenFile.setText("Clear");

		lblGeneratedChecksum.setText("Generated checksum");

		txtGeneratedChecksum.setColumns(20);
		txtGeneratedChecksum.setRows(5);
		srcGeneratedChecksum.setViewportView(txtGeneratedChecksum);

		lblOwnersChecksum.setText("Checksum from owner");

		txtChecksumFile.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				txtChecksumFileActionPerformed(evt);
			}
		});

		btnOpenChecksumFile.setText("Open Checksum File");

		btnClearChecksumFile.setText("Clear");

		txtOwnersChecksum.setColumns(20);
		txtOwnersChecksum.setRows(5);
		srcOwnersChecksum.setViewportView(txtOwnersChecksum);

		lblChecksumAlgorithm.setText("Checksum algorithm");

		cbxChecksumAlgorithm.setModel(new javax.swing.DefaultComboBoxModel<>(
			new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

		btnCompare.setText("Compare");

		txtChecksumMatchResult.setText("Checksum Match Result");

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addGap(30, 30, 30).addGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addGroup(layout
					.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addComponent(lblOwnersChecksum, javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addGroup(layout.createSequentialGroup().addGroup(layout
						.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
						.addComponent(lblGeneratedChecksum,
							javax.swing.GroupLayout.Alignment.LEADING,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addGroup(layout.createSequentialGroup()
							.addComponent(txtOpenFile, javax.swing.GroupLayout.PREFERRED_SIZE, 780,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addGap(18, 18, 18)
							.addComponent(btnOpenFile, javax.swing.GroupLayout.PREFERRED_SIZE, 240,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addGap(18, 18, 18).addComponent(btnClearOpenFile))
						.addComponent(srcGeneratedChecksum,
							javax.swing.GroupLayout.Alignment.LEADING))
						.addGap(0, 0, Short.MAX_VALUE)))
					.addGap(30, 30, 30))
				.addGroup(layout.createSequentialGroup()
					.addComponent(lblChecksumAlgorithm, javax.swing.GroupLayout.PREFERRED_SIZE, 199,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGap(0, 0, Short.MAX_VALUE))
				.addGroup(layout.createSequentialGroup().addGroup(layout
					.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
					.addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout
						.createSequentialGroup()
						.addComponent(cbxChecksumAlgorithm, javax.swing.GroupLayout.PREFERRED_SIZE,
							240, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addGap(18, 18, 18)
						.addComponent(btnCompare, javax.swing.GroupLayout.PREFERRED_SIZE, 180,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addGap(18, 18, 18).addComponent(txtChecksumMatchResult))
					.addComponent(srcOwnersChecksum, javax.swing.GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup()
						.addComponent(txtChecksumFile, javax.swing.GroupLayout.PREFERRED_SIZE, 780,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addGap(18, 18, 18)
						.addComponent(btnOpenChecksumFile, javax.swing.GroupLayout.PREFERRED_SIZE,
							240, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addGap(18, 18, 18).addComponent(btnClearChecksumFile)))
					.addContainerGap(42, Short.MAX_VALUE)))));
		layout
			.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addGap(30, 30, 30)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(btnOpenFile)
						.addComponent(txtOpenFile, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(btnClearOpenFile))
					.addGap(18, 18, 18).addComponent(lblGeneratedChecksum).addGap(26, 26, 26)
					.addComponent(srcGeneratedChecksum, javax.swing.GroupLayout.PREFERRED_SIZE, 58,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18).addComponent(lblOwnersChecksum).addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(btnOpenChecksumFile).addComponent(btnClearChecksumFile)
						.addComponent(txtChecksumFile, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(18, 18, 18)
					.addComponent(srcOwnersChecksum, javax.swing.GroupLayout.PREFERRED_SIZE, 60,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18).addComponent(lblChecksumAlgorithm).addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(cbxChecksumAlgorithm, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(btnCompare).addComponent(txtChecksumMatchResult,
							javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addContainerGap(33, Short.MAX_VALUE)));
	}// </editor-fold>//GEN-END:initComponents

	private void txtOpenFileActionPerformed(java.awt.event.ActionEvent evt)
	{// GEN-FIRST:event_txtOpenFileActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_txtOpenFileActionPerformed

	private void txtChecksumFileActionPerformed(java.awt.event.ActionEvent evt)
	{// GEN-FIRST:event_txtChecksumFileActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_txtChecksumFileActionPerformed


	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton btnClearChecksumFile;
	private javax.swing.JButton btnClearOpenFile;
	private javax.swing.JButton btnCompare;
	private javax.swing.JButton btnOpenChecksumFile;
	private javax.swing.JButton btnOpenFile;
	private javax.swing.JComboBox<String> cbxChecksumAlgorithm;
	private javax.swing.JLabel lblChecksumAlgorithm;
	private javax.swing.JLabel lblGeneratedChecksum;
	private javax.swing.JLabel lblOwnersChecksum;
	private javax.swing.JScrollPane srcGeneratedChecksum;
	private javax.swing.JScrollPane srcOwnersChecksum;
	private javax.swing.JTextField txtChecksumFile;
	private javax.swing.JTextField txtChecksumMatchResult;
	private javax.swing.JTextArea txtGeneratedChecksum;
	private javax.swing.JTextField txtOpenFile;
	private javax.swing.JTextArea txtOwnersChecksum;
	// End of variables declaration//GEN-END:variables
}
