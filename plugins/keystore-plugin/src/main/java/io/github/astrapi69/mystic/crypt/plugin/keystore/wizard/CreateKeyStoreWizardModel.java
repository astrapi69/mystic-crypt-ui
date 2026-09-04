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

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.type.KeystoreType;
import io.github.astrapi69.mystic.crypt.plugin.keystore.KeyStoreSupport;

/**
 * The domain model the "Create Key Store..." wizard carries through its three steps: where the new
 * store is written, its type and password, and - if the user chose to add one - the first key pair.
 * <p>
 * This is the wizard's single source of truth - every step panel reads and writes it rather than
 * keeping its own copy, so the wizard's state is readable at any moment from outside the panels too
 * (tests included). It is a new, wizard-only model, deliberately not {@code KeyStorePanelModel}:
 * that one carries fields a creation wizard has no use for, such as the live {@link java.security.KeyStore}
 * object and the import/export file paths - the same flatter-than-the-dense-panel-needed
 * simplification {@code ConversionWizardModel} already made for the conversion wizard.
 */
public class CreateKeyStoreWizardModel
{

	/** The path of the new key store file, as typed or picked */
	private String keyStoreFilePath = "";

	/** The type the new key store is created as */
	private KeystoreType keystoreType = KeyStoreSupport.USABLE_TYPES.get(0);

	/** The password the new key store is protected with; stays a character array, never a string */
	private char[] storePassword = new char[0];

	/** The repeated password, typed a second time so a typo cannot lock the store immediately */
	private char[] storePasswordRepeated = new char[0];

	/** Whether the Entry step's fields apply - an unchecked box means an empty store is created */
	private boolean addKeyPairNow;

	/** The alias the first key pair is added under, when {@link #addKeyPairNow} is set */
	private String alias = "";

	/** The subject of the first key pair's self-signed certificate, for instance {@code CN=example.com} */
	private String distinguishedName = "";

	/** The algorithm the first key pair is generated with */
	private KeyPairGeneratorAlgorithm keyAlgorithm = KeyStoreSupport.KEY_ALGORITHMS.get(0);

	/**
	 * Gets the path of the new key store file
	 *
	 * @return the path of the new key store file
	 */
	public String getKeyStoreFilePath()
	{
		return keyStoreFilePath;
	}

	/**
	 * Sets the path of the new key store file
	 *
	 * @param keyStoreFilePath
	 *            the path of the new key store file
	 */
	public void setKeyStoreFilePath(final String keyStoreFilePath)
	{
		this.keyStoreFilePath = keyStoreFilePath;
	}

	/**
	 * Gets the type the new key store is created as
	 *
	 * @return the key store type
	 */
	public KeystoreType getKeystoreType()
	{
		return keystoreType;
	}

	/**
	 * Sets the type the new key store is created as
	 *
	 * @param keystoreType
	 *            the key store type
	 */
	public void setKeystoreType(final KeystoreType keystoreType)
	{
		this.keystoreType = keystoreType;
	}

	/**
	 * Gets the password the new key store is protected with
	 *
	 * @return the store password
	 */
	public char[] getStorePassword()
	{
		return storePassword;
	}

	/**
	 * Sets the password the new key store is protected with
	 *
	 * @param storePassword
	 *            the store password
	 */
	public void setStorePassword(final char[] storePassword)
	{
		this.storePassword = storePassword;
	}

	/**
	 * Gets the repeated password
	 *
	 * @return the repeated password
	 */
	public char[] getStorePasswordRepeated()
	{
		return storePasswordRepeated;
	}

	/**
	 * Sets the repeated password
	 *
	 * @param storePasswordRepeated
	 *            the repeated password
	 */
	public void setStorePasswordRepeated(final char[] storePasswordRepeated)
	{
		this.storePasswordRepeated = storePasswordRepeated;
	}

	/**
	 * Gets whether the Entry step's fields apply
	 *
	 * @return true if a key pair is added to the new store, false for an empty store
	 */
	public boolean isAddKeyPairNow()
	{
		return addKeyPairNow;
	}

	/**
	 * Sets whether the Entry step's fields apply
	 *
	 * @param addKeyPairNow
	 *            true to add a key pair to the new store, false for an empty store
	 */
	public void setAddKeyPairNow(final boolean addKeyPairNow)
	{
		this.addKeyPairNow = addKeyPairNow;
	}

	/**
	 * Gets the alias the first key pair is added under
	 *
	 * @return the alias
	 */
	public String getAlias()
	{
		return alias;
	}

	/**
	 * Sets the alias the first key pair is added under
	 *
	 * @param alias
	 *            the alias
	 */
	public void setAlias(final String alias)
	{
		this.alias = alias;
	}

	/**
	 * Gets the subject of the first key pair's self-signed certificate
	 *
	 * @return the distinguished name
	 */
	public String getDistinguishedName()
	{
		return distinguishedName;
	}

	/**
	 * Sets the subject of the first key pair's self-signed certificate
	 *
	 * @param distinguishedName
	 *            the distinguished name
	 */
	public void setDistinguishedName(final String distinguishedName)
	{
		this.distinguishedName = distinguishedName;
	}

	/**
	 * Gets the algorithm the first key pair is generated with
	 *
	 * @return the key algorithm
	 */
	public KeyPairGeneratorAlgorithm getKeyAlgorithm()
	{
		return keyAlgorithm;
	}

	/**
	 * Sets the algorithm the first key pair is generated with
	 *
	 * @param keyAlgorithm
	 *            the key algorithm
	 */
	public void setKeyAlgorithm(final KeyPairGeneratorAlgorithm keyAlgorithm)
	{
		this.keyAlgorithm = keyAlgorithm;
	}
}
