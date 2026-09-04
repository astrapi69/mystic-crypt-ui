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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.plugin.keystore.KeyStoreSupport;

/**
 * Tests that the wizard's Entry step keeps its state in the wizard model, and that its alias,
 * subject and algorithm fields are only enabled while "Add a key pair now" is actually checked -
 * they apply to nothing otherwise.
 */
class EntryPanelTest
{

	private static EntryPanel newPanel(CreateKeyStoreWizardModel model)
	{
		BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine = BaseWizardStateMachineModel
			.<CreateKeyStoreWizardModel> builder().currentState(CreateKeyStoreWizardState.ENTRY)
			.modelObject(model).build();
		return new EntryPanel(BaseModel.of(stateMachine));
	}

	private static List<Object> itemsOf(JComboBox<?> comboBox)
	{
		List<Object> items = new ArrayList<>();
		for (int index = 0; index < comboBox.getItemCount(); index++)
		{
			items.add(comboBox.getItemAt(index));
		}
		return items;
	}

	@Test
	void theFieldsStartDisabledSinceNoKeyPairIsRequestedByDefault()
	{
		EntryPanel panel = newPanel(new CreateKeyStoreWizardModel());

		assertFalse(textField(panel, "txtAlias").isEnabled());
		assertFalse(textField(panel, "txtDistinguishedName").isEnabled());
		assertFalse(comboBox(panel, "cmbKeyAlgorithm").isEnabled());
	}

	@Test
	void checkingAddKeyPairNowFillsTheModelAndEnablesTheFields()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();
		EntryPanel panel = newPanel(model);

		checkBox(panel, "chkAddKeyPairNow").doClick();

		assertTrue(model.isAddKeyPairNow());
		assertTrue(textField(panel, "txtAlias").isEnabled());
		assertTrue(textField(panel, "txtDistinguishedName").isEnabled());
		assertTrue(comboBox(panel, "cmbKeyAlgorithm").isEnabled());
	}

	@Test
	void uncheckingAddKeyPairNowDisablesTheFieldsAgain()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();
		EntryPanel panel = newPanel(model);
		checkBox(panel, "chkAddKeyPairNow").doClick();

		checkBox(panel, "chkAddKeyPairNow").doClick();

		assertFalse(model.isAddKeyPairNow());
		assertFalse(textField(panel, "txtAlias").isEnabled());
		assertFalse(textField(panel, "txtDistinguishedName").isEnabled());
		assertFalse(comboBox(panel, "cmbKeyAlgorithm").isEnabled());
	}

	@Test
	void typingAnAliasAndASubjectFillsTheModelAtOnce()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();
		EntryPanel panel = newPanel(model);

		textField(panel, "txtAlias").setText("server");
		textField(panel, "txtDistinguishedName").setText("CN=example.com");

		assertEquals("server", model.getAlias());
		assertEquals("CN=example.com", model.getDistinguishedName());
	}

	@Test
	void choosingAnAlgorithmFillsTheModelAtOnce()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();
		EntryPanel panel = newPanel(model);

		comboBox(panel, "cmbKeyAlgorithm").setSelectedItem(KeyPairGeneratorAlgorithm.EC);

		assertEquals(KeyPairGeneratorAlgorithm.EC, model.getKeyAlgorithm());
	}

	@Test
	void theAlgorithmComboBoxOffersTheSupportedAlgorithmsInTheOfferedOrder()
	{
		EntryPanel panel = newPanel(new CreateKeyStoreWizardModel());

		assertEquals(KeyStoreSupport.KEY_ALGORITHMS, itemsOf(comboBox(panel, "cmbKeyAlgorithm")));
	}

	@Test
	void everyFieldExplainsItselfWithATooltip()
	{
		EntryPanel panel = newPanel(new CreateKeyStoreWizardModel());

		assertHasTooltip((JComponent)componentNamed(panel, "chkAddKeyPairNow"), "add key pair now");
		assertHasTooltip((JComponent)componentNamed(panel, "txtAlias"), "alias");
		assertHasTooltip((JComponent)componentNamed(panel, "txtDistinguishedName"),
			"distinguished name");
		assertHasTooltip((JComponent)componentNamed(panel, "cmbKeyAlgorithm"), "key algorithm");
	}

	private static void assertHasTooltip(JComponent component, String fieldName)
	{
		String tooltip = component.getToolTipText();
		assertTrue(tooltip != null && !tooltip.isBlank(), fieldName + " must have a tooltip");
	}

	private JTextField textField(Container container, String name)
	{
		return (JTextField)componentNamed(container, name);
	}

	private JCheckBox checkBox(Container container, String name)
	{
		return (JCheckBox)componentNamed(container, name);
	}

	@SuppressWarnings("unchecked")
	private JComboBox<Object> comboBox(Container container, String name)
	{
		return (JComboBox<Object>)componentNamed(container, name);
	}

	private static Component componentNamed(Container container, String name)
	{
		for (Component child : container.getComponents())
		{
			if (name.equals(child.getName()))
			{
				return child;
			}
			if (child instanceof Container nested)
			{
				Component found = componentNamed(nested, name);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}
}
