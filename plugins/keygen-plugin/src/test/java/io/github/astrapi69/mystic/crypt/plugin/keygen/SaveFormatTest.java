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
package io.github.astrapi69.mystic.crypt.plugin.keygen;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.Security;

import javax.swing.JComboBox;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.key.KeyFileFormat;
import io.github.astrapi69.crypt.api.key.KeySize;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;

/**
 * The window writes PEM unless the user asks for the binary encoding. The choice has to reach the
 * file, and the file ending has to say which of the two it is - a name that promises PEM over
 * binary content is refused by whatever it is handed to next.
 */
class SaveFormatTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private static GenerateKeysPanel panelWithAnRsaKeyPair()
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();
		panel.getCmbAlgorithm().setSelectedItem(KeyPairGeneratorAlgorithm.RSA);
		panel.getCryptographyPanel().getCmbKeySize().setSelectedItem(KeySize.KEYSIZE_1024);
		panel.getCryptographyPanel().getBtnGenerate().doClick();
		return panel;
	}

	@Test
	@DisplayName("the window starts on PEM")
	void theWindowStartsOnPem()
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();

		assertEquals(KeyFileFormat.PEM, panel.getModelObject().getSaveFormat());
		assertEquals(KeyFileFormat.PEM, panel.getCmbSaveFormat().getSelectedItem(),
			"the box does not show the format the window is set to");
	}

	@Test
	@DisplayName("what is chosen in the box is what the window saves in")
	void whatIsChosenInTheBoxIsWhatTheWindowSavesIn()
	{
		GenerateKeysPanel panel = new GenerateKeysPanel();

		((JComboBox<?>)panel.getCmbSaveFormat()).setSelectedItem(KeyFileFormat.DER);

		assertEquals(KeyFileFormat.DER, panel.getModelObject().getSaveFormat(),
			"the chosen format did not reach the model the save buttons read");
	}

	@Test
	@DisplayName("a private key saved as PEM is text, as DER it is not")
	void aPrivateKeySavedAsPemIsTextAsDerItIsNot(@TempDir File directory) throws Exception
	{
		GenerateKeysPanel panel = panelWithAnRsaKeyPair();
		File asPem = new File(directory, "key.pem");
		File asDer = new File(directory, "key.der");

		panel.getModelObject().setSaveFormat(KeyFileFormat.PEM);
		panel.savePrivateKeyTo(asPem);
		panel.getModelObject().setSaveFormat(KeyFileFormat.DER);
		panel.savePrivateKeyTo(asDer);

		assertTrue(Files.readString(asPem.toPath()).contains("PRIVATE KEY"),
			"the PEM file does not hold PEM");
		assertFalse(new String(Files.readAllBytes(asDer.toPath())).contains("PRIVATE KEY"),
			"the DER file holds PEM text");
		assertArrayEquals(panel.getModelObject().getPrivateKey().getEncoded(),
			PrivateKeyReader.readPrivateKey(asDer).getEncoded(),
			"the key read back from the DER file is not the key that was generated");
	}

	@Test
	@DisplayName("the file ending follows the chosen format")
	void theFileEndingFollowsTheChosenFormat()
	{
		assertEquals(".pem", KeygenSupport.endingFor(KeyFileFormat.PEM));
		assertEquals(".der", KeygenSupport.endingFor(KeyFileFormat.DER));
	}

}
