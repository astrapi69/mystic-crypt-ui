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

import javax.swing.*;

import io.github.astrapi69.mystic.crypt.pw.PasswordEncryptor;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;

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
		// one layout for every tool window, so this one looks like the one next to it: labels in a
		// narrow right aligned column, fields taking the width, the hash taking the height that is
		// left, buttons under what they act on
		super(ToolForm.newLayout());
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

		ToolForm.sized(txtPassword);
		ToolForm.sized(txtVerifyPassword);

		JButton btnHash = new JButton("Hash");
		btnHash.setName("btnHash");
		btnHash.addActionListener(event -> onHash());
		JButton btnVerify = new JButton("Verify");
		btnVerify.setName("btnVerify");
		btnVerify.addActionListener(event -> onVerify());

		add(new JLabel("Algorithm:"));
		add(cmbAlgorithm, ToolForm.FIELD);
		add(lblAbout, ToolForm.WIDE);

		add(new JLabel("Password:"));
		add(txtPassword, ToolForm.FIELD);
		add(ToolForm.buttons(btnHash), ToolForm.BUTTON_ROW);

		add(new JLabel("Hash:"), "aligny top");
		add(ToolForm.scrolled(txtHash), "grow, push");

		add(new JSeparator(), ToolForm.WIDE);

		add(new JLabel("Verify password:"));
		add(txtVerifyPassword, ToolForm.FIELD);
		add(ToolForm.buttons(btnVerify), ToolForm.BUTTON_ROW);
		add(lblResult, ToolForm.RESULT_LINE);
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




}
