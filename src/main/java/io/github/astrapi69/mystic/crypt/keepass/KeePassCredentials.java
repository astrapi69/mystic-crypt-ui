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
package io.github.astrapi69.mystic.crypt.keepass;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.linguafranca.pwdb.format.KdbxCredentials;

/**
 * The credentials of a KeePass file from what the user gave: a password, a key file, or both. The
 * import and the export build them here, and so do their tests, so a test cannot pass on a copy of
 * the logic that lacks a branch (#1039).
 * <p>
 * The password becomes UTF-8 bytes without a String in between, and those bytes are overwritten as
 * soon as the library has made its digest of them: {@link KdbxCredentials} keeps only that digest.
 * The caller's characters are left as they are
 */
public final class KeePassCredentials
{

	private KeePassCredentials()
	{
	}

	/**
	 * The credentials of a KeePass file
	 *
	 * @param password
	 *            the password, empty when the file has none
	 * @param keyFile
	 *            the key file, or {@code null} when the file has none
	 * @return the credentials: password and key file, the key file alone when the password is
	 *         empty, or the password alone when there is no key file
	 * @throws IOException
	 *             if the key file cannot be read
	 */
	public static KdbxCredentials of(final char[] password, final File keyFile) throws IOException
	{
		if (keyFile == null)
		{
			return withPassword(password, null);
		}
		try (InputStream keyFileStream = new FileInputStream(keyFile))
		{
			return password.length == 0
				? new KdbxCredentials(keyFileStream)
				: withPassword(password, keyFileStream);
		}
	}

	private static KdbxCredentials withPassword(final char[] password,
		final InputStream keyFileStream)
	{
		byte[] passwordBytes = utf8(password);
		try
		{
			return keyFileStream == null
				? new KdbxCredentials(passwordBytes)
				: new KdbxCredentials(passwordBytes, keyFileStream);
		}
		finally
		{
			Arrays.fill(passwordBytes, (byte)0);
		}
	}

	private static byte[] utf8(final char[] password)
	{
		ByteBuffer encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password));
		byte[] bytes = new byte[encoded.remaining()];
		encoded.get(bytes);
		if (encoded.hasArray())
		{
			Arrays.fill(encoded.array(), (byte)0);
		}
		return bytes;
	}
}
