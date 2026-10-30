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
package io.github.astrapi69.mystic.crypt.plugin.releasekey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.UUID;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;

/**
 * The plugin as the installed application runs it (#644): started by pf4j from its zip, in a JVM
 * whose class path is the host's and nothing else. The GnuPG home is read by mystic-crypt's
 * {@code GnuPgHome}, which the host ships together with Bouncy Castle's OpenPGP module, so the zip
 * carries no library of its own (docs/decisions/release-key-openpgp-2026-10-29.md, library first).
 * <p>
 * The plugin's status reading is called by reflection, since this class path has no plugin class to
 * compile against.
 */
class ReleaseKeyPluginFromItsZipTest
{

	private static final String PLUGIN_ID = "release-key-plugin";

	private static final String STATUS = "io.github.astrapi69.mystic.crypt.plugin.releasekey.ReleaseKeyStatus";

	private static final String LIBRARY_READER = "io.github.astrapi69.mystic.crypt.gnupg.GnuPgHome";

	@TempDir
	Path directory;

	@Test
	@DisplayName("the test runs headless and on no display, so it needs no Xvfb and reaches no screen")
	void theTest_runsHeadlessOnNoDisplay()
	{
		assertTrue(GraphicsEnvironment.isHeadless(), "java.awt.headless is set for this task");
		assertNull(System.getenv("DISPLAY"),
			"the build removes the display a headless task would otherwise inherit");
	}

	@Test
	@DisplayName("the plugin started from its zip reads a keybox gpg wrote, through the host's mystic-crypt")
	void thePluginFromItsZip_readsAKeyboxThroughTheLibrary() throws Exception
	{
		Path home = homeWithOneKey();
		Path plugins = Files.createDirectories(directory.resolve("plugins"));
		Path zip = Path.of(System.getProperty("release.key.plugin.zip"));
		Files.copy(zip, plugins.resolve(zip.getFileName()));
		PluginManager manager = new DefaultPluginManager(plugins);
		try
		{
			manager.loadPlugins();
			manager.startPlugins();
			assertEquals(PluginState.STARTED, manager.getPlugin(PLUGIN_ID).getPluginState());
			ClassLoader plugin = manager.getPluginClassLoader(PLUGIN_ID);
			Method read = plugin.loadClass(STATUS).getDeclaredMethod("read", Path.class,
				String.class);
			read.setAccessible(true);

			Object report = read.invoke(null, home, "");

			List<?> certificates = (List<?>)report.getClass().getMethod("certificates")
				.invoke(report);
			assertEquals(1, certificates.size(), "the one certificate gpg wrote: " + report);
			assertEquals(getClass().getClassLoader().loadClass(LIBRARY_READER),
				plugin.loadClass(LIBRARY_READER), "the reader is the host's, from mystic-crypt");
		}
		finally
		{
			manager.stopPlugins();
			manager.unloadPlugins();
			gpg(home, null, "gpgconf", "--homedir", home.toString(), "--kill", "gpg-agent");
		}
	}

	@Test
	@DisplayName("the zip carries the plugin's classes and resources, and no library or licence of its own")
	void theZip_carriesNoLibraryOfItsOwn() throws Exception
	{
		try (ZipFile zip = new ZipFile(System.getProperty("release.key.plugin.zip")))
		{
			List<String> names = zip.stream().map(ZipEntry::getName).toList();

			assertTrue(names.contains("plugin.properties"), "plugin.properties: " + names);
			assertFalse(names.stream().anyMatch(name -> name.startsWith("lib/")),
				"no library under lib/: " + names);
			assertFalse(names.stream().anyMatch(name -> name.startsWith("legal/")),
				"no licence under legal/: " + names);
			assertFalse(
				names.stream()
					.anyMatch(name -> name.startsWith("classes/io/github/astrapi69/mystic/crypt/gnupg/")
						|| name.endsWith("/GnuPgHomeSupport.class")),
				"no reader of its own: " + names);
		}
	}

	/** A GnuPG home with one Ed25519 key, written by gpg into this test's directory */
	private Path homeWithOneKey() throws IOException, InterruptedException
	{
		Path home = Files.createDirectories(directory.resolve("gnupg"));
		Files.setPosixFilePermissions(home, PosixFilePermissions.fromString("rwx------"));
		gpg(home, ("throwaway-" + UUID.randomUUID() + "\n").getBytes(StandardCharsets.UTF_8),
			"gpg", "--homedir", home.toString(), "--batch", "--no-tty", "--pinentry-mode",
			"loopback", "--passphrase-fd", "0", "--quick-gen-key", "Zip Test <zip@example.org>",
			"ed25519", "sign,cert", "0");
		return home;
	}

	private void gpg(final Path home, final byte[] input, final String... command)
		throws IOException, InterruptedException
	{
		ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true)
			.redirectOutput(directory.resolve("gpg.log").toFile());
		builder.environment().put("LC_ALL", "C");
		builder.environment().remove("GNUPGHOME");
		Process process = builder.start();
		try (OutputStream in = process.getOutputStream())
		{
			if (input != null)
			{
				in.write(input);
			}
		}
		assertTrue(process.waitFor(120, TimeUnit.SECONDS), "gpg finished: " + List.of(command));
		assertEquals(0, process.exitValue(),
			"gpg succeeded, see " + directory.resolve("gpg.log") + ": " + List.of(command));
	}
}
