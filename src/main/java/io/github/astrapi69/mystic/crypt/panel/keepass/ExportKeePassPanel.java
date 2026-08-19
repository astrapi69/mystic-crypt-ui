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
package io.github.astrapi69.mystic.crypt.panel.keepass;

import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import lombok.Getter;

/**
 * Form for picking a destination {@code .kdbx} file plus password/key-file credentials to export to
 */
@Getter
public class ExportKeePassPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final JTextField txtFile = new JTextField(24);
	private final JPasswordField txtPassword = new JPasswordField(24);
	private final JCheckBox cbxKeyFile = new JCheckBox("Key File:");
	private final JTextField txtKeyFile = new JTextField(24);
	private File selectedFile;
	private File selectedKeyFile;

	public ExportKeePassPanel()
	{
		this(null, null);
	}

	/**
	 * Instantiates a new {@link ExportKeePassPanel}, pre-filled with the given, previously used
	 * file paths
	 *
	 * @param lastFilePath
	 *            the last used destination file path, or {@code null}
	 * @param lastKeyFilePath
	 *            the last used key file path, or {@code null}
	 */
	public ExportKeePassPanel(String lastFilePath, String lastKeyFilePath)
	{
		JButton btnFile = new JButton("Browse...");
		JButton btnKeyFile = new JButton("Browse...");

		txtFile.setEditable(false);
		txtKeyFile.setEditable(false);
		txtKeyFile.setEnabled(false);
		btnKeyFile.setEnabled(false);

		if (lastFilePath != null)
		{
			selectedFile = new File(lastFilePath);
			txtFile.setText(lastFilePath);
		}
		if (lastKeyFilePath != null && new File(lastKeyFilePath).exists())
		{
			selectedKeyFile = new File(lastKeyFilePath);
			txtKeyFile.setText(lastKeyFilePath);
			cbxKeyFile.setSelected(true);
			txtKeyFile.setEnabled(true);
			btnKeyFile.setEnabled(true);
		}

		btnFile.addActionListener(event -> {
			JFileChooser fileChooser = selectedFile != null
				? new JFileChooser(selectedFile.getParentFile())
				: new JFileChooser();
			fileChooser.setDialogTitle("Specify the KeePass database file to export to");
			fileChooser
				.setFileFilter(new FileNameExtensionFilter("KeePass files (*.kdbx)", "kdbx"));
			if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				selectedFile = fileChooser.getSelectedFile();
				if (!selectedFile.getName().toLowerCase().endsWith(".kdbx"))
				{
					selectedFile = new File(selectedFile.getParentFile(),
						selectedFile.getName() + ".kdbx");
				}
				txtFile.setText(selectedFile.getAbsolutePath());
			}
		});

		cbxKeyFile.addActionListener(event -> {
			boolean selected = cbxKeyFile.isSelected();
			txtKeyFile.setEnabled(selected);
			btnKeyFile.setEnabled(selected);
			if (!selected)
			{
				selectedKeyFile = null;
				txtKeyFile.setText("");
			}
		});

		btnKeyFile.addActionListener(event -> {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Select the KeePass key file");
			if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				selectedKeyFile = fileChooser.getSelectedFile();
				txtKeyFile.setText(selectedKeyFile.getAbsolutePath());
			}
		});

		setLayout(new java.awt.GridBagLayout());
		java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
		c.insets = new java.awt.Insets(4, 4, 4, 4);
		c.gridy = 0;
		c.gridx = 0;
		add(new JLabel("KeePass file:"), c);
		c.gridx = 1;
		add(txtFile, c);
		c.gridx = 2;
		add(btnFile, c);

		c.gridy = 1;
		c.gridx = 0;
		add(new JLabel("Password:"), c);
		c.gridx = 1;
		add(txtPassword, c);

		c.gridy = 2;
		c.gridx = 0;
		add(cbxKeyFile, c);
		c.gridx = 1;
		add(txtKeyFile, c);
		c.gridx = 2;
		add(btnKeyFile, c);
	}

	public char[] getPassword()
	{
		return txtPassword.getPassword();
	}

}
