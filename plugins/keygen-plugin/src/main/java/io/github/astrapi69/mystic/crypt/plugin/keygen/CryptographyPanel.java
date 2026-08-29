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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.logging.Level;

import javax.swing.*;

import io.github.astrapi69.crypt.api.key.KeySize;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.ui.screen.ScreenPlacement;
import io.github.astrapi69.mystic.crypt.panel.certificate.NewCertificateInfoPanel;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.dialog.factory.JDialogFactory;
import io.github.astrapi69.swing.listener.RequestFocusListener;
import io.github.astrapi69.swing.model.combobox.EnumComboBoxModel;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMTextArea;
import lombok.Getter;
import lombok.extern.java.Log;

/**
 * The class {@link CryptographyPanel} can generate private and public keys and save them to files.
 */
@Getter
@Log
public class CryptographyPanel extends BasePanel<GenerateKeysModelBean>
{

	/**
	 * The Constant serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The smallest width in pixels a key area keeps when the window is narrower than this panel
	 * wants. It belongs on the scroll pane around the text area, because a text area on its own
	 * reports a minimum width of nearly zero and would collapse instead of shrinking.
	 */
	private static final int MINIMUM_KEY_AREA_WIDTH = 180;

	/**
	 * The smallest height in pixels a key area keeps when the window is shorter than this panel
	 * wants.
	 */
	private static final int MINIMUM_KEY_AREA_HEIGHT = 120;

	/**
	 * The width of a key area in characters. A PEM line is 64 characters wide, so a key area that
	 * asks for this many columns shows a key line unwrapped at the size this panel prefers.
	 */
	private static final int KEY_AREA_COLUMNS = 64;

	/** The number of key lines a key area shows before it scrolls */
	private static final int KEY_AREA_ROWS = 5;

	/**
	 * The btn clear.
	 */
	private JButton btnClear;

	/**
	 * The btn generate.
	 */
	private JButton btnGenerate;

	/**
	 * The btn save private key.
	 */
	private JButton btnSavePrivateKey;

	private JButton btnSavePrivKeyWithPw;

	private JButton btnSaveCertificate;
	/**
	 * The btn save public key.
	 */
	private JButton btnSavePublicKey;

	/**
	 * The cmb key size.
	 */
	private JMComboBox<KeySize, EnumComboBoxModel<KeySize>> cmbKeySize;

	/**
	 * The lbl key size.
	 */
	private JLabel lblKeySize;

	/**
	 * The lbl private key.
	 */
	private JLabel lblPrivateKey;

	/**
	 * The lbl public key.
	 */
	private JLabel lblPublicKey;

	/**
	 * The scp private key.
	 */
	private JScrollPane scpPrivateKey;

	/**
	 * The scp public key.
	 */
	private JScrollPane scpPublicKey;

	/**
	 * The txt private key.
	 */
	private JMTextArea txtPrivateKey;

	/**
	 * The txt public key.
	 */
	private JMTextArea txtPublicKey;

	/**
	 * Instantiates a new {@link CryptographyPanel}.
	 */
	public CryptographyPanel()
	{
		this(BaseModel.of(GenerateKeysModelBean.builder().build()));
	}

	public CryptographyPanel(final IModel<GenerateKeysModelBean> model)
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
	protected void onChangeKeySize(final ActionEvent actionEvent)
	{
	}

