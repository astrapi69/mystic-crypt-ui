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
package de.alpharogroup.mystic.crypt.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;

import javax.crypto.NoSuchPaddingException;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.alpharogroup.crypto.key.KeySize;
import de.alpharogroup.crypto.key.PrivateKeyExtensions;
import de.alpharogroup.crypto.key.PrivateKeyHexDecryptor;
import de.alpharogroup.crypto.key.PublicKeyExtensions;
import de.alpharogroup.crypto.key.PublicKeyHexEncryptor;
import de.alpharogroup.crypto.key.reader.EncryptedPrivateKeyReader;
import de.alpharogroup.crypto.key.reader.PrivateKeyReader;
import de.alpharogroup.mystic.crypt.SpringBootSwingApplication;
import de.alpharogroup.mystic.crypt.panels.privatekey.PrivateKeyModelBean;
import de.alpharogroup.mystic.crypt.panels.privatekey.PrivateKeyPanel;
import de.alpharogroup.swing.actions.OpenFileAction;
import de.alpharogroup.swing.components.factories.JComponentFactory;
import de.alpharogroup.swing.utils.JInternalFrameExtensions;
import lombok.NonNull;
import lombok.extern.java.Log;

/**
 * The class {@link OpenPrivateKeyAction}.
 */
@Log
public class OpenPrivateKeyAction extends OpenFileAction
{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Gets the {@link KeySize} of the given {@link PrivateKey} or null if not found.
	 *
	 * @param privateKey
	 *            the private key
	 * @return the {@link KeySize} of the given {@link PrivateKey} or null if not found.
	 * @deprecated use same name method from PrivateKeyExtensions.
	 */
	@Deprecated
	public static KeySize getKeySize(final PrivateKey privateKey)
	{
		final int length = PrivateKeyExtensions.getKeyLength(privateKey);
		if (length == 1024)
		{
			return KeySize.KEYSIZE_1024;
		}
		if (length == 2048)
		{
			return KeySize.KEYSIZE_2048;
		}
		if (length == 4096)
		{
			return KeySize.KEYSIZE_4096;
		}
		return null;
	}

	/**
	 * Instantiates a new {@link OpenPrivateKeyAction} object.
	 *
	 * @param name
	 *            the name
	 * @param parent
	 *            the parent
	 */
	public OpenPrivateKeyAction(final String name, final @NonNull Component parent)
	{
		super(name, parent);
	}

	private PrivateKey getPrivateKey(final File file)
	{
		PrivateKey privateKey = null;
		try
		{
			if (!PrivateKeyReader.isPrivateKeyPasswordProtected(file))
			{
				privateKey = PrivateKeyReader.readPrivateKey(file);
			}
			else
			{
				String password = null;
				JPasswordField pf = new JPasswordField();
				int okCxl = JOptionPane.showConfirmDialog(null, pf, "Enter Password",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

				if (okCxl == JOptionPane.OK_OPTION)
				{
					password = new String(pf.getPassword());
				}
				if (password != null)
				{
					privateKey = EncryptedPrivateKeyReader.readPasswordProtectedPrivateKey(file,
						password);
				}
				password = null;
			}

		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException
			| NoSuchPaddingException | InvalidAlgorithmParameterException | NoSuchProviderException
			| IOException e)
		{
			String title = e.getLocalizedMessage();
			String htmlMessage = "<html><body width='650'>" + "<h2>" + title + "</h2>" + "<p>"
				+ e.getMessage();
			JOptionPane.showMessageDialog(this.getParent(), htmlMessage, title,
				JOptionPane.ERROR_MESSAGE);
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		if (privateKey == null)
		{
			try
			{
				Security.addProvider(new BouncyCastleProvider());
				privateKey = PrivateKeyReader.readPemPrivateKey(file);
			}
			catch (final Exception e)
			{
				String title = e.getLocalizedMessage();
				String htmlMessage = "<html><body width='650'>" + "<h2>" + title + "</h2>" + "<p>"
					+ e.getMessage();
				JOptionPane.showMessageDialog(this.getParent(), htmlMessage, title,
					JOptionPane.ERROR_MESSAGE);
				log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
		return privateKey;
	}

	@Override
	protected void onApproveOption(final File file, final ActionEvent actionEvent)
	{
		// create internal frame
		final JInternalFrame internalFrame = JComponentFactory.newInternalFrame("Private key view",
			true, true, true, true);
		final PrivateKeyPanel component = new PrivateKeyPanel();
		final PrivateKeyModelBean model = component.getModelObject();
		model.setPrivateKeyFile(file);
		final PrivateKey privateKey = getPrivateKey(file);
		model.setPrivateKey(privateKey);
		try
		{
			model.setPublicKey(PrivateKeyExtensions.generatePublicKey(privateKey));
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException e)
		{
			String title = e.getLocalizedMessage();
			String htmlMessage = "<html><body width='650'>" + "<h2>" + title + "</h2>" + "<p>"
				+ e.getMessage();
			JOptionPane.showMessageDialog(this.getParent(), htmlMessage, title,
				JOptionPane.ERROR_MESSAGE);
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}

		model.setKeySize(getKeySize(privateKey));
		model.setKeyLength(PrivateKeyExtensions.getKeyLength(privateKey));
		component.getPrivateKeyViewPanel().getLblKeySizeDisplay()
			.setText("" + model.getKeyLength());

		model.setDecryptor(new PrivateKeyHexDecryptor(model.getPrivateKey()));
		model.setEncryptor(new PublicKeyHexEncryptor(model.getPublicKey()));

		String privateKeyFormat = "";
		try
		{
			privateKeyFormat = PrivateKeyExtensions.toPemFormat(model.getPrivateKey());
		}
		catch (final IOException e)
		{
			String title = e.getLocalizedMessage();
			String htmlMessage = "<html><body width='650'>" + "<h2>" + title + "</h2>" + "<p>"
				+ e.getMessage();
			JOptionPane.showMessageDialog(this.getParent(), htmlMessage, title,
				JOptionPane.ERROR_MESSAGE);
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}

		final String publicKeyFormat = PublicKeyExtensions.toPemFormat(model.getPublicKey());

		component.getPrivateKeyViewPanel().getTxtPrivateKey().setText("");
		component.getPrivateKeyViewPanel().getTxtPublicKey().setText("");
		component.getPrivateKeyViewPanel().getTxtPrivateKey().setText(privateKeyFormat);
		component.getPrivateKeyViewPanel().getTxtPublicKey().setText(publicKeyFormat);

		JInternalFrameExtensions.addComponentToFrame(internalFrame, component);
		JInternalFrameExtensions.addJInternalFrame(
			SpringBootSwingApplication.getInstance().getDesktopPane(), internalFrame);
	}


	@Override
	protected void onCancel(final ActionEvent actionEvent)
	{
	}

}