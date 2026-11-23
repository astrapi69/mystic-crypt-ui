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
package io.github.astrapi69.mystic.crypt.app.file;

import java.io.File;
import java.io.IOException;

/**
 * Key file paths could not be removed from a remembered file, and why: the file cannot be read, or
 * cannot be written. The reason is a value, so the user interface looks up its text in the language
 * of the user; {@link #getMessage()} is English and is for the log (#997).
 */
public class KeyFilePathsNotRemovedException extends IOException
{

	private static final long serialVersionUID = 1L;

	/** Why the paths stay in the file */
	public enum Reason
	{
		/** The file exists and cannot be read, so the paths in it cannot be named */
		UNREADABLE("the file cannot be read"),
		/** The file was read, and the cleaned copy cannot be written */
		NOT_WRITABLE("the file cannot be written");

		private final String logText;

		Reason(final String logText)
		{
			this.logText = logText;
		}

		String logText()
		{
			return logText;
		}
	}

	private final transient File file;

	private final Reason reason;

	/**
	 * @param file
	 *            the remembered file that still holds key file paths
	 * @param reason
	 *            why they could not be removed
	 * @param cause
	 *            the underlying failure, may be null
	 */
	public KeyFilePathsNotRemovedException(final File file, final Reason reason,
		final Throwable cause)
	{
		super(
			file + ": " + reason.logText() + (cause == null ? "" : " (" + cause.getMessage() + ")"),
			cause);
		this.file = file;
		this.reason = reason;
	}

	/** @return the remembered file that still holds key file paths */
	public File getFile()
	{
		return file;
	}

	/** @return why they could not be removed */
	public Reason getReason()
	{
		return reason;
	}
}
