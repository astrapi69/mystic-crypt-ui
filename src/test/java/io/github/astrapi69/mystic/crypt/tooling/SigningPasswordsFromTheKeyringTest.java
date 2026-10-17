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
package io.github.astrapi69.mystic.crypt.tooling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Drives {@code scripts/with-signing-passwords.sh} the way {@code make izpack-installer} does
 * (#579): the release keystore's two passwords reach the one command that signs as environment
 * variables, read from the desktop's keyring through {@code secret-tool}, and the script never
 * prints them. A fake {@code secret-tool} on the {@code PATH} stands in for the keyring.
 */
class SigningPasswordsFromTheKeyringTest
{

	private static final File SCRIPT = new File("scripts/with-signing-passwords.sh")
		.getAbsoluteFile();

	private static final String STORE_PASSWORD = "store-password-from-the-keyring";

	private static final String KEY_PASSWORD = "key-password-from-the-keyring";

	/** What the command checks: both variables carry the keyring's values */
	private static final String BOTH_HANDED_OVER = "test \"$MYSTIC_CRYPT_STORE_PASSWORD\" = '"
		+ STORE_PASSWORD + "' && test \"$MYSTIC_CRYPT_KEY_PASSWORD\" = '" + KEY_PASSWORD + "'";

	/** What the command checks: neither variable is set */
	private static final String NONE_HANDED_OVER = "test -z \"${MYSTIC_CRYPT_STORE_PASSWORD+set}\" "
		+ "&& test -z \"${MYSTIC_CRYPT_KEY_PASSWORD+set}\"";

	@TempDir
	Path directory;

	@Test
	@DisplayName("the keyring's two passwords reach the command as environment variables, and the script prints neither")
	void bothPasswordsInTheKeyring_reachTheCommand_unprinted() throws Exception
	{
		Path bin = fakeSecretTool(true);

		Run run = run(bin, BOTH_HANDED_OVER);

		assertEquals(0, run.exit(), run.output());
		assertTrue(run.output().contains("from the keyring"), run.output());
		assertFalse(run.output().contains(STORE_PASSWORD), run.output());
		assertFalse(run.output().contains(KEY_PASSWORD), run.output());
	}

	@Test
	@DisplayName("without entries in the keyring the command runs without the variables, and the script says so")
	void noEntriesInTheKeyring_theCommandRunsWithoutTheVariables() throws Exception
	{
		Path bin = fakeSecretTool(false);

		Run run = run(bin, NONE_HANDED_OVER);

		assertEquals(0, run.exit(), run.output());
		assertTrue(run.output().contains("the keyring holds no signing passwords"), run.output());
	}

	@Test
	@DisplayName("without secret-tool the command runs without the variables, and the script names the package")
	void noSecretTool_theCommandRunsWithoutTheVariables() throws Exception
	{
		Path bin = shellOnly();

		Run run = run(bin, NONE_HANDED_OVER);

		assertEquals(0, run.exit(), run.output());
		assertTrue(run.output().contains("libsecret-tools"), run.output());
	}

	@Test
	@DisplayName("the command's exit code is the script's, so a failed signing run fails the make target")
	void theCommandsExitCode_isTheScripts() throws Exception
	{
		Path bin = fakeSecretTool(true);

		assertEquals(3, run(bin, "exit 3").exit());
	}

	@Test
	@DisplayName("with the snap's SNAP variables the command gets the GNOME keyring's passwords, as without them (#595)")
	void withAndWithoutSnapVariables_theKeyringIsRead() throws Exception
	{
		Path bin = fakeSecretTool(true);

		Run withoutSnap = run(bin, BOTH_HANDED_OVER, Map.of());
		Run withSnap = run(bin, BOTH_HANDED_OVER + " && test \"$SNAP_NAME\" = code",
			Map.of("SNAP", "/snap/code/208", "SNAP_NAME", "code"));

		assertEquals(0, withoutSnap.exit(), withoutSnap.output());
		assertEquals(0, withSnap.exit(),
			"the lookup reads the GNOME keyring, and the command keeps its environment: "
				+ withSnap.output());
	}

	/**
	 * A directory that is the whole PATH the script gets: bash and sh, and nothing else - a real
	 * secret-tool in /usr/bin must not stand in for the fake one, or for its absence
	 */
	private Path shellOnly() throws IOException
	{
		Path bin = Files.createDirectories(directory.resolve("bin"));
		for (String shell : List.of("bash", "sh"))
		{
			Files.createSymbolicLink(bin.resolve(shell), Path.of("/bin", shell).toRealPath());
		}
		return bin;
	}

	/**
	 * A secret-tool that answers lookups of service mystic-crypt-ui, or finds nothing, next to the
	 * shells. Like libsecret, it reads another store when it runs with the snap's SNAP variables
	 * (#595): there it answers with a value the GNOME keyring does not hold.
	 */
	private Path fakeSecretTool(final boolean holdsThePasswords) throws IOException
	{
		Path bin = shellOnly();
		Path tool = bin.resolve("secret-tool");
		String answers = holdsThePasswords
			? "if [ -n \"${SNAP:-}\" ]; then printf '%s' 'from-the-file-store'; exit 0; fi\n"
				+ "case \"$*\" in\n"
				+ "  'lookup service mystic-crypt-ui purpose store-password') printf '%s' '"
				+ STORE_PASSWORD + "' ;;\n"
				+ "  'lookup service mystic-crypt-ui purpose key-password') printf '%s' '"
				+ KEY_PASSWORD + "' ;;\n" + "  *) exit 1 ;;\n" + "esac\n"
			: "exit 1\n";
		Files.writeString(tool, "#!/bin/sh\n" + answers, StandardCharsets.UTF_8);
		Files.setPosixFilePermissions(tool, PosixFilePermissions.fromString("rwxr-xr-x"));
		return bin;
	}

	private Run run(final Path bin, final String command) throws Exception
	{
		return run(bin, command, Map.of());
	}

	/**
	 * Runs the script with exactly the given SNAP variables: none come from the JVM running the
	 * test, which may itself have been started from a snap
	 */
	private Run run(final Path bin, final String command, final Map<String, String> snapVariables)
		throws Exception
	{
		List<String> arguments = new ArrayList<>(List.of(SCRIPT.getPath(), "sh", "-c", command));
		ProcessBuilder builder = new ProcessBuilder(arguments).redirectErrorStream(true);
		Map<String, String> environment = builder.environment();
		environment.remove("MYSTIC_CRYPT_STORE_PASSWORD");
		environment.remove("MYSTIC_CRYPT_KEY_PASSWORD");
		environment.keySet().removeIf(name -> name.startsWith("SNAP"));
		environment.putAll(snapVariables);
		environment.put("PATH", bin.toString());
		Process process = builder.start();
		assertTrue(process.waitFor(30, TimeUnit.SECONDS), "the script ended");
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		return new Run(process.exitValue(), output);
	}

	private record Run(int exit, String output) {
	}
}
