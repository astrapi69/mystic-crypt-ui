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

import io.github.astrapi69.file.create.model.FileInfo;

/**
 * The key file of a database as the file on the disk, found without creating anything (#937).
 * <p>
 * Opening and saving used to name the key file with {@code FileFactory.newFileQuietly}, which
 * creates a missing file and its missing directories. A key file that was not there - moved,
 * deleted, on a drive that is not connected - so turned into an empty file at its path, the read
 * failed on that, and the real key file could not be put back without replacing it.
 */
final class KeyFileOnDisk
{

	private KeyFileOnDisk()
	{
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
	static File existing(final FileInfo keyFileInfo)
	{
		if (keyFileInfo == null)
		{
			throw new IllegalStateException("No key file is set for this database.");
		}
		File keyFile = keyFileInfo.toFile();
		if (!keyFile.isFile())
		{
			throw new IllegalStateException("The key file " + keyFile.getAbsolutePath()
				+ " is not there. It was moved or deleted, or it is on a drive that is not "
				+ "connected. Put it back at this path and try again.");
		}
		return keyFile;
	}
}
