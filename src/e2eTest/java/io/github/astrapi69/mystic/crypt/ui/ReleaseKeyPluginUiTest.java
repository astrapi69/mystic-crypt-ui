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
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * End-to-end test of the release key plugin (#644): with a vault open, the status view reads a
 * GnuPG home that gpg wrote - a primary key taken offline and a signing subkey that stayed - and
 * shows what gpg would say about it, without writing a byte into that home.
 */
class ReleaseKeyPluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	void showsTheStateOfAKeyThatGpgWrote() throws Exception
	{
		installPluginRequiringItBuilt(RELEASE_KEY_ZIP);
		Path home = gnuPgHomeWithAnOfflinePrimaryKey();
		try
		{
			Map<String, String> before = contentOf(home);

			File databaseFile = new File(tempHome, "release-key-database.mcrdb");
			createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
			ApplicationSteps application = signInWithExistingDatabase(databaseFile,
				MASTER_PASSWORD);
			FrameFixture frame = application.showMainFrame();
			application.openPluginTool("Key Status", "Release Key Status");

			GuiActionRunner.execute(() -> {
				frame.textBox("txtGnuPgHome").target().setText(home.toString());
				frame.textBox("txtReleaseKey").target().setText("");
			});
			robot.waitForIdle();
			GuiActionRunner.execute(() -> frame.button("btnRead").target().doClick());
			robot.waitForIdle();

			assertEquals(2, rowCount(frame), "the primary key and its subkey: " + result(frame));
			assertEquals("primary", cell(frame, 0, 0));
			assertEquals("offline", cell(frame, 0, 6), "the primary key's secret part left");
			assertEquals("valid", cell(frame, 0, 7), "gpg wrote a revocation certificate for it");
			assertEquals("subkey", cell(frame, 1, 0));
			assertEquals("[S]", cell(frame, 1, 2));
			assertEquals("here", cell(frame, 1, 6), "the signing subkey stayed");
			assertTrue(result(frame).contains("1 certificate"), result(frame));
			assertEquals(before, contentOf(home), "the view wrote nothing into the GnuPG home");
		}
		finally
		{
			run(null, "gpgconf", "--homedir", home.toString(), "--kill", "gpg-agent");
		}
	}

	/**
	 * A GnuPG home written by gpg: an Ed25519 primary key that signs and certifies, a signing
	 * subkey, and the primary key's secret part deleted, which is what is left once it was moved to
	 * an offline medium
	 */
	private Path gnuPgHomeWithAnOfflinePrimaryKey() throws IOException
	{
		Path home = Files.createDirectories(tempHome.toPath().resolve("gnupg-throwaway"));
		Files.setPosixFilePermissions(home, PosixFilePermissions.fromString("rwx------"));
		byte[] passphrase = ("throwaway-" + UUID.randomUUID() + "\n")
			.getBytes(StandardCharsets.UTF_8);
		gpg(home, passphrase, "--pinentry-mode", "loopback", "--passphrase-fd", "0",
			"--quick-gen-key", "Release Test <release@example.org>", "ed25519", "sign,cert", "0");
		String fingerprint = field(gpg(home, null, "--with-colons", "--list-keys"), "fpr");
		gpg(home, passphrase, "--pinentry-mode", "loopback", "--passphrase-fd", "0",
			"--quick-add-key", fingerprint, "ed25519", "sign", "1y");
		String keygrip = field(
			gpg(home, null, "--with-colons", "--with-keygrip", "--list-secret-keys", fingerprint),
			"grp");
		Files.delete(home.resolve("private-keys-v1.d").resolve(keygrip + ".key"));
		return home;
	}

	/**
	 * The tenth field of the first line of the given kind, where gpg puts fingerprints and grips
	 */
	private static String field(final String colons, final String kind)
	{
		return colons.lines().filter(line -> line.startsWith(kind + ":")).findFirst()
			.map(line -> line.split(":", -1)[9])
			.orElseThrow(() -> new AssertionError("gpg printed no " + kind + " line: " + colons));
	}

	private String gpg(final Path home, final byte[] input, final String... arguments)
		throws IOException
	{
		List<String> command = new java.util.ArrayList<>(
			List.of("gpg", "--homedir", home.toString(), "--batch", "--no-tty"));
		command.addAll(List.of(arguments));
		return run(input, command.toArray(String[]::new));
	}

	private String run(final byte[] input, final String... command) throws IOException
	{
		ProcessBuilder builder = new ProcessBuilder(command)
			.redirectError(ProcessBuilder.Redirect.appendTo(new File(tempHome, "gpg.log")));
		builder.environment().put("LC_ALL", "C");
		builder.environment().remove("GNUPGHOME");
		Process process = builder.start();
		try (OutputStream in = process.getOutputStream())
		{
			if (input != null)
			{
				in.write(input);
			}
		}
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		try
		{
			assertTrue(process.waitFor(120, TimeUnit.SECONDS), "finished: " + List.of(command));
		}
		catch (InterruptedException exception)
		{
			Thread.currentThread().interrupt();
			throw new IOException("interrupted while waiting for " + List.of(command), exception);
		}
		assertEquals(0, process.exitValue(), "succeeded, see gpg.log: " + List.of(command));
		return output;
	}

	/** Every file of the directory with the SHA-256 of its content */
	private static Map<String, String> contentOf(final Path root) throws Exception
	{
		Map<String, String> content = new TreeMap<>();
		try (Stream<Path> files = Files.walk(root))
		{
			for (Path file : files.filter(Files::isRegularFile).toList())
			{
				content.put(root.relativize(file).toString(), HexFormat.of().formatHex(
					MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))));
			}
		}
		return content;
	}

	private int rowCount(final FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> frame.table("tblKeys").target().getRowCount());
	}

	private String cell(final FrameFixture frame, final int row, final int column)
	{
		return GuiActionRunner
			.execute(() -> String.valueOf(frame.table("tblKeys").target().getValueAt(row, column)));
	}

	private String result(final FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> frame.label("lblResult").target().getText());
	}
}
