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
package io.github.astrapi69.mystic.crypt.panel.keygen;

import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import io.github.astrapi69.collection.pair.Pair;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.model.component.JMTextArea;
import lombok.Getter;

/**
 * The class {@link EnDecryptPanel} holds components for encrypt and decrypt text.
 * <p>
 * Both text areas are bound to the model of this panel: the left side of the pair carries the text
 * to encrypt, the right side the encrypted text. A callback that encrypts or decrypts therefore
 * reads what the user entered from {@link #getModelObject()} and not out of the widgets.
 */
@Getter
public class EnDecryptPanel extends BasePanel<Pair<String, String>>
{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The btn decrypt. */
	private JButton btnDecrypt;

	/** The btn encrypt. */
	private JButton btnEncrypt;

	/** The lbl encrypted. */
	private JLabel lblEncrypted;

	/** The lbl to encrypt. */
	private JLabel lblToEncrypt;

	/** The scp encrypted. */
	private JScrollPane scpEncrypted;

	/** The scp to encrypt. */
	private JScrollPane scpToEncrypt;

	/** The txt encrypted. */
	private JMTextArea txtEncrypted;

	/** The txt to encrypt. */
	private JMTextArea txtToEncrypt;

	/**
	 * Instantiates a new {@link EnDecryptPanel}.
	 */
	public EnDecryptPanel()
	{
		this(BaseModel.<Pair<String, String>> of(
			Pair.<String, String> builder().leftContent("").rightContent("").build()));
	}

	/**
	 * Instantiates a new {@link EnDecryptPanel}.
	 *
	 * @param model
	 *            the model
	 */
	public EnDecryptPanel(final IModel<Pair<String, String>> model)
	{
		super(model);
	}

	/**
	 * Initialize components.
	 */
	protected void initializeComponents()
	{
		lblToEncrypt = new JLabel();
		scpToEncrypt = new JScrollPane();
		txtToEncrypt = new JMTextArea();
		btnEncrypt = new JButton();
		btnDecrypt = new JButton();
		scpEncrypted = new JScrollPane();
		txtEncrypted = new JMTextArea();
		lblEncrypted = new JLabel();

		lblToEncrypt.setText("Text to encrypt");

		// stable component names for UI tests (AssertJ-Swing lookups by name); this panel is
		// shared by the obfuscation and keygen plugins, which drive it through these names
		txtToEncrypt.setName("txtToEncrypt");
		txtEncrypted.setName("txtEncrypted");
		btnEncrypt.setName("btnEncrypt");
		btnDecrypt.setName("btnDecrypt");

		bindToModel();

		txtToEncrypt.setColumns(20);
		txtToEncrypt.setRows(5);
		scpToEncrypt.setViewportView(txtToEncrypt);

		btnEncrypt.setText("Encrypt >");
		btnEncrypt.addActionListener(actionEvent -> onEncrypt(actionEvent));

		btnDecrypt.setText("< Decrypt");
		btnDecrypt.addActionListener(actionEvent -> onDecrypt(actionEvent));

		followTheContentOf(txtToEncrypt);
		followTheContentOf(txtEncrypted);
		updateButtonState();

		txtEncrypted.setColumns(20);
		txtEncrypted.setRows(5);
		scpEncrypted.setViewportView(txtEncrypted);

		lblEncrypted.setText("Encrypted text");

	}

	/**
	 * Whether this panel is in a state where encrypting and decrypting can work at all. False puts
	 * both buttons out of reach, whatever the text areas contain.
	 */
	private boolean enDecryptAvailable = true;

	/** What to tell the user while the buttons are out of reach */
	private String unavailableReason;

	/**
	 * Says whether encrypting and decrypting are possible at all right now, and why not when they
	 * are not.
	 * <p>
	 * A button that is offered and then answers with a message box has already wasted the click.
	 * The owner of this panel knows what it can serve - only an RSA key pair carries the hex
	 * encrypt/decrypt demo, for instance - and says so here, so the buttons are simply out of reach
	 * with the reason on them.
	 *
	 * @param available
	 *            true if both buttons may be used once there is text for them
	 * @param reason
	 *            what to show on a button that is out of reach, ignored when available
	 */
	public void setEnDecryptAvailable(final boolean available, final String reason)
	{
		this.enDecryptAvailable = available;
		this.unavailableReason = reason;
		updateButtonState();
	}

