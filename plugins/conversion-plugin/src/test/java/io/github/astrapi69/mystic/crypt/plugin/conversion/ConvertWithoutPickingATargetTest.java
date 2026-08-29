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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;

import javax.swing.JButton;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.key.KeyType;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.model.BaseModel;

/**
 * The way this window is actually used: pick a DER file, press Convert. Nothing in the suite went
 * that way - every test set both files into the model first - and pressing Convert without having
 * picked an output file threw a NullPointerException the user never saw, which is the reported
 * "nothing happens".
 */
class ConvertWithoutPickingATargetTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
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

	private static File aPrivateKeyInDerForm(File directory, String name) throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);
		File derFile = new File(directory, name);
		PrivateKeyWriter.write(keyPair.getPrivate(), derFile);
		return derFile;
	}

	private static FileConversionPanel panelFor(File derFile, File pemFile)
	{
		return new FileConversionPanel(BaseModel.of(FileConversionModelBean.builder()
			.derFile(derFile).pemFile(pemFile).keyType(KeyType.PRIVATE_KEY).build()));
	}

	@Test
	@DisplayName("converting without having picked a target writes the pem next to the source")
	void convertingWithoutHavingPickedATargetWritesNextToTheSource(@TempDir File directory)
		throws Exception
	{
		File derFile = aPrivateKeyInDerForm(directory, "chosen.der");
		FileConversionPanel panel = panelFor(derFile, null);

		((JButton)componentNamed(panel, "btnConvert")).doClick();

		File expected = new File(directory, "chosen.pem");
		assertTrue(expected.exists(),
			"nothing was written: " + panel.getModelObject().getConsoleOutput());
		assertArrayEquals(PrivateKeyReader.readPrivateKey(derFile).getEncoded(),
			PrivateKeyReader.readPemPrivateKey(expected).getEncoded(),
			"what was written is not the key of the chosen file");
		assertTrue(panel.getModelObject().getConsoleOutput().contains("chosen.pem"),
			"the tool did not say where it wrote to");
	}

	@Test
	@DisplayName("a target that was picked is still the one that is written")
	void aTargetThatWasPickedIsStillTheOneThatIsWritten(@TempDir File directory) throws Exception
	{
		File derFile = aPrivateKeyInDerForm(directory, "source.der");
		File pemFile = new File(directory, "somewhere-else.pem");
		FileConversionPanel panel = panelFor(derFile, pemFile);

		((JButton)componentNamed(panel, "btnConvert")).doClick();

		assertTrue(pemFile.exists(),
			"the picked target was not written: " + panel.getModelObject().getConsoleOutput());
		assertFalse(new File(directory, "source.pem").exists(),
			"a second file was written next to the source although a target was picked");
	}

	@Test
	@DisplayName("converting without a source says so instead of throwing")
	void convertingWithoutASourceSaysSo(@TempDir File directory)
	{
		FileConversionPanel panel = panelFor(null, null);

		((JButton)componentNamed(panel, "btnConvert")).doClick();

		String console = panel.getModelObject().getConsoleOutput();
		assertTrue(console.toLowerCase().contains("no file") || console.toLowerCase().contains("choose"),
			"the tool did not say that no file was chosen, it said: " + console);
	}

}
