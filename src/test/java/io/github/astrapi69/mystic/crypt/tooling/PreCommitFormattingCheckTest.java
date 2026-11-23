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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Drives {@code scripts/check-formatting.sh}, which the pre-commit hook runs (#592), in a real
 * throwaway git repository: it asks Gradle to check only what Spotless covers among the staged
 * files, nothing when they are outside its reach, and refuses the commit with the command that
 * formats them. Gradle runs only inside the build slot: inside one or on a CI runner the check runs
 * it itself, with a slot script it queues through that, and otherwise it refuses before Gradle
 * starts. A fake {@code gradlew} records the tasks it is asked for and answers as told.
 */
class PreCommitFormattingCheckTest
{

	private static final File SCRIPT = new File("scripts/check-formatting.sh").getAbsoluteFile();

	/** What the fake gradlew prints when it refuses, in the shape Spotless reports it */
	private static final String VIOLATION = "The following files had format violations:\n"
		+ "      src/main/java/A.java\n";

	/** The variables that say whether a run is inside the build slot or on a CI runner */
	private static final List<String> SLOT_VARIABLES = List.of("GRADLE_SLOT", "GRADLE_SLOT_RUNNER",
		"GITHUB_ACTIONS");

	/** Neither inside the build slot nor on a CI runner, as a commit typed into a terminal */
	private static final Map<String, String> OUTSIDE_THE_SLOT = Map.of();

	@TempDir
	Path repository;

	@Test
	@DisplayName("a commit outside Spotless's reach costs no Gradle run")
	void stagedFilesOutsideSpotless_runNoGradle() throws Exception
	{
		stage("plugins/some-plugin/src/main/java/A.java", "docs/notes.txt",
			"gradle/libs.versions.toml");

		Run run = check(0);

		assertEquals(0, run.exit(), run.output());
		assertEquals(List.of(), run.gradleCalls());
	}

	@Test
	@DisplayName("a staged Java source of the host is checked with spotlessJavaCheck, and passes when it is formatted")
	void aStagedJavaSource_isCheckedWithTheJavaTask() throws Exception
	{
		stage("src/main/java/A.java");

		Run run = check(0);

		assertEquals(0, run.exit(), run.output());
		assertEquals(List.of("spotlessJavaCheck"), run.gradleCalls());
	}

	@Test
	@DisplayName("a staged top-level Markdown, Gradle or .gitignore file is checked with spotlessMiscCheck")
	void aStagedTopLevelFile_isCheckedWithTheMiscTask() throws Exception
	{
		stage("CHANGELOG.md", "build.gradle", ".gitignore");

		Run run = check(0);

		assertEquals(List.of("spotlessMiscCheck"), run.gradleCalls());
	}

	@Test
	@DisplayName("Java and top-level files together are checked in one Gradle run")
	void javaAndTopLevelFiles_areCheckedInOneRun() throws Exception
	{
		stage("src/test/java/B.java", "README.md");

		Run run = check(0);

		assertEquals(List.of("spotlessJavaCheck spotlessMiscCheck"), run.gradleCalls());
	}

	@Test
	@DisplayName("a file Spotless would format refuses the commit, names the file and the command that formats it")
	void aViolation_refusesTheCommit_withTheCommandThatFormats() throws Exception
	{
		stage("src/main/java/A.java");

		Run run = check(1);

		assertNotEquals(0, run.exit(), run.output());
		assertTrue(run.output().contains("src/main/java/A.java"), run.output());
		assertTrue(run.output().contains("./gradlew spotlessApply"), run.output());
	}

	@Test
	@DisplayName("a deleted Java source is nothing to format")
	void aDeletedJavaSource_runsNoGradle() throws Exception
	{
		stage("src/main/java/A.java");
		git("commit", "-q", "-m", "a file to delete");
		git("rm", "-q", "src/main/java/A.java");

		Run run = check(1);

		assertEquals(0, run.exit(), run.output());
		assertEquals(List.of(), run.gradleCalls());
	}

	@Test
	@DisplayName("outside the build slot and outside CI the check refuses before Gradle starts, and says how to run it")
	void outsideTheSlot_theCheckRefuses_beforeGradleStarts() throws Exception
	{
		stage("src/main/java/A.java");

		Run run = check(0, OUTSIDE_THE_SLOT);

		assertNotEquals(0, run.exit(), run.output());
		assertEquals(List.of(), run.gradleCalls());
		assertTrue(run.output().contains("build slot"), run.output());
		assertTrue(run.output().contains("GRADLE_SLOT_RUNNER="), run.output());
		assertTrue(run.output().contains("GRADLE_SLOT=none"), run.output());
	}

	@Test
	@DisplayName("outside the build slot a commit that stages nothing Spotless covers is not refused")
	void outsideTheSlot_nothingToCheck_isNotRefused() throws Exception
	{
		stage("docs/notes.txt");

		Run run = check(0, OUTSIDE_THE_SLOT);

		assertEquals(0, run.exit(), run.output());
		assertEquals(List.of(), run.gradleCalls());
	}

	@ParameterizedTest(name = "{0}={1}")
	@CsvSource({ "GRADLE_SLOT,1", "GRADLE_SLOT,none", "GITHUB_ACTIONS,true" })
	@DisplayName("inside a build slot, deliberately without one, or on a CI runner the check runs Gradle itself")
	void insideTheSlotOrOnCi_theCheckRunsGradleItself(final String name, final String value)
		throws Exception
	{
		stage("src/main/java/A.java");

		Run run = check(0, Map.of(name, value));

		assertEquals(0, run.exit(), run.output());
		assertEquals(List.of("spotlessJavaCheck"), run.gradleCalls());
	}

	@Test
	@DisplayName("with GRADLE_SLOT_RUNNER the check queues through that slot script, in the repository's root")
	void withASlotRunner_theCheckRunsThroughIt() throws Exception
	{
		stage("src/main/java/A.java");

		Run run = check(0, Map.of("GRADLE_SLOT_RUNNER", slotRunner().toString()));

		assertEquals(0, run.exit(), run.output());
		assertEquals(List.of("spotlessJavaCheck"), run.gradleCalls());
		assertEquals(List.of(repository.toRealPath().toString()),
			Files.readAllLines(repository.resolve(".git/runner-trees.txt")));
	}

	@Test
	@DisplayName("a violation found through the slot script refuses the commit and names the file")
	void aViolationThroughTheSlotRunner_refusesTheCommit() throws Exception
	{
		stage("src/main/java/A.java");

		Run run = check(1, Map.of("GRADLE_SLOT_RUNNER", slotRunner().toString()));

		assertNotEquals(0, run.exit(), run.output());
		assertTrue(run.output().contains("src/main/java/A.java"), run.output());
		assertTrue(run.output().contains("./gradlew spotlessApply"), run.output());
	}

	@Test
	@DisplayName("inside a build slot a slot script that is set as well is not asked for a second slot")
	void insideTheSlot_aSlotRunnerIsNotUsed() throws Exception
	{
		stage("src/main/java/A.java");

		Run run = check(0,
			Map.of("GRADLE_SLOT", "1", "GRADLE_SLOT_RUNNER", slotRunner().toString()));

		assertEquals(0, run.exit(), run.output());
		assertEquals(List.of("spotlessJavaCheck"), run.gradleCalls());
		assertTrue(!Files.exists(repository.resolve(".git/runner-trees.txt")),
			"the runner was not called");
	}

	@Test
	@DisplayName("a slot script that fails by itself refuses the commit as a check that could not run, with what it said")
	void aFailingSlotRunner_isReportedAsACheckThatCouldNotRun() throws Exception
	{
		stage("src/main/java/A.java");

		Run run = check(0, Map.of("GRADLE_SLOT_RUNNER", failingSlotRunner().toString()));

		assertNotEquals(0, run.exit(), run.output());
		assertTrue(run.output().contains("slot script broke"), run.output());
		assertTrue(run.output().contains("could not run (exit 3)"), run.output());
		assertTrue(!run.output().contains("Spotless would format"), run.output());
	}

	@Test
	@DisplayName("through a slot script the check says that it waits for the build slot")
	void throughASlotRunner_theCheckSaysThatItWaits() throws Exception
	{
		stage("src/main/java/A.java");

		Run run = check(0, Map.of("GRADLE_SLOT_RUNNER", slotRunner().toString()));

		assertEquals(0, run.exit(), run.output());
		assertTrue(run.output().contains("waiting for the build slot"), run.output());
	}

	@Test
	@DisplayName("through a slot script, a temporary directory that cannot be written refuses the commit and says so")
	void anUnwritableTemporaryDirectory_isReportedAsSuch() throws Exception
	{
		stage("src/main/java/A.java");

		Run run = check(0, Map.of("GRADLE_SLOT_RUNNER", slotRunner().toString(), "TMPDIR",
			repository.resolve(".git/no-such-directory").toString()));

		assertNotEquals(0, run.exit(), run.output());
		assertTrue(run.output().contains("could not create its temporary file"), run.output());
		assertTrue(!run.output().contains("Spotless would format"), run.output());
		assertEquals(List.of(), run.gradleCalls());
	}

	@ParameterizedTest(name = "Gradle exit {0}")
	@CsvSource({ "0", "1" })
	@DisplayName("through a slot script the check leaves its temporary file behind, after a pass and after a violation")
	void throughASlotRunner_theTemporaryFileIsLeftBehind(final int gradleExit) throws Exception
	{
		stage("src/main/java/A.java");

		Run run = check(gradleExit, Map.of("GRADLE_SLOT_RUNNER", slotRunner().toString()));

		assertEquals(gradleExit == 0 ? 0 : 1, run.exit(), run.output());
		assertEquals(1, run.leftBehind().size(), run.leftBehind() + " " + run.output());
	}

	/** A slot script that fails before it runs anything, as one that is missing or broken does */
	private Path failingSlotRunner() throws IOException
	{
		Path runner = repository.resolve(".git/failing-slot-runner");
		Files.writeString(runner, "#!/bin/sh\necho 'slot script broke' >&2\nexit 3\n",
			StandardCharsets.UTF_8);
		Files.setPosixFilePermissions(runner, PosixFilePermissions.fromString("rwxr-xr-x"));
		return runner;
	}

	/**
	 * A fake slot script with the command line of the real one, {@code <runner> <tree> <log file>
	 * <gradle arguments...>}: it records the tree and runs the fake gradlew there, its output into
	 * the log
	 */
	private Path slotRunner() throws IOException
	{
		Path runner = repository.resolve(".git/fake-slot-runner");
		Files.writeString(runner,
			"#!/bin/sh\n" + "tree=$1; log=$2; shift 2\n" + "(cd \"$tree\" && pwd -P) >> '"
				+ repository.resolve(".git/runner-trees.txt") + "'\n"
				+ "\"$GRADLEW\" -p \"$tree\" \"$@\" > \"$log\" 2>&1\n",
			StandardCharsets.UTF_8);
		Files.setPosixFilePermissions(runner, PosixFilePermissions.fromString("rwxr-xr-x"));
		return runner;
	}

	private void stage(final String... paths) throws Exception
	{
		if (!Files.isDirectory(repository.resolve(".git")))
		{
			git("init", "-q");
		}
		for (String path : paths)
		{
			Path file = repository.resolve(path);
			Files.createDirectories(file.getParent());
			Files.writeString(file, "content of " + path + "\n", StandardCharsets.UTF_8);
			git("add", path);
		}
	}

	/** Runs the script inside a build slot, with a fake gradlew that exits with the given code */
	private Run check(final int gradleExit) throws Exception
	{
		return check(gradleExit, Map.of("GRADLE_SLOT", "1"));
	}

	/**
	 * Runs the script in the repository with a fake gradlew that exits with the given code; the
	 * variables of the build slot and of CI are taken out of the environment first and then set as
	 * given, so the machine that runs the test decides nothing
	 */
	private Run check(final int gradleExit, final Map<String, String> slotEnvironment)
		throws Exception
	{
		// inside .git: part of no commit and of no other test
		Path calls = repository.resolve(".git/gradle-calls.txt");
		Path gradlew = repository.resolve(".git/fake-gradlew");
		Files.writeString(gradlew, "#!/bin/sh\n" + "tasks=''\n"
			+ "for argument in \"$@\"; do case \"$argument\" in spotless*) tasks=\"$tasks $argument\" ;; esac; done\n"
			+ "echo \"${tasks# }\" >> '" + calls + "'\n"
			+ (gradleExit == 0
				? "exit 0\n"
				: "printf '%s' '" + VIOLATION + "' >&2\nexit " + gradleExit + "\n"),
			StandardCharsets.UTF_8);
		Files.setPosixFilePermissions(gradlew, PosixFilePermissions.fromString("rwxr-xr-x"));
		ProcessBuilder builder = new ProcessBuilder(SCRIPT.getPath()).directory(repository.toFile())
			.redirectErrorStream(true);
		Map<String, String> environment = builder.environment();
		environment.put("GRADLEW", gradlew.toString());
		SLOT_VARIABLES.forEach(environment::remove);
		// a temporary directory of the test's own, empty, so what the check leaves in it can be
		// seen
		Path temporary = Files.createDirectories(repository.resolve(".git/check-tmp"));
		environment.put("TMPDIR", temporary.toString());
		environment.putAll(slotEnvironment);
		Process process = builder.start();
		assertTrue(process.waitFor(60, TimeUnit.SECONDS), "the script ended");
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		List<String> gradleCalls = Files.exists(calls) ? Files.readAllLines(calls) : List.of();
		List<String> leftBehind;
		try (var files = Files.list(temporary))
		{
			leftBehind = files.map(file -> file.getFileName().toString()).toList();
		}
		return new Run(process.exitValue(), output, gradleCalls, leftBehind);
	}

	private void git(final String... arguments) throws Exception
	{
		List<String> command = new ArrayList<>(List.of("git"));
		command.addAll(List.of(arguments));
		ProcessBuilder builder = new ProcessBuilder(command).directory(repository.toFile())
			.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD);
		Map<String, String> environment = builder.environment();
		// the test's own identity must not leak in from the machine that runs it
		environment.put("GIT_CONFIG_GLOBAL", "/dev/null");
		environment.put("GIT_AUTHOR_NAME", "Test");
		environment.put("GIT_AUTHOR_EMAIL", "test@example.invalid");
		environment.put("GIT_COMMITTER_NAME", "Test");
		environment.put("GIT_COMMITTER_EMAIL", "test@example.invalid");
		Process process = builder.start();
		assertTrue(process.waitFor(30, TimeUnit.SECONDS), "git " + String.join(" ", arguments));
		if (process.exitValue() != 0)
		{
			throw new IOException("git " + String.join(" ", arguments) + " failed with exit code "
				+ process.exitValue());
		}
	}

	private record Run(int exit, String output, List<String> gradleCalls, List<String> leftBehind) {
	}
}
