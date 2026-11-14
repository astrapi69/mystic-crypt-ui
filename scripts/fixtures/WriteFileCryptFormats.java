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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Security;
import java.util.Arrays;
import java.util.Scanner;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.github.astrapi69.mystic.crypt.cli.MysticCryptCli;
import io.github.astrapi69.mystic.crypt.plugin.filecrypt.FileCryptSupport;
import io.github.astrapi69.mystic.crypt.pw.PassphraseCryptor;

/**
 * Writes the files that the file encryption window and the mystic-crypt command line hand each
 * other, each one through the code that writes it for a user (#877).
 * <p>
 * {@code window}: the window's {@code FileCryptSupport.encryptFile} and {@code encryptText}, the
 * marker {@code MCFILE1}. {@code command-line}: {@code encrypt} of the library's command line, a
 * file and a text, the marker {@code MCFILE}, and {@code PassphraseCryptor.encrypt}, the layout
 * {@code MCRYPT} that {@code encrypt} wrote before it moved to {@code MCFILE} and that
 * {@code decrypt} still reads.
 * <p>
 * Usage:
 * {@code java -cp <window classes>:<mystic-crypt class path> WriteFileCryptFormats.java window|command-line <empty directory>},
 * the passphrase on standard input; {@code write-file-crypt-formats.sh} assembles the class path.
 * It prints each file it wrote with its size and its marker.
 */
public class WriteFileCryptFormats
{

	/** What the window's file holds */
	static final String WINDOW_FILE = "a file the file encryption window sealed\n";

	/** The window's text, with characters outside ASCII so that a wrong charset shows */
	static final String WINDOW_TEXT = "a text the file encryption window sealed: ä ö ü ß";

	/** What {@code encrypt}'s file holds */
	static final String COMMAND_LINE_FILE = "a file the command line sealed\n";

	/** {@code encrypt}'s text */
	static final String COMMAND_LINE_TEXT = "a text the command line sealed: ä ö ü ß";

	/** What the file in the old layout holds */
	static final String OLD_LAYOUT_FILE = "a file sealed in the layout encrypt wrote first\n";

	public static void main(final String[] args) throws Exception
	{
		if (args.length != 2 || !Arrays.asList("window", "command-line").contains(args[0]))
		{
			throw new IllegalArgumentException(
				"usage: WriteFileCryptFormats.java window|command-line <empty directory>");
		}
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
		File directory = new File(args[1]);
		String passphrase = new Scanner(System.in, StandardCharsets.UTF_8).nextLine();
		if ("window".equals(args[0]))
		{
			writeTheWindowsFiles(directory, passphrase);
		}
		else
		{
			writeTheCommandLinesFiles(directory, passphrase);
		}
	}

	private static void writeTheWindowsFiles(final File directory, final String passphrase)
		throws Exception
	{
		File source = plainFile(directory, "plain.txt", WINDOW_FILE);
		File sealed = FileCryptSupport.encryptFile(source,
			new File(directory, "written-by-the-window.txt.mcenc"), passphrase);
		Files.delete(source.toPath());
		report(sealed);
		File text = new File(directory, "written-by-the-window.text");
		Files.writeString(text.toPath(), FileCryptSupport.encryptText(WINDOW_TEXT, passphrase),
			StandardCharsets.US_ASCII);
		report(text);
	}

	private static void writeTheCommandLinesFiles(final File directory, final String passphrase)
		throws Exception
	{
		File source = plainFile(directory, "plain.txt", COMMAND_LINE_FILE);
		File sealed = new File(directory, "written-by-encrypt.txt.mcenc");
		encrypt(passphrase, "encrypt", "-i", source.getPath(), "-o", sealed.getPath(),
			"--passphrase-stdin");
		Files.delete(source.toPath());
		report(sealed);
		File text = new File(directory, "written-by-encrypt.text");
		Files.writeString(text.toPath(),
			encrypt(passphrase, "encrypt", "--text", COMMAND_LINE_TEXT, "--passphrase-stdin").strip(),
			StandardCharsets.US_ASCII);
		report(text);
		File oldLayout = new File(directory, "written-by-passphrase-cryptor.mcrypt");
		Files.write(oldLayout.toPath(), PassphraseCryptor.encrypt(passphrase.toCharArray(),
			OLD_LAYOUT_FILE.getBytes(StandardCharsets.UTF_8)));
		report(oldLayout);
	}

	/**
	 * Runs the library's root command the way {@code mystic-crypt} does, its {@code main} without
	 * the exit, with the passphrase as
	 * the first line of standard input, and returns what it printed
	 */
	private static String encrypt(final String passphrase, final String... args)
	{
		InputStream originalIn = System.in;
		PrintStream originalOut = System.out;
		ByteArrayOutputStream printed = new ByteArrayOutputStream();
		try
		{
			System.setIn(new ByteArrayInputStream(
				(passphrase + "\n").getBytes(StandardCharsets.UTF_8)));
			System.setOut(new PrintStream(printed, true, StandardCharsets.UTF_8));
			int exitCode = MysticCryptCli.execute(args);
			if (exitCode != 0)
			{
				throw new IllegalStateException(
					"encrypt ended with " + exitCode + ": " + printed.toString(StandardCharsets.UTF_8));
			}
		}
		finally
		{
			System.setIn(originalIn);
			System.setOut(originalOut);
		}
		return printed.toString(StandardCharsets.UTF_8);
	}

	private static File plainFile(final File directory, final String name, final String content)
		throws Exception
	{
		File file = new File(directory, name);
		Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
		return file;
	}

	private static void report(final File file) throws Exception
	{
		byte[] content = Files.readAllBytes(file.toPath());
		if (file.getName().endsWith(".text"))
		{
			content = java.util.Base64.getDecoder().decode(new String(content, StandardCharsets.US_ASCII));
		}
		System.out.println(file.getName() + ": " + content.length + " bytes, starts with "
			+ new String(content, 0, 7, StandardCharsets.US_ASCII).replaceAll("[^ -~]", "?"));
	}
}
