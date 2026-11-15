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
package io.github.astrapi69.mystic.crypt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A JVM a test starts itself, on a German desktop: its environment says {@code de_DE}, as a test
 * JVM's does wherever the desktop speaks German, while the test JVM itself speaks English through
 * system properties (gradle/test-jvms.gradle, #615). The environment is set here, so the case holds
 * on any machine
 */
class ForkedJvmTest
{

	private static final String MARKER = "forked-jvm-warning-marker";

	/** What the forked JVM runs: one java.util.logging warning */
	public static final class WarningProbe
	{
		public static void main(final String[] args)
		{
			Logger.getLogger("probe").warning(MARKER);
		}
	}

	@Test
	@DisplayName("without the options a forked JVM takes the German environment's language")
	void withoutTheOptions_theForkedJvmWritesInTheEnvironmentsLanguage() throws Exception
	{
		String stderr = probeStderr(List.of());

		assertTrue(stderr.contains("WARNUNG: " + MARKER), stderr);
	}

	@Test
	@DisplayName("with the options a forked JVM speaks the test JVM's language")
	void withTheOptions_theForkedJvmWritesInTheTestJvmsLanguage() throws Exception
	{
		String stderr = probeStderr(ForkedJvm.localeOptions());

		assertTrue(stderr.contains("WARNING: " + MARKER), stderr);
	}

	private static String probeStderr(final List<String> options) throws Exception
	{
		List<String> command = new ArrayList<>();
		command.add(System.getProperty("java.home") + "/bin/java");
		command.add("-cp");
		command.add(System.getProperty("java.class.path"));
		command.addAll(options);
		command.add(WarningProbe.class.getName());
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.environment().put("LANG", "de_DE.UTF-8");
		builder.environment().put("LC_ALL", "de_DE.UTF-8");
		builder.environment().put("LC_MESSAGES", "de_DE.UTF-8");
		Process process = builder.start();
		String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
		if (!process.waitFor(60, TimeUnit.SECONDS))
		{
			process.destroyForcibly();
			throw new IllegalStateException("the probe did not finish: " + stderr);
		}
		return stderr;
	}
}
