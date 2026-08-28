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
package io.github.astrapi69.mystic.crypt.plugin.password;

/**
 * Everything the {@link PasswordHashPanel} holds: the algorithm a password is hashed with, the
 * password to hash, the hash that was produced or pasted in, the password a hash is checked
 * against, and the message the last operation left.
 * <p>
 * Every component of the panel is bound to one of these fields, so the state of the window can be
 * read here at any moment instead of out of the widgets when a button is pressed. The passwords
 * start as empty arrays and the hash as empty text rather than {@code null}: an untouched text
 * component holds empty content, and the binding would otherwise have to guess which of the two an
 * untouched field means.
 */
public class PasswordHashPanelModel
{

	/**
	 * The algorithm the password is hashed with, one of {@link PasswordHashSupport#algorithms()}
	 */
	private String algorithm = PasswordHashSupport.ARGON2ID;

	/** The password to hash; stays a character array, never a string */
	private char[] password = new char[0];

	/** The hash that was produced, or the one that was pasted in to be checked */
	private String hash = "";

	/** The password a hash is checked against; stays a character array, never a string */
	private char[] verifyPassword = new char[0];

	/** What the panel last said about what it did; a space keeps the label its height */
	private String resultMessage = " ";

	/**
	 * Gets the algorithm the password is hashed with
	 *
	 * @return the algorithm
	 */
	public String getAlgorithm()
	{
		return algorithm;
	}

	/**
	 * Sets the algorithm the password is hashed with
	 *
	 * @param algorithm
	 *            the algorithm
	 */
	public void setAlgorithm(final String algorithm)
	{
		this.algorithm = algorithm;
	}

	/**
	 * Gets the password to hash
	 *
	 * @return the password to hash
	 */
	public char[] getPassword()
	{
		return password;
	}

	/**
	 * Sets the password to hash
	 *
	 * @param password
	 *            the password to hash
	 */
	public void setPassword(final char[] password)
	{
		this.password = password;
	}

	/**
	 * Gets the hash that was produced or pasted in
	 *
	 * @return the hash
	 */
	public String getHash()
	{
		return hash;
	}

	/**
	 * Sets the hash that was produced or pasted in
	 *
	 * @param hash
	 *            the hash
	 */
	public void setHash(final String hash)
	{
		this.hash = hash;
	}

	/**
	 * Gets the password a hash is checked against
	 *
	 * @return the password a hash is checked against
	 */
	public char[] getVerifyPassword()
	{
		return verifyPassword;
	}

	/**
	 * Sets the password a hash is checked against
	 *
	 * @param verifyPassword
	 *            the password a hash is checked against
	 */
	public void setVerifyPassword(final char[] verifyPassword)
	{
		this.verifyPassword = verifyPassword;
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
