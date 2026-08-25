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
package io.github.astrapi69.mystic.crypt.plugin.keystore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import picocli.CommandLine;

/**
 * Tests of the {@code keystore} command: every subcommand is driven exactly the way a terminal
 * would drive it, on real files in a temporary directory, and checked by its exit code and its
 * output.
 */
class KeyStoreCommandTest
{

	private static final String STORE_PASSWORD = "cli-test-pw-1969";

	private StringWriter out;

	private StringWriter err;

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	/** Runs the command like the application does and captures what it writes */
	private int execute(String... args)
	{
		out = new StringWriter();
		err = new StringWriter();
		CommandLine commandLine = new CommandLine(new KeyStoreCommand());
		commandLine.setOut(new PrintWriter(out));
		commandLine.setErr(new PrintWriter(err));
		return commandLine.execute(args);
	}

	private String output()
	{
		return out.toString();
	}

	@Test
	void runsTheWholeLifeCycleOfAKeyStore(@TempDir File directory)
	{
		File store = new File(directory, "cli.p12");
		File pem = new File(directory, "cli.pem");

		assertEquals(0, execute("create", "-f", store.getAbsolutePath(), "-p", STORE_PASSWORD));
		assertTrue(store.exists(), "create must write the file");

		assertEquals(0, execute("list", "-f", store.getAbsolutePath(), "-p", STORE_PASSWORD));
		assertTrue(output().contains("0 entries"), "a new store is empty, was: " + output());

		assertEquals(0, execute("add-keypair", "-f", store.getAbsolutePath(), "-p", STORE_PASSWORD,
			"-a", "server", "-d", "CN=server", "-A", "RSA"));

		assertEquals(0, execute("list", "-f", store.getAbsolutePath(), "-p", STORE_PASSWORD));
		assertTrue(output().contains("server"), "list must show the alias, was: " + output());
		assertTrue(output().contains("private key"), "list must show the kind, was: " + output());
		assertTrue(output().contains("1 entries"), "list must count the entries, was: " + output());

		assertEquals(0, execute("export-cert", "-f", store.getAbsolutePath(), "-p", STORE_PASSWORD,
			"-a", "server", "-o", pem.getAbsolutePath()));
		assertTrue(pem.exists(), "export-cert must write the pem file");

		assertEquals(0, execute("import-cert", "-f", store.getAbsolutePath(), "-p", STORE_PASSWORD,
			"-a", "trusted", "-c", pem.getAbsolutePath()));
		assertEquals(0, execute("list", "-f", store.getAbsolutePath(), "-p", STORE_PASSWORD));
		assertTrue(output().contains("2 entries"),
			"the imported certificate must be listed, was: " + output());

		assertEquals(0, execute("delete", "-f", store.getAbsolutePath(), "-p", STORE_PASSWORD, "-a",
			"trusted"));
		assertEquals(0, execute("list", "-f", store.getAbsolutePath(), "-p", STORE_PASSWORD));
		assertTrue(output().contains("1 entries"),
			"delete must remove the alias, was: " + output());
		assertFalse(output().contains("trusted"), "the deleted alias must be gone");
	}

	/** The store types the tool offers must all work from the command line as well */
	@ParameterizedTest
	@CsvSource({ "PKCS12,p12", "JKS,jks", "JCEKS,jceks" })
	void createsAndFillsEveryStoreType(String type, String extension, @TempDir File directory)
	{
		File store = new File(directory, "typed." + extension);

		assertEquals(0, execute("create", "-f", store.getAbsolutePath(), "-t", type, "-p",
			STORE_PASSWORD));
		assertEquals(0, execute("add-keypair", "-f", store.getAbsolutePath(), "-t", type, "-p",
			STORE_PASSWORD, "-a", "entry"));
		assertEquals(0, execute("list", "-f", store.getAbsolutePath(), "-t", type, "-p",
			STORE_PASSWORD));

		assertTrue(output().contains("1 entries"),
			"the " + type + " store must hold the entry, was: " + output());
	}

	/** Including the post-quantum one, which is the reason the tool offers more than RSA */
	@ParameterizedTest
	@ValueSource(strings = { "RSA", "EC", "ML_DSA_65" })
	void addsAKeyPairForEveryOfferedAlgorithm(String algorithm, @TempDir File directory)
	{
		File store = new File(directory, "algorithm.p12");
		execute("create", "-f", store.getAbsolutePath(), "-p", STORE_PASSWORD);

		assertEquals(0, execute("add-keypair", "-f", store.getAbsolutePath(), "-p", STORE_PASSWORD,
			"-a", "entry", "-A", algorithm), "adding a " + algorithm + " key pair must succeed: "
				+ err);
	}

	@Test
	void aWrongPasswordEndsWithAFailingExitCode(@TempDir File directory)
	{
		File store = new File(directory, "wrong.p12");
		execute("create", "-f", store.getAbsolutePath(), "-p", STORE_PASSWORD);

		assertNotEquals(0,
			execute("list", "-f", store.getAbsolutePath(), "-p", "not the password"),
			"a wrong password must not end with a successful exit code");
	}

	@Test
	void aMissingRequiredOptionEndsWithAFailingExitCode(@TempDir File directory)
	{
		assertNotEquals(0, execute("create", "-p", STORE_PASSWORD),
			"the key store file is required");
	}

	@Test
	void anAlgorithmThatCannotSignACertificateIsRejected(@TempDir File directory)
	{
		File store = new File(directory, "x25519.p12");
		execute("create", "-f", store.getAbsolutePath(), "-p", STORE_PASSWORD);

		assertNotEquals(0, execute("add-keypair", "-f", store.getAbsolutePath(), "-p",
			STORE_PASSWORD, "-a", "entry", "-A", "X25519"),
			"a key exchange algorithm cannot sign a certificate and must be refused");
	}

	@Test
	void withoutASubcommandTheUsageIsPrinted()
	{
		assertEquals(0, execute());

		assertTrue(output().contains("add-keypair"),
			"the usage must list the subcommands, was: " + output());
	}

	@Test
	void theContributionOffersTheKeyStoreCommand()
	{
		assertEquals("keystore",
			new CommandLine(new KeyStoreCommandContribution().getCommands().get(0)).getCommandName(),
			"the contributed command must be the keystore command");
	}
}
