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
package io.github.astrapi69.mystic.crypt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * What the installer carries must not be what an advisory names (#617).
 * <p>
 * jackson and commons-beanutils are not this application's dependencies: they arrive through
 * mystic-crypt, from {@code randomizer} and {@code jobj-copy} and from {@code file-worker} and
 * {@code jobj-compare}, and the installer packages whatever the runtime class path holds. 8.10
 * shipped commons-beanutils 1.9.4 (CVE-2025-48734) that way; since mystic-crypt 13.7 the libraries
 * bring 1.11.0 and jackson 2.22.4. Dependabot cannot raise an alert for a dependency nobody
 * declares, so the floor is held here, against the class path the tests run on, which holds the
 * same resolved versions as the runtime one: a library that brought a lower version back fails it.
 * The same guard stands in lethenon (astrapi69/lethenon#231).
 * <p>
 * The same floors are held against what an installer ships: the application jar of a published
 * installer, which {@code scripts/fetch-release-jar.sh} takes out of it after checking the
 * installer against its published sha256, read through the
 * {@code META-INF/maven/.../pom.properties} it carries for each library:
 * {@code ./scripts/fetch-release-jar.sh 8.11 && ./gradlew test --tests '*.RuntimeDependenciesTest'
 * -PapplicationJarUnderTest=$HOME/.cache/mystic-crypt-ui/release-jars/8.11/mystic-crypt-ui-8.11-app.jar}.
 * Without a jar named, that check is skipped and says so.
 */
class RuntimeDependenciesTest
{

	private static final String APPLICATION_JAR = System
		.getProperty("mystic.crypt.ui.application.jar", "");

	@ParameterizedTest(name = "{0} is at least {1}")
	@CsvSource({ "jackson-core, 2.22.4", "jackson-databind, 2.22.4", "jackson-annotations, 2.22",
			"commons-beanutils, 1.11.0" })
	void transitiveDependency_isAtLeastItsPatchedRelease(final String artifact, final String floor)
	{
		List<String> versions = versionsOnTheClassPath(artifact);

		assertEquals(1, versions.size(),
			"exactly one " + artifact + " on the class path, found " + versions);
		assertTrue(compare(versions.getFirst(), floor) >= 0,
			artifact + " " + versions.getFirst() + " is below " + floor);
	}

	@ParameterizedTest(name = "the application jar carries {1} at least {2}")
	@CsvSource({ "com.fasterxml.jackson.core, jackson-core, 2.22.4",
			"com.fasterxml.jackson.core, jackson-databind, 2.22.4",
			"com.fasterxml.jackson.core, jackson-annotations, 2.22",
			"commons-beanutils, commons-beanutils, 1.11.0" })
	void applicationJar_carriesAtLeastThePatchedRelease(final String group, final String artifact,
		final String floor) throws IOException
	{
		assumeFalse(APPLICATION_JAR.isBlank(),
			"no application jar named; -PapplicationJarUnderTest=<jar> names one");

		String version = versionInTheJar(Path.of(APPLICATION_JAR), group, artifact);

		assertTrue(compare(version, floor) >= 0,
			artifact + " " + version + " in " + APPLICATION_JAR + " is below " + floor);
	}

	private static String versionInTheJar(final Path jar, final String group, final String artifact)
		throws IOException
	{
		String properties = "META-INF/maven/" + group + "/" + artifact + "/pom.properties";
		try (ZipFile archive = new ZipFile(jar.toFile()))
		{
			ZipEntry entry = archive.getEntry(properties);
			assertNotNull(entry, jar + " carries no " + properties);
			Properties pom = new Properties();
			try (InputStream in = archive.getInputStream(entry))
			{
				pom.load(in);
			}
			return pom.getProperty("version");
		}
	}

	private static List<String> versionsOnTheClassPath(final String artifact)
	{
		Pattern jar = Pattern.compile(Pattern.quote(artifact) + "-(\\d[^/]*)\\.jar");
		return Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
			.map(entry -> jar.matcher(new File(entry).getName())).filter(Matcher::matches)
			.map(matcher -> matcher.group(1)).toList();
	}

	/**
	 * Compares the numeric parts of two versions; a qualifier such as {@code -rc1} counts as below
	 * the release it precedes
	 */
	private static int compare(final String version, final String floor)
	{
		String[] left = version.split("-", 2);
		int[] have = numbers(left[0]);
		int[] want = numbers(floor);
		for (int index = 0; index < Math.max(have.length, want.length); index++)
		{
			int difference = Integer.compare(index < have.length ? have[index] : 0,
				index < want.length ? want[index] : 0);
			if (difference != 0)
			{
				return difference;
			}
		}
		return left.length > 1 ? -1 : 0;
	}

	private static int[] numbers(final String version)
	{
		return Arrays.stream(version.split("\\.")).mapToInt(Integer::parseInt).toArray();
	}
}
