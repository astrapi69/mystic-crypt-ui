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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.type.KeystoreType;
import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.plugin.keystore.KeyStoreSupport;

/**
 * Tests that the wizard's Store step keeps its state in the wizard model: what is typed or chosen
 * is in the model at once, and the type combo box offers exactly the store types the tool supports,
 * in the order it offers them.
 */
class StorePanelTest
{

	private static StorePanel newPanel(CreateKeyStoreWizardModel model)
	{
		BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine = BaseWizardStateMachineModel
			.<CreateKeyStoreWizardModel> builder().currentState(CreateKeyStoreWizardState.STORE)
			.modelObject(model).build();
		return new StorePanel(BaseModel.of(stateMachine));
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
	void typingAFilePathFillsTheModelAtOnce()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();
		StorePanel panel = newPanel(model);

		textField(panel, "txtKeyStoreFile").setText("/tmp/new.p12");

		assertEquals("/tmp/new.p12", model.getKeyStoreFilePath());
	}

	@Test
	void choosingATypeFillsTheModelAtOnce()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();
		StorePanel panel = newPanel(model);

		comboBox(panel, "cmbKeystoreType").setSelectedItem(KeystoreType.JKS);

		assertEquals(KeystoreType.JKS, model.getKeystoreType());
	}

	@Test
	void typingThePasswordAndItsRepetitionFillsTheModelAtOnce()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();
		StorePanel panel = newPanel(model);

		passwordField(panel, "pwdStorePassword").setText("secret");
		passwordField(panel, "pwdStorePasswordRepeated").setText("secret");

		assertEquals("secret", new String(model.getStorePassword()));
		assertEquals("secret", new String(model.getStorePasswordRepeated()));
	}

	@Test
	void theTypeComboBoxOffersTheSupportedTypesInTheOfferedOrder()
	{
		StorePanel panel = newPanel(new CreateKeyStoreWizardModel());

		assertEquals(KeyStoreSupport.USABLE_TYPES, itemsOf(comboBox(panel, "cmbKeystoreType")));
	}

	@Test
	void everyFieldExplainsItselfWithATooltip()
	{
		StorePanel panel = newPanel(new CreateKeyStoreWizardModel());

		assertHasTooltip((JComponent)componentNamed(panel, "txtKeyStoreFile"), "key store file");
		assertHasTooltip((JComponent)componentNamed(panel, "btnBrowseKeyStoreFile"), "browse button");
		assertHasTooltip((JComponent)componentNamed(panel, "cmbKeystoreType"), "type");
		assertHasTooltip((JComponent)componentNamed(panel, "pwdStorePassword"), "password");
		assertHasTooltip((JComponent)componentNamed(panel, "pwdStorePasswordRepeated"),
			"repeated password");
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

	private JPasswordField passwordField(Container container, String name)
	{
		return (JPasswordField)componentNamed(container, name);
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
