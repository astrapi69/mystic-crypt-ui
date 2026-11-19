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
package io.github.astrapi69.mystic.crypt.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.pf4j.DefaultPluginManager;

/**
 * What installing a chosen zip means for the plugins that are loaded, decided by a real plugin
 * manager over a plugins directory, before anything of an installed plugin is touched (#1045,
 * #1060)
 */
class PluginZipInstallationTest
{

	private static final String INSTALLED = "demo-plugin-1.0.0.zip";

	@TempDir
	Path pluginsRoot;

	@TempDir
	Path chosen;

	private DefaultPluginManager pluginManager;

	@BeforeEach
	void loadTheInstalledPlugin() throws IOException
	{
		writeZip(pluginsRoot.resolve(INSTALLED), "demo-plugin", "1.0.0");
		pluginManager = new DefaultPluginManager(pluginsRoot);
		pluginManager.loadPlugins();
		assertNotNull(pluginManager.getPlugin("demo-plugin"), "the precondition: it is loaded");
	}

	@Test
	@DisplayName("the zip of a plugin that is not loaded is a new plugin")
	void theZipOfAPluginThatIsNotLoaded_isANewPlugin() throws Exception
	{
		Path zip = writeZip(chosen.resolve("new-plugin-1.0.0.zip"), "new-plugin", "1.0.0");

		PluginZipInstallation.Decision decision = PluginZipInstallation.decide(pluginManager, zip,
			pluginsRoot.resolve(zip.getFileName()));

		assertEquals(PluginZipInstallation.Kind.NEW_PLUGIN, decision.kind());
		assertEquals("new-plugin", decision.pluginId());
		assertNull(decision.loaded());
	}

	@Test
	@DisplayName("a zip of the loaded plugin under the name it was installed with is an update of it")
	void aZipOfTheLoadedPlugin_isAnUpdateOfIt() throws Exception
	{
		Path zip = writeZip(chosen.resolve(INSTALLED), "demo-plugin", "1.0.1");

		PluginZipInstallation.Decision decision = PluginZipInstallation.decide(pluginManager, zip,
			pluginsRoot.resolve(INSTALLED));

		assertEquals(PluginZipInstallation.Kind.UPDATE_OF_LOADED, decision.kind());
		assertEquals(pluginManager.getPlugin("demo-plugin"), decision.loaded());
	}

	@Test
	@DisplayName("the zip of another plugin under the name of the loaded one is refused")
	void theZipOfAnotherPlugin_isRefused() throws Exception
	{
		Path zip = writeZip(chosen.resolve(INSTALLED), "other-plugin", "1.0.0");

		PluginZipInstallation.Decision decision = PluginZipInstallation.decide(pluginManager, zip,
			pluginsRoot.resolve(INSTALLED));

		assertEquals(PluginZipInstallation.Kind.REFUSED_OTHER_PLUGIN, decision.kind());
		assertEquals("other-plugin", decision.pluginId());
		assertEquals("demo-plugin", decision.otherPluginId());
	}

	@Test
	@DisplayName("the zip of a loaded plugin under another file name is refused")
	void theZipOfALoadedPluginUnderAnotherFileName_isRefused() throws Exception
	{
		Path zip = writeZip(chosen.resolve("demo-copy.zip"), "demo-plugin", "1.0.0");

		PluginZipInstallation.Decision decision = PluginZipInstallation.decide(pluginManager, zip,
			pluginsRoot.resolve("demo-copy.zip"));

		assertEquals(PluginZipInstallation.Kind.REFUSED_OTHER_FILE, decision.kind());
		assertEquals("demo-plugin", decision.pluginId());
	}

	@ParameterizedTest(name = "the file name {0} is refused")
	@ValueSource(strings = { ".zip", "demo-plugin-1.0.0.jar", "demo-plugin-1.0.0", "demo.zip.txt" })
	@DisplayName("a file name that is not a name followed by .zip is refused")
	void aFileNameThatIsNotANameFollowedByZip_isRefused(final String fileName) throws Exception
	{
		Path file = writeZip(chosen.resolve(fileName), "demo-plugin", "1.0.0");

		assertThrows(InvalidPluginZipException.class,
			() -> PluginZipInstallation.decide(pluginManager, file, pluginsRoot.resolve(fileName)));
	}

	@Test
	@DisplayName("a file name ending in .ZIP is accepted")
	void aFileNameEndingInCapitalZip_isAccepted() throws Exception
	{
		Path zip = writeZip(chosen.resolve("new-plugin-1.0.0.ZIP"), "new-plugin", "1.0.0");

		assertEquals(PluginZipInstallation.Kind.NEW_PLUGIN, PluginZipInstallation
			.decide(pluginManager, zip, pluginsRoot.resolve(zip.getFileName())).kind());
	}

	static Path writeZip(final Path zip, final String pluginId, final String version)
		throws IOException
	{
		try (OutputStream out = Files.newOutputStream(zip);
			ZipOutputStream archive = new ZipOutputStream(out))
		{
			archive.putNextEntry(new ZipEntry("plugin.properties"));
			archive.write(("plugin.id=" + pluginId + "\nplugin.version=" + version + "\n")
				.getBytes(StandardCharsets.UTF_8));
			archive.closeEntry();
		}
		return zip;
	}
}
