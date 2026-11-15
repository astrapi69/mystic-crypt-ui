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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pf4j.util.FileUtils;

/**
 * How the plugins directory is unpacked when the application starts (#998): the installer puts a
 * plugin's zip into {@code ~/.config/mystic-crypt-ui/plugins} with the time it was built, and pf4j
 * unpacks a zip next to it only when the zip is newer than the directory unpacked before
 */
class PluginArchivesTest
{

	private static final String PLUGIN = "demo-plugin-1.0.0";

	@TempDir
	Path pluginsRoot;

	@Test
	@DisplayName("a directory younger than a zip with other content keeps the old content")
	void aDirectoryYoungerThanAChangedZip_keepsTheOldContent() throws IOException
	{
		Path zip = writeZip("version 1");
		unpackAsTheApplicationStarts();
		Path directory = pluginsRoot.resolve(PLUGIN);

		writeZip("version 2");
		makeOlderThan(zip, directory);
		unpackAsTheApplicationStarts();

		assertEquals("version 1", content(directory));
	}

	/** What the start of the application does with the plugins directory: pf4j's own unpacking */
	private void unpackAsTheApplicationStarts() throws IOException
	{
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
		}
		return zip;
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
}
