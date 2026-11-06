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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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
 * randomizer is not a dependency of this application either since #658, which draws its random
 * values from {@code SecureRandom}. Before 10.5 its default generator is seeded with the current
 * time (GHSA-395w-fmf5-xfj3). Since #682 no library asks for such a version: roboter 3, through
 * swing-tree-component 3.3, brings 10.7, and swing-components 10 brings none. The constraint and
 * the floor here stay as a second guard, against a library that would bring an old version back.
 * <p>
 * silly-io and file-worker are direct dependencies, and their floors hold the data-loss fixes the
 * application ships with (#720): silly-io 4.0 takes the extension from the file name only
 * (astrapi69/silly-io#9, behind astrapi69/file-worker#37), file-worker 20.0 copies binary files
 * byte for byte (#45) and refuses a copy onto the file itself (#73).
 * <p>
 * commons-collections4 is no dependency of this application either: silly-collection brings it, up
 * to 28.2 as 4.5.0-M2, a milestone, which 8.12 and 8.13 shipped (#751). A milestone at runtime is
 * no release; the floor holds 4.6.0, the release after it, which silly-collection 28.3 declares.
 * <p>
 * The same floors are held against what an installer ships: the application jar of a published
 * installer, which {@code scripts/fetch-release-jar.sh} takes out of it after checking the
 * installer against its published sha256, read through the
 * {@code META-INF/maven/.../pom.properties} it carries for each library:
 * {@code ./scripts/fetch-release-jar.sh 8.11 && ./gradlew test --tests '*.RuntimeDependenciesTest'
 * -PapplicationJarUnderTest=$HOME/.cache/mystic-crypt-ui/release-jars/8.11/mystic-crypt-ui-8.11-app.jar}.
 * Without a jar named, that check is skipped and says so.
 * <p>
 * <b>What must not come back (#639).</b> resource-viewmodels, declared from 2022 and used by
 * nothing, brought Spring Boot Web along: Spring, an embedded Tomcat 9.0.63 and logback 1.2.11 sat
 * in the application jar, never started, and carried 87 of Dependabot's runtime alerts. The class
 * path and an application jar under test are held free of that stack.
 */
class RuntimeDependenciesTest
{

	private static final String APPLICATION_JAR = System
		.getProperty("mystic.crypt.ui.application.jar", "");

	/**
	 * Where the build names the version of every module the application jar is packed from (#769)
	 */
	private static final String RUNTIME_VERSIONS = "META-INF/mystic-crypt-ui/runtime-versions.txt";

	@ParameterizedTest(name = "{0} is at least {1}")
	@CsvSource({ "jackson-core, 2.22.4", "jackson-databind, 2.22.4", "jackson-annotations, 2.22",
			"commons-beanutils, 1.11.0", "randomizer, 10.5", "jettison, 1.5.4", "silly-io, 4.0",
			"file-worker, 20.0", "commons-collections4, 4.6.0" })
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
			"commons-beanutils, commons-beanutils, 1.11.0", "io.github.astrapi69, randomizer, 10.5",
			"org.codehaus.jettison, jettison, 1.5.4", "io.github.astrapi69, silly-io, 4.0",
			"io.github.astrapi69, file-worker, 20.0",
			"org.apache.commons, commons-collections4, 4.6.0" })
	void applicationJar_carriesAtLeastThePatchedRelease(final String group, final String artifact,
		final String floor) throws IOException
	{
		assumeFalse(APPLICATION_JAR.isBlank(),
			"no application jar named; -PapplicationJarUnderTest=<jar> names one");

		String version = versionInTheJar(Path.of(APPLICATION_JAR), group, artifact);

		assertTrue(compare(version, floor) >= 0,
			artifact + " " + version + " in " + APPLICATION_JAR + " is below " + floor);
	}

	@ParameterizedTest(name = "{0} is not on the class path")
	@ValueSource(strings = { "resource-viewmodels", "spring-boot", "spring-webmvc",
			"tomcat-embed-core" })
	void unusedWebStack_isNotOnTheClassPath(final String artifact)
	{
		assertEquals(List.of(), versionsOnTheClassPath(artifact),
			artifact + " is on the class path again (#639)");
	}

	/**
	 * An artifact the family renamed is not on the class path beside its successor: visitor 6 (now
	 * visitor-pattern) and state 6 (now state-pattern) carry the same classes under the same names
	 * with other bytes, and the class path order alone decided which ran (#774)
	 */
	@ParameterizedTest(name = "{0}, the artifact before its rename, is not on the class path")
	@ValueSource(strings = { "visitor", "state" })
	void supersededArtifact_isNotOnTheClassPath(final String artifact)
	{
		assertEquals(List.of(), versionsOnTheClassPath(artifact),
			artifact + " is on the class path beside its renamed successor (#774)");
	}

	@Test
	@DisplayName("no class of the family comes from two jars on the class path")
	void noClassOfTheFamily_comesFromTwoJars() throws IOException
	{
		Map<String, List<String>> jarsOfEachClass = new HashMap<>();
		for (String entry : System.getProperty("java.class.path").split(File.pathSeparator))
		{
			if (!entry.endsWith(".jar"))
			{
				continue;
			}
			try (ZipFile archive = new ZipFile(entry))
			{
				String jar = new File(entry).getName();
				archive.stream().map(ZipEntry::getName)
					.filter(
						name -> name.startsWith("io/github/astrapi69/") && name.endsWith(".class"))
					.forEach(name -> jarsOfEachClass.computeIfAbsent(name, key -> new ArrayList<>())
						.add(jar));
			}
		}
		Map<String, List<String>> twice = new TreeMap<>();
		jarsOfEachClass.forEach((name, jars) -> {
			if (jars.size() > 1)
			{
				twice.put(name, jars);
			}
		});

		assertEquals(Map.of(), twice, "classes carried by more than one jar (#774)");
	}

	@ParameterizedTest(name = "the application jar names no {0}")
	@ValueSource(strings = { "io.github.astrapi69:visitor", "io.github.astrapi69:state" })
	void applicationJar_namesNoSupersededArtifact(final String module) throws IOException
	{
		assumeFalse(APPLICATION_JAR.isBlank(),
			"no application jar named; -PapplicationJarUnderTest=<jar> names one");

		try (ZipFile archive = new ZipFile(APPLICATION_JAR))
		{
			assertFalse(runtimeVersionsIn(archive, Path.of(APPLICATION_JAR)).containsKey(module),
				APPLICATION_JAR + " is packed from " + module + " (#774)");
		}
	}

	/**
	 * The licence in the application jar is the application's: the libraries are packed first, and
	 * swing-components' LICENSE.txt, an unfilled template ("Copyright (C) ${year} ${owner}"), used
	 * to be the one that stayed (#773)
	 */
	@Test
	@DisplayName("the application jar's LICENSE.txt is the application's own")
	void applicationJar_carriesTheApplicationsLicence() throws IOException
	{
		assumeFalse(APPLICATION_JAR.isBlank(),
			"no application jar named; -PapplicationJarUnderTest=<jar> names one");

		try (ZipFile archive = new ZipFile(APPLICATION_JAR))
		{
			ZipEntry licence = archive.getEntry("LICENSE.txt");
			assertNotNull(licence, APPLICATION_JAR + " carries no LICENSE.txt");
			try (InputStream in = archive.getInputStream(licence))
			{
				assertEquals(Files.readString(Path.of("src", "main", "resources", "LICENSE.txt")),
					new String(in.readAllBytes(), StandardCharsets.UTF_8),
					APPLICATION_JAR + " carries another LICENSE.txt than the application's (#773)");
			}
		}
	}

	/**
	 * license-header.txt is a build file of the family's libraries, a template for source headers
	 * and the licence of nothing in the jar (#777)
	 */
	@Test
	@DisplayName("the application jar carries no license-header.txt")
	void applicationJar_carriesNoLicenseHeaderTemplate() throws IOException
	{
		assumeFalse(APPLICATION_JAR.isBlank(),
			"no application jar named; -PapplicationJarUnderTest=<jar> names one");

		try (ZipFile archive = new ZipFile(APPLICATION_JAR))
		{
			assertNull(archive.getEntry("license-header.txt"),
				APPLICATION_JAR + " carries license-header.txt (#777)");
		}
	}

	@ParameterizedTest(name = "the application jar carries nothing under {0}")
	@ValueSource(strings = { "org/apache/catalina/", "org/springframework/boot/" })
	void applicationJar_carriesNoUnusedWebStack(final String packagePath) throws IOException
	{
		assumeFalse(APPLICATION_JAR.isBlank(),
			"no application jar named; -PapplicationJarUnderTest=<jar> names one");

		try (ZipFile archive = new ZipFile(APPLICATION_JAR))
		{
			long entries = archive.stream().filter(entry -> entry.getName().startsWith(packagePath))
				.count();

			assertEquals(0, entries, APPLICATION_JAR + " carries " + entries + " entries under "
				+ packagePath + " (#639)");
		}
	}

	/**
	 * The application jar is no module, and must not say it is one: started with {@code -jar} or
	 * {@code -cp}, it ignores a module descriptor, and packed from 145 jars it used to keep the
	 * first library's - {@code swing.tree.component.main} - at its root and pf4j's under
	 * {@code META-INF/versions/9} (#771)
	 */
	@Test
	@DisplayName("the application jar carries no module descriptor of another library")
	void applicationJar_carriesNoModuleDescriptor() throws IOException
	{
		assumeFalse(APPLICATION_JAR.isBlank(),
			"no application jar named; -PapplicationJarUnderTest=<jar> names one");

		try (ZipFile archive = new ZipFile(APPLICATION_JAR))
		{
			List<String> descriptors = archive.stream().map(ZipEntry::getName)
				.filter(name -> name.equals("module-info.class")
					|| name.matches("META-INF/versions/\\d+/module-info\\.class"))
				.toList();

			assertEquals(List.of(), descriptors,
				APPLICATION_JAR + " carries module descriptors (#771)");
		}
	}

	/**
	 * The version of a library in the application jar, from the list the build writes into it
	 * ({@value #RUNTIME_VERSIONS}): one line {@code group:artifact=version} per module the jar is
	 * packed from. The family's libraries are built with Gradle and carry no
	 * {@code pom.properties}, and the all-dependencies jar keeps no library's own manifest, so
	 * without the list their version could not be read at all (#769). Where the library's
	 * {@code pom.properties} is in the jar too, both must name the same version
	 */
	private static String versionInTheJar(final Path jar, final String group, final String artifact)
		throws IOException
	{
		try (ZipFile archive = new ZipFile(jar.toFile()))
		{
			String version = runtimeVersionsIn(archive, jar).get(group + ":" + artifact);
			assertNotNull(version,
				jar + " names no version of " + group + ":" + artifact + " in " + RUNTIME_VERSIONS);
			String pomProperties = "META-INF/maven/" + group + "/" + artifact + "/pom.properties";
			if (archive.getEntry(pomProperties) != null)
			{
				assertEquals(version,
					propertiesIn(archive, pomProperties, jar).getProperty("version"),
					jar + ": " + RUNTIME_VERSIONS + " and " + pomProperties + " disagree");
			}
			return version;
		}
	}

	/**
	 * The lines {@code group:artifact=version} of {@value #RUNTIME_VERSIONS}, as a map from
	 * {@code group:artifact} to the version
	 */
	private static Map<String, String> runtimeVersionsIn(final ZipFile archive, final Path jar)
		throws IOException
	{
		ZipEntry entry = archive.getEntry(RUNTIME_VERSIONS);
		assertNotNull(entry, jar + " carries no " + RUNTIME_VERSIONS);
		Map<String, String> versions = new HashMap<>();
		try (InputStream in = archive.getInputStream(entry))
		{
			new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
				.filter(line -> line.contains("="))
				.forEach(line -> versions.put(line.substring(0, line.indexOf('=')),
					line.substring(line.indexOf('=') + 1)));
		}
		return versions;
	}

	private static Properties propertiesIn(final ZipFile archive, final String name, final Path jar)
		throws IOException
	{
		ZipEntry entry = archive.getEntry(name);
		assertNotNull(entry, jar + " carries no " + name);
		Properties properties = new Properties();
		try (InputStream in = archive.getInputStream(entry))
		{
			properties.load(in);
		}
		return properties;
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
