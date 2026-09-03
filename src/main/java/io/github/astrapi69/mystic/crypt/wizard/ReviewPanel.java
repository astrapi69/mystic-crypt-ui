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
package io.github.astrapi69.mystic.crypt.wizard;

import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.model.component.JMTextArea;
import io.github.astrapi69.swing.model.component.JMTextField;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.miginfocom.swing.MigLayout;

/**
 * The class {@link ReviewPanel} shows the final step of the certificate wizard: a read-only preview
 * of what would be generated, and where it would be saved. Whoever opens the wizard fills the
 * preview in through {@link #refresh(String, String, File)} - generating the certificate needs the
 * mystic-crypt library and is application-specific, so this panel only holds the form, it does not
 * generate anything itself.
 * <p>
 * What the entry form holds is kept in a {@link ReviewPanelModel}: every input component is bound
 * to it, so the form state can be read at any moment instead of being fished out of the widgets
 * when Finish is pressed
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewPanel extends BasePanel<BaseWizardStateMachineModel<CertificateInfoModel>>
{

	JLabel lblHeader;
	JMTextArea txtPreview;
	JScrollPane scrPreview;
	JLabel lblFileName;
	JMTextField txtFileName;
	JLabel lblDirectory;
	JMTextField txtSaveDirectory;
	JButton btnBrowseDirectory;

	/**
	 * What the entry form currently holds. It is created in {@link #onInitializeComponents()} and
	 * not in a field initializer, because the base panel initializes the components from its own
	 * constructor, before any field initializer of this class has run
	 */
	ReviewPanelModel reviewFormModel;

	/**
	 * Instantiates a new ReviewPanel
	 *
	 * @param model
	 *            the model
	 */
	public ReviewPanel(IModel<BaseWizardStateMachineModel<CertificateInfoModel>> model)
	{
		super(model);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		reviewFormModel = new ReviewPanelModel();

		lblHeader = new JLabel("Review");

		txtPreview = new JMTextArea(16, 60);
		txtPreview.setName("txtPreview");
		txtPreview.setEditable(false);
		txtPreview.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
		txtPreview.setPropertyModel(
			LambdaModel.of(reviewFormModel::getPreview, reviewFormModel::setPreview));
		scrPreview = new JScrollPane(txtPreview);

		lblFileName = new JLabel("File name:");
		txtFileName = new JMTextField(24);
		txtFileName.setName("txtFileName");
		txtFileName.setPropertyModel(
			LambdaModel.of(reviewFormModel::getFileName, reviewFormModel::setFileName));

		lblDirectory = new JLabel("Save in:");
		txtSaveDirectory = new JMTextField(24);
		txtSaveDirectory.setName("txtSaveDirectory");
		txtSaveDirectory.setPropertyModel(LambdaModel.of(reviewFormModel::getSaveDirectoryPath,
			reviewFormModel::setSaveDirectoryPath));

		btnBrowseDirectory = new JButton("Browse...");
		btnBrowseDirectory.setName("btnBrowseDirectory");
		btnBrowseDirectory.addActionListener(event -> onBrowseDirectory());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		setLayout(new MigLayout("wrap 2", "[grow,fill][grow,fill]", "[][grow][][]"));

		add(lblHeader, "span, align center, wrap 10");
		add(scrPreview, "span, grow, wrap");

		add(lblFileName);
		add(txtFileName, "wrap");

		add(lblDirectory);
		add(txtSaveDirectory, "split 2, growx");
		add(btnBrowseDirectory, "wrap");
	}

	/**
	 * Lets the user pick the directory to save the certificate in, starting from whatever is
	 * currently entered
	 */
	protected void onBrowseDirectory()
	{
		JFileChooser directoryChooser = new JFileChooser(reviewFormModel.getSaveDirectory());
		directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		directoryChooser.setDialogTitle("Choose the folder to save the certificate in");
		if (directoryChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			reviewFormModel.setSaveDirectory(directoryChooser.getSelectedFile());
			txtSaveDirectory.setText(reviewFormModel.getSaveDirectoryPath());
		}
	}

	/**
	 * Regenerates the read-only preview and fills in the defaults for the file name and the save
	 * directory - unless the user already typed something of their own, which a fresh default must
	 * not silently overwrite
	 *
	 * @param preview
	 *            what the wizard would generate right now
	 * @param defaultFileName
	 *            the file name a fresh default would use
	 * @param defaultDirectory
	 *            the directory a fresh default would use
	 */
	public void refresh(final String preview, final String defaultFileName,
		final File defaultDirectory)
	{
		txtPreview.setText(preview);
		txtPreview.setCaretPosition(0);
		if (reviewFormModel.getFileName() == null || reviewFormModel.getFileName().isBlank())
		{
			reviewFormModel.setFileName(defaultFileName);
			txtFileName.setText(defaultFileName);
		}
		if (reviewFormModel.getSaveDirectory() == null)
		{
			reviewFormModel.setSaveDirectory(defaultDirectory);
			txtSaveDirectory.setText(reviewFormModel.getSaveDirectoryPath());
		}
	}
}
