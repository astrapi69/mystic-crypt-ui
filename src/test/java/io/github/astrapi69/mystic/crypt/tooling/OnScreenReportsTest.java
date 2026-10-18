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
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Drives {@code scripts/print-on-screen-reports.sh} the way a red CI run does (#575): the report of
 * {@code WhatWasOnScreen} lands in the standard output of the test JVM, which Gradle writes into
 * the result XML and not to the console, so a failed run's log carried the stack trace and nothing
 * of what was on screen. The script reads the reports back out of the XML into the log.
 */
class OnScreenReportsTest
{

	private static final File SCRIPT = new File("scripts/print-on-screen-reports.sh")
		.getAbsoluteFile();

	private static final String PASSED = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
		+ "<testsuite name=\"io.github.astrapi69.mystic.crypt.ui.AddEntryUiTest\" tests=\"1\">\n"
		+ "  <system-out><![CDATA[started the application\n]]></system-out>\n</testsuite>\n";

	private static final String FAILED = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
		+ "<testsuite name=\"io.github.astrapi69.mystic.crypt.ui.ChecksumPluginUiTest\" tests=\"2\">\n"
		+ "  <system-out><![CDATA[[on-screen] === what was on screen when the test failed ===\n"
		+ "[on-screen] window \"Verify Checksum\" showing\n" + "an unrelated line\n"
		+ "]]></system-out>\n</testsuite>\n";

	@TempDir
	Path directory;

	@Test
	@DisplayName("the report lines of a failed class are printed, under the name of its result file")
	void theReportOfAFailedClass_isPrinted() throws Exception
	{
		Path results = suite("e2eTest");
		Files.writeString(results.resolve("TEST-AddEntryUiTest.xml"), PASSED,
			StandardCharsets.UTF_8);
		Files.writeString(results.resolve("TEST-ChecksumPluginUiTest.xml"), FAILED,
			StandardCharsets.UTF_8);

		Run run = run(results.toString());

		assertEquals(0, run.exit(), run.output());
		assertTrue(run.output().contains("TEST-ChecksumPluginUiTest.xml"), run.output());
		assertTrue(
			run.output().contains("\n[on-screen] === what was on screen when the test failed ==="),
			"a report that opens the output of the test JVM is printed without the XML around it: "
				+ run.output());
		assertTrue(run.output().contains("[on-screen] window \"Verify Checksum\" showing"),
			run.output());
		assertFalse(run.output().contains("an unrelated line"),
			"only the report, not the whole output of the test JVM: " + run.output());
		assertFalse(run.output().contains("TEST-AddEntryUiTest.xml"),
			"a class without a report is not named: " + run.output());
		assertTrue(run.output().contains("1 of 2 result files carried a report"),
			"it says how much it read: " + run.output());
	}

	@Test
	@DisplayName("result files without a report say so with the number read, and that is no failure")
	void resultFilesWithoutAReport_sayHowManyWereRead() throws Exception
	{
		Path results = suite("e2eTest");
		Files.writeString(results.resolve("TEST-AddEntryUiTest.xml"), PASSED,
			StandardCharsets.UTF_8);

		Run run = run(results.toString());

		assertEquals(0, run.exit(), run.output());
		assertTrue(run.output().contains("0 of 1 result files carried a report"), run.output());
	}

	@Test
	@DisplayName("several suites are read together, and a suite that wrote nothing is skipped")
	void severalSuites_areReadTogether() throws Exception
	{
		Path e2e = suite("e2eTest");
		Files.writeString(e2e.resolve("TEST-ChecksumPluginUiTest.xml"), FAILED,
			StandardCharsets.UTF_8);
		Path lock = suite("e2eLockTest");
		Files.writeString(lock.resolve("TEST-AddEntryUiTest.xml"), PASSED, StandardCharsets.UTF_8);

		Run run = run(e2e.toString(), lock.toString(), directory.resolve("e2eKdbxTest").toString());

		assertEquals(0, run.exit(), run.output());
		assertTrue(run.output().contains("1 of 2 result files carried a report"), run.output());
	}

	@Test
	@DisplayName("no result file at all is a failure, not an empty report")
	void noResultFileAtAll_fails() throws Exception
	{
		Run run = run(directory.resolve("e2eTest").toString());

		assertNotEquals(0, run.exit(), run.output());
		assertTrue(run.output().contains("no result file"), run.output());
	}

	private Path suite(final String name) throws Exception
	{
		return Files.createDirectories(directory.resolve(name));
	}

	private Run run(final String... directories) throws Exception
	{
		String[] command = new String[directories.length + 1];
		command[0] = SCRIPT.getPath();
		System.arraycopy(directories, 0, command, 1, directories.length);
		Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
		assertTrue(process.waitFor(60, TimeUnit.SECONDS), "the script ended");
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		return new Run(process.exitValue(), output);
	}

	private record Run(int exit, String output) {
	}
}
