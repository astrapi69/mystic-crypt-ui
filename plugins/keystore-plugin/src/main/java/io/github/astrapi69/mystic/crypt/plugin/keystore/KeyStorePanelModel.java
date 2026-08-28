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

import java.security.KeyStore;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.type.KeystoreType;

/**
 * Everything the {@link KeyStorePanel} holds: the key store file and its type, the store password,
 * the alias and the certificate data a new entry is built from, the two file paths for importing
 * and exporting, the key store that is currently open, the certificate the details view last
 * showed and the message the last operation left.
 * <p>
 * The panel binds its components to this object, so what the user typed or chose is readable here
 * at any moment - no button handler has to ask a widget for its content.
 */
public class KeyStorePanelModel
{

	/** The path of the key store file, as typed or picked */
	private String keyStoreFilePath = "";

	/** The type the key store is created with, or opened as when the file does not say */
	private KeystoreType keystoreType;

	/** The password of the key store; stays a character array, never a string */
	private char[] storePassword = new char[0];

	/** The alias an entry is added under, imported as or deleted by */
	private String alias = "";

	/** The subject a generated self-signed certificate is issued to */
	private String distinguishedName = "";

	/** The algorithm a new key pair is generated with */
	private KeyPairGeneratorAlgorithm keyAlgorithm;

	/** The path of the certificate file to import from or to export to */
	private String certificateFilePath = "";

	/** The path of the private key file to import from */
	private String privateKeyFilePath = "";

	/** The key store that is currently open, null as long as none was opened or created */
	private KeyStore keyStore;

	/** The certificate the details view last showed, null before one was shown */
	private KeyStoreSupport.CertificateDetails certificateDetails;

	/** The message the last operation left, empty before the first one */
	private String resultMessage = "";

	/**
	 * Gets the path of the key store file
	 *
	 * @return the path of the key store file
	 */
	public String getKeyStoreFilePath()
	{
		return keyStoreFilePath;
	}

	/**
	 * Sets the path of the key store file
	 *
	 * @param keyStoreFilePath
	 *            the path of the key store file
	 */
	public void setKeyStoreFilePath(final String keyStoreFilePath)
	{
		this.keyStoreFilePath = keyStoreFilePath;
	}

	/**
	 * Gets the key store type
	 *
	 * @return the key store type
	 */
	public KeystoreType getKeystoreType()
	{
		return keystoreType;
	}

	/**
	 * Sets the key store type
	 *
	 * @param keystoreType
	 *            the key store type
	 */
	public void setKeystoreType(final KeystoreType keystoreType)
	{
		this.keystoreType = keystoreType;
	}

	/**
	 * Gets the password of the key store
	 *
	 * @return the password of the key store
	 */
	public char[] getStorePassword()
	{
		return storePassword;
	}

	/**
	 * Sets the password of the key store
	 *
	 * @param storePassword
	 *            the password of the key store
	 */
	public void setStorePassword(final char[] storePassword)
	{
		this.storePassword = storePassword;
	}

	/**
	 * Gets the alias
	 *
	 * @return the alias
	 */
	public String getAlias()
	{
		return alias;
	}

	/**
	 * Sets the alias
	 *
	 * @param alias
	 *            the alias
	 */
	public void setAlias(final String alias)
	{
		this.alias = alias;
	}

	/**
	 * Gets the distinguished name
	 *
	 * @return the distinguished name
	 */
	public String getDistinguishedName()
	{
		return distinguishedName;
	}

	/**
	 * Sets the distinguished name
	 *
	 * @param distinguishedName
	 *            the distinguished name
	 */
	public void setDistinguishedName(final String distinguishedName)
	{
		this.distinguishedName = distinguishedName;
	}

	/**
	 * Gets the algorithm a new key pair is generated with
	 *
	 * @return the key algorithm
	 */
	public KeyPairGeneratorAlgorithm getKeyAlgorithm()
	{
		return keyAlgorithm;
	}

	/**
	 * Sets the algorithm a new key pair is generated with
	 *
	 * @param keyAlgorithm
	 *            the key algorithm
	 */
	public void setKeyAlgorithm(final KeyPairGeneratorAlgorithm keyAlgorithm)
	{
		this.keyAlgorithm = keyAlgorithm;
	}

	/**
	 * Gets the path of the certificate file
	 *
	 * @return the path of the certificate file
	 */
	public String getCertificateFilePath()
	{
		return certificateFilePath;
	}

	/**
	 * Sets the path of the certificate file
	 *
	 * @param certificateFilePath
	 *            the path of the certificate file
	 */
	public void setCertificateFilePath(final String certificateFilePath)
	{
		this.certificateFilePath = certificateFilePath;
	}

	/**
	 * Gets the path of the private key file
	 *
	 * @return the path of the private key file
	 */
	public String getPrivateKeyFilePath()
	{
		return privateKeyFilePath;
	}

	/**
	 * Sets the path of the private key file
	 *
	 * @param privateKeyFilePath
	 *            the path of the private key file
	 */
	public void setPrivateKeyFilePath(final String privateKeyFilePath)
	{
		this.privateKeyFilePath = privateKeyFilePath;
	}

	/**
	 * Gets the key store that is currently open
	 *
	 * @return the open key store, null when none is open
	 */
	public KeyStore getKeyStore()
	{
		return keyStore;
	}

	/**
	 * Sets the key store that is currently open
	 *
	 * @param keyStore
	 *            the open key store
	 */
	public void setKeyStore(final KeyStore keyStore)
	{
		this.keyStore = keyStore;
	}

	/**
	 * Gets the certificate the details view last showed
	 *
	 * @return the certificate details, null as long as none were shown
	 */
	public KeyStoreSupport.CertificateDetails getCertificateDetails()
	{
		return certificateDetails;
	}

	/**
	 * Sets the certificate the details view shows
	 *
	 * @param certificateDetails
	 *            the certificate details
	 */
	public void setCertificateDetails(final KeyStoreSupport.CertificateDetails certificateDetails)
	{
		this.certificateDetails = certificateDetails;
	}

	/**
	 * Gets the message the last operation left
	 *
	 * @return the message of the last operation
	 */
	public String getResultMessage()
	{
		return resultMessage;
	}

	/**
	 * Sets the message the last operation left
	 *
	 * @param resultMessage
	 *            the message of the last operation
	 */
	public void setResultMessage(final String resultMessage)
	{
		this.resultMessage = resultMessage;
	}
}
