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
package io.github.astrapi69.mystic.crypt.plugin.kem;

import javax.crypto.SecretKey;

/**
 * The state of {@link KeyExchangePanel}: what the user typed, what was loaded from a file, and what
 * the exchange produced. Every component of the panel is bound to a property here, so what a button
 * works with is what the model holds and not what a widget happens to contain.
 * <p>
 * The two secrets and the party are held as the objects they are rather than as text, because they
 * are never shown: what the panel displays of a secret is its fingerprint.
 */
public class KeyExchangePanelModel
{

	private String algorithm = KemSettingsContribution.exchangeAlgorithm();
	private String myPrivateKey = "";
	private String myPublicKey = "";
	private String handshakeIn = "";
	private String encryptedIn = "";
	private String messageReceived = "";
	private String theirPublicKey = "";
	private String handshakeOut = "";
	private String messageToSend = "";
	private String encryptedOut = "";
	private String resultText = " ";
	private transient KeyExchangeSupport.Party party;
	private transient SecretKey recipientSecret;
	private transient SecretKey senderSecret;

	public String getAlgorithm()
	{
		return algorithm;
	}

	public void setAlgorithm(String algorithm)
	{
		this.algorithm = algorithm;
	}

	public String getMyPrivateKey()
	{
		return myPrivateKey;
	}

	public void setMyPrivateKey(String myPrivateKey)
	{
		this.myPrivateKey = myPrivateKey;
	}

	public String getMyPublicKey()
	{
		return myPublicKey;
	}

	public void setMyPublicKey(String myPublicKey)
	{
		this.myPublicKey = myPublicKey;
	}

	public String getHandshakeIn()
	{
		return handshakeIn;
	}

	public void setHandshakeIn(String handshakeIn)
	{
		this.handshakeIn = handshakeIn;
	}

	public String getEncryptedIn()
	{
		return encryptedIn;
	}

	public void setEncryptedIn(String encryptedIn)
	{
		this.encryptedIn = encryptedIn;
	}

	public String getMessageReceived()
	{
		return messageReceived;
	}

	public void setMessageReceived(String messageReceived)
	{
		this.messageReceived = messageReceived;
	}

	public String getTheirPublicKey()
	{
		return theirPublicKey;
	}

	public void setTheirPublicKey(String theirPublicKey)
	{
		this.theirPublicKey = theirPublicKey;
	}

	public String getHandshakeOut()
	{
		return handshakeOut;
	}

	public void setHandshakeOut(String handshakeOut)
	{
		this.handshakeOut = handshakeOut;
	}

	public String getMessageToSend()
	{
		return messageToSend;
	}

	public void setMessageToSend(String messageToSend)
	{
		this.messageToSend = messageToSend;
	}

	public String getEncryptedOut()
	{
		return encryptedOut;
	}

	public void setEncryptedOut(String encryptedOut)
	{
		this.encryptedOut = encryptedOut;
	}

	public String getResultText()
	{
		return resultText;
	}

	public void setResultText(String resultText)
	{
		this.resultText = resultText;
	}

	public KeyExchangeSupport.Party getParty()
	{
		return party;
	}

	public void setParty(KeyExchangeSupport.Party party)
	{
		this.party = party;
	}

	public SecretKey getRecipientSecret()
	{
		return recipientSecret;
	}

	public void setRecipientSecret(SecretKey recipientSecret)
	{
		this.recipientSecret = recipientSecret;
	}

	public SecretKey getSenderSecret()
	{
		return senderSecret;
	}

	public void setSenderSecret(SecretKey senderSecret)
	{
		this.senderSecret = senderSecret;
	}
}
