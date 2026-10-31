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
package io.github.astrapi69.mystic.crypt.panel.pw;

import io.github.astrapi69.mystic.crypt.random.SecureRandomValues;

/**
 * Generates a password from the character sets the user chose, drawn from
 * {@link java.security.SecureRandom} (#658). The sets are the ones the generator has always
 * offered.
 */
public class GeneratePasswordExtensions
{

	private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";

	private static final String DIGITS = "0123456789";

	private static final String UNDERSCORE = "_";

	private static final String SPECIAL = "#@$%^&*?!";

	private static final String MORE_SPECIAL = "\u00b0\u00a7=~.:,;\u00b5|\u20ac\u00b2\u00b3^";

	private static final String BRACKETS = "(){}[]<>";

	private static final String WHITESPACE = " ";

	/**
	 * Generates a password
	 *
	 * @param modelObject
	 *            the chosen character sets and the length
	 * @return the password, for the caller to overwrite when it is done with it
	 */
	public static char[] generatePassword(GeneratePasswordModelBean modelObject)
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(modelObject.isLowercase() ? LOWERCASE : "")
			.append(modelObject.isUppercase() ? LOWERCASE.toUpperCase() : "")
			.append(modelObject.isDigits() ? DIGITS : "")
			.append(modelObject.isUnderscore() ? UNDERSCORE : "")
			.append(modelObject.isSpecial() ? SPECIAL : "")
			.append(modelObject.isMoreSpecial() ? MORE_SPECIAL : "")
			.append(modelObject.isBrackets() ? BRACKETS : "")
			.append(modelObject.isWhitespace() ? WHITESPACE : "")
			.append(modelObject.isMinus() ? "-" : "").append(modelObject.isPlus() ? "+" : "");
		String chars = stringBuilder.toString().trim();
		return SecureRandomValues.characters(modelObject.getPasswordLength(), chars);
	}
}
