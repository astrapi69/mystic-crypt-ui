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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@code scripts/check-forced-run-results.sh}, which ends the release gate (#630): every result
 * file the four suites hold after the gate has to come from the forced phase, not from a build
 * cache entry of an earlier run. The final {@code ./gradlew build} once restored the first phase's
 * results over the forced ones, and the gate stayed green while its XML described another run.
 */
class ForcedRunResultsCheckTest
{

	private static final File SCRIPT = new File("scripts/check-forced-run-results.sh")
		.getAbsoluteFile();

	/** The moment the forced phase started, as the gate records it */
	private static final Instant FORCED_RUN = Instant.parse("2026-10-27T15:44:39Z");

	@TempDir
	Path directory;

	@Test
	@DisplayName("result files from the forced run pass, and each suite says how many it read")
	void resultsOfTheForcedRun_pass() throws Exception
	{
		Path unit = suite("test");
		write(unit, "TEST-VaultXmlCodecTest.xml", "2026-10-27T15:45:02.115Z");
		write(unit, "TEST-SampleVaultsTest.xml", "2026-10-27T15:46:10.002Z");
		Path e2e = suite("e2eTest");
		write(e2e, "TEST-AddEntryUiTest.xml", "2026-10-27T15:51:40.570Z");

		Run run = run(String.valueOf(FORCED_RUN.getEpochSecond()), unit.toString(), e2e.toString());

		assertEquals(0, run.exit(), run.output());
		assertTrue(run.output().contains("test: 2 result files, all from the forced run"),
			run.output());
		assertTrue(run.output().contains("e2eTest: 1 result files, all from the forced run"),
			run.output());
	}

	@Test
	@DisplayName("a result file older than the forced run fails the gate and is named (#630)")
	void aResultFileOlderThanTheForcedRun_failsTheGate() throws Exception
	{
		Path unit = suite("test");
		write(unit, "TEST-VaultXmlCodecTest.xml", "2026-10-27T15:45:02.115Z");
		write(unit, "TEST-RestoredFromTheCacheTest.xml", "2026-10-27T14:31:48.773Z");

		Run run = run(String.valueOf(FORCED_RUN.getEpochSecond()), unit.toString());

		assertNotEquals(0, run.exit(), run.output());
		assertTrue(run.output().contains("TEST-RestoredFromTheCacheTest.xml"), run.output());
		assertTrue(run.output().contains("2026-10-27T14:31:48.773Z"), run.output());
		assertFalse(run.output().contains("TEST-VaultXmlCodecTest.xml"),
			"only the file that is older is named: " + run.output());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("theEdgeOfTheForcedRun")
	@DisplayName("the start of the forced run is the boundary, to the second")
	void theStartOfTheForcedRun_isTheBoundary(final String name, final String timestamp,
		final boolean passes) throws Exception
	{
		Path unit = suite("test");
		write(unit, "TEST-OnTheEdgeTest.xml", timestamp);

		Run run = run(String.valueOf(FORCED_RUN.getEpochSecond()), unit.toString());

		assertEquals(passes, run.exit() == 0, run.output());
	}

	static Stream<Arguments> theEdgeOfTheForcedRun()
	{
		return Stream.of(
			Arguments.of("in the second the forced run started", "2026-10-27T15:44:39.000Z", true),
			Arguments.of("the last moment of the second before", "2026-10-27T15:44:38.999Z",
				false));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("whatCannotBeChecked")
	@DisplayName("what cannot be checked fails the gate instead of passing it")
	void whatCannotBeChecked_failsTheGate(final String name, final Setup setup,
		final String expectedReason) throws Exception
	{
		String[] arguments = setup.prepare(directory);

		Run run = run(arguments);

		assertNotEquals(0, run.exit(), run.output());
		assertTrue(run.output().contains(expectedReason), run.output());
	}

	static Stream<Arguments> whatCannotBeChecked()
	{
		String start = String.valueOf(FORCED_RUN.getEpochSecond());
		return Stream.of(
			Arguments.of("a suite whose directory does not exist",
				(Setup)directory -> new String[] { start,
						directory.resolve("e2eKdbxTest").toString() },
				"no result file in"),
			Arguments.of("a suite whose directory holds no result file",
				(Setup)directory -> new String[] { start,
						Files.createDirectories(directory.resolve("e2eLockTest")).toString() },
				"no result file in"),
			Arguments.of("a result file without a timestamp", (Setup)directory -> {
				Path unit = Files.createDirectories(directory.resolve("test"));
				Files.writeString(unit.resolve("TEST-NoTimestampTest.xml"),
					"<testsuite name=\"NoTimestampTest\" tests=\"1\"></testsuite>\n",
					StandardCharsets.UTF_8);
				return new String[] { start, unit.toString() };
			}, "no readable timestamp"),
			Arguments.of("a start that is not a number of seconds", (Setup)directory -> {
				Path unit = Files.createDirectories(directory.resolve("test"));
				return new String[] { "yesterday", unit.toString() };
			}, "usage"), Arguments.of("no suite named at all",
				(Setup)directory -> new String[] { start }, "usage"));
	}

	@FunctionalInterface
	interface Setup
	{
		String[] prepare(Path directory) throws Exception;
	}

	private Path suite(final String name) throws Exception
	{
		return Files.createDirectories(directory.resolve(name));
	}

	private static void write(final Path suite, final String file, final String timestamp)
		throws Exception
	{
		Files.writeString(suite.resolve(file),
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<testsuite name=\""
				+ file.replace(".xml", "")
				+ "\" tests=\"1\" skipped=\"0\" failures=\"0\" errors=\"0\"" + " timestamp=\""
				+ timestamp + "\" hostname=\"gate\" time=\"0.1\">\n</testsuite>\n",
			StandardCharsets.UTF_8);
	}

	private Run run(final String... arguments) throws Exception
	{
		String[] command = new String[arguments.length + 1];
		command[0] = SCRIPT.getPath();
		System.arraycopy(arguments, 0, command, 1, arguments.length);
		Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
		assertTrue(process.waitFor(60, TimeUnit.SECONDS), "the script ended");
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		return new Run(process.exitValue(), output);
	}

	private record Run(int exit, String output) {
	}
}
