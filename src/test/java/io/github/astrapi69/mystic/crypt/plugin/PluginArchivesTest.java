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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pf4j.DefaultPluginRepository;
import org.pf4j.util.FileUtils;

/**
 * How the plugins directory is unpacked when the application starts (#998): the installer puts a
 * plugin's zip into {@code ~/.config/mystic-crypt-ui/plugins} with the time it was built, and pf4j
 * unpacks a zip next to it only when the zip is newer than the directory unpacked before. The
 * application unpacks by content first ({@link PluginArchives}), then pf4j does its own part
 */
class PluginArchivesTest
{

	private static final String PLUGIN = "demo-plugin-1.0.0";

	/** A file that is in no zip: it is there as long as the directory is not unpacked again */
	private static final String SENTINEL = "not-from-the-zip.txt";

	@TempDir
	Path pluginsRoot;

	@Test
	@DisplayName("a directory younger than a zip with other content gets the new content")
	void aDirectoryYoungerThanAChangedZip_getsTheNewContent() throws Exception
	{
		Path zip = writeZip("version 1");
		unpackAsTheApplicationStarts();
		Path directory = pluginsRoot.resolve(PLUGIN);

		writeZip("version 2");
		makeOlderThan(zip, directory);
		unpackAsTheApplicationStarts();

		assertEquals("version 2", content(directory));
		assertEquals(sha256(zip), recorded(directory));
	}

	@Test
	@DisplayName("a zip with the content the directory was unpacked from is not unpacked again")
	void aZipWithTheSameContent_isNotUnpackedAgain() throws Exception
	{
		writeZip("version 1");
		unpackAsTheApplicationStarts();
		Path directory = pluginsRoot.resolve(PLUGIN);
		Files.writeString(directory.resolve(SENTINEL), "kept");

		unpackAsTheApplicationStarts();

		assertTrue(Files.exists(directory.resolve(SENTINEL)), "the directory was unpacked again");
		assertEquals("version 1", content(directory));
	}

	@Test
	@DisplayName("a directory without a recorded checksum, as 8.14 left it, is unpacked once again")
	void aDirectoryWithoutARecordedChecksum_isUnpackedOnceAgain() throws Exception
	{
		Path zip = writeZip("version 1");
		FileUtils.expandIfZip(zip);
		Path directory = pluginsRoot.resolve(PLUGIN);
		Files.writeString(directory.resolve(SENTINEL), "from 8.14");

		unpackAsTheApplicationStarts();

		assertFalse(Files.exists(directory.resolve(SENTINEL)),
			"the directory was not unpacked again");
		assertEquals(sha256(zip), recorded(directory));

		Files.writeString(directory.resolve(SENTINEL), "kept");
		unpackAsTheApplicationStarts();

		assertTrue(Files.exists(directory.resolve(SENTINEL)), "unpacked a second time");
	}

	@Test
	@DisplayName("pf4j takes the checksum file for no plugin")
	void theChecksumFile_isNoPluginForPf4j() throws Exception
	{
		writeZip("version 1");
		unpackAsTheApplicationStarts();

		List<Path> plugins = new DefaultPluginRepository(pluginsRoot).getPluginPaths();

		assertEquals(List.of(pluginsRoot.resolve(PLUGIN)), plugins);
	}

	@Test
	@DisplayName("a plugins directory that does not exist is left alone")
	void aMissingPluginsDirectory_isLeftAlone() throws IOException
	{
		Path missing = pluginsRoot.resolve("not-there");

		PluginArchives.unpackChanged(missing);

		assertFalse(Files.exists(missing));
	}

	@Test
	@DisplayName("a directory that cannot be moved aside stays as it was, and the next start unpacks the zip")
	void aDirectoryThatCannotBeMovedAside_staysAsItWas() throws Exception
	{
		assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"),
			"the plugins directory is made read-only through POSIX permissions");
		Path zip = writeZip("version 1");
		unpackAsTheApplicationStarts();
		Path directory = pluginsRoot.resolve(PLUGIN);
		String firstChecksum = sha256(zip);
		writeZip("version 2");
		Files.setPosixFilePermissions(pluginsRoot, PosixFilePermissions.fromString("r-xr-xr-x"));
		try
		{
			assumeFalse(Files.isWritable(pluginsRoot),
				"a user who may write anywhere cannot be refused");

			assertEquals(PluginArchives.Outcome.AFTER_RESTART, PluginArchives.unpackIfChanged(zip));

			assertEquals("version 1", content(directory));
			assertEquals(firstChecksum, recorded(directory),
				"the checksum of the new zip was written");
			assertEquals(List.of(), movedAside(), "something was left aside");
		}
		finally
		{
			Files.setPosixFilePermissions(pluginsRoot,
				PosixFilePermissions.fromString("rwxr-xr-x"));
		}

		unpackAsTheApplicationStarts();

