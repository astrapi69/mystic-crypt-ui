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
package io.github.astrapi69.mystic.crypt.panel.signin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.file.create.model.FileInfo;

/**
 * A remembered key file path preselects a key file only when it names one (#499).
 * <p>
 * A remembered {@code "selectedKeyFilePath": ""} ended the application before the sign-in dialog
 * appeared, on JDK 24 and later: since then the empty abstract pathname stands for the current
 * directory, so {@code new File("").exists()} is true, and {@code FileInfo.toFileInfo} failed on
 * its missing parent. The guard had held by accident, because the empty path used to not exist.
 */
class RememberedKeyFileSupportTest
{

	@ParameterizedTest(name = "[{index}] \"{0}\"")
	@NullSource
	@ValueSource(strings = { "", " ", "\t" })
	@DisplayName("a blank or missing remembered path is no key file")
	void aBlankPathIsNoKeyFile(final String rememberedPath)
	{
		assertEquals(Optional.empty(), RememberedKeyFileSupport.keyFileOf(rememberedPath));
	}

	@Test
	@DisplayName("a remembered path that names a directory is no key file")
	void aDirectoryIsNoKeyFile(@TempDir File directory)
	{
		assertEquals(Optional.empty(),
			RememberedKeyFileSupport.keyFileOf(directory.getAbsolutePath()));
	}

	@Test
	@DisplayName("a remembered path whose file is gone is no key file")
	void aMissingFileIsNoKeyFile(@TempDir File directory)
	{
		assertEquals(Optional.empty(),
			RememberedKeyFileSupport.keyFileOf(new File(directory, "gone.key").getAbsolutePath()));
	}

	@Test
	@DisplayName("a remembered path that names a file preselects that file")
	void anExistingFileIsTheKeyFile(@TempDir File directory) throws Exception
	{
		File keyFile = new File(directory, "remembered.key");
		Files.writeString(keyFile.toPath(), "a key file");

		Optional<FileInfo> preselected = RememberedKeyFileSupport
			.keyFileOf(keyFile.getAbsolutePath());

		assertTrue(preselected.isPresent());
		assertEquals(keyFile.getAbsoluteFile(), preselected.get().toFile().getAbsoluteFile());
	}
}
