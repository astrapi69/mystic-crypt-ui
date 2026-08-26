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
package io.github.astrapi69.mystic.crypt.plugin.conversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.Security;

import javax.swing.JButton;
import javax.swing.JTextField;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.mystic.crypt.crypto.KeyFiles;

/**
 * Tests that the panel keeps its state in its model: what is typed into a field is in the model at
 * once, the file is looked at from the model, and a conversion started by a button reads the model
 * rather than the widgets.
 */
class ConversionPanelModelBindingTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@Test
	void typingAPathIntoTheSourceFieldFillsTheModelAndSaysWhatTheFileHolds(@TempDir File directory)
		throws Exception
	{
		File source = writePkcs1PrivateKey(directory);
		ConversionPanel panel = new ConversionPanel();

		textField(panel, "txtSourceFile").setText(source.getAbsolutePath());

		assertEquals(source.getAbsolutePath(), panel.getModelObject().getSourceFilePath(),
			"what is typed into the field has to be in the model at once");
		assertEquals("an RSA private key, PKCS#1", panel.getWhatItHoldsText(),
			"the file has to be looked at from the model, not from the widget");
		assertNotNull(panel.getModelObject().getFileKind(),
			"the detected kind belongs in the model too");
		assertTrue(button(panel, "btnToPkcs8").isEnabled(),
			"a private key can be converted to PKCS#8");
	}

	@Test
	void clearingTheSourceFieldEmptiesTheModelAndTheConversionsOnOffer(@TempDir File directory)
		throws Exception
	{
		File source = writePkcs1PrivateKey(directory);
		ConversionPanel panel = new ConversionPanel();
		JTextField sourceField = textField(panel, "txtSourceFile");
		sourceField.setText(source.getAbsolutePath());

		sourceField.setText("");

		assertEquals("", panel.getModelObject().getSourceFilePath(),
			"clearing the field has to clear the model");
		assertEquals(ConversionPanelModel.NOTHING_TO_SAY, panel.getWhatItHoldsText(),
			"with no file chosen there is nothing to say about it");
		assertNull(panel.getModelObject().getFileKind(), "no file means no detected kind");
		assertTrue(!button(panel, "btnToPkcs8").isEnabled(),
			"with no file chosen no conversion is on offer");
	}

	@Test
	void theConversionButtonConvertsTheFilesNamedInTheModel(@TempDir File directory)
		throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		File source = new File(directory, "openssl-key.pem");
		PrivateKeyWriter.writeInPemFormat(keyPair.getPrivate(), source);
		File target = new File(directory, "java-key.pem");
		ConversionPanel panel = new ConversionPanel();
		textField(panel, "txtSourceFile").setText(source.getAbsolutePath());
		textField(panel, "txtTargetFile").setText(target.getAbsolutePath());

		button(panel, "btnToPkcs8").doClick();

		assertTrue(target.exists(), "the converted key must be written: " + panel.getResultText());
		assertTrue(Files.readString(target.toPath()).contains("BEGIN PRIVATE KEY"),
			"PKCS#8 is what Java reads, and it says so in its header");
		assertEquals(keyPair.getPrivate(), KeyFiles.readPrivateKey(target),
			"the converted file has to hold the same key");
		assertEquals("written as PKCS#8 to " + target.getName(),
			panel.getModelObject().getResultMessage(), "the message shown belongs in the model");
	}

	private File writePkcs1PrivateKey(File directory) throws Exception
	{
		File file = new File(directory, "openssl-key.pem");
		PrivateKeyWriter.writeInPemFormat(KeyPairFactory.newKeyPair("RSA").getPrivate(), file);
		assertTrue(Files.readString(file.toPath()).contains("BEGIN RSA PRIVATE KEY"),
			"the fixture must really be PKCS#1, otherwise this test proves nothing");
		return file;
	}

	private JTextField textField(Container container, String name)
	{
		return (JTextField)componentNamed(container, name);
	}

	private JButton button(Container container, String name)
	{
		return (JButton)componentNamed(container, name);
	}

	private Component componentNamed(Container container, String name)
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
