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
package io.github.astrapi69.mystic.crypt.plugin.keystore.wizard;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;

import io.github.astrapi69.crypt.api.type.KeystoreType;
import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.plugin.keystore.KeyStoreMessages;
import io.github.astrapi69.mystic.crypt.plugin.keystore.KeyStoreSupport;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.model.combobox.EnumComboBoxModel;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMPasswordField;
import io.github.astrapi69.swing.model.component.JMTextField;
import net.miginfocom.swing.MigLayout;

/**
 * The wizard's first step: where the new key store is written, its format, and its password - asked
 * for twice, the same "asked twice because a typo is costly" convention {@code FileCryptPanel} and
 * {@code SecretSharingPanel} already use elsewhere in this app. Unlike the dense "Manage Key Store"
 * panel, this step exists for exactly one job - describing a store that does not exist yet - so it
 * has none of the open/import/export fields that panel needs.
 */
public class StorePanel extends BasePanel<BaseWizardStateMachineModel<CreateKeyStoreWizardModel>>
{

	private static final long serialVersionUID = 1L;

	private JLabel lblHeader;
	private JLabel lblKeyStoreFile;
	private JMTextField txtKeyStoreFile;
	private JButton btnBrowseKeyStoreFile;
	private JLabel lblType;
	private JMComboBox<KeystoreType, ?> cmbKeystoreType;
	private JLabel lblPassword;
	private JMPasswordField pwdStorePassword;
	private JLabel lblPasswordRepeated;
	private JMPasswordField pwdStorePasswordRepeated;

	public StorePanel(IModel<BaseWizardStateMachineModel<CreateKeyStoreWizardModel>> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		lblHeader = new JLabel("Store");
		lblKeyStoreFile = new JLabel("Key store file:");
		txtKeyStoreFile = new JMTextField(38);
		txtKeyStoreFile.setName("txtKeyStoreFile");
		btnBrowseKeyStoreFile = new JButton("...");
		btnBrowseKeyStoreFile.setName("btnBrowseKeyStoreFile");
		btnBrowseKeyStoreFile.addActionListener(event -> onBrowse());

		lblType = new JLabel("Type:");
		cmbKeystoreType = new JMComboBox<>(
			offeredValues(KeystoreType.class, KeyStoreSupport.USABLE_TYPES));
		cmbKeystoreType.setName("cmbKeystoreType");

		lblPassword = new JLabel("Store password:");
		pwdStorePassword = new JMPasswordField(20);
		pwdStorePassword.setName("pwdStorePassword");

		lblPasswordRepeated = new JLabel("Repeat password:");
		pwdStorePasswordRepeated = new JMPasswordField(20);
		pwdStorePasswordRepeated.setName("pwdStorePasswordRepeated");

		CreateKeyStoreWizardModel domainModel = getModelObject().getModelObject();
		txtKeyStoreFile.setPropertyModel(
			LambdaModel.of(domainModel::getKeyStoreFilePath, domainModel::setKeyStoreFilePath));
		cmbKeystoreType.setPropertyModel(
			LambdaModel.of(domainModel::getKeystoreType, domainModel::setKeystoreType));
		pwdStorePassword.setPropertyModel(
			LambdaModel.of(domainModel::getStorePassword, domainModel::setStorePassword));
		pwdStorePasswordRepeated.setPropertyModel(LambdaModel.of(
			domainModel::getStorePasswordRepeated, domainModel::setStorePasswordRepeated));

		txtKeyStoreFile.setToolTipText(KeyStoreMessages.getString("keystore.wizard.store.tooltip.file",
			"where the new key store is written"));
		btnBrowseKeyStoreFile.setToolTipText(KeyStoreMessages.getString(
			"keystore.wizard.store.tooltip.browse", "choose where to write the new key store"));
		cmbKeystoreType.setToolTipText(KeyStoreMessages.getString("keystore.wizard.store.tooltip.type",
			"the key store format - JKS, PKCS12 or JCEKS"));
		pwdStorePassword.setToolTipText(KeyStoreMessages.getString(
			"keystore.wizard.store.tooltip.password", "the password that will protect the new key store"));
		pwdStorePasswordRepeated.setToolTipText(KeyStoreMessages.getString(
			"keystore.wizard.store.tooltip.password.repeated",
			"the same password again - a typo here that nothing catches locks the store immediately"));
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		MigLayout migLayout = new MigLayout("wrap 3", "[][grow,fill][]", "[][][][][][grow]");
		setLayout(migLayout);

		add(lblHeader, "span, align center, gapbottom 10");
		add(lblKeyStoreFile);
		add(txtKeyStoreFile, "growx");
		add(btnBrowseKeyStoreFile);
		add(lblType);
		add(cmbKeystoreType, "span 2, growx");
		add(lblPassword);
		add(pwdStorePassword, "span 2, growx");
		add(lblPasswordRepeated);
		add(pwdStorePasswordRepeated, "span 2, growx");
	}

	private void onBrowse()
	{
		CreateKeyStoreWizardModel domainModel = getModelObject().getModelObject();
		String currentPath = domainModel.getKeyStoreFilePath() == null
			? ""
			: domainModel.getKeyStoreFilePath().trim();
		JFileChooser fileChooser = new JFileChooser();
		if (!currentPath.isEmpty())
		{
			fileChooser.setSelectedFile(new File(currentPath));
		}
		if (fileChooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION)
		{
			txtKeyStoreFile.setText(fileChooser.getSelectedFile().getAbsolutePath());
		}
	}

	/**
	 * A combo box model over an enum, holding only the values this wizard offers and holding them in
	 * the order they are offered in - the exclude set of {@link EnumComboBoxModel} is a hash set and
	 * would leave the order to the hash codes. Mirrors {@code KeyStorePanel}'s own helper of the same
	 * shape.
	 *
	 * @param <E>
	 *            the type of the enum
	 * @param enumClass
	 *            the enum class the values come from
	 * @param offered
	 *            the values the wizard offers, in the order they are offered in
	 * @return the combo box model over the offered values
	 */
	private static <E extends Enum<E>> EnumComboBoxModel<E> offeredValues(final Class<E> enumClass,
		final List<E> offered)
	{
		EnumComboBoxModel<E> comboBoxModel = new EnumComboBoxModel<>(enumClass, offered.get(0));
		comboBoxModel.setComboList(new ArrayList<>(offered));
		return comboBoxModel;
	}
}