		assertEquals("version 2", content(directory));
		assertEquals(sha256(zip), recorded(directory));
	}

	@Test
	@DisplayName("a jar of the directory held open leaves no half directory, and is replaced once closed")
	void aJarHeldOpen_leavesNoHalfDirectory() throws Exception
	{
		Path zip = writeZip("version 1");
		unpackAsTheApplicationStarts();
		Path directory = pluginsRoot.resolve(PLUGIN);
		String firstChecksum = sha256(zip);
		writeZip("version 2");
		PluginArchives.Outcome outcome;
		try (
			URLClassLoader loader = new URLClassLoader(
				new URL[] { directory.resolve("lib/held.jar").toUri().toURL() }, null);
			InputStream held = loader.getResourceAsStream("held.txt"))
		{
			held.read();
			outcome = PluginArchives.unpackIfChanged(zip);
			System.out.println("MEASURED #998 os.name=" + System.getProperty("os.name")
				+ " a jar of the directory held open by a class loader: " + outcome);

			if (outcome == PluginArchives.Outcome.AFTER_RESTART)
			{
				assertEquals("version 1", content(directory));
				assertEquals(firstChecksum, recorded(directory));
			}
			else
			{
				assertEquals(PluginArchives.Outcome.UNPACKED, outcome);
				assertEquals("version 2", content(directory));
				assertEquals(sha256(zip), recorded(directory));
			}
		}

		unpackAsTheApplicationStarts();

		assertEquals("version 2", content(directory));
		assertEquals(sha256(zip), recorded(directory));
		assertEquals(List.of(), movedAside(), "what was moved aside is still there");
	}

	@Test
	@DisplayName("what an earlier replacement left aside is removed at the next start")
	void whatWasMovedAside_isRemovedAtTheNextStart() throws Exception
	{
		Path leftover = pluginsRoot.resolve("." + PLUGIN + PluginArchives.ASIDE_SUFFIX + "-1");
		Files.createDirectories(leftover.resolve("classes"));
		Files.writeString(leftover.resolve("classes/version.txt"), "version 0");

		unpackAsTheApplicationStarts();

		assertFalse(Files.exists(leftover));
	}

	/** The directories moved aside that are still in the plugins directory */
	private List<Path> movedAside() throws IOException
	{
		try (var entries = Files.list(pluginsRoot))
		{
			return entries
				.filter(
					entry -> entry.getFileName().toString().contains(PluginArchives.ASIDE_SUFFIX))
				.toList();
		}
	}

	/**
	 * What the start of the application does with the plugins directory: it unpacks by content,
	 * then pf4j unpacks as it always does
	 */
	private void unpackAsTheApplicationStarts() throws IOException
	{
		PluginArchives.unpackChanged(pluginsRoot);
		try (DirectoryStream<Path> zips = Files.newDirectoryStream(pluginsRoot, "*.zip"))
		{
			for (Path zip : zips)
			{
				FileUtils.expandIfZip(zip);
			}
		}
	}

	private Path writeZip(final String version) throws IOException
	{
		Path zip = pluginsRoot.resolve(PLUGIN + ".zip");
		try (OutputStream out = Files.newOutputStream(zip);
			ZipOutputStream archive = new ZipOutputStream(out))
		{
			archive.putNextEntry(new ZipEntry("plugin.properties"));
			archive.write(
				("plugin.id=demo-plugin\nplugin.version=1.0.0\n").getBytes(StandardCharsets.UTF_8));
			archive.closeEntry();
			archive.putNextEntry(new ZipEntry("classes/version.txt"));
			archive.write(version.getBytes(StandardCharsets.UTF_8));
			archive.closeEntry();
			archive.putNextEntry(new ZipEntry("lib/held.jar"));
			archive.write(jarHolding(version));
			archive.closeEntry();
		}
		return zip;
	}

	/** A jar with one resource, the kind a plugin's lib directory holds */
	private static byte[] jarHolding(final String version) throws IOException
	{
		java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
		try (JarOutputStream jar = new JarOutputStream(bytes))
		{
			jar.putNextEntry(new JarEntry("held.txt"));
			jar.write(version.getBytes(StandardCharsets.UTF_8));
			jar.closeEntry();
		}
		return bytes.toByteArray();
	}

	/** Gives the zip a time before the directory's, as an installer gives it its build time */
	private static void makeOlderThan(final Path zip, final Path directory) throws IOException
	{
		FileTime directoryTime = Files.getLastModifiedTime(directory);
		Files.setLastModifiedTime(zip, FileTime.fromMillis(directoryTime.toMillis() - 60_000));
	}

	private static String content(final Path directory) throws IOException
	{
		return Files.readString(directory.resolve("classes/version.txt"), StandardCharsets.UTF_8);
	}

	private static String recorded(final Path directory) throws IOException
	{
		return Files.readString(
			directory.resolveSibling(directory.getFileName() + PluginArchives.CHECKSUM_SUFFIX),
			StandardCharsets.US_ASCII).strip();
	}

	private static String sha256(final Path file) throws IOException, NoSuchAlgorithmException
	{
		return HexFormat.of()
			.formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
	}
}
