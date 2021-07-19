package io.github.astrapi69.mystic.crypt.panels.pw;

import io.github.astrapi69.random.RandomCharacters;
import io.github.astrapi69.random.object.RandomStringFactory;

public class GeneratePasswordExtensions
{

	public static char[] generatePassword(GeneratePasswordModelBean modelObject)
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder
			.append(modelObject.isLowercase() ? RandomCharacters.LOWCASECHARS : "")
			.append(modelObject.isUppercase() ? RandomCharacters.LOWCASECHARS.toUpperCase() : "")
			.append(modelObject.isDigits() ? RandomCharacters.NUMBERS : "")
			.append(modelObject.isUnderscore() ? RandomCharacters.UNDERSCORE_CHAR : "")
			.append(modelObject.isSpecial() ? RandomCharacters.SPECIALCHARS : "")
			.append(modelObject.isMoreSpecial() ? RandomCharacters.OTHER_SPECIALCHARS : "")
			.append(modelObject.isBrackets() ? RandomCharacters.BRACKETS_CHAR : "")
			.append(modelObject.isWhitespace() ? RandomCharacters.WHITE_SPACE_CHAR : "")
			.append(modelObject.isMinus() ? "-" : "")
			.append(modelObject.isPlus() ? "+" : "");
		String chars = stringBuilder.toString().trim();
		String randomPassword = RandomStringFactory.newRandomLongString(chars
			, modelObject.getPasswordLength());
		return randomPassword.toCharArray();
	}
}
