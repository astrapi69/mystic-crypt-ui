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
package io.github.astrapi69.mystic.crypt.plugin.certificate;

import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.file.Files;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.model.component.JMTextArea;
import lombok.Getter;

/**
 * There is no in-app viewer for a certificate file otherwise, so this shows what
 * {@link CertificateFileEditorModel#getFile()} holds as plain text and can save what was typed
 * back to it - the wizard this is opened from always writes PEM, which is text (#110)
 */
@Getter
public class CertificateFileEditorPanel extends BasePanel<CertificateFileEditorModel>
{

	private static final long serialVersionUID = 1L;

	private JMTextArea txtContent;
	private JScrollPane scpContent;
	private JButton btnSave;
	private JButton btnClose;

	public CertificateFileEditorPanel(IModel<CertificateFileEditorModel> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
		txtContent = new JMTextArea();
		txtContent.setName("txtContent");
		txtContent.setPropertyModel(
			LambdaModel.of(getModelObject()::getContent, getModelObject()::setContent));
		scpContent = new JScrollPane(txtContent);

		btnSave = new JButton("Save");
		btnSave.setName("btnSave");
		btnSave.addActionListener(event -> onSave());

		btnClose = new JButton("Close");
		btnClose.setName("btnClose");
		btnClose.addActionListener(event -> onClose());
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		setLayout(new BorderLayout());
		add(scpContent, BorderLayout.CENTER);

		JPanel buttonRow = new JPanel();
		buttonRow.add(btnSave);
		buttonRow.add(btnClose);
		add(buttonRow, BorderLayout.SOUTH);
	}

	/**
	 * Writes what is currently in the editor back to the file it was opened for - not what the
	 * file held when the editor was opened, so an edit that was never saved does not silently win
	 * back on the next open
	 */
	protected void onSave()
	{
		try
		{
			Files.writeString(getModelObject().getFile().toPath(), getModelObject().getContent());
		}
		catch (IOException exception)
		{
			showErrorDialog("Could not save",
				"Could not save " + getModelObject().getFile().getName() + ": "
					+ exception.getMessage());
		}
	}

	/**
	 * Callback for the Close button; overridden by whoever opens this panel in a dialog, to
	 * actually close it
	 */
	protected void onClose()
	{
	}

	/**
	 * Shows an error dialog with the specified title and message
	 *
	 * @param title
	 *            the title of the error dialog
	 * @param message
	 *            the message of the error dialog
	 */
	protected void showErrorDialog(String title, String message)
	{
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
	}

}
