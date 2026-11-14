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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * What the build packs depends on the sources alone, not on the machine that packed it (#920,
 * RL-116): no manifest in the application's jars or in a plugin's zip names the building JDK's
 * patch level, the operating system or the moment of the build. The JDK is named once, as the
 * feature release in {@code Build-Jdk-Spec}, the attribute Maven writes for the same reason; which
 * JDK builds a release is {@code .sdkmanrc}'s to say.
 * <p>
 * Every manifest is read, including those of the jars an archive carries, so a plugin that one day
 * packs a jar of its own is held to the same rule. The archives are the ones the build just made:
 * the two jars of the host by the paths the build hands in, the plugins' zips from
 * {@code make plugins}. One that is missing fails the test rather than leaving nothing to check.
 */
class BuiltArchivesNameNoBuildMachineTest
{

	/** The attributes that make two builds of the same commit differ */
	private static final List<String> MACHINE_ATTRIBUTES = List.of("Build-Jdk", "Build-OS",
		"Build-Timestamp", "Build-Date", "Build-Time", "Bnd-LastModified");

	/** Where a jar keeps its manifest */
	private static final String MANIFEST_ENTRY = "META-INF/MANIFEST.MF";

	/** Where the internal plugins are, one directory each */
	private static final Path PLUGINS = Path.of("plugins");

	/** An archive the build made, with what it is */
	record BuiltArchive(String description, Path path) {
		@Override
		public String toString()
		{
			return description;
		}
	}

	/** A manifest found inside an archive, with where it was found */
	record FoundManifest(String location, Manifest manifest) {
	}

	/** An attribute of a manifest that names the machine or the moment of the build */
	record MachineAttribute(String location, String name, String value) {
		@Override
		public String toString()
		{
			return location + ": " + name + ": " + value;
		}
	}

	@Test
	@DisplayName("the application jars name their JDK only as the feature release in Build-Jdk-Spec")
	void theApplicationJars_nameTheJdkOnlyAsTheFeatureRelease() throws IOException
	{
		for (BuiltArchive archive : hostJars().toList())
		{
			Attributes main = mainManifestOf(archive).getMainAttributes();

			String jdkSpec = main.getValue("Build-Jdk-Spec");
			assertTrue(jdkSpec != null && jdkSpec.matches("\\d+"), archive + ": Build-Jdk-Spec is '"
				+ jdkSpec + "', not a feature release such as 25 (#920)");
		}
	}

	@ParameterizedTest(name = "{0} names no build machine and no build time")
	@MethodSource("builtArchives")
	void aBuiltArchive_namesNoBuildMachineAndNoBuildTime(final BuiltArchive archive)
		throws IOException
	{
		assertEquals(List.of(), machineAttributesIn(archive),
			archive + " names the machine or the moment of its build, so two builds of the "
				+ "same commit differ (#920, RL-116)");
	}

	/** Every attribute of {@link #MACHINE_ATTRIBUTES} in every manifest of the archive */
	private static List<MachineAttribute> machineAttributesIn(final BuiltArchive archive)
		throws IOException
	{
		List<MachineAttribute> found = new ArrayList<>();
		for (FoundManifest manifest : manifestsIn(archive))
		{
			Attributes main = manifest.manifest().getMainAttributes();
			for (String attribute : MACHINE_ATTRIBUTES)
			{
				String value = main.getValue(attribute);
				if (value != null)
				{
					found.add(new MachineAttribute(manifest.location(), attribute, value));
				}
			}
		}
		return found;
	}

	static Stream<BuiltArchive> builtArchives() throws IOException
	{
		return Stream.concat(hostJars(), pluginZips());
	}

	/**
	 * The application's jar and the all-dependencies jar the installer packs, by the paths the
	 * build hands in (gradle/testing.gradle)
	 */
	private static Stream<BuiltArchive> hostJars()
	{
		return Stream.of(
			new BuiltArchive("the application jar", builtJar("mystic.crypt.ui.built.jar")),
			new BuiltArchive("the all-dependencies jar",
				builtJar("mystic.crypt.ui.built.all.jar")));
	}

	private static Path builtJar(final String property)
	{
		String path = System.getProperty(property, "");
		if (path.isBlank())
		{
			fail("the build hands in no " + property + " (gradle/testing.gradle): "
				+ "without it there is no jar to check");
		}
		Path jar = Path.of(path);
		if (!Files.isRegularFile(jar))
		{
			fail(property + " is " + jar + ", which is not built");
		}
		return jar;
	}

	/**
	 * One zip per internal plugin, every plugin directory with a build of its own: a missing zip is
	 * a failure, not a smaller set
	 */
	private static Stream<BuiltArchive> pluginZips() throws IOException
	{
		List<BuiltArchive> zips = new ArrayList<>();
		List<String> missing = new ArrayList<>();
		try (Stream<Path> directories = Files.list(PLUGINS))
		{
			for (Path plugin : directories
				.filter(directory -> Files.isRegularFile(directory.resolve("build.gradle")))
				.sorted().toList())
			{
				List<Path> built = zipsIn(plugin.resolve("build/plugin-dist"));
				if (built.size() == 1)
				{
					zips.add(new BuiltArchive("the zip of " + plugin.getFileName(), built.get(0)));
				}
				else
				{
					missing.add(plugin.getFileName() + " (" + built.size() + " zips)");
				}
			}
		}
		if (!missing.isEmpty() || zips.isEmpty())
		{
			fail("not exactly one built zip for " + (zips.isEmpty() ? "any plugin" : missing)
				+ ". Run 'make plugins': a plugin whose zip is missing is a plugin nothing checks");
		}
		return zips.stream();
	}

	private static List<Path> zipsIn(final Path distribution) throws IOException
	{
		if (!Files.isDirectory(distribution))
		{
			return List.of();
		}
		try (Stream<Path> files = Files.list(distribution))
		{
			return files.filter(file -> file.getFileName().toString().endsWith(".zip")).toList();
		}
	}

	private static Manifest mainManifestOf(final BuiltArchive archive) throws IOException
	{
		return manifestsIn(archive).stream()
			.filter(manifest -> manifest.location().equals(MANIFEST_ENTRY)).findFirst()
			.orElseGet(() -> fail(archive + " carries no " + MANIFEST_ENTRY)).manifest();
	}

	private static List<FoundManifest> manifestsIn(final BuiltArchive archive) throws IOException
	{
		List<FoundManifest> found = new ArrayList<>();
		try (InputStream in = Files.newInputStream(archive.path()))
		{
			collectManifests(in, "", found);
		}
		return found;
	}

	/**
	 * Every manifest of the archive the stream reads, wherever it stands in it, and of every jar
	 * inside it
	 */
	private static void collectManifests(final InputStream archive, final String prefix,
		final List<FoundManifest> found) throws IOException
	{
		ZipInputStream entries = new ZipInputStream(archive);
		for (ZipEntry entry = entries.getNextEntry(); entry != null; entry = entries.getNextEntry())
		{
			String name = entry.getName();
			// a manifest at any depth: a plugin's zip has its class path root under classes/, where
			// pf4j reads a plugin's own META-INF/MANIFEST.MF
			if (name.toUpperCase(Locale.ROOT).endsWith(MANIFEST_ENTRY))
			{
				found.add(new FoundManifest(prefix + name,
					new Manifest(new ByteArrayInputStream(entries.readAllBytes()))));
			}
			else if (name.endsWith(".jar"))
			{
				collectManifests(new ByteArrayInputStream(entries.readAllBytes()),
					prefix + name + "!", found);
			}
		}
	}
}
