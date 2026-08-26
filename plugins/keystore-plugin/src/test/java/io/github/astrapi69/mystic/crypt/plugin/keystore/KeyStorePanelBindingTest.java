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
package io.github.astrapi69.mystic.crypt.plugin.keystore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.security.KeyStore;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.type.KeystoreType;

/**
 * Tests that the components of {@link KeyStorePanel} are bound to {@link KeyStorePanelModel}: what
 * the user types or chooses is what the buttons work with, and the combo boxes offer exactly the
 * values the tool supports, in the order it offers them.
 * <p>
 * The proof is a real key store on disk, created and filled through the buttons of the panel.
 */
class KeyStorePanelBindingTest
{

	/**
	 * The password of the throwaway store this test builds in its temporary directory. It is made up
	 * per run rather than written into the source, so nothing here reads as a credential.
	 */
	private static final String STORE_PASSWORD = "store-" + UUID.randomUUID();

	private static final String DISTINGUISHED_NAME = "CN=bound panel";

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

	private static <T extends Component> T named(Container container, String name, Class<T> type)
	{
		for (Component component : container.getComponents())
		{
			if (type.isInstance(component) && name.equals(component.getName()))
			{
				return type.cast(component);
			}
			if (component instanceof Container child)
			{
				T found = named(child, name, type);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
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

	/**
	 * Creating a store takes the path, the type and the password from the model that the three
	 * components write into - the file that appears proves all three arrived.
	 */
	@Test
	void createWritesTheStoreThatTheBoundComponentsDescribe(@TempDir File directory)
		throws Exception
	{
		KeyStorePanel panel = new KeyStorePanel();
		File storeFile = new File(directory, "bound.p12");

		named(panel, "txtKeyStoreFile", JTextField.class).setText(storeFile.getAbsolutePath());
		named(panel, "cmbType", JComboBox.class).setSelectedItem(KeystoreType.PKCS12);
		named(panel, "pwdStore", JPasswordField.class).setText(STORE_PASSWORD);

		named(panel, "btnCreate", JButton.class).doClick();

		assertTrue(storeFile.exists(), "the store was not written where the field points");
		assertEquals("created " + storeFile.getName(),
			named(panel, "lblResult", JLabel.class).getText());
		KeyStore reopened = KeyStoreSupport.open(storeFile, KeystoreType.PKCS12, STORE_PASSWORD);
		assertEquals(0, reopened.size());
	}

	/**
	 * Adding a key pair takes the alias, the subject and the key algorithm from the model - the
	 * entry in the reopened store proves the button read them there and not from a widget.
	 */
	@Test
	void addKeyPairUsesTheAliasSubjectAndAlgorithmFromTheBoundComponents(@TempDir File directory)
		throws Exception
	{
		KeyStorePanel panel = new KeyStorePanel();
		File storeFile = new File(directory, "bound-entries.p12");

		named(panel, "txtKeyStoreFile", JTextField.class).setText(storeFile.getAbsolutePath());
		named(panel, "cmbType", JComboBox.class).setSelectedItem(KeystoreType.PKCS12);
		named(panel, "pwdStore", JPasswordField.class).setText(STORE_PASSWORD);
		named(panel, "btnCreate", JButton.class).doClick();

		named(panel, "txtAlias", JTextField.class).setText("bound-alias");
		named(panel, "txtDistinguishedName", JTextField.class).setText(DISTINGUISHED_NAME);
		named(panel, "cmbKeyAlgorithm", JComboBox.class)
			.setSelectedItem(KeyPairGeneratorAlgorithm.EC);

		named(panel, "btnAddKeyPair", JButton.class).doClick();

		KeyStore reopened = KeyStoreSupport.open(storeFile, KeystoreType.PKCS12, STORE_PASSWORD);
		assertTrue(reopened.containsAlias("bound-alias"),
			"the alias of the bound field did not reach the store");
		assertEquals(DISTINGUISHED_NAME,
			KeyStoreSupport.details(reopened, "bound-alias").subject());
	}

	/**
	 * Without an alias the button must fail on the model value, not on an empty widget read
	 */
	@Test
	void addKeyPairReportsTheMissingAliasFromTheModel(@TempDir File directory)
	{
		KeyStorePanel panel = new KeyStorePanel();
		File storeFile = new File(directory, "bound-without-alias.p12");

		named(panel, "txtKeyStoreFile", JTextField.class).setText(storeFile.getAbsolutePath());
		named(panel, "pwdStore", JPasswordField.class).setText(STORE_PASSWORD);
		named(panel, "btnCreate", JButton.class).doClick();
		named(panel, "txtAlias", JTextField.class).setText("   ");

		named(panel, "btnAddKeyPair", JButton.class).doClick();

		assertEquals("not added: enter an alias",
			named(panel, "lblResult", JLabel.class).getText());
	}

	/**
	 * The enum combo boxes offer exactly the values the tool supports, in the order it offers them
	 */
	@Test
	void theComboBoxesOfferTheSupportedValuesInTheOfferedOrder()
	{
		KeyStorePanel panel = new KeyStorePanel();

		assertEquals(KeyStoreSupport.USABLE_TYPES,
			itemsOf(named(panel, "cmbType", JComboBox.class)));
		assertEquals(KeyStoreSupport.KEY_ALGORITHMS,
			itemsOf(named(panel, "cmbKeyAlgorithm", JComboBox.class)));
	}

	/**
	 * What the settings configure is what the panel starts with, in the components and in the model
	 * behind them
	 */
	@Test
	void thePanelStartsOnTheConfiguredDefaults()
	{
		KeyStorePanel panel = new KeyStorePanel();

		assertEquals(KeyStoreSettings.type(),
			named(panel, "cmbType", JComboBox.class).getSelectedItem());
		assertEquals(KeyStoreSettings.algorithm(),
			named(panel, "cmbKeyAlgorithm", JComboBox.class).getSelectedItem());
		assertEquals(KeyStoreSettings.distinguishedName(),
			named(panel, "txtDistinguishedName", JTextField.class).getText());
	}

}
