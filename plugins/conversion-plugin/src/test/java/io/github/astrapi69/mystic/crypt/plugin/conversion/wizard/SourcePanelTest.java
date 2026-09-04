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
package io.github.astrapi69.mystic.crypt.plugin.conversion.wizard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.security.Security;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.BaseModel;

/**
 * Tests that the wizard's Source step keeps its state in the wizard model: what is typed into the
 * source field is in the model at once, the file is looked at from there, and a file that cannot be
 * read says so instead of leaving the wizard silently stuck.
 */
class SourcePanelTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private static SourcePanel newPanel(ConversionWizardModel model)
	{
		BaseWizardStateMachineModel<ConversionWizardModel> stateMachine = BaseWizardStateMachineModel
			.<ConversionWizardModel> builder().currentState(ConversionWizardState.SOURCE)
			.modelObject(model).build();
		return new SourcePanel(BaseModel.of(stateMachine));
	}

	@Test
	void typingAPathIntoTheSourceFieldFillsTheModelAndSaysWhatTheFileHolds(@TempDir File directory)
		throws Exception
	{
		File source = new File(directory, "key.pem");
		PrivateKeyWriter.writeInPemFormat(KeyPairFactory.newKeyPair("RSA").getPrivate(), source);
		ConversionWizardModel model = new ConversionWizardModel();
		SourcePanel panel = newPanel(model);

		textField(panel, "txtSourceFile").setText(source.getAbsolutePath());

		assertEquals(source.getAbsolutePath(), model.getSourceFilePath(),
			"what is typed into the field has to be in the model at once");
		assertEquals("an RSA private key, PKCS#1", label(panel, "lblWhatItHolds").getText(),
			"the file has to be looked at from the model, not from the widget");
		assertNotNull(model.getFileKind(), "the detected kind belongs in the model too");
	}

	@Test
	void clearingTheSourceFieldEmptiesTheModel(@TempDir File directory) throws Exception
	{
		File source = new File(directory, "key.pem");
		PrivateKeyWriter.writeInPemFormat(KeyPairFactory.newKeyPair("RSA").getPrivate(), source);
		ConversionWizardModel model = new ConversionWizardModel();
		SourcePanel panel = newPanel(model);
		JTextField sourceField = textField(panel, "txtSourceFile");
		sourceField.setText(source.getAbsolutePath());

		sourceField.setText("");

		assertEquals("", model.getSourceFilePath(), "clearing the field has to clear the model");
		assertEquals(ConversionWizardModel.NOTHING_TO_SAY, model.getWhatItHolds(),
			"with no file chosen there is nothing to say about it");
		assertNull(model.getFileKind(), "no file means no detected kind");
	}

	@Test
	void aFileThatCannotBeReadSaysSoInsteadOfLeavingTheWizardStuck(@TempDir File directory)
	{
		ConversionWizardModel model = new ConversionWizardModel();
		SourcePanel panel = newPanel(model);

		textField(panel, "txtSourceFile").setText(new File(directory, "missing.pem").getAbsolutePath());

		assertNull(model.getFileKind(), "a file that cannot be read has no detected kind");
		assertTrue(model.getWhatItHolds().startsWith("not read:"),
			"the model has to say why nothing was detected, it holds: " + model.getWhatItHolds());
	}

	@Test
	void everyFieldExplainsItselfWithATooltip()
	{
		SourcePanel panel = newPanel(new ConversionWizardModel());

		assertHasTooltip((JComponent)componentNamed(panel, "txtSourceFile"), "source file");
		assertHasTooltip((JComponent)componentNamed(panel, "btnBrowseSource"), "browse source button");
		assertHasTooltip((JComponent)componentNamed(panel, "lblWhatItHolds"), "what it holds");
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

	private JLabel label(Container container, String name)
	{
		return (JLabel)componentNamed(container, name);
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
