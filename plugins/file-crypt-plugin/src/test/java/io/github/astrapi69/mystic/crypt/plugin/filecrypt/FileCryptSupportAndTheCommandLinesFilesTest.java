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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Security;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Stream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * What the file encryption window makes of the files the library's command line wrote (#877): a
 * file and a text {@code encrypt} sealed, and a file in the layout {@code encrypt} wrote first, all
 * three in {@code filecrypt/compat}, as written by the published mystic-crypt 13.10. Each opens;
 * with a wrong passphrase or an altered byte it does not, and nothing is written. What the window
 * writes stays its own format.
 */
class FileCryptSupportAndTheCommandLinesFilesTest
{

	/**
	 * The passphrase the checked-in files were sealed with. It cannot be made up per run like the
	 * other passphrases of these tests: the files are in the repository and were encrypted with it
	 * ({@code scripts/fixtures/write-file-crypt-formats.sh command-line}). They hold invented text
	 * and guard nothing else; the host keeps its fixture passphrases in {@code TestPasswords},
	 * which a plugin's tests do not see.
	 */
	static final String COMMAND_LINE_FILES_FIXTURE = "sealed-by-the-command-line-for-877";

	/** A checked-in file, what it holds, and whether the window gets it as a text */
	record Sealed(String name, String plaintext, boolean text)
	{
		@Override
		public String toString()
		{
			return name;
		}
	}

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	static Stream<Sealed> theCommandLinesFiles()
	{
		return Stream.of(
			new Sealed("written-by-encrypt.txt.mcenc", "a file the command line sealed\n", false),
			new Sealed("written-by-encrypt.text",
				"a text the command line sealed: ä ö ü ß", true),
			new Sealed("written-by-passphrase-cryptor.mcrypt",
				"a file sealed in the layout encrypt wrote first\n", false));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("theCommandLinesFiles")
	void theWindowOpensWhatTheCommandLineWrote(Sealed sealed, @TempDir File directory)
		throws Exception
	{
		String plaintext = open(sealed, fixtureBytes(sealed.name()), directory,
			new File(directory, "back.txt"), COMMAND_LINE_FILES_FIXTURE);

		assertEquals(sealed.plaintext(), plaintext);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("theCommandLinesFiles")
	void aWrongPassphraseIsRefusedAndNothingIsWritten(Sealed sealed, @TempDir File directory)
		throws Exception
	{
		File back = new File(directory, "back.txt");
		byte[] content = fixtureBytes(sealed.name());

		SecurityException refusal = assertThrows(SecurityException.class,
			() -> open(sealed, content, directory, back, "throwaway-" + UUID.randomUUID()));

		assertTrue(refusal.getMessage().contains("the passphrase is wrong or the data was altered"),
			refusal.getMessage());
		assertFalse(back.exists(), "nothing may be written for a file that did not open");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("theCommandLinesFiles")
	void anAlteredFileIsRefusedAndNothingIsWritten(Sealed sealed, @TempDir File directory)
		throws Exception
	{
		File back = new File(directory, "back.txt");
		byte[] content = fixtureBytes(sealed.name());
		content[content.length / 2] ^= 0x01;

		SecurityException refusal = assertThrows(SecurityException.class,
			() -> open(sealed, content, directory, back, COMMAND_LINE_FILES_FIXTURE));

		assertTrue(refusal.getMessage().contains("the passphrase is wrong or the data was altered"),
			refusal.getMessage());
		assertFalse(back.exists(), "nothing may be written for a file that did not open");
	}

	/** Reading more is not writing more: a file and a text still get the window's own marker */
	@Test
	void theWindowStillWritesItsOwnMarker(@TempDir File directory) throws Exception
	{
		File source = new File(directory, "plain.txt");
		Files.writeString(source.toPath(), "still the window's own format");

		File sealedFile = FileCryptSupport.encryptFile(source, null, COMMAND_LINE_FILES_FIXTURE);
		byte[] sealedText = Base64.getDecoder()
			.decode(FileCryptSupport.encryptText("and a text", COMMAND_LINE_FILES_FIXTURE));

		assertEquals("MCFILE1", marker(Files.readAllBytes(sealedFile.toPath())));
		assertEquals("MCFILE1", marker(sealedText));
	}

	private static String marker(final byte[] content)
	{
		return new String(content, 0, FileCryptSupport.MAGIC.length, StandardCharsets.US_ASCII);
	}

	/** Opens the content the way the window does for a file or a text, and returns the plaintext */
	private static String open(final Sealed sealed, final byte[] content, final File directory,
		final File back, final String passphrase) throws Exception
	{
		if (sealed.text())
		{
			return FileCryptSupport.decryptText(new String(content, StandardCharsets.US_ASCII),
				passphrase);
		}
		File source = new File(directory, sealed.name());
		Files.write(source.toPath(), content);
		File written = FileCryptSupport.decryptFile(source, back, passphrase);
		return Files.readString(written.toPath(), StandardCharsets.UTF_8);
	}

	private static byte[] fixtureBytes(final String name) throws IOException
	{
		try (InputStream in = FileCryptSupportAndTheCommandLinesFilesTest.class
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
