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
package io.github.astrapi69.mystic.crypt.plugin.kem;

import java.awt.Font;
import java.util.HexFormat;

import javax.swing.*;

import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;

/**
 * Tool panel demonstrating a key-encapsulation exchange between two parties. The recipient generates
 * a key pair; the sender uses only the recipient's public key to encapsulate a fresh shared secret,
 * producing a ciphertext; the recipient decapsulates that ciphertext with its private key and
 * recovers the same shared secret. Both derived secrets are shown side by side and compared.
 * <p>
 * The pure ML-KEM modes use the NIST post-quantum key-encapsulation mechanism; the hybrid mode
 * combines classical X25519 key agreement with ML-KEM, so the exchange stays secure as long as
 * either building block does. All of it runs on the JDK 25 native crypto providers - see
 * {@link NativeKemExchange}.
 */
public class KemDemoPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private static final String ML_KEM_512 = "ML-KEM-512";
	private static final String ML_KEM_768 = "ML-KEM-768";
	private static final String ML_KEM_1024 = "ML-KEM-1024";
	private static final String HYBRID = "Hybrid X25519 + ML-KEM-768";

	/** The mechanisms this tool offers, in the order the combo box shows them */
	public static final java.util.List<String> ALGORITHMS = java.util.List.of(ML_KEM_768,
		ML_KEM_512, ML_KEM_1024, HYBRID);

	/** The label of a row whose field is a text area belongs at the top of that row */
	private static final String AREA_LABEL = "aligny top";

	/**
	 * The ciphertext is by far the longest of the three values, so its row takes twice the height a
	 * larger window has left over
	 */
	private static final String CIPHERTEXT_ROW = "grow, pushx, pushy 200";

	/** A shared secret is a single line of hex, so its row takes half of what the ciphertext takes */
	private static final String SECRET_ROW = "grow, pushx, pushy 100";

	private static final HexFormat HEX = HexFormat.of();

	private final JComboBox<String> cmbAlgorithm = new JComboBox<>(
		ALGORITHMS.toArray(new String[0]));
	private final JTextArea txtCiphertext = new JTextArea(3, 46);
	private final JTextArea txtSenderSecret = new JTextArea(2, 46);
	private final JTextArea txtRecipientSecret = new JTextArea(2, 46);
	private final JLabel lblResult = new JLabel(" ");

	public KemDemoPanel()
	{
		// one layout for every tool window, so this one looks like the one next to it: labels in a
		// narrow right aligned column, fields taking the width, the areas taking the height that is
		// left, buttons under what they act on
		super(ToolForm.newLayout());

		cmbAlgorithm.setName("cmbAlgorithm");
		// the tool starts with what the user configured in the settings dialog
		cmbAlgorithm.setSelectedItem(KemSettingsContribution.algorithm());
		configureReadOnly(txtCiphertext, "txtCiphertext");
		configureReadOnly(txtSenderSecret, "txtSenderSecret");
		configureReadOnly(txtRecipientSecret, "txtRecipientSecret");
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));

		JButton btnRun = new JButton("Run key exchange");
		btnRun.setName("btnRun");
		btnRun.addActionListener(event -> onRun());

		add(new JLabel("Algorithm:"));
		add(cmbAlgorithm, ToolForm.FIELD);
		add(ToolForm.buttons(btnRun), ToolForm.BUTTON_ROW);

		add(new JLabel("Ciphertext (sender to recipient):"), AREA_LABEL);
		add(ToolForm.scrolled(txtCiphertext), CIPHERTEXT_ROW);
		add(new JLabel("Sender shared secret:"), AREA_LABEL);
		add(ToolForm.scrolled(txtSenderSecret), SECRET_ROW);
		add(new JLabel("Recipient shared secret:"), AREA_LABEL);
		add(ToolForm.scrolled(txtRecipientSecret), SECRET_ROW);
		add(lblResult, ToolForm.RESULT_LINE);
	}

	private void onRun()
	{
		try
		{
			String selection = (String)cmbAlgorithm.getSelectedItem();
			NativeKemExchange.Result result = HYBRID.equals(selection) ? NativeKemExchange.hybrid()
				: NativeKemExchange.mlKem(selection);

			txtCiphertext.setText(HEX.formatHex(result.getCiphertext()));
			txtSenderSecret.setText(HEX.formatHex(result.getSenderSecret()));
			txtRecipientSecret.setText(HEX.formatHex(result.getRecipientSecret()));
			lblResult.setText(
				result.secretsMatch() ? "shared secrets match" : "shared secrets do not match");
		}
		catch (Exception exception)
		{
			txtCiphertext.setText("");
			txtSenderSecret.setText("");
			txtRecipientSecret.setText("");
			lblResult.setText("error: " + exception.getMessage());
		}
	}

	private static void configureReadOnly(JTextArea textArea, String name)
	{
		textArea.setName(name);
		textArea.setEditable(false);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(false);
		textArea.setFont(new Font("monospaced", Font.PLAIN, 12));
	}
}
