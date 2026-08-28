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
package io.github.astrapi69.mystic.crypt.plugin.keygen;

import io.github.astrapi69.mystic.crypt.panel.keygen.EnDecryptPanel;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.util.logging.Level;

import javax.swing.*;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.key.KeyFormat;
import io.github.astrapi69.crypt.api.key.KeySize;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.PrivateKeyExtensions;
import io.github.astrapi69.crypt.data.key.PublicKeyExtensions;
import io.github.astrapi69.crypt.data.key.writer.EncryptedPrivateKeyWriter;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.crypt.data.key.writer.PublicKeyWriter;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.key.PrivateKeyHexDecryptor;
import io.github.astrapi69.mystic.crypt.key.PublicKeyHexEncryptor;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.model.component.JMComboBox;
import lombok.Getter;
import lombok.extern.java.Log;

@Getter
@Log
public class GenerateKeysPanel extends BasePanel<GenerateKeysModelBean>
{

	private static final long serialVersionUID = 1L;

	/**
	 * The algorithms offered in the key-pair dropdown: RSA (classical, drives the full
	 * encrypt/decrypt demo) plus a curated set of modern algorithms - the X25519/X448 key-agreement
	 * curves and the NIST post-quantum ML-KEM (key encapsulation) and ML-DSA (signature) parameter
	 * sets. The modern ones only generate and display their key pair as PEM; the RSA-only hex
	 * encrypt/decrypt demo does not apply to them.
	 */
	private JMComboBox<String, ?> cmbCurve;
	private JMComboBox<KeyFormat, ?> cmbKeyFormat;

	private static final KeyPairGeneratorAlgorithm[] SUPPORTED_ALGORITHMS = {
			KeyPairGeneratorAlgorithm.RSA, KeyPairGeneratorAlgorithm.EC,
			KeyPairGeneratorAlgorithm.X25519,
			KeyPairGeneratorAlgorithm.X448, KeyPairGeneratorAlgorithm.ML_KEM_768,
			KeyPairGeneratorAlgorithm.ML_DSA_65 };

	private CryptographyPanel cryptographyPanel;

	private EnDecryptPanel enDecryptPanel;

	private JMComboBox<KeyPairGeneratorAlgorithm, ?> cmbAlgorithm;

	private JLabel lblAlgorithm;

	public GenerateKeysPanel()
	{
		this(BaseModel.of(GenerateKeysModelBean.builder().build()));
	}

	public GenerateKeysPanel(final IModel<GenerateKeysModelBean> model)
	{
		super(model);
	}

