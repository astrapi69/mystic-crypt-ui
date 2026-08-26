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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import picocli.CommandLine;

/**
 * Tests of the {@code filecrypt} command, driven exactly the way a terminal drives it and checked
 * by its exit code and its output.
 */
class FileCryptCommandTest
{

	private static final String PASSPHRASE = "throwaway-" + java.util.UUID.randomUUID();

	private StringWriter out;

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private int execute(String... args)
	{
		out = new StringWriter();
		CommandLine commandLine = new CommandLine(new FileCryptCommand());
		commandLine.setOut(new PrintWriter(out));
		commandLine.setErr(new PrintWriter(new StringWriter()));
		return commandLine.execute(args);
	}

	private String output()
	{
		return out.toString();
	}

	@Test
	void encryptsAndDecryptsAFile(@TempDir File directory) throws Exception
	{
		File source = new File(directory, "notes.txt");
		Files.writeString(source.toPath(), "remember the milk");

		assertEquals(0, execute("encrypt", "-f", source.getAbsolutePath(), "-p", PASSPHRASE));
		File encrypted = new File(directory, "notes.txt.mcenc");
		assertTrue(encrypted.exists(), "was: " + output());
		assertTrue(output().contains("encrypted to"), output());

		Files.delete(source.toPath());
		assertEquals(0, execute("decrypt", "-f", encrypted.getAbsolutePath(), "-p", PASSPHRASE));

		assertEquals("remember the milk", Files.readString(source.toPath()),
			"the file must come back where it came from");
	}

	@Test
	void writesWhereItIsToldTo(@TempDir File directory) throws Exception
	{
		File source = new File(directory, "in.bin");
		Files.write(source.toPath(), new byte[] { 1, 2, 3 });
		File target = new File(directory, "elsewhere.enc");

		assertEquals(0, execute("encrypt", "-f", source.getAbsolutePath(), "-o",
			target.getAbsolutePath(), "-p", PASSPHRASE));

		assertTrue(target.exists());
	}

	@Test
	void encryptsAndDecryptsAText()
	{
		assertEquals(0, execute("encrypt", "-t", "a short note", "-p", PASSPHRASE));
		String encrypted = output().trim();
		assertTrue(encrypted.matches("[A-Za-z0-9+/=]+"), "was: " + encrypted);

		assertEquals(0, execute("decrypt", "-t", encrypted, "-p", PASSPHRASE));

		assertEquals("a short note", output().trim());
	}

	@ParameterizedTest
	@ValueSource(strings = { "encrypt", "decrypt" })
	void neitherFileNorTextIsRefused(String subcommand)
	{
		assertNotEquals(0, execute(subcommand, "-p", PASSPHRASE),
			"without anything to work on there is nothing to do");
	}

	@ParameterizedTest
	@ValueSource(strings = { "encrypt", "decrypt" })
	void bothFileAndTextIsRefused(String subcommand, @TempDir File directory) throws Exception
	{
		File source = new File(directory, "in.bin");
		Files.write(source.toPath(), new byte[] { 1 });

		assertNotEquals(0, execute(subcommand, "-f", source.getAbsolutePath(), "-t", "text", "-p",
			PASSPHRASE), "two inputs at once is a mistake worth reporting");
	}

	@Test
	void aWrongPassphraseEndsWithAFailingExitCode(@TempDir File directory) throws Exception
	{
		File source = new File(directory, "secret.bin");
		Files.write(source.toPath(), new byte[] { 9, 9, 9 });
		execute("encrypt", "-f", source.getAbsolutePath(), "-p", PASSPHRASE);

		assertNotEquals(0, execute("decrypt", "-f",
			new File(directory, "secret.bin.mcenc").getAbsolutePath(), "-o",
			new File(directory, "out.bin").getAbsolutePath(), "-p", "not the passphrase"));
	}

	@Test
	void aMissingPassphraseIsRefused(@TempDir File directory) throws Exception
	{
		File source = new File(directory, "in.bin");
		Files.write(source.toPath(), new byte[] { 1 });

		assertNotEquals(0, execute("encrypt", "-f", source.getAbsolutePath()),
			"the passphrase is what protects the result, it cannot be optional");
	}

	@Test
	void withoutASubcommandTheUsageIsPrinted()
	{
		assertEquals(0, execute());

		assertTrue(output().contains("encrypt") && output().contains("decrypt"), output());
	}

	@Test
	void theContributionOffersTheFileCryptCommand()
	{
		assertEquals("filecrypt",
			new CommandLine(new FileCryptCommandContribution().getCommands().get(0))
				.getCommandName());
	}
}
