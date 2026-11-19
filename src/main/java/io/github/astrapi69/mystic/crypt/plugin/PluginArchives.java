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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.pf4j.CompoundPluginDescriptorFinder;
import org.pf4j.ManifestPluginDescriptorFinder;
import org.pf4j.PluginDescriptor;
import org.pf4j.PropertiesPluginDescriptorFinder;
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
 * The old directory is first moved aside in one step, to a hidden name pf4j does not take for a
 * plugin, and deleted after the new one is unpacked. Where it cannot be moved - on Windows a jar of
 * a plugin that is loaded stays open - nothing changes: no half directory is left, the checksum is
 * not written, and the next start, which loads no plugin before this, unpacks the zip again. A
 * directory moved aside that could not be deleted is removed at the next start.
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

	/**
	 * What the name of a directory moved aside adds to the directory's name, after a leading dot
	 */
	static final String ASIDE_SUFFIX = ".replaced";

	private static final Logger LOGGER = Logger.getLogger(PluginArchives.class.getName());

	/** What became of a plugin zip */
	public enum Outcome
	{
		/** its directory holds what it holds already */
		UNCHANGED,
		/** it is unpacked again, and its checksum is recorded */
		UNPACKED,
		/** its directory is in use and stays as it was; the next start unpacks the zip */
		AFTER_RESTART
	}

	private PluginArchives()
	{
	}

	/**
	 * Removes what an earlier replacement left aside, then unpacks again every zip of the given
	 * plugins directory whose content is not the content its directory was unpacked from
	 *
	 * @param pluginsRoot
	 *            the plugins directory; nothing happens when it does not exist
	 * @throws IOException
	 *             if a zip cannot be read or a directory cannot be written
	 */
	public static void unpackChanged(final Path pluginsRoot) throws IOException
	{
		if (!Files.isDirectory(pluginsRoot))
		{
			return;
		}
		removeWhatWasMovedAside(pluginsRoot);
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
	 * @return what became of it
	 * @throws IOException
	 *             if the zip cannot be read or unpacked, or the checksum cannot be written
	 */
	public static Outcome unpackIfChanged(final Path zip) throws IOException
	{
		String checksum = checksumOf(zip);
		Path directory = directoryOf(zip);
		Path record = recordOf(directory);
		if (Files.isDirectory(directory) && Files.isRegularFile(record)
			&& checksum.equals(Files.readString(record, StandardCharsets.US_ASCII).strip()))
		{
			return Outcome.UNCHANGED;
		}
		Path aside = null;
		if (Files.exists(directory))
		{
			aside = asideOf(directory);
			try
			{
				Files.move(directory, aside);
			}
			catch (IOException inUse)
			{
				LOGGER.log(Level.WARNING, inUse, () -> directory + " is in use and stays as it is"
					+ " until the next start, which unpacks " + zip + ": " + inUse.getMessage());
				return Outcome.AFTER_RESTART;
			}
		}
		try
		{
			FileUtils.expandIfZip(zip);
		}
		catch (IOException unpacking)
		{
			putBack(directory, aside);
			throw unpacking;
		}
		Files.writeString(record, checksum + "\n", StandardCharsets.US_ASCII);
		if (aside != null)
		{
			removeQuietly(aside);
		}
		return Outcome.UNPACKED;
	}

	/**
	 * Reads the id of the plugin a zip holds, the way the plugin manager reads it (the properties
	 * and the manifest finders), without unpacking or changing anything: a zip that is no plugin
	 * zip is found out before the installed plugin is touched
	 *
	 * @param zip
	 *            the plugin zip
	 * @return the plugin id the zip's descriptor names
	 * @throws IOException
	 *             if the file is not a zip, or holds no plugin descriptor with an id
	 */
	public static String pluginIdOf(final Path zip) throws IOException
	{
		if (!Files.isRegularFile(zip))
		{
			throw new IOException(zip.getFileName() + " is not a file");
		}
		try (java.util.zip.ZipFile ignored = new java.util.zip.ZipFile(zip.toFile()))
		{
			PluginDescriptor descriptor = new CompoundPluginDescriptorFinder()
				.add(new PropertiesPluginDescriptorFinder())
				.add(new ManifestPluginDescriptorFinder()).find(zip);
			String pluginId = descriptor.getPluginId();
			if (pluginId == null || pluginId.isBlank())
			{
				throw new IOException(zip.getFileName() + " holds no plugin id");
			}
			return pluginId;
		}
		catch (java.util.zip.ZipException notAZip)
		{
			throw new IOException(
				zip.getFileName() + " is not a plugin zip: " + notAZip.getMessage(), notAZip);
		}
		catch (RuntimeException noDescriptor)
		{
			throw new IOException(
				zip.getFileName() + " is not a plugin zip: it holds no plugin descriptor ("
					+ noDescriptor.getMessage() + ")",
				noDescriptor);
		}
	}

	/**
	 * The directory a plugin zip is unpacked into, its name without {@code .zip}, as pf4j names it
	 *
	 * @param zip
	 *            the plugin zip
	 * @return the directory beside it
	 */
	public static Path directoryOf(final Path zip)
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

	/**
	 * A hidden name beside the directory, which pf4j does not take for a plugin and no other
	 * replacement uses
	 */
	private static Path asideOf(final Path directory)
	{
		return directory
			.resolveSibling("." + directory.getFileName() + ASIDE_SUFFIX + "-" + System.nanoTime());
	}

	/** Puts the old directory back after the new one could not be unpacked */
	private static void putBack(final Path directory, final Path aside) throws IOException
	{
		if (aside == null)
		{
			return;
		}
		if (Files.exists(directory))
		{
			DeleteFileExtensions.delete(directory);
		}
		Files.move(aside, directory);
	}

	private static void removeWhatWasMovedAside(final Path pluginsRoot) throws IOException
	{
		try (DirectoryStream<Path> leftovers = Files.newDirectoryStream(pluginsRoot,
			entry -> Files.isDirectory(entry) && entry.getFileName().toString().startsWith(".")
				&& entry.getFileName().toString().contains(ASIDE_SUFFIX + "-")))
		{
			for (Path leftover : leftovers)
			{
				removeQuietly(leftover);
			}
		}
	}

	/** Deletes a directory moved aside; one that is still in use is removed at the next start */
	private static void removeQuietly(final Path aside)
	{
		try
		{
			DeleteFileExtensions.delete(aside);
		}
		catch (IOException inUse)
		{
			LOGGER.log(Level.INFO, inUse,
				() -> aside + " is removed at the next start: " + inUse.getMessage());
		}
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
