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
package io.github.astrapi69.mystic.crypt.panel.certificate;

import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import io.github.astrapi69.crypt.data.model.DistinguishedNameInfo;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.ui.screen.ScreenPlacement;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.ValidityModel;
import io.github.astrapi69.random.number.RandomBigIntegerFactory;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.dialog.factory.JDialogFactory;
import io.github.astrapi69.swing.listener.RequestFocusListener;
import io.github.astrapi69.swing.model.combobox.GenericComboBoxModel;
import io.github.astrapi69.swing.model.component.JMBigIntegerTextField;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMTextField;
import lombok.Getter;

/**
 * Panel for the data of a new certificate: its version and serial number, the issuer and the
 * subject that are picked in a dialog of their own, the period it is valid in and the algorithm it
 * is signed with.
 * <p>
 * Every component is bound to the {@link CertificateInfoModel} the panel was constructed with, so
 * what was typed or chosen is readable there at any moment - the same model the certificate wizard
 * fills on its own steps.
 */
@Getter
public class NewCertificateInfoPanel extends BasePanel<CertificateInfoModel>
{

	/** The X.509 versions a new certificate can be created in */
	private static final Integer[] VERSIONS = { 1, 2, 3 };

	private JButton btnAddExtension;
	private JButton btnCreateIssuer;
	private JButton btnCreateSubject;
	private JButton btnGenerateSerialNumber;
	private JMComboBox<Integer, GenericComboBoxModel<Integer>> cmbVersion;
	private JLabel lblExtensions;
	private JLabel lblIssuer;
	private JLabel lblNotAfter;
	private JLabel lblNotBefore;
	private JLabel lblPublicKey;
	private JLabel lblSerialNumber;
	private JLabel lblSignatureAlgorithm;
	private JLabel lblSubject;
	private JLabel lblVersion;
	private JScrollPane scpExtensions;
	private JScrollPane scpPublicKey;
	private JTable srcExtensions;
	private JMTextField txtIssuer;
	private JMTextField txtNotAfter;
	private JMTextField txtNotBefore;
	private JTextArea txtPublicKey;
	private JMBigIntegerTextField txtSerialNumber;
	private JMTextField txtSignatureAlgorithm;
	private JMTextField txtSubject;

	public NewCertificateInfoPanel()
	{
		this(BaseModel.<CertificateInfoModel> of(CertificateInfoModel.builder().build()));
	}

