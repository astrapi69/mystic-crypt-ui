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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The checks of an installer before a release is tagged (#793): {@code scripts/installer-pack.py},
 * which reads what the installer packs, and {@code scripts/verify-installer.sh}, which fails when
 * any check failed or could not run. The installers here are built in the test the way IzPack packs
 * its files, zlib streams one after another in {@code resources/packs/<pack>}; none of them is
 * signed or large, so no check that needs the application jar, the network or a build runs
 */
class VerifyInstallerTest
{

	private static final File PACK = new File("scripts/installer-pack.py").getAbsoluteFile();

	private static final File VERIFY = new File("scripts/verify-installer.sh").getAbsoluteFile();

	private static final String APPLICATION_CLASS = "io/github/astrapi69/mystic/crypt/StartMysticCryptApplication.class";

	/** A line a check prints, "ok <check>: ..." or "FAILED <check>: ...", with the check's name */
	private static final Pattern CHECK_LINE = Pattern.compile("^(?:ok|FAILED) ([^:]+): ");

	@TempDir
	Path directory;

	@Test
	@DisplayName("the application jar is the zip in pack-executables that holds the start class")
	void applicationJar_isTakenFromTheExecutablesPack() throws Exception
	{
		byte[] applicationJar = applicationJar("9.9");
		Path installer = installer(List.of("a launcher script".getBytes(StandardCharsets.UTF_8),
			zip("lib/other.txt", "not the application"), applicationJar), List.of());
		Path target = directory.resolve("app.jar");

		Run run = run("python3", "-I", PACK.getPath(), "application-jar", installer.toString(),
			target.toString(), "9.9");

		assertEquals(0, run.exit(), run.output());
		assertArrayEquals(applicationJar, Files.readAllBytes(target));
	}

	@Test
	@DisplayName("an application jar of another version is refused and its version named")
	void applicationJar_isRefused_whenItDeclaresAnotherVersion() throws Exception
	{
		Path installer = installer(List.of(applicationJar("9.8")), List.of());

		Run run = run("python3", "-I", PACK.getPath(), "application-jar", installer.toString(),
			directory.resolve("app.jar").toString(), "9.9");

		assertNotEquals(0, run.exit(), run.output());
		assertTrue(run.output().contains("['9.8'], not 9.9"), run.output());
		assertFalse(Files.exists(directory.resolve("app.jar")));
	}

	@Test
	@DisplayName("the plugins are the plugin.id of every plugin zip in pack-plugins")
	void plugins_areNamedByTheirPluginId() throws Exception
	{
		Path installer = installer(List.of(),
			List.of(zip("plugin.properties", "plugin.id=checksum-plugin\nplugin.version=1.0.0\n"),
				"readme".getBytes(StandardCharsets.UTF_8),
				zip("plugin.properties", "plugin.id=keygen-plugin\n")));

		Run run = run("python3", "-I", PACK.getPath(), "plugins", installer.toString());

		assertEquals(0, run.exit(), run.output());
		assertEquals("checksum-plugin\nkeygen-plugin\n", run.output());
	}

	@Test
	@DisplayName("no installer is a failure that names the file it looked for")
	void verify_fails_whenThereIsNoInstaller() throws Exception
	{
		Path missing = directory.resolve("mystic-crypt-ui-9.9-installer.jar");

		Run run = run(VERIFY.getPath(), "9.9", "--installer", missing.toString());

		assertNotEquals(0, run.exit(), run.output());
		assertTrue(run.output().contains(missing.toString()), run.output());
	}

	@Test
	@DisplayName("a small installer is not refused for its size: every check but a size check reports")
	void verify_checksNoSize_whenTheInstallerIsSmall() throws Exception
	{
		Path installer = installer(List.of(), List.of());

		Run run = run(VERIFY.getPath(), "9.9", "--installer", installer.toString());

		List<String> reported = run.output().lines().map(CHECK_LINE::matcher).filter(Matcher::find)
			.map(matcher -> matcher.group(1)).toList();
		assertEquals(List.of("archive", "sha256", "sha512", "application jar", "signature",
			"class major", "mystic-crypt", "plugins", "jar tests", "start"), reported,
			run.output());
	}

	@ParameterizedTest(name = "an installer without resources/packs/{0} fails the check {1}")
	@CsvSource({ "pack-plugins, plugins", "pack-executables, application jar" })
	void verify_failsTheCheckThatReadsThePack_whenTheInstallerHasNoSuchPack(final String missing,
		final String check) throws Exception
	{
		Map<String, List<byte[]>> packs = new LinkedHashMap<>();
		packs.put("pack-executables",
			List.of("a launcher script".getBytes(StandardCharsets.UTF_8)));
		packs.put("pack-plugins", List.of(zip("plugin.properties", "plugin.id=checksum-plugin\n")));
		packs.remove(missing);
		Path installer = installer(packs);

		Run run = run(VERIFY.getPath(), "9.9", "--installer", installer.toString());

		assertNotEquals(0, run.exit(), run.output());
		String expected = "FAILED " + check + ": " + installer + " holds no resources/packs/"
			+ missing;
		assertTrue(run.output().lines().anyMatch(expected::equals), run.output());
	}

	@Test
	@DisplayName("a check that fails or cannot run is reported FAILED, and the run fails")
	void verify_reportsEveryCheckItCouldNotPass() throws Exception
	{
		Path installer = installer(
			List.of("no application jar in here".getBytes(StandardCharsets.UTF_8)),
			List.of(zip("plugin.properties", "plugin.id=checksum-plugin\n")));
		Files.writeString(Path.of(installer + ".sha256"),
			"0".repeat(64) + "  " + installer.getFileName() + "\n");
		Files.writeString(Path.of(installer + ".sha512"),
			"0".repeat(128) + "  " + installer.getFileName() + "\n");

		Run run = run(VERIFY.getPath(), "9.9", "--installer", installer.toString());

		assertNotEquals(0, run.exit(), run.output());
		for (String check : List.of("sha256", "sha512", "application jar", "signature",
			"class major", "mystic-crypt", "jar tests", "start"))
		{
			assertTrue(run.output().lines().anyMatch(line -> line.startsWith("FAILED " + check)),
				check + " is not reported FAILED:\n" + run.output());
		}
		assertTrue(run.output().lines().anyMatch(line -> line.startsWith("ok archive")),
			"the archive opens:\n" + run.output());
		assertFalse(run.output().contains("ok sha256"), run.output());
	}

	private byte[] applicationJar(final String version) throws IOException
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipOutputStream jar = new ZipOutputStream(bytes))
		{
			jar.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
			jar.write(("Manifest-Version: 1.0\r\nImplementation-Version: " + version + "\r\n\r\n")
				.getBytes(StandardCharsets.UTF_8));
			jar.putNextEntry(new ZipEntry(APPLICATION_CLASS));
			jar.write(new byte[] { (byte)0xca, (byte)0xfe, (byte)0xba, (byte)0xbe, 0, 0, 0, 69 });
		}
		return bytes.toByteArray();
	}

	private static byte[] zip(final String name, final String content) throws IOException
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(bytes))
		{
			zip.putNextEntry(new ZipEntry(name));
			zip.write(content.getBytes(StandardCharsets.UTF_8));
		}
		return bytes.toByteArray();
	}

	/** An installer whose packs hold the given files as zlib streams one after another */
	private Path installer(final List<byte[]> executables, final List<byte[]> plugins)
		throws IOException
	{
		Map<String, List<byte[]>> packs = new LinkedHashMap<>();
		packs.put("pack-executables", executables);
		packs.put("pack-plugins", plugins);
		return installer(packs);
	}

	/** An installer with only the given packs, by their entry name under resources/packs/ */
	private Path installer(final Map<String, List<byte[]>> packs) throws IOException
	{
		Path installer = directory.resolve("mystic-crypt-ui-9.9-installer.jar");
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(installer)))
		{
			for (Map.Entry<String, List<byte[]>> pack : packs.entrySet())
			{
				zip.putNextEntry(new ZipEntry("resources/packs/" + pack.getKey()));
				zip.write(streams(pack.getValue()));
			}
		}
		return installer;
	}

	private static byte[] streams(final List<byte[]> files)
	{
		ByteArrayOutputStream pack = new ByteArrayOutputStream();
		for (byte[] file : files)
		{
			Deflater deflater = new Deflater();
			deflater.setInput(file);
			deflater.finish();
			byte[] buffer = new byte[8192];
			while (!deflater.finished())
			{
				pack.write(buffer, 0, deflater.deflate(buffer));
			}
			deflater.end();
		}
		return pack.toByteArray();
	}

	private Run run(final String... command) throws Exception
	{
		List<String> arguments = new ArrayList<>(List.of(command));
		Process process = new ProcessBuilder(arguments).redirectErrorStream(true).start();
		assertTrue(process.waitFor(120, TimeUnit.SECONDS), "the command ended: " + arguments);
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		return new Run(process.exitValue(), output);
	}

	private record Run(int exit, String output) {
	}
}
