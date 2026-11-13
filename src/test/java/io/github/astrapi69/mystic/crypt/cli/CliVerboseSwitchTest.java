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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * The command line's logging in a JVM of its own, because SLF4J binds once per JVM (#893): without
 * the verbose switch a {@code --cli} run prints neither the "No SLF4J providers" warnings nor a
 * debug line, and with {@code -Dmystic.crypt.ui.cli.verbose=true} the debug lines come - so the
 * switch finally keeps what its name promises. Everything the backend writes goes to standard
 * error, so piped {@code --cli} output stays clean
 */
class CliVerboseSwitchTest
{

	/** What the forked JVM runs: the CLI's logging setup, then one debug and one info line */
	public static final class Probe
	{
		public static void main(final String[] args)
		{
			MysticCryptUiCli.configureCliLogging();
			LoggerFactory.getLogger("probe").debug("probe-debug-marker");
			LoggerFactory.getLogger("probe").info("probe-info-marker");
		}
	}

	@Test
	@DisplayName("without the switch: no provider warning, no debug line, no info line")
	void withoutTheSwitch_noWarningAndNoDebugLine() throws Exception
	{
		String stderr = probeStderr(false);

		assertFalse(stderr.contains("SLF4J"),
			"no provider warning - the binding is on the class path: " + stderr);
		assertFalse(stderr.contains("probe-debug-marker"), "no debug line: " + stderr);
		assertFalse(stderr.contains("probe-info-marker"),
			"no info noise in a quiet run: " + stderr);
	}

	@Test
	@DisplayName("with the switch: the debug lines come")
	void withTheSwitch_debugLinesCome() throws Exception
	{
		String stderr = probeStderr(true);

		assertTrue(stderr.contains("probe-debug-marker"),
			"the verbose switch turns the debug lines on: " + stderr);
		assertFalse(stderr.contains("SLF4J:"), "still no provider warning: " + stderr);
	}

	private static String probeStderr(final boolean verbose) throws Exception
	{
		List<String> command = new ArrayList<>();
		command.add(System.getProperty("java.home") + "/bin/java");
		command.add("-cp");
		command.add(System.getProperty("java.class.path"));
		if (verbose)
		{
			command.add("-D" + MysticCryptUiCli.VERBOSE_PROPERTY + "=true");
		}
		command.add(Probe.class.getName());
		Process process = new ProcessBuilder(command).start();
		String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
		if (!process.waitFor(60, TimeUnit.SECONDS))
		{
			process.destroyForcibly();
			throw new IllegalStateException("the probe did not finish: " + stderr);
		}
		return stderr;
	}
}
