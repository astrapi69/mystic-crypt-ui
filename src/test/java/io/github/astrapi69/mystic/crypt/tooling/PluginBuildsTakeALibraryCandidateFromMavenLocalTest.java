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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * A plugin build checks a family library candidate that is only published locally when it is asked
 * to with {@code -PuseMavenLocal}, and otherwise takes nothing from the local repository but the
 * host (#508, #475).
 * <p>
 * {@code make bump-check} is the pre-check before a crypt-data or mystic-crypt release, and the
 * plugins depend on those libraries directly, so a breaking change shows there first (#480). Its
 * point is to run before the candidate is on Central, where a version can never be replaced. The
 * plugin builds took only the host from the local repository, with or without the switch, so the
 * check could not see the candidate at the one moment it mattered.
 * <p>
 * The test runs a real plugin build against a local repository of its own
 * ({@code -Dmaven.repo.local}) that holds a made-up version of mystic-crypt, which exists nowhere
 * else, and asks the build to resolve it. Offline, so the answer can only come from that
 * repository. Every plugin takes its repositories from the same file, which the last test pins, so
 * one plugin build stands for all of them.
 */
class PluginBuildsTakeALibraryCandidateFromMavenLocalTest
{

	/** A version of the library that exists only in the test's own local repository */
	private static final String CANDIDATE = "io.github.astrapi69:mystic-crypt:0.0.1-candidate508";

	/** The plugin build the test runs; any would do, they share their repositories */
	private static final String PLUGIN = "plugins/checksum-plugin";

	/** What the init script prints for the resolved candidate */
	private static final String RESOLVED = "RESOLVED ";

	/** The line every plugin build takes its repositories from */
	private static final String PLUGIN_APPLIES_REPOSITORIES = "apply from: '../../gradle/plugin-repositories.gradle'";

	@Test
	@Timeout(value = 5, unit = TimeUnit.MINUTES)
	@DisplayName("with -PuseMavenLocal, a plugin build resolves a library candidate from the local repository")
	void withTheSwitchTheCandidateComesFromTheLocalRepository(@TempDir File directory)
		throws Exception
	{
		File localRepository = localRepositoryWithTheCandidate(directory);

		Run run = resolveTheCandidate(directory, localRepository, true);

		assertEquals(0, run.exit(), run.output());
		String resolved = run.resolvedFile();
		assertNotNull(resolved, "the build printed no resolved file: " + run.output());
		assertTrue(resolved.startsWith(localRepository.getAbsolutePath()),
			"the candidate came from " + resolved + ", not from the local repository "
				+ localRepository);
	}

	@Test
	@Timeout(value = 5, unit = TimeUnit.MINUTES)
	@DisplayName("without the switch, the local repository gives a plugin build the host only, never a library (#475)")
	void withoutTheSwitchTheLocalRepositoryGivesNoLibrary(@TempDir File directory) throws Exception
	{
		File localRepository = localRepositoryWithTheCandidate(directory);

		Run run = resolveTheCandidate(directory, localRepository, false);

		assertNotEquals(0, run.exit(), "the candidate must not resolve: " + run.output());
		assertFalse(run.output().contains(RESOLVED), run.output());
		assertTrue(run.output().contains("io.github.astrapi69:mystic-crypt:0.0.1-candidate508"),
			"the failure names what could not be found: " + run.output());
	}

	@Test
	@DisplayName("every plugin build takes its repositories from the one shared file, so one plugin stands for all")
	void everyPluginBuildAppliesTheSharedRepositories() throws IOException
	{
		File[] plugins = new File("plugins").listFiles(File::isDirectory);
		assertNotNull(plugins, "the plugins directory is where it was");
		List<String> missing = new ArrayList<>();
		int builds = 0;
		for (File plugin : plugins)
		{
			File buildFile = new File(plugin, "build.gradle");
			if (!buildFile.isFile())
			{
				continue;
			}
			builds++;
			String build = Files.readString(buildFile.toPath(), StandardCharsets.UTF_8);
			if (!build.contains(PLUGIN_APPLIES_REPOSITORIES) || build.contains("mavenLocal {")
				|| build.contains("mavenLocal()"))
			{
				missing.add(plugin.getName());
			}
		}

		assertTrue(builds > 0, "no plugin build found - an empty set is not a clean result");
		assertTrue(missing.isEmpty(), builds
			+ " plugin builds; without the shared repositories, or with a mavenLocal of their own: "
			+ missing);
	}

	/**
	 * A local repository in Maven's layout holding the made-up candidate: a pom and an empty jar,
	 * which is all resolution looks at
	 */
	private static File localRepositoryWithTheCandidate(File directory) throws IOException
	{
		File localRepository = new File(directory, "m2");
		File version = new File(localRepository,
			"io/github/astrapi69/mystic-crypt/0.0.1-candidate508");
		assertTrue(version.mkdirs());
		Files.writeString(new File(version, "mystic-crypt-0.0.1-candidate508.pom").toPath(),
			"<project xmlns=\"http://maven.apache.org/POM/4.0.0\"><modelVersion>4.0.0</modelVersion>"
				+ "<groupId>io.github.astrapi69</groupId><artifactId>mystic-crypt</artifactId>"
				+ "<version>0.0.1-candidate508</version></project>",
			StandardCharsets.UTF_8);
		Files.write(new File(version, "mystic-crypt-0.0.1-candidate508.jar").toPath(), new byte[0]);
		return localRepository;
	}

	/**
	 * Runs the plugin build with an init script that resolves the candidate against the build's own
	 * repositories, offline, with the test's local repository
	 */
	private static Run resolveTheCandidate(File directory, File localRepository,
		boolean useMavenLocal) throws Exception
	{
		File initScript = new File(directory, "resolve-candidate.init.gradle");
		Files.writeString(initScript.toPath(), "allprojects {\n"
			+ "    tasks.register('resolveCandidate') {\n"
			+ "        def candidate = configurations.detachedConfiguration(dependencies.create('"
			+ CANDIDATE + "'))\n" + "        candidate.transitive = false\n"
			+ "        doLast { println '" + RESOLVED + "' + candidate.singleFile }\n" + "    }\n"
			+ "}\n", StandardCharsets.UTF_8);
		List<String> command = new ArrayList<>(
			List.of(new File("gradlew").getAbsolutePath(), "-p", PLUGIN, "--offline",
				"--no-configuration-cache", "--console=plain", "-I", initScript.getAbsolutePath(),
				"-Dmaven.repo.local=" + localRepository.getAbsolutePath()));
		if (useMavenLocal)
		{
			command.add("-PuseMavenLocal");
		}
		command.add("resolveCandidate");
		Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		assertTrue(process.waitFor(4, TimeUnit.MINUTES),
			"the plugin build did not finish: " + output);
		return new Run(process.exitValue(), output);
	}

	private record Run(int exit, String output) {

		/** The file the init script printed for the candidate, or null */
		String resolvedFile()
		{
			return output.lines().filter(line -> line.startsWith(RESOLVED))
				.map(line -> line.substring(RESOLVED.length()).trim()).findFirst().orElse(null);
		}
	}
}
