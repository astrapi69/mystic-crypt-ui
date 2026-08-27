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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HexFormat;

import javax.swing.*;

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

	private static final HexFormat HEX = HexFormat.of();

	private final JComboBox<String> cmbAlgorithm = new JComboBox<>(
		ALGORITHMS.toArray(new String[0]));
	private final JTextArea txtCiphertext = new JTextArea(3, 46);
	private final JTextArea txtSenderSecret = new JTextArea(2, 46);
	private final JTextArea txtRecipientSecret = new JTextArea(2, 46);
	private final JLabel lblResult = new JLabel(" ");

	public KemDemoPanel()
	{
		super(new GridBagLayout());

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

		int row = 0;
		add(new JLabel("Algorithm:"), at(0, row, GridBagConstraints.EAST));
		add(cmbAlgorithm, stretched(1, row++));
		add(btnRun, at(1, row++, GridBagConstraints.WEST));
		add(new JLabel("Ciphertext (sender to recipient):"),
			at(0, row, GridBagConstraints.NORTHEAST));
		add(KemPanelLayout.scrolled(txtCiphertext), growing(1, row++, 2.0));
		add(new JLabel("Sender shared secret:"), at(0, row, GridBagConstraints.NORTHEAST));
		add(KemPanelLayout.scrolled(txtSenderSecret), growing(1, row++, 1.0));
		add(new JLabel("Recipient shared secret:"), at(0, row, GridBagConstraints.NORTHEAST));
		add(KemPanelLayout.scrolled(txtRecipientSecret), growing(1, row++, 1.0));
		add(lblResult, at(1, row, GridBagConstraints.WEST));
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

	private static GridBagConstraints at(int x, int y, int anchor)
	{
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.anchor = anchor;
		constraints.insets = new Insets(4, 6, 4, 6);
		return constraints;
	}

	/**
	 * The constraints of the column that holds the entry components. That column takes whatever
	 * width the labels leave over, so a field grows with the window and shrinks with it instead of
	 * collapsing to a few pixels. Labels and buttons keep their anchor.
	 *
	 * @param x
	 *            the column of the cell
	 * @param y
	 *            the row of the cell
	 * @return the constraints for that cell
	 */
	private static GridBagConstraints stretched(int x, int y)
	{
		GridBagConstraints constraints = at(x, y, GridBagConstraints.WEST);
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1.0;
		return constraints;
	}

	/**
	 * A row that grows with the window, for the areas that hold something worth reading.
	 * <p>
	 * Without a vertical weight the extra height of a larger window goes nowhere and the area keeps
	 * the rows it was built with, which is why the ciphertext used to show two lines in a window
	 * with room for twenty.
	 *
	 * @param x
	 *            the column
	 * @param y
	 *            the row
	 * @param share
	 *            how much of the free height this row takes, relative to the other growing rows
	 * @return the constraints
	 */
	private static GridBagConstraints growing(int x, int y, double share)
	{
		GridBagConstraints constraints = stretched(x, y);
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weighty = share;
		return constraints;
	}
}
