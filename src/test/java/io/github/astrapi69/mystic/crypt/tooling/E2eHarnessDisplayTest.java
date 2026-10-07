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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * Drives {@code scripts/e2e-harness.sh} through its own command line and asks one question: which
 * display does the command it starts get (#504).
 * <p>
 * The harness used to run the suite on whatever {@code DISPLAY} it inherited. On a desktop session
 * that is the person's own screen, and the robot clicked and typed there. It now starts an Xvfb of
 * its own unless the caller opts in to the current display with {@code E2E_USE_CURRENT_DISPLAY=1}.
 * <p>
 * <b>This test never starts a user interface.</b> The harness is pointed at a stub through
 * {@code E2E_RUN_COMMAND}, which only writes down the {@code DISPLAY} it was given, and the harness
 * runs in an empty directory that holds no {@code ./gradlew}. So the test stays harmless when
 * somebody runs it on a real desktop with {@code DISPLAY=:0}, and when the harness under test is
 * wrong.
 */
class E2eHarnessDisplayTest
{

	private static final File HARNESS = new File("scripts/e2e-harness.sh").getAbsoluteFile();

	/** The command the build runs under the harness to borrow its display (#505) */
	private static final File HOLD = new File("scripts/hold-display.sh").getAbsoluteFile();

	/** The line the hold command reports the display on */
	private static final String HANDED_OUT = "E2E_DISPLAY=";

	/** What the harness prints for each process it starts, so the test can check they are gone */
	private static final Pattern STARTED = Pattern
		.compile("started (Xvfb|fluxbox) \\(pid (\\d+)\\)");

	/** The display the harness reports for the Xvfb it started */
	private static final Pattern OWN_DISPLAY = Pattern
		.compile("started Xvfb \\(pid \\d+\\) on (:\\d+)");

	@Test
	@DisplayName("with DISPLAY=:0 and no opt-in, the command runs on the harness's own display, not on :0")
	void anInheritedDesktopDisplayIsNotUsed(@TempDir File directory) throws Exception
	{
		Run run = runHarness(directory, Map.of("DISPLAY", ":0"), 0);

		assertEquals(0, run.exit(), run.output());
		assertEquals(ownDisplayOf(run), run.displaySeen(),
			"the command runs on the Xvfb the harness started, whatever :0 is: " + run.output());
	}

	@Test
	@DisplayName("an inherited display that is really there is not used without the opt-in")
	void anInheritedDisplayThatAnswersIsNotUsed(@TempDir File directory) throws Exception
	{
		Process inherited = startXvfb(directory);
		try
		{
			String display = ":"
				+ awaitDisplayNumber(new File(directory, "test-display-number"), inherited);

			Run run = runHarness(directory, Map.of("DISPLAY", display), 0);

			assertEquals(0, run.exit(), run.output());
			assertNotEquals(display, run.displaySeen(), run.output());
			assertEquals(ownDisplayOf(run), run.displaySeen(), run.output());
			assertEverythingItStartedIsGone(run);
		}
		finally
		{
			inherited.destroy();
			inherited.waitFor(10, TimeUnit.SECONDS);
		}
	}

	@Test
	@DisplayName("without any DISPLAY, the harness starts its own display instead of refusing")
	void noDisplayAtAllIsNotAnError(@TempDir File directory) throws Exception
	{
		Run run = runHarness(directory, Map.of(), 0);

		assertEquals(0, run.exit(), run.output());
		assertEquals(ownDisplayOf(run), run.displaySeen(), run.output());
	}

	@Test
	@DisplayName("E2E_USE_CURRENT_DISPLAY=1 runs the command on exactly the display that was inherited")
	void theOptInUsesTheInheritedDisplay(@TempDir File directory) throws Exception
	{
		Process ownXvfb = null;
		try
		{
			ownXvfb = startXvfb(directory);
			String display = ":"
				+ awaitDisplayNumber(new File(directory, "test-display-number"), ownXvfb);

			Run run = runHarness(directory,
				Map.of("DISPLAY", display, "E2E_USE_CURRENT_DISPLAY", "1"), 0);

			assertEquals(0, run.exit(), run.output());
			assertEquals(display, run.displaySeen(), run.output());
			assertFalse(run.output().contains("started Xvfb"),
				"with the opt-in the harness starts no display of its own: " + run.output());
			assertEverythingItStartedIsGone(run);
		}
		finally
		{
			if (ownXvfb != null)
			{
				ownXvfb.destroy();
				ownXvfb.waitFor(10, TimeUnit.SECONDS);
			}
		}
	}

	@Test
	@DisplayName("the command is told that its display is one it may use, so a build under the harness starts no second one")
	void theCommandIsToldItsDisplayIsSettled(@TempDir File directory) throws Exception
	{
		Run run = runHarness(directory, Map.of("DISPLAY", ":0"), 0);

		assertEquals(0, run.exit(), run.output());
		assertEquals("1", run.optInSeen(), run.output());
		assertEquals(ownDisplayOf(run), run.displaySeen(), run.output());
	}

	@Test
	@DisplayName("the hold command hands the build the harness's display and keeps it until its input ends")
	@Timeout(90)
	void theHoldCommandHandsOutTheOwnDisplayUntilItsInputEnds(@TempDir File directory)
		throws Exception
	{
		ProcessBuilder builder = new ProcessBuilder(HARNESS.getPath(), "hold").directory(directory)
			.redirectErrorStream(true);
		Map<String, String> processEnvironment = builder.environment();
		processEnvironment.remove("E2E_USE_CURRENT_DISPLAY");
		processEnvironment.put("DISPLAY", ":0");
		processEnvironment.put("E2E_RUN_COMMAND", HOLD.getPath());
		processEnvironment.put("TMPDIR", directory.getAbsolutePath());
		Process harness = builder.start();
		try
		{
			BufferedReader output = new BufferedReader(
				new InputStreamReader(harness.getInputStream(), StandardCharsets.UTF_8));
			StringBuilder transcript = new StringBuilder();
			String handedOut = readUntilHandedOut(output, transcript);
			Thread.sleep(1000);

			assertTrue(harness.isAlive(),
				"the display is held while the input is open: " + transcript);

			harness.getOutputStream().close();
			assertTrue(harness.waitFor(30, TimeUnit.SECONDS),
				"the harness ends once the input ends: " + transcript);
			transcript.append(
				new String(harness.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
			Run run = new Run(harness.exitValue(), handedOut, "", transcript.toString());

			assertEquals(0, run.exit(), run.output());
			assertNotEquals(":0", handedOut, run.output());
			assertEquals(ownDisplayOf(run), handedOut, run.output());
			assertEverythingItStartedIsGone(run);
		}
		finally
		{
			harness.destroyForcibly();
		}
	}

	@Test
	@DisplayName("a failing command keeps its exit code, and the harness's Xvfb and window manager are gone afterwards")
	void afterAFailingCommandEverythingItStartedIsGone(@TempDir File directory) throws Exception
	{
		Run run = runHarness(directory, Map.of("DISPLAY", ":0"), 7);

		assertEquals(7, run.exit(), "the command's exit code comes through: " + run.output());
		assertTrue(run.output().contains("started Xvfb"), run.output());
		assertTrue(run.output().contains("started fluxbox"), run.output());
		assertEverythingItStartedIsGone(run);
	}

	@Test
	@DisplayName("after a passing command the harness's Xvfb and window manager are gone as well")
	void afterAPassingCommandEverythingItStartedIsGone(@TempDir File directory) throws Exception
	{
		Run run = runHarness(directory, Map.of(), 0);

		assertEquals(0, run.exit(), run.output());
		assertEverythingItStartedIsGone(run);
	}

	/**
	 * Runs the harness against a stub that records its {@code DISPLAY} and exits with the given
	 * code, in a directory without {@code ./gradlew}
	 */
	private static Run runHarness(File directory, Map<String, String> environment, int stubExit)
		throws Exception
	{
		File seen = new File(directory, "display-seen");
		File optInSeen = new File(directory, "opt-in-seen");
		File stub = new File(directory, "stub-command.sh");
		Files.writeString(stub.toPath(),
			"#!/usr/bin/env bash\nprintf '%s' \"${DISPLAY:-}\" > '" + seen.getAbsolutePath()
				+ "'\nprintf '%s' \"${E2E_USE_CURRENT_DISPLAY:-}\" > '"
				+ optInSeen.getAbsolutePath() + "'\nexit " + stubExit + "\n",
			StandardCharsets.UTF_8);
		assertTrue(stub.setExecutable(true));

		ProcessBuilder builder = new ProcessBuilder(HARNESS.getPath(), "e2eTest")
			.directory(directory).redirectErrorStream(true);
		Map<String, String> processEnvironment = builder.environment();
		processEnvironment.remove("DISPLAY");
		processEnvironment.remove("E2E_USE_CURRENT_DISPLAY");
		processEnvironment.put("E2E_RUN_COMMAND", stub.getAbsolutePath());
		processEnvironment.put("TMPDIR", directory.getAbsolutePath());
		processEnvironment.putAll(new HashMap<>(environment));
		Process process = builder.start();
		if (!process.waitFor(60, TimeUnit.SECONDS))
		{
			process.destroyForcibly();
			throw new IllegalStateException("the harness did not finish within 60s");
		}
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		String displaySeen = seen.exists()
			? Files.readString(seen.toPath(), StandardCharsets.UTF_8)
			: "<the command never ran>";
		String optIn = optInSeen.exists()
			? Files.readString(optInSeen.toPath(), StandardCharsets.UTF_8)
			: "<the command never ran>";
		return new Run(process.exitValue(), displaySeen, optIn, output);
	}

	/** Reads the harness's output until the hold command reports the display it was handed */
	private static String readUntilHandedOut(BufferedReader output, StringBuilder transcript)
		throws IOException
	{
		String line;
		while ((line = output.readLine()) != null)
		{
			transcript.append(line).append('\n');
			if (line.startsWith(HANDED_OUT))
			{
				return line.substring(HANDED_OUT.length());
			}
		}
		throw new IllegalStateException(
			"the harness ended without handing out a display: " + transcript);
	}

	/** An Xvfb the test owns, standing for a display the caller already has */
	private static Process startXvfb(File directory) throws IOException
	{
		File numberFile = new File(directory, "test-display-number");
		return new ProcessBuilder("bash", "-c",
			"exec Xvfb -displayfd 3 -nolisten tcp -screen 0 1280x1024x24 3>\"$0\"",
			numberFile.getAbsolutePath()).redirectErrorStream(true)
				.redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
	}

	private static String ownDisplayOf(Run run)
	{
		Matcher own = OWN_DISPLAY.matcher(run.output());
		assertTrue(own.find(), "the harness reports the Xvfb it started: " + run.output());
		return own.group(1);
	}

	private static void assertEverythingItStartedIsGone(Run run) throws InterruptedException
	{
		Matcher started = STARTED.matcher(run.output());
		while (started.find())
		{
			long pid = Long.parseLong(started.group(2));
			assertFalse(processIsAlive(pid), started.group(1) + " (pid " + pid
				+ ") is still running after the harness ended: " + run.output());
		}
	}

	/** Alive and not a zombie: a process that exited but was not reaped yet counts as gone */
	private static boolean processIsAlive(long pid) throws InterruptedException
	{
		for (int attempt = 0; attempt < 50; attempt++)
		{
			File status = new File("/proc/" + pid + "/status");
			try
			{
				if (!status.exists() || Files.readAllLines(status.toPath()).stream()
					.anyMatch(line -> line.startsWith("State:") && line.contains("Z")))
				{
					return false;
				}
			}
			catch (IOException vanished)
			{
				return false;
			}
			Thread.sleep(100);
		}
		return true;
	}

	private static String awaitDisplayNumber(File numberFile, Process xvfb) throws Exception
	{
		for (int attempt = 0; attempt < 100; attempt++)
		{
			if (numberFile.exists())
			{
				List<String> lines = Files.readAllLines(numberFile.toPath());
				if (!lines.isEmpty() && !lines.getFirst().isBlank())
				{
					return lines.getFirst().trim();
				}
			}
			if (!xvfb.isAlive())
			{
				throw new IllegalStateException("the test's own Xvfb did not start");
			}
			Thread.sleep(100);
		}
		throw new IllegalStateException("the test's own Xvfb reported no display number in 10s");
	}

	private record Run(int exit, String displaySeen, String optInSeen, String output) {
	}
}
