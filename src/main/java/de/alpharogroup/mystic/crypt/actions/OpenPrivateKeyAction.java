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
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import javax.swing.JInternalFrame;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.alpharogroup.crypto.key.KeySize;
import de.alpharogroup.crypto.key.PrivateKeyExtensions;
import de.alpharogroup.crypto.key.PrivateKeyHexDecryptor;
import de.alpharogroup.crypto.key.PublicKeyExtensions;
import de.alpharogroup.crypto.key.PublicKeyHexEncryptor;
import de.alpharogroup.crypto.key.reader.PrivateKeyReader;
import de.alpharogroup.crypto.provider.SecurityProvider;
import de.alpharogroup.mystic.crypt.MainFrame;
import de.alpharogroup.mystic.crypt.panels.privatekey.PrivateKeyPanel;
import de.alpharogroup.mystic.crypt.panels.privatekey.PrivateKeyViewBean;
import de.alpharogroup.swing.actions.OpenFileAction;
import de.alpharogroup.swing.components.factories.JComponentFactory;
import de.alpharogroup.swing.utils.JInternalFrameExtensions;

/**
 * The class {@link OpenPrivateKeyAction}.
 */
public class OpenPrivateKeyAction extends OpenFileAction
{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new {@link OpenPrivateKeyAction} object.
	 *
	 * @param name
	 *            the name
	 */
	public OpenPrivateKeyAction(final String name, Component parent)
	{
		super(name, parent);
	}

	@Override
	protected void onApproveOption(File file, ActionEvent actionEvent)
	{
		// create internal frame
		final JInternalFrame internalFrame = JComponentFactory.newInternalFrame("Private key view",
			true, true, true, true);
		PrivateKeyPanel component = new PrivateKeyPanel();
		PrivateKeyViewBean model = component.getModel();
		model.setPrivateKeyFile(file);
		PrivateKey privateKey = getPrivateKey(file);
		model.setPrivateKey(privateKey);
		try
		{
			model.setPublicKey(PrivateKeyExtensions.generatePublicKey(privateKey));
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		model.setKeySize(getKeySize(privateKey));
		model.setKeyLength(PrivateKeyExtensions.getKeyLength(privateKey));
		component.getPrivateKeyViewPanel().getLblKeySizeDisplay().setText(""+model.getKeyLength());

		model.setDecryptor(new PrivateKeyHexDecryptor(model.getPrivateKey()));
		model.setEncryptor(new PublicKeyHexEncryptor(model.getPublicKey()));

		String privateKeyFormat = "";
		try
		{
			privateKeyFormat = PrivateKeyExtensions.toPemFormat(model.getPrivateKey());
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		final String publicKeyFormat = PublicKeyExtensions.toPemFormat(model.getPublicKey());

		component.getPrivateKeyViewPanel().getTxtPrivateKey().setText("");
		component.getPrivateKeyViewPanel().getTxtPublicKey().setText("");
		component.getPrivateKeyViewPanel().getTxtPrivateKey().setText(privateKeyFormat);
		component.getPrivateKeyViewPanel().getTxtPublicKey().setText(publicKeyFormat);

		JInternalFrameExtensions.addComponentToFrame(internalFrame, component);
		JInternalFrameExtensions.addJInternalFrame(MainFrame.getInstance().getDesktopPane(),
			internalFrame);
	}

	private PrivateKey getPrivateKey(File file)
	{
		PrivateKey privateKey = null;
		try
		{
			privateKey = PrivateKeyReader.readPrivateKey(file);

		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException
			| IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if( privateKey == null) {
			try
			{
				Security.addProvider(new BouncyCastleProvider());
				privateKey = PrivateKeyReader.readPemPrivateKey(file, SecurityProvider.BC);
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return privateKey;
	}

	@Override
	protected void onCancel(ActionEvent actionEvent)
	{
	}


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
		int length = PrivateKeyExtensions.getKeyLength(privateKey);
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

}