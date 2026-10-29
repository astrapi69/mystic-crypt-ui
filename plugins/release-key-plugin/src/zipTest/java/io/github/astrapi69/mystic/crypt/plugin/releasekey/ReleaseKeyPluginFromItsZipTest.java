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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;

/**
 * The plugin as the installed application runs it (#644): started by pf4j from its zip, in a JVM
 * whose class path carries the host and not Bouncy Castle's OpenPGP module. In the end-to-end suite
 * that module is on the application's class path, where pf4j's plugin class loader would fall back
 * to it, so a zip without it would pass there and fail for every user. Here it cannot.
 * <p>
 * The plugin's reader is called by reflection, since this class path has no plugin class to
 * compile against.
 */
class ReleaseKeyPluginFromItsZipTest
{

	private static final String PLUGIN_ID = "release-key-plugin";

	private static final String READER = "io.github.astrapi69.mystic.crypt.plugin.releasekey.GnuPgHomeSupport";

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
	@DisplayName("the plugin started from its zip reads a keybox gpg wrote, with the bcpg it carries itself")
	void thePluginFromItsZip_readsAKeyboxWithItsOwnBcpg() throws Exception
	{
		assertThrows(ClassNotFoundException.class,
			() -> Class.forName("org.bouncycastle.gpg.keybox.bc.BcKeyBox", false,
				getClass().getClassLoader()),
			"precondition: bcpg is not on the application's class path");
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

			List<?> certificates = (List<?>)plugin.loadClass(READER).getMethod("read", Path.class)
				.invoke(null, home);

			assertEquals(1, certificates.size(), "the one certificate gpg wrote: " + certificates);
			URL from = plugin.loadClass("org.bouncycastle.gpg.keybox.bc.BcKeyBox")
				.getProtectionDomain().getCodeSource().getLocation();
			// real paths: macOS reaches its temporary directory through a symbolic link
			assertTrue(Path.of(from.toURI()).toRealPath().startsWith(plugins.toRealPath()),
				"bcpg came from the plugin, from " + from);
		}
		finally
		{
			manager.stopPlugins();
			manager.unloadPlugins();
			gpg(home, null, "gpgconf", "--homedir", home.toString(), "--kill", "gpg-agent");
		}
	}

	@Test
	@DisplayName("the zip carries the licence of the library it bundles, unchanged from that library's jar")
	void theZip_carriesTheLicenceOfWhatItBundles() throws Exception
	{
		try (ZipFile zip = new ZipFile(System.getProperty("release.key.plugin.zip")))
		{
			String notice = new String(entry(zip, "legal/NOTICE.txt"), StandardCharsets.UTF_8);
			List<String> libraries = zip.stream().map(ZipEntry::getName)
				.filter(name -> name.startsWith("lib/") && name.endsWith(".jar")).toList();
			assertEquals(1, libraries.size(), "the zip bundles bcpg and nothing else: " + libraries);
			String bcpg = libraries.get(0);
			assertTrue(bcpg.startsWith("lib/bcpg-jdk18on-"), bcpg);
			assertTrue(notice.contains("lib/bcpg-jdk18on-<version>.jar"), "NOTICE.txt names it");
			assertArrayEquals(entryOfJar(zip, bcpg, "META-INF/LICENSE.md"),
				entry(zip, "legal/bcpg-jdk18on/LICENSE.md"));
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

	private static byte[] entry(final ZipFile zip, final String name) throws IOException
	{
		ZipEntry found = zip.getEntry(name);
		assertNotNull(found, name + " is in the zip");
		try (InputStream in = zip.getInputStream(found))
		{
			return in.readAllBytes();
		}
	}

	/** An entry of a jar that is itself an entry of the zip */
	private static byte[] entryOfJar(final ZipFile zip, final String jar, final String name)
		throws IOException
	{
		try (ZipInputStream in = new ZipInputStream(zip.getInputStream(zip.getEntry(jar))))
		{
			for (ZipEntry each = in.getNextEntry(); each != null; each = in.getNextEntry())
			{
				if (each.getName().equals(name))
				{
					return in.readAllBytes();
				}
			}
		}
		throw new AssertionError(name + " is not in " + jar);
	}
}
