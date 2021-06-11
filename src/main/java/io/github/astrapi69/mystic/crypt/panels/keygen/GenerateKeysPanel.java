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
package io.github.astrapi69.mystic.crypt.panels.keygen;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.commons.codec.DecoderException;

import io.github.astrapi69.crypto.algorithm.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypto.factories.KeyPairFactory;
import io.github.astrapi69.crypto.key.KeySize;
import io.github.astrapi69.crypto.key.PrivateKeyExtensions;
import io.github.astrapi69.crypto.key.PrivateKeyHexDecryptor;
import io.github.astrapi69.crypto.key.PublicKeyExtensions;
import io.github.astrapi69.crypto.key.PublicKeyHexEncryptor;
import io.github.astrapi69.crypto.key.writer.EncryptedPrivateKeyWriter;
import io.github.astrapi69.crypto.key.writer.PrivateKeyWriter;
import io.github.astrapi69.crypto.key.writer.PublicKeyWriter;
import de.alpharogroup.model.BaseModel;
import de.alpharogroup.model.api.Model;
import de.alpharogroup.swing.base.BasePanel;
import lombok.Getter;
import lombok.extern.java.Log;
import net.miginfocom.swing.MigLayout;

@Getter
@Log
public class GenerateKeysPanel extends BasePanel<GenerateKeysModelBean>
{

	private static final long serialVersionUID = 1L;

	private CryptographyPanel cryptographyPanel;

	private EnDecryptPanel enDecryptPanel;

	public GenerateKeysPanel()
	{
		this(BaseModel.<GenerateKeysModelBean> of(GenerateKeysModelBean.builder().build()));
	}

	public GenerateKeysPanel(final Model<GenerateKeysModelBean> model)
	{
		super(model);
	}

	/**
	 * Callback method that can be overwritten to provide specific action for the on change key
	 * size.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	@SuppressWarnings("unchecked")
	protected void onChangeKeySize(final ActionEvent actionEvent)
	{
		final JComboBox<String> cb = (JComboBox<String>)actionEvent.getSource();
		final KeySize selected = (KeySize)cb.getSelectedItem();
		getModelObject().setKeySize(selected);
	}

	/**
	 * Callback method that can be overwritten to provide specific action for the on clear.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onClear(final ActionEvent actionEvent)
	{
		getCryptographyPanel().getCmbKeySize().setSelectedItem(KeySize.KEYSIZE_1024);
		getCryptographyPanel().getTxtPrivateKey().setText("");
		getCryptographyPanel().getTxtPublicKey().setText("");
		getEnDecryptPanel().getTxtEncrypted().setText("");
		getEnDecryptPanel().getTxtToEncrypt().setText("");
		getCryptographyPanel().getBtnSaveCertificate().setEnabled(false);
		getModelObject().setDecryptor(null);
		getModelObject().setEncryptor(null);
		getModelObject().setKeySize(KeySize.KEYSIZE_1024);
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
		System.out.println("onDecrypt");
		try
		{
			final String decryted = getModelObject().getDecryptor()
				.decrypt(getEnDecryptPanel().getTxtEncrypted().getText());
			getEnDecryptPanel().getTxtToEncrypt().setText(decryted);
			getEnDecryptPanel().getTxtEncrypted().setText("");
		}
		catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
			| IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException
			| InvalidAlgorithmParameterException | DecoderException | IOException e)
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
		System.out.println("onEncrypt");
		try
		{
			getEnDecryptPanel().getTxtEncrypted().setText(getModelObject().getEncryptor()
				.encrypt(getEnDecryptPanel().getTxtToEncrypt().getText()));
			getEnDecryptPanel().getTxtToEncrypt().setText("");
		}
		catch (InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException
			| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException
			| IOException e)
		{
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}

	}

	/**
	 * Callback method that can be overwritten to provide specific action for the on generate.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onGenerate(final ActionEvent actionEvent)
	{
		final KeySize selected = (KeySize)getCryptographyPanel().getCmbKeySize().getSelectedItem();
		getCryptographyPanel().getTxtPrivateKey().setText("Generating private key...");
		getCryptographyPanel().getTxtPublicKey().setText("Generating public key...");
		try
		{
			final KeyPair keyPair = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA,
				selected.getKeySize());

			getModelObject().setPrivateKey(keyPair.getPrivate());
			getModelObject().setPublicKey(keyPair.getPublic());

			getModelObject()
				.setDecryptor(new PrivateKeyHexDecryptor(getModelObject().getPrivateKey()));
			getModelObject()
				.setEncryptor(new PublicKeyHexEncryptor(getModelObject().getPublicKey()));

			final String privateKeyFormat = PrivateKeyExtensions
				.toPemFormat(getModelObject().getPrivateKey());

			final String publicKeyFormat = PublicKeyExtensions
				.toPemFormat(getModelObject().getPublicKey());

			getCryptographyPanel().getTxtPrivateKey().setText("");
			getCryptographyPanel().getTxtPublicKey().setText("");
			getCryptographyPanel().getTxtPrivateKey().setText(privateKeyFormat);
			getCryptographyPanel().getTxtPublicKey().setText(publicKeyFormat);
			getCryptographyPanel().getBtnSaveCertificate().setEnabled(true);
		}
		catch (final NoSuchAlgorithmException | NoSuchProviderException | IOException e)
		{
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}

	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		cryptographyPanel = new CryptographyPanel(getModel())
		{

			private static final long serialVersionUID = 1L;

			@Override
			protected void onChangeKeySize(final ActionEvent actionEvent)
			{
				GenerateKeysPanel.this.onChangeKeySize(actionEvent);
			}

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
				// TODO Auto-generated method stub
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


	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeMigLayout();
	}

	/**
	 * Initialize layout.
	 */
	protected void onInitializeMigLayout()
	{
		setLayout(new MigLayout());
		add(cryptographyPanel, "wrap");
		add(enDecryptPanel);
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
			try
			{
				PrivateKeyWriter.writeInPemFormat(getModelObject().getPrivateKey(),
					fileChooser.getSelectedFile());
			}
			catch (final Exception e)
			{
				log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
		if (state == JFileChooser.CANCEL_OPTION)
		{
		}
	}


	protected void onSavePrivateKeyWithPassword(final ActionEvent actionEvent)
	{
		// TODO here comes the dialog for enter the pw...
		final Object[] options = { "Set password", "Cancel" };
		final PasswordPanel panel = new PasswordPanel();

		final int result = JOptionPane.showOptionDialog(null, panel, "Enter a password",
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
		final String password = String.copyValueOf(panel.getTxtPassword().getPassword());
		final String repeatPassword = String
			.copyValueOf(panel.getTxtRepeatPassword().getPassword());
		if (result == 0 && 0 < password.length())
		{
			if (password.equals(repeatPassword))
			{

				final JFileChooser fileChooser = new JFileChooser();
				final int state = fileChooser.showSaveDialog(this);
				if (state == JFileChooser.APPROVE_OPTION)
				{
					// TODO save private key with password...
					PrivateKey privateKey = null;
					try
					{
						privateKey = getModelObject().getPrivateKey();
						EncryptedPrivateKeyWriter.encryptPrivateKeyWithPassword(privateKey,
							fileChooser.getSelectedFile(), password);
					}
					catch (final Exception e)
					{
						log.log(Level.SEVERE, e.getLocalizedMessage(), e);
					}
				}
				if (state == JFileChooser.CANCEL_OPTION)
				{
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
		if (state == JFileChooser.CANCEL_OPTION)
		{
		}
	}
}
