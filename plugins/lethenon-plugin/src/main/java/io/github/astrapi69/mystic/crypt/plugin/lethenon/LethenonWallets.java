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
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;

/**
 * Opens a wallet file the way every tool of this plugin does: a missing or unnamed file and a wrong
 * password are refused with the file's name and never with the password
 */
final class LethenonWallets
{

	private LethenonWallets()
	{
	}

	/**
	 * Opens a wallet file
	 *
	 * @param walletFile
	 *            the wallet file lethenon's command line wrote
	 * @param password
	 *            the file's password; read, not wiped - that stays the caller's job
	 * @return the wallet
	 * @throws IOException
	 *             when the file cannot be read
	 * @throws IllegalArgumentException
	 *             when no file is named, there is none, or the password does not open it
	 */
	static Wallet open(final Path walletFile, final char[] password) throws IOException
	{
		if (walletFile == null || walletFile.toString().isBlank())
		{
			throw new IllegalArgumentException("no wallet file was named: pick the file "
				+ "lethenon's 'wallet create' or 'wallet restore' wrote");
		}
		if (!Files.isRegularFile(walletFile))
		{
			throw new IllegalArgumentException(
				"there is no wallet file at " + walletFile.toAbsolutePath());
		}
		try
		{
			return WalletFile.read(walletFile, password);
		}
		catch (SecurityException refused)
		{
			throw new IllegalArgumentException(
				walletFile.toAbsolutePath()
					+ " does not open with this password, or it was changed after it was written",
				refused);
		}
	}
}
