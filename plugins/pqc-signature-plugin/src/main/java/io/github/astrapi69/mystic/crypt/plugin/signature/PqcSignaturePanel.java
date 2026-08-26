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
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import javax.swing.*;

import io.github.astrapi69.crypt.data.key.PrivateKeyExtensions;
import io.github.astrapi69.crypt.data.key.PublicKeyExtensions;

/**
 * Tool panel for signing and verifying, with the classical Ed25519 and RSA/ECDSA/DSA algorithms and
 * with the NIST post-quantum ML-DSA and SLH-DSA parameter sets.
 * <p>
 * The keys can come from two places. "Generate key pair" makes a throwaway pair for trying an
 * algorithm out - useful to see that an SLH-DSA signature is tens of kilobytes where an Ed25519 one
 * is 64 bytes. The other way is the one that matters in practice: a private key from a file to sign
 * with, and a public key - or the certificate carrying it - to verify against. What is signed is
 * either the text in the field or a file on disk.
 */
public class PqcSignaturePanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final JComboBox<String> cmbAlgorithm = new JComboBox<>(
		SignatureSupport.allAlgorithms().toArray(new String[0]));
	private final JTextArea txtMessage = new JTextArea(3, 60);
	private final JTextField txtDataFile = new JTextField(40);
	private final JCheckBox chkUseFile = new JCheckBox("sign the file instead of the text");
	private final JTextField txtPrivateKeyFile = new JTextField(40);
	private final JTextField txtPublicKeyFile = new JTextField(40);
	private final JTextField txtSignatureFile = new JTextField(40);
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
		txtDataFile.setName("txtDataFile");
		chkUseFile.setName("chkUseFile");
		txtPrivateKeyFile.setName("txtPrivateKeyFile");
		txtPublicKeyFile.setName("txtPublicKeyFile");
		txtSignatureFile.setName("txtSignatureFile");
		configureReadOnly(txtPublicKey, "txtPublicKey");
		configureReadOnly(txtSignature, "txtSignature");
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));

		int row = 0;
		add(new JLabel("Algorithm:"), at(0, row, GridBagConstraints.EAST));
		add(cmbAlgorithm, at(1, row, GridBagConstraints.WEST));
		add(button("btnGenerate", "Generate key pair", event -> onGenerate()),
			at(2, row++, GridBagConstraints.WEST));

		add(new JLabel("Private key file:"), at(0, row, GridBagConstraints.EAST));
		add(txtPrivateKeyFile, at(1, row, GridBagConstraints.WEST));
		add(button("btnBrowsePrivateKey", "...", event -> onBrowse(txtPrivateKeyFile)),
			at(2, row++, GridBagConstraints.WEST));
		add(new JLabel("Public key or certificate:"), at(0, row, GridBagConstraints.EAST));
		add(txtPublicKeyFile, at(1, row, GridBagConstraints.WEST));
		add(button("btnBrowsePublicKey", "...", event -> onBrowse(txtPublicKeyFile)),
			at(2, row++, GridBagConstraints.WEST));

		add(new JLabel("Public key:"), at(0, row, GridBagConstraints.NORTHEAST));
		add(new JScrollPane(txtPublicKey), at(1, row++, GridBagConstraints.WEST));

		add(new JLabel("Message:"), at(0, row, GridBagConstraints.NORTHEAST));
		add(new JScrollPane(txtMessage), at(1, row++, GridBagConstraints.WEST));
		add(new JLabel("File to sign:"), at(0, row, GridBagConstraints.EAST));
		add(txtDataFile, at(1, row, GridBagConstraints.WEST));
		add(button("btnBrowseDataFile", "...", event -> onBrowse(txtDataFile)),
			at(2, row++, GridBagConstraints.WEST));
		add(chkUseFile, at(1, row++, GridBagConstraints.WEST));

		add(new JLabel("Signature (Base64):"), at(0, row, GridBagConstraints.NORTHEAST));
		add(new JScrollPane(txtSignature), at(1, row++, GridBagConstraints.WEST));
		add(new JLabel("Signature file:"), at(0, row, GridBagConstraints.EAST));
		add(txtSignatureFile, at(1, row, GridBagConstraints.WEST));
		add(button("btnBrowseSignatureFile", "...", event -> onBrowse(txtSignatureFile)),
			at(2, row++, GridBagConstraints.WEST));

		add(buttonRow(button("btnSign", "Sign", event -> onSign()),
			button("btnVerify", "Verify", event -> onVerify()),
			button("btnSaveSignature", "Save signature", event -> onSaveSignature()),
			button("btnLoadSignature", "Load signature", event -> onLoadSignature())),
			at(1, row++, GridBagConstraints.WEST));
		add(lblResult, at(1, row, GridBagConstraints.WEST));
	}

	private void onGenerate()
	{
		String algorithm = selectedAlgorithm();
		run("generated", () -> {
			if (!SignatureSupport.canGenerateKeyPair(algorithm))
			{
				throw new IllegalArgumentException("a key for '" + algorithm
					+ "' has to come from a file - this tool generates only the Ed25519, ML-DSA "
					+ "and SLH-DSA families");
			}
			long started = System.currentTimeMillis();
			keyPair = SignatureSupport.newKeyPair(algorithm);
			long took = System.currentTimeMillis() - started;
			txtPublicKey.setText(PublicKeyExtensions.toPemFormat(keyPair.getPublic()));
			txtPublicKey.setCaretPosition(0);
			txtSignature.setText("");
			return "generated a " + algorithm + " key pair in " + took + " ms";
		});
	}

	private void onSign()
	{
		run("signed", () -> {
			PrivateKey privateKey = privateKey();
			String algorithm = algorithmFor(privateKey);
			byte[] data = dataToSign();
			long started = System.currentTimeMillis();
			byte[] signature = SignatureSupport.sign(algorithm, privateKey, data);
			long took = System.currentTimeMillis() - started;
			txtSignature.setText(Base64.getEncoder().encodeToString(signature));
			txtSignature.setCaretPosition(0);
			return "signed " + data.length + " bytes with " + algorithm + " in " + took
				+ " ms, signature is " + signature.length + " bytes";
		});
	}

	private void onVerify()
	{
		try
		{
			PublicKey publicKey = publicKey();
			if (txtSignature.getText().isBlank())
			{
				lblResult.setText("there is no signature to check");
				return;
			}
			byte[] signature = Base64.getDecoder().decode(txtSignature.getText().trim());
			boolean valid = SignatureSupport.verify(algorithmFor(publicKey), publicKey,
				dataToSign(), signature);
			lblResult.setText(valid ? "signature is valid" : "signature is not valid");
		}
		catch (Exception exception)
		{
			// a signature that does not belong to the data can also make the verifier throw
			lblResult.setText("signature is not valid");
		}
	}

	private void onSaveSignature()
	{
		run("saved", () -> {
			if (txtSignature.getText().isBlank())
			{
				throw new IllegalArgumentException("there is no signature to save");
			}
			File target = requireFile(txtSignatureFile, "a signature file");
			Files.write(target.toPath(),
				Base64.getDecoder().decode(txtSignature.getText().trim()));
			return "saved the signature to " + target.getName();
		});
	}

	private void onLoadSignature()
	{
		run("loaded", () -> {
			File source = requireFile(txtSignatureFile, "a signature file");
			txtSignature.setText(
				Base64.getEncoder().encodeToString(Files.readAllBytes(source.toPath())));
			txtSignature.setCaretPosition(0);
			return "loaded the signature from " + source.getName();
		});
	}

	/**
	 * The private key to sign with: the one from the file when a file was named, otherwise the one
	 * that was generated here
	 */
	private PrivateKey privateKey() throws Exception
	{
		if (!txtPrivateKeyFile.getText().isBlank())
		{
			PrivateKey privateKey = SignatureSupport
				.readPrivateKey(new File(txtPrivateKeyFile.getText().trim()));
			txtPublicKey.setText(describe(privateKey));
			return privateKey;
		}
		if (keyPair == null)
		{
			throw new IllegalStateException(
				"choose a private key file, or generate a key pair first");
		}
		return keyPair.getPrivate();
	}

	/**
	 * The public key to verify against: the one from the file when a file was named, otherwise the
	 * one that was generated here
	 */
	private PublicKey publicKey() throws Exception
	{
		if (!txtPublicKeyFile.getText().isBlank())
		{
			return SignatureSupport.readPublicKey(new File(txtPublicKeyFile.getText().trim()));
		}
		if (keyPair == null)
		{
			throw new IllegalStateException(
				"choose a public key or certificate file, or generate a key pair first");
		}
		return keyPair.getPublic();
	}

	/**
	 * The algorithm to use: what the key itself says, unless the key came from here - then the
	 * chosen one decides, because a generated key is generated for that very algorithm
	 */
	private String algorithmFor(final java.security.Key key)
	{
		boolean keyCameFromAFile = key instanceof PrivateKey
			? !txtPrivateKeyFile.getText().isBlank()
			: !txtPublicKeyFile.getText().isBlank();
		return keyCameFromAFile ? SignatureSupport.algorithmFor(key) : selectedAlgorithm();
	}

	private byte[] dataToSign() throws Exception
	{
		if (chkUseFile.isSelected())
		{
			return Files.readAllBytes(requireFile(txtDataFile, "a file to sign").toPath());
		}
		return txtMessage.getText().getBytes(StandardCharsets.UTF_8);
	}

	private File requireFile(final JTextField field, final String what)
	{
		if (field.getText().isBlank())
		{
			throw new IllegalArgumentException("choose " + what);
		}
		return new File(field.getText().trim());
	}

	private String describe(final PrivateKey privateKey)
	{
		try
		{
			return PublicKeyExtensions
				.toPemFormat(PrivateKeyExtensions.generatePublicKey(privateKey));
		}
		catch (Exception exception)
		{
			// not every private key can produce its public counterpart on its own
			return privateKey.getAlgorithm() + " private key loaded from file";
		}
	}

	private String selectedAlgorithm()
	{
		return String.valueOf(cmbAlgorithm.getSelectedItem());
	}

	private void run(String what, SignatureOperation operation)
	{
		try
		{
			lblResult.setText(operation.execute());
		}
		catch (Exception exception)
		{
			lblResult.setText("not " + what + ": " + message(exception));
		}
	}

	/** The message shown at the bottom of the panel */
	public String getResultText()
	{
		return lblResult.getText();
	}

	private void onBrowse(JTextField target)
	{
		JFileChooser fileChooser = new JFileChooser();
		if (!target.getText().isBlank())
		{
			fileChooser.setSelectedFile(new File(target.getText()));
		}
		if (fileChooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION)
		{
			target.setText(fileChooser.getSelectedFile().getAbsolutePath());
		}
	}

	private static String message(Exception exception)
	{
		return exception.getMessage() != null
			? exception.getMessage()
			: exception.getClass().getSimpleName();
	}

	@FunctionalInterface
	private interface SignatureOperation
	{
		String execute() throws Exception;
	}

	private static void configureReadOnly(JTextArea textArea, String name)
	{
		textArea.setName(name);
		textArea.setEditable(false);
		textArea.setLineWrap(true);
	}

	private static JButton button(String name, String text, java.awt.event.ActionListener listener)
	{
		JButton button = new JButton(text);
		button.setName(name);
		button.addActionListener(listener);
		return button;
	}

	private static JPanel buttonRow(JButton... buttons)
	{
		JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
		for (JButton button : buttons)
		{
			panel.add(button);
		}
		return panel;
	}

	private static GridBagConstraints at(int column, int row, int anchor)
	{
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = column;
		constraints.gridy = row;
		constraints.anchor = anchor;
		constraints.insets = new Insets(4, 4, 4, 4);
		return constraints;
	}
}
