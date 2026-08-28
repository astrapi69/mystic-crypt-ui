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
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.component.JMCheckBox;
import io.github.astrapi69.swing.model.component.JMPasswordField;
import io.github.astrapi69.swing.model.component.JMTextField;
import lombok.Getter;

/**
 * Form for picking a destination {@code .kdbx} file plus password/key-file credentials to export to
 */
@Getter
public class ExportKeePassPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** Everything the user picked or typed; every component below writes into it */
	private final transient KeePassPanelModel modelObject = new KeePassPanelModel();

	private final JMTextField txtFile = new JMTextField(24);
	private final JMPasswordField txtPassword = new JMPasswordField(24);
	private final JMCheckBox cbxKeyFile = new JMCheckBox("Key File:");
	private final JMTextField txtKeyFile = new JMTextField(24);

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

		bindComponents();

		// stable component names for UI tests (AssertJ-Swing lookups by name)
		txtFile.setName("txtFile");
		txtPassword.setName("txtPassword");
		cbxKeyFile.setName("cbxKeyFile");
		txtKeyFile.setName("txtKeyFile");
		btnFile.setName("btnFile");
		btnKeyFile.setName("btnKeyFile");

		txtFile.setEditable(false);
		txtKeyFile.setEditable(false);
		txtKeyFile.setEnabled(false);
		btnKeyFile.setEnabled(false);

		if (lastFilePath != null)
		{
			txtFile.setText(lastFilePath);
		}
		if (lastKeyFilePath != null && new File(lastKeyFilePath).exists())
		{
			txtKeyFile.setText(lastKeyFilePath);
			cbxKeyFile.setSelected(true);
			txtKeyFile.setEnabled(true);
			btnKeyFile.setEnabled(true);
		}

		btnFile.addActionListener(event -> {
			File pickedFile = getSelectedFile();
			JFileChooser fileChooser = pickedFile != null
				? new JFileChooser(pickedFile.getParentFile())
				: new JFileChooser();
			fileChooser.setDialogTitle("Specify the KeePass database file to export to");
			fileChooser
				.setFileFilter(new FileNameExtensionFilter("KeePass files (*.kdbx)", "kdbx"));
			if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				File destinationFile = fileChooser.getSelectedFile();
				if (!destinationFile.getName().toLowerCase().endsWith(".kdbx"))
				{
					destinationFile = new File(destinationFile.getParentFile(),
						destinationFile.getName() + ".kdbx");
				}
				txtFile.setText(destinationFile.getAbsolutePath());
			}
		});

		cbxKeyFile.addActionListener(event -> {
			boolean selected = cbxKeyFile.isSelected();
			txtKeyFile.setEnabled(selected);
			btnKeyFile.setEnabled(selected);
			if (!selected)
			{
				txtKeyFile.setText("");
			}
		});

		btnKeyFile.addActionListener(event -> {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Select the KeePass key file");
			if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				txtKeyFile.setText(fileChooser.getSelectedFile().getAbsolutePath());
			}
		});

		// one layout for every tool window: labels in a narrow right aligned column, the fields
		// taking the width, each browse button next to the field it fills
		setLayout(ToolForm.newLayout());
		ToolForm.sized(txtFile);
		ToolForm.sized(txtPassword);
		ToolForm.sized(txtKeyFile);

		add(new JLabel("KeePass file:"));
		add(txtFile, ToolForm.FIELD + ", split 2");
		add(btnFile);

		add(new JLabel("Password:"));
		add(txtPassword, ToolForm.FIELD);

		add(cbxKeyFile);
		add(txtKeyFile, ToolForm.FIELD + ", split 2");
		add(btnKeyFile);
	}

	/**
	 * Binds every component to the model, so that each edit lands in the model and the model is
	 * what the export reads - the components carry the values the model already holds
	 */
	private void bindComponents()
	{
		txtFile
			.setPropertyModel(LambdaModel.of(modelObject::getFilePath, modelObject::setFilePath));
		txtPassword
			.setPropertyModel(LambdaModel.of(modelObject::getPassword, modelObject::setPassword));
		cbxKeyFile.setPropertyModel(
			LambdaModel.of(modelObject::isUseKeyFile, modelObject::setUseKeyFile));
		txtKeyFile.setPropertyModel(
			LambdaModel.of(modelObject::getKeyFilePath, modelObject::setKeyFilePath));
	}

	/**
	 * Gets the KeePass file to export to
	 *
	 * @return the picked destination file, or {@code null} when none was picked
	 */
	public File getSelectedFile()
	{
		return modelObject.getFile();
	}

	/**
	 * Gets the key file the exported KeePass file is protected with
	 *
	 * @return the picked key file, or {@code null} when none was picked
	 */
	public File getSelectedKeyFile()
	{
		return modelObject.getKeyFile();
	}

	/**
	 * Gets the password the exported KeePass file is protected with
	 *
	 * @return the entered password, empty when none was entered
	 */
	public char[] getPassword()
	{
		return modelObject.getPassword();
	}

}
