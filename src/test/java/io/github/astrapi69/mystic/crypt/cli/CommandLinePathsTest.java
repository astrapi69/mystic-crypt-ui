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
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The same call through the library's own entry point, {@link MysticCryptCli#execute(String...)},
 * and through the application's command line, {@link MysticCryptUiCli#execute(String...)}, the path
 * {@code java -jar mystic-crypt-ui.jar --cli} takes, gets the same answer: the same exit code, the
 * same standard error and the same standard output. Both run without a plugins directory, so a
 * difference cannot come from a plugin. Until #1189 a command that failed while it ran was answered
 * with a stack trace and exit code 1 on the application's path (measured in CI on 2026-11-26, run
 * 45806102101).
 */
class CommandLinePathsTest
{

	@TempDir
	File directory;

	/** What one call answered */
	record Answer(int exitCode, String standardOutput, String standardError) {
	}

	/** One of the two entry points */
	@FunctionalInterface
	interface Entry
	{
		int execute(String... args);
	}

	/** Calls of every kind, with the exit code both paths end with */
	static Stream<Arguments> calls()
	{
		return Stream.concat(
			Stream.of(
				Arguments.of("an unknown option",
					(Function<File, String[]>)directory -> new String[] { "der2pem",
							"--no-such-option" },
					2),
				Arguments.of("checksum of a small file",
					(Function<File, String[]>)directory -> new String[] { "checksum",
							smallFile(directory).getPath() },
					0)),
			failuresWhileRunning()
				.map(failure -> Arguments.of(failure.get()[0], failure.get()[1], 2)));
	}

	/** Calls that fail while the command runs */
	static Stream<Arguments> failuresWhileRunning()
	{
		return Stream.of(
			Arguments.of("der2pem on a file that does not exist",
				(Function<File, String[]>)directory -> new String[] { "der2pem", "--in",
						new File(directory, "missing.der").getPath() }),
			Arguments.of("hash without a password on standard input",
				(Function<File, String[]>)directory -> new String[] { "hash" }),
			Arguments.of("keystore list without a password on standard input",
				(Function<File, String[]>)directory -> new String[] { "keystore", "list", "--file",
						new File(directory, "store.p12").getPath() }));
	}

	@ParameterizedTest(name = "{0}: exit code {2} and the same output on both paths")
	@MethodSource("calls")
	void aCall_isAnsweredByBothPathsAlike(final String caseName,
		final Function<File, String[]> call, final int expectedExitCode)
	{
		String[] args = call.apply(directory);

		Answer library = run(MysticCryptCli::execute, args);
		Answer application = run(MysticCryptUiCli::execute, args);

		assertEquals(expectedExitCode, library.exitCode(), library::toString);
		assertEquals(library, application);
	}

	@ParameterizedTest(name = "{0}: the application answers with one error line and exit code 2, no stack trace")
	@MethodSource("failuresWhileRunning")
	void aFailureWhileRunning_isOneErrorLineAndExitCode2_onTheApplicationsPath(
		final String caseName, final Function<File, String[]> call)
	{
		Answer application = run(MysticCryptUiCli::execute, call.apply(directory));

		assertEquals(2, application.exitCode(), application::toString);
		assertTrue(application.standardError().startsWith("error: "), application::toString);
		assertEquals(1, application.standardError().lines().count(), application::toString);
		assertFalse(application.standardError().contains("\tat "), application::toString);
	}

	/**
	 * Runs one call with an empty standard input, the output of both streams captured, and a
	 * plugins directory that does not exist
	 */
	private Answer run(final Entry entry, final String... args)
	{
		InputStream originalIn = System.in;
		PrintStream originalOut = System.out;
		PrintStream originalErr = System.err;
		String originalPlugins = System.getProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		try
		{
			System.setProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY,
				new File(directory, "no-plugins").getPath());
			System.setIn(new ByteArrayInputStream(new byte[0]));
			System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
			System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
			int exitCode = entry.execute(args);
			return new Answer(exitCode, out.toString(StandardCharsets.UTF_8),
				err.toString(StandardCharsets.UTF_8));
		}
		finally
		{
			System.setIn(originalIn);
			System.setOut(originalOut);
			System.setErr(originalErr);
			if (originalPlugins == null)
			{
				System.clearProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY);
			}
			else
			{
				System.setProperty(MysticCryptUiCli.PLUGINS_DIRECTORY_PROPERTY, originalPlugins);
			}
		}
	}

	private static File smallFile(final File directory)
	{
		File file = new File(directory, "small.txt");
		try
		{
			Files.writeString(file.toPath(), "a small file to checksum\n", StandardCharsets.UTF_8);
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("could not write " + file, exception);
		}
		return file;
	}
}