	/**
	 * Callback method that can be overwritten to provide specific action for the on clear.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onClear(final ActionEvent actionEvent)
	{
	}

	/**
	 * Callback method that can be overwritten to provide specific action for the on generate.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onGenerate(final ActionEvent actionEvent)
	{
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		scpPrivateKey = new JScrollPane();
		txtPrivateKey = new JMTextArea();
		cmbKeySize = new JMComboBox<>(new EnumComboBoxModel<>(KeySize.class));
		lblPrivateKey = new JLabel();
		lblKeySize = new JLabel();
		scpPublicKey = new JScrollPane();
		txtPublicKey = new JMTextArea();
		lblPublicKey = new JLabel();
		btnGenerate = new JButton();
		btnClear = new JButton();
		btnSavePrivateKey = new JButton();
		btnSavePublicKey = new JButton();
		btnSavePrivKeyWithPw = new JButton();
		btnSaveCertificate = new JButton();

		// the tool starts with the key size the user configured in the settings dialog; the model
		// carries it, and the bound key size box takes it from there
		getModelObject().setKeySize(KeygenSettingsContribution.keySize());
		bindComponents();

		txtPrivateKey.setEditable(false);
		txtPublicKey.setEditable(false);
		// a key area wraps instead of scrolling sideways, so that a window narrower than a PEM
		// line still shows the whole key
		txtPrivateKey.setLineWrap(true);
		txtPrivateKey.setWrapStyleWord(false);
		txtPublicKey.setLineWrap(true);
		txtPublicKey.setWrapStyleWord(false);

		// stable component names for UI tests (AssertJ-Swing lookups by name)
		btnGenerate.setName("btnGenerate");
		cmbKeySize.setName("cmbKeySize");
		txtPrivateKey.setName("txtPrivateKey");
		txtPublicKey.setName("txtPublicKey");

		txtPrivateKey.setFont(new Font("monospaced", Font.PLAIN, 12));
		txtPublicKey.setFont(new Font("monospaced", Font.PLAIN, 12));

		cmbKeySize.addActionListener(actionEvent -> onChangeKeySize(actionEvent));
		btnGenerate.addActionListener(actionEvent -> onGenerate(actionEvent));
		btnClear.addActionListener(actionEvent -> onClear(actionEvent));
		btnSavePrivateKey.addActionListener(actionEvent -> onSavePrivateKey(actionEvent));
		btnSavePrivKeyWithPw
			.addActionListener(actionEvent -> onSavePrivateKeyWithPassword(actionEvent));
		btnSavePublicKey.addActionListener(actionEvent -> onSavePublicKey(actionEvent));
		btnSaveCertificate.addActionListener(actionEvent -> onSaveCertificate(actionEvent));

		txtPrivateKey.setColumns(KEY_AREA_COLUMNS);
		txtPrivateKey.setRows(KEY_AREA_ROWS);
		scpPrivateKey.setViewportView(txtPrivateKey);
		applyMinimumKeyAreaSize(scpPrivateKey);
		txtPrivateKey.getAccessibleContext().setAccessibleDescription("");

		btnGenerate.setText("Generate keys");

		lblPrivateKey.setText("Private key");

		lblKeySize.setText("Keysize");

		txtPublicKey.setColumns(KEY_AREA_COLUMNS);
		txtPublicKey.setRows(KEY_AREA_ROWS);
		scpPublicKey.setViewportView(txtPublicKey);
		applyMinimumKeyAreaSize(scpPublicKey);

		lblPublicKey.setText("Public key");

		btnClear.setText("Clear keys");

		btnSavePrivateKey.setText("Save private key");
		btnSavePrivKeyWithPw.setText("Save private key with password");

		btnSavePublicKey.setText("Save public key");
		btnSaveCertificate.setText("Save certificate...");
		//
		btnSaveCertificate.setEnabled(false);
	}

	/**
	 * Binds the key size box and the two key areas to the model of this panel, so that the chosen
	 * key size and the generated key pair are readable from the model at any moment instead of out
	 * of the components
	 */
	private void bindComponents()
	{
		final GenerateKeysModelBean modelObject = getModelObject();
		cmbKeySize.setPropertyModel(
			LambdaModel.of(modelObject::getKeySize, modelObject::setKeySize));
		txtPrivateKey.setPropertyModel(
			LambdaModel.of(modelObject::getPrivateKeyPem, modelObject::setPrivateKeyPem));
		txtPublicKey.setPropertyModel(
			LambdaModel.of(modelObject::getPublicKeyPem, modelObject::setPublicKeyPem));
	}

