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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The Makefile is the one place a target is described: every target carries its description on its
 * rule line, {@code make help} lists them, and a document that names a target names one that exists
 * (#911).
 * <p>
 * The descriptions in the README and the build guide used to be written by hand beside the
 * Makefile, and both went on saying that {@code make run} starts the last build long after it had
 * started building and testing every plugin. A description on the rule line changes with the rule.
 */
class MakefileTargetsAreDescribedTest
{

	/** A rule line: a target, its colon, not an assignment */
	private static final Pattern RULE = Pattern.compile("^([a-zA-Z0-9_-]+):(?!=)(.*)$");

	/** The description after the prerequisites: {@code ## [group] what it does} */
	private static final Pattern DESCRIPTION = Pattern.compile("## \\[([a-z]+)\\] (\\S.*)$");

	/** A target a document asks the reader to run: {@code make <target>} */
	private static final Pattern NAMED_TARGET = Pattern.compile(
		"`make ([a-zA-Z][a-zA-Z0-9_-]*)[` ]|^make ([a-zA-Z][a-zA-Z0-9_-]*)\\b", Pattern.MULTILINE);

	/** The documents that tell a developer which target to run */
	private static final List<Path> DOCUMENTS = List.of(Path.of("README.md"),
		Path.of("docs/development.md"), Path.of("docs/build-and-release.md"), Path.of("CLAUDE.md"));

	@Test
	@DisplayName("every target says what it does on its rule line, in a group make help shows")
	void everyTargetIsDescribed() throws IOException
	{
		List<String> undescribed = new ArrayList<>();
		Set<String> groups = helpGroups();
		List<String> targets = new ArrayList<>();
		for (String line : makefile())
		{
			Matcher rule = RULE.matcher(line);
			if (!rule.matches())
			{
				continue;
			}
			targets.add(rule.group(1));
			Matcher description = DESCRIPTION.matcher(rule.group(2));
			boolean shown = description.find()
				&& (groups.contains(description.group(1)) || "help".equals(rule.group(1)));
			if (!shown)
			{
				undescribed.add(rule.group(1));
			}
		}

		assertTrue(targets.size() > 50,
			"the Makefile has its targets, found " + targets.size() + ": " + targets);
		assertTrue(undescribed.isEmpty(), "targets without a description on their rule line, "
			+ "out of " + targets.size() + ": " + undescribed);
	}

	@Test
	@DisplayName("make help lists every target once")
	void makeHelpListsEveryTarget() throws IOException, InterruptedException
	{
		Set<String> targets = targets();
		targets.remove("help");

		List<String> listed = new ArrayList<>();
		for (String line : run("make", "--no-print-directory", "help"))
		{
			String trimmed = line.strip();
			if (line.startsWith("  ") && !trimmed.isEmpty())
			{
				listed.add(trimmed.split("\\s+", 2)[0]);
			}
		}

		assertEquals(targets, new LinkedHashSet<>(listed),
			"make help lists exactly the targets of the Makefile");
		assertEquals(listed.size(), new LinkedHashSet<>(listed).size(), "each of them once");
	}

	@Test
	@DisplayName("make without a target lists the targets instead of running the first one")
	void makeWithoutATargetIsHelp() throws IOException, InterruptedException
	{
		assertEquals(run("make", "--no-print-directory", "help"),
			run("make", "--no-print-directory"));
	}

	/**
	 * No target writes development plugins into the real profile: a start in the real profile is
	 * what the installed release is for (#498, #501), and bootRun did exactly that before failing
	 * on a Gradle task this build does not have (#918)
	 */
	@Test
	void noTargetInstallsIntoTheRealProfile() throws IOException
	{
		assertFalse(targets().contains("bootRun"),
			"bootRun installs the develop plugins into the real profile and then fails (#918)");
	}

	@Test
	@DisplayName("every target a developer document names exists")
	void everyNamedTargetExists() throws IOException
	{
		Set<String> targets = targets();
		List<String> unknown = new ArrayList<>();
		int named = 0;
		for (Path document : DOCUMENTS)
		{
			Matcher target = NAMED_TARGET
				.matcher(Files.readString(document, StandardCharsets.UTF_8));
			while (target.find())
			{
				String name = target.group(1) != null ? target.group(1) : target.group(2);
				named++;
				if (!targets.contains(name))
				{
					unknown.add(document + ": make " + name);
				}
			}
		}

		assertTrue(named > 10, "the documents name targets, found " + named);
		assertTrue(unknown.isEmpty(),
			"targets named in a document that the Makefile does not have, " + "out of " + named
				+ ": " + unknown);
	}

	private static List<String> makefile() throws IOException
	{
		return Files.readAllLines(Path.of("Makefile"), StandardCharsets.UTF_8);
	}

	private static Set<String> targets() throws IOException
	{
		Set<String> targets = new LinkedHashSet<>();
		for (String line : makefile())
		{
			Matcher rule = RULE.matcher(line);
			if (rule.matches())
			{
				targets.add(rule.group(1));
			}
		}
		return targets;
	}

	private static Set<String> helpGroups() throws IOException
	{
		String line = makefile().stream()
			.filter(candidate -> candidate.startsWith("HELP_GROUPS :=")).findFirst()
			.orElse("HELP_GROUPS :=");
		return new LinkedHashSet<>(
			List.of(line.substring("HELP_GROUPS :=".length()).strip().split("\\s+")));
	}

	private static List<String> run(final String... command)
		throws IOException, InterruptedException
	{
		Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
		List<String> output = new String(process.getInputStream().readAllBytes(),
			StandardCharsets.UTF_8).lines().toList();
		assertTrue(process.waitFor(30, TimeUnit.SECONDS), "make did not finish");
		assertEquals(0, process.exitValue(), String.join("\n", output));
		return output;
	}
}
