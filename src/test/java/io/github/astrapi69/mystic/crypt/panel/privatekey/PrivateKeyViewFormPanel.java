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
package io.github.astrapi69.mystic.crypt.panel.privatekey;

/**
 *
 * @author astrapi69
 */
public class PrivateKeyViewFormPanel extends javax.swing.JPanel
{

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JLabel lblKeySize;

	private javax.swing.JLabel lblKeySizeDisplay;


	private javax.swing.JLabel lblPrivateKey;
	private javax.swing.JLabel lblPublicKey;
	private javax.swing.JScrollPane scpPrivateKey;
	private javax.swing.JScrollPane scpPublicKey;
	private javax.swing.JTextArea txtPrivateKey;
	private javax.swing.JTextArea txtPublicKey;

	// End of variables declaration//GEN-END:variables
	/**
	 * Creates new form PrivateKeyViewPanel
	 */
	public PrivateKeyViewFormPanel()
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

		lblKeySize = new javax.swing.JLabel();
		lblKeySizeDisplay = new javax.swing.JLabel();
		lblPrivateKey = new javax.swing.JLabel();
		scpPrivateKey = new javax.swing.JScrollPane();
		txtPrivateKey = new javax.swing.JTextArea();
		lblPublicKey = new javax.swing.JLabel();
		scpPublicKey = new javax.swing.JScrollPane();
		txtPublicKey = new javax.swing.JTextArea();

		lblKeySize.setText("Keysize");

		lblKeySizeDisplay.setText("keysizedisplay-1024");

		lblPrivateKey.setText("Private key");

		txtPrivateKey.setColumns(20);
		txtPrivateKey.setRows(5);
		scpPrivateKey.setViewportView(txtPrivateKey);

		lblPublicKey.setText("Public key");

		txtPublicKey.setColumns(20);
		txtPublicKey.setRows(5);
		scpPublicKey.setViewportView(txtPublicKey);

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout
			.setHorizontalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup().addGap(46, 46, 46)
						.addGroup(layout
							.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
							.addComponent(lblKeySize, javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(lblKeySizeDisplay, javax.swing.GroupLayout.DEFAULT_SIZE,
								128, Short.MAX_VALUE))
						.addGap(33, 33, 33)
						.addGroup(
							layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(lblPrivateKey, javax.swing.GroupLayout.PREFERRED_SIZE,
									179, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(scpPrivateKey, javax.swing.GroupLayout.PREFERRED_SIZE,
									480, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 55,
							Short.MAX_VALUE)
						.addGroup(
							layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(scpPublicKey, javax.swing.GroupLayout.PREFERRED_SIZE,
									480, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(lblPublicKey, javax.swing.GroupLayout.PREFERRED_SIZE,
									165, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addGap(48, 48, 48)));
		layout
			.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addGap(43, 43, 43)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblKeySize).addComponent(lblPrivateKey)
						.addComponent(lblPublicKey))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
					.addGroup(
						layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
							.addComponent(lblKeySizeDisplay)
							.addComponent(scpPrivateKey, javax.swing.GroupLayout.DEFAULT_SIZE, 520,
								Short.MAX_VALUE)
							.addComponent(scpPublicKey))
					.addContainerGap(40, Short.MAX_VALUE)));
	}// </editor-fold>//GEN-END:initComponents
}