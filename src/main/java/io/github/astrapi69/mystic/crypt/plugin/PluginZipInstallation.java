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

import java.io.IOException;
import java.nio.file.Path;

import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;

/**
 * Decides what installing a chosen zip means before anything of an installed plugin is touched
 * (#1045, #1060): whether it is a plugin zip at all, and how it stands to the plugins that are
 * loaded - a new plugin, an update of a loaded one, or a zip that must be refused because it holds
 * another plugin than the one installed under its file name, or a plugin that is loaded from
 * another file
 */
public final class PluginZipInstallation
{

	private PluginZipInstallation()
	{
	}

	/** How the chosen zip stands to the loaded plugins */
	public enum Kind
	{
		/** no loaded plugin has its id or its file name */
		NEW_PLUGIN,
		/** it holds the loaded plugin that was installed under this file name */
		UPDATE_OF_LOADED,
		/** the plugin installed under this file name is another one */
		REFUSED_OTHER_PLUGIN,
		/** the plugin it holds is loaded from another file */
		REFUSED_OTHER_FILE
	}

	/**
	 * The decision
	 *
	 * @param kind
	 *            how the zip stands to the loaded plugins
	 * @param pluginId
	 *            the id of the plugin the zip holds
	 * @param loaded
	 *            the loaded plugin the zip updates, only for {@link Kind#UPDATE_OF_LOADED}
	 * @param otherPluginId
	 *            the id of the plugin installed under the file name, only for
	 *            {@link Kind#REFUSED_OTHER_PLUGIN}
	 */
	public record Decision(Kind kind, String pluginId, PluginWrapper loaded, String otherPluginId) {
	}

	/**
	 * Reads the chosen zip and decides, changing nothing
	 *
	 * @param pluginManager
	 *            the plugin manager whose plugins are loaded
	 * @param zip
	 *            the chosen file
	 * @param target
	 *            where the zip would be installed, in the plugins directory, under the same name
	 * @return the decision
	 * @throws InvalidPluginZipException
	 *             if the file name does not end in .zip, or the file is no plugin zip
	 * @throws IOException
	 *             if the file cannot be read
	 */
	public static Decision decide(final PluginManager pluginManager, final Path zip,
		final Path target) throws IOException
	{
		requireZipName(target.getFileName().toString());
		String pluginId = PluginArchives.pluginIdOf(zip);
		Path directory = PluginArchives.directoryOf(target).toAbsolutePath().normalize();
		PluginWrapper loaded = pluginManager.getPlugin(pluginId);
		PluginWrapper installedUnderTheName = null;
		for (PluginWrapper candidate : pluginManager.getPlugins())
		{
			if (directory.equals(candidate.getPluginPath().toAbsolutePath().normalize()))
			{
				installedUnderTheName = candidate;
			}
		}
		if (installedUnderTheName != null && installedUnderTheName != loaded)
		{
			return new Decision(Kind.REFUSED_OTHER_PLUGIN, pluginId, null,
				installedUnderTheName.getPluginId());
		}
		if (loaded != null && installedUnderTheName == null)
		{
			return new Decision(Kind.REFUSED_OTHER_FILE, pluginId, loaded, null);
		}
		if (loaded != null)
		{
			return new Decision(Kind.UPDATE_OF_LOADED, pluginId, loaded, null);
		}
		return new Decision(Kind.NEW_PLUGIN, pluginId, null, null);
	}

	/**
	 * A name that is not {@code <name>.zip} or {@code <name>.ZIP}, the forms pf4j lists and the
	 * application unpacks at the start, would map onto the plugins directory or a jar
	 */
	private static void requireZipName(final String fileName) throws InvalidPluginZipException
	{
		if (!PluginArchives.isZipName(Path.of(fileName)))
		{
			throw new InvalidPluginZipException(fileName,
				"its file name must be a name followed by .zip", null);
		}
	}
}
