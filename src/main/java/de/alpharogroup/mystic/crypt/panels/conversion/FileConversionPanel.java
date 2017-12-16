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
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle;

import org.apache.log4j.Logger;

import de.alpharogroup.crypto.key.KeyType;
import de.alpharogroup.crypto.key.reader.CertificateReader;
import de.alpharogroup.crypto.key.reader.PrivateKeyReader;
import de.alpharogroup.crypto.key.reader.PublicKeyReader;
import de.alpharogroup.crypto.key.writer.CertificateWriter;
import de.alpharogroup.crypto.key.writer.PrivateKeyWriter;
import de.alpharogroup.crypto.key.writer.PublicKeyWriter;
import de.alpharogroup.exception.ExceptionExtensions;
import de.alpharogroup.model.BaseModel;
import de.alpharogroup.model.api.Model;
import de.alpharogroup.swing.base.BasePanel;
import lombok.Getter;

@Getter
public class FileConversionPanel extends BasePanel<FileConversionModelBean>
{
	/** The Constant logger. */
	protected static final Logger logger = Logger.getLogger(FileConversionPanel.class.getName());

	private static final long serialVersionUID = 1L;

    private JButton btnChoose;
    private JButton btnConvert;
    private JButton btnSaveTo;
    private JComboBox<String> cmbChooseType;
    private JLabel lblChoose;
    private JLabel lblChooseType;
    private JLabel lblConsole;
    private JLabel lblSaveTo;
    private JScrollPane srcConsole;
    private JTextArea txtConsole;
    private JFileChooser fileChooser;

	public FileConversionPanel()
	{
		this(BaseModel.<FileConversionModelBean>of(FileConversionModelBean.builder().keyType(KeyType.PRIVATE_KEY).build()));
	}
	public FileConversionPanel(final Model<FileConversionModelBean> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		fileChooser = new JFileChooser();
		// -----------------------------

        lblChooseType = new JLabel();
        cmbChooseType = new JComboBox<>();
        lblChoose = new JLabel();
        btnChoose = new JButton();
        lblSaveTo = new JLabel();
        btnSaveTo = new JButton();
        lblConsole = new JLabel();
        srcConsole = new JScrollPane();
        txtConsole = new JTextArea();
        btnConvert = new JButton();

        lblChooseType.setText("Choose type to convert");

        cmbChooseType.setModel(new DefaultComboBoxModel(KeyType.values()));
        cmbChooseType.setSelectedItem(getModelObject().getKeyType());
        cmbChooseType.addActionListener(actionEvent -> onChangeKeyType(actionEvent));

        lblChoose.setText("Choose private key in *.der format to convert");

        btnChoose.setText("Choose");

        lblSaveTo.setText("Save to *.pem private key");

        btnSaveTo.setText("Save");

        lblConsole.setText("Output");

        txtConsole.setColumns(20);
        txtConsole.setRows(5);
        srcConsole.setViewportView(txtConsole);

        btnConvert.setText("Convert");

		// -----------------------------

        btnChoose.addActionListener(actionEvent -> onChooseFile(actionEvent));

        btnSaveTo.addActionListener(actionEvent -> onSaveFile(actionEvent));

        btnConvert.addActionListener(actionEvent -> onConvert(actionEvent));
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
	}

	/**
	 * Callback method that can be overwritten to provide specific action for the on change key
	 * size.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	@SuppressWarnings("unchecked")
	protected void onChangeKeyType(final ActionEvent actionEvent)
	{
		final JComboBox<String> cb = (JComboBox<String>)actionEvent.getSource();
		final KeyType selected = (KeyType)cb.getSelectedItem();
		getModelObject().setKeyType(selected);
	}

	protected void onConvert(final ActionEvent actionEvent)
	{
		txtConsole.append("Coversion started...");

		try
		{
			final KeyType keyType = getModelObject().getKeyType();
			switch (keyType)
			{
				case PRIVATE_KEY :
					final PrivateKey privateKey = PrivateKeyReader.readPrivateKey(getModelObject().getDerFile());
					PrivateKeyWriter.writeInPemFormat(privateKey, getModelObject().getPemFile());
					txtConsole.append("private key written to file...");
					break;
				case CERTIFICATE :
					final X509Certificate certificate = CertificateReader.readCertificate(getModelObject().getDerFile());
					CertificateWriter.writeInPemFormat(certificate, getModelObject().getPemFile());
					txtConsole.append("X.509 certificate written to file...");
					break;
				case PUBLIC_KEY :
					final PublicKey publicKey = PublicKeyReader.readPublicKey(getModelObject().getDerFile());
					PublicKeyWriter.write(publicKey, getModelObject().getPemFile());
					txtConsole.append("public key written to file...");
					break;
				default :
					txtConsole.append("unknown key type...");
					break;
			}

		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException | CertificateException
			| IOException e)
		{
			txtConsole.append(ExceptionExtensions.getStackTrace(e));
			e.printStackTrace();
		}
	}

	protected void onSaveFile(final ActionEvent actionEvent)
	{
		final int returnVal = fileChooser.showSaveDialog(FileConversionPanel.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File pemFile = fileChooser.getSelectedFile();
            getModelObject().setPemFile(pemFile);
            txtConsole.append("Set pem file '" + pemFile.getName() + "' to insert output." + System.lineSeparator());
        } else {
        	txtConsole.append("Set pem file command cancelled by user." + System.lineSeparator());
        }
        txtConsole.setCaretPosition(txtConsole.getDocument().getLength());
	}

	protected void onChooseFile(final ActionEvent actionEvent)
	{
		final int returnVal = fileChooser.showOpenDialog(FileConversionPanel.this);

		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File derFile = fileChooser.getSelectedFile();
            getModelObject().setDerFile(derFile);
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
	protected void onInitializeGroupLayout()
	{

        final GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(45, 45, 45)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                    .addComponent(srcConsole)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                            .addComponent(lblConsole, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblSaveTo, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblChoose, GroupLayout.DEFAULT_SIZE, 469, Short.MAX_VALUE)
                            .addComponent(lblChooseType, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(104, 104, 104)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                            .addComponent(cmbChooseType, 0, 190, Short.MAX_VALUE)
                            .addComponent(btnChoose, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnSaveTo, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnConvert, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap(59, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(59, 59, 59)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                    .addComponent(cmbChooseType)
                    .addComponent(lblChooseType, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(lblChoose, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnChoose))
                .addGap(30, 30, 30)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSaveTo)
                    .addComponent(lblSaveTo, GroupLayout.PREFERRED_SIZE, 26, GroupLayout.PREFERRED_SIZE))
                .addGap(34, 34, 34)
                .addComponent(lblConsole, GroupLayout.PREFERRED_SIZE, 27, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(srcConsole, GroupLayout.PREFERRED_SIZE, 244, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 32, Short.MAX_VALUE)
                .addComponent(btnConvert)
                .addGap(20, 20, 20))
        );
	}
}
