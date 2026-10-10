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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every dependency version a plugin build names comes from the host's version catalog, which the
 * plugin builds read since #197: a version written into a plugin's build file stays where it is
 * when the catalog moves, so the plugin's tests check another library than the application ships.
 * Measured at #547: 25 coordinates written with their version, nine of them different from the
 * catalog, Bouncy Castle 1.85.2 against the 1.86 the application ships among them.
 * <p>
 * The check reads the build files as text. It fails closed: no plugin directory, no build file, or
 * a build file it cannot read is a failure, not an empty pass, and it reports how many build files
 * and dependency lines it read.
 */
class PluginBuildsTakeEveryVersionFromTheCatalogTest
{

	/** A dependency written as "group:artifact:version" or 'group:artifact:version' */
	private static final Pattern PINNED = Pattern
		.compile("[\"']([A-Za-z0-9_.-]+):([A-Za-z0-9_.-]+):([^\"'\\s]+)[\"']");

	/** A dependency in map notation, which names its version on a key of its own */
	private static final Pattern MAP_VERSION = Pattern.compile("\\bversion\\s*:\\s*[\"']");

	/** The configurations a dependency line in a plugin build starts with */
	private static final Pattern DEPENDENCY_LINE = Pattern.compile(
		"^\\s*(implementation|compileOnly|runtimeOnly|annotationProcessor|testImplementation|testCompileOnly|testRuntimeOnly|api)\\b");

	/**
	 * The versions a build file writes itself, as "file:line: text"; a version from a variable, as
	 * the host's own {@code $hostVersion} (#217), is no literal and passes
	 */
	static List<String> versionsWrittenIn(final String name, final String build)
	{
		List<String> pinned = new ArrayList<>();
		String[] lines = build.split("\n", -1);
		for (int index = 0; index < lines.length; index++)
		{
			String line = lines[index];
			if (line.trim().startsWith("//"))
			{
				continue;
			}
			Matcher matcher = PINNED.matcher(line);
			boolean literal = false;
			while (matcher.find())
			{
				literal |= !matcher.group(3).contains("$");
			}
			if (literal || MAP_VERSION.matcher(line).find())
			{
				pinned.add(name + ":" + (index + 1) + ": " + line.trim());
			}
		}
		return pinned;
	}

	record Example(String name, String line, boolean pinned) {
		@Override
		public String toString()
		{
			return name;
		}
	}

	static Stream<Example> examples()
	{
		return Stream.of(
			new Example("a version in double quotes is found",
				"    compileOnly \"com.miglayout:miglayout-swing:11.4.3\"", true),
			new Example("a version in single quotes is found",
				"    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.4'", true),
			new Example("a version in map notation is found",
				"    compileOnly group: 'org.pf4j', name: 'pf4j', version: '3.15.0'", true),
			new Example("a catalog alias passes", "    compileOnly libs.miglayout.swing", false),
			new Example("the host's version from a variable passes",
				"    compileOnly \"io.github.astrapi69:mystic-crypt-ui:$hostVersion\"", false),
			new Example("a dependency without a version passes",
				"    testRuntimeOnly \"org.junit.platform:junit-platform-launcher\"", false),
			new Example("a commented line passes",
				"    // compileOnly \"com.miglayout:miglayout-swing:11.4.3\"", false));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("examples")
	void theCheck_findsWhatItShould(final Example example)
	{
		assertEquals(example.pinned(), !versionsWrittenIn("build.gradle", example.line()).isEmpty(),
			example.line());
	}

	@Test
	@DisplayName("every plugin build reads the host's version catalog, so libs.* resolves in it (#547)")
	void everyPluginBuildReadsTheHostsCatalog() throws IOException
	{
		File[] plugins = new File("plugins").listFiles(File::isDirectory);
		assertNotNull(plugins, "the plugins directory is where it was");
		List<String> missing = new ArrayList<>();
		int builds = 0;
		for (File plugin : plugins)
		{
			if (!new File(plugin, "build.gradle").isFile())
			{
				continue;
			}
			builds++;
			File settings = new File(plugin, "settings.gradle");
			if (!settings.isFile() || !Files.readString(settings.toPath(), StandardCharsets.UTF_8)
				.contains("from(files(\"../../gradle/libs.versions.toml\"))"))
			{
				missing.add(plugin.getName());
			}
		}

		assertTrue(builds > 0, "no plugin build found - an empty set is not a clean result");
		assertTrue(missing.isEmpty(),
			builds + " plugin builds; not reading the host's version catalog: " + missing);
	}

	@Test
	@DisplayName("no plugin build names a dependency version of its own: every version comes from the catalog (#547)")
	void noPluginBuildPinsAVersion(final TestReporter reporter) throws IOException
	{
		File[] plugins = new File("plugins").listFiles(File::isDirectory);
		assertNotNull(plugins, "the plugins directory is where it was");
		List<String> pinned = new ArrayList<>();
		List<String> unreadable = new ArrayList<>();
		int builds = 0;
		int dependencyLines = 0;
		for (File plugin : plugins)
		{
			File buildFile = new File(plugin, "build.gradle");
			if (!buildFile.isFile())
			{
				continue;
			}
			builds++;
			if (!Files.isReadable(buildFile.toPath()))
			{
				unreadable.add(buildFile.getPath());
				continue;
			}
			String build = Files.readString(buildFile.toPath(), StandardCharsets.UTF_8);
			dependencyLines += (int)build.lines()
				.filter(line -> DEPENDENCY_LINE.matcher(line).find()).count();
			pinned.addAll(versionsWrittenIn(buildFile.getPath(), build));
		}
		reporter.publishEntry("plugin builds read", String.valueOf(builds));
		reporter.publishEntry("dependency lines read", String.valueOf(dependencyLines));

		assertTrue(builds > 0, "no plugin build found - an empty set is not a clean result");
		assertTrue(dependencyLines > 0, builds
			+ " plugin builds, but no dependency line read - a check that read nothing passes nothing");
		assertTrue(unreadable.isEmpty(), "plugin builds that could not be read: " + unreadable);
		assertTrue(pinned.isEmpty(),
			builds + " plugin builds, " + dependencyLines + " dependency lines; " + pinned.size()
				+ " name a version of their own instead of the catalog's:\n"
				+ String.join("\n", pinned));
	}
}
