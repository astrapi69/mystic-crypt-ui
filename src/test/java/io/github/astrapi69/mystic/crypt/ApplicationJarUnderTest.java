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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The application jar a test checks, named with {@code -PapplicationJarUnderTest=<jar>}: the
 * all-dependencies jar of this build, or the one {@code scripts/fetch-release-jar.sh} takes out of
 * a published installer (#617). Without a jar named, the checks that read one are skipped and say
 * so
 */
final class ApplicationJarUnderTest
{

	/** The jar under test, empty when none is named */
	static final String PATH = System.getProperty("mystic.crypt.ui.application.jar", "");

	/**
	 * Where the build names the version of every module the application jar is packed from (#769)
	 */
	static final String RUNTIME_VERSIONS = "META-INF/mystic-crypt-ui/runtime-versions.txt";

	private static final String RUNTIME_LIBRARIES = System
		.getProperty("mystic.crypt.ui.runtime.libraries", "");

	private ApplicationJarUnderTest()
	{
	}

	/** Skips the calling test when no application jar is named */
	static void assumeNamed()
	{
		assumeFalse(PATH.isBlank(),
			"no application jar named; -PapplicationJarUnderTest=<jar> names one");
	}

	/**
	 * The libraries of this build's runtime class path, in class path order, which the build passes
	 * to the test JVM in {@code mystic.crypt.ui.runtime.libraries} (#779): what the application jar
	 * of this build is packed from. The test class path holds other versions of some of them
	 *
	 * @return the library jars
	 */
	static List<File> runtimeLibraries()
	{
		assertFalse(RUNTIME_LIBRARIES.isBlank(), "the build names no runtime libraries in "
			+ "mystic.crypt.ui.runtime.libraries; without them nothing can be compared");
		return Arrays.stream(RUNTIME_LIBRARIES.split(File.pathSeparator)).map(File::new).toList();
	}

	/**
	 * The lines {@code group:artifact=version} of {@value #RUNTIME_VERSIONS}, as a map from
	 * {@code group:artifact} to the version
	 *
	 * @param archive
	 *            the opened application jar
	 * @param jar
	 *            its path, for the messages
	 * @return the version of every module the jar is packed from
	 * @throws IOException
	 *             if the list cannot be read
	 */
	static Map<String, String> runtimeVersionsIn(final ZipFile archive, final Path jar)
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
}