	/**
	 * Callback for the algorithm dropdown: the bound box has already written the chosen algorithm
	 * into the model, so this only keeps the key size relevant for RSA, since the modern algorithms
	 * have fixed parameter sets.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onChangeAlgorithm(final ActionEvent actionEvent)
	{
		final KeyPairGeneratorAlgorithm algorithm = getModelObject().getAlgorithm();
		if (algorithm != null)
		{
			getCryptographyPanel().getCmbKeySize()
				.setEnabled(KeyPairGeneratorAlgorithm.RSA.equals(algorithm));
		}
	}

	/**
	 * Callback method that can be overwritten to provide specific action for the on clear.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onClear(final ActionEvent actionEvent)
	{
		cmbAlgorithm.setSelectedItem(KeygenSettingsContribution.algorithm());
		getCryptographyPanel().getCmbKeySize().setEnabled(true);
		getCryptographyPanel().getCmbKeySize().setSelectedItem(KeySize.KEYSIZE_1024);
		getCryptographyPanel().getTxtPrivateKey().setText("");
		getCryptographyPanel().getTxtPublicKey().setText("");
		getEnDecryptPanel().getTxtEncrypted().setText("");
		getEnDecryptPanel().getTxtToEncrypt().setText("");
		getCryptographyPanel().getBtnSaveCertificate().setEnabled(false);
		// the algorithm, the key size and both key areas are bound, so setting them above has put
		// them into the model as well; what is left is the state no component shows
		getModelObject().setDecryptor(null);
		getModelObject().setEncryptor(null);
		getModelObject().setPrivateKey(null);
		getModelObject().setPublicKey(null);
	}

	/**
	 * Callback method that can be overwritten to provide specific action for the on decrypt.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onDecrypt(final ActionEvent actionEvent)
	{
		if (getModelObject().getDecryptor() == null)
		{
			showEncryptDecryptUnavailable();
			return;
		}
		try
		{
			final String decryted = getModelObject().getDecryptor()
				.decrypt(getEnDecryptPanel().getTxtEncrypted().getText());
			getEnDecryptPanel().getTxtToEncrypt().setText(decryted);
			getEnDecryptPanel().getTxtEncrypted().setText("");
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}

	}

	// callbacks

	/**
	 * Callback method that can be overwritten to provide specific action for the on encrypt.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onEncrypt(final ActionEvent actionEvent)
	{
		if (getModelObject().getEncryptor() == null)
		{
			showEncryptDecryptUnavailable();
			return;
		}
		try
		{
			getEnDecryptPanel().getTxtEncrypted().setText(getModelObject().getEncryptor()
				.encrypt(getEnDecryptPanel().getTxtToEncrypt().getText()));
			getEnDecryptPanel().getTxtToEncrypt().setText("");
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}

	}

	/**
	 * Tells the user that the hex encrypt/decrypt demo only works with an RSA key pair, which is the
	 * case when no {@link PublicKeyHexEncryptor}/{@link PrivateKeyHexDecryptor} was built for the
	 * generated key pair (the modern key-agreement and post-quantum algorithms).
	 */
	private void showEncryptDecryptUnavailable()
	{
		JOptionPane.showMessageDialog(this,
			"Encrypt/decrypt is available for RSA keys only. Select RSA and generate a key pair to use it.",
			"Not available for this algorithm", JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Callback method that can be overwritten to provide specific action for the on generate.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onGenerate(final ActionEvent actionEvent)
	{
		final KeyPairGeneratorAlgorithm algorithm = getModelObject().getAlgorithm();
		final boolean rsa = KeyPairGeneratorAlgorithm.RSA.equals(algorithm);
		getCryptographyPanel().getTxtPrivateKey().setText("Generating private key...");
		getCryptographyPanel().getTxtPublicKey().setText("Generating public key...");
		try
		{
			final KeyPair keyPair;
			if (rsa)
			{
				final KeySize selected = getModelObject().getKeySize();
				keyPair = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA,
					selected.getKeySize());
			}
			else if (KeyPairGeneratorAlgorithm.EC.equals(algorithm))
			{
				// an EC key without a named curve leaves the choice to the provider, and what it
				// chooses is neither written down nor the same everywhere
				String curve = getModelObject().getCurve();
				try
				{
					keyPair = KeygenSupport.newEcKeyPair(curve);
				}
				catch (Exception thisMachineDoesNotKnowThatCurve)
				{
					throw new NoSuchAlgorithmException(
						"this machine cannot generate a key on the curve '" + curve + "'",
						thisMachineDoesNotKnowThatCurve);
				}
			}
			else
			{
				// the modern algorithms (X25519/X448, ML-KEM, ML-DSA) have fixed parameter sets and
				// take no classical key size
				keyPair = KeyPairFactory.newKeyPair(algorithm);
			}

			getModelObject().setPrivateKey(keyPair.getPrivate());
			getModelObject().setPublicKey(keyPair.getPublic());

			if (rsa)
			{
				getModelObject()
					.setDecryptor(new PrivateKeyHexDecryptor(getModelObject().getPrivateKey()));
				getModelObject()
					.setEncryptor(new PublicKeyHexEncryptor(getModelObject().getPublicKey()));
			}
			else
			{
				// no hex public-key encrypt/decrypt demo for key-agreement/signature algorithms
				getModelObject().setDecryptor(null);
				getModelObject().setEncryptor(null);
				getEnDecryptPanel().getTxtToEncrypt().setText("");
				getEnDecryptPanel().getTxtEncrypted().setText("");
			}

			final String privateKeyFormat = PrivateKeyExtensions
				.toPemFormat(getModelObject().getPrivateKey());

			final String publicKeyFormat = PublicKeyExtensions
				.toPemFormat(getModelObject().getPublicKey());

			getCryptographyPanel().getTxtPrivateKey().setText("");
			getCryptographyPanel().getTxtPublicKey().setText("");
			getCryptographyPanel().getTxtPrivateKey().setText(privateKeyFormat);
			getCryptographyPanel().getTxtPublicKey().setText(publicKeyFormat);
			// certificate creation only wired for RSA in this demo
			getCryptographyPanel().getBtnSaveCertificate().setEnabled(rsa);
		}
		catch (final NoSuchAlgorithmException | NoSuchProviderException | IOException e)
		{
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			getCryptographyPanel().getTxtPrivateKey().setText("");
			getCryptographyPanel().getTxtPublicKey().setText("");
		}

	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		lblAlgorithm = new JLabel("Algorithm");
		cmbCurve = new JMComboBox<>(KeygenSupport.CURVES.toArray(new String[0]));
		cmbCurve.setName("cmbCurve");
		cmbCurve.setToolTipText("the curve an EC key sits on - a certificate or a wallet usually "
			+ "requires one particular one");
		cmbKeyFormat = new JMComboBox<>(KeygenSupport.keyFormats().toArray(new KeyFormat[0]));
		cmbKeyFormat.setName("cmbKeyFormat");
		cmbKeyFormat.setRenderer(new javax.swing.DefaultListCellRenderer()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list,
				Object value, int index, boolean isSelected, boolean cellHasFocus)
			{
				return super.getListCellRendererComponent(list,
					value == null ? "" : KeygenSupport.displayName((KeyFormat)value), index,
					isSelected, cellHasFocus);
			}
		});
		cmbAlgorithm = new JMComboBox<>(SUPPORTED_ALGORITHMS);
		cmbAlgorithm.setName("cmbAlgorithm");

