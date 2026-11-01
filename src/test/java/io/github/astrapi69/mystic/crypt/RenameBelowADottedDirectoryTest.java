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
package io.github.astrapi69.mystic.crypt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.file.rename.RenameFileExtensions;

/**
 * The file-worker and silly-io this application ships keep a renamed file in its directory below a
 * directory whose name holds a dot (#720): up to file-worker 19.1 with silly-io 3.6,
 * {@code changeFilenameSuffix} turned {@code ~/.cache/x/data} with {@code .bak} into
 * {@code ~/.bak}, out of its directory (astrapi69/file-worker#37, astrapi69/silly-io#9). The
 * application calls no such method today; this test runs on its runtime class path and fails as
 * soon as an old jar comes back, next to the floors of {@link RuntimeDependenciesTest}
 */
class RenameBelowADottedDirectoryTest
{

	@TempDir
	Path tempDir;

	@Test
	void changeFilenameSuffix_keepsTheFileInItsDirectory_belowADottedDirectory() throws Exception
	{
		Path directory = Files.createDirectories(tempDir.resolve(".cache").resolve("x"));
		Path file = Files.writeString(directory.resolve("data"), "content");

		assertTrue(RenameFileExtensions.changeFilenameSuffix(file.toFile(), ".bak", true));

		assertEquals("content", Files.readString(directory.resolve("data.bak")));
		assertFalse(Files.exists(file));
		try (var top = Files.list(tempDir))
		{
			assertEquals(1, top.count(), "nothing moved up into the temporary directory");
		}
	}
}
