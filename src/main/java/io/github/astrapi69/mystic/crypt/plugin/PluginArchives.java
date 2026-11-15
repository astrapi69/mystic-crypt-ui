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
package io.github.astrapi69.mystic.crypt.plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import org.pf4j.util.FileUtils;

import io.github.astrapi69.checksum.FileChecksumExtensions;
import io.github.astrapi69.crypt.api.algorithm.HashAlgorithm;
import io.github.astrapi69.file.delete.DeleteFileExtensions;

/**
 * Unpacks the plugin zips of a plugins directory by their content, before pf4j loads them (#998).
 * <p>
 * pf4j unpacks a zip only when it is newer than the directory unpacked from it before
 * ({@code FileUtils.expandIfZip}), and the installer gives each zip the time it was built. A
 * directory younger than an updated zip therefore kept the plugin code of the release before, while
 * the zip next to it was the new one. Each unpacking here writes the zip's SHA-256 next to the
 * directory, into {@code <directory>.sha256}; a zip whose checksum is not the recorded one, or that
 * has none recorded (a directory unpacked by 8.14 or earlier), is unpacked again. pf4j then finds a
 * directory that holds what its zip holds, and its own unpacking leaves it alone.
 * <p>
 * Unpacking again loses nothing: no plugin keeps data of its own in its directory, every plugin
 * writes only to files the user chooses, and the plugin settings live in the configuration
 * directory (measured for 8.15 in #998). pf4j looks at the directories and jars of the plugins
 * directory only, so the checksum files beside them are not taken for plugins.
 */
public final class PluginArchives
{

	/** What the name of a directory's checksum file adds to the directory's name */
	static final String CHECKSUM_SUFFIX = ".sha256";

	private PluginArchives()
	{
	}

	/**
	 * Unpacks again every zip of the given plugins directory whose content is not the content its
	 * directory was unpacked from
	 *
	 * @param pluginsRoot
	 *            the plugins directory; nothing happens when it does not exist
	 * @throws IOException
	 *             if a zip cannot be read or a directory cannot be deleted or written
	 */
	public static void unpackChanged(final Path pluginsRoot) throws IOException
	{
		if (!Files.isDirectory(pluginsRoot))
		{
			return;
		}
		try (DirectoryStream<Path> zips = Files.newDirectoryStream(pluginsRoot, "*.zip"))
		{
			for (Path zip : zips)
			{
				unpackIfChanged(zip);
			}
		}
	}

	/**
	 * Unpacks the given plugin zip again unless the directory next to it was unpacked from this
	 * very content, and records the zip's checksum next to the directory
	 *
	 * @param zip
	 *            the plugin zip
	 * @return the directory the zip is unpacked into
	 * @throws IOException
	 *             if the zip cannot be read or the directory cannot be deleted or written
	 */
	public static Path unpackIfChanged(final Path zip) throws IOException
	{
		String checksum = checksumOf(zip);
		Path directory = directoryOf(zip);
		Path record = recordOf(directory);
		if (Files.isDirectory(directory) && Files.isRegularFile(record)
			&& checksum.equals(Files.readString(record, StandardCharsets.US_ASCII).strip()))
		{
			return directory;
		}
		if (Files.exists(directory))
		{
			DeleteFileExtensions.delete(directory);
		}
		Path unpacked = FileUtils.expandIfZip(zip);
		Files.writeString(record, checksum + "\n", StandardCharsets.US_ASCII);
		return unpacked;
	}

	/**
	 * The directory a plugin zip is unpacked into, its name without {@code .zip}, as pf4j names it
	 *
	 * @param zip
	 *            the plugin zip
	 * @return the directory beside it
	 */
	static Path directoryOf(final Path zip)
	{
		String name = zip.getFileName().toString();
		return zip.resolveSibling(name.substring(0, name.length() - ".zip".length()));
	}

	/**
	 * The file that records which zip content a directory was unpacked from
	 *
	 * @param directory
	 *            the unpacked directory
	 * @return the checksum file beside it
	 */
	static Path recordOf(final Path directory)
	{
		return directory.resolveSibling(directory.getFileName() + CHECKSUM_SUFFIX);
	}

	private static String checksumOf(final Path zip) throws IOException
	{
		try
		{
			return FileChecksumExtensions.getChecksum(zip.toFile(), HashAlgorithm.SHA_256);
		}
		catch (NoSuchAlgorithmException exception)
		{
			throw new IllegalStateException("this JVM offers no SHA-256 to check " + zip,
				exception);
		}
	}
}
