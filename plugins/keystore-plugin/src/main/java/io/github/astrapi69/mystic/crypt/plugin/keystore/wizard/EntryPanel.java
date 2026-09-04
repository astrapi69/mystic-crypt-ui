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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.plugin.keystore.KeyStoreMessages;
import io.github.astrapi69.mystic.crypt.plugin.keystore.KeyStoreSupport;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.model.combobox.EnumComboBoxModel;
import io.github.astrapi69.swing.model.component.JMCheckBox;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMTextField;
import net.miginfocom.swing.MigLayout;

/**
 * The wizard's second step: offer to generate one key pair with a self-signed certificate right
 * away, skippable for an empty store. The alias, subject and algorithm fields only apply while
 * "Add a key pair now" is checked, so they stay disabled otherwise - a value typed into a disabled
 * field would suggest it means something when {@link CreateKeyStoreWizardModel#isAddKeyPairNow()}
 * says it does not.
 */
public class EntryPanel extends BasePanel<BaseWizardStateMachineModel<CreateKeyStoreWizardModel>>
{

	private static final long serialVersionUID = 1L;

	private JLabel lblHeader;
	private JMCheckBox chkAddKeyPairNow;
	private JLabel lblAlias;
	private JMTextField txtAlias;
	private JLabel lblDistinguishedName;
	private JMTextField txtDistinguishedName;
	private JLabel lblKeyAlgorithm;
	private JMComboBox<KeyPairGeneratorAlgorithm, ?> cmbKeyAlgorithm;

	public EntryPanel(IModel<BaseWizardStateMachineModel<CreateKeyStoreWizardModel>> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		lblHeader = new JLabel("Entry");
		chkAddKeyPairNow = new JMCheckBox("Add a key pair now");
		chkAddKeyPairNow.setName("chkAddKeyPairNow");

		lblAlias = new JLabel("Alias:");
		txtAlias = new JMTextField(20);
		txtAlias.setName("txtAlias");

		lblDistinguishedName = new JLabel("Distinguished name:");
		txtDistinguishedName = new JMTextField(30);
		txtDistinguishedName.setName("txtDistinguishedName");

		lblKeyAlgorithm = new JLabel("Key algorithm:");
		cmbKeyAlgorithm = new JMComboBox<>(
			offeredValues(KeyPairGeneratorAlgorithm.class, KeyStoreSupport.KEY_ALGORITHMS));
		cmbKeyAlgorithm.setName("cmbKeyAlgorithm");

		CreateKeyStoreWizardModel domainModel = getModelObject().getModelObject();
		chkAddKeyPairNow.setPropertyModel(
			LambdaModel.of(domainModel::isAddKeyPairNow, domainModel::setAddKeyPairNow));
		txtAlias.setPropertyModel(LambdaModel.of(domainModel::getAlias, domainModel::setAlias));
		txtDistinguishedName.setPropertyModel(
			LambdaModel.of(domainModel::getDistinguishedName, domainModel::setDistinguishedName));
		cmbKeyAlgorithm.setPropertyModel(
			LambdaModel.of(domainModel::getKeyAlgorithm, domainModel::setKeyAlgorithm));

		chkAddKeyPairNow.addActionListener(event -> updateFieldsEnabled());
		updateFieldsEnabled();

		chkAddKeyPairNow.setToolTipText(KeyStoreMessages.getString(
			"keystore.wizard.entry.tooltip.add.key.pair",
			"generate one key pair with a self-signed certificate right away, or leave the new "
				+ "store empty"));
		txtAlias.setToolTipText(KeyStoreMessages.getString("keystore.wizard.entry.tooltip.alias",
			"the name the first entry is stored and looked up under"));
		txtDistinguishedName.setToolTipText(
			KeyStoreMessages.getString("keystore.wizard.entry.tooltip.distinguished.name",
				"the subject of the self-signed certificate, for example CN=example.com"));
		cmbKeyAlgorithm.setToolTipText(KeyStoreMessages.getString(
			"keystore.wizard.entry.tooltip.key.algorithm",
			"the algorithm the first key pair is generated with"));
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		MigLayout migLayout = new MigLayout("wrap 2", "[][grow,fill]", "[][][][][grow]");
		setLayout(migLayout);

		add(lblHeader, "span, align center, gapbottom 10");
		add(chkAddKeyPairNow, "span 2");
		add(lblAlias);
		add(txtAlias, "growx");
		add(lblDistinguishedName);
		add(txtDistinguishedName, "growx");
		add(lblKeyAlgorithm);
		add(cmbKeyAlgorithm, "growx");
	}

	/**
	 * Only while a key pair is actually requested do the alias, subject and algorithm mean anything
	 */
	private void updateFieldsEnabled()
	{
		boolean requested = chkAddKeyPairNow.isSelected();
		txtAlias.setEnabled(requested);
		txtDistinguishedName.setEnabled(requested);
		cmbKeyAlgorithm.setEnabled(requested);
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
