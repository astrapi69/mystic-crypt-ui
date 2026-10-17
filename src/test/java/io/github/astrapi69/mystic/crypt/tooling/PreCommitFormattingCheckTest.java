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

/**
 * Drives {@code scripts/check-formatting.sh}, which the pre-commit hook runs (#592), in a real
 * throwaway git repository: it asks Gradle to check only what Spotless covers among the staged
 * files, nothing when they are outside its reach, and refuses the commit with the command that
 * formats them. A fake {@code gradlew} records the tasks it is asked for and answers as told.
 */
class PreCommitFormattingCheckTest
{

	private static final File SCRIPT = new File("scripts/check-formatting.sh").getAbsoluteFile();

	/** What the fake gradlew prints when it refuses, in the shape Spotless reports it */
	private static final String VIOLATION = "The following files had format violations:\n"
		+ "      src/main/java/A.java\n";

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

	/** Runs the script in the repository with a fake gradlew that exits with the given code */
	private Run check(final int gradleExit) throws Exception
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
		builder.environment().put("GRADLEW", gradlew.toString());
		Process process = builder.start();
		assertTrue(process.waitFor(60, TimeUnit.SECONDS), "the script ended");
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		List<String> gradleCalls = Files.exists(calls) ? Files.readAllLines(calls) : List.of();
		return new Run(process.exitValue(), output, gradleCalls);
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

	private record Run(int exit, String output, List<String> gradleCalls) {
	}
}
