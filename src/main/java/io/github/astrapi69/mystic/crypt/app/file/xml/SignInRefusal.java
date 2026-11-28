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

import io.github.astrapi69.mystic.crypt.Messages;

/**
 * What the sign-in shows and logs for a failure of the read or of taking over what it read.
 * <p>
 * A refusal of the reader is known by its type, {@link SignInRefusedException}, and shown with the
 * title and the dialog it carries, in the bundle's words. Any other failure - an exception of the
 * take-over, an error the read ended with - is shown in the bundle's words for a sign-in that could
 * not be completed (#1005). What goes into the log is the failure told by its types
 * ({@link ReaderFailureTrace}).
 */
public final class SignInRefusal
{

	private final String title;

	private final String html;

	private final Throwable toldByItsTypes;

	private SignInRefusal(final String title, final String html, final Throwable toldByItsTypes)
	{
		this.title = title;
		this.html = html;
		this.toldByItsTypes = toldByItsTypes;
	}

	/**
	 * The refusal for the given failure
	 *
	 * @param failure
	 *            what the read or the take-over ended with
	 * @return the refusal
	 */
	public static SignInRefusal of(final Exception failure)
	{
		if (failure instanceof SignInRefusedException refused)
		{
			return new SignInRefusal(refused.getTitle(), refused.getHtml(),
				ReaderFailureTrace.of(failure));
		}
		String notCompleted = Messages.getString("dialog.signin.failed.title",
			"Database not opened");
		return new SignInRefusal(notCompleted,
			asTheDialogShowsIt(notCompleted,
				Messages.getString("dialog.signin.failed.message",
					"The sign-in could not be completed. The database was not opened, and the "
						+ "file is unchanged.")),
			ReaderFailureTrace.of(failure));
	}

	/**
	 * The dialog of a refusal as the sign-in shows it: the title as its heading, then the text
	 *
	 * @param title
	 *            the title
	 * @param text
	 *            the text
	 * @return the html of the dialog
	 */
	static String asTheDialogShowsIt(final String title, final String text)
	{
		return "<html><body width='350'><h2>" + title + "</h2><p>" + text;
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
	 * The dialog's html
	 *
	 * @return the html
	 */
	public String getHtml()
	{
		return html;
	}

	/**
	 * The failure for the log, told by the type of each exception of its chain and the line a
	 * parser names, and with its frames
	 *
	 * @return the failure, told that way
	 */
	public Throwable getToldByItsTypes()
	{
		return toldByItsTypes;
	}
}
