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
 * The key file opened the key of a vault, and the content behind it is in no format this version
 * reads: neither the one it writes nor the one 8 wrote. The key file is right, so the sign-in says
 * that the format is unknown instead of calling the key file wrong (#650).
 * <p>
 * The reason names the kind of failure alone and never quotes any of the content.
 */
public class VaultFormatUnknownException extends IllegalArgumentException
{

	private static final long serialVersionUID = 1L;

	/** What failed, without any of the content */
	private final String reason;

	/**
	 * The failure of reading the content behind a key that the key file opened
	 *
	 * @param cause
	 *            what the last reader that was tried threw
	 */
	public VaultFormatUnknownException(final Exception cause)
	{
		this(reasonOf(cause), cause);
	}

	private VaultFormatUnknownException(final String reason, final Exception cause)
	{
		super("The key file opened the key of the database, but its content is in no known format: "
			+ reason + ".", cause);
		this.reason = reason;
	}

	/**
	 * What failed
	 *
	 * @return the reason, without any of the content
	 */
	public String getReason()
	{
		return reason;
	}

	private static String reasonOf(final Throwable failure)
	{
		Throwable innermost = failure;
		while (innermost.getCause() != null)
		{
			innermost = innermost.getCause();
		}
		return "the reader failed with " + innermost.getClass().getSimpleName();
	}
}
