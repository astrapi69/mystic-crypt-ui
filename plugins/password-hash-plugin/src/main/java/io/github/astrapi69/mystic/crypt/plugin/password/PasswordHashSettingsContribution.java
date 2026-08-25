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
package io.github.astrapi69.mystic.crypt.plugin.password;

import java.util.LinkedHashMap;
import java.util.Map;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The configuration of the password hash plugin: which of the two algorithms the tool starts with.
 */
@Extension
public class PasswordHashSettingsContribution implements PluginSettingsContribution
{

	/** The plugin id, the same one {@code plugin.properties} declares */
	public static final String PLUGIN_ID = "password-hash-plugin";

	/** The hash algorithm the tool starts with */
	public static final String KEY_ALGORITHM = "default.algorithm";

	@Override
	public String getPluginId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getDisplayName()
	{
		return "Password Hash";
	}

	@Override
	public Map<String, String> getDefaults()
	{
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put(KEY_ALGORITHM, PasswordHashPanel.ARGON2ID);
		return defaults;
	}

	@Override
	public String getDescription(String key)
	{
		return KEY_ALGORITHM.equals(key)
			? PasswordHashPanel.ARGON2ID + " or " + PasswordHashPanel.PBKDF2
			: null;
	}

	/**
	 * The configured algorithm, the built-in default when the setting names none of the two
	 *
	 * @return the algorithm the tool starts with
	 */
	public static String algorithm()
	{
		String configured = PluginSettings
			.load(PLUGIN_ID, new PasswordHashSettingsContribution().getDefaults())
			.get(KEY_ALGORITHM);
		return PasswordHashPanel.PBKDF2.equals(configured)
			? PasswordHashPanel.PBKDF2
			: PasswordHashPanel.ARGON2ID;
	}
}
