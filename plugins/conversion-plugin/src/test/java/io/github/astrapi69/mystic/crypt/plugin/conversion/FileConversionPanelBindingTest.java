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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextArea;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.key.KeyType;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.model.BaseModel;

/**
 * Tests that the components of {@link FileConversionPanel} are bound to its
 * {@link FileConversionModelBean}: the type picked in the combo box is in the model at once, the
 * convert button reads it there rather than from the widget, and the console text the tool writes
 * is in the model with it.
 * <p>
 * The proof is a real key on disk: a DER file written before the click, the PEM file the click
 * produces, and the key read back out of it.
 */
class FileConversionPanelBindingTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		// self-contained on purpose: the application registers the provider at startup, but a test
		// must never depend on another class having run first
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	/**
	 * The type chosen in the combo box is what the convert button works with: the model starts on
	 * the certificate, the combo box is set to the private key, and the PEM that appears holds the
	 * private key of the DER file. A button reading the widget would convert the same file, but a
	 * button reading a model the combo box never wrote into would try to read a certificate and
	 * write nothing.
	 */
	@Test
	void convertWritesThePemForTheKeyTypeChosenInTheComboBox(@TempDir File directory)
		throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);
		PrivateKey originalKey = keyPair.getPrivate();
		File derFile = new File(directory, "key.der");
		PrivateKeyWriter.write(originalKey, derFile);
		File pemFile = new File(directory, "key.pem");
		FileConversionPanel panel = panelFor(derFile, pemFile, KeyType.CERTIFICATE);

		comboBox(panel).setSelectedItem(KeyType.PRIVATE_KEY);

		assertEquals(KeyType.PRIVATE_KEY, panel.getModelObject().getKeyType(),
			"what is chosen in the combo box has to be in the model at once");

		button(panel, "btnConvert").doClick();

		assertTrue(pemFile.exists(),
			"the PEM was not written: " + panel.getModelObject().getConsoleOutput());
		assertArrayEquals(originalKey.getEncoded(),
			PrivateKeyReader.readPemPrivateKey(pemFile).getEncoded(),
			"the key read back from the produced PEM must equal the original");
	}

	/**
	 * What the console shows is in the model too, so the state of the panel is readable without
	 * reaching into the text area
	 */
	@Test
	void theConsoleTextTheConversionWritesIsInTheModel(@TempDir File directory) throws Exception
	{
		File derFile = new File(directory, "key.der");
		PrivateKeyWriter.write(
			KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048).getPrivate(), derFile);
		File pemFile = new File(directory, "key.pem");
		FileConversionPanel panel = panelFor(derFile, pemFile, KeyType.PRIVATE_KEY);

		assertEquals("", panel.getModelObject().getConsoleOutput(),
			"the panel starts with an empty console, in the model as on the screen");

		button(panel, "btnConvert").doClick();

		String consoleOutput = panel.getModelObject().getConsoleOutput();
		assertTrue(consoleOutput.contains("private key written to key.pem"),
			"the console messages belong in the model, it holds: " + consoleOutput);
		assertEquals(textArea(panel, "txtConsole").getText(), consoleOutput,
			"the model has to hold exactly what the console shows");
	}

	/**
	 * A type the tool cannot convert is reported and nothing is written - the edge of the switch
	 * the convert button drives off the model
	 */
	@Test
	void anUnknownKeyTypeChosenInTheComboBoxWritesNothing(@TempDir File directory) throws Exception
	{
		File derFile = new File(directory, "key.der");
		PrivateKeyWriter.write(
			KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048).getPrivate(), derFile);
		File pemFile = new File(directory, "key.pem");
		FileConversionPanel panel = panelFor(derFile, pemFile, KeyType.PRIVATE_KEY);

		comboBox(panel).setSelectedItem(KeyType.UNKNOWN);
		button(panel, "btnConvert").doClick();

		assertEquals(KeyType.UNKNOWN, panel.getModelObject().getKeyType(),
			"the chosen type has to be in the model");
		assertTrue(panel.getModelObject().getConsoleOutput().contains("unknown key type..."),
			"the tool has to say that it cannot convert this type");
		assertFalse(pemFile.exists(), "nothing may be written for a type the tool cannot convert");
	}

	/**
	 * The combo box is built over the enum, so it offers every key type there is - as a set,
	 * because {@link io.github.astrapi69.swing.model.combobox.EnumComboBoxModel} builds its list
	 * from a hash set and does not keep the order the enum declares
	 */
	@Test
	void theComboBoxOffersEveryKeyType(@TempDir File directory)
	{
		FileConversionPanel panel = panelFor(new File(directory, "key.der"),
			new File(directory, "key.pem"), KeyType.PRIVATE_KEY);

		List<Object> offered = itemsOf(comboBox(panel));
		assertEquals(KeyType.values().length, offered.size(), "no key type may be offered twice");
		assertEquals(Set.of(KeyType.values()), Set.copyOf(offered));
	}

	/**
	 * The panel starts on the type its model carries, in the combo box as in the model
	 */
	@Test
	void thePanelStartsOnTheKeyTypeItsModelCarries(@TempDir File directory)
	{
		FileConversionPanel panel = panelFor(new File(directory, "key.der"),
			new File(directory, "key.pem"), KeyType.PUBLIC_KEY);

		assertEquals(KeyType.PUBLIC_KEY, comboBox(panel).getSelectedItem());
		assertEquals(KeyType.PUBLIC_KEY, panel.getModelObject().getKeyType());
	}

	private FileConversionPanel panelFor(File derFile, File pemFile, KeyType keyType)
	{
		// the files are put into the model the way the file choosers put them there, so the click
		// on the convert button is the only thing this test drives through the user interface
		return new FileConversionPanel(BaseModel.of(FileConversionModelBean.builder()
			.derFile(derFile).pemFile(pemFile).keyType(keyType).build()));
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

	private JComboBox<?> comboBox(Container container)
	{
		return (JComboBox<?>)componentNamed(container, "cmbChooseType");
	}

	private JTextArea textArea(Container container, String name)
	{
		return (JTextArea)componentNamed(container, name);
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
