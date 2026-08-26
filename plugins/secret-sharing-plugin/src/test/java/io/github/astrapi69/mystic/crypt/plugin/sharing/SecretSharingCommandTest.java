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
package io.github.astrapi69.mystic.crypt.plugin.sharing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Security;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import picocli.CommandLine;

/**
 * Tests of the {@code share} command, driven the way a terminal drives it.
 */
class SecretSharingCommandTest
{

	private static final String SECRET = "the master password of this database";

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
		CommandLine commandLine = new CommandLine(new SecretSharingCommand());
		commandLine.setOut(new PrintWriter(out));
		commandLine.setErr(new PrintWriter(new StringWriter()));
		return commandLine.execute(args);
	}

	private String output()
	{
		return out.toString();
	}

	@Test
	void splitsAndCombinesThroughTheCommandLine(@TempDir File directory) throws Exception
	{
		File sharesFile = new File(directory, "shares.txt");

		assertEquals(0, execute("split", "-s", SECRET, "-t", "3", "-n", "5", "-o",
			sharesFile.getAbsolutePath()));
		List<String> shares = Files.readAllLines(sharesFile.toPath(), StandardCharsets.UTF_8);
		assertEquals(5, shares.size(), output());

		// three of the five, given one at a time the way they would arrive
		assertEquals(0, execute("combine", "-s", shares.get(0), "-s", shares.get(2), "-s",
			shares.get(4)));

		assertEquals(SECRET, output().trim());
	}

	@Test
	void readsTheSharesFromAFileAndWritesTheSecretToOne(@TempDir File directory) throws Exception
	{
		File sharesFile = new File(directory, "shares.txt");
		execute("split", "-s", SECRET, "-t", "2", "-n", "3", "-o", sharesFile.getAbsolutePath());
		File secretFile = new File(directory, "secret.txt");

		assertEquals(0, execute("combine", "-f", sharesFile.getAbsolutePath(), "-o",
			secretFile.getAbsolutePath()));

		assertEquals(SECRET, Files.readString(secretFile.toPath(), StandardCharsets.UTF_8));
	}

	@Test
	void splitsAFile(@TempDir File directory) throws Exception
	{
		File keyFile = new File(directory, "key.bin");
		byte[] content = new byte[48];
		new java.security.SecureRandom().nextBytes(content);
		Files.write(keyFile.toPath(), content);
		File sharesFile = new File(directory, "shares.txt");
		File rebuilt = new File(directory, "rebuilt.bin");

		assertEquals(0, execute("split", "-f", keyFile.getAbsolutePath(), "-t", "2", "-n", "3", "-o",
			sharesFile.getAbsolutePath()));
		assertEquals(0, execute("combine", "-f", sharesFile.getAbsolutePath(), "-o",
			rebuilt.getAbsolutePath()));

		assertTrue(java.util.Arrays.equals(content, Files.readAllBytes(rebuilt.toPath())),
			"a key file has to come back byte for byte");
	}

	@Test
	void tooFewSharesEndWithAFailingExitCode(@TempDir File directory) throws Exception
	{
		File sharesFile = new File(directory, "shares.txt");
		execute("split", "-s", SECRET, "-t", "3", "-n", "5", "-o", sharesFile.getAbsolutePath());
		List<String> shares = Files.readAllLines(sharesFile.toPath(), StandardCharsets.UTF_8);

		assertNotEquals(0, execute("combine", "-s", shares.get(0), "-s", shares.get(1)));
	}

	@Test
	void neitherSecretNorFileIsRefused()
	{
		assertNotEquals(0, execute("split", "-t", "2", "-n", "3"));
	}

	@Test
	void nothingToCombineIsRefused()
	{
		assertNotEquals(0, execute("combine"));
	}

	@Test
	void anExistingFileIsNeverOverwritten(@TempDir File directory) throws Exception
	{
		File occupied = new File(directory, "occupied.txt");
		Files.writeString(occupied.toPath(), "something valuable");

		assertNotEquals(0, execute("split", "-s", SECRET, "-t", "2", "-n", "3", "-o",
			occupied.getAbsolutePath()));

		assertEquals("something valuable", Files.readString(occupied.toPath()));
	}

	@ParameterizedTest
	@ValueSource(strings = { "split", "combine" })
	void everySubcommandExplainsItself(String subcommand)
	{
		assertEquals(0, execute(subcommand, "--help"));
		assertTrue(output().contains("--"), output());
	}

	@Test
	void withoutASubcommandTheUsageIsPrinted()
	{
		assertEquals(0, execute());

		assertTrue(output().contains("split") && output().contains("combine"), output());
	}

	@Test
	void theContributionOffersTheShareCommand()
	{
		assertEquals("share",
			new CommandLine(new SecretSharingCommandContribution().getCommands().get(0))
				.getCommandName());
	}
}
