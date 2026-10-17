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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A plugin whose zip bundles a library brings that library's licence, and the installer installs it
 * where a person finds it, not only inside the zip in the plugin directory (#581). The Zentus
 * licence sqlite-jdbc carries asks for its notice in every binary distribution, and the installer
 * is one.
 * <p>
 * Checked mechanically: every plugin build that collects licences in a {@code pluginLegal} task has
 * its {@code build/legal} directory installed by the installer, under {@code legal/<plugin>}.
 */
class InstallerCarriesBundledLicencesTest
{

	private static final Path INSTALL_XML = Path.of("src/main/izpack/install.xml");

	@Test
	@DisplayName("the installer installs the licences of every plugin that bundles a library")
	void everyPluginThatBundlesALibrary_hasItsLicencesInstalled() throws IOException
	{
		String installer = Files.readString(INSTALL_XML, StandardCharsets.UTF_8);
		List<String> missing = new ArrayList<>();
		List<String> bundling = pluginsWithLicences();

		for (String plugin : bundling)
		{
			String installed = "<fileset dir=\"../plugins/" + plugin
				+ "/build/legal\" targetdir=\"${INSTALL_PATH}/legal/" + plugin + "\"";
			if (!installer.contains(installed))
			{
				missing.add(plugin);
			}
		}

		assertFalse(bundling.isEmpty(),
			"no plugin build collects licences - an empty set is not a clean result");
		assertTrue(missing.isEmpty(), bundling.size() + " plugins bundle libraries, and the "
			+ "installer does not install the licences of " + missing);
	}

	private static List<String> pluginsWithLicences() throws IOException
	{
		File[] plugins = new File("plugins").listFiles(File::isDirectory);
		assertNotNull(plugins, "the plugins directory is where it was");
		List<String> bundling = new ArrayList<>();
		for (File plugin : plugins)
		{
			File buildFile = new File(plugin, "build.gradle");
			if (buildFile.isFile() && Files.readString(buildFile.toPath(), StandardCharsets.UTF_8)
				.contains("tasks.register('pluginLegal'"))
			{
				bundling.add(plugin.getName());
			}
		}
		return bundling;
	}
}
