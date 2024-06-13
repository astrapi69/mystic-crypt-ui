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
package io.github.astrapi69.mystic.crypt.panel.signin;

/**
 * The enum class {@link PasswordType} provides the password types for signing in an application
 */
public enum PasswordType
{

	/**
	 * The <code>PasswordType#PASSWORD</code> type indicates that only a password is needed
	 */
	PASSWORD,

	/**
	 * The <code>PasswordType#PRIVATE_KEY</code> type indicates that only a private key is needed
	 */
	PRIVATE_KEY,

	/**
	 * The <code>PasswordType#PASSWORD_WITH_PRIVATE_KEY</code> type indicates that a password and a
	 * key is needed
	 */
	PASSWORD_WITH_PRIVATE_KEY,

	/**
	 * The <code>PasswordType#UNKNOWN</code> type indicates that the password type is unknown
	 */
	UNKNOWN;

	/**
	 * Resolves the {@link PasswordType} object from the given flags
	 *
	 * @param withPassword
	 *            the flag if it is with a password
	 * @param withPrivateKey
	 *            the flag if it is with a private key file
	 * @return the resolved {@link PasswordType} object
	 */
	public static PasswordType resolve(boolean withPassword, boolean withPrivateKey)
	{
		if (withPassword && withPrivateKey)
		{
			return PASSWORD_WITH_PRIVATE_KEY;
		}
		else if (withPassword)
		{
			return PASSWORD;
		}
		return PRIVATE_KEY;
	}

}
