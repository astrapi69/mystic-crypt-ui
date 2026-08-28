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
package io.github.astrapi69.mystic.crypt.plugin.conversion;

import java.awt.Font;
import java.io.File;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.component.JMTextField;

/**
 * Tool panel for converting a key or certificate file between the shapes it can have.
 * <p>
 * Nothing is declared here. Choosing a file says what it holds - a PEM file carries its type in its
 * header, a DER file is decided by what can be read out of it - and only the conversions that make
 * sense for it stay available. The old tool asked the user to say what the file was and produced a
 * broken file, or a stack trace on the console, when that guess was wrong.
 * <p>
 * Every value the panel works with lives in its {@link ConversionPanelModel}: the text fields are
 * bound to it, and the buttons read the model rather than the widgets.
 * <p>
 * The form is laid out with {@link ToolForm}, the one shape every tool window in this application
 * uses: labels in a narrow right aligned column, fields taking the width the window has, a row of
 * buttons under what they act on and the result line at the bottom.
 */
public class ConversionPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/**
	 * A field that shares its cell with the button that browses for it: the field takes the width
	 * of the field column, the button keeps its own width at the end of the row
	 */
	private static final String FIELD_WITH_BUTTON = ToolForm.FIELD + ", split 2";

	private final ConversionPanelModel modelObject = new ConversionPanelModel();

	private final JMTextField txtSourceFile = new JMTextField(38);
	private final JMTextField txtTargetFile = new JMTextField(38);
	private final JLabel lblWhatItHolds = new JLabel(ConversionPanelModel.NOTHING_TO_SAY);
	private final JLabel lblResult = new JLabel(ConversionPanelModel.NOTHING_TO_SAY);
	private final JButton btnPemToDer = button("btnPemToDer", "to DER", event -> onPemToDer());
	private final JButton btnDerToPem = button("btnDerToPem", "to PEM", event -> onDerToPem());
	private final JButton btnToPkcs8 = button("btnToPkcs8", "to PKCS#8 (Java)",
		event -> onToPkcs8());
	private final JButton btnToPkcs1 = button("btnToPkcs1", "to PKCS#1 (openssl)",
		event -> onToPkcs1());

	public ConversionPanel()
	{
		super(ToolForm.newLayout());

		txtSourceFile.setName("txtSourceFile");
		txtTargetFile.setName("txtTargetFile");
		lblWhatItHolds.setName("lblWhatItHolds");
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
		ToolForm.sized(txtSourceFile);
		ToolForm.sized(txtTargetFile);

		txtSourceFile.setPropertyModel(
			LambdaModel.of(modelObject::getSourceFilePath, modelObject::setSourceFilePath));
		txtTargetFile.setPropertyModel(
			LambdaModel.of(modelObject::getTargetFilePath, modelObject::setTargetFilePath));

		add(new JLabel("File:"));
		add(txtSourceFile, FIELD_WITH_BUTTON);
		add(button("btnBrowseSource", "...", event -> onBrowseSource()));

		add(new JLabel("It holds:"));
		add(lblWhatItHolds, ToolForm.FIELD);

		add(new JLabel("Write to:"));
		add(txtTargetFile, FIELD_WITH_BUTTON);
		add(button("btnBrowseTarget", "...", event -> onBrowseTarget()));

		add(ToolForm.buttons(btnPemToDer, btnDerToPem, btnToPkcs8, btnToPkcs1),
			ToolForm.BUTTON_ROW);
		add(lblResult, ToolForm.RESULT_LINE);

		// the file itself says what it is, so the tool looks as soon as there is a path - typed,
		// pasted or picked
		txtSourceFile.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				onSourceEdited();
			}

			@Override
			public void removeUpdate(DocumentEvent event)
			{
				onSourceEdited();
			}

			@Override
			public void changedUpdate(DocumentEvent event)
			{
				onSourceEdited();
			}
		});

		setConversionsFor(null);
	}

	/**
	 * Carries an edit of the source field into the model and looks at the file it now names. The
	 * field's own binding is notified after this listener, so the edited text is taken from the
	 * field here instead of waiting for it.
	 */
	private void onSourceEdited()
	{
		modelObject.setSourceFilePath(txtSourceFile.getText());
		onSourceChosen();
	}

	/** Looks at the chosen file and offers only what can be done with it */
	protected void onSourceChosen()
	{
		if (sourcePath().isEmpty())
		{
			showWhatItHolds(ConversionPanelModel.NOTHING_TO_SAY);
			setConversionsFor(null);
			return;
		}
		try
		{
			ConversionSupport.FileKind kind = ConversionSupport.kindOf(new File(sourcePath()));
			showWhatItHolds(kind.description());
			setConversionsFor(kind);
			showResult(ConversionPanelModel.NOTHING_TO_SAY);
		}
		catch (Exception exception)
		{
			showWhatItHolds(ConversionPanelModel.NOTHING_TO_SAY);
			setConversionsFor(null);
			showResult("not read: " + message(exception));
		}
	}

	private void setConversionsFor(ConversionSupport.FileKind kind)
	{
		modelObject.setFileKind(kind);
		boolean readable = kind != null;
		btnPemToDer.setEnabled(readable && kind.pem());
		btnDerToPem.setEnabled(readable && !kind.pem()
			&& !kind.description().startsWith("nothing"));
		boolean privateKey = readable
			&& ConversionSupport.holdsAPrivateKey(new File(sourcePath()));
		btnToPkcs8.setEnabled(privateKey);
		btnToPkcs1.setEnabled(privateKey);
	}

	private void onPemToDer()
	{
		run("converted", () -> {
			ConversionSupport.pemToDer(source(), target());
			return "written as DER to " + target().getName();
		});
	}

	private void onDerToPem()
	{
		run("converted", () -> {
			ConversionSupport.derToPem(source(), target());
			return "written as PEM to " + target().getName();
		});
	}

	private void onToPkcs8()
	{
		run("converted", () -> {
			ConversionSupport.toPkcs8(source(), target());
			return "written as PKCS#8 to " + target().getName();
		});
	}

	private void onToPkcs1()
	{
		run("converted", () -> {
			ConversionSupport.toPkcs1(source(), target());
			return "written as PKCS#1 to " + target().getName();
		});
	}

	/** The message shown at the bottom of the panel */
	public String getResultText()
	{
		return modelObject.getResultMessage();
	}

	/** What the chosen file was found to hold */
	public String getWhatItHoldsText()
	{
		return modelObject.getWhatItHolds();
	}

	/** The state this panel holds, readable at any moment */
	protected ConversionPanelModel getModelObject()
	{
		return modelObject;
	}

	private void showWhatItHolds(String description)
	{
		modelObject.setWhatItHolds(description);
		lblWhatItHolds.setText(description);
	}

	private void showResult(String resultMessage)
	{
		modelObject.setResultMessage(resultMessage);
		lblResult.setText(resultMessage);
	}

	private String sourcePath()
	{
		String path = modelObject.getSourceFilePath();
		return path == null ? "" : path.trim();
	}

	private String targetPath()
	{
		String path = modelObject.getTargetFilePath();
		return path == null ? "" : path.trim();
	}

	private File source()
	{
		String path = sourcePath();
		if (path.isEmpty())
		{
			throw new IllegalArgumentException("choose a file to convert");
		}
		return new File(path);
	}

	private File target()
	{
		String path = targetPath();
		if (path.isEmpty())
		{
			throw new IllegalArgumentException("choose a file to write");
		}
		return new File(path);
	}

	private void run(String what, ConversionOperation operation)
	{
		try
		{
			showResult(operation.execute());
		}
		catch (Exception exception)
		{
			// the old tool logged a stack trace to the console, where nobody using the application
			// would ever see it
			showResult("not " + what + ": " + message(exception));
		}
	}

	private void onBrowseSource()
	{
		onBrowse(txtSourceFile, sourcePath());
	}

	private void onBrowseTarget()
	{
		onBrowse(txtTargetFile, targetPath());
	}

	/**
	 * Lets the user pick a file and writes its path into the given field, whose binding carries it
	 * into the model
	 */
	private void onBrowse(JMTextField field, String chosenPath)
	{
		JFileChooser fileChooser = new JFileChooser();
		if (!chosenPath.isEmpty())
		{
			fileChooser.setSelectedFile(new File(chosenPath));
		}
		if (fileChooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION)
		{
			field.setText(fileChooser.getSelectedFile().getAbsolutePath());
		}
	}

	private static String message(Exception exception)
	{
		return exception.getMessage() != null
			? exception.getMessage()
			: exception.getClass().getSimpleName();
	}

	@FunctionalInterface
	private interface ConversionOperation
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
}
