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
package io.github.astrapi69.mystic.crypt.panel.privatekey;

import java.awt.event.ActionEvent;
import java.util.logging.Level;

import lombok.Getter;
import lombok.extern.java.Log;
import net.miginfocom.swing.MigLayout;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.panel.keygen.EnDecryptPanel;
import io.github.astrapi69.swing.base.BasePanel;

@Getter
@Log
public class PrivateKeyPanel extends BasePanel<PrivateKeyModelBean>
{

	private static final long serialVersionUID = 1L;

	private EnDecryptPanel enDecryptPanel;

	private PrivateKeyViewPanel privateKeyViewPanel;

	public PrivateKeyPanel()
	{
		this(BaseModel.<PrivateKeyModelBean> of(PrivateKeyModelBean.builder().build()));
	}

	public PrivateKeyPanel(final IModel<PrivateKeyModelBean> model)
	{
		super(model);
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
		catch (Exception e)
		{
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
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
			getEnDecryptPanel().getTxtEncrypted().setText(getModelObject().getEncryptor()
				.encrypt(getEnDecryptPanel().getTxtToEncrypt().getText()));
			getEnDecryptPanel().getTxtToEncrypt().setText("");
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
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
		add(privateKeyViewPanel, "wrap");
		add(enDecryptPanel);
	}

}
