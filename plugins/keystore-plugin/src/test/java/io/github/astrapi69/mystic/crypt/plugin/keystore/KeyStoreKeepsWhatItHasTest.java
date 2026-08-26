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

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.type.KeystoreType;

/**
 * The window must not throw away keys without being asked.
 * <p>
 * Two ways it used to: Create on a path that already held a key store replaced it with an empty
 * one, and adding an entry under an alias that was taken overwrote what was there. Both are
 * refused on the command line and were not in the window, which is the half most people use.
 */
class KeyStoreKeepsWhatItHasTest
{

	private static final String STORE_PASSWORD = "throwaway-" + java.util.UUID.randomUUID();

	private static final String ALIAS = "the-key-that-matters";

	@BeforeAll
	static void registerBouncyCastle()
	{
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

	private static KeyStorePanel panelPointingAt(File store)
	{
		KeyStorePanel panel = new KeyStorePanel();
		named(panel, "txtKeyStoreFile", JTextField.class).setText(store.getAbsolutePath());
		named(panel, "cmbType", JComboBox.class).setSelectedItem(KeystoreType.PKCS12);
		named(panel, "pwdStore", JPasswordField.class).setText(STORE_PASSWORD);
		return panel;
	}

	private static String resultOf(KeyStorePanel panel)
	{
		return named(panel, "lblResult", JLabel.class).getText();
	}

	private static void click(KeyStorePanel panel, String button)
	{
		named(panel, button, JButton.class).doClick();
	}

	/** Builds a store with one key pair in it, the way the window does */
	private static File storeHoldingOneKeyPair(File directory, String name)
	{
		File store = new File(directory, name);
		KeyStorePanel panel = panelPointingAt(store);
		click(panel, "btnCreate");
		named(panel, "txtAlias", JTextField.class).setText(ALIAS);
		named(panel, "txtDistinguishedName", JTextField.class).setText("CN=important");
		named(panel, "cmbKeyAlgorithm", JComboBox.class).setSelectedItem(KeyPairGeneratorAlgorithm.EC);
		click(panel, "btnAddKeyPair");
		return store;
	}

	/**
	 * Create on a file that already holds a key store must not replace it. Measured before the fix:
	 * one alias and 2526 bytes became no aliases and 103 bytes, with the result line reading
	 * "created".
	 */
	@Test
	@DisplayName("create refuses a file that already holds a key store")
	void createDoesNotEmptyAnExistingStore(@TempDir File directory) throws Exception
	{
		File store = storeHoldingOneKeyPair(directory, "already-there.p12");
		long sizeBefore = store.length();

		KeyStorePanel second = panelPointingAt(store);
		click(second, "btnCreate");

		assertTrue(resultOf(second).startsWith("not created:"), resultOf(second));
		assertTrue(resultOf(second).contains("already-there.p12"), resultOf(second));
		assertEquals(sizeBefore, store.length(), "the existing key store was written to");
		KeyStore reopened = KeyStoreSupport.open(store, KeystoreType.PKCS12, STORE_PASSWORD);
		assertTrue(reopened.containsAlias(ALIAS), "the key that was in the store is gone");
	}

	/**
	 * Adding a key pair under an alias that is taken must not replace what is there
	 */
	@Test
	@DisplayName("add key pair refuses an alias that is already taken")
	void addKeyPairDoesNotOverwriteATakenAlias(@TempDir File directory) throws Exception
	{
		File store = storeHoldingOneKeyPair(directory, "taken-alias.p12");
		String fingerprintBefore = KeyStoreSupport
			.details(KeyStoreSupport.open(store, KeystoreType.PKCS12, STORE_PASSWORD), ALIAS)
			.fingerprint();

		KeyStorePanel panel = panelPointingAt(store);
		click(panel, "btnOpen");
		named(panel, "txtAlias", JTextField.class).setText(ALIAS);
		named(panel, "txtDistinguishedName", JTextField.class).setText("CN=the one that overwrites");
		click(panel, "btnAddKeyPair");

		assertTrue(resultOf(panel).startsWith("not added:"), resultOf(panel));
		assertTrue(resultOf(panel).contains(ALIAS), resultOf(panel));
		assertEquals(fingerprintBefore,
			KeyStoreSupport
				.details(KeyStoreSupport.open(store, KeystoreType.PKCS12, STORE_PASSWORD), ALIAS)
				.fingerprint(),
			"the entry under that alias was replaced");
	}

	/**
	 * The same guard has to hold for the secret key, which goes through the same field
	 */
	@Test
	@DisplayName("add secret key refuses an alias that is already taken")
	void addSecretKeyDoesNotOverwriteATakenAlias(@TempDir File directory) throws Exception
	{
		File store = storeHoldingOneKeyPair(directory, "taken-for-secret.p12");

		KeyStorePanel panel = panelPointingAt(store);
		click(panel, "btnOpen");
		named(panel, "txtAlias", JTextField.class).setText(ALIAS);
		click(panel, "btnAddSecretKey");

		assertTrue(resultOf(panel).startsWith("not added:"), resultOf(panel));
		KeyStore reopened = KeyStoreSupport.open(store, KeystoreType.PKCS12, STORE_PASSWORD);
		assertTrue(reopened.entryInstanceOf(ALIAS, KeyStore.PrivateKeyEntry.class),
			"the key pair was replaced by a secret key");
	}

	/**
	 * Creating a store where there is nothing yet must still work, or the guard has taken the
	 * feature with it
	 */
	@Test
	@DisplayName("create still works where there is no file yet")
	void createStillWorksOnAFreePath(@TempDir File directory) throws Exception
	{
		File store = new File(directory, "fresh.p12");

		KeyStorePanel panel = panelPointingAt(store);
		click(panel, "btnCreate");

		assertEquals("created fresh.p12", resultOf(panel));
		assertEquals(0, KeyStoreSupport.open(store, KeystoreType.PKCS12, STORE_PASSWORD).size());
	}
}
