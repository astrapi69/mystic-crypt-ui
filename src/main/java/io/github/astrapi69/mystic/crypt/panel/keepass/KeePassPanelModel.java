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
package io.github.astrapi69.mystic.crypt.panel.keepass;

import java.io.File;

/**
 * Everything a KeePass credentials form holds: the path of the {@code .kdbx} file, the password
 * that opens or protects it, whether a key file takes part and the path of that key file.
 * <p>
 * {@link ImportKeePassPanel} and {@link ExportKeePassPanel} ask the user for the same four things,
 * so they hold them in the same shape - each panel binds its own instance, and what the user picked
 * or typed is readable here at any moment, without asking a widget for its content.
 */
public class KeePassPanelModel
{

	/** The path of the KeePass file, as picked in the file chooser */
	private String filePath = "";

	/** The password of the KeePass file; stays a character array, never a string */
	private char[] password = new char[0];

	/** Whether a key file takes part in the credentials */
	private boolean useKeyFile;

	/** The path of the key file, empty as long as none takes part */
	private String keyFilePath = "";

	/**
	 * Gets the path of the KeePass file
	 *
	 * @return the path of the KeePass file
	 */
	public String getFilePath()
	{
		return filePath;
	}

	/**
	 * Sets the path of the KeePass file
	 *
	 * @param filePath
	 *            the path of the KeePass file
	 */
	public void setFilePath(final String filePath)
	{
		this.filePath = filePath;
	}

	/**
	 * Gets the password of the KeePass file
	 *
	 * @return the password of the KeePass file
	 */
	public char[] getPassword()
	{
		return password;
	}

	/**
	 * Sets the password of the KeePass file
	 *
	 * @param password
	 *            the password of the KeePass file
	 */
	public void setPassword(final char[] password)
	{
		this.password = password;
	}

	/**
	 * Gets whether a key file takes part in the credentials
	 *
	 * @return true if a key file takes part, otherwise false
	 */
	public boolean isUseKeyFile()
	{
		return useKeyFile;
	}

	/**
	 * Sets whether a key file takes part in the credentials
	 *
	 * @param useKeyFile
	 *            true if a key file takes part, otherwise false
	 */
	public void setUseKeyFile(final boolean useKeyFile)
	{
		this.useKeyFile = useKeyFile;
	}

	/**
	 * Gets the path of the key file
	 *
	 * @return the path of the key file
	 */
	public String getKeyFilePath()
	{
		return keyFilePath;
	}

	/**
	 * Sets the path of the key file
	 *
	 * @param keyFilePath
	 *            the path of the key file
	 */
	public void setKeyFilePath(final String keyFilePath)
	{
		this.keyFilePath = keyFilePath;
	}

	/**
	 * Gets the KeePass file the path points at
	 *
	 * @return the KeePass file, or null as long as no path is set
	 */
	public File getFile()
	{
		return toFile(filePath);
	}

	/**
	 * Gets the key file the key file path points at
	 *
	 * @return the key file, or null as long as no path is set
	 */
	public File getKeyFile()
	{
		return toFile(keyFilePath);
	}

	/**
	 * The file a path points at, or null when there is no path - an unset path is what "nothing
	 * picked" looks like in this form, and the callers act on the null, not on an empty string
	 *
	 * @param path
	 *            the path
	 * @return the file, or null when the path is empty
	 */
	private static File toFile(final String path)
	{
		return path == null || path.isBlank() ? null : new File(path);
	}
}
