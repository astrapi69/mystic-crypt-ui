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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * What {@code --cli decrypt} makes of the files the file encryption window wrote (#877): the
 * window's file and text in {@code src/test/resources/compat/file-crypt}, through the application's
 * own command line with the library it ships, as a user runs it. They open, and one that does not -
 * a wrong passphrase, an altered byte - is the command's negative answer, exit code 1, with nothing
 * written.
 */
class DecryptWhatTheWindowWroteTest
{

	/**
	 * What the window's file holds, as {@code scripts/fixtures/WriteFileCryptFormats.java} wrote it
	 */
	private static final String WINDOW_FILE = "a file the file encryption window sealed\n";

	/** What the window's text was */
	private static final String WINDOW_TEXT = "a text the file encryption window sealed: ä ö ü ß";

	/** The window's file */
	private static final String FILE = "written-by-the-window.txt.mcenc";

	/** The window's text, as the Base64 it shows */
	private static final String TEXT = "written-by-the-window.text";

	/**
	 * What the file of the published 8.2's window holds, as
	 * {@code scripts/fixtures/WriteFileCryptWithAnOldRelease.java} wrote it through that release's
	 * own window
	 */
	private static final String WINDOW_OF_8_2_FILE = "a file the file encryption window of 8.2 sealed\n";

	/** What the text of the published 8.2's window was */
	private static final String WINDOW_OF_8_2_TEXT = "a text the file encryption window of 8.2 sealed: ä ö ü ß";

	/** The file the window of the published 8.2 wrote */
	private static final String FILE_OF_8_2 = "written-by-the-window-8.2.txt.mcenc";

	/** The text the window of the published 8.2 wrote, as the Base64 it showed */
	private static final String TEXT_OF_8_2 = "written-by-the-window-8.2.text";

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
	void decryptOpensAFileTheWindowWrote(@TempDir File directory) throws IOException
	{
		File sealed = fixture(directory, "written-by-the-window.txt.mcenc", fixtureBytes(FILE));
		File back = new File(directory, "back.txt");

		Run run = run(TestPasswords.FILE_CRYPT_WINDOW_FIXTURE, "decrypt", "-i", sealed.getPath(),
			"-o", back.getPath(), "--passphrase-stdin");

		assertEquals(0, run.exitCode(), run.err());
		assertEquals(WINDOW_FILE, Files.readString(back.toPath(), StandardCharsets.UTF_8));
	}

	@Test
	void decryptOpensATextTheWindowWrote() throws IOException
	{
		Run run = run(TestPasswords.FILE_CRYPT_WINDOW_FIXTURE, "decrypt", "--text",
			fixtureText(fixtureBytes(TEXT)), "--passphrase-stdin");

		assertEquals(0, run.exitCode(), run.err());
		assertEquals(WINDOW_TEXT, run.out().strip());
	}

	/**
	 * A file the window of the published 8.2 wrote, the first release with the window, before #490
	 * moved it onto the library's envelope: a format once written stays readable (#877)
	 */
	@Test
	void decryptOpensAFileTheWindowOfEightTwoWrote(@TempDir File directory) throws IOException
	{
		File sealed = fixture(directory, FILE_OF_8_2, fixtureBytes(FILE_OF_8_2));
		File back = new File(directory, "back.txt");

		Run run = run(TestPasswords.FILE_CRYPT_WINDOW_FIXTURE, "decrypt", "-i", sealed.getPath(),
			"-o", back.getPath(), "--passphrase-stdin");

		assertEquals(0, run.exitCode(), run.err());
		assertEquals(WINDOW_OF_8_2_FILE, Files.readString(back.toPath(), StandardCharsets.UTF_8));
	}

	/** A text the window of the published 8.2 wrote */
	@Test
	void decryptOpensATextTheWindowOfEightTwoWrote() throws IOException
	{
		Run run = run(TestPasswords.FILE_CRYPT_WINDOW_FIXTURE, "decrypt", "--text",
			fixtureText(fixtureBytes(TEXT_OF_8_2)), "--passphrase-stdin");

		assertEquals(0, run.exitCode(), run.err());
		assertEquals(WINDOW_OF_8_2_TEXT, run.out().strip());
	}

	/** What is wrong with a window file handed to decrypt: the passphrase, or the bytes */
	record Damage(String description, String passphrase, UnaryOperator<byte[]> change) {
		@Override
		public String toString()
		{
			return description;
		}
	}

	static Stream<Damage> damages()
	{
		return Stream.of(
			new Damage("a wrong passphrase", TestPasswords.throwaway(), UnaryOperator.identity()),
			new Damage("a byte in the middle altered", TestPasswords.FILE_CRYPT_WINDOW_FIXTURE,
				content -> flipped(content, content.length / 2)),
			new Damage("the last byte altered", TestPasswords.FILE_CRYPT_WINDOW_FIXTURE,
				content -> flipped(content, content.length - 1)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("damages")
	void aWindowFileThatWillNotOpenIsTheNegativeAnswer(Damage damage, @TempDir File directory)
		throws IOException
	{
		File sealed = fixture(directory, "written-by-the-window.txt.mcenc",
			damage.change().apply(fixtureBytes(FILE)));
		File back = new File(directory, "back.txt");

		Run run = run(damage.passphrase(), "decrypt", "-i", sealed.getPath(), "-o", back.getPath(),
			"--passphrase-stdin");

		// exit code 1 is "it is a sealed file and it does not open", 2 is an error
		assertEquals(1, run.exitCode(), run.err());
		assertTrue(run.err().contains("the passphrase is wrong or the data was altered"),
			run.err());
		assertFalse(back.exists(), "nothing may be written for a file that did not open");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("damages")
	void aWindowTextThatWillNotOpenIsTheNegativeAnswer(Damage damage) throws IOException
	{
		Run run = run(damage.passphrase(), "decrypt", "--text",
			fixtureText(damage.change().apply(fixtureBytes(TEXT))), "--passphrase-stdin");

		assertEquals(1, run.exitCode(), run.err());
		assertTrue(run.err().contains("the passphrase is wrong or the data was altered"),
			run.err());
		assertTrue(run.out().isBlank(),
			"nothing may be printed for a text that did not open: " + run.out());
	}

	private static byte[] flipped(final byte[] content, final int position)
	{
		byte[] changed = content.clone();
		changed[position] ^= 0x01;
		return changed;
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

	/** A file the command line is pointed at, holding the given content */
	private static File fixture(final File directory, final String name, final byte[] content)
		throws IOException
	{
		File copy = new File(directory, name);
		Files.write(copy.toPath(), content);
		return copy;
	}

	/** The content as the window shows a text, Base64 */
	private static String fixtureText(final byte[] content)
	{
		return Base64.getEncoder().encodeToString(content);
	}

	/**
	 * The bytes of a checked-in file of the window; for the text, the bytes its Base64 stands for
	 */
	private static byte[] fixtureBytes(final String name) throws IOException
	{
		try (InputStream in = DecryptWhatTheWindowWroteTest.class
			.getResourceAsStream("/compat/file-crypt/" + name))
		{
			if (in == null)
			{
				throw new IOException("the fixture /compat/file-crypt/" + name + " is missing");
			}
			byte[] content = in.readAllBytes();
			return name.endsWith(".text")
				? Base64.getDecoder().decode(new String(content, StandardCharsets.US_ASCII).strip())
				: content;
		}
	}
}
