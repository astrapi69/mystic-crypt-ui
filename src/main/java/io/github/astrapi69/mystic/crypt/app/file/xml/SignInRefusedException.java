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
package io.github.astrapi69.mystic.crypt.app.file.xml;

/**
 * The reader's refusal of a sign-in, with the title and the dialog the sign-in shows, both in the
 * bundle's words. The sign-in knows a refusal by this type ({@link SignInRefusal}); its message has
 * the form {@code title::html}. The cause is what opening threw.
 */
public class SignInRefusedException extends RuntimeException
{

	private static final long serialVersionUID = 1L;

	/** What separates the title from the dialog in the message */
	static final String SEPARATOR = "::";

	private final String title;

	private final String html;

	/**
	 * A refusal with the given title and dialog
	 *
	 * @param title
	 *            the title of the dialog
	 * @param html
	 *            the dialog
	 * @param cause
	 *            what opening threw
	 */
	SignInRefusedException(final String title, final String html, final Throwable cause)
	{
		super(title + SEPARATOR + html, cause);
		this.title = title;
		this.html = html;
	}

	/**
	 * The title of the dialog
	 *
	 * @return the title
	 */
	public String getTitle()
	{
		return title;
	}

	/**
	 * The dialog
	 *
	 * @return its html
	 */
	public String getHtml()
	{
		return html;
	}
}
