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
package io.github.astrapi69.mystic.crypt.keepass;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Reads a KDBX file with KeePassXC, so that a round trip is judged by the program a user would open
 * the file with rather than by the model that wrote it.
 * <p>
 * This is the whole point of the round trip test: our own model cannot be the yardstick, because a
 * field the model does not carry compares equal to itself and the test passes while the file lost
 * it. That is the circular measurement the KDBX phase A was set up to avoid.
 * <p>
 * {@code keepassxc-cli} is therefore a prerequisite of the suite, and a missing one FAILS rather
 * than skips (#333): a gate that cannot check must never report green.
 */
public final class KeePassXcDump
{

	/** Points the test at a keepassxc-cli that is not on the PATH, e.g. an extracted package */
	public static final String EXECUTABLE_PROPERTY = "keepassxc.cli";

	private KeePassXcDump()
	{
	}

	/**
	 * The version of the keepassxc-cli that reads both sides of the round trip. It belongs in every
	 * failure message, because the one this suite runs against is not always the one the fixture
	 * was written with: CI installs Ubuntu's package, which is older than the KeePassXC that wrote
	 * the fixture
	 *
	 * @return the version as the tool prints it, e.g. {@code 2.7.10}
	 */
	public static String version()
	{
		String executable = executable();
		try
		{
			Process process = new ProcessBuilder(executable, "--version").redirectErrorStream(true)
				.start();
			process.getOutputStream().close();
			String printed = readFully(process.getInputStream()).strip();
			if (!process.waitFor(30, TimeUnit.SECONDS))
			{
				process.destroyForcibly();
				throw new IllegalStateException(
					executable + " --version did not finish within half a minute");
			}
			return printed;
		}
		catch (IOException | InterruptedException exception)
		{
			throw new IllegalStateException("could not run " + executable + " --version",
				exception);
		}
	}

	/**
	 * The XML KeePassXC writes for the given database, which is the yardstick both sides of the
	 * round trip are measured against
	 *
	 * @param database
	 *            the KDBX file
	 * @param password
	 *            the password that opens it
	 * @return the exported XML
	 */
	public static String xmlOf(final File database, final String password)
	{
		return run(password, "export", "-f", "xml", "-q",
			database.getAbsolutePath()).standardOutput;
	}

	/**
	 * Writes one attachment of one entry to a file, which is how the attachment's CONTENT is
	 * compared rather than only its name
	 *
	 * @param database
	 *            the KDBX file
	 * @param password
	 *            the password that opens it
	 * @param entryPath
	 *            the entry's path inside the database, as {@code keepassxc-cli ls} prints it
	 * @param attachmentName
	 *            the name of the attachment
	 * @param target
	 *            the file to write it to
	 * @return what the tool wrote to standard error, which carries its warnings
	 */
	public static String exportAttachment(final File database, final String password,
		final String entryPath, final String attachmentName, final File target)
	{
		return run(password, "attachment-export", "-q", database.getAbsolutePath(), entryPath,
			attachmentName, target.getAbsolutePath()).errorOutput;
	}

	/**
	 * What KeePassXC says on standard error while reading the database - its warnings about the
	 * file, which are part of what a round trip has to be judged on
	 *
	 * @param database
	 *            the KDBX file
	 * @param password
	 *            the password that opens it
	 * @return the warnings, empty when there are none
	 */
	public static String warningsWhileReading(final File database, final String password)
	{
		return run(password, "export", "-f", "xml", "-q", database.getAbsolutePath()).errorOutput;
	}

	private static Result run(final String password, final String... arguments)
	{
		String executable = executable();
		String[] command = new String[arguments.length + 1];
		command[0] = executable;
		System.arraycopy(arguments, 0, command, 1, arguments.length);
		try
		{
			Process process = new ProcessBuilder(command).start();
			try (OutputStream toProcess = process.getOutputStream())
			{
				toProcess.write((password + "\n").getBytes(StandardCharsets.UTF_8));
				toProcess.flush();
			}
			String standardOutput = readFully(process.getInputStream());
			String errorOutput = readFully(process.getErrorStream());
			if (!process.waitFor(60, TimeUnit.SECONDS))
			{
				process.destroyForcibly();
				throw new IllegalStateException(
					"keepassxc-cli did not finish within a minute: " + String.join(" ", command));
			}
			return new Result(standardOutput, errorOutput, process.exitValue());
		}
		catch (IOException | InterruptedException exception)
		{
			throw new IllegalStateException("could not run " + String.join(" ", command),
				exception);
		}
	}

	/**
	 * The keepassxc-cli to use, failing with what to do about it when there is none - a skipped
	 * test here would report a green round trip that was never measured
	 */
	private static String executable()
	{
		String configured = System.getProperty(EXECUTABLE_PROPERTY);
		if (configured != null && !configured.isBlank())
		{
			if (!new File(configured).canExecute())
			{
				throw new IllegalStateException("-D" + EXECUTABLE_PROPERTY + " points at '"
					+ configured + "', which is not executable");
			}
			return configured;
		}
		if (isOnThePath())
		{
			return "keepassxc-cli";
		}
		throw new IllegalStateException("keepassxc-cli is not on the PATH, so the KDBX round trip "
			+ "cannot be measured against the program that reads these files. This test fails rather "
			+ "than skipping, because a round trip nobody checked must not report green (#333). "
			+ "Install it (apt-get install -y keepassxc-minimal) or point the test at one with "
			+ "-D" + EXECUTABLE_PROPERTY + "=/path/to/keepassxc-cli");
	}

	private static boolean isOnThePath()
	{
		try
		{
			Process process = new ProcessBuilder("keepassxc-cli", "--version").start();
			return process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0;
		}
		catch (IOException | InterruptedException exception)
		{
			return false;
		}
	}

	private static String readFully(final InputStream stream) throws IOException
	{
		ByteArrayOutputStream collected = new ByteArrayOutputStream();
		stream.transferTo(collected);
		return collected.toString(StandardCharsets.UTF_8);
	}

	private record Result(String standardOutput, String errorOutput, int exitCode) {
	}
}
