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
package io.github.astrapi69.mystic.crypt.panels.pw;

import io.github.astrapi69.random.RandomCharacters;
import io.github.astrapi69.random.object.RandomStringFactory;

public class GeneratePasswordExtensions
{

	public static char[] generatePassword(GeneratePasswordModelBean modelObject)
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(modelObject.isLowercase() ? RandomCharacters.LOWCASECHARS : "")
			.append(modelObject.isUppercase() ? RandomCharacters.LOWCASECHARS.toUpperCase() : "")
			.append(modelObject.isDigits() ? RandomCharacters.NUMBERS : "")
			.append(modelObject.isUnderscore() ? RandomCharacters.UNDERSCORE_CHAR : "")
			.append(modelObject.isSpecial() ? RandomCharacters.SPECIALCHARS : "")
			.append(modelObject.isMoreSpecial() ? RandomCharacters.OTHER_SPECIALCHARS : "")
			.append(modelObject.isBrackets() ? RandomCharacters.BRACKETS_CHAR : "")
			.append(modelObject.isWhitespace() ? RandomCharacters.WHITE_SPACE_CHAR : "")
			.append(modelObject.isMinus() ? "-" : "").append(modelObject.isPlus() ? "+" : "");
		String chars = stringBuilder.toString().trim();
		String randomPassword = RandomStringFactory.newRandomLongString(chars,
			modelObject.getPasswordLength());
		return randomPassword.toCharArray();
	}
}
