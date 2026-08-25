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
package io.github.astrapi69.mystic.crypt.settings;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The configuration of a single plugin: one properties file per plugin, in a
 * {@code plugin-settings} directory next to the application settings.
 * <p>
 * A plugin declares its keys and defaults through
 * {@link io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution}; everything else
 * happens here. Reading always returns every declared key, whether the file exists or not, so a
 * plugin never has to deal with a missing value. Writing keeps only the declared keys, so a key
 * that a plugin drops in a new version does not linger forever.
 * <p>
 * Deliberately free of Swing and of the application frame: the plugin panels use it, the settings
 * dialog uses it, and the command line uses it - and it stays testable without a display.
 */
public final class PluginSettings
{

	/** The directory, inside the configuration directory, that holds one file per plugin */
	public static final String DIRECTORY_NAME = "plugin-settings";

	/** System property that points at the configuration directory; mainly there for tests */
	public static final String CONFIGURATION_DIRECTORY_PROPERTY = "mystic.crypt.ui.config.dir";

	/** The application name, which is also the name of the configuration directory */
	private static final String APPLICATION_NAME = "mystic-crypt-ui";

	private PluginSettings()
	{
	}

	/**
	 * The configuration directory to use when no explicit one is at hand - the one named by
	 * {@value #CONFIGURATION_DIRECTORY_PROPERTY}, or the installed one in the user's home
	 * directory. This is what a plugin uses, which runs in its own class loader and has no access
	 * to the application frame
	 *
	 * @return the configuration directory
	 */
	public static File configurationDirectory()
	{
		String configured = System.getProperty(CONFIGURATION_DIRECTORY_PROPERTY);
		if (configured != null && !configured.isBlank())
		{
			return new File(configured);
		}
		return new File(System.getProperty("user.home"), ".config/" + APPLICATION_NAME);
	}

	/**
	 * The file the settings of a plugin are stored in
	 *
	 * @param configurationDirectory
	 *            the application's configuration directory
	 * @param pluginId
	 *            the id of the plugin
	 * @return the settings file, which need not exist
	 */
	public static Path file(final File configurationDirectory, final String pluginId)
	{
		return configurationDirectory.toPath().resolve(DIRECTORY_NAME)
			.resolve(pluginId + ".properties");
	}

	/**
	 * Loads the settings of a plugin, filling in the default of every key that the stored file does
	 * not have
	 *
	 * @param configurationDirectory
	 *            the application's configuration directory
	 * @param pluginId
	 *            the id of the plugin
	 * @param defaults
	 *            the keys the plugin declares, with their default values
	 * @return the effective settings, in the order the defaults declare them
	 */
	public static Map<String, String> load(final File configurationDirectory, final String pluginId,
		final Map<String, String> defaults)
	{
		Properties stored = new Properties();
		Path file = file(configurationDirectory, pluginId);
		if (Files.exists(file))
		{
			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
			{
				stored.load(reader);
			}
			catch (IOException exception)
			{
				// an unreadable file must never keep a plugin from starting; the defaults win
			}
		}
		Map<String, String> effective = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : defaults.entrySet())
		{
			effective.put(entry.getKey(), stored.getProperty(entry.getKey(), entry.getValue()));
		}
		return effective;
	}

	/**
	 * Loads the settings of a plugin from the configuration directory of the installation. This is
	 * the entry point for a plugin itself, which has no access to the application frame
	 *
	 * @param pluginId
	 *            the id of the plugin
	 * @param defaults
	 *            the keys the plugin declares, with their default values
	 * @return the effective settings
	 */
	public static Map<String, String> load(final String pluginId,
		final Map<String, String> defaults)
	{
		return load(configurationDirectory(), pluginId, defaults);
	}

	/**
	 * Stores the settings of a plugin, keeping only the keys the plugin declares
	 *
	 * @param configurationDirectory
	 *            the application's configuration directory
	 * @param pluginId
	 *            the id of the plugin
	 * @param defaults
	 *            the keys the plugin declares, with their default values
	 * @param values
	 *            the values to store
	 * @throws IOException
	 *             if the file cannot be written
	 */
	public static void save(final File configurationDirectory, final String pluginId,
		final Map<String, String> defaults, final Map<String, String> values) throws IOException
	{
		Properties properties = new Properties();
		for (String key : defaults.keySet())
		{
			String value = values.get(key);
			properties.setProperty(key, value != null ? value : defaults.get(key));
		}
		Path file = file(configurationDirectory, pluginId);
		Files.createDirectories(file.getParent());
		try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8))
		{
			properties.store(writer, "settings of the plugin '" + pluginId + "'");
		}
	}

	/**
	 * Removes the stored settings of a plugin, so the defaults apply again
	 *
	 * @param configurationDirectory
	 *            the application's configuration directory
	 * @param pluginId
	 *            the id of the plugin
	 * @return true if a file was removed
	 * @throws IOException
	 *             if the file exists but cannot be removed
	 */
	public static boolean reset(final File configurationDirectory, final String pluginId)
		throws IOException
	{
		return Files.deleteIfExists(file(configurationDirectory, pluginId));
	}

	/**
	 * Reads a setting as a number, falling back to the given value when it is not one
	 *
	 * @param values
	 *            the settings
	 * @param key
	 *            the key to read
	 * @param fallback
	 *            the value to use when the setting is missing or not a number
	 * @return the number
	 */
	public static int asInt(final Map<String, String> values, final String key, final int fallback)
	{
		try
		{
			return Integer.parseInt(values.get(key).trim());
		}
		catch (NumberFormatException | NullPointerException exception)
		{
			return fallback;
		}
	}
}
