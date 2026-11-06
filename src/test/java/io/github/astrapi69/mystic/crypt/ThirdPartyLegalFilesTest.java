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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The application jar carries the licence and notice files of every library it is packed from, each
 * under {@value #THIRD_PARTY}{@code <group>/<artifact>/} as the library carries it (#779).
 * <p>
 * The all-dependencies jar keeps one copy of each path, the first library's, and the libraries
 * share their paths: 27 of them carry a {@code META-INF/NOTICE}, in eight different texts, and the
 * jar kept log4j-to-slf4j's. Jackson's, Batik's and Apache Commons' notices were not in it, and the
 * installer ships nothing else - the Apache License 2.0 asks that a redistribution carries the
 * notices of a NOTICE file (section 4 (d)).
 * <p>
 * A licence or notice file is one at the root of a library, at the top of its {@code META-INF} or
 * under its {@code license} directory, named as {@link #LEGAL_FILE} says. A file of that name in a
 * package is a resource of the library's code and stays where the code looks for it.
 * <p>
 * Checked against the libraries of this build's runtime class path, which the build names in
 * {@code mystic.crypt.ui.runtime.libraries}; the jar under test is named with
 * {@code -PapplicationJarUnderTest=<jar>}, see {@link ApplicationJarUnderTest}.
 */
class ThirdPartyLegalFilesTest
{

	private static final String THIRD_PARTY = "META-INF/third-party/";

	/** The application's own licence, which stands at the root of the jar (#773) */
	private static final String APPLICATIONS_LICENCE = "LICENSE.txt";

	private static final Pattern LEGAL_FILE = Pattern
		.compile("(LICENSE|NOTICE)[^/]*" + "|META-INF/[^/]*(LICENSE|NOTICE)[^/]*"
			+ "|META-INF/(COPYING|DEPENDENCIES|AUTHORS)[^/]*" + "|license/.+");

	private static final String RUNTIME_LIBRARIES = System
		.getProperty("mystic.crypt.ui.runtime.libraries", "");

	/**
	 * Holds the wiring the jar checks depend on, and runs without a jar named: the build passes the
	 * runtime class path, every entry of it a library jar that exists
	 */
	@Test
	@DisplayName("the build names the runtime libraries the jar is compared with")
	void runtimeLibraries_areNamedByTheBuild()
	{
		List<File> libraries = runtimeLibraries();

		assertFalse(libraries.isEmpty(), "no runtime library named");
		assertEquals(
			List.of(), libraries.stream()
				.filter(file -> !file.isFile() || !file.getName().endsWith(".jar")).toList(),
			"runtime class path entries that are no library jar");
	}

	@Test
	@DisplayName("every library's licence and notice files are in the jar, byte for byte, under its own name")
	void applicationJar_carriesTheLegalFilesOfEveryLibrary() throws IOException
	{
		ApplicationJarUnderTest.assumeNamed();
		Path jar = Path.of(ApplicationJarUnderTest.PATH);
		List<String> missing = new ArrayList<>();
		int checked = 0;

		try (ZipFile archive = new ZipFile(jar.toFile()))
		{
			Map<String, File> libraries = librariesOf(
				ApplicationJarUnderTest.runtimeVersionsIn(archive, jar));
			for (Map.Entry<String, File> library : libraries.entrySet())
			{
				try (ZipFile own = new ZipFile(library.getValue()))
				{
					for (ZipEntry entry : legalFilesIn(own))
					{
						checked++;
						String path = THIRD_PARTY + library.getKey().replace(':', '/') + "/"
							+ entry.getName();
						ZipEntry packed = archive.getEntry(path);
						if (packed == null
							|| !Arrays.equals(bytes(own, entry), bytes(archive, packed)))
						{
							missing.add(path);
						}
					}
				}
			}
		}

		assertTrue(checked > 0, "no library carries a licence or notice file - "
			+ "an empty set is not a clean result");
		assertEquals(List.of(), missing, "of " + checked + " licence and notice files of the "
			+ "libraries, these are not in " + jar + " as the library carries them (#779)");
	}

	@Test
	@DisplayName("no licence or notice file of a library stands where the application's own do")
	void applicationJar_carriesNoLegalFileOfALibraryOutsideItsOwnDirectory() throws IOException
	{
		ApplicationJarUnderTest.assumeNamed();

		try (ZipFile archive = new ZipFile(ApplicationJarUnderTest.PATH))
		{
			List<String> elsewhere = legalFilesIn(archive).stream().map(ZipEntry::getName)
				.filter(name -> !name.startsWith(THIRD_PARTY))
				.filter(name -> !name.equals(APPLICATIONS_LICENCE)).toList();

			assertEquals(List.of(), elsewhere, ApplicationJarUnderTest.PATH
				+ " carries licence and notice files of whichever library came first (#779)");
		}
	}

	/**
	 * The library jar of each module the application jar names, from the runtime class path the
	 * build passes in; a module without exactly one jar of its name and version there cannot be
	 * checked, and fails the test rather than leaving it out
	 */
	private static Map<String, File> librariesOf(final Map<String, String> runtimeVersions)
	{
		List<File> classPath = runtimeLibraries();
		Map<String, File> libraries = new TreeMap<>();
		List<String> unmatched = new ArrayList<>();
		runtimeVersions.forEach((module, version) -> {
			String fileName = module.substring(module.indexOf(':') + 1) + "-" + version + ".jar";
			List<File> jars = classPath.stream().filter(file -> file.getName().equals(fileName))
				.toList();
			if (jars.size() == 1)
			{
				libraries.put(module, jars.getFirst());
			}
			else
			{
				unmatched.add(module + "=" + version + " " + jars);
			}
		});
		assertEquals(List.of(), unmatched,
			"modules of the jar without exactly one jar of theirs on the runtime class path");
		return libraries;
	}

	private static List<File> runtimeLibraries()
	{
		assertFalse(RUNTIME_LIBRARIES.isBlank(), "the build names no runtime libraries in "
			+ "mystic.crypt.ui.runtime.libraries; without them nothing can be compared");
		return Arrays.stream(RUNTIME_LIBRARIES.split(File.pathSeparator)).map(File::new).toList();
	}

	private static List<? extends ZipEntry> legalFilesIn(final ZipFile archive)
	{
		return archive.stream().filter(entry -> !entry.isDirectory())
			.filter(entry -> LEGAL_FILE.matcher(entry.getName()).matches()).toList();
	}

	private static byte[] bytes(final ZipFile archive, final ZipEntry entry) throws IOException
	{
		try (InputStream in = archive.getInputStream(entry))
		{
			return in.readAllBytes();
		}
	}
}
