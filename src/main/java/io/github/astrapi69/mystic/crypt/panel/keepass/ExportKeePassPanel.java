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
import javax.swing.filechooser.FileNameExtensionFilter;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.model.component.JMCheckBox;
import io.github.astrapi69.swing.model.component.JMPasswordField;
import io.github.astrapi69.swing.model.component.JMTextField;
import lombok.Getter;

/**
 * Form for picking a destination {@code .kdbx} file plus password/key-file credentials to export
 * to.
 * <p>
 * Everything the user picks or types lives in a {@link KeePassPanelModel}; the components are bound
 * to it and the export reads it there.
 */
@Getter
public class ExportKeePassPanel extends BasePanel<KeePassPanelModel>
{

	private static final long serialVersionUID = 1L;

	private JMTextField txtFile;

	private JMPasswordField txtPassword;

	private JMCheckBox cbxKeyFile;

	private JMTextField txtKeyFile;

	private JButton btnFile;

	private JButton btnKeyFile;

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
	public ExportKeePassPanel(final String lastFilePath, final String lastKeyFilePath)
	{
		this(BaseModel.of(KeePassPanelModel.rememberingDestination(lastFilePath, lastKeyFilePath)));
	}

	/**
	 * Instantiates a new {@link ExportKeePassPanel} over the given model
	 *
	 * @param model
	 *            what the panel is to show and write into
	 */
	public ExportKeePassPanel(final IModel<KeePassPanelModel> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
		txtFile = new JMTextField(24);
		txtPassword = new JMPasswordField(24);
		cbxKeyFile = new JMCheckBox("Key File:");
		txtKeyFile = new JMTextField(24);
		btnFile = new JButton("Browse...");
		btnKeyFile = new JButton("Browse...");

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

		txtFile.setToolTipText(
			Messages.getString("keepass.export.tooltip.file", "the KeePass file to write to"));
		btnFile.setToolTipText(Messages.getString("keepass.export.tooltip.browse.file.button",
			"choose the destination file"));
		txtPassword.setToolTipText(Messages.getString("keepass.export.tooltip.password",
			"the password that protects the exported file"));
		cbxKeyFile.setToolTipText(Messages.getString("keepass.export.tooltip.key.file.checkbox",
			"additionally protect the exported file with a key file, the way KeePass itself supports"));
		txtKeyFile.setToolTipText(Messages.getString("keepass.export.tooltip.key.file",
			"the key file to protect the exported file with"));
		btnKeyFile.setToolTipText(Messages
			.getString("keepass.export.tooltip.browse.key.file.button", "choose the key file"));

		showWhatTheModelHolds();

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

		cbxKeyFile.addActionListener(event -> onCheckKeyFile());

		btnKeyFile.addActionListener(event -> {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Select the KeePass key file");
			if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				txtKeyFile.setText(fileChooser.getSelectedFile().getAbsolutePath());
			}
		});
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
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
		txtFile.setPropertyModel(
			LambdaModel.of(() -> getModelObject().getFilePath(), getModelObject()::setFilePath));
		txtPassword.setPropertyModel(
			LambdaModel.of(() -> getModelObject().getPassword(), getModelObject()::setPassword));
		cbxKeyFile.setPropertyModel(
			LambdaModel.of(() -> getModelObject().isUseKeyFile(), getModelObject()::setUseKeyFile));
		txtKeyFile.setPropertyModel(LambdaModel.of(() -> getModelObject().getKeyFilePath(),
			getModelObject()::setKeyFilePath));
	}

	/**
	 * Puts what the model already holds on screen, for a panel that was opened with remembered
	 * paths in it
	 */
	private void showWhatTheModelHolds()
	{
		KeePassPanelModel modelObject = getModelObject();
		txtFile.setText(modelObject.getFilePath());
		txtKeyFile.setText(modelObject.getKeyFilePath());
		cbxKeyFile.setSelected(modelObject.isUseKeyFile());
		toggleKeyFileComponents();
	}

	/**
	 * The key file is only pickable while it is in use. What decides that is the model, not the
	 * check box: the box writes into the model and the rest of the panel reads it there.
	 */
	protected void onCheckKeyFile()
	{
		getModelObject().setUseKeyFile(cbxKeyFile.isSelected());
		if (!getModelObject().isUseKeyFile())
		{
			txtKeyFile.setText("");
		}
		toggleKeyFileComponents();
	}

	/** Puts the key file components in the state the model asks for */
	private void toggleKeyFileComponents()
	{
		boolean useKeyFile = getModelObject().isUseKeyFile();
		txtKeyFile.setEnabled(useKeyFile);
		btnKeyFile.setEnabled(useKeyFile);
	}

	/**
	 * Gets the KeePass file to export to
	 *
	 * @return the picked destination, or {@code null} when none was picked
	 */
	public File getSelectedFile()
	{
		return getModelObject().getFile();
	}

	/**
	 * Gets the key file the KeePass file is protected with
	 *
	 * @return the picked key file, or {@code null} when none was picked
	 */
	public File getSelectedKeyFile()
	{
		return getModelObject().getKeyFile();
	}

	/**
	 * Gets the password that opens the KeePass file
	 *
	 * @return the entered password, empty when none was entered
	 */
	public char[] getPassword()
	{
		return getModelObject().getPassword();
	}

}