	public NewCertificateInfoPanel(final IModel<CertificateInfoModel> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		lblVersion = new JLabel();
		// the combo box model starts on the version the model holds, so the selection the combo box
		// remembers and the one the model carries cannot drift apart
		cmbVersion = new JMComboBox<>(
			new GenericComboBoxModel<>(VERSIONS, getModelObject().getVersion()));
		lblSerialNumber = new JLabel();
		txtSerialNumber = new JMBigIntegerTextField();
		txtSerialNumber.setName("txtSerialNumber");
		lblIssuer = new JLabel();
		lblSubject = new JLabel();
		txtIssuer = new JMTextField();
		txtIssuer.setName("txtIssuer");
		txtSubject = new JMTextField();
		txtSubject.setName("txtSubject");
		lblNotBefore = new JLabel();
		txtNotBefore = new JMTextField();
		txtNotBefore.setName("txtNotBefore");
		lblNotAfter = new JLabel();
		btnCreateIssuer = new JButton();
		btnCreateSubject = new JButton();
		txtNotAfter = new JMTextField();
		txtNotAfter.setName("txtNotAfter");
		lblPublicKey = new JLabel();
		scpPublicKey = new JScrollPane();
		txtPublicKey = new JTextArea();
		lblSignatureAlgorithm = new JLabel();
		txtSignatureAlgorithm = new JMTextField();
		txtSignatureAlgorithm.setName("txtSignatureAlgorithm");
		lblExtensions = new JLabel();
		scpExtensions = new JScrollPane();
		srcExtensions = new JTable();
		btnAddExtension = new JButton();
		btnGenerateSerialNumber = new JButton();

		lblVersion.setText("Version:");

		lblSerialNumber.setText("Serial Number:");

		txtSerialNumber.setText("");

		lblIssuer.setText("Issuer:");

		lblSubject.setText("Subject:");

		txtIssuer.setText("");

		txtSubject.setText("");

		lblNotBefore.setText("Not Before:");

		txtNotBefore.setText("");

		lblNotAfter.setText("Not After:");

		btnCreateIssuer.setText("Create Issuer");
		btnCreateIssuer.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				onCreateIssuer(evt);
			}
		});

		btnCreateSubject.setText("Create Subj.");
		btnCreateSubject.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				onCreateSubject(evt);
			}
		});

		txtNotAfter.setText("");

		lblPublicKey.setText("Public Key:");

		txtPublicKey.setColumns(20);
		txtPublicKey.setRows(5);
		scpPublicKey.setViewportView(txtPublicKey);

		lblSignatureAlgorithm.setText("Signature Algorithm:");

		txtSignatureAlgorithm.setText("");

		lblExtensions.setText("Extensions:");

		srcExtensions.setModel(new DefaultTableModel(new Object[][] { { null, null } },
			new String[] { "Key", "Value" })
		{
			Class[] types = new Class[] { java.lang.String.class, java.lang.String.class };

			public Class getColumnClass(int columnIndex)
			{
				return types[columnIndex];
			}
		});
		scpExtensions.setViewportView(srcExtensions);

		btnAddExtension.setText("Add");
		btnAddExtension.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				onAddExtension(evt);
			}
		});

		btnGenerateSerialNumber.setText("Generate");
		btnGenerateSerialNumber.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				onGenerateSerialNumber(evt);
			}
		});

		// Manually added:

		// enable when functionality is given...
		btnAddExtension.setEnabled(false);

		bindComponents();
	}

	/**
	 * Binds every component to the {@link CertificateInfoModel} of this panel, so that each edit
	 * lands in the model and the model is what the buttons read - the components carry the values
	 * the model already holds
	 */
	protected void bindComponents()
	{
		CertificateInfoModel modelObject = getModelObject();
		cmbVersion
			.setPropertyModel(LambdaModel.of(modelObject::getVersion, modelObject::setVersion));
		txtSerialNumber
			.setPropertyModel(LambdaModel.of(modelObject::getSerial, modelObject::setSerial));
		txtIssuer.setPropertyModel(LambdaModel
			.<String> of(() -> representableString(modelObject.getIssuer()), issuer -> modelObject
				.setIssuer(toDistinguishedNameInfoModel(issuer, modelObject.getIssuer()))));
		txtSubject.setPropertyModel(LambdaModel
			.<String> of(() -> representableString(modelObject.getSubject()), subject -> modelObject
				.setSubject(toDistinguishedNameInfoModel(subject, modelObject.getSubject()))));
		txtNotBefore.setPropertyModel(LambdaModel.of(this::notBeforeText, this::setNotBeforeText));
		txtNotAfter.setPropertyModel(LambdaModel.of(this::notAfterText, this::setNotAfterText));
		txtSignatureAlgorithm.setPropertyModel(
			LambdaModel.of(modelObject::getSignatureAlgorithm, modelObject::setSignatureAlgorithm));
	}

	/**
	 * The first moment the certificate is valid at, as text and empty when no validity period was
	 * collected yet
	 *
	 * @return the start of the validity period as text
	 */
	protected String notBeforeText()
	{
		ValidityModel validityModel = getModelObject().getValidityModel();
		return validityModel == null ? "" : text(validityModel.getNotBefore());
	}

	/**
	 * Sets the first moment the certificate is valid at from what was typed
	 *
	 * @param notBefore
	 *            the start of the validity period as text
	 */
	protected void setNotBeforeText(final String notBefore)
	{
		ValidityModel validityModel = getModelObject().getValidityModel();
		if (validityModel != null)
		{
			validityModel.setNotBefore(toZonedDateTime(notBefore, validityModel.getNotBefore()));
		}
	}

	/**
	 * The last moment the certificate is valid at, as text and empty when no validity period was
	 * collected yet
	 *
	 * @return the end of the validity period as text
	 */
	protected String notAfterText()
	{
		ValidityModel validityModel = getModelObject().getValidityModel();
		return validityModel == null ? "" : text(validityModel.getNotAfter());
	}

	/**
	 * Sets the last moment the certificate is valid at from what was typed
	 *
	 * @param notAfter
	 *            the end of the validity period as text
	 */
	protected void setNotAfterText(final String notAfter)
	{
		ValidityModel validityModel = getModelObject().getValidityModel();
		if (validityModel != null)
		{
			validityModel.setNotAfter(toZonedDateTime(notAfter, validityModel.getNotAfter()));
		}
	}

	/**
	 * The moment as text, empty when there is none
	 *
	 * @param moment
	 *            the moment, may be null
	 * @return the moment as text, empty when there is none
	 */
	private static String text(final ZonedDateTime moment)
	{
		return moment == null ? "" : moment.toString();
	}

	/**
	 * The moment a typed text describes. A blank or half typed date is an edit in progress and not
	 * a new moment, so it keeps the one the validity period already holds.
	 *
	 * @param text
	 *            the text that was typed
	 * @param current
	 *            the moment the validity period holds
	 * @return the moment the text describes, or the current one when the text is not a moment yet
	 */
	private static ZonedDateTime toZonedDateTime(final String text, final ZonedDateTime current)
	{
		if (text == null || text.isBlank())
		{
			return current;
		}
		try
		{
			return ZonedDateTime.parse(text);
		}
		catch (final DateTimeParseException editInProgress)
		{
			return current;
		}
	}

	/**
	 * The distinguished name as a certificate writes it, empty when there is none yet
	 *
	 * @param distinguishedNameInfoModel
	 *            the distinguished name, may be null
	 * @return the representable string of the distinguished name, empty when there is none
	 */
	private static String representableString(
		final DistinguishedNameInfoModel distinguishedNameInfoModel)
	{
		return distinguishedNameInfoModel == null
			? ""
			: DistinguishedNameInfoModel.toDistinguishedNameInfo(distinguishedNameInfoModel)
				.toRepresentableString();
	}

	/**
	 * The distinguished name a typed text describes. A blank field says that nothing was entered
	 * and not that the distinguished name is gone, so it keeps the one the model already holds.
	 *
	 * @param text
	 *            the text that was typed
	 * @param current
	 *            the distinguished name the model holds
	 * @return the distinguished name the text describes, or the current one when the text is blank
	 */
	private static DistinguishedNameInfoModel toDistinguishedNameInfoModel(final String text,
		final DistinguishedNameInfoModel current)
	{
		if (text == null || text.isBlank())
		{
			return current;
		}
		DistinguishedNameInfo distinguishedNameInfo = DistinguishedNameInfo
			.toDistinguishedNameInfo(text);
		return DistinguishedNameInfoModel.builder()
			.commonName(distinguishedNameInfo.getCommonName())
			.countryCode(distinguishedNameInfo.getCountryCode())
			.location(distinguishedNameInfo.getLocation())
			.organisation(distinguishedNameInfo.getOrganisation())
			.organisationUnit(distinguishedNameInfo.getOrganisationUnit())
			.state(distinguishedNameInfo.getState()).build();
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
	}

	protected void onInitializeGroupLayout()
	{
		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout
				.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(scpExtensions, GroupLayout.PREFERRED_SIZE, 459,
					GroupLayout.PREFERRED_SIZE)
				.addGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
						.addComponent(lblNotAfter, GroupLayout.Alignment.TRAILING,
							GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(lblExtensions, GroupLayout.DEFAULT_SIZE,
							GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(lblSignatureAlgorithm, GroupLayout.DEFAULT_SIZE, 200,
							Short.MAX_VALUE)
						.addComponent(lblPublicKey, GroupLayout.DEFAULT_SIZE,
							GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(lblVersion, GroupLayout.DEFAULT_SIZE,
							GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(lblSerialNumber, GroupLayout.Alignment.TRAILING,
							GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
					.addGap(59, 59, 59)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
						.addComponent(txtNotAfter, GroupLayout.Alignment.LEADING)
						.addComponent(txtNotBefore, GroupLayout.Alignment.LEADING)
						.addComponent(txtSubject, GroupLayout.Alignment.LEADING)
						.addComponent(txtIssuer, GroupLayout.Alignment.LEADING)
						.addComponent(cmbVersion, GroupLayout.Alignment.LEADING, 0,
							GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(txtSerialNumber, GroupLayout.Alignment.LEADING)
						.addComponent(scpPublicKey, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
						.addComponent(txtSignatureAlgorithm, GroupLayout.Alignment.LEADING,
							GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)))
				.addComponent(lblNotBefore, GroupLayout.PREFERRED_SIZE, 200,
					GroupLayout.PREFERRED_SIZE)
				.addComponent(lblSubject, GroupLayout.PREFERRED_SIZE, 200,
					GroupLayout.PREFERRED_SIZE)
				.addComponent(lblIssuer, GroupLayout.PREFERRED_SIZE, 200,
					GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE,
					Short.MAX_VALUE)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
					.addComponent(btnCreateIssuer, GroupLayout.DEFAULT_SIZE,
						GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(btnCreateSubject, GroupLayout.DEFAULT_SIZE,
						GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(btnAddExtension, GroupLayout.DEFAULT_SIZE,
						GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(btnGenerateSerialNumber, GroupLayout.DEFAULT_SIZE,
						GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				.addContainerGap(55, Short.MAX_VALUE)));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(cmbVersion, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE)
					.addComponent(lblVersion))
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(btnGenerateSerialNumber, GroupLayout.Alignment.TRAILING)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(lblSerialNumber)
						.addComponent(txtSerialNumber, GroupLayout.PREFERRED_SIZE,
							GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(btnCreateIssuer, GroupLayout.Alignment.TRAILING)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(lblIssuer).addComponent(txtIssuer, GroupLayout.PREFERRED_SIZE,
							GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(lblSubject)
					.addComponent(txtSubject, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE)
					.addComponent(btnCreateSubject))
				.addGap(15, 15, 15)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(lblNotBefore).addComponent(txtNotBefore,
						GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE))
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(lblNotAfter).addComponent(txtNotAfter, GroupLayout.PREFERRED_SIZE,
						GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(lblPublicKey).addComponent(scpPublicKey,
						GroupLayout.PREFERRED_SIZE, 220, GroupLayout.PREFERRED_SIZE))
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(txtSignatureAlgorithm, GroupLayout.PREFERRED_SIZE,
						GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addComponent(lblSignatureAlgorithm))
				.addGap(18, 18, 18).addComponent(lblExtensions).addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(scpExtensions, GroupLayout.PREFERRED_SIZE, 50,
						GroupLayout.PREFERRED_SIZE)
					.addComponent(btnAddExtension))
				.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
	}

	protected void onCreateIssuer(java.awt.event.ActionEvent evt)
	{
		NewCertificateAttributesPanel panel = new NewCertificateAttributesPanel();

		JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION);

		JDialog dialog = JDialogFactory.newJDialog(MysticCryptApplicationFrame.getInstance(),
			optionPane, "Create issuer");
		dialog.addWindowFocusListener(new RequestFocusListener(panel.getTxtCommonName()));
		dialog.pack();
		ScreenPlacement.centerOnScreenOf(dialog, this);
		dialog.setVisible(true);

		if (optionPane.getValue().equals(JOptionPane.OK_OPTION))
		{
			DistinguishedNameInfoModel newIssuerModelObject = panel.getModelObject();

			String issuer = DistinguishedNameInfoModel.toDistinguishedNameInfo(newIssuerModelObject)
				.toRepresentableString();
			CertificateInfoModel modelObject = getModelObject();
			// the field writes what it shows back into the model, so the issuer that was picked is
			// set afterwards and is the one the model keeps
			getTxtIssuer().setText(issuer);
			modelObject.setIssuer(newIssuerModelObject);
		}

	}

	protected void onGenerateSerialNumber(java.awt.event.ActionEvent evt)
	{
		BigInteger serialNumber = RandomBigIntegerFactory.randomSerialNumber();
		CertificateInfoModel modelObject = getModelObject();
		modelObject.setSerial(serialNumber);
		getTxtSerialNumber().setText(serialNumber.toString());
	}

	protected void onCreateSubject(java.awt.event.ActionEvent evt)
	{
		NewCertificateAttributesPanel panel = new NewCertificateAttributesPanel();

		JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION);

		JDialog dialog = JDialogFactory.newJDialog(MysticCryptApplicationFrame.getInstance(),
			optionPane, "Create subject");
		dialog.addWindowFocusListener(new RequestFocusListener(panel.getTxtCommonName()));
		dialog.pack();
		ScreenPlacement.centerOnScreenOf(dialog, this);
		dialog.setVisible(true);

		if (optionPane.getValue().equals(JOptionPane.OK_OPTION))
		{
			DistinguishedNameInfoModel newSubjectModelObject = panel.getModelObject();
			CertificateInfoModel modelObject = getModelObject();

			String subject = DistinguishedNameInfoModel
				.toDistinguishedNameInfo(newSubjectModelObject).toRepresentableString();
			// the field writes what it shows back into the model, so the subject that was picked is
			// set afterwards and is the one the model keeps
			getTxtSubject().setText(subject);
			modelObject.setSubject(newSubjectModelObject);
		}
	}

	protected void onAddExtension(java.awt.event.ActionEvent evt)
	{

	}

}