		// the tool starts with the algorithm the user configured in the settings dialog and with
		// the first curve and key file format it offers, which is what the boxes showed before;
		// the components take all three from the model when they are bound to it
		getModelObject().setAlgorithm(KeygenSettingsContribution.algorithm());
		getModelObject().setCurve(KeygenSupport.CURVES.get(0));
		getModelObject().setKeyFormat(KeygenSupport.keyFormats().get(0));
		bindComponents();

		// the listener comes after the binding, because it reaches for the key generation panel
		// that is built below
		cmbAlgorithm.addActionListener(actionEvent -> onChangeAlgorithm(actionEvent));

		cryptographyPanel = new CryptographyPanel(getModel())
		{

			private static final long serialVersionUID = 1L;

			@Override
			protected void onClear(final ActionEvent actionEvent)
			{
				GenerateKeysPanel.this.onClear(actionEvent);
			}

			@Override
			protected void onGenerate(final ActionEvent actionEvent)
			{
				GenerateKeysPanel.this.onGenerate(actionEvent);
			}

			@Override
			protected void onSavePrivateKey(final ActionEvent actionEvent)
			{
				GenerateKeysPanel.this.onSavePrivateKey(actionEvent);
			}

			@Override
			protected void onSavePrivateKeyWithPassword(final ActionEvent actionEvent)
			{
				GenerateKeysPanel.this.onSavePrivateKeyWithPassword(actionEvent);
			}

			@Override
			protected void onSavePublicKey(final ActionEvent actionEvent)
			{
				GenerateKeysPanel.this.onSavePublicKey(actionEvent);
			}
		};
		enDecryptPanel = new EnDecryptPanel()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onDecrypt(final ActionEvent actionEvent)
			{
				GenerateKeysPanel.this.onDecrypt(actionEvent);
			}

