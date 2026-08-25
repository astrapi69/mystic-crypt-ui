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
package io.github.astrapi69.mystic.crypt.plugin.signature;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Base64;

import javax.swing.*;

import io.github.astrapi69.crypt.data.key.PublicKeyExtensions;

/**
 * Tool panel for signing a text and verifying the signature again, with the classical Ed25519 and
 * the NIST post-quantum ML-DSA and SLH-DSA parameter sets.
 * <p>
 * Besides the round trip it reports how long signing took and how large the signature is, because
 * that is where the families differ most: an Ed25519 signature is 64 bytes, an ML-DSA one a few
 * kilobytes, and an SLH-DSA one can reach tens of kilobytes.
 */
public class PqcSignaturePanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final JComboBox<String> cmbAlgorithm = new JComboBox<>(
		SignatureSupport.algorithms().toArray(new String[0]));
	private final JTextArea txtMessage = new JTextArea(3, 60);
	private final JTextArea txtPublicKey = new JTextArea(4, 60);
	private final JTextArea txtSignature = new JTextArea(5, 60);
	private final JLabel lblResult = new JLabel(" ");

	private transient KeyPair keyPair;

	public PqcSignaturePanel()
	{
		super(new GridBagLayout());

		cmbAlgorithm.setName("cmbAlgorithm");
		// the tool starts with what the user configured in the settings dialog
		cmbAlgorithm.setSelectedItem(SignatureSettingsContribution.algorithm());
		txtMessage.setText(SignatureSettingsContribution.message());
		txtMessage.setName("txtMessage");
		txtMessage.setLineWrap(true);
		configureReadOnly(txtPublicKey, "txtPublicKey");
		configureReadOnly(txtSignature, "txtSignature");
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));

		JButton btnGenerate = button("btnGenerate", "Generate key pair", event -> onGenerate());
		JButton btnSign = button("btnSign", "Sign", event -> onSign());
		JButton btnVerify = button("btnVerify", "Verify", event -> onVerify());

		int row = 0;
		add(new JLabel("Algorithm:"), at(0, row, GridBagConstraints.EAST));
		add(cmbAlgorithm, at(1, row++, GridBagConstraints.WEST));
		add(btnGenerate, at(1, row++, GridBagConstraints.WEST));
		add(new JLabel("Public key:"), at(0, row, GridBagConstraints.NORTHEAST));
		add(new JScrollPane(txtPublicKey), at(1, row++, GridBagConstraints.WEST));
		add(new JLabel("Message:"), at(0, row, GridBagConstraints.NORTHEAST));
		add(new JScrollPane(txtMessage), at(1, row++, GridBagConstraints.WEST));
		add(btnSign, at(1, row++, GridBagConstraints.WEST));
		add(new JLabel("Signature (Base64):"), at(0, row, GridBagConstraints.NORTHEAST));
		add(new JScrollPane(txtSignature), at(1, row++, GridBagConstraints.WEST));
		add(btnVerify, at(1, row++, GridBagConstraints.WEST));
		add(lblResult, at(1, row, GridBagConstraints.WEST));
	}

	private void onGenerate()
	{
		String algorithm = String.valueOf(cmbAlgorithm.getSelectedItem());
		try
		{
			long started = System.currentTimeMillis();
			keyPair = SignatureSupport.newKeyPair(algorithm);
			long took = System.currentTimeMillis() - started;
			txtPublicKey.setText(PublicKeyExtensions.toPemFormat(keyPair.getPublic()));
			txtPublicKey.setCaretPosition(0);
			txtSignature.setText("");
			lblResult.setText("generated a " + algorithm + " key pair in " + took + " ms");
		}
		catch (Exception exception)
		{
			keyPair = null;
			txtPublicKey.setText("");
			lblResult.setText("key generation failed: " + exception.getMessage());
		}
	}

	private void onSign()
	{
		if (keyPair == null)
		{
			lblResult.setText("generate a key pair first");
			return;
		}
		String algorithm = String.valueOf(cmbAlgorithm.getSelectedItem());
		try
		{
			byte[] data = txtMessage.getText().getBytes(StandardCharsets.UTF_8);
			long started = System.currentTimeMillis();
			byte[] signature = SignatureSupport.sign(algorithm, keyPair.getPrivate(), data);
			long took = System.currentTimeMillis() - started;
			txtSignature.setText(Base64.getEncoder().encodeToString(signature));
			txtSignature.setCaretPosition(0);
			lblResult.setText(
				"signed in " + took + " ms, signature is " + signature.length + " bytes");
		}
		catch (Exception exception)
		{
			txtSignature.setText("");
			lblResult.setText("signing failed: " + exception.getMessage());
		}
	}

	private void onVerify()
	{
		if (keyPair == null)
		{
			lblResult.setText("generate a key pair first");
			return;
		}
		if (txtSignature.getText().isBlank())
		{
			lblResult.setText("sign a message first");
			return;
		}
		String algorithm = String.valueOf(cmbAlgorithm.getSelectedItem());
		try
		{
			byte[] data = txtMessage.getText().getBytes(StandardCharsets.UTF_8);
			byte[] signature = Base64.getDecoder().decode(txtSignature.getText().trim());
			boolean valid = SignatureSupport.verify(algorithm, keyPair.getPublic(), data, signature);
			lblResult.setText(valid ? "signature is valid" : "signature is not valid");
		}
		catch (Exception exception)
		{
			// a signature that does not belong to the message can also make the verifier throw
			lblResult.setText("signature is not valid");
		}
	}

	private static JButton button(String name, String text, java.awt.event.ActionListener listener)
	{
		JButton button = new JButton(text);
		button.setName(name);
		button.addActionListener(listener);
		return button;
	}

	private static void configureReadOnly(JTextArea textArea, String name)
	{
		textArea.setName(name);
		textArea.setEditable(false);
		textArea.setLineWrap(true);
		textArea.setFont(new Font("monospaced", Font.PLAIN, 12));
	}

	private static GridBagConstraints at(int x, int y, int anchor)
	{
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.anchor = anchor;
		constraints.insets = new Insets(4, 6, 4, 6);
		return constraints;
	}
}
