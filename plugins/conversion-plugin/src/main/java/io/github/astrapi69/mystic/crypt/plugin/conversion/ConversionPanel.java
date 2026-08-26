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

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.*;

/**
 * Tool panel for converting a key or certificate file between the shapes it can have.
 * <p>
 * Nothing is declared here. Choosing a file says what it holds - a PEM file carries its type in its
 * header, a DER file is decided by what can be read out of it - and only the conversions that make
 * sense for it stay available. The old tool asked the user to say what the file was and produced a
 * broken file, or a stack trace on the console, when that guess was wrong.
 */
public class ConversionPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final JTextField txtSourceFile = new JTextField(38);
	private final JTextField txtTargetFile = new JTextField(38);
	private final JLabel lblWhatItHolds = new JLabel(" ");
	private final JLabel lblResult = new JLabel(" ");
	private final JButton btnPemToDer = button("btnPemToDer", "to DER", event -> onPemToDer());
	private final JButton btnDerToPem = button("btnDerToPem", "to PEM", event -> onDerToPem());
	private final JButton btnToPkcs8 = button("btnToPkcs8", "to PKCS#8 (Java)",
		event -> onToPkcs8());
	private final JButton btnToPkcs1 = button("btnToPkcs1", "to PKCS#1 (openssl)",
		event -> onToPkcs1());

	public ConversionPanel()
	{
		super(new BorderLayout(4, 4));

		txtSourceFile.setName("txtSourceFile");
		txtTargetFile.setName("txtTargetFile");
		lblWhatItHolds.setName("lblWhatItHolds");
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));

		JPanel form = new JPanel(new GridBagLayout());
		int row = 0;
		form.add(new JLabel("File:"), at(0, row, GridBagConstraints.EAST));
		form.add(txtSourceFile, at(1, row, GridBagConstraints.WEST));
		form.add(button("btnBrowseSource", "...", event -> onBrowseSource()),
			at(2, row++, GridBagConstraints.WEST));
		form.add(new JLabel("It holds:"), at(0, row, GridBagConstraints.EAST));
		form.add(lblWhatItHolds, at(1, row++, GridBagConstraints.WEST));
		form.add(new JLabel("Write to:"), at(0, row, GridBagConstraints.EAST));
		form.add(txtTargetFile, at(1, row, GridBagConstraints.WEST));
		form.add(button("btnBrowseTarget", "...", event -> onBrowse(txtTargetFile)),
			at(2, row++, GridBagConstraints.WEST));
		form.add(buttonRow(btnPemToDer, btnDerToPem, btnToPkcs8, btnToPkcs1),
			at(1, row, GridBagConstraints.WEST));

		// the file itself says what it is, so the tool looks as soon as there is a path - typed,
		// pasted or picked
		txtSourceFile.getDocument()
			.addDocumentListener(new javax.swing.event.DocumentListener()
			{
				@Override
				public void insertUpdate(javax.swing.event.DocumentEvent event)
				{
					onSourceChosen();
				}

				@Override
				public void removeUpdate(javax.swing.event.DocumentEvent event)
				{
					onSourceChosen();
				}

				@Override
				public void changedUpdate(javax.swing.event.DocumentEvent event)
				{
					onSourceChosen();
				}
			});

		add(form, BorderLayout.CENTER);
		add(lblResult, BorderLayout.SOUTH);
		setConversionsFor(null);
	}

	/** Looks at the chosen file and offers only what can be done with it */
	protected void onSourceChosen()
	{
		if (txtSourceFile.getText().isBlank())
		{
			lblWhatItHolds.setText(" ");
			setConversionsFor(null);
			return;
		}
		try
		{
			ConversionSupport.FileKind kind = ConversionSupport
				.kindOf(new File(txtSourceFile.getText().trim()));
			lblWhatItHolds.setText(kind.description());
			setConversionsFor(kind);
			lblResult.setText(" ");
		}
		catch (Exception exception)
		{
			lblWhatItHolds.setText(" ");
			setConversionsFor(null);
			lblResult.setText("not read: " + message(exception));
		}
	}

	private void setConversionsFor(ConversionSupport.FileKind kind)
	{
		boolean readable = kind != null;
		btnPemToDer.setEnabled(readable && kind.pem());
		btnDerToPem.setEnabled(readable && !kind.pem()
			&& !kind.description().startsWith("nothing"));
		boolean privateKey = readable
			&& ConversionSupport.holdsAPrivateKey(new File(txtSourceFile.getText().trim()));
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
		return lblResult.getText();
	}

	/** What the chosen file was found to hold */
	public String getWhatItHoldsText()
	{
		return lblWhatItHolds.getText();
	}

	private File source()
	{
		String path = txtSourceFile.getText().trim();
		if (path.isEmpty())
		{
			throw new IllegalArgumentException("choose a file to convert");
		}
		return new File(path);
	}

	private File target()
	{
		String path = txtTargetFile.getText().trim();
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
			lblResult.setText(operation.execute());
		}
		catch (Exception exception)
		{
			// the old tool logged a stack trace to the console, where nobody using the application
			// would ever see it
			lblResult.setText("not " + what + ": " + message(exception));
		}
	}

	private void onBrowseSource()
	{
		onBrowse(txtSourceFile);
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
