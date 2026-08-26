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
package io.github.astrapi69.mystic.crypt.plugin.filecrypt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.security.Security;

import javax.swing.AbstractButton;
import javax.swing.JPasswordField;
import javax.swing.text.JTextComponent;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests that the panel works off its model: what is typed into a component is what the buttons act
 * on, and a file and a text go through encrypting and decrypting and come back unchanged.
 */
class FileCryptPanelTest
{

	static
	{
		System.setProperty("java.awt.headless", "true");
	}

	private static final String PASSPHRASE = "file-crypt-panel-test-passphrase-1969";

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@Test
	void getResultText_isTheModelsMessage_whenNothingHasRunYet()
	{
		FileCryptPanel panel = new FileCryptPanel();

		assertEquals(" ", panel.getResultText(), "the message starts out blank");
	}

	@Test
	void onEncryptText_bringsTheTextBack_whenItIsDecryptedAgain()
	{
		FileCryptPanel panel = new FileCryptPanel();
		String secret = "a text that must survive the round trip - äöüß";
		type(panel, "txtPlainText", secret);
		type(panel, "pwdText", PASSPHRASE);
		type(panel, "pwdTextRepeated", PASSPHRASE);

		click(panel, "btnEncryptText");

		String encrypted = textOf(panel, "txtEncryptedText");
		assertFalse(encrypted.isEmpty(), "the encrypted text is shown");
		assertFalse(encrypted.contains("survive"),
			"the encrypted text does not hold the plain one");
		assertEquals("encrypted " + secret.length() + " characters", panel.getResultText());

		type(panel, "txtPlainText", "");
		click(panel, "btnDecryptText");

		assertEquals(secret, textOf(panel, "txtPlainText"), "the text comes back as it was");
		assertEquals("decrypted " + secret.length() + " characters", panel.getResultText());
	}

	@Test
	void onEncryptText_refusesToEncrypt_whenTheRepeatedPassphraseDiffers()
	{
		FileCryptPanel panel = new FileCryptPanel();
		type(panel, "txtPlainText", "a text nobody gets to lose");
		type(panel, "pwdText", PASSPHRASE);
		type(panel, "pwdTextRepeated", PASSPHRASE + "-typo");

		click(panel, "btnEncryptText");

		assertEquals("not encrypted: the two passphrases are not the same", panel.getResultText());
		assertTrue(textOf(panel, "txtEncryptedText").isEmpty(), "nothing was encrypted");
	}

	@Test
	void onEncryptFile_bringsTheFileBack_whenItIsDecryptedAgain(@TempDir File directory)
		throws Exception
	{
		byte[] original = new byte[4096];
		new SecureRandom().nextBytes(original);
		File source = new File(directory, "round-trip.bin");
		Files.write(source.toPath(), original);
		File encrypted = new File(directory, "round-trip.mcenc");
		FileCryptPanel panel = new FileCryptPanel();
		type(panel, "txtSourceFile", source.getAbsolutePath());
		type(panel, "txtTargetFile", encrypted.getAbsolutePath());
		type(panel, "pwdFile", PASSPHRASE);
		type(panel, "pwdFileRepeated", PASSPHRASE);

		click(panel, "btnEncryptFile");

		assertTrue(panel.getResultText().startsWith("encrypted to round-trip.mcenc"),
			"the message names the written file, but was: " + panel.getResultText());
		assertEquals(encrypted.getAbsolutePath(), textOf(panel, "txtTargetFile"),
			"the written file is shown in the target field");

		File decrypted = new File(directory, "back-again.bin");
		type(panel, "txtSourceFile", encrypted.getAbsolutePath());
		type(panel, "txtTargetFile", decrypted.getAbsolutePath());
		type(panel, "pwdFile", PASSPHRASE);

		click(panel, "btnDecryptFile");

		assertEquals("decrypted to back-again.bin", panel.getResultText());
		assertArrayEquals(original, Files.readAllBytes(decrypted.toPath()),
			"the file comes back byte for byte");
	}

	@Test
	void onDecryptFile_saysWhatWentWrong_whenThePassphraseIsWrong(@TempDir File directory)
		throws Exception
	{
		File source = new File(directory, "wrong-passphrase.bin");
		Files.write(source.toPath(), "content".getBytes());
		FileCryptPanel panel = new FileCryptPanel();
		type(panel, "txtSourceFile", source.getAbsolutePath());
		type(panel, "pwdFile", PASSPHRASE);
		type(panel, "pwdFileRepeated", PASSPHRASE);
		click(panel, "btnEncryptFile");
		String encrypted = textOf(panel, "txtTargetFile");

		type(panel, "txtSourceFile", encrypted);
		type(panel, "txtTargetFile", new File(directory, "never-written.bin").getAbsolutePath());
		type(panel, "pwdFile", "a passphrase that was never used");

		click(panel, "btnDecryptFile");

		assertTrue(panel.getResultText().startsWith("not decrypted: "),
			"the message says what went wrong, but was: " + panel.getResultText());
	}

	private void type(Container panel, String name, String text)
	{
		Component component = find(panel, name);
		assertNotNull(component, "no component named " + name);
		((JTextComponent)component).setText(text);
	}

	private String textOf(Container panel, String name)
	{
		Component component = find(panel, name);
		assertNotNull(component, "no component named " + name);
		return component instanceof JPasswordField password
			? new String(password.getPassword())
			: ((JTextComponent)component).getText();
	}

	private void click(Container panel, String name)
	{
		Component component = find(panel, name);
		assertNotNull(component, "no component named " + name);
		((AbstractButton)component).doClick();
	}

	private Component find(Container container, String name)
	{
		for (Component component : container.getComponents())
		{
			if (name.equals(component.getName()))
			{
				return component;
			}
			if (component instanceof Container child)
			{
				Component found = find(child, name);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}
}
