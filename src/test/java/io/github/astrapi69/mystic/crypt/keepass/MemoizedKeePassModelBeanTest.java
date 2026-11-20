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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
	@DisplayName("a file that is not UTF-8 is a fresh bean, and a save after it replaces the file")
	void aFileThatIsNotUtf8_isAFreshBean_andTheNextSaveReplacesIt(
		@TempDir final File configurationDirectory) throws IOException
	{
		File file = new File(configurationDirectory, MemoizedKeePassModelBean.JSON_FILENAME);
		Files.write(file.toPath(), "{\"lastImportFilePath\":\"/home/Jürgen/a.kdbx\"}"
			.getBytes(StandardCharsets.ISO_8859_1));

		assertNull(MemoizedKeePassModelBean.load(configurationDirectory).getLastImportFilePath(),
			"file-worker 21 reads strictly: the bytes are not taken for text, the bean is fresh");

		MemoizedKeePassModelBean saved = MemoizedKeePassModelBean.builder()
			.lastImportFilePath("/home/jürgen/b.kdbx").build();
		saved.save(configurationDirectory);

		assertEquals(saved, MemoizedKeePassModelBean.load(configurationDirectory),
			"the dialog's next save writes a readable file over the unreadable one");
	}
}
