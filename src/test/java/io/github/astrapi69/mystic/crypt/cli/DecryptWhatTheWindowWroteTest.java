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
package io.github.astrapi69.mystic.crypt.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * What {@code --cli decrypt} makes of the files the file encryption window wrote (#877): the
 * window's file and text in {@code src/test/resources/compat/file-crypt}, through the application's
 * own command line with the library it ships, as a user runs it.
 */
class DecryptWhatTheWindowWroteTest
{

	/**
	 * What the window's file holds, as {@code scripts/fixtures/WriteFileCryptFormats.java} wrote it
	 */
	private static final String WINDOW_FILE = "a file the file encryption window sealed\n";

	/** What the window's text was */
	private static final String WINDOW_TEXT = "a text the file encryption window sealed: ä ö ü ß";

	private String originalPluginsDirectory;

	/** What a run of the command line ended with and printed */
	record Run(int exitCode, String out, String err) {
	}

	@BeforeEach
	void withoutAnyPlugin(@TempDir File emptyPluginsDirectory)
	{
		originalPluginsDirectory = System.getProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY);
		System.setProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY,
			emptyPluginsDirectory.getAbsolutePath());
	}

	@AfterEach
	void restoreThePluginsDirectory()
	{
		if (originalPluginsDirectory != null)
		{
			System.setProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY,
				originalPluginsDirectory);
		}
		else
		{
			System.clearProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY);
		}
	}

	@Test
	void decryptRefusesAFileTheWindowWrote(@TempDir File directory) throws IOException
	{
		File sealed = fixture(directory, "written-by-the-window.txt.mcenc");
		File back = new File(directory, "back.txt");

		Run run = run(TestPasswords.FILE_CRYPT_WINDOW_FIXTURE, "decrypt", "-i", sealed.getPath(),
			"-o", back.getPath(), "--passphrase-stdin");

		// read as an MCFILE, the window's MCFILE1 gives a negative iteration count
		assertEquals(2, run.exitCode(), run.err());
		assertFalse(back.exists(), "nothing may be written for a file that did not open");
	}

	@Test
	void decryptRefusesATextTheWindowWrote() throws IOException
	{
		Run run = run(TestPasswords.FILE_CRYPT_WINDOW_FIXTURE, "decrypt", "--text",
			fixtureText("written-by-the-window.text"), "--passphrase-stdin");

		assertEquals(2, run.exitCode(), run.err());
		assertFalse(run.out().contains(WINDOW_TEXT), run.out());
	}

	private static Run run(final String passphrase, final String... args)
	{
		InputStream originalIn = System.in;
		PrintStream originalOut = System.out;
		PrintStream originalErr = System.err;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		try
		{
			System.setIn(
				new ByteArrayInputStream((passphrase + "\n").getBytes(StandardCharsets.UTF_8)));
			System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
			System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
			int exitCode = MysticCryptUiCli.execute(args);
			return new Run(exitCode, out.toString(StandardCharsets.UTF_8),
				err.toString(StandardCharsets.UTF_8));
		}
		finally
		{
			System.setIn(originalIn);
			System.setOut(originalOut);
			System.setErr(originalErr);
		}
	}

	/** A copy of a checked-in file of the window, where the command line is pointed at it */
	private static File fixture(final File directory, final String name) throws IOException
	{
		File copy = new File(directory, name);
		Files.write(copy.toPath(), fixtureBytes(name));
		return copy;
	}

	private static String fixtureText(final String name) throws IOException
	{
		return new String(fixtureBytes(name), StandardCharsets.US_ASCII).strip();
	}

	private static byte[] fixtureBytes(final String name) throws IOException
	{
		try (InputStream in = DecryptWhatTheWindowWroteTest.class
			.getResourceAsStream("/compat/file-crypt/" + name))
		{
			if (in == null)
			{
				throw new IOException("the fixture /compat/file-crypt/" + name + " is missing");
			}
			return in.readAllBytes();
		}
	}
}
