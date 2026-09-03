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
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.component.JMCheckBox;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMTextArea;
import io.github.astrapi69.swing.model.component.JMTextField;
import net.miginfocom.swing.MigLayout;

/**
 * Tool panel for signing and verifying, with the classical Ed25519 and RSA/ECDSA/DSA algorithms and
 * with the NIST post-quantum ML-DSA and SLH-DSA parameter sets.
 * <p>
 * The keys can come from two places. "Generate key pair" makes a throwaway pair for trying an
 * algorithm out - useful to see that an SLH-DSA signature is tens of kilobytes where an Ed25519 one
 * is 64 bytes. The other way is the one that matters in practice: a private key from a file to sign
 * with, and a public key - or the certificate carrying it - to verify against. What is signed is
 * either the text in the field or a file on disk.
 * <p>
 * The state of the panel lives in a {@link PqcSignaturePanelModel}: every component is bound to one
 * of its fields, so an action reads what the user entered from the model instead of out of the
 * widgets.
 */
public class PqcSignaturePanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** What the result line shows as long as nothing was done */
	private static final String NO_RESULT_YET = " ";

	/** The label of a text area belongs at the top of the area, not in the middle of it */
	private static final String AREA_LABEL = "aligny top";

	/** A text area takes its share of the height the window has left over */
	private static final String GROWING_AREA = "grow, push";

	private final PqcSignaturePanelModel modelObject = new PqcSignaturePanelModel();

	private final JMComboBox<String, DefaultComboBoxModel<String>> cmbAlgorithm = new JMComboBox<>(
		SignatureSupport.allAlgorithms().toArray(new String[0]));
	private final JMTextArea txtMessage = new JMTextArea(3, 60);
	private final JMTextField txtDataFile = new JMTextField(40);
	private final JMCheckBox chkUseFile = new JMCheckBox("sign the file instead of the text");
	private final JMTextField txtPrivateKeyFile = new JMTextField(40);
	private final JMTextField txtPublicKeyFile = new JMTextField(40);
	private final JMTextField txtSignatureFile = new JMTextField(40);
	private final JMTextArea txtPublicKey = new JMTextArea(4, 60);
	private final JMTextArea txtSignature = new JMTextArea(5, 60);
	private final JLabel lblResult = new JLabel(NO_RESULT_YET);

	public PqcSignaturePanel()
	{
		// one layout for every tool window, so this one looks like the one next to it: labels in a
		// narrow right aligned column, fields taking the width, the text areas taking the height
		// that is left, buttons under what they act on
		super(ToolForm.newLayout());

		configureComponents();
		bindComponentsToModel();
		layoutComponents();
	}

	private void configureComponents()
	{
		cmbAlgorithm.setName("cmbAlgorithm");
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
		ToolForm.sized(txtDataFile);
		ToolForm.sized(txtPrivateKeyFile);
		ToolForm.sized(txtPublicKeyFile);
		ToolForm.sized(txtSignatureFile);

		cmbAlgorithm.setToolTipText(SignatureMessages.getString("signature.tooltip.algorithm",
			"the signature algorithm - Ed25519 and RSA/ECDSA/DSA are classical, ML-DSA and "
				+ "SLH-DSA are post-quantum; a SLH-DSA signature runs tens of kilobytes where an "
				+ "Ed25519 one is 64 bytes"));
		txtMessage.setToolTipText(SignatureMessages.getString("signature.tooltip.message",
			"the text to sign or verify, used instead of the file below unless checked"));
		txtDataFile.setToolTipText(SignatureMessages.getString("signature.tooltip.data.file",
			"the file to sign or verify, used instead of the text above when checked"));
		chkUseFile.setToolTipText(SignatureMessages.getString("signature.tooltip.use.file",
			"sign or verify the file above instead of the typed text"));
		txtPrivateKeyFile.setToolTipText(SignatureMessages.getString(
			"signature.tooltip.private.key.file",
			"the private key file to sign with - leave blank to use a key pair generated here"));
		txtPublicKeyFile.setToolTipText(SignatureMessages.getString(
			"signature.tooltip.public.key.file",
			"the public key or certificate file to verify against - leave blank to use a key "
				+ "pair generated here"));
		txtSignatureFile.setToolTipText(SignatureMessages.getString(
			"signature.tooltip.signature.file", "the file the signature is saved to or loaded from"));
		txtPublicKey.setToolTipText(SignatureMessages.getString("signature.tooltip.public.key",
			"the public key, in PEM format"));
		txtSignature.setToolTipText(SignatureMessages.getString("signature.tooltip.signature",
			"the signature, Base64 encoded"));
	}

	/**
	 * Binds every component to its field in the model. The algorithm and the message start with
	 * what the user configured in the settings dialog
	 */
	private void bindComponentsToModel()
	{
		modelObject.setAlgorithm(SignatureSettingsContribution.algorithm());
		modelObject.setMessage(SignatureSettingsContribution.message());

		cmbAlgorithm.setPropertyModel(
			LambdaModel.of(modelObject::getAlgorithm, modelObject::setAlgorithm));
		txtMessage
			.setPropertyModel(LambdaModel.of(modelObject::getMessage, modelObject::setMessage));
		txtDataFile
			.setPropertyModel(LambdaModel.of(modelObject::getDataFile, modelObject::setDataFile));
		chkUseFile
			.setPropertyModel(LambdaModel.of(modelObject::isUseFile, modelObject::setUseFile));
		txtPrivateKeyFile.setPropertyModel(
			LambdaModel.of(modelObject::getPrivateKeyFile, modelObject::setPrivateKeyFile));
		txtPublicKeyFile.setPropertyModel(
			LambdaModel.of(modelObject::getPublicKeyFile, modelObject::setPublicKeyFile));
		txtSignatureFile.setPropertyModel(
			LambdaModel.of(modelObject::getSignatureFile, modelObject::setSignatureFile));
		txtPublicKey
			.setPropertyModel(LambdaModel.of(modelObject::getPublicKey, modelObject::setPublicKey));
		txtSignature
			.setPropertyModel(LambdaModel.of(modelObject::getSignature, modelObject::setSignature));
		showResult(NO_RESULT_YET);
	}

	private void layoutComponents()
	{
		add(new JLabel("Algorithm:"));
		add(cmbAlgorithm, ToolForm.FIELD);
		add(ToolForm.buttons(button("btnGenerate", "Generate key pair", event -> onGenerate(),
			SignatureMessages.getString("signature.tooltip.generate",
				"generates a throwaway key pair for the selected algorithm - only Ed25519, "
					+ "ML-DSA and SLH-DSA can be generated here, RSA/ECDSA/DSA need a key file"))),
			ToolForm.BUTTON_ROW);

		add(new JLabel("Private key file:"));
		add(fileRow(txtPrivateKeyFile,
			button("btnBrowsePrivateKey", "...", event -> onBrowse(txtPrivateKeyFile),
				SignatureMessages.getString("signature.tooltip.browse.private.key",
					"choose the private key file"))),
			ToolForm.FIELD);
		add(new JLabel("Public key or certificate:"));
		add(fileRow(txtPublicKeyFile,
			button("btnBrowsePublicKey", "...", event -> onBrowse(txtPublicKeyFile),
				SignatureMessages.getString("signature.tooltip.browse.public.key",
					"choose the public key or certificate file"))),
			ToolForm.FIELD);

		add(new JLabel("Public key:"), AREA_LABEL);
		add(ToolForm.scrolled(txtPublicKey), GROWING_AREA);

		add(new JLabel("Message:"), AREA_LABEL);
		add(ToolForm.scrolled(txtMessage), GROWING_AREA);
		add(new JLabel("File to sign:"));
		add(fileRow(txtDataFile,
			button("btnBrowseDataFile", "...", event -> onBrowse(txtDataFile),
				SignatureMessages.getString("signature.tooltip.browse.data.file",
					"choose the file to sign or verify"))),
			ToolForm.FIELD);
		add(chkUseFile, "skip 1, growx");

		add(new JLabel("Signature (Base64):"), AREA_LABEL);
		add(ToolForm.scrolled(txtSignature), GROWING_AREA);
		add(new JLabel("Signature file:"));
		add(fileRow(txtSignatureFile,
			button("btnBrowseSignatureFile", "...", event -> onBrowse(txtSignatureFile),
				SignatureMessages.getString("signature.tooltip.browse.signature.file",
					"choose the signature file"))),
			ToolForm.FIELD);

		add(ToolForm.buttons(
			button("btnSign", "Sign", event -> onSign(),
				SignatureMessages.getString("signature.tooltip.sign",
					"signs the message or file above with the private key")),
			button("btnVerify", "Verify", event -> onVerify(),
				SignatureMessages.getString("signature.tooltip.verify",
					"checks the signature against the message or file above, with the public "
						+ "key")),
			button("btnSaveSignature", "Save signature", event -> onSaveSignature(),
				SignatureMessages.getString("signature.tooltip.save.signature",
					"writes the signature to the signature file above")),
			button("btnLoadSignature", "Load signature", event -> onLoadSignature(),
				SignatureMessages.getString("signature.tooltip.load.signature",
					"reads the signature from the signature file above"))),
			ToolForm.BUTTON_ROW);
		add(lblResult, ToolForm.RESULT_LINE);
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
			KeyPair generatedKeyPair = SignatureSupport.newKeyPair(algorithm);
			long took = System.currentTimeMillis() - started;
			modelObject.setKeyPair(generatedKeyPair);
			showPublicKey(PublicKeyExtensions.toPemFormat(generatedKeyPair.getPublic()));
			showSignature("");
			return "generated a " + algorithm + " key pair in " + took + " ms";
		});
	}

	private void onSign()
	{
		run("signed", () -> {
			PrivateKey privateKey = privateKey();
			String algorithm = algorithmFor(privateKey);
			byte[] dataToSign = dataToSign();
			long started = System.currentTimeMillis();
			byte[] signature = SignatureSupport.sign(algorithm, privateKey, dataToSign);
			long took = System.currentTimeMillis() - started;
			showSignature(Base64.getEncoder().encodeToString(signature));
			return "signed " + dataToSign.length + " bytes with " + algorithm + " in " + took
				+ " ms, signature is " + signature.length + " bytes";
		});
	}

	private void onVerify()
	{
		try
		{
			PublicKey publicKey = publicKey();
			String signature = modelObject.getSignature();
			if (signature.isBlank())
			{
				showResult("there is no signature to check");
				return;
			}
			boolean valid = SignatureSupport.verify(algorithmFor(publicKey), publicKey,
				dataToSign(), Base64.getDecoder().decode(signature.trim()));
			showResult(valid ? "signature is valid" : "signature is not valid");
		}
		catch (Exception exception)
		{
			// a signature that does not belong to the data can also make the verifier throw
			showResult("signature is not valid");
		}
	}

	private void onSaveSignature()
	{
		run("saved", () -> {
			String signature = modelObject.getSignature();
			if (signature.isBlank())
			{
				throw new IllegalArgumentException("there is no signature to save");
			}
			File target = requireFile(modelObject.getSignatureFile(), "a signature file");
			Files.write(target.toPath(), Base64.getDecoder().decode(signature.trim()));
			return "saved the signature to " + target.getName();
		});
	}

	private void onLoadSignature()
	{
		run("loaded", () -> {
			File source = requireFile(modelObject.getSignatureFile(), "a signature file");
			showSignature(Base64.getEncoder().encodeToString(Files.readAllBytes(source.toPath())));
			return "loaded the signature from " + source.getName();
		});
	}

	/**
	 * The private key to sign with: the one from the file when a file was named, otherwise the one
	 * that was generated here
	 */
	private PrivateKey privateKey() throws Exception
	{
		String privateKeyFile = modelObject.getPrivateKeyFile();
		if (!privateKeyFile.isBlank())
		{
			PrivateKey privateKey = SignatureSupport
				.readPrivateKey(new File(privateKeyFile.trim()));
			showPublicKey(describe(privateKey));
			return privateKey;
		}
		KeyPair keyPair = modelObject.getKeyPair();
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
		String publicKeyFile = modelObject.getPublicKeyFile();
		if (!publicKeyFile.isBlank())
		{
			return SignatureSupport.readPublicKey(new File(publicKeyFile.trim()));
		}
		KeyPair keyPair = modelObject.getKeyPair();
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
			? !modelObject.getPrivateKeyFile().isBlank()
			: !modelObject.getPublicKeyFile().isBlank();
		return keyCameFromAFile ? SignatureSupport.algorithmFor(key) : selectedAlgorithm();
	}

	private byte[] dataToSign() throws Exception
	{
		if (modelObject.isUseFile())
		{
			return Files
				.readAllBytes(requireFile(modelObject.getDataFile(), "a file to sign").toPath());
		}
		return modelObject.getMessage().getBytes(StandardCharsets.UTF_8);
	}

	private File requireFile(final String path, final String what)
	{
		if (path.isBlank())
		{
			throw new IllegalArgumentException("choose " + what);
		}
		return new File(path.trim());
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
		return modelObject.getAlgorithm();
	}

	private void run(String what, SignatureOperation operation)
	{
		try
		{
			showResult(operation.execute());
		}
		catch (Exception exception)
		{
			showResult("not " + what + ": " + message(exception));
		}
	}

	/** The message shown at the bottom of the panel */
	public String getResultText()
	{
		return modelObject.getResultText();
	}

	/** Shows the public key; the bound model follows through the document of the area */
	private void showPublicKey(final String publicKeyInPemFormat)
	{
		txtPublicKey.setText(publicKeyInPemFormat);
		txtPublicKey.setCaretPosition(0);
	}

	/** Shows the signature; the bound model follows through the document of the area */
	private void showSignature(final String base64Signature)
	{
		txtSignature.setText(base64Signature);
		txtSignature.setCaretPosition(0);
	}

	/** Shows the result line; a label has no document, so the model is set here */
	private void showResult(final String resultText)
	{
		modelObject.setResultText(resultText);
		lblResult.setText(resultText);
	}

	private void onBrowse(JMTextField target)
	{
		JFileChooser fileChooser = new JFileChooser();
		String chosenBefore = target.getPropertyModel().getObject();
		if (chosenBefore != null && !chosenBefore.isBlank())
		{
			fileChooser.setSelectedFile(new File(chosenBefore));
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

	private static JButton button(String name, String text, java.awt.event.ActionListener listener,
		String tooltip)
	{
		JButton button = new JButton(text);
		button.setName(name);
		button.addActionListener(listener);
		button.setToolTipText(tooltip);
		return button;
	}

	/**
	 * Keeps a file field and the button that opens its file chooser in one row of the form: the
	 * field takes the width the window leaves over, the button stays as wide as its text
	 *
	 * @param fileField
	 *            the field that holds the path
	 * @param browseButton
	 *            the button that opens the file chooser for that field
	 * @return the component that goes into the field cell of the form
	 */
	private static JComponent fileRow(JMTextField fileField, JButton browseButton)
	{
		JPanel row = new JPanel(new MigLayout("insets 0, gap 6", "[grow,fill][]", "[]"));
		row.setOpaque(false);
		row.add(fileField, "growx");
		row.add(browseButton);
		return row;
	}
}
