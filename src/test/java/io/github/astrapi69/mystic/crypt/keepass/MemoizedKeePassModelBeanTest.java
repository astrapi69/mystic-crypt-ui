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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.mystic.crypt.settings.MysticCryptSettings;

/**
 * The file the KeePass import and export dialogs remember their last paths in, written and read
 * through file-worker and gson-extensions: what is saved is what the next start loads, and a file
 * that cannot be read is a fresh bean, never an error in the dialog that asks for it.
 */
class MemoizedKeePassModelBeanTest
{

	@Test
	@DisplayName("the four remembered paths survive a save and a load, umlauts included")
	void saveThenLoad_returnsTheFourPaths(@TempDir final File configurationDirectory)
		throws IOException
	{
		MemoizedKeePassModelBean remembered = MemoizedKeePassModelBean.builder()
			.lastImportFilePath("/home/jürgen/Tresor/import.kdbx")
			.lastImportKeyFilePath("/home/jürgen/schlüssel/import.key")
			.lastExportFilePath("/home/jürgen/Tresor/export.kdbx")
			.lastExportKeyFilePath("/home/jürgen/schlüssel/export.key").build();

		remembered.save(configurationDirectory);

		File written = new File(configurationDirectory, MemoizedKeePassModelBean.JSON_FILENAME);
		assertTrue(Files.readString(written.toPath(), StandardCharsets.UTF_8)
			.contains("/home/jürgen/Tresor/import.kdbx"), "the file holds the path as UTF-8");
		assertEquals(remembered, MemoizedKeePassModelBean.load(configurationDirectory),
			"what was saved is what is loaded");
	}

	@Test
	@DisplayName("a second save replaces the first, it does not append to it")
	void aSecondSave_replacesTheFirst(@TempDir final File configurationDirectory)
	{
		MemoizedKeePassModelBean.builder().lastImportFilePath("/first/a-long-path/import.kdbx")
			.lastExportFilePath("/first/export.kdbx").build().save(configurationDirectory);
		MemoizedKeePassModelBean second = MemoizedKeePassModelBean.builder()
			.lastImportFilePath("/b.kdbx").build();

		second.save(configurationDirectory);

		assertEquals(second, MemoizedKeePassModelBean.load(configurationDirectory),
			"the shorter second content must not leave the end of the first one behind");
	}

	@Test
	@DisplayName("without a file, the bean is a fresh one with no path")
	void loadWithoutAFile_isAFreshBean(@TempDir final File configurationDirectory)
	{
		MemoizedKeePassModelBean loaded = MemoizedKeePassModelBean.load(configurationDirectory);

		assertNull(loaded.getLastImportFilePath());
		assertNull(loaded.getLastExportFilePath());
	}

	@Test
	@DisplayName("a file in Windows-1252 is read with its umlauts, and the next save writes it in UTF-8")
	void aFileInWindows1252_isReadWithItsUmlauts_andTheNextSaveWritesUtf8(
		@TempDir final File configurationDirectory) throws IOException
	{
		File file = new File(configurationDirectory, MemoizedKeePassModelBean.JSON_FILENAME);
		Files.write(file.toPath(), "{\"lastImportFilePath\":\"/home/J\u00fcrgen/a.kdbx\"}"
			.getBytes(Charset.forName("windows-1252")));

		MemoizedKeePassModelBean loaded = MemoizedKeePassModelBean.load(configurationDirectory);
		assertEquals("/home/J\u00fcrgen/a.kdbx", loaded.getLastImportFilePath(),
			"the remembered path is read, umlaut included");

		loaded.save(configurationDirectory);

		String writtenBack = StandardCharsets.UTF_8.newDecoder()
			.decode(ByteBuffer.wrap(Files.readAllBytes(file.toPath()))).toString();
		assertTrue(writtenBack.contains("/home/J\u00fcrgen/a.kdbx"),
			"the next save writes the file in UTF-8: " + writtenBack);
	}

