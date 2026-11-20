/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every library the application jar is packed from brings its license files, or has curated ones
 * under {@code legal/third-party/<group>/<artifact>/}, or is allowed here with a reason (#1014).
 * The list is the build's runtime class path, not a list written by hand: a library added later
 * without license files fails this test until its license is read from upstream and kept
 */
class ThirdPartyLicensesTest
{

	/** The curated license files, which the application jar and the installation carry */
	private static final Path CURATED = Path.of("src", "main", "resources", "legal", "third-party");

	/** The group of the family's own modules */
	private static final String FAMILY_GROUP = "io.github.astrapi69";

	/*
	 * The family's own modules (group io.github.astrapi69) need no license files of their own: they
	 * are licensed under the MIT License with the application's holder, so the application's
	 * LICENSE is theirs
	 */

	/** The libraries allowed without license files, each with the reason */
	private static final Map<String, String> ALLOWED = Map.of("com.google.guava:listenablefuture",
		"the jar holds only a manifest and a POM, no class: nothing of it is redistributed in substance");

	private static final Pattern LICENSE_FILE = Pattern.compile("(?i)(licen[sc]e|copying)[^/]*");

	private static final String RUNTIME_MODULES = System
		.getProperty("mystic.crypt.ui.runtime.modules", "");

	private static final String LEGAL_FILE_PATTERNS = System
		.getProperty("mystic.crypt.ui.third.party.legal.files", "");

	/** The result of sorting every runtime module */
	private record Coverage(List<String> brought, List<String> curated, List<String> allowed,
		List<String> missing) {
		int checked()
		{
			return brought.size() + curated.size() + allowed.size() + missing.size();
		}
	}

	@Test
	@DisplayName("every runtime library brings license files, has curated ones, or is allowed with a reason")
	void everyRuntimeLibraryHasItsLicense() throws IOException
	{
		Coverage coverage = coverage();

		System.out
			.println("ThirdPartyLicensesTest: " + coverage.checked() + " runtime modules checked: "
				+ coverage.brought().size() + " bring license files, " + coverage.curated().size()
				+ " are curated under legal/third-party, " + coverage.allowed().size()
				+ " are allowed with a reason, " + coverage.missing().size() + " have none");
		assertTrue(coverage.missing().isEmpty(),
			coverage.missing().size()
				+ " runtime modules ship without license files, have no curated directory "
				+ "legal/third-party/<group>/<artifact>/ with a LICENSE and a SOURCE.txt, and are "
				+ "not allowed in this test: " + coverage.missing());
	}

	@Test
	@DisplayName("a curated directory names its license and where it was read, and an LGPL library how to replace it")
	void curatedDirectoriesAreComplete() throws IOException
	{
		List<String> problems = new ArrayList<>();
		List<Path> directories = curatedDirectories();
		assertFalse(directories.isEmpty(),
			"no curated directory: an empty set is not a clean result");
		int filesChecked = 0;

		for (Path directory : directories)
		{
			String module = CURATED.relativize(directory).toString();
			List<Path> files = filesIn(directory);
			filesChecked += files.size();
			boolean hasLicense = files.stream()
				.anyMatch(file -> LICENSE_FILE.matcher(file.getFileName().toString()).matches());
			if (!hasLicense)
			{
				problems.add(module + " has no license file");
			}
			Path source = directory.resolve("SOURCE.txt");
			if (!Files.isRegularFile(source) || !read(source).contains("http"))
			{
				problems.add(module + " has no SOURCE.txt naming the URL it was read from");
			}
			if (isLgpl(files) && !hasReplacementNote(directory))
			{
				problems.add(module + " is LGPL and has no REPLACING.txt saying how to replace it");
			}
		}
		System.out.println("ThirdPartyLicensesTest: checked " + directories.size()
			+ " curated directories holding " + filesChecked + " files");
		assertTrue(problems.isEmpty(), problems.toString());
	}

	@Test
	@DisplayName("every curated directory and every allowed library belongs to a library of the runtime class path")
	void noCuratedDirectoryOrAllowedEntryIsLeftOver() throws IOException
	{
		Map<String, File> modules = runtimeModules();
		List<String> leftOver = new ArrayList<>();

		for (Path directory : curatedDirectories())
		{
			String module = moduleOf(directory);
			if (!modules.containsKey(module))
			{
				leftOver.add("legal/third-party/" + CURATED.relativize(directory)
					+ " is the license of a library that is no longer shipped");
			}
		}
		for (String allowed : ALLOWED.keySet())
		{
			if (!modules.containsKey(allowed) || bringsLicenseFiles(modules.get(allowed)))
			{
				leftOver.add(allowed + " is allowed here but is not shipped, or now brings its "
					+ "license files: " + ALLOWED.get(allowed));
			}
		}
		assertTrue(leftOver.isEmpty(), leftOver.toString());
	}

	@Test
	@DisplayName("the application jar carries the curated license files")
	void theApplicationJarCarriesTheCuratedFiles() throws IOException
	{
		ApplicationJarUnderTest.assumeNamed();
		List<String> missing = new ArrayList<>();
		int entriesChecked = 0;

		try (ZipFile jar = new ZipFile(ApplicationJarUnderTest.PATH))
		{
			for (Path directory : curatedDirectories())
			{
				for (Path file : filesIn(directory))
				{
					entriesChecked++;
					String entry = "legal/third-party/"
						+ CURATED.relativize(file).toString().replace(File.separatorChar, '/');
					if (jar.getEntry(entry) == null)
					{
						missing.add(entry);
					}
				}
			}
		}
		System.out.println("ThirdPartyLicensesTest: looked for " + entriesChecked
			+ " curated entries in " + ApplicationJarUnderTest.PATH);
		assertTrue(missing.isEmpty(), ApplicationJarUnderTest.PATH + " lacks " + missing);
	}

	private static Coverage coverage() throws IOException
	{
		Map<String, File> modules = runtimeModules();
		Map<String, Path> curated = new TreeMap<>();
		for (Path directory : curatedDirectories())
		{
			curated.put(moduleOf(directory), directory);
		}
		List<String> brought = new ArrayList<>();
		List<String> curatedModules = new ArrayList<>();
		List<String> allowed = new ArrayList<>();
		List<String> missing = new ArrayList<>();
		for (Map.Entry<String, File> module : modules.entrySet())
		{
			String name = module.getKey();
			if (bringsLicenseFiles(module.getValue()))
			{
				brought.add(name);
			}
			else if (curated.containsKey(name) && hasLicenseAndSource(curated.get(name)))
			{
				curatedModules.add(name);
			}
			else if (name.startsWith(FAMILY_GROUP + ":") || ALLOWED.containsKey(name))
			{
				allowed.add(name);
			}
			else
			{
				missing.add(name);
			}
		}
		return new Coverage(brought, curatedModules, allowed, missing);
	}

	/**
	 * The runtime class path of this build as {@code group:artifact} to the library jar. Fails
	 * closed when the build names none
	 */
	private static Map<String, File> runtimeModules()
	{
		assertFalse(RUNTIME_MODULES.isBlank(), "the build names no runtime modules in "
			+ "mystic.crypt.ui.runtime.modules; without them nothing can be checked");
		Map<String, File> modules = new TreeMap<>();
		for (String entry : RUNTIME_MODULES.split("\\|"))
		{
			int equals = entry.indexOf('=');
			assertTrue(equals > 0, "not group:artifact=jar: " + entry);
			modules.put(entry.substring(0, equals), new File(entry.substring(equals + 1)));
		}
		return modules;
	}

	/**
	 * The build's own criterion, from gradle/packaging.gradle: the jar has an entry the patterns of
	 * {@code thirdPartyLegalFiles} match, which the build copies to META-INF/third-party/
	 */
	private static boolean bringsLicenseFiles(final File library)
	{
		assertFalse(LEGAL_FILE_PATTERNS.isBlank(), "the build names no license file patterns in "
			+ "mystic.crypt.ui.third.party.legal.files");
		List<PathMatcher> matchers = Arrays.stream(LEGAL_FILE_PATTERNS.split(","))
			.map(pattern -> FileSystems.getDefault().getPathMatcher("glob:" + pattern)).toList();
		try (ZipFile archive = new ZipFile(library))
		{
			return archive.stream().filter(entry -> !entry.isDirectory())
				.map(entry -> Path.of(entry.getName()))
				.anyMatch(name -> matchers.stream().anyMatch(matcher -> matcher.matches(name)));
		}
		catch (IOException e)
		{
			throw new IllegalStateException("cannot read " + library, e);
		}
	}

	/** The group/artifact directories of the curated tree, as {@code <group>/<artifact>} paths */
	private static List<Path> curatedDirectories() throws IOException
	{
		assertTrue(Files.isDirectory(CURATED), CURATED.toAbsolutePath() + " is a directory");
		try (Stream<Path> groups = Files.list(CURATED))
		{
			List<Path> directories = new ArrayList<>();
			for (Path group : groups.filter(Files::isDirectory).sorted().toList())
			{
				try (Stream<Path> artifacts = Files.list(group))
				{
					artifacts.filter(Files::isDirectory).sorted().forEach(directories::add);
				}
			}
			return directories;
		}
	}

	private static String moduleOf(final Path directory)
	{
		Path relative = CURATED.relativize(directory);
		return relative.getName(0) + ":" + relative.getName(1);
	}

	private static List<Path> filesIn(final Path directory) throws IOException
	{
		try (Stream<Path> files = Files.walk(directory))
		{
			return files.filter(Files::isRegularFile).sorted().toList();
		}
	}

	private static boolean hasLicenseAndSource(final Path directory) throws IOException
	{
		return Files.isRegularFile(directory.resolve("SOURCE.txt")) && filesIn(directory).stream()
			.anyMatch(file -> LICENSE_FILE.matcher(file.getFileName().toString()).matches());
	}

	private static boolean isLgpl(final List<Path> files) throws IOException
	{
		for (Path file : files)
		{
			if (read(file).toUpperCase().contains("GNU LESSER GENERAL PUBLIC LICENSE"))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean hasReplacementNote(final Path directory) throws IOException
	{
		Path note = directory.resolve("REPLACING.txt");
		return Files.isRegularFile(note) && read(note).toLowerCase().contains("replace");
	}

	private static String read(final Path file) throws IOException
	{
		return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
	}
}