			@Override
			protected void onEncrypt(final ActionEvent actionEvent)
			{
				GenerateKeysPanel.this.onEncrypt(actionEvent);
			}
		};
	}


	/**
	 * Binds the algorithm, the curve and the key file format box to the model of this panel, so
	 * that what was chosen is readable from the model at any moment instead of out of the boxes
	 */
	private void bindComponents()
	{
		final GenerateKeysModelBean modelObject = getModelObject();
		cmbAlgorithm.setPropertyModel(
			LambdaModel.of(modelObject::getAlgorithm, modelObject::setAlgorithm));
		cmbCurve.setPropertyModel(LambdaModel.of(modelObject::getCurve, modelObject::setCurve));
		cmbKeyFormat.setPropertyModel(
			LambdaModel.of(modelObject::getKeyFormat, modelObject::setKeyFormat));
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeMigLayout();
	}

	/**
	 * Lays this tool window out with the layout every tool window in this application uses: the
	 * algorithm, the curve and the key file format in labelled rows, the key generation panel
	 * taking the height the window has left and the encrypt/decrypt panel under it.
	 */
	protected void onInitializeMigLayout()
	{
		setLayout(ToolForm.newLayout());
		add(lblAlgorithm);
		add(cmbAlgorithm, ToolForm.FIELD);
		add(new javax.swing.JLabel("Curve (EC):"));
		add(cmbCurve, ToolForm.FIELD);
		add(new javax.swing.JLabel("Private key file format:"));
		add(cmbKeyFormat, ToolForm.FIELD);
		add(cryptographyPanel, ToolForm.GROWING);
		add(enDecryptPanel, ToolForm.WIDE);
	}

	/**
	 * Callback method that can be overwritten to provide specific action for the on save private
	 * key.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onSavePrivateKey(final ActionEvent actionEvent)
	{
		final JFileChooser fileChooser = new JFileChooser();
		final int state = fileChooser.showSaveDialog(this);
		if (state == JFileChooser.APPROVE_OPTION)
		{
			savePrivateKeyTo(fileChooser.getSelectedFile());
		}
	}

	/**
	 * Writes the generated private key to the given file, in the key file format the model holds
	 *
	 * @param file
	 *            the file the private key is written to
	 */
	protected void savePrivateKeyTo(final File file)
	{
		try
		{
			KeygenSupport.writePrivateKey(getModelObject().getPrivateKey(), file,
				getModelObject().getKeyFormat());
		}
		catch (final Exception e)
		{
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}


	protected void onSavePrivateKeyWithPassword(final ActionEvent actionEvent)
	{
		final Object[] options = { "Set password", "Cancel" };
		final PasswordPanel panel = new PasswordPanel();

		final int result = JOptionPane.showOptionDialog(null, panel, "Enter a password",
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
		final PasswordBean enteredPasswords = panel.getModelObject();
		final String password = String.copyValueOf(enteredPasswords.getPassword());
		final String repeatPassword = String.copyValueOf(enteredPasswords.getRepeatPassword());
		if (result == 0 && 0 < password.length())
		{
			if (password.equals(repeatPassword))
			{

				final JFileChooser fileChooser = new JFileChooser();
				final int state = fileChooser.showSaveDialog(this);
				if (state == JFileChooser.APPROVE_OPTION)
				{
					PrivateKey privateKey;
					try
					{
						privateKey = getModelObject().getPrivateKey();
						File selectedFile = fileChooser.getSelectedFile();
						EncryptedPrivateKeyWriter.encryptPrivateKeyWithPassword(privateKey,
							selectedFile, password);
					}
					catch (final Exception e)
					{
						log.log(Level.SEVERE, e.getLocalizedMessage(), e);
					}
				}
			}
			else
			{
				JOptionPane.showMessageDialog(null, "Entered passwords are not the same.");
			}
		}
	}


	/**
	 * Callback method that can be overwritten to provide specific action for the on save public
	 * key.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onSavePublicKey(final ActionEvent actionEvent)
	{
		final JFileChooser fileChooser = new JFileChooser();
		final int state = fileChooser.showSaveDialog(this);
		if (state == JFileChooser.APPROVE_OPTION)
		{
			try
			{
				PublicKeyWriter.write(getModelObject().getPublicKey(),
					fileChooser.getSelectedFile());
			}
			catch (final Exception ex)
			{
				log.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
			}
		}
	}
}
