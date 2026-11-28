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
package io.github.astrapi69.mystic.crypt.panel.keygen;

/**
 * The two texts of an {@link EnDecryptPanel}: the text to encrypt on the left, the encrypted text
 * on the right. Mutable, because the panel binds both text areas to its setters (#775: this
 * replaces silly-bean's {@code Pair}, which is archived).
 */
public class EnDecryptTexts
{

	private String leftContent;

	private String rightContent;

	/**
	 * Creates the two texts
	 *
	 * @param leftContent
	 *            the text to encrypt
	 * @param rightContent
	 *            the encrypted text
	 */
	public EnDecryptTexts(final String leftContent, final String rightContent)
	{
		this.leftContent = leftContent;
		this.rightContent = rightContent;
	}

	/**
	 * @return the text to encrypt
	 */
	public String getLeftContent()
	{
		return leftContent;
	}

	/**
	 * @param leftContent
	 *            the text to encrypt
	 */
	public void setLeftContent(final String leftContent)
	{
		this.leftContent = leftContent;
	}

	/**
	 * @return the encrypted text
	 */
	public String getRightContent()
	{
		return rightContent;
	}

	/**
	 * @param rightContent
	 *            the encrypted text
	 */
	public void setRightContent(final String rightContent)
	{
		this.rightContent = rightContent;
	}
}
