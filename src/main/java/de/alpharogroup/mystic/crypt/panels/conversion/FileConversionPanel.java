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
package de.alpharogroup.mystic.crypt.panels.conversion;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXPanel;

import de.alpharogroup.crypto.key.reader.PrivateKeyReader;
import de.alpharogroup.crypto.key.writer.PrivateKeyWriter;
import de.alpharogroup.exception.ExceptionExtensions;
import lombok.Getter;

@Getter
public class FileConversionPanel extends JXPanel
{
	/** The Constant logger. */
	protected static final Logger logger = Logger.getLogger(FileConversionPanel.class.getName());

	private static final long serialVersionUID = 1L;

	private final FileConversionModelBean model = FileConversionModelBean.builder().build();

    private JButton btnChoose;
    private JButton btnConvert;
    private JButton btnSaveTo;
    private JScrollPane jScrollPane1;
    private JLabel lblChoose;
    private JLabel lblConsole;
    private JLabel lblSaveTo;
    private JTextArea txtConsole;
    JFileChooser fileChooser;

	public FileConversionPanel()
	{
		initialize();
	}

	/**
	 * Initialize Panel.
	 */
	protected void initialize()
	{
		initializeComponents();
		initializeLayout();
	}


	/**
	 * Initialize components.
	 */
	protected void initializeComponents()
	{
		fileChooser = new JFileChooser();

        btnChoose = new JButton();
        lblChoose = new JLabel();
        lblSaveTo = new JLabel();
        btnSaveTo = new JButton();
        jScrollPane1 = new JScrollPane();
        txtConsole = new JTextArea();
        lblConsole = new JLabel();
        btnConvert = new JButton();

        btnChoose.setText("Choose");
        btnChoose.addActionListener(actionEvent -> onChooseFile(actionEvent));

        lblChoose.setText("Choose private key in *.der format to convert");

        lblSaveTo.setText("Save to *.pem private key");

        btnSaveTo.setText("Save");
        btnSaveTo.addActionListener(actionEvent -> onSaveFile(actionEvent));

        txtConsole.setColumns(20);
        txtConsole.setRows(5);
        jScrollPane1.setViewportView(txtConsole);

        lblConsole.setText("Output");

        btnConvert.setText("Convert");
        btnConvert.addActionListener(actionEvent -> onConvert(actionEvent));

	}

	protected void onConvert(ActionEvent actionEvent)
	{
		txtConsole.append("Coversion started...");

		try
		{
			final PrivateKey privateKey = PrivateKeyReader.readPrivateKey(model.getDerFile());
			PrivateKeyWriter.writeInPemFormat(privateKey, model.getPemFile());
			txtConsole.append("");
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException
			| IOException e)
		{
			txtConsole.append(ExceptionExtensions.getStackTrace(e));
			e.printStackTrace();
		}
	}

	protected void onSaveFile(ActionEvent actionEvent)
	{
		int returnVal = fileChooser.showSaveDialog(FileConversionPanel.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File pemFile = fileChooser.getSelectedFile();
            model.setPemFile(pemFile);
            txtConsole.append("Set pem file '" + pemFile.getName() + "' to insert output." + System.lineSeparator());
        } else {
        	txtConsole.append("Set pem file command cancelled by user." + System.lineSeparator());
        }
        txtConsole.setCaretPosition(txtConsole.getDocument().getLength());
	}

	protected void onChooseFile(ActionEvent actionEvent)
	{
		int returnVal = fileChooser.showOpenDialog(FileConversionPanel.this);

		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			File derFile = fileChooser.getSelectedFile();
            model.setDerFile(derFile);
            txtConsole.append("Set der file '" + derFile.getName() + "' to convert." + System.lineSeparator());
		}
		else
		{
        	txtConsole.append("Set der file command cancelled by user." + System.lineSeparator());
		}
		txtConsole.setCaretPosition(txtConsole.getDocument().getLength());
	}

	/**
	 * Initialize layout.
	 */
	protected void initializeLayout()
	{

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(37, 37, 37)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblConsole, GroupLayout.PREFERRED_SIZE, 149, GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(btnConvert))
                            .addComponent(jScrollPane1)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                    .addComponent(lblChoose, GroupLayout.DEFAULT_SIZE, 280, Short.MAX_VALUE)
                                    .addComponent(lblSaveTo, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 80, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                    .addComponent(btnChoose, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(btnSaveTo, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                        .addGap(55, 55, 55))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(43, 43, 43)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(btnChoose)
                    .addComponent(lblChoose))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(lblSaveTo)
                    .addComponent(btnSaveTo))
                .addGap(18, 18, 18)
                .addComponent(lblConsole)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, GroupLayout.PREFERRED_SIZE, 107, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnConvert)
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
	}
}
