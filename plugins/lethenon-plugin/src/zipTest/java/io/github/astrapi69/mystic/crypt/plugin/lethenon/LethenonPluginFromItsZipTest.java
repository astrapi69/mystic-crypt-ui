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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.TempDirDeletionStrategy;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;

/**
 * The plugin as the installed application runs it (#581): started by pf4j from its zip, in a JVM
 * whose class path carries the host and nothing of the plugin's own - neither lethenon nor
 * sqlite-jdbc. In the end-to-end suite both are on the application's class path, where pf4j's
 * plugin class loader would fall back to them, so a plugin zip without sqlite-jdbc would pass
 * there and fail for every user. Here it cannot.
 * <p>
 * The chain is written and read through the classes the plugin's class loader finds, by
 * reflection, since this class path has no lethenon to compile against.
 */
class LethenonPluginFromItsZipTest
{

	private static final String PLUGIN_ID = "lethenon-plugin";

	/**
	 * On Windows the plugin's sqlite-jdbc jar stays open after the plugin was unloaded, and its
	 * deletion fails while everything else goes (#583); the failure is ignored, not the test
	 */
	@TempDir(deletionStrategy = TempDirDeletionStrategy.IgnoreFailures.class)
	Path directory;

	@Test
	@DisplayName("the test runs headless and on no display, so it needs no Xvfb and reaches no screen (#581, #505)")
	void theTest_runsHeadlessOnNoDisplay()
	{
		assertTrue(GraphicsEnvironment.isHeadless(), "java.awt.headless is set for this task");
		assertNull(System.getenv("DISPLAY"),
			"the build removes the display a headless task would otherwise inherit");
	}

	@Test
	@DisplayName("the plugin started from its zip keeps a chain in SQLite with the sqlite-jdbc it carries itself (#581)")
	void thePluginFromItsZip_keepsAChainWithItsOwnSqlite() throws Exception
	{
		ClassLoader application = getClass().getClassLoader();
		assertThrows(ClassNotFoundException.class,
			() -> Class.forName("org.sqlite.JDBC", false, application),
			"precondition: sqlite-jdbc is not on the application's class path");
		assertThrows(ClassNotFoundException.class,
			() -> Class.forName("io.github.astrapi69.lethenon.ChainFile", false, application),
			"precondition: lethenon is not on the application's class path");
		Path plugins = Files.createDirectories(directory.resolve("plugins"));
		Path zip = Path.of(System.getProperty("lethenon.plugin.zip"));
		Files.copy(zip, plugins.resolve(zip.getFileName()));
		PluginManager manager = new DefaultPluginManager(plugins);
		try
		{
			manager.loadPlugins();
			manager.startPlugins();
			assertEquals(PluginState.STARTED, manager.getPlugin(PLUGIN_ID).getPluginState());
			ClassLoader plugin = manager.getPluginClassLoader(PLUGIN_ID);

			Path chainFile = directory.resolve("from-the-zip.lethenon");
			List<?> read = writeAndRead(plugin, chainFile);

			assertEquals(1, read.size(), "the genesis block, read back");
			assertArrayEquals("SQLite format 3\0".getBytes(StandardCharsets.US_ASCII),
				Arrays.copyOf(Files.readAllBytes(chainFile), 16), "the chain is an SQLite database");
			URL from = plugin.loadClass("org.sqlite.JDBC").getProtectionDomain().getCodeSource()
				.getLocation();
			// real paths: macOS reaches its temporary directory through a symbolic link, Windows
			// through a short name, and the class loader names the jar by the other one
			assertTrue(Path.of(from.toURI()).toRealPath().startsWith(plugins.toRealPath()),
				"sqlite-jdbc came from the plugin, from " + from);
		}
		finally
		{
			manager.stopPlugins();
			manager.unloadPlugins();
		}
	}

	/** A test chain's genesis block, written by the plugin's lethenon and read back by it */
	private static List<?> writeAndRead(final ClassLoader plugin, final Path chainFile)
		throws ReflectiveOperationException
	{
		String test = (String)plugin.loadClass("io.github.astrapi69.lethenon.Chain")
			.getField("TEST_IDENTIFIER").get(null);
		Object genesis = plugin.loadClass("io.github.astrapi69.lethenon.Genesis")
			.getMethod("start", String.class, String.class, long.class)
			.invoke(null, test, "started from the zip", System.currentTimeMillis());
		Class<?> files = plugin.loadClass("io.github.astrapi69.lethenon.ChainFile");
		Object chain = files.getConstructor(Path.class).newInstance(chainFile);
		files.getMethod("write", List.class).invoke(chain, List.of(genesis));
		return (List<?>)files.getMethod("read").invoke(chain);
	}
}
