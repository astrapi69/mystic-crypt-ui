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
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every test JVM of a Gradle build runs on the display the build itself provides, not on the one
 * the build was started from (#505), and every test task fails when a test class left no result
 * (#699).
 * <p>
 * A harness called by name did not close this: {@code ./gradlew build}, {@code check} and
 * {@code jacocoTestReport}, and every plugin build behind {@code make plugins}, reached the UI
 * tests without it, on whatever {@code DISPLAY} the shell carried - on a desktop the person's real
 * screen. So every build that runs tests applies the own-display plugin of
 * astrapi69/own-display-gradle-plugin (#741), which hands each test JVM its display as
 * {@code DISPLAY} and, so that this test can tell the two apart from an inherited one, as
 * {@code E2E_HARNESS_DISPLAY}, and the test-results-complete plugin beside it. Which display the
 * harness picks - its own Xvfb, or the inherited one only with {@code E2E_USE_CURRENT_DISPLAY=1} -
 * is the question of the plugin's own tests; this one asks whether every build here applies both.
 */
class EveryTestTaskRunsOnTheBuildsOwnDisplayTest
{

	/**
	 * The lines every plugin build carries in its plugins block, through the version catalog the
	 * plugin builds share with the host
	 */
	private static final List<String> PLUGIN_BUILD_APPLIES = List
		.of("alias(libs.plugins.own.display)", "alias(libs.plugins.test.results.complete)");

	@Test
	@DisplayName("this suite's JVM runs on the display the build handed out")
	void theUnitSuiteRunsOnTheDisplayTheBuildHandedOut()
	{
		assumeTrue(System.getProperty("org.gradle.test.worker") != null,
			"only a Gradle build hands out a display; an IDE run starts the JVM itself");

		String handedOut = System.getenv("E2E_HARNESS_DISPLAY");

		assertNotNull(handedOut,
			"the build hands every test task a display of the harness's: DISPLAY is "
				+ System.getenv("DISPLAY") + ", and nothing says where it came from");
		assertEquals(handedOut, System.getenv("DISPLAY"));
	}

	@Test
	@DisplayName("every plugin build applies the own-display and the test-results-complete plugin - they are separate Gradle builds, make plugins runs them")
	void everyPluginBuildAppliesTheOwnDisplayAndTheResultCheck() throws IOException
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
			String buildScript = Files.readString(buildFile.toPath(), StandardCharsets.UTF_8);
			PLUGIN_BUILD_APPLIES.stream().filter(line -> !buildScript.contains(line))
				.forEach(line -> missing.add(plugin.getName() + " without " + line));
		}

		assertTrue(builds > 0, "no plugin build found - an empty set is not a clean result");
		assertTrue(missing.isEmpty(), builds + " plugin builds: " + missing);
	}
}
