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

import java.awt.Component;
import java.io.IOException;
import java.nio.file.Path;

import io.github.astrapi69.lethenon.ChainRejected;

/**
 * What every window of this plugin does before it reads a chain file (#581): a file of lethenon
 * 0.5.0 or earlier is converted into a database only once the person agreed to it, after its chain
 * verified. No chain logic of its own: {@link ChainFiles} verifies and converts.
 */
final class ChainConversion
{

	private ChainConversion()
	{
	}

	/**
	 * Converts the chain file when it is one of the old format and the person agrees
	 *
	 * @param parent
	 *            the window that wants to read the file, may be null
	 * @param consent
	 *            whom to ask
	 * @param chainFile
	 *            the chain file
	 * @return true when the window may go on: the file needs no conversion, or was converted; false
	 *         when the person declined, and the file is as it was
	 * @throws IOException
	 *             when the file cannot be read or converted
	 * @throws IllegalArgumentException
	 *             when the bytes are no chain file
	 * @throws ChainRejected
	 *             when a chain of the old format does not verify; nobody is asked then
	 */
	static boolean convertedIfAgreed(final Component parent, final ChainConversionConsent consent,
		final Path chainFile) throws IOException
	{
		if (!ChainFiles.needsConversion(chainFile))
		{
			return true;
		}
		if (!consent.agrees(parent, chainFile))
		{
			return false;
		}
		ChainFiles.convert(chainFile);
		return true;
	}

	/**
	 * @return the result line of a window whose chain file the person did not want converted
	 */
	static String notConverted()
	{
		return LethenonMessages.getString("lethenon.result.not.converted",
			"nothing was done: the chain file was not converted, and this version reads it only "
				+ "converted");
	}
}
