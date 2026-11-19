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
package io.github.astrapi69.mystic.crypt.plugin;

import java.io.IOException;

/**
 * A file chosen to be installed as a plugin that is no plugin zip. The file name and the reason are
 * kept apart so a dialog can put the sentence in the user's language and the reason, which is a
 * technical one, after it
 */
public class InvalidPluginZipException extends IOException
{

	private static final long serialVersionUID = 1L;

	private final String fileName;

	private final String reason;

	/**
	 * @param fileName
	 *            the name of the file
	 * @param reason
	 *            why it is no plugin zip
	 * @param cause
	 *            what made it known, or null
	 */
	public InvalidPluginZipException(final String fileName, final String reason,
		final Throwable cause)
	{
		super(fileName + " is not a plugin zip: " + reason, cause);
		this.fileName = fileName;
		this.reason = reason;
	}

	/** @return the name of the file */
	public String getFileName()
	{
		return fileName;
	}

	/** @return why it is no plugin zip */
	public String getReason()
	{
		return reason;
	}
}
