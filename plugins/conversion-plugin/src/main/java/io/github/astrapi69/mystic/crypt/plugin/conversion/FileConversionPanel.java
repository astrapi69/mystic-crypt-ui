/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.plugin.conversion;

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
import java.util.logging.Level;

import javax.swing.*;

import io.github.astrapi69.crypt.api.key.KeyType;
import io.github.astrapi69.crypt.data.key.reader.CertificateReader;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypt.data.key.reader.PublicKeyReader;
import io.github.astrapi69.crypt.data.key.writer.CertificateWriter;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.crypt.data.key.writer.PublicKeyWriter;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.model.combobox.EnumComboBoxModel;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMTextArea;
import io.github.astrapi69.throwable.ThrowableExtensions;
import lombok.Getter;
import lombok.extern.java.Log;

/**
 * Tool panel that writes a DER encoded key or certificate out in the PEM format.
 * <p>
 * Every value the panel works with lives in its {@link FileConversionModelBean}: the type to
 * convert is chosen in a combo box bound to it, the two files are put there by the file choosers,
 * and the console shows what the model holds. The convert button reads the model, never the
 * widgets.
 */
@Getter
@Log
public class FileConversionPanel extends BasePanel<FileConversionModelBean>
{
	private static final long serialVersionUID = 1L;

	private JButton btnChoose;
	private JButton btnConvert;
	private JButton btnSaveTo;
	private JMComboBox<KeyType, EnumComboBoxModel<KeyType>> cmbChooseType;
	private JFileChooser fileChooser;
	private JLabel lblChoose;
	private JLabel lblChooseType;
	private JLabel lblConsole;
	private JLabel lblSaveTo;
	private JScrollPane srcConsole;
	private JMTextArea txtConsole;

	public FileConversionPanel()
	{
		this(BaseModel.of(FileConversionModelBean.builder().keyType(KeyType.PRIVATE_KEY).build()));
	}

	public FileConversionPanel(final IModel<FileConversionModelBean> model)
	{
		super(model);
	}

