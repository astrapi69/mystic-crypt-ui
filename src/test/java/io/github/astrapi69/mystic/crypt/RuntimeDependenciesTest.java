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
import java.util.jar.Attributes;
import java.util.jar.JarFile;
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

	private static final String APPLICATION_JAR = ApplicationJarUnderTest.PATH;

	private static final String RUNTIME_VERSIONS = ApplicationJarUnderTest.RUNTIME_VERSIONS;

	/**
	 * A pre-release, read from a jar's name: a milestone, a release candidate, an alpha, a beta, a
	 * preview, an early access build, a numbered build such as {@code -b180830.0359} or a snapshot.
	 * The {@code -jre} flavour of a release and Guava's {@code 9999.0-empty...} placeholder are
	 * releases
	 */
	private static final Pattern PRE_RELEASE = Pattern.compile(
		".*-\\d[^/]*[-.](M\\d+|[Rr][Cc]\\d*|alpha\\d*|beta\\d*|preview\\d*|ea|SNAPSHOT|b\\d+(?:\\.\\d+)?)\\.jar");

	/**
	 * The pre-releases this build's runtime class path still carries, each with what removes it.
	 * Empty since crypt-data 13.3: jaxb-api 2.4.0-b180830.0359, a numbered build of a 2.4.0 that
	 * was never released, left with crypt-data#133 (#841). The set is matched exactly, so a new
	 * pre-release fails the test and an entry cannot outlive its cause. The test class path is not
	 * checked: it carries test-only pre-releases that never ship, such as meanbean 3.0.0-M9
	 */
	private static final List<String> KNOWN_PRE_RELEASES = List.of();

	/**
	 * What the pattern reads as a pre-release, from the jar's name
	 *
	 * @param jar
	 *            the name of a jar
	 * @param preRelease
	 *            whether the pattern reads it as a pre-release
	 */
	@ParameterizedTest(name = "{0}: pre-release {1}")
	@CsvSource({ "commons-collections4-4.5.0-M2.jar, true", "jackson-core-2.16.0-rc1.jar, true",
			"jaxb-api-2.4.0-b180830.0359.jar, true", "jaxb-api-2.2.12-b141001.1542.jar, true",
			"example-3.0.0-beta.jar, true", "example-2.0.0-alpha2.jar, true",
			"mystic-crypt-ui-8.15-SNAPSHOT.jar, true", "guava-33.8.0-jre.jar, false",
			"listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar, false",
			"swingx-all-1.6.5-1.jar, false", "commons-collections4-4.6.0.jar, false",
			"jaxb-api-2.3.1.jar, false", "bcprov-jdk18on-1.86.jar, false",
			"commons-beanutils-1.11.0.jar, false" })
	void aJarName_isAPreRelease_asThePatternReadsIt(final String jar, final boolean preRelease)
	{
		assertEquals(preRelease, PRE_RELEASE.matcher(jar).matches(), jar);
	}

	/**
	 * This build's runtime class path carries no pre-release but the ones
	 * {@link #KNOWN_PRE_RELEASES} names with their issue. The libraries come from
	 * {@code mystic.crypt.ui.runtime.libraries}, which the build fills from
	 * {@code configurations.runtimeClasspath}: what the application jar is packed from, and so what
	 * ships, which the test class path is not (#841)
	 */
	@Test
	@DisplayName("the runtime class path carries no pre-release (#841)")
	void theRuntimeClassPath_carriesOnlyTheKnownPreReleases()
	{
		List<String> preReleases = ApplicationJarUnderTest.runtimeLibraries().stream()
			.map(File::getName).filter(name -> PRE_RELEASE.matcher(name).matches()).sorted()
			.toList();

		assertEquals(KNOWN_PRE_RELEASES, preReleases,
			"the runtime class path carries other pre-releases than the known ones (#841)");
	}

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
		ApplicationJarUnderTest.assumeNamed();

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
		ApplicationJarUnderTest.assumeNamed();

		try (ZipFile archive = new ZipFile(APPLICATION_JAR))
		{
			assertFalse(
				ApplicationJarUnderTest.runtimeVersionsIn(archive, Path.of(APPLICATION_JAR))
					.containsKey(module),
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
		ApplicationJarUnderTest.assumeNamed();

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
		ApplicationJarUnderTest.assumeNamed();

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
		ApplicationJarUnderTest.assumeNamed();

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
		ApplicationJarUnderTest.assumeNamed();

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
	 * The manifest is version 1.0 of the format, the only one the JAR File Specification defines,
	 * and its main section names no entry: the build wrote the project version as
	 * {@code Manifest-Version} and a {@code Name}, which belongs to a per-entry section (#783). The
	 * application's own version stands in {@code Implementation-Version}
	 */
	@Test
	@DisplayName("the application jar's manifest is version 1.0 and its main section names no entry")
	void applicationJar_manifestIsOfTheSpecifiedVersion() throws IOException
	{
		ApplicationJarUnderTest.assumeNamed();

		try (JarFile archive = new JarFile(APPLICATION_JAR))
		{
			Attributes main = archive.getManifest().getMainAttributes();

			assertEquals("1.0", main.getValue(Attributes.Name.MANIFEST_VERSION),
				APPLICATION_JAR + ": the version of the manifest format (#783)");
			assertNull(main.getValue("Name"),
				APPLICATION_JAR + ": a Name in the main section (#783)");
			assertNotNull(main.getValue(Attributes.Name.IMPLEMENTATION_VERSION),
				APPLICATION_JAR + " names no Implementation-Version");
		}
	}

	/**
	 * A jar that carries the {@code META-INF/versions/<n>/} classes of its multi-release libraries
	 * says {@code Multi-Release: true}, or the JVM never reads them and the libraries run the code
	 * they carry for Java 8: 107 of the jar's versioned classes had no class of their name outside
	 * {@code META-INF/versions}, BouncyCastle's KEM and KDF SPIs among them, and FlatLaf's
	 * multi-resolution images were not available (#785). The tests run with the libraries as
	 * separate jars, each with its own {@code Multi-Release: true}
	 */
	@Test
	@DisplayName("the application jar says Multi-Release when it carries versioned classes")
	void applicationJar_isMultiRelease_whenItCarriesVersionedClasses() throws IOException
	{
		ApplicationJarUnderTest.assumeNamed();

		try (JarFile archive = new JarFile(APPLICATION_JAR))
		{
			long versioned = archive.stream().map(ZipEntry::getName)
				.filter(name -> name.matches("META-INF/versions/\\d+/.+\\.class")).count();

			assertTrue(versioned > 0, APPLICATION_JAR + " carries no versioned class - "
				+ "an empty set is not a clean result");
			assertEquals("true",
				archive.getManifest().getMainAttributes().getValue(Attributes.Name.MULTI_RELEASE),
				APPLICATION_JAR + " carries " + versioned
					+ " versioned classes and does not say Multi-Release (#785)");
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
			String version = ApplicationJarUnderTest.runtimeVersionsIn(archive, jar)
				.get(group + ":" + artifact);
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
