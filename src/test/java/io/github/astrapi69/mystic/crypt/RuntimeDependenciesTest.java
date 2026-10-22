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
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.io.TempDir;
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
 * The same floors are held against an installer archive itself, the published one after a release:
 * {@code ./gradlew test --tests '*.RuntimeDependenciesTest' -PinstallerUnderTest=<installer.jar>}.
 * Its packs are zlib streams; the jars inside are found by their end-of-central-directory record,
 * and a library's version is read from the {@code META-INF/maven/.../pom.properties} the
 * application's jar carries for it. Without an installer named, that check is skipped and says so.
 */
class RuntimeDependenciesTest
{

	/** The end-of-central-directory record of a zip archive, PK\5\6, as a little-endian int */
	private static final int END_OF_CENTRAL_DIRECTORY = 0x06054b50;

	/** A zip archive's local file header, PK\3\4, as a little-endian int */
	private static final int LOCAL_FILE_HEADER = 0x04034b50;

	private static final String INSTALLER = System.getProperty("mystic.crypt.ui.installer", "");

	@TempDir
	Path scratch;

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

	@ParameterizedTest(name = "the installer carries {1} at least {2}")
	@CsvSource({ "com.fasterxml.jackson.core, jackson-core, 2.22.4",
			"com.fasterxml.jackson.core, jackson-databind, 2.22.4",
			"com.fasterxml.jackson.core, jackson-annotations, 2.22",
			"commons-beanutils, commons-beanutils, 1.11.0" })
	void installerArchive_carriesAtLeastThePatchedRelease(final String group, final String artifact,
		final String floor) throws IOException, DataFormatException
	{
		assumeFalse(INSTALLER.isBlank(),
			"no installer named; -PinstallerUnderTest=<installer.jar> names one");

		List<String> versions = versionsInTheInstaller(Path.of(INSTALLER), group, artifact);

		assertEquals(1, versions.size(),
			"exactly one " + artifact + " in " + INSTALLER + ", found " + versions);
		assertTrue(compare(versions.getFirst(), floor) >= 0,
			artifact + " " + versions.getFirst() + " in " + INSTALLER + " is below " + floor);
	}

	private List<String> versionsInTheInstaller(final Path installer, final String group,
		final String artifact) throws IOException, DataFormatException
	{
		String properties = "META-INF/maven/" + group + "/" + artifact + "/pom.properties";
		List<String> versions = new ArrayList<>();
		try (ZipFile archive = new ZipFile(installer.toFile()))
		{
			for (ZipEntry pack : Collections.list(archive.entries()))
			{
				if (pack.getName().startsWith("resources/packs/pack-"))
				{
					try (InputStream in = archive.getInputStream(pack))
					{
						versions
							.addAll(versionsInThePack(inflateAll(in.readAllBytes()), properties));
					}
				}
			}
		}
		return versions;
	}

	private List<String> versionsInThePack(final byte[] pack, final String properties)
		throws IOException
	{
		List<String> versions = new ArrayList<>();
		for (Path jar : embeddedJars(pack))
		{
			try (ZipFile inner = new ZipFile(jar.toFile()))
			{
				ZipEntry entry = inner.getEntry(properties);
				if (entry != null)
				{
					Properties pom = new Properties();
					try (InputStream in = inner.getInputStream(entry))
					{
						pom.load(in);
					}
					versions.add(pom.getProperty("version"));
				}
			}
		}
		return versions;
	}

	/** An izpack pack: one zlib stream after another */
	private static byte[] inflateAll(final byte[] packed) throws DataFormatException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[1 << 16];
		int offset = 0;
		while (offset < packed.length)
		{
			Inflater inflater = new Inflater();
			inflater.setInput(packed, offset, packed.length - offset);
			while (!inflater.finished() && !inflater.needsInput())
			{
				out.write(buffer, 0, inflater.inflate(buffer));
			}
			boolean whole = inflater.finished();
			offset = packed.length - inflater.getRemaining();
			inflater.end();
			if (!whole)
			{
				break;
			}
		}
		return out.toByteArray();
	}

	/**
	 * Every zip archive inside the bytes, found by its end-of-central-directory record, as a file
	 */
	private List<Path> embeddedJars(final byte[] content) throws IOException
	{
		List<Path> jars = new ArrayList<>();
		ByteBuffer little = ByteBuffer.wrap(content).order(ByteOrder.LITTLE_ENDIAN);
		for (int end = 0; end <= content.length - 22; end++)
		{
			if (little.getInt(end) != END_OF_CENTRAL_DIRECTORY)
			{
				continue;
			}
			long directorySize = Integer.toUnsignedLong(little.getInt(end + 12));
			long directoryOffset = Integer.toUnsignedLong(little.getInt(end + 16));
			int comment = Short.toUnsignedInt(little.getShort(end + 20));
			long start = end - directorySize - directoryOffset;
			if (start < 0 || end + 22 + comment > content.length
				|| little.getInt((int)start) != LOCAL_FILE_HEADER)
			{
				continue;
			}
			Path jar = Files.createTempFile(scratch, "embedded", ".jar");
			Files.write(jar, Arrays.copyOfRange(content, (int)start, end + 22 + comment));
			jars.add(jar);
		}
		return jars;
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
