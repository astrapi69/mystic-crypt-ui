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

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * When this program last synced a chain file (#831): recorded at every sync the plugin does, the
 * sync window's and the overview's "Fetch the chain", so that the overview can say how current its
 * chain is. A record, not a setting: it is kept in a file of its own next to the plugin's settings
 * ({@value #RECORD_ID}.properties), which the settings window does not list, and only the last
 * sync is kept.
 */
final class ChainSync
{

	/** The name of the record among the plugin settings files */
	static final String RECORD_ID = "lethenon-plugin-sync";

	private static final String KEY_FILE = "chain.file";

	private static final String KEY_AT = "synced.at";

	private static final Logger LOGGER = Logger.getLogger(ChainSync.class.getName());

	private ChainSync()
	{
	}

	/**
	 * Records that a chain file was synced
	 *
	 * @param chainFile
	 *            the chain file the sync wrote
	 * @param at
	 *            when it ended
	 */
	static void record(final Path chainFile, final Instant at)
	{
		Map<String, String> values = new LinkedHashMap<>();
		values.put(KEY_FILE, keyOf(chainFile));
		values.put(KEY_AT, Long.toString(at.toEpochMilli()));
		try
		{
			PluginSettings.save(PluginSettings.configurationDirectory(), RECORD_ID, defaults(),
				values);
		}
		catch (IOException unwritable)
		{
			LOGGER.log(Level.WARNING, "the Lethenon plugin could not record the sync of "
				+ chainFile + " in " + PluginSettings.configurationDirectory(), unwritable);
		}
	}

	/**
	 * When this program last synced a chain file
	 *
	 * @param chainFile
	 *            the chain file
	 * @return the time, or empty when the last sync recorded was of another file, or none was
	 */
	static Optional<Instant> lastOf(final Path chainFile)
	{
		Map<String, String> stored = PluginSettings.load(RECORD_ID, defaults());
		if (!keyOf(chainFile).equals(stored.get(KEY_FILE)))
		{
			return Optional.empty();
		}
		try
		{
			return Optional.of(Instant.ofEpochMilli(Long.parseLong(stored.get(KEY_AT).trim())));
		}
		catch (NumberFormatException unreadable)
		{
			LOGGER.log(Level.WARNING, "the Lethenon plugin's record of the last sync holds no time: '"
				+ stored.get(KEY_AT) + "'", unreadable);
			return Optional.empty();
		}
	}

	private static Map<String, String> defaults()
	{
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put(KEY_FILE, "");
		defaults.put(KEY_AT, "");
		return defaults;
	}

	private static String keyOf(final Path chainFile)
	{
		return chainFile.toAbsolutePath().normalize().toString();
	}
}
