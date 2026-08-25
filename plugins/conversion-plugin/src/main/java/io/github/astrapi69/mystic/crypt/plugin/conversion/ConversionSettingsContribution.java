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
package io.github.astrapi69.mystic.crypt.plugin.conversion;

import java.util.LinkedHashMap;
import java.util.Map;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

import io.github.astrapi69.crypt.api.key.KeyType;

/**
 * The configuration of the der to pem plugin: which kind of key the tool starts with.
 */
@Extension
public class ConversionSettingsContribution implements PluginSettingsContribution
{

	/** The plugin id, the same one {@code plugin.properties} declares */
	public static final String PLUGIN_ID = "conversion-plugin";

	/** The key type the tool starts with */
	public static final String KEY_KEY_TYPE = "default.key.type";

	@Override
	public String getPluginId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getDisplayName()
	{
		return "Der to Pem";
	}

	@Override
	public Map<String, String> getDefaults()
	{
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put(KEY_KEY_TYPE, KeyType.PRIVATE_KEY.name());
		return defaults;
	}

	@Override
	public String getDescription(String key)
	{
		return KEY_KEY_TYPE.equals(key) ? "one of the KeyType constants, such as PRIVATE_KEY" : null;
	}

	/**
	 * The configured key type, the private key when the setting names none that exists
	 *
	 * @return the key type the tool starts with
	 */
	public static KeyType keyType()
	{
		String configured = PluginSettings
			.load(PLUGIN_ID, new ConversionSettingsContribution().getDefaults()).get(KEY_KEY_TYPE);
		try
		{
			return KeyType.valueOf(configured.trim());
		}
		catch (IllegalArgumentException | NullPointerException exception)
		{
			return KeyType.PRIVATE_KEY;
		}
	}
}
