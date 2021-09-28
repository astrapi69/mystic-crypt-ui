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
package io.github.astrapi69.mystic.crypt.actions;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;

import javax.crypto.NoSuchPaddingException;
import javax.swing.*;

import lombok.NonNull;
import lombok.extern.java.Log;
import net.miginfocom.swing.MigLayout;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.github.astrapi69.crypto.key.PrivateKeyExtensions;
import io.github.astrapi69.crypto.key.PrivateKeyHexDecryptor;
import io.github.astrapi69.crypto.key.PublicKeyExtensions;
import io.github.astrapi69.crypto.key.PublicKeyHexEncryptor;
import io.github.astrapi69.crypto.key.reader.EncryptedPrivateKeyReader;
import io.github.astrapi69.crypto.key.reader.PrivateKeyReader;
import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.panels.privatekey.PrivateKeyModelBean;
import io.github.astrapi69.mystic.crypt.panels.privatekey.PrivateKeyPanel;
import io.github.astrapi69.swing.actions.OpenFileAction;
import io.github.astrapi69.swing.components.factories.JComponentFactory;
import io.github.astrapi69.swing.dialog.factories.JDialogFactory;
import io.github.astrapi69.swing.listener.RequestFocusListener;
import io.github.astrapi69.swing.utils.JInternalFrameExtensions;

/**
 * The class {@link OpenPrivateKeyAction}.
 */
@Log
public class OpenPrivateKeyAction extends OpenFileAction
{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

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
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(MysticCryptApplicationFrame.getInstance().getBouncyCastleProvider());
		}
		try
		{
			if (!PrivateKeyReader.isPrivateKeyPasswordProtected(file))
			{
				privateKey = EncryptedPrivateKeyReader.readPasswordProtectedPrivateKey(file, null);
			}
			else
			{
				String password = null;
				JPasswordField pf = new JPasswordField("", 10);
				pf.setFocusable(true);
				pf.setRequestFocusEnabled(true);
				JPanel panel = new JPanel(new MigLayout(""));
				panel.add(new JLabel("Password:"));
				panel.add(pf, "growy");

				JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
					JOptionPane.OK_CANCEL_OPTION);

				JDialog dialog = JDialogFactory.newJDialog(MysticCryptApplicationFrame.getInstance(),
					optionPane, "Enter Password");
				dialog.addWindowFocusListener(new RequestFocusListener(pf));
				dialog.pack();
				dialog.setLocationRelativeTo(null);
				dialog.setVisible(true);

				if (optionPane.getValue().equals(JOptionPane.OK_OPTION))
				{
					password = new String(pf.getPassword());
				}
				else
				{
					return null;
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
			| NoSuchPaddingException | InvalidAlgorithmParameterException | IOException e)
		{
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		if (privateKey == null)
		{
			try
			{
				privateKey = PrivateKeyReader.readPemPrivateKey(file);
			}
			catch (final Exception e)
			{
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
		if (privateKey == null)
		{
			return;
		}
		model.setPrivateKey(privateKey);
		try
		{
			model.setPublicKey(PrivateKeyExtensions.generatePublicKey(privateKey));
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException e)
		{
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			return;
		}

		model.setKeySize(PrivateKeyExtensions.getKeySize(privateKey));
		model.setKeyLength(PrivateKeyExtensions.getKeyLength(privateKey));
		component.getPrivateKeyViewPanel().getLblKeySizeDisplay()
			.setText("" + model.getKeyLength());

		model.setDecryptor(new PrivateKeyHexDecryptor(model.getPrivateKey()));
		model.setEncryptor(new PublicKeyHexEncryptor(model.getPublicKey()));

		String privateKeyFormat = "";
		try
		{
			if (PrivateKeyReader.isPemFormat(file))
			{
				privateKeyFormat = ReadFileExtensions.readFromFile(file);
			}
			else
			{
				privateKeyFormat = PrivateKeyExtensions.toPemFormat(model.getPrivateKey());
			}
		}
		catch (final IOException e)
		{
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			return;
		}

		final String publicKeyFormat = PublicKeyExtensions.toPemFormat(model.getPublicKey());

		component.getPrivateKeyViewPanel().getTxtPrivateKey().setText("");
		component.getPrivateKeyViewPanel().getTxtPublicKey().setText("");
		component.getPrivateKeyViewPanel().getTxtPrivateKey().setText(privateKeyFormat);
		component.getPrivateKeyViewPanel().getTxtPublicKey().setText(publicKeyFormat);

		JInternalFrameExtensions.addComponentToFrame(internalFrame, component);
		JInternalFrameExtensions.addJInternalFrame(
			MysticCryptApplicationFrame.getInstance().getMainComponent(), internalFrame);
	}


	@Override
	protected void onCancel(final ActionEvent actionEvent)
	{
	}

}
