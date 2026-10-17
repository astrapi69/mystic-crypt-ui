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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;

/**
 * A chain file of lethenon 0.5.0 or earlier that verifies and has not been converted into a
 * database: the plugin reads it only after the person agreed to the conversion (#581). A window that
 * reaches this has not asked first; the file is then left as it was.
 */
final class UnconvertedChainFile extends IOException
{

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * @param chainFile
	 *            the file
	 */
	UnconvertedChainFile(final Path chainFile)
	{
		super(chainFile.toAbsolutePath() + " is a chain file of lethenon 0.5.0 or earlier, as "
			+ "mystic-crypt-ui 8.9 and earlier wrote them, and is read only once it is converted, "
			+ "which this window did not ask for: nothing was changed");
	}
}