	@Test
	@DisplayName("a memo file that cannot be read is kept aside by the next save, which writes the memo anew")
	void anUnreadableMemoFile_isKeptAsideByTheNextSave(@TempDir final File configurationDirectory)
		throws IOException
	{
		File file = new File(configurationDirectory, MemoizedKeePassModelBean.JSON_FILENAME);
		byte[] broken = "{\"lastImportFilePath\": ".getBytes(StandardCharsets.UTF_8);
		Files.write(file.toPath(), broken);
		java.util.List<io.github.astrapi69.mystic.crypt.app.file.UnreadableFileNotices.SetAside> told = new java.util.ArrayList<>();
		io.github.astrapi69.mystic.crypt.app.file.UnreadableFileNotices.listen(told::add);
		try
		{
			MemoizedKeePassModelBean memo = MemoizedKeePassModelBean.load(configurationDirectory);
			assertNull(memo.getLastImportFilePath(), "a fresh bean is used");
			memo.setLastImportFilePath("/home/user/new.kdbx");
			memo.save(configurationDirectory);
		}
		finally
		{
			io.github.astrapi69.mystic.crypt.app.file.UnreadableFileNotices.listen(null);
		}

		File[] keptAside = configurationDirectory.listFiles((directory, name) -> name
			.startsWith(MemoizedKeePassModelBean.JSON_FILENAME + ".unreadable-"));
		assertEquals(1, keptAside.length, "the old file is kept beside the new one");
		assertTrue(java.util.Arrays.equals(broken, Files.readAllBytes(keptAside[0].toPath())),
			"with its bytes unchanged");
		assertEquals("/home/user/new.kdbx",
			MemoizedKeePassModelBean.load(configurationDirectory).getLastImportFilePath(),
			"the new file reads back");
		assertEquals(1, told.size(), "the user is told once: " + told);
	}

	private static MemoizedKeePassModelBean withAllFourPaths()
	{
		return MemoizedKeePassModelBean.builder().lastImportFilePath("/vaults/import.kdbx")
			.lastImportKeyFilePath("/keys/import.key").lastExportFilePath("/vaults/export.kdbx")
			.lastExportKeyFilePath("/keys/export.key").build();
	}

	@Test
	@DisplayName("with 'Remember key files' off, a save writes the file paths and no key file path (#997)")
	void save_withRememberKeyFilesOff_writesNoKeyFilePath(
		@TempDir final File configurationDirectory) throws IOException
	{
		MysticCryptSettings notRemembering = new MysticCryptSettings();
		notRemembering.setRememberKeyFiles(false);
		notRemembering.save(configurationDirectory);

		withAllFourPaths().save(configurationDirectory);

		String text = Files.readString(
			new File(configurationDirectory, MemoizedKeePassModelBean.JSON_FILENAME).toPath(),
			StandardCharsets.UTF_8);
		assertFalse(text.contains(".key"), "no key file path in the file: " + text);
		MemoizedKeePassModelBean loaded = MemoizedKeePassModelBean.load(configurationDirectory);
		assertEquals("/vaults/import.kdbx", loaded.getLastImportFilePath());
		assertEquals("/vaults/export.kdbx", loaded.getLastExportFilePath());
		assertNull(loaded.getLastImportKeyFilePath());
		assertNull(loaded.getLastExportKeyFilePath());
	}

	@Test
	@DisplayName("with the setting on, which is the default, a save keeps the key file paths as before")
	void save_withRememberKeyFilesOn_keepsTheKeyFilePaths(
		@TempDir final File configurationDirectory)
	{
		withAllFourPaths().save(configurationDirectory);

		assertEquals(withAllFourPaths(), MemoizedKeePassModelBean.load(configurationDirectory));
	}

	@Test
	@DisplayName("forgetting key files removes the two key file paths from the stored file and keeps the file paths")
	void forgetKeyFiles_removesTheKeyFilePaths(@TempDir final File configurationDirectory)
		throws IOException
	{
		withAllFourPaths().save(configurationDirectory);

		assertTrue(MemoizedKeePassModelBean.forgetKeyFiles(configurationDirectory));

		String text = Files.readString(
			new File(configurationDirectory, MemoizedKeePassModelBean.JSON_FILENAME).toPath(),
			StandardCharsets.UTF_8);
		assertFalse(text.contains(".key"), "no key file path in the file: " + text);
		MemoizedKeePassModelBean loaded = MemoizedKeePassModelBean.load(configurationDirectory);
		assertEquals("/vaults/import.kdbx", loaded.getLastImportFilePath());
		assertEquals("/vaults/export.kdbx", loaded.getLastExportFilePath());
	}

	@Test
	@DisplayName("forgetting key files without a stored file writes nothing, and a file that cannot be read is reported")
	void forgetKeyFiles_withoutAFile_writesNothing_andReportsAnUnreadableOne(
		@TempDir final File configurationDirectory) throws IOException
	{
		assertFalse(MemoizedKeePassModelBean.forgetKeyFiles(configurationDirectory));
		File file = new File(configurationDirectory, MemoizedKeePassModelBean.JSON_FILENAME);
		assertFalse(file.exists());

		String unreadable = "{\"lastImportKeyFilePath\": \"/keys/import.key\", ";
		Files.writeString(file.toPath(), unreadable);
		IOException reported = assertThrows(IOException.class,
			() -> MemoizedKeePassModelBean.forgetKeyFiles(configurationDirectory));
		assertTrue(
			reported.getMessage().contains(file.getAbsolutePath())
				&& reported.getMessage().contains("cannot be read"),
			"the report names the file and the reason: " + reported.getMessage());
		assertEquals(unreadable, Files.readString(file.toPath()), "nothing was written over it");
	}
}
