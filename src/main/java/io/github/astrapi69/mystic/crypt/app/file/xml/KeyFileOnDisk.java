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

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;

import io.github.astrapi69.crypt.data.key.reader.PemObjectReader;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.Messages;

/**
 * The key file of a database as the file on the disk, found without creating anything (#937).
 * Opening, saving and unlocking find the key file here, so all three name a missing one with the
 * same words, and they read its key here, so the file that opens a database also saves and unlocks
 * it.
 * <p>
 * Opening and saving used to name the key file with {@code FileFactory.newFileQuietly}, which
 * creates a missing file and its missing directories. A key file that was not there - moved,
 * deleted, on a drive that is not connected - so turned into an empty file at its path, the read
 * failed on that, and the real key file could not be put back without replacing it.
 */
public final class KeyFileOnDisk
{

	private KeyFileOnDisk()
	{
	}

	/**
	 * Whether a database of the given kind can read its key from this file, the rule
	 * {@link #readKey(File, boolean)} reads by: a database with its key file alone takes PEM and
	 * DER, one with a master password and a key file PEM only. The dialogs that create and open a
	 * database offer a key file by it (#943)
	 *
	 * @param keyFile
	 *            the key file
	 * @param withMasterPassword
	 *            whether the database has a master password besides its key file
	 * @return true when a database of this kind takes the file
	 */
	public static boolean takes(final File keyFile, final boolean withMasterPassword)
	{
		if (!withMasterPassword)
		{
			return true;
		}
		try
		{
			return PemObjectReader.isPemObject(keyFile);
		}
		catch (IOException unreadable)
		{
			return false;
		}
	}

	/**
	 * Reads the key of a database from its key file, the one way opening, saving and unlocking read
	 * it (#937)
	 * <p>
	 * A database with its key file alone has always taken PEM and DER; one with a master password
	 * and a key file has taken PEM only. Both stay as they were: reading the key in one place is
	 * not where it is decided that the second takes DER as well.
	 *
	 * @param keyFile
	 *            the key file
	 * @param withMasterPassword
	 *            whether the database has a master password besides its key file
	 * @return the key
	 * @throws IllegalStateException
	 *             naming the path, when no key file is given or nothing is there
	 * @throws IOException
	 *             when the file cannot be read
	 * @throws GeneralSecurityException
	 *             when the file holds no key in a format this kind of database takes
	 */
	public static PrivateKey readKey(final File keyFile, final boolean withMasterPassword)
		throws IOException, GeneralSecurityException
	{
		File existingKeyFile = existing(keyFile);
		if (withMasterPassword || PemObjectReader.isPemObject(existingKeyFile))
		{
			return PrivateKeyReader.readPemPrivateKey(existingKeyFile);
		}
		return PrivateKeyReader.readPrivateKey(existingKeyFile);
	}

	/**
	 * Reads the key of a database from the key file the given information names, see
	 * {@link #readKey(File, boolean)}
	 *
	 * @param keyFileInfo
	 *            where the database's key file is
	 * @param withMasterPassword
	 *            whether the database has a master password besides its key file
	 * @return the key
	 * @throws IOException
	 *             when the file cannot be read
	 * @throws GeneralSecurityException
	 *             when the file holds no key in a format this kind of database takes
	 */
	public static PrivateKey readKey(final FileInfo keyFileInfo, final boolean withMasterPassword)
		throws IOException, GeneralSecurityException
	{
		return readKey(existing(keyFileInfo), withMasterPassword);
	}

	/**
	 * The key file the given information names, when it is a file on the disk
	 *
	 * @param keyFileInfo
	 *            where the database's key file is
	 * @return the key file
	 * @throws IllegalStateException
	 *             naming the path, when no key file is set or nothing is there
	 */
	public static File existing(final FileInfo keyFileInfo)
	{
		return existing(keyFileInfo == null ? null : keyFileInfo.toFile());
	}

	/**
	 * The given key file, when it is a file on the disk
	 *
	 * @param keyFile
	 *            the key file, as the unlock prompt names it
	 * @return the key file
	 * @throws IllegalStateException
	 *             naming the path, when no key file is given or nothing is there
	 */
	public static File existing(final File keyFile)
	{
		if (keyFile == null)
		{
			throw new IllegalStateException(Messages.getString("dialog.key.file.not.set.message",
				"No key file is set for this database."));
		}
		if (!keyFile.isFile())
		{
			// %1$s rather than {0}: the bundle lookup formats braces itself and fills them with
			// null
			throw new IllegalStateException(String.format(Messages.getString(
				"dialog.key.file.missing.message",
				"The key file %1$s is not there. It was moved or deleted, or it is on a drive that "
					+ "is not connected. Put it back at this path and try again."),
				keyFile.getAbsolutePath()));
		}
		return keyFile;
	}
}
