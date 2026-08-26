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
package io.github.astrapi69.mystic.crypt.plugin.password;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.*;

import io.github.astrapi69.mystic.crypt.pw.PasswordEncryptor;

/**
 * Tool panel: hash a password with Argon2id or PBKDF2 and verify a password against the produced
 * hash. Hashing and verification are delegated to the mystic-crypt {@link PasswordEncryptor}.
 */
public class PasswordHashPanel extends JPanel
{

	private static final long serialVersionUID = 1L;
	/** The memory hard algorithm, the one to prefer */
	public static final String ARGON2ID = "Argon2id";

	/** The classical alternative, for a system that has to stay interoperable */
	public static final String PBKDF2 = "PBKDF2";

	/** The width below which a text input is no longer usable */
	private static final int MINIMUM_INPUT_WIDTH = 160;

	private final transient PasswordEncryptor passwordEncryptor = PasswordEncryptor.getInstance();

	private final JComboBox<String> cmbAlgorithm = new JComboBox<>(
		PasswordHashSupport.algorithms().toArray(new String[0]));
	private final JPasswordField txtPassword = new JPasswordField(24);
	private final JTextArea txtHash = new JTextArea(3, 40);
	private final JPasswordField txtVerifyPassword = new JPasswordField(24);
	private final JLabel lblResult = new JLabel(" ");
	private final JLabel lblAbout = new JLabel(" ");

	public PasswordHashPanel()
	{
		super(new GridBagLayout());
		cmbAlgorithm.setName("cmbAlgorithm");
		// the tool starts with what the user configured in the settings dialog
		cmbAlgorithm.setSelectedItem(PasswordHashSettingsContribution.algorithm());
		lblAbout.setName("lblAbout");
		lblAbout.setText(PasswordHashSupport
			.describe(String.valueOf(cmbAlgorithm.getSelectedItem())));
		cmbAlgorithm.addActionListener(event -> lblAbout.setText(
			PasswordHashSupport.describe(String.valueOf(cmbAlgorithm.getSelectedItem()))));
		txtPassword.setName("txtPassword");
		txtHash.setName("txtHash");
		txtHash.setEditable(true);
		txtHash.setLineWrap(true);
		txtHash.setWrapStyleWord(false);
		txtVerifyPassword.setName("txtVerifyPassword");
		lblResult.setName("lblResult");

		JScrollPane hashScrollPane = new JScrollPane(txtHash);
		keepUsableWhenNarrow(txtPassword);
		keepUsableWhenNarrow(txtVerifyPassword);
		// a text area reports the minimum of its own content, so the minimum that keeps the
		// hash readable belongs on the scroll pane around it
		keepUsableWhenNarrow(hashScrollPane);

		JButton btnHash = new JButton("Hash");
		btnHash.setName("btnHash");
		btnHash.addActionListener(event -> onHash());
		JButton btnVerify = new JButton("Verify");
		btnVerify.setName("btnVerify");
		btnVerify.addActionListener(event -> onVerify());

		int row = 0;
		add(new JLabel("Algorithm:"), at(0, row, GridBagConstraints.EAST));
		add(cmbAlgorithm, input(row++));
		add(lblAbout, at(1, row++, GridBagConstraints.WEST));
		add(new JLabel("Password:"), at(0, row, GridBagConstraints.EAST));
		add(txtPassword, input(row++));
		add(btnHash, at(1, row++, GridBagConstraints.WEST));
		add(new JLabel("Hash:"), at(0, row, GridBagConstraints.NORTHEAST));
		add(hashScrollPane, input(row++));
		add(new JSeparator(), fullWidth(row++));
		add(new JLabel("Verify password:"), at(0, row, GridBagConstraints.EAST));
		add(txtVerifyPassword, input(row++));
		add(btnVerify, at(1, row++, GridBagConstraints.WEST));
		add(lblResult, at(1, row, GridBagConstraints.WEST));
	}

	private void onHash()
	{
		try
		{
			String algorithm = String.valueOf(cmbAlgorithm.getSelectedItem());
			long started = System.currentTimeMillis();
			txtHash.setText(PasswordHashSupport.hash(algorithm, txtPassword.getPassword()));
			long took = System.currentTimeMillis() - started;
			txtHash.setCaretPosition(0);
			String cost = PasswordHashSupport.costOf(txtHash.getText());
			lblResult.setText(algorithm + " took " + took + " ms"
				+ (cost.isEmpty() ? "" : " at " + cost));
		}
		catch (Exception exception)
		{
			txtHash.setText("");
			lblResult.setText("not hashed: " + message(exception));
		}
	}

	private void onVerify()
	{
		String encodedHash = txtHash.getText();
		if (encodedHash.isBlank())
		{
			lblResult.setText("hash a password first");
			return;
		}
		// which algorithm made the hash is read out of the hash, so a value pasted in is checked
		// correctly whatever the combo box happens to show
		String algorithm = PasswordHashSupport.algorithmOf(encodedHash);
		if (algorithm == null)
		{
			lblResult.setText("this does not look like a hash any of these algorithms made");
			return;
		}
		boolean matches = PasswordHashSupport.verify(txtVerifyPassword.getPassword(), encodedHash);
		lblResult.setText(matches ? "matches (" + algorithm + ")" : "does not match");
	}

	private static String message(Exception exception)
	{
		return exception.getMessage() != null
			? exception.getMessage()
			: exception.getClass().getSimpleName();
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
	 * Constraints for the input column: the component fills the width its column gets, and the
	 * column takes whatever width the panel has left over. Without this a window narrower than the
	 * panel wants falls back to the minimum widths and the inputs collapse instead of shrinking.
	 *
	 * @param y
	 *            the row of the grid
	 * @return the constraints for the input component of that row
	 */
	private static GridBagConstraints input(int y)
	{
		GridBagConstraints constraints = at(1, y, GridBagConstraints.WEST);
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1.0;
		return constraints;
	}

	/**
	 * Gives the component an honest minimum width, so that even the minimum layout of a narrow
	 * window still shows an input one can read and type in
	 *
	 * @param component
	 *            the component that gets the minimum width
	 */
	private static void keepUsableWhenNarrow(JComponent component)
	{
		component
			.setMinimumSize(new Dimension(MINIMUM_INPUT_WIDTH, component.getPreferredSize().height));
	}

	private static GridBagConstraints fullWidth(int y)
	{
		GridBagConstraints constraints = at(0, y, GridBagConstraints.CENTER);
		constraints.gridwidth = 2;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		return constraints;
	}
}
