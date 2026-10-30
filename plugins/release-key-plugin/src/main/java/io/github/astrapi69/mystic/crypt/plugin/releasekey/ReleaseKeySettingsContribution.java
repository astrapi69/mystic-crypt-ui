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

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

import io.github.astrapi69.mystic.crypt.gnupg.GnuPgHome;

/**
 * The configuration of this plugin: which GnuPG home the status view reads, and which certificate
 * in it is the release key. Both start blank, which means the home gpg itself uses and every
 * certificate of it.
 */
@Extension
public class ReleaseKeySettingsContribution implements PluginSettingsContribution
{

	/** The plugin id, the same one {@code plugin.properties} declares */
	public static final String PLUGIN_ID = "release-key-plugin";

	/** The GnuPG home the status view reads */
	public static final String KEY_GNUPG_HOME = "gnupg.home";

	/** The fingerprint or long key id of the release key */
	public static final String KEY_RELEASE_KEY = "release.key";

	@Override
	public String getPluginId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getDisplayName()
	{
		return ReleaseKeyMessages.getString("releasekey.menu", "Release Key");
	}

	@Override
	public Map<String, String> getDefaults()
	{
		// a LinkedHashMap: this is also the order the settings dialog shows them in
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put(KEY_GNUPG_HOME, "");
		defaults.put(KEY_RELEASE_KEY, "");
		return defaults;
	}

	@Override
	public String getDescription(String key)
	{
		return switch (key)
		{
			case KEY_GNUPG_HOME -> "the GnuPG home to read; blank for the one gpg uses, "
				+ "GNUPGHOME or ~/.gnupg";
			case KEY_RELEASE_KEY -> "the fingerprint or long key id of the release key; blank "
				+ "shows every certificate of the home";
			default -> null;
		};
	}

	/**
	 * The effective settings of this plugin: what the user configured, with the declared defaults
	 * filling in everything else
	 *
	 * @return the effective settings
	 */
	public static Map<String, String> current()
	{
		return PluginSettings.load(PLUGIN_ID, new ReleaseKeySettingsContribution().getDefaults());
	}

	/**
	 * The configured GnuPG home
	 *
	 * @return the home, the one gpg uses when none is configured
	 */
	public static Path gnuPgHome()
	{
		return homeOf(current().get(KEY_GNUPG_HOME));
	}

	/**
	 * The configured release key
	 *
	 * @return its fingerprint or long key id, blank when none is configured
	 */
	public static String releaseKey()
	{
		String releaseKey = current().get(KEY_RELEASE_KEY);
		return releaseKey == null ? "" : releaseKey.trim();
	}

	/**
	 * The GnuPG home a configured or typed value stands for
	 *
	 * @param configured
	 *            the value, blank for the home gpg itself uses
	 * @return the home
	 */
	static Path homeOf(final String configured)
	{
		return configured == null || configured.isBlank()
			? GnuPgHome.defaultHome(System.getenv(), System.getProperty("user.home"))
			: Path.of(configured.trim());
	}
}
