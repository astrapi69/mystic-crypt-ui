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
package io.github.astrapi69.mystic.crypt.plugin.signature;

import java.io.Serializable;
import java.security.KeyPair;

/**
 * The state of the {@link PqcSignaturePanel}: the chosen algorithm, what is signed - the typed
 * message or the named file - the private key file to sign with and the public key file to verify
 * against, the signature file to save to or load from, the public key and the signature the panel
 * shows, the result line at the bottom, and the key pair that was generated in the panel.
 * <p>
 * Every field is bound to the component that shows it, so the state of the panel can be read here
 * at any moment instead of out of the widgets.
 */
public class PqcSignaturePanelModel implements Serializable
{

	private static final long serialVersionUID = 1L;

	/** The signature algorithm to sign and verify with */
	private String algorithm = "";

	/** The text that is signed when no file is used */
	private String message = "";

	/** The path of the file that is signed when {@link #useFile} is set */
	private String dataFile = "";

	/** Whether the file is signed instead of the message */
	private boolean useFile;

	/** The path of the private key file to sign with */
	private String privateKeyFile = "";

	/** The path of the public key file or the certificate to verify against */
	private String publicKeyFile = "";

	/** The path of the file a signature is saved to and loaded from */
	private String signatureFile = "";

	/** The public key the panel shows, in PEM format */
	private String publicKey = "";

	/** The signature the panel shows, Base64 encoded */
	private String signature = "";

	/** The message shown at the bottom of the panel */
	private String resultText = "";

	/** The key pair generated in the panel, null as long as none was generated */
	private transient KeyPair keyPair;

	/**
	 * Gets the signature algorithm to sign and verify with
	 *
	 * @return the algorithm
	 */
	public String getAlgorithm()
	{
		return algorithm;
	}

	/**
	 * Sets the signature algorithm to sign and verify with
	 *
	 * @param algorithm
	 *            the algorithm
	 */
	public void setAlgorithm(final String algorithm)
	{
		this.algorithm = algorithm;
	}

	/**
	 * Gets the text that is signed when no file is used
	 *
	 * @return the message
	 */
	public String getMessage()
	{
		return message;
	}

	/**
	 * Sets the text that is signed when no file is used
	 *
	 * @param message
	 *            the message
	 */
	public void setMessage(final String message)
	{
		this.message = message;
	}

	/**
	 * Gets the path of the file that is signed
	 *
	 * @return the path of the file to sign
	 */
	public String getDataFile()
	{
		return dataFile;
	}

	/**
	 * Sets the path of the file that is signed
	 *
	 * @param dataFile
	 *            the path of the file to sign
	 */
	public void setDataFile(final String dataFile)
	{
		this.dataFile = dataFile;
	}

	/**
	 * Whether the file is signed instead of the message
	 *
	 * @return true if the file is signed
	 */
	public boolean isUseFile()
	{
		return useFile;
	}

	/**
	 * Sets whether the file is signed instead of the message
	 *
	 * @param useFile
	 *            true if the file is signed
	 */
	public void setUseFile(final boolean useFile)
	{
		this.useFile = useFile;
	}

	/**
	 * Gets the path of the private key file to sign with
	 *
	 * @return the path of the private key file
	 */
	public String getPrivateKeyFile()
	{
		return privateKeyFile;
	}

	/**
	 * Sets the path of the private key file to sign with
	 *
	 * @param privateKeyFile
	 *            the path of the private key file
	 */
	public void setPrivateKeyFile(final String privateKeyFile)
	{
		this.privateKeyFile = privateKeyFile;
	}

	/**
	 * Gets the path of the public key file or the certificate to verify against
	 *
	 * @return the path of the public key file
	 */
	public String getPublicKeyFile()
	{
		return publicKeyFile;
	}

	/**
	 * Sets the path of the public key file or the certificate to verify against
	 *
	 * @param publicKeyFile
	 *            the path of the public key file
	 */
	public void setPublicKeyFile(final String publicKeyFile)
	{
		this.publicKeyFile = publicKeyFile;
	}

	/**
	 * Gets the path of the file a signature is saved to and loaded from
	 *
	 * @return the path of the signature file
	 */
	public String getSignatureFile()
	{
		return signatureFile;
	}

	/**
	 * Sets the path of the file a signature is saved to and loaded from
	 *
	 * @param signatureFile
	 *            the path of the signature file
	 */
	public void setSignatureFile(final String signatureFile)
	{
		this.signatureFile = signatureFile;
	}

	/**
	 * Gets the public key the panel shows, in PEM format
	 *
	 * @return the public key
	 */
	public String getPublicKey()
	{
		return publicKey;
	}

	/**
	 * Sets the public key the panel shows, in PEM format
	 *
	 * @param publicKey
	 *            the public key
	 */
	public void setPublicKey(final String publicKey)
	{
		this.publicKey = publicKey;
	}

	/**
	 * Gets the signature the panel shows, Base64 encoded
	 *
	 * @return the signature
	 */
	public String getSignature()
	{
		return signature;
	}

	/**
	 * Sets the signature the panel shows, Base64 encoded
	 *
	 * @param signature
	 *            the signature
	 */
	public void setSignature(final String signature)
	{
		this.signature = signature;
	}

	/**
	 * Gets the message shown at the bottom of the panel
	 *
	 * @return the result text
	 */
	public String getResultText()
	{
		return resultText;
	}

	/**
	 * Sets the message shown at the bottom of the panel
	 *
	 * @param resultText
	 *            the result text
	 */
	public void setResultText(final String resultText)
	{
		this.resultText = resultText;
	}

	/**
	 * Gets the key pair generated in the panel
	 *
	 * @return the generated key pair or null when none was generated
	 */
	public KeyPair getKeyPair()
	{
		return keyPair;
	}

	/**
	 * Sets the key pair generated in the panel
	 *
	 * @param keyPair
	 *            the generated key pair
	 */
	public void setKeyPair(final KeyPair keyPair)
	{
		this.keyPair = keyPair;
	}
}
