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

import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;

/**
 * A vault decrypted, and its content could not be read: the password or key file was right, and the
 * file holds something this version cannot build. The sign-in says so and names the reason, instead
 * of calling the password or the key file wrong (#624, decided by the maintainer).
 * <p>
 * The reason never quotes the content: the only text taken from the failure is a type name, which
 * is no secret. Anything else is named by the kind of failure alone, because a parser's message can
 * carry a piece of the decrypted document.
 */
public class VaultContentUnreadableException extends IllegalArgumentException
{

	private static final long serialVersionUID = 1L;

	/** What could not be read, without any of the content */
	private final String reason;

	/**
	 * The failure of reading a decrypted vault
	 *
	 * @param cause
	 *            what the reader threw
	 */
	public VaultContentUnreadableException(final XStreamException cause)
	{
		this(reasonOf(cause), cause);
	}

	private VaultContentUnreadableException(final String reason, final XStreamException cause)
	{
		super("The database was decrypted, but its content could not be read: " + reason + ".",
			cause);
		this.reason = reason;
	}

	/**
	 * What could not be read
	 *
	 * @return the reason, without any of the content
	 */
	public String getReason()
	{
		return reason;
	}

	private static String reasonOf(final Throwable failure)
	{
		for (Throwable cause = failure; cause != null; cause = cause.getCause())
		{
			if (cause instanceof CannotResolveClassException unknown)
			{
				return "it names the type " + unknown.getMessage()
					+ ", which this version does not know";
			}
		}
		Throwable innermost = failure;
		while (innermost.getCause() != null)
		{
			innermost = innermost.getCause();
		}
		return "the reader failed with " + innermost.getClass().getSimpleName();
	}
}
