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
package io.github.astrapi69.mystic.crypt.plugin.kem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import picocli.CommandLine;

/**
 * Tests the command line the way the two sides would use it: three separate runs, with files
 * passing between them and nothing shared but what those files carry.
 */
class KeyExchangeCommandTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	static java.util.List<String> algorithms()
	{
		return KeyExchangeSupport.algorithms();
	}

	private static String out;

	private static int run(String... arguments)
	{
		StringWriter written = new StringWriter();
		CommandLine commandLine = new CommandLine(new KeyExchangeCommand());
		commandLine.setOut(new PrintWriter(written));
		commandLine.setErr(new PrintWriter(written));
		int code = commandLine.execute(arguments);
		out = written.toString();
		return code;
	}

	/**
	 * The three steps in three runs, with only the files in between - which is the whole point of
	 * the command line side
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("algorithms")
	@DisplayName("three separate runs arrive at the same secret and carry a message")
	void theThreeStepsRunSeparately(String algorithm, @TempDir File directory) throws Exception
	{
		File keyFile = new File(directory, "recipient.key");
		File publicKeyFile = new File(directory, "recipient.pub");
		File handshakeFile = new File(directory, "handshake.txt");
		File encryptedFile = new File(directory, "message.txt");
		String message = "meet at eight";

		assertEquals(0, run("new", "-a", algorithm, "-k", keyFile.getAbsolutePath(), "-p",
			publicKeyFile.getAbsolutePath()));
		assertTrue(keyFile.isFile() && publicKeyFile.isFile());

		assertEquals(0, run("send", "-r", publicKeyFile.getAbsolutePath(), "-m", message, "-o",
			handshakeFile.getAbsolutePath(), "-e", encryptedFile.getAbsolutePath()));
		String senderFingerprint = fingerprintIn(out);

		assertEquals(0, run("receive", "-k", keyFile.getAbsolutePath(), "-s",
			handshakeFile.getAbsolutePath(), "-e", encryptedFile.getAbsolutePath()));

		assertEquals(senderFingerprint, fingerprintIn(out), "the two sides read different secrets");
		assertTrue(out.contains(message), "the message did not come back: " + out);
		assertFalse(Files.readString(encryptedFile.toPath(), StandardCharsets.UTF_8).contains(message),
			"the message was written out in the clear");
	}

	/**
	 * A private key file is not overwritten: doing so would lose every message ever sent to it
	 */
	@Test
	@DisplayName("an existing key file is not overwritten")
	void anExistingKeyFileIsKept(@TempDir File directory) throws Exception
	{
		File keyFile = new File(directory, "existing.key");
		Files.writeString(keyFile.toPath(), "the key that must not be lost");

		assertNotEquals(0, run("new", "-k", keyFile.getAbsolutePath()));

		assertEquals("the key that must not be lost", Files.readString(keyFile.toPath()));
	}

	/**
	 * The public key belongs on the sending side and the private one on the receiving side; handing
	 * in the wrong one has to say so
	 */
	@Test
	@DisplayName("the public key handed in where the private one belongs is refused")
	void thePublicKeyIsNotAPrivateKey(@TempDir File directory) throws Exception
	{
		File keyFile = new File(directory, "recipient.key");
		File publicKeyFile = new File(directory, "recipient.pub");
		File handshakeFile = new File(directory, "handshake.txt");
		run("new", "-k", keyFile.getAbsolutePath(), "-p", publicKeyFile.getAbsolutePath());
		run("send", "-r", publicKeyFile.getAbsolutePath(), "-o", handshakeFile.getAbsolutePath());

		assertNotEquals(0, run("receive", "-k", publicKeyFile.getAbsolutePath(), "-s",
			handshakeFile.getAbsolutePath()));
		assertTrue(out.contains("private"), out);
	}

	/**
	 * A missing file is named, rather than failing on something further down
	 */
	@Test
	@DisplayName("a file that is not there is named")
	void aMissingFileIsNamed(@TempDir File directory)
	{
		assertNotEquals(0,
			run("send", "-r", new File(directory, "nothing.pub").getAbsolutePath()));
		assertTrue(out.contains("nothing.pub"), out);
	}

	/**
	 * Every subcommand answers --help, so the command line is usable without reading the source
	 */
	@ParameterizedTest(name = "keyx {0} --help")
	@MethodSource("subcommands")
	@DisplayName("every subcommand explains itself")
	void everySubcommandExplainsItself(String subcommand)
	{
		assertEquals(0, run(subcommand, "--help"));
		assertTrue(out.contains("Usage:") && out.contains(subcommand), out);
	}

	static java.util.List<String> subcommands()
	{
		return java.util.List.of("new", "send", "receive");
	}

	private static String fingerprintIn(String output)
	{
		for (String line : output.split("\\R"))
		{
			if (line.startsWith("fingerprint "))
			{
				return line.substring("fingerprint ".length()).trim();
			}
		}
		throw new AssertionError("no fingerprint was printed: " + output);
	}
}
