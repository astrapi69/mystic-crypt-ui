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
package io.github.astrapi69.mystic.crypt.plugin.sharing;

import java.util.LinkedHashMap;
import java.util.Map;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The configuration of the secret sharing plugin: how a secret is split when nothing else is said.
 */
@Extension
public class SecretSharingSettingsContribution implements PluginSettingsContribution
{

	/** The plugin id, the same one {@code plugin.properties} declares */
	public static final String PLUGIN_ID = "secret-sharing-plugin";

	/** How many shares are needed to rebuild the secret */
	public static final String KEY_THRESHOLD = "default.threshold";

	/** How many shares are produced */
	public static final String KEY_TOTAL_SHARES = "default.total.shares";

	@Override
	public String getPluginId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getDisplayName()
	{
		return "Secret Sharing";
	}

	@Override
	public Map<String, String> getDefaults()
	{
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put(KEY_THRESHOLD, "3");
		defaults.put(KEY_TOTAL_SHARES, "5");
		return defaults;
	}

	@Override
	public String getDescription(String key)
	{
		return switch (key)
		{
			case KEY_THRESHOLD -> "how many shares are needed to rebuild the secret, at least two";
			case KEY_TOTAL_SHARES -> "how many shares are produced, at least as many as are needed";
			default -> null;
		};
	}

	/** The settings as stored, with the declared defaults filled in */
	public static Map<String, String> current()
	{
		return PluginSettings.load(PLUGIN_ID, new SecretSharingSettingsContribution().getDefaults());
	}

	/**
	 * How many shares are needed to rebuild the secret, at least two
	 *
	 * @return the threshold
	 */
	public static int threshold()
	{
		return Math.max(2, PluginSettings.asInt(current(), KEY_THRESHOLD, 3));
	}

	/**
	 * How many shares are produced, never fewer than are needed to rebuild the secret
	 *
	 * @return the number of shares
	 */
	public static int totalShares()
	{
		return Math.max(threshold(), PluginSettings.asInt(current(), KEY_TOTAL_SHARES, 5));
	}
}
