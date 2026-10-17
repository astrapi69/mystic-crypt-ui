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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The plugin's settings: the chain file, the wallet file, the node and the SOCKS proxy that worked
 * last, so that they are typed once (#605).
 * <p>
 * The windows remember a value only after it worked - a chain read or written, a wallet opened with
 * its password, a sync taken from a node or through a proxy - and a failure leaves what worked
 * before. Every value is editable in the plugin's settings page, where clearing it forgets it, and
 * every window starts with them (architecture.md: no hidden setting, no setting nobody reads).
 * <p>
 * Only these four keys can be written, by {@link #remember}: a password is never remembered, in no
 * form, and there is no key it could be written under.
 */
@Extension
public class LethenonSettingsContribution implements PluginSettingsContribution
{

	/** The plugin id, the same string as in {@code plugin.properties} */
	public static final String PLUGIN_ID = "lethenon-plugin";

	/** The chain file every window starts with */
	public static final String KEY_CHAIN_FILE = "chain.file";

	/** The wallet file the windows that open a wallet start with */
	public static final String KEY_WALLET_FILE = "wallet.file";

	/** The node a sync or a transfer was handed to; empty for the test network's seed nodes */
	public static final String KEY_NODE = "node";

	/** The SOCKS proxy a sync went through, host:port */
	public static final String KEY_PROXY = "proxy";

	/** The keys this plugin has, in the order the settings page shows them */
	static final List<String> KEYS = List.of(KEY_CHAIN_FILE, KEY_WALLET_FILE, KEY_NODE, KEY_PROXY);

	private static final Logger LOGGER = Logger
		.getLogger(LethenonSettingsContribution.class.getName());

	@Override
	public String getPluginId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getDisplayName()
	{
		return "Lethenon";
	}

	@Override
	public Map<String, String> getDefaults()
	{
		Map<String, String> defaults = new LinkedHashMap<>();
		for (String key : KEYS)
		{
			defaults.put(key, "");
		}
		return defaults;
	}

	@Override
	public String getDescription(String key)
	{
		return switch (key)
		{
			case KEY_CHAIN_FILE -> LethenonMessages.getString(
				"lethenon.settings.description.chain.file",
				"the chain file every window starts with; remembered once a window read or wrote "
					+ "it, empty for none");
			case KEY_WALLET_FILE -> LethenonMessages.getString(
				"lethenon.settings.description.wallet.file",
				"the wallet file the windows start with; remembered once a window opened it with "
					+ "its password, which is never remembered");
			case KEY_NODE -> LethenonMessages.getString("lethenon.settings.description.node",
				"the node, host:port, a sync took the chain from; empty for the test network's "
					+ "seed nodes");
			case KEY_PROXY -> LethenonMessages.getString("lethenon.settings.description.proxy",
				"the SOCKS proxy, host:port, a sync went through, for Tor usually 127.0.0.1:9050");
			default -> null;
		};
	}

	/**
	 * Gets this plugin's settings as they are stored, defaults filled in
	 *
	 * @return the settings
	 */
	public static Map<String, String> current()
	{
		return PluginSettings.load(PLUGIN_ID, new LethenonSettingsContribution().getDefaults());
	}

	/**
	 * Gets the chain file the windows start with
	 *
	 * @return the remembered path, or an empty string
	 */
	public static String chainFile()
	{
		return current().getOrDefault(KEY_CHAIN_FILE, "");
	}

	/**
	 * Gets the wallet file the windows start with
	 *
	 * @return the remembered path, or an empty string
	 */
	public static String walletFile()
	{
		return current().getOrDefault(KEY_WALLET_FILE, "");
	}

	/**
	 * Gets the node a sync or a transfer starts with
	 *
	 * @return the remembered host:port, or an empty string for the seed nodes
	 */
	public static String node()
	{
		return current().getOrDefault(KEY_NODE, "");
	}

	/**
	 * Gets the SOCKS proxy a sync starts with
	 *
	 * @return the remembered host:port, or an empty string for none
	 */
	public static String proxy()
	{
		return current().getOrDefault(KEY_PROXY, "");
	}

	/**
	 * Remembers values that worked, keeping the others as they are. A setting that cannot be
	 * written is logged and does not turn what worked into a failure: the window's own outcome
	 * stands, and the next window starts with what was remembered before
	 *
	 * @param worked
	 *            the values that worked, by key
	 * @throws IllegalArgumentException
	 *             for a key that is not one of {@link #KEYS}; a password has none
	 */
	static void remember(final Map<String, String> worked)
	{
		for (String key : worked.keySet())
		{
			if (!KEYS.contains(key))
			{
				throw new IllegalArgumentException("'" + key
					+ "' is not a value the Lethenon plugin remembers; it remembers " + KEYS);
			}
		}
		Map<String, String> values = new LinkedHashMap<>(current());
		values.putAll(worked);
		try
		{
			PluginSettings.save(PluginSettings.configurationDirectory(), PLUGIN_ID,
				new LethenonSettingsContribution().getDefaults(), values);
		}
		catch (IOException unwritable)
		{
			LOGGER.log(Level.WARNING, "the Lethenon plugin could not remember " + worked.keySet()
				+ " in " + PluginSettings.configurationDirectory(), unwritable);
		}
	}
}