	protected void onChooseFile(final ActionEvent actionEvent)
	{
		final int returnVal = fileChooser.showOpenDialog(FileConversionPanel.this);

		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File derFile = fileChooser.getSelectedFile();
			getModelObject().setDerFile(derFile);
			txtConsole.append(
				"Set der file '" + derFile.getName() + "' to convert." + System.lineSeparator());
		}
		else
		{
			txtConsole.append("Set der file command cancelled by user." + System.lineSeparator());
		}
		txtConsole.setCaretPosition(txtConsole.getDocument().getLength());
	}

	protected void onConvert(final ActionEvent actionEvent)
	{
		final File derFile = getModelObject().getDerFile();
		if (derFile == null)
		{
			say("No file chosen - choose a *.der file first.");
			return;
		}
		final File chosenTarget = getModelObject().getPemFile();
		final File pemFile = ConversionSupport.pemFileFor(derFile, chosenTarget);
		say("Conversion started...");
		say("  reading:  " + derFile.getAbsolutePath() + " (" + derFile.length() + " bytes)");
		if (chosenTarget == null)
		{
			// the most common way to lose a converted file is not knowing where it went
			say("  no output file was chosen, writing next to the source");
		}
		say("  writing:  " + pemFile.getAbsolutePath());

		try
		{
			final KeyType keyType = getModelObject().getKeyType();
			switch (keyType)
			{
				case PRIVATE_KEY :
					final PrivateKey privateKey = PrivateKeyReader.readPrivateKey(derFile);
					if (privateKey == null)
					{
						sayNothingWasRead("private key");
						break;
					}
					say("  read a private key: " + describe(privateKey.getAlgorithm(),
						privateKey.getFormat()));
					PrivateKeyWriter.writeInPemFormat(privateKey, pemFile);
					sayWritten("private key", pemFile);
					break;
				case CERTIFICATE :
					final X509Certificate certificate = CertificateReader.readCertificate(derFile);
					if (certificate == null)
					{
						sayNothingWasRead("X.509 certificate");
						break;
					}
					say("  read an X.509 certificate: subject "
						+ certificate.getSubjectX500Principal().getName() + ", signed with "
						+ certificate.getSigAlgName());
					CertificateWriter.writeInPemFormat(certificate, pemFile);
					sayWritten("X.509 certificate", pemFile);
					break;
				case PUBLIC_KEY :
					final PublicKey publicKey = PublicKeyReader.readPublicKey(derFile);
					if (publicKey == null)
					{
						sayNothingWasRead("public key");
						break;
					}
					say("  read a public key: " + describe(publicKey.getAlgorithm(),
						publicKey.getFormat()));
					// in PEM form: this window converts to PEM, and writing the binary encoding
					// into a file called .pem is how a converted key is refused everywhere else
					PublicKeyWriter.writeInPemFormat(publicKey, pemFile);
					sayWritten("public key", pemFile);
					break;
				default :
					say("  nothing was written: this window cannot convert " + keyType);
					break;
			}
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException
			| CertificateException | IOException conversionFailed)
		{
			log.log(Level.SEVERE, conversionFailed.getLocalizedMessage(), conversionFailed);
			say("  nothing was written: " + conversionFailed.getClass().getSimpleName() + " - "
				+ conversionFailed.getMessage());
			say("  the file was read as " + getModelObject().getKeyType()
				+ " - if that is not what it is, choose the right type above");
		}
		say("Conversion finished...");
	}

	/**
	 * Puts one line into the output and keeps the newest line in view
	 *
	 * @param line
	 *            what to say
	 */
	private void say(final String line)
	{
		txtConsole.append(line + System.lineSeparator());
		txtConsole.setCaretPosition(txtConsole.getDocument().getLength());
	}

	/**
	 * Reports what was written, with the size, so an empty file is visible as one
	 *
	 * @param what
	 *            what was written
	 * @param file
	 *            where it went
	 */
	private void sayWritten(final String what, final File file)
	{
		say("  wrote the " + what + ": " + file.getAbsolutePath() + " (" + file.length()
			+ " bytes)");
	}

	/**
	 * Reports that the chosen file did not hold what the selected key type expects - the readers
	 * this method delegates to return {@code null} rather than throwing for input that is not a
	 * DER-encoded instance of the requested type, which is not itself an error state
	 *
	 * @param what
	 *            what was expected to be read
	 */
	private void sayNothingWasRead(final String what)
	{
		say("  nothing was written: the file is not a valid " + what
			+ " - if it is a different key type, choose the right one above");
	}

	/**
	 * Describes a key by what it says about itself
	 *
	 * @param algorithm
	 *            the key algorithm
	 * @param format
	 *            the encoding the key reports
	 * @return the description
	 */
	private static String describe(final String algorithm, final String format)
	{
		return algorithm + ", encoded as " + format;
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		fileChooser = new JFileChooser();
		// -----------------------------

		lblChooseType = new JLabel();
		cmbChooseType = new JMComboBox<>(new EnumComboBoxModel<>(KeyType.class));
		lblChoose = new JLabel();
		btnChoose = new JButton();
		lblSaveTo = new JLabel();
		btnSaveTo = new JButton();
		lblConsole = new JLabel();
		srcConsole = new JScrollPane();
		txtConsole = new JMTextArea();
		btnConvert = new JButton();

		lblChooseType.setText("Choose type to convert");

		// what the model already carries wins; otherwise the tool starts with the configured type
		KeyType keyType = getModelObject().getKeyType() != null
			? getModelObject().getKeyType()
			: ConversionSettingsContribution.keyType();
		getModelObject().setKeyType(keyType);

		lblChoose.setText("Choose private key in *.der format to convert");

		btnChoose.setText("Choose");

		lblSaveTo.setText("Save to *.pem private key");

		btnSaveTo.setText("Save");

		lblConsole.setText("Output");

		txtConsole.setColumns(20);
		txtConsole.setRows(5);
		srcConsole.setViewportView(txtConsole);

		btnConvert.setText("Convert");

		// stable component names for UI tests (AssertJ-Swing lookups by name)
		btnChoose.setName("btnChoose");
		btnSaveTo.setName("btnSaveTo");
		btnConvert.setName("btnConvert");
		txtConsole.setName("txtConsole");
		cmbChooseType.setName("cmbChooseType");

		bindComponents();

		// -----------------------------

		btnChoose.addActionListener(this::onChooseFile);

		btnSaveTo.addActionListener(this::onSaveFile);

		btnConvert.addActionListener(this::onConvert);
	}

	/**
	 * Binds the components to the panel's model, so that what is chosen is in the model at once and
	 * the console shows what the model holds. The combo box carries the type the model already
	 * names, and every line appended to the console lands in the model with it.
	 */
	private void bindComponents()
	{
		final FileConversionModelBean modelObject = getModelObject();
		cmbChooseType
			.setPropertyModel(LambdaModel.of(modelObject::getKeyType, modelObject::setKeyType));
		txtConsole.setPropertyModel(
			LambdaModel.of(modelObject::getConsoleOutput, modelObject::setConsoleOutput));
	}

	/**
	 * Initialize layout.
	 */
	protected void onInitializeGroupLayout()
	{

		final GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addGap(45, 45, 45)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
					.addComponent(srcConsole)
					.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
							.addComponent(lblConsole, GroupLayout.DEFAULT_SIZE,
								GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(lblSaveTo, GroupLayout.DEFAULT_SIZE,
								GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(lblChoose, GroupLayout.DEFAULT_SIZE, 469, Short.MAX_VALUE)
							.addComponent(lblChooseType, GroupLayout.DEFAULT_SIZE,
								GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
						.addGap(104, 104, 104)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
							.addComponent(cmbChooseType, 0, 190, Short.MAX_VALUE)
							.addComponent(btnChoose, GroupLayout.DEFAULT_SIZE,
								GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(btnSaveTo, GroupLayout.DEFAULT_SIZE,
								GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(btnConvert, GroupLayout.DEFAULT_SIZE,
								GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
				.addContainerGap(59, Short.MAX_VALUE)));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addGap(59, 59, 59)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
					.addComponent(cmbChooseType).addComponent(lblChooseType,
						GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				.addGap(25, 25, 25)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(lblChoose, GroupLayout.PREFERRED_SIZE, 25,
						GroupLayout.PREFERRED_SIZE)
					.addComponent(btnChoose))
				.addGap(30, 30, 30)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(btnSaveTo).addComponent(lblSaveTo, GroupLayout.PREFERRED_SIZE, 26,
						GroupLayout.PREFERRED_SIZE))
				.addGap(34, 34, 34)
				.addComponent(lblConsole, GroupLayout.PREFERRED_SIZE, 27,
					GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
				.addComponent(srcConsole, GroupLayout.PREFERRED_SIZE, 244,
					GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 32, Short.MAX_VALUE)
				.addComponent(btnConvert).addGap(20, 20, 20)));
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
	}

	protected void onSaveFile(final ActionEvent actionEvent)
	{
		final int returnVal = fileChooser.showSaveDialog(FileConversionPanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File pemFile = fileChooser.getSelectedFile();
			getModelObject().setPemFile(pemFile);
			txtConsole.append("Set pem file '" + pemFile.getName() + "' to insert output."
				+ System.lineSeparator());
		}
		else
		{
			txtConsole.append("Set pem file command cancelled by user." + System.lineSeparator());
		}
		txtConsole.setCaretPosition(txtConsole.getDocument().getLength());
	}
}
