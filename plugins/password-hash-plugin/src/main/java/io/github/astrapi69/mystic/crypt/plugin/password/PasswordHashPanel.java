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
	private static final String ARGON2ID = "Argon2id";
	private static final String PBKDF2 = "PBKDF2";

	private final transient PasswordEncryptor passwordEncryptor = PasswordEncryptor.getInstance();

	private final JComboBox<String> cmbAlgorithm = new JComboBox<>(new String[] { ARGON2ID, PBKDF2 });
	private final JPasswordField txtPassword = new JPasswordField(24);
	private final JTextArea txtHash = new JTextArea(3, 40);
	private final JPasswordField txtVerifyPassword = new JPasswordField(24);
	private final JLabel lblResult = new JLabel(" ");

	public PasswordHashPanel()
	{
		super(new GridBagLayout());
		cmbAlgorithm.setName("cmbAlgorithm");
		txtPassword.setName("txtPassword");
		txtHash.setName("txtHash");
		txtHash.setEditable(false);
		txtHash.setLineWrap(true);
		txtHash.setWrapStyleWord(false);
		txtVerifyPassword.setName("txtVerifyPassword");
		lblResult.setName("lblResult");

		JButton btnHash = new JButton("Hash");
		btnHash.setName("btnHash");
		btnHash.addActionListener(event -> onHash());
		JButton btnVerify = new JButton("Verify");
		btnVerify.setName("btnVerify");
		btnVerify.addActionListener(event -> onVerify());

		int row = 0;
		add(new JLabel("Algorithm:"), at(0, row, GridBagConstraints.EAST));
		add(cmbAlgorithm, at(1, row++, GridBagConstraints.WEST));
		add(new JLabel("Password:"), at(0, row, GridBagConstraints.EAST));
		add(txtPassword, at(1, row++, GridBagConstraints.WEST));
		add(btnHash, at(1, row++, GridBagConstraints.WEST));
		add(new JLabel("Hash:"), at(0, row, GridBagConstraints.NORTHEAST));
		add(new JScrollPane(txtHash), at(1, row++, GridBagConstraints.WEST));
		add(new JSeparator(), fullWidth(row++));
		add(new JLabel("Verify password:"), at(0, row, GridBagConstraints.EAST));
		add(txtVerifyPassword, at(1, row++, GridBagConstraints.WEST));
		add(btnVerify, at(1, row++, GridBagConstraints.WEST));
		add(lblResult, at(1, row, GridBagConstraints.WEST));
	}

	private void onHash()
	{
		String password = new String(txtPassword.getPassword());
		txtHash.setText(isArgon2id() ? passwordEncryptor.hashPasswordArgon2id(password)
			: passwordEncryptor.hashPasswordPbkdf2(password));
		lblResult.setText(" ");
	}

	private void onVerify()
	{
		String password = new String(txtVerifyPassword.getPassword());
		String encodedHash = txtHash.getText();
		if (encodedHash.isBlank())
		{
			lblResult.setText("hash a password first");
			return;
		}
		boolean matches = isArgon2id() ? passwordEncryptor.matchArgon2id(password, encodedHash)
			: passwordEncryptor.matchPbkdf2(password, encodedHash);
		lblResult.setText(matches ? "matches" : "does not match");
	}

	private boolean isArgon2id()
	{
		return ARGON2ID.equals(cmbAlgorithm.getSelectedItem());
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

	private static GridBagConstraints fullWidth(int y)
	{
		GridBagConstraints constraints = at(0, y, GridBagConstraints.CENTER);
		constraints.gridwidth = 2;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		return constraints;
	}
}
