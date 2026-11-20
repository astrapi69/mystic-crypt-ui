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
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;

import io.github.astrapi69.file.read.ReadFileExtensions;

/**
 * Reads the text files the application keeps or shows: the remembered sign-in, the settings, the
 * KeePass memo and a PEM file (#1081).
 * <p>
 * Releases 6 to 8 wrote the remembered sign-in in the platform charset, which on JDK 11 to 17 is
 * the system's, for example Windows-1252 on a Western Windows, and a file from another tool or
 * edited by hand can carry any charset. file-worker 21 reads strictly, so such a file ended in a
 * {@link MalformedInputException} and the start died before the sign-in dialog. The text is read as
 * UTF-8 first and, when its bytes are not valid UTF-8, as ISO-8859-1, in which every byte is a
 * character, so the read never fails for an encoding reason. What writes the file next writes
 * UTF-8, so the file is UTF-8 from then on.
 * <p>
 * TODO(astrapi69/file-worker#164): use the library's method once file-worker has one
 */
public final class TextFiles
{

	private TextFiles()
	{
	}

	/**
	 * The text of the given file, read as UTF-8, or as ISO-8859-1 when its bytes are not valid
	 * UTF-8
	 *
	 * @param file
	 *            the file to read
	 * @return the text of the file
	 * @throws IOException
	 *             if the file cannot be read, for a reason other than its encoding
	 */
	public static String readUtf8OrLatin1(final File file) throws IOException
	{
		try
		{
			return ReadFileExtensions.fromFile(file);
		}
		catch (final MalformedInputException notUtf8)
		{
			return ReadFileExtensions.fromFile(file, StandardCharsets.ISO_8859_1);
		}
	}
}
