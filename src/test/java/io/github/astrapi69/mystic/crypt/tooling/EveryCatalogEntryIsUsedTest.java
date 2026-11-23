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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every entry of the version catalog is used by a build script of the host or of a plugin (#1008).
 * A catalog line nothing uses still reads like a dependency of the application: eight of them
 * stayed behind 048f49c8 for three months, and nothing in the build noticed until #1132 removed
 * them.
 * <p>
 * An alias counts as used when a build script names its accessor ({@code libs.silk.icon.theme},
 * {@code libs.bundles.unit.testing}, {@code libs.plugins.spotless.plugin.gradle},
 * {@code libs.versions.jacoco.version.get()}), when it is a member of a used bundle, or, for a
 * version, when a used library or plugin takes it by {@code version.ref}. A version only an unused
 * library takes is unused with it. The check reads the files as text and fails closed: no catalog,
 * no build script or an empty section is a failure, not an empty pass, and it reports how many
 * build scripts and aliases it read.
 */
class EveryCatalogEntryIsUsedTest
{

	private static final Path CATALOG = Path.of("gradle", "libs.versions.toml");

	/**
	 * An accessor of the catalog in a build script, such as {@code libs.versions.pitest.version}
	 */
	private static final Pattern ACCESSOR = Pattern.compile("\\blibs(?:\\.\\w+)+");

	/** What a build script calls on an accessor, which is no part of the alias */
	private static final Set<String> PROVIDER_CALLS = Set.of("get", "asProvider", "getOrNull",
		"orNull", "getOrElse", "map", "flatMap", "isPresent", "present");

	/** A section header of the catalog, such as {@code [libraries]} */
	private static final Pattern SECTION = Pattern.compile("^\\[(\\w+)]\\s*$", Pattern.MULTILINE);

	/** An entry of a section: its alias and its value, a bundle's list spanning several lines */
	private static final Pattern ENTRY = Pattern
		.compile("^([A-Za-z0-9_.-]+)\\s*=\\s*(\\[[^\\]]*]|[^\\n]*)", Pattern.MULTILINE);

	private static final Pattern VERSION_REF = Pattern
		.compile("version\\.ref\\s*=\\s*\"([^\"]+)\"");

	private static final Pattern QUOTED = Pattern.compile("\"([^\"]+)\"");

	/** The sections of the catalog an accessor names, with the prefix of its accessor */
	private static final Map<String, String> ACCESSOR_PREFIX = Map.of("libraries", "libs.",
		"bundles", "libs.bundles.", "plugins", "libs.plugins.", "versions", "libs.versions.");

	/**
	 * The entries of a catalog nobody uses, as "section alias", in the order of the catalog
	 *
	 * @param catalog
	 *            the text of the catalog
	 * @param scripts
	 *            the texts of the build scripts that read it
	 * @return the unused entries; empty when every entry is used
	 */
	static List<String> unusedEntries(final String catalog, final List<String> scripts)
	{
		Map<String, Map<String, String>> sections = sectionsOf(catalog);
		Set<String> named = accessorsNamedIn(scripts);
		Set<String> usedLibraries = used(sections, "libraries", named);
		Set<String> usedBundles = used(sections, "bundles", named);
		usedBundles
			.forEach(bundle -> usedLibraries.addAll(quotedIn(sections.get("bundles").get(bundle))));
		Set<String> usedPlugins = used(sections, "plugins", named);
		Set<String> usedVersions = used(sections, "versions", named);
		usedVersions.addAll(versionsTakenBy(sections.get("libraries"), usedLibraries));
		usedVersions.addAll(versionsTakenBy(sections.get("plugins"), usedPlugins));
		Map<String, Set<String>> usedBySection = Map.of("libraries", usedLibraries, "bundles",
			usedBundles, "plugins", usedPlugins, "versions", usedVersions);
		List<String> unused = new ArrayList<>();
		sections.forEach((section, entries) -> entries.keySet().stream()
			.filter(alias -> !usedBySection.getOrDefault(section, Set.of()).contains(alias))
			.forEach(alias -> unused.add(section + " " + alias)));
		return unused;
	}

	/**
	 * The sections of a catalog, each with its entries by alias
	 *
	 * @param catalog
	 *            the text of the catalog
	 * @return the sections in the order of the catalog
	 */
	static Map<String, Map<String, String>> sectionsOf(final String catalog)
	{
		Map<String, Map<String, String>> sections = new LinkedHashMap<>();
		Matcher header = SECTION.matcher(catalog);
		List<int[]> bounds = new ArrayList<>();
		List<String> names = new ArrayList<>();
		while (header.find())
		{
			names.add(header.group(1));
			bounds.add(new int[] { header.end(), catalog.length() });
			if (bounds.size() > 1)
			{
				bounds.get(bounds.size() - 2)[1] = header.start();
			}
		}
		for (int index = 0; index < names.size(); index++)
		{
			String body = catalog.substring(bounds.get(index)[0], bounds.get(index)[1]);
			Map<String, String> entries = new LinkedHashMap<>();
			Matcher entry = ENTRY.matcher(body);
			while (entry.find())
			{
				entries.put(entry.group(1), entry.group(2));
			}
			sections.put(names.get(index), entries);
		}
		return sections;
	}

	/**
	 * The accessors the build scripts name outside comments, without the provider calls on them:
	 * {@code libs.versions.jacoco.version.get()} names {@code libs.versions.jacoco.version}
	 */
	private static Set<String> accessorsNamedIn(final List<String> scripts)
	{
		Set<String> named = new TreeSet<>();
		for (String script : scripts)
		{
			String code = script.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
			Matcher accessor = ACCESSOR.matcher(code);
			while (accessor.find())
			{
				List<String> parts = new ArrayList<>(List.of(accessor.group().split("\\.")));
				while (PROVIDER_CALLS.contains(parts.getLast()))
				{
					parts.removeLast();
				}
				named.add(String.join(".", parts));
			}
		}
		return named;
	}

	private static Set<String> used(final Map<String, Map<String, String>> sections,
		final String section, final Set<String> named)
	{
		Set<String> used = new TreeSet<>();
		for (String alias : sections.getOrDefault(section, Map.of()).keySet())
		{
			if (named.contains(ACCESSOR_PREFIX.get(section) + alias.replaceAll("[-_.]", ".")))
			{
				used.add(alias);
			}
		}
		return used;
	}

	private static Set<String> versionsTakenBy(final Map<String, String> entries,
		final Set<String> aliases)
	{
		Set<String> versions = new TreeSet<>();
		if (entries != null)
		{
			aliases.stream().map(entries::get).filter(Objects::nonNull).map(VERSION_REF::matcher)
				.filter(Matcher::find).forEach(ref -> versions.add(ref.group(1)));
		}
		return versions;
	}

	private static List<String> quotedIn(final String value)
	{
		List<String> quoted = new ArrayList<>();
		Matcher matcher = QUOTED.matcher(value);
		while (matcher.find())
		{
			quoted.add(matcher.group(1));
		}
		return quoted;
	}

	record Example(String name, String catalog, String script, List<String> unused) {
		@Override
		public String toString()
		{
			return name;
		}
	}

	private static final String VERSIONS = "[versions]\nfoo-version = \"1\"\n";

	private static final String FOO = "[libraries]\n"
		+ "foo-core = { module = \"org.example:foo-core\", version.ref = \"foo-version\" }\n";

	static Stream<Example> examples()
	{
		return Stream.of(
			new Example("a library named by its accessor is used, and so is its version",
				VERSIONS + FOO, "dependencies { implementation libs.foo.core }", List.of()),
			new Example("a library nothing names is reported, and its version with it",
				VERSIONS + FOO, "dependencies { }",
				List.of("versions foo-version", "libraries foo-core")),
			new Example("a library named only in a comment is reported", VERSIONS + FOO,
				"// implementation libs.foo.core\n/* implementation libs.foo.core */",
				List.of("versions foo-version", "libraries foo-core")),
			new Example("a longer alias does not make its prefix used", VERSIONS + FOO
				+ "foo-core-extra = { module = \"org.example:x\", version.ref = \"foo-version\" }\n",
				"implementation libs.foo.core.extra", List.of("libraries foo-core")),
			new Example("a library named only in a used bundle is used",
				VERSIONS + FOO + "[bundles]\ntesting = [\n    \"foo-core\",\n]\n",
				"testImplementation libs.bundles.testing", List.of()),
			new Example("a bundle nothing names is reported, and its members with it",
				VERSIONS + FOO + "[bundles]\ntesting = [\n    \"foo-core\",\n]\n", "",
				List.of("versions foo-version", "libraries foo-core", "bundles testing")),
			new Example("a plugin applied by its alias is used, and so is its version",
				"[versions]\nbar-version = \"2\"\n[plugins]\n"
					+ "bar-plugin = { id = \"org.example.bar\", version.ref = \"bar-version\" }\n",
				"plugins { alias(libs.plugins.bar.plugin) }", List.of()),
			new Example("a version read through libs.versions is used",
				"[versions]\njacoco-version = \"0.8\"\n",
				"toolVersion = libs.versions.jacoco.version.get()", List.of()));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("examples")
	void theCheck_findsWhatItShould(final Example example)
	{
		assertEquals(example.unused(), unusedEntries(example.catalog(), List.of(example.script())));
	}

	@Test
	@DisplayName("every entry of the version catalog is used by a build script of the host or of a plugin (#1008)")
	void everyCatalogEntryIsUsed(final TestReporter reporter) throws IOException
	{
		assertTrue(Files.isRegularFile(CATALOG), CATALOG + " is where it was");
		String catalog = Files.readString(CATALOG, StandardCharsets.UTF_8);
		List<Path> scriptFiles = buildScripts();
		List<String> scripts = new ArrayList<>();
		for (Path script : scriptFiles)
		{
			scripts.add(Files.readString(script, StandardCharsets.UTF_8));
		}
		Map<String, Map<String, String>> sections = sectionsOf(catalog);
		int aliases = sections.values().stream().mapToInt(Map::size).sum();
		reporter.publishEntry("build scripts read", String.valueOf(scriptFiles.size()));
		reporter.publishEntry("catalog aliases read", String.valueOf(aliases));

		assertTrue(scriptFiles.stream().anyMatch(script -> script.startsWith("plugins")),
			"no plugin build script found among " + scriptFiles.size()
				+ " - an empty set is not a clean result");
		for (String section : List.of("versions", "libraries", "plugins"))
		{
			assertFalse(sections.getOrDefault(section, Map.of()).isEmpty(), "the catalog has no ["
				+ section + "] entries - a check that read nothing passes nothing");
		}
		List<String> unused = unusedEntries(catalog, scripts);
		assertTrue(unused.isEmpty(),
			scriptFiles.size() + " build scripts, " + aliases + " aliases; " + unused.size()
				+ " used by no build script - remove them from the catalog:\n"
				+ String.join("\n", unused));
	}

	/**
	 * The build scripts that read the catalog: the host's at the top and under {@code gradle/}, and
	 * every plugin build's under {@code plugins/<name>/}
	 */
	private static List<Path> buildScripts() throws IOException
	{
		List<Path> scripts = new ArrayList<>();
		for (Map.Entry<Path, Integer> root : Map
			.of(Path.of("."), 1, Path.of("gradle"), 1, Path.of("plugins"), 3).entrySet())
		{
			try (Stream<Path> walk = Files.walk(root.getKey(), root.getValue()))
			{
				walk.filter(Files::isRegularFile)
					.filter(path -> path.toString().endsWith(".gradle")
						|| path.toString().endsWith(".gradle.kts"))
					.filter(path -> !path.toString().contains("/build/"))
					.map(path -> root.getKey().equals(Path.of(".")) ? path.normalize() : path)
					.forEach(scripts::add);
			}
		}
		scripts.sort(null);
		return scripts;
	}
}
