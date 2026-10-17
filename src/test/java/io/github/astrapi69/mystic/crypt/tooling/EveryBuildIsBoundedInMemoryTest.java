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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The full gate died of memory next to a browser and an IDE (#580). What a build may take is
 * bounded, the same for the host and every plugin build: each test JVM by gradle/test-jvms.gradle,
 * each Gradle daemon by the daemon arguments in gradle.properties, which the Makefile hands to the
 * plugin builds - they are Gradle builds of their own and have no gradle.properties. Both bounds
 * are Gradle's own defaults today, written down so that they hold whatever a Gradle upgrade
 * changes, and so that the host and the plugin builds keep asking for the same daemon.
 */
class EveryBuildIsBoundedInMemoryTest
{

	private static final String PLUGIN_APPLIES_TEST_JVMS = "apply from: '../../gradle/test-jvms.gradle'";

	private static final String HOST_APPLIES_TEST_JVMS = "apply from: 'gradle/test-jvms.gradle'";

	@Test
	@DisplayName("this suite's JVM may not grow beyond 512 MB")
	void theUnitSuitesHeap_isBounded()
	{
		assumeTrue(System.getProperty("org.gradle.test.worker") != null,
			"only a Gradle build sets the bound; an IDE run starts the JVM itself");

		long megabytes = Runtime.getRuntime().maxMemory() / (1024 * 1024);

		assertTrue(megabytes <= 512, "the test JVM may grow to " + megabytes + " MB");
	}

	@Test
	@DisplayName("the host and every plugin build bound their test JVMs with the same file")
	void everyBuild_appliesTheTestJvmBounds() throws IOException
	{
		assertTrue(Files.readString(Path.of("gradle/testing.gradle"), StandardCharsets.UTF_8)
			.contains(HOST_APPLIES_TEST_JVMS),
			"the host's test tasks apply gradle/test-jvms.gradle");
		List<String> missing = new ArrayList<>();
		int builds = 0;
		for (File plugin : plugins())
		{
			File buildFile = new File(plugin, "build.gradle");
			if (!buildFile.isFile())
			{
				continue;
			}
			builds++;
			if (!Files.readString(buildFile.toPath(), StandardCharsets.UTF_8)
				.contains(PLUGIN_APPLIES_TEST_JVMS))
			{
				missing.add(plugin.getName());
			}
		}

		assertTrue(builds > 0, "no plugin build found - an empty set is not a clean result");
		assertEquals(List.of(), missing, builds + " plugin builds, without the test JVM bounds");
	}

	@Test
	@DisplayName("every plugin build the Makefile runs gets the daemon arguments of gradle.properties")
	void everyPluginBuildInTheMakefile_getsTheDaemonArguments() throws IOException
	{
		List<String> makefile = Files.readAllLines(Path.of("Makefile"), StandardCharsets.UTF_8);
		List<String> pluginBuilds = makefile.stream()
			.filter(line -> line.contains("./gradlew") && line.contains(" -p ")).toList();
		List<String> without = pluginBuilds.stream()
			.filter(line -> !line.contains("$(PLUGIN_DAEMON)")).toList();
		String definition = makefile.stream().filter(line -> line.startsWith("PLUGIN_DAEMON :="))
			.findFirst().orElse(null);

		assertTrue(pluginBuilds.size() >= plugins().length - 1,
			"the Makefile runs the plugin builds: " + pluginBuilds.size());
		assertEquals(List.of(), without, "plugin builds without the daemon arguments");
		assertNotNull(definition, "the Makefile defines PLUGIN_DAEMON");
		assertTrue(
			definition.contains("org.gradle.jvmargs") && definition.contains("gradle.properties"),
			"PLUGIN_DAEMON reads the daemon arguments out of gradle.properties, not a copy: "
				+ definition);
		assertTrue(Files.readString(Path.of("gradle.properties"), StandardCharsets.UTF_8)
			.contains("\norg.gradle.jvmargs="), "gradle.properties names the daemon arguments");
	}

	@Test
	@DisplayName("every Gradle call of the Makefile takes GRADLE_FLAGS, so that one daemon serves all calls of a target")
	void everyGradleCallInTheMakefile_takesTheFlags() throws IOException
	{
		List<String> calls = Files.readAllLines(Path.of("Makefile"), StandardCharsets.UTF_8)
			.stream().filter(line -> line.startsWith("\t") && line.contains("./gradlew")).toList();
		List<String> without = calls.stream().filter(line -> !line.contains("$(GRADLE_FLAGS)"))
			.toList();

		// a flag such as -Djava.io.tmpdir is a daemon option to Gradle: a call without it needs a
		// daemon of its own next to the calls with it - measured in the #580 gate, where
		// build-full's
		// own two calls lacked the flags and the gate ran two daemons side by side
		assertTrue(calls.size() > 10, "the Makefile calls Gradle: " + calls.size());
		assertEquals(List.of(), without, "Gradle calls without $(GRADLE_FLAGS)");
	}

	private static File[] plugins()
	{
		File[] plugins = new File("plugins").listFiles(File::isDirectory);
		assertNotNull(plugins, "the plugins directory is where it was");
		return plugins;
	}
}
