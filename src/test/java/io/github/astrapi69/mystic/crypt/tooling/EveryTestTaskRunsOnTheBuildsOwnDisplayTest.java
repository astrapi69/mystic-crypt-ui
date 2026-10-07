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
 * the build was started from (#505).
 * <p>
 * The harness alone did not close this: {@code ./gradlew build}, {@code check} and
 * {@code jacocoTestReport}, and every plugin build behind {@code make plugins}, reached the UI
 * tests without it, on whatever {@code DISPLAY} the shell carried - on a desktop the person's real
 * screen. So the build now borrows a display from the harness for every test task, and hands it to
 * the test JVM as {@code DISPLAY} and, so that this test can tell the two apart from an inherited
 * one, as {@code E2E_HARNESS_DISPLAY}. Which display the harness picks - its own Xvfb, or the
 * inherited one only with {@code E2E_USE_CURRENT_DISPLAY=1} - is {@link E2eHarnessDisplayTest}'s
 * question; this one asks whether every build that runs tests asks the harness at all.
 */
class EveryTestTaskRunsOnTheBuildsOwnDisplayTest
{

	/** The line every build that runs tests carries, from the plugins' directory */
	private static final String PLUGIN_APPLIES_OWN_DISPLAY = "apply from: '../../gradle/own-display.gradle'";

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
	@DisplayName("every plugin build asks for the display as well - they are separate Gradle builds, make plugins runs them")
	void everyPluginBuildAppliesTheOwnDisplay() throws IOException
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
			if (!Files.readString(buildFile.toPath(), StandardCharsets.UTF_8)
				.contains(PLUGIN_APPLIES_OWN_DISPLAY))
			{
				missing.add(plugin.getName());
			}
		}

		assertTrue(builds > 0, "no plugin build found - an empty set is not a clean result");
		assertTrue(missing.isEmpty(), builds + " plugin builds, without the display: " + missing);
	}
}
