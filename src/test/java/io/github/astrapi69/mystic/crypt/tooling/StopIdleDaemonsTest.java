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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Drives {@code scripts/stop-idle-daemons.sh} the way the release gate does (#580): the Gradle
 * daemons are stopped only once none of them is building, so that a build of another worktree or
 * the IDE is never killed in the middle. A fake {@code gradlew} answers {@code --status} from a
 * list of answers, one per call, and records every {@code --stop}.
 */
class StopIdleDaemonsTest
{

	private static final File SCRIPT = new File("scripts/stop-idle-daemons.sh").getAbsoluteFile();

	private static final String IDLE = "   PID STATUS   INFO\n 30432 IDLE     9.7.1\n 56810 IDLE     9.7.1\n"
		+ " 76167 STOPPED  (other compatible daemons were started)\n";

	private static final String ONE_BUSY = "   PID STATUS   INFO\n 30432 IDLE     9.7.1\n161386 BUSY     9.7.1\n";

	private static final String ONE_CANCELED = "   PID STATUS   INFO\n161386 CANCELED 9.7.1\n";

	@TempDir
	Path directory;

	@Test
	@DisplayName("with every daemon idle they are stopped at once")
	void everyDaemonIdle_stopsThem() throws Exception
	{
		Run run = run(List.of(IDLE), 60);

		assertEquals(0, run.exit(), run.output());
		assertEquals(1, run.stops(), run.output());
		assertEquals(1, run.statusCalls());
	}

	@Test
	@DisplayName("a busy daemon is waited for, and the daemons are stopped once it is idle")
	void aBusyDaemon_isWaitedFor_thenTheyAreStopped() throws Exception
	{
		Run run = run(List.of(ONE_BUSY, ONE_BUSY, IDLE), 60);

		assertEquals(0, run.exit(), run.output());
		assertEquals(3, run.statusCalls(), "asked again after each wait");
		assertEquals(1, run.stops(), run.output());
		assertTrue(run.output().contains("161386 BUSY"),
			"names what it waits for: " + run.output());
	}

	@Test
	@DisplayName("a daemon still busy when the wait is over is not stopped, and the gate does not start")
	void aDaemonBusyPastTheWait_isNotStopped() throws Exception
	{
		Run run = run(List.of(ONE_BUSY), 0);

		assertNotEquals(0, run.exit(), run.output());
		assertEquals(0, run.stops(), "nothing is stopped while a daemon builds: " + run.output());
		assertTrue(run.output().contains("161386 BUSY"), run.output());
	}

	@Test
	@DisplayName("a status that is neither idle nor stopped counts as working")
	void anUnknownStatus_countsAsWorking() throws Exception
	{
		Run run = run(List.of(ONE_CANCELED), 0);

		assertNotEquals(0, run.exit(), run.output());
		assertEquals(0, run.stops(), run.output());
	}

	/**
	 * Runs the script against a fake gradlew whose n-th --status prints the n-th answer, the last
	 * one from then on
	 */
	private Run run(final List<String> answers, final int waitSeconds) throws Exception
	{
		for (int index = 0; index < answers.size(); index++)
		{
			Files.writeString(directory.resolve("answer-" + (index + 1)), answers.get(index),
				StandardCharsets.UTF_8);
		}
		Path gradlew = directory.resolve("gradlew");
		Files.writeString(gradlew,
			"#!/bin/sh\n" + "cd \"$(dirname \"$0\")\"\n" + "case \"$1\" in\n" + "  --status)\n"
				+ "    echo x >> status-calls\n" + "    n=$(wc -l < status-calls)\n"
				+ "    [ -f answer-$n ] || n=" + answers.size() + "\n" + "    cat answer-$n ;;\n"
				+ "  --stop) echo x >> stops ;;\n" + "  *) exit 9 ;;\n" + "esac\n",
			StandardCharsets.UTF_8);
		Files.setPosixFilePermissions(gradlew, PosixFilePermissions.fromString("rwxr-xr-x"));
		ProcessBuilder builder = new ProcessBuilder(SCRIPT.getPath()).redirectErrorStream(true);
		Map<String, String> environment = builder.environment();
		environment.put("GRADLEW", gradlew.toString());
		environment.put("STOP_IDLE_DAEMONS_INTERVAL", "0");
		environment.put("STOP_IDLE_DAEMONS_WAIT", String.valueOf(waitSeconds));
		Process process = builder.start();
		assertTrue(process.waitFor(60, TimeUnit.SECONDS), "the script ended");
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		return new Run(process.exitValue(), output, lines("status-calls"), lines("stops"));
	}

	private int lines(final String name) throws IOException
	{
		Path file = directory.resolve(name);
		return Files.exists(file) ? Files.readAllLines(file).size() : 0;
	}

	private record Run(int exit, String output, int statusCalls, int stops) {
	}
}
