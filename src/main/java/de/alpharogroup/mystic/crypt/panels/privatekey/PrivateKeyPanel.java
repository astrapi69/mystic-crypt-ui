package de.alpharogroup.mystic.crypt.panels.privatekey;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.codec.DecoderException;
import org.jdesktop.swingx.JXPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.alpharogroup.mystic.crypt.panels.keygen.EnDecryptPanel;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;


@Getter
public class PrivateKeyPanel extends JXPanel
{
	/** The logger. */
	protected static final Logger logger = LoggerFactory.getLogger(PrivateKeyPanel.class.getName());

	private PrivateKeyViewPanel privateKeyViewPanel;

	private EnDecryptPanel enDecryptPanel;

	private PrivateKeyViewBean model = PrivateKeyViewBean.builder().build();

	public PrivateKeyPanel()
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
		privateKeyViewPanel = new PrivateKeyViewPanel();

		enDecryptPanel = new EnDecryptPanel()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onDecrypt(final ActionEvent actionEvent)
			{
				PrivateKeyPanel.this.onDecrypt(actionEvent);
			}

			@Override
			protected void onEncrypt(final ActionEvent actionEvent)
			{
				PrivateKeyPanel.this.onEncrypt(actionEvent);
			}
		};
	}


	/**
	 * Initialize layout.
	 */
	protected void initializeLayout()
	{
		setLayout(new MigLayout());
		add(privateKeyViewPanel, "wrap");
		add(enDecryptPanel);
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
			final String decryted = model.getDecryptor()
				.decrypt(getEnDecryptPanel().getTxtEncrypted().getText());
			getEnDecryptPanel().getTxtToEncrypt().setText(decryted);
			getEnDecryptPanel().getTxtEncrypted().setText("");
		}
		catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
			| IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException
			| InvalidAlgorithmParameterException | DecoderException | IOException e)
		{
			logger.error("", e);
		}

	}


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
			getEnDecryptPanel().getTxtEncrypted().setText(
				model.getEncryptor().encrypt(getEnDecryptPanel().getTxtToEncrypt().getText()));
			getEnDecryptPanel().getTxtToEncrypt().setText("");
		}
		catch (InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException
			| NoSuchPaddingException | UnsupportedEncodingException e)
		{
			logger.error("", e);
		}
		catch (final IllegalBlockSizeException e)
		{
			logger.error("", e);
		}
		catch (final BadPaddingException e)
		{
			logger.error("", e);
		}
		catch (final IOException e)
		{
			logger.error("", e);
		}

	}

}
