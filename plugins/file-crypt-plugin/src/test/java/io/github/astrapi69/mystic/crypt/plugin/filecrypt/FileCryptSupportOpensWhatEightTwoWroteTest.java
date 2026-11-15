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
package io.github.astrapi69.mystic.crypt.plugin.filecrypt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What today's file encryption window makes of a file and a text the window of the published 8.2
 * wrote, the first release with the window, before #490 moved it onto the library's envelope "byte
 * for byte the same layout": a format once written stays readable (#877). Written through that
 * release's own window ({@code scripts/fixtures/write-file-crypt-of-an-old-release.sh 8.2}), in
 * {@code filecrypt/compat}; the host's {@code DecryptWhatTheWindowWroteTest} opens the same two
 * files with {@code --cli decrypt}.
 */
class FileCryptSupportOpensWhatEightTwoWroteTest
{

	/**
	 * The passphrase the checked-in files were sealed with, the host's
	 * {@code TestPasswords.FILE_CRYPT_WINDOW_FIXTURE}, which a plugin's tests do not see. It cannot be
	 * made up per run: the files are in the repository and were encrypted with it. They hold
	 * invented text and guard nothing else.
	 */
	static final String WINDOW_FILES_FIXTURE = "sealed-by-the-window-for-877";

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@Test
	void theWindowOpensAFileTheWindowOfEightTwoWrote(@TempDir File directory) throws Exception
	{
		File source = new File(directory, "written-by-the-window-8.2.txt.mcenc");
		Files.write(source.toPath(), fixtureBytes("written-by-the-window-8.2.txt.mcenc"));

		File written = FileCryptSupport.decryptFile(source, new File(directory, "back.txt"),
			WINDOW_FILES_FIXTURE);

		assertEquals("a file the file encryption window of 8.2 sealed\n",
			Files.readString(written.toPath(), StandardCharsets.UTF_8));
	}

	@Test
	void theWindowOpensATextTheWindowOfEightTwoWrote() throws Exception
	{
		String text = new String(fixtureBytes("written-by-the-window-8.2.text"),
			StandardCharsets.US_ASCII);

		assertEquals("a text the file encryption window of 8.2 sealed: ä ö ü ß",
			FileCryptSupport.decryptText(text, WINDOW_FILES_FIXTURE));
	}

	private static byte[] fixtureBytes(final String name) throws IOException
	{
		try (InputStream in = FileCryptSupportOpensWhatEightTwoWroteTest.class
			.getResourceAsStream("/filecrypt/compat/" + name))
		{
			if (in == null)
			{
				throw new IOException("the fixture /filecrypt/compat/" + name + " is missing");
			}
			return in.readAllBytes();
		}
	}
}
