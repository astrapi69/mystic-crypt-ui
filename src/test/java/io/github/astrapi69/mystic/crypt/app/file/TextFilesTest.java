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
package io.github.astrapi69.mystic.crypt.app.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link TextFiles#readUtf8OrLatin1(File)} reads UTF-8 as UTF-8 and every other byte sequence as
 * ISO-8859-1, so a file an older release wrote in Windows-1252 reads with its umlauts (#1081)
 */
class TextFilesTest
{

	private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

	@TempDir
	File directory;

	static Stream<Arguments> textsAndTheirBytes()
	{
		String path = "/home/Jürgen/Grüße.mcrdb";
		return Stream.of(
			Arguments.of("a path with umlauts in UTF-8", path,
				path.getBytes(StandardCharsets.UTF_8)),
			Arguments.of("the same path in Windows-1252", path, path.getBytes(WINDOWS_1252)),
			Arguments.of("plain ASCII", "/home/user/vault.mcrdb",
				"/home/user/vault.mcrdb".getBytes(StandardCharsets.US_ASCII)),
			Arguments.of("an empty file", "", new byte[0]));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("textsAndTheirBytes")
	@DisplayName("the text comes back as it was written")
	void aFile_readsBackAsItsText(final String what, final String text, final byte[] bytes)
		throws IOException
	{
		File file = new File(directory, "text.json");
		Files.write(file.toPath(), bytes);

		assertEquals(text, TextFiles.readUtf8OrLatin1(file), what);
	}

	@Test
	@DisplayName("bytes that are no UTF-8 at all read as one character per byte, none is lost")
	void everyByte_readsAsOneCharacter() throws IOException
	{
		byte[] everyByte = new byte[256];
		StringBuilder expected = new StringBuilder();
		for (int value = 0; value < everyByte.length; value++)
		{
			everyByte[value] = (byte)value;
			expected.append((char)value);
		}
		File file = new File(directory, "every-byte.bin");
		Files.write(file.toPath(), everyByte);

		assertEquals(expected.toString(), TextFiles.readUtf8OrLatin1(file));
	}

	@Test
	@DisplayName("a file that is not there is an error, not an empty text")
	void aMissingFile_isAnError()
	{
		assertThrows(IOException.class,
			() -> TextFiles.readUtf8OrLatin1(new File(directory, "missing.json")));
	}
}