	/**
	 * Keeps the buttons in step with what the given text area holds
	 *
	 * @param textArea
	 *            the text area whose content decides whether its button has anything to work on
	 */
	private void followTheContentOf(final JMTextArea textArea)
	{
		textArea.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void changedUpdate(final DocumentEvent documentEvent)
			{
				updateButtonState();
			}

			@Override
			public void insertUpdate(final DocumentEvent documentEvent)
			{
				updateButtonState();
			}

			@Override
			public void removeUpdate(final DocumentEvent documentEvent)
			{
				updateButtonState();
			}
		});
	}

	/**
	 * A button is usable when this panel can do the job at all and there is text for it to work on;
	 * otherwise it is out of reach and carries the reason
	 */
	private void updateButtonState()
	{
		btnEncrypt.setEnabled(enDecryptAvailable && 0 < txtToEncrypt.getDocument().getLength());
		btnDecrypt.setEnabled(enDecryptAvailable && 0 < txtEncrypted.getDocument().getLength());
		btnEncrypt.setToolTipText(enDecryptAvailable ? null : unavailableReason);
		btnDecrypt.setToolTipText(enDecryptAvailable ? null : unavailableReason);
	}

	/**
	 * Binds both text areas to the model of this panel, so that every edit lands in the model and
	 * the model is what an encrypt or decrypt callback reads: the left side of the pair carries the
	 * text to encrypt, the right side the encrypted text
	 */
	private void bindToModel()
	{
		txtToEncrypt.setPropertyModel(
			LambdaModel.of(getModel(), Pair::getLeftContent, Pair::setLeftContent));
		txtEncrypted.setPropertyModel(
			LambdaModel.of(getModel(), Pair::getRightContent, Pair::setRightContent));
	}

	/**
	 * Callback method that can be overwritten to provide specific action for the on decrypt.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onDecrypt(final ActionEvent actionEvent)
	{
	}

	/**
	 * Callback method that can be overwritten to provide specific action for the on encrypt.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onEncrypt(final ActionEvent actionEvent)
	{

	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
		initializeComponents();
	}

	/**
	 * Initialize layout.
	 */
	protected void onInitializeGroupLayout()
	{
		final GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);

		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(lblToEncrypt, GroupLayout.PREFERRED_SIZE, 381,
						GroupLayout.PREFERRED_SIZE)
					.addComponent(scpToEncrypt, GroupLayout.PREFERRED_SIZE, 500,
						GroupLayout.PREFERRED_SIZE))
				.addGap(39, 39, 39)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
					.addComponent(btnDecrypt, GroupLayout.DEFAULT_SIZE, 167, Short.MAX_VALUE)
					.addComponent(btnEncrypt, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
						Short.MAX_VALUE))
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 36, Short.MAX_VALUE)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(scpEncrypted, GroupLayout.PREFERRED_SIZE, 500,
						GroupLayout.PREFERRED_SIZE)
					.addComponent(lblEncrypted, GroupLayout.PREFERRED_SIZE, 381,
						GroupLayout.PREFERRED_SIZE))
				.addGap(26, 26, 26)));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addGap(31, 31, 31)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(lblToEncrypt, GroupLayout.PREFERRED_SIZE, 31,
						GroupLayout.PREFERRED_SIZE)
					.addComponent(lblEncrypted, GroupLayout.PREFERRED_SIZE, 31,
						GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
					.addGroup(layout.createSequentialGroup().addComponent(btnEncrypt)
						.addGap(90, 90, 90).addComponent(btnDecrypt))
					.addComponent(scpEncrypted).addComponent(scpToEncrypt))
				.addContainerGap(40, Short.MAX_VALUE)));
	}


	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
	}

}