	/**
	 * Gives the given scroll pane around a key area an honest minimum size, so that the key stays
	 * readable when the layout has no room for the preferred width.
	 *
	 * @param keyAreaScrollPane
	 *            the scroll pane around the private or the public key area
	 */
	private void applyMinimumKeyAreaSize(final JScrollPane keyAreaScrollPane)
	{
		keyAreaScrollPane
			.setMinimumSize(new Dimension(MINIMUM_KEY_AREA_WIDTH, MINIMUM_KEY_AREA_HEIGHT));
	}

	/**
	 * Asks for the issuer and the subject of the certificate, then writes the certificate of the
	 * generated key pair where the user points.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onSaveCertificate(ActionEvent actionEvent)
	{
		GenerateKeysModelBean modelObject = getModelObject();
		NewCertificateInfoPanel panel = new NewCertificateInfoPanel(BaseModel.of(KeygenSupport
			.newCertificateInfo(modelObject.getPublicKey(), modelObject.getPrivateKey())));

		JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION);
		JDialog dialog = JDialogFactory.newJDialog(MysticCryptApplicationFrame.getInstance(),
			optionPane, "Create certificate");
		dialog.addWindowFocusListener(new RequestFocusListener(panel.getTxtIssuer()));
		dialog.pack();
		ScreenPlacement.centerOnScreenOf(dialog, this);
		dialog.setVisible(true);

		if (!Integer.valueOf(JOptionPane.OK_OPTION).equals(optionPane.getValue()))
		{
			return;
		}
		final JFileChooser fileChooser = new JFileChooser();
		if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}
		writeCertificateTo(
			KeygenSupport.withEnding(fileChooser.getSelectedFile(), KeygenSupport.PEM_ENDING),
			panel.getModelObject());
	}

	/**
	 * Writes the described certificate to the given file and tells the user when that fails.
	 * <p>
	 * A failure here is the user's business: the reason - a name that is not a distinguished name,
	 * a key that cannot sign with the chosen algorithm - is what makes the next attempt work, and
	 * a button that answers with nothing at all is worse than one that answers with a problem.
	 *
	 * @param file
	 *            the file to write the certificate to
	 * @param certificateInfo
	 *            what the certificate is to contain
	 */
	protected void writeCertificateTo(final File file,
		final CertificateInfoModel certificateInfo)
	{
		try
		{
			KeygenSupport.writeCertificate(certificateInfo, file);
		}
		catch (final Exception couldNotWriteTheCertificate)
		{
			log.log(Level.SEVERE, couldNotWriteTheCertificate.getLocalizedMessage(),
				couldNotWriteTheCertificate);
			JOptionPane.showMessageDialog(this,
				"The certificate was not written: " + couldNotWriteTheCertificate.getMessage(),
				"Creation of certificate failed", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Lays this panel out with the layout every tool window in this application uses: the key size
	 * in a labelled row, the two key areas below each other taking the height the window has left,
	 * and every button in a row under the thing it acts on.
	 */
	protected void onInitializeMigLayout()
	{
		setLayout(ToolForm.newLayout());

		add(lblKeySize);
		add(cmbKeySize, ToolForm.FIELD);
		add(ToolForm.buttons(btnGenerate, btnClear), ToolForm.BUTTON_ROW);

		add(lblPrivateKey, "aligny top");
		add(scpPrivateKey, ToolForm.KEY_AREA);
		add(ToolForm.buttons(btnSavePrivateKey, btnSavePrivKeyWithPw), ToolForm.BUTTON_ROW);

		add(lblPublicKey, "aligny top");
		add(scpPublicKey, ToolForm.KEY_AREA);
		add(ToolForm.buttons(btnSavePublicKey, btnSaveCertificate), ToolForm.BUTTON_ROW);
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeMigLayout();
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
	}

	/**
	 * Callback method that can be overwritten to provide specific action for the on save private
	 * key with password.
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onSavePrivateKeyWithPassword(final ActionEvent actionEvent)
	{
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
	}

}
