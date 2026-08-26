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
package io.github.astrapi69.mystic.crypt.plugin.sharing;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;

/**
 * Tool panel for splitting a secret into shares and putting it back together.
 * <p>
 * This answers what happens to a master password if its owner cannot use it any more: the secret is
 * split so that a few of the shares are enough to rebuild it, and fewer than that reveal nothing.
 * Each share is a line of text meant to leave this machine - onto paper, into someone else's safe -
 * so each one carries a check value that catches a character mistyped while copying it back.
 */
public class SecretSharingPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final JPasswordField pwdSecret = new JPasswordField(28);
	private final JTextField txtSecretFile = new JTextField(34);
	private final JCheckBox chkUseFile = new JCheckBox("split the file instead of the secret above");
	private final JSpinner spnThreshold = new JSpinner(new SpinnerNumberModel(3, 2, 255, 1));
	private final JSpinner spnTotalShares = new JSpinner(new SpinnerNumberModel(5, 2, 255, 1));
	private final JTextArea txtShares = new JTextArea(8, 62);
	private final JTextArea txtRebuilt = new JTextArea(3, 62);
	private final JTextField txtRebuiltFile = new JTextField(34);
	private final JLabel lblResult = new JLabel(" ");

	public SecretSharingPanel()
	{
		super(new BorderLayout(4, 4));

		pwdSecret.setName("pwdSecret");
		txtSecretFile.setName("txtSecretFile");
		chkUseFile.setName("chkUseFile");
		spnThreshold.setName("spnThreshold");
		spnTotalShares.setName("spnTotalShares");
		txtShares.setName("txtShares");
		txtShares.setFont(new Font("monospaced", Font.PLAIN, 12));
		txtRebuilt.setName("txtRebuilt");
		txtRebuilt.setLineWrap(true);
		txtRebuiltFile.setName("txtRebuiltFile");
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));

		// the tool starts with what the user configured in the settings dialog
		spnThreshold.setValue(SecretSharingSettingsContribution.threshold());
		spnTotalShares.setValue(SecretSharingSettingsContribution.totalShares());

		JPanel form = new JPanel(new GridBagLayout());
		int row = 0;
		form.add(new JLabel("Secret:"), at(0, row, GridBagConstraints.EAST));
		form.add(pwdSecret, at(1, row++, GridBagConstraints.WEST));
		form.add(new JLabel("or file:"), at(0, row, GridBagConstraints.EAST));
		form.add(txtSecretFile, at(1, row, GridBagConstraints.WEST));
		form.add(button("btnBrowseSecretFile", "...", event -> onBrowse(txtSecretFile)),
			at(2, row++, GridBagConstraints.WEST));
		form.add(chkUseFile, at(1, row++, GridBagConstraints.WEST));
		form.add(new JLabel("Shares needed:"), at(0, row, GridBagConstraints.EAST));
		form.add(spnThreshold, at(1, row++, GridBagConstraints.WEST));
		form.add(new JLabel("Shares produced:"), at(0, row, GridBagConstraints.EAST));
		form.add(spnTotalShares, at(1, row++, GridBagConstraints.WEST));
		form.add(buttonRow(button("btnSplit", "Split", event -> onSplit()),
			button("btnSaveShares", "Save shares", event -> onSaveShares())),
			at(1, row++, GridBagConstraints.WEST));

		form.add(new JLabel("Shares:"), at(0, row, GridBagConstraints.NORTHEAST));
		form.add(new JScrollPane(txtShares), at(1, row++, GridBagConstraints.WEST));
		form.add(buttonRow(button("btnCombine", "Combine", event -> onCombine()),
			button("btnLoadShares", "Load shares", event -> onLoadShares())),
			at(1, row++, GridBagConstraints.WEST));

		form.add(new JLabel("Rebuilt secret:"), at(0, row, GridBagConstraints.NORTHEAST));
		form.add(new JScrollPane(txtRebuilt), at(1, row++, GridBagConstraints.WEST));
		form.add(new JLabel("Write it to:"), at(0, row, GridBagConstraints.EAST));
		form.add(txtRebuiltFile, at(1, row, GridBagConstraints.WEST));
		form.add(button("btnBrowseRebuiltFile", "...", event -> onBrowse(txtRebuiltFile)),
			at(2, row++, GridBagConstraints.WEST));
		form.add(button("btnSaveRebuilt", "Save rebuilt secret", event -> onSaveRebuilt()),
			at(1, row, GridBagConstraints.WEST));

		add(form, BorderLayout.CENTER);
		add(lblResult, BorderLayout.SOUTH);
	}

	private void onSplit()
	{
		run("split", () -> {
			int threshold = (Integer)spnThreshold.getValue();
			int totalShares = (Integer)spnTotalShares.getValue();
			byte[] secret = secret();
			try
			{
				List<String> shares = SecretSharingSupport.split(secret, threshold, totalShares);
				txtShares.setText(String.join(System.lineSeparator(), shares));
				txtShares.setCaretPosition(0);
				return totalShares + " shares, " + threshold + " of them are enough - keep them apart";
			}
			finally
			{
				Arrays.fill(secret, (byte)0);
			}
		});
	}

	private void onCombine()
	{
		run("combined", () -> {
			List<String> shares = List.of(txtShares.getText().split("\\R"));
			byte[] secret = SecretSharingSupport.combine(shares);
			txtRebuilt.setText(new String(secret, StandardCharsets.UTF_8));
			txtRebuilt.setCaretPosition(0);
			return "rebuilt " + secret.length + " bytes";
		});
	}

	private void onSaveShares()
	{
		run("saved", () -> {
			if (txtShares.getText().isBlank())
			{
				throw new IllegalArgumentException("there is nothing to save - split a secret first");
			}
			File target = requireNewFile(txtSecretFile, ".shares.txt");
			Files.writeString(target.toPath(), txtShares.getText(), StandardCharsets.UTF_8);
			return "wrote the shares to " + target.getName()
				+ " - they belong apart, not in one file";
		});
	}

	private void onLoadShares()
	{
		run("loaded", () -> {
			File source = new File(txtSecretFile.getText().trim());
			if (!source.isFile())
			{
				throw new IllegalArgumentException("choose the file the shares are in");
			}
			txtShares.setText(Files.readString(source.toPath(), StandardCharsets.UTF_8));
			txtShares.setCaretPosition(0);
			return "read the shares from " + source.getName();
		});
	}

	private void onSaveRebuilt()
	{
		run("saved", () -> {
			if (txtRebuilt.getText().isEmpty())
			{
				throw new IllegalArgumentException("there is nothing to save - combine the shares first");
			}
			String path = txtRebuiltFile.getText().trim();
			if (path.isEmpty())
			{
				throw new IllegalArgumentException("choose a file to write the secret to");
			}
			File target = new File(path);
			if (target.exists())
			{
				throw new IllegalArgumentException("'" + target
					+ "' already exists - pick another name or remove it first");
			}
			Files.write(target.toPath(),
				txtRebuilt.getText().getBytes(StandardCharsets.UTF_8));
			return "wrote the secret to " + target.getName();
		});
	}

	private byte[] secret() throws Exception
	{
		if (chkUseFile.isSelected())
		{
			File file = new File(txtSecretFile.getText().trim());
			if (!file.isFile())
			{
				throw new IllegalArgumentException("choose a file to split");
			}
			return Files.readAllBytes(file.toPath());
		}
		char[] typed = pwdSecret.getPassword();
		if (typed.length == 0)
		{
			throw new IllegalArgumentException("there is no secret to split");
		}
		return new String(typed).getBytes(StandardCharsets.UTF_8);
	}

	private File requireNewFile(JTextField field, String suffix)
	{
		String path = field.getText().trim();
		File target = path.isEmpty() ? new File("shares" + suffix) : new File(path + suffix);
		if (target.exists())
		{
			throw new IllegalArgumentException(
				"'" + target + "' already exists - pick another name or remove it first");
		}
		return target;
	}

	/** The message shown at the bottom of the panel */
	public String getResultText()
	{
		return lblResult.getText();
	}

	private void run(String what, SharingOperation operation)
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
	private interface SharingOperation
	{
		String execute() throws Exception;
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
