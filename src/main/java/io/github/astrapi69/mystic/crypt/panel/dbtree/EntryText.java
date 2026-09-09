/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.panel.dbtree;

/**
 * The two conversions between an entry's text and a {@link String}, in one place so that every one
 * of them is visible (#294).
 * <p>
 * An entry's title, user name, url and notes are character arrays so that closing a vault can
 * overwrite them. Swing cannot be given a character array: {@code JTextField.getText()} returns a
 * String and {@code setText} takes one, and a table cell is rendered through {@code toString}. So
 * the text becomes a String at the screen and at the KeePass boundary, and nowhere else.
 * <p>
 * Those Strings are short lived and nothing holds on to them; the field on the entry, which is held
 * for as long as the vault is open, is the copy this exists to keep out of a String. Every call
 * here is therefore a boundary, and a call in a place that is not a boundary is a bug.
 */
public final class EntryText
{

	private EntryText()
	{
	}

	/**
	 * The given characters as text, for a component or a library that cannot take characters
	 *
	 * @param characters
	 *            the characters, may be null
	 * @return the text, or null if there were no characters
	 */
	public static String asText(final char[] characters)
	{
		return characters == null ? null : new String(characters);
	}

	/**
	 * The given text as characters, so that it can be overwritten later
	 *
	 * @param text
	 *            the text, may be null
	 * @return the characters, or null if there was no text
	 */
	public static char[] asCharacters(final String text)
	{
		return text == null ? null : text.toCharArray();
	}
}
