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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import lombok.Getter;
import lombok.extern.java.Log;
import io.github.astrapi69.crypto.algorithm.HashAlgorithm;
import io.github.astrapi69.crypto.algorithm.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypto.algorithm.UnionWord;
import io.github.astrapi69.crypto.factories.CertFactory;
import io.github.astrapi69.crypto.key.KeyFileFormat;
import io.github.astrapi69.crypto.key.KeySize;
import io.github.astrapi69.crypto.key.writer.CertificateWriter;
import io.github.astrapi69.layout.GridBagLayoutModel;
import io.github.astrapi69.layout.InsetsModel;
import io.github.astrapi69.layout.LayoutExtensions;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.panel.certificate.NewCertificateInfoPanel;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.combobox.model.EnumComboBoxModel;
import io.github.astrapi69.swing.dialog.factory.JDialogFactory;
import io.github.astrapi69.swing.listener.RequestFocusListener;

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
	private JComboBox<KeySize> cmbKeySize;

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
	private JTextArea txtPrivateKey;

	/**
	 * The txt public key.
	 */
	private JTextArea txtPublicKey;

	/**
	 * Instantiates a new {@link CryptographyPanel}.
	 */
	public CryptographyPanel()
	{
		this(BaseModel.<GenerateKeysModelBean> of(GenerateKeysModelBean.builder().build()));
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
		txtPrivateKey = new JTextArea();
		cmbKeySize = new JComboBox<>();
		lblPrivateKey = new JLabel();
		lblKeySize = new JLabel();
		scpPublicKey = new JScrollPane();
		txtPublicKey = new JTextArea();
		lblPublicKey = new JLabel();
		btnGenerate = new JButton();
		btnClear = new JButton();
		btnSavePrivateKey = new JButton();
		btnSavePublicKey = new JButton();
		btnSavePrivKeyWithPw = new JButton();
		btnSaveCertificate = new JButton();

		txtPrivateKey.setEditable(false);
		txtPublicKey.setEditable(false);

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

		txtPrivateKey.setColumns(20);
		txtPrivateKey.setRows(5);
		scpPrivateKey.setViewportView(txtPrivateKey);
		txtPrivateKey.getAccessibleContext().setAccessibleDescription("");

		cmbKeySize.setModel(new EnumComboBoxModel<>(KeySize.class));

		btnGenerate.setText("Generate keys");

		lblPrivateKey.setText("Private key");

		lblKeySize.setText("Keysize");

		txtPublicKey.setColumns(20);
		txtPublicKey.setRows(5);
		scpPublicKey.setViewportView(txtPublicKey);

		lblPublicKey.setText("Public key");

		btnClear.setText("Clear keys");

		btnSavePrivateKey.setText("Save private key");
		btnSavePrivKeyWithPw.setText("Save private key with password");

		btnSavePublicKey.setText("Save public key");
		btnSaveCertificate.setText("Save certificate...");
		//
		btnSaveCertificate.setEnabled(false);
	}

	protected void onSaveCertificate(ActionEvent actionEvent)
	{
		NewCertificateInfoPanel panel = new NewCertificateInfoPanel();

		JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION);

		JDialog dialog = JDialogFactory.newJDialog(MysticCryptApplicationFrame.getInstance(),
			optionPane, "Create certificate");
		dialog.addWindowFocusListener(new RequestFocusListener(panel.getTxtIssuer()));
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);

		if (optionPane.getValue().equals(JOptionPane.OK_OPTION))
		{
			final JFileChooser fileChooser = new JFileChooser();
			final int state = fileChooser.showSaveDialog(this);
			if (state == JFileChooser.APPROVE_OPTION)
			{
				try
				{
					File selectedFile = fileChooser.getSelectedFile();
					String signatureAlgorithm;
					Date start;
					Date end;
					BigInteger serialNumber;
					String subject;
					String issuer;
					subject = panel.getModelObject().getSubject();
					issuer = panel.getModelObject().getIssuer();
					GenerateKeysModelBean modelObject = getModelObject();
					signatureAlgorithm = modelObject.getSignatureAlgorithm() != null
						? modelObject.getSignatureAlgorithm()
						: HashAlgorithm.SHA256.getAlgorithm() + UnionWord.With.name()
							+ KeyPairGeneratorAlgorithm.RSA.getAlgorithm();
					start = modelObject.getStart() != null
						? modelObject.getStart()
						: Date.from(LocalDate.of(2021, Month.JANUARY, 1)
							.atStartOfDay(ZoneId.systemDefault()).toInstant());

					end = modelObject.getEnd() != null
						? modelObject.getEnd()
						: Date.from(LocalDate.of(2031, Month.JANUARY, 1)
							.atStartOfDay(ZoneId.systemDefault()).toInstant());
					serialNumber = panel.getModelObject().getSerialNumber();
					PublicKey publicKey = modelObject.getPublicKey();
					PrivateKey privateKey = modelObject.getPrivateKey();
					X509Certificate x509Certificate = CertFactory.newX509Certificate(publicKey,
						privateKey, serialNumber, subject, issuer, signatureAlgorithm, start, end);
					CertificateWriter.write(x509Certificate, selectedFile, KeyFileFormat.PEM);
				}
				catch (final Exception ex)
				{
					log.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
				}
			}
		}
		if (optionPane.getValue().equals(JOptionPane.CANCEL_OPTION))
		{
		}

	}

	protected void onInitializeGridBagLayout()
	{
		final GridBagLayout gbl = new GridBagLayout();
		final GridBagConstraints gbc = new GridBagConstraints();
		this.setLayout(gbl);

		LayoutExtensions.add(GridBagLayoutModel.builder().layoutComponent(lblKeySize).parent(this)
			.gridBagLayout(gbl).gridBagConstraints(gbc).anchor(GridBagConstraints.NORTHWEST)
			.fill(GridBagConstraints.BOTH)
			.insets(InsetsModel.builder().top(2).left(2).bottom(2).right(2).build()).gridx(0)
			.gridy(1).gridwidth(1).gridheight(1).weightx(100).weighty(100).build());

		LayoutExtensions.add(GridBagLayoutModel.builder().layoutComponent(lblPrivateKey)
			.parent(this).gridBagLayout(gbl).gridBagConstraints(gbc)
			.anchor(GridBagConstraints.NORTHWEST).fill(GridBagConstraints.BOTH)
			.insets(InsetsModel.builder().top(2).left(2).bottom(2).right(2).build()).gridx(1)
			.gridy(1).gridwidth(1).gridheight(1).weightx(100).weighty(100).build());

		LayoutExtensions.add(GridBagLayoutModel.builder().layoutComponent(lblPublicKey).parent(this)
			.gridBagLayout(gbl).gridBagConstraints(gbc).anchor(GridBagConstraints.NORTHWEST)
			.fill(GridBagConstraints.BOTH)
			.insets(InsetsModel.builder().top(2).left(2).bottom(2).right(2).build()).gridx(2)
			.gridy(1).gridwidth(1).gridheight(1).weightx(100).weighty(100).build());

		LayoutExtensions.add(GridBagLayoutModel.builder().layoutComponent(cmbKeySize).parent(this)
			.gridBagLayout(gbl).gridBagConstraints(gbc).anchor(GridBagConstraints.NORTHWEST)
			.fill(GridBagConstraints.HORIZONTAL)
			.insets(InsetsModel.builder().top(2).left(2).bottom(2).right(2).build()).gridx(0)
			.gridy(2).gridwidth(1).gridheight(1).weightx(100).weighty(100).build());

		LayoutExtensions.add(GridBagLayoutModel.builder().layoutComponent(txtPrivateKey)
			.parent(this).gridBagLayout(gbl).gridBagConstraints(gbc)
			.anchor(GridBagConstraints.NORTHWEST).fill(GridBagConstraints.BOTH)
			.insets(InsetsModel.builder().top(2).left(2).bottom(2).right(2).build()).gridx(1)
			.gridy(2).gridwidth(1).gridheight(1).weightx(100).weighty(100).build());

		LayoutExtensions.add(GridBagLayoutModel.builder().layoutComponent(txtPublicKey).parent(this)
			.gridBagLayout(gbl).gridBagConstraints(gbc).anchor(GridBagConstraints.NORTHWEST)
			.fill(GridBagConstraints.BOTH)
			.insets(InsetsModel.builder().top(2).left(2).bottom(2).right(2).build()).gridx(2)
			.gridy(2).ipady(140)// make this component tall
			.gridwidth(1).gridheight(1).weightx(100).weighty(100).build());

		LayoutExtensions.add(GridBagLayoutModel.builder().layoutComponent(btnGenerate).parent(this)
			.gridBagLayout(gbl).gridBagConstraints(gbc).anchor(GridBagConstraints.NORTHWEST)
			.fill(GridBagConstraints.HORIZONTAL)
			.insets(InsetsModel.builder().top(2).left(2).bottom(2).right(2).build()).gridx(0)
			.gridy(3).gridwidth(1).gridheight(1).weightx(100).weighty(100).build());

		LayoutExtensions.add(GridBagLayoutModel.builder().layoutComponent(btnClear).parent(this)
			.gridBagLayout(gbl).gridBagConstraints(gbc).anchor(GridBagConstraints.NORTHWEST)
			.fill(GridBagConstraints.HORIZONTAL)
			.insets(InsetsModel.builder().top(2).left(2).bottom(2).right(2).build()).gridx(0)
			.gridy(4).gridwidth(1).gridheight(1).weightx(100).weighty(100).build());

		LayoutExtensions.add(GridBagLayoutModel.builder().layoutComponent(btnSavePrivateKey)
			.parent(this).gridBagLayout(gbl).gridBagConstraints(gbc)
			.anchor(GridBagConstraints.NORTHWEST).fill(GridBagConstraints.BOTH)
			.insets(InsetsModel.builder().top(2).left(2).bottom(2).right(2).build()).gridx(1)
			.gridy(5).gridwidth(1).gridheight(1).weightx(100).weighty(100).build());

		LayoutExtensions.add(GridBagLayoutModel.builder().layoutComponent(btnSavePublicKey)
			.parent(this).gridBagLayout(gbl).gridBagConstraints(gbc)
			.anchor(GridBagConstraints.NORTHWEST).fill(GridBagConstraints.BOTH)
			.insets(InsetsModel.builder().top(2).left(2).bottom(2).right(2).build()).gridx(2)
			.gridy(5).gridwidth(1).gridheight(1).weightx(100).weighty(100).build());

	}

	protected void onInitializeGroupLayout()
	{
		final javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
				.addGroup(layout.createSequentialGroup().addContainerGap().addGroup(
					layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
						.addComponent(cmbKeySize, 0, javax.swing.GroupLayout.DEFAULT_SIZE,
							Short.MAX_VALUE)
						.addComponent(btnGenerate, javax.swing.GroupLayout.DEFAULT_SIZE, 206,
							Short.MAX_VALUE)))
				.addGroup(layout.createSequentialGroup().addGap(21, 21, 21).addComponent(lblKeySize,
					javax.swing.GroupLayout.PREFERRED_SIZE, 147,
					javax.swing.GroupLayout.PREFERRED_SIZE))
				.addGroup(layout.createSequentialGroup().addContainerGap().addComponent(btnClear,
					javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
					Short.MAX_VALUE)))
				.addGap(18, 18, 18)
				.addGroup(layout
					.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
					.addGroup(layout.createSequentialGroup()
						.addComponent(btnSavePrivKeyWithPw, javax.swing.GroupLayout.PREFERRED_SIZE,
							267, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
							javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(btnSavePrivateKey, javax.swing.GroupLayout.PREFERRED_SIZE,
							174, javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(scpPrivateKey, javax.swing.GroupLayout.PREFERRED_SIZE, 480,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(lblPrivateKey, javax.swing.GroupLayout.PREFERRED_SIZE, 147,
							javax.swing.GroupLayout.PREFERRED_SIZE)))
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 56,
					Short.MAX_VALUE)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addComponent(lblPublicKey, javax.swing.GroupLayout.PREFERRED_SIZE, 147,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGroup(layout
						.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
						.addGroup(layout.createSequentialGroup()
							.addComponent(btnSaveCertificate,
								javax.swing.GroupLayout.PREFERRED_SIZE, 174,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(btnSavePublicKey, javax.swing.GroupLayout.PREFERRED_SIZE,
								174, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addComponent(scpPublicKey, javax.swing.GroupLayout.PREFERRED_SIZE, 480,
							javax.swing.GroupLayout.PREFERRED_SIZE)))
				.addContainerGap(28, Short.MAX_VALUE)));
		layout
			.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addGap(26, 26, 26)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblPrivateKey, javax.swing.GroupLayout.PREFERRED_SIZE, 29,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(lblKeySize, javax.swing.GroupLayout.PREFERRED_SIZE, 29,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(lblPublicKey, javax.swing.GroupLayout.PREFERRED_SIZE, 29,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
					.addGroup(layout
						.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
						.addGroup(layout.createSequentialGroup()
							.addComponent(cmbKeySize, javax.swing.GroupLayout.PREFERRED_SIZE, 43,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addGap(18, 18, 18)
							.addComponent(btnGenerate, javax.swing.GroupLayout.PREFERRED_SIZE, 41,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
							.addComponent(btnClear, javax.swing.GroupLayout.PREFERRED_SIZE, 41,
								javax.swing.GroupLayout.PREFERRED_SIZE))
						.addComponent(scpPrivateKey, javax.swing.GroupLayout.DEFAULT_SIZE, 265,
							Short.MAX_VALUE)
						.addComponent(scpPublicKey))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 24,
						Short.MAX_VALUE)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(btnSaveCertificate, javax.swing.GroupLayout.PREFERRED_SIZE,
							41, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(btnSavePrivateKey, javax.swing.GroupLayout.PREFERRED_SIZE, 41,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(btnSavePrivKeyWithPw, javax.swing.GroupLayout.PREFERRED_SIZE,
							41, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(btnSavePublicKey, javax.swing.GroupLayout.PREFERRED_SIZE, 41,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addContainerGap()));
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
		// onInitializeGridBagLayout();
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
