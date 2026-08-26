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
package io.github.astrapi69.mystic.crypt.plugin.checksum;

import java.util.LinkedHashMap;
import java.util.Map;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

import io.github.astrapi69.crypt.api.algorithm.ChecksumAlgorithm;

/**
 * The configuration of the checksum plugin: which algorithm the tool starts with.
 */
@Extension
public class ChecksumSettingsContribution implements PluginSettingsContribution
{

	/** The plugin id, the same one {@code plugin.properties} declares */
	public static final String PLUGIN_ID = "checksum-plugin";

	/** The checksum algorithm the tool starts with */
	public static final String KEY_ALGORITHM = "default.algorithm";

	/** The digest the checksum and MAC tool starts with */
	public static final String KEY_DIGEST = "default.digest";

	@Override
	public String getPluginId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getDisplayName()
	{
		return "Checksum";
	}

	@Override
	public Map<String, String> getDefaults()
	{
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put(KEY_ALGORITHM, ChecksumAlgorithm.MD5.name());
		defaults.put(KEY_DIGEST, "SHA-256");
		return defaults;
	}

	@Override
	public String getDescription(String key)
	{
		if (KEY_ALGORITHM.equals(key))
		{
			return "one of the ChecksumAlgorithm constants, such as SHA_256";
		}
		return KEY_DIGEST.equals(key) ? String.join(", ", ChecksumSupport.DIGESTS) : null;
	}

	/**
	 * The digest the checksum and MAC tool starts with, SHA-256 when the setting names none this
	 * machine knows
	 *
	 * @return the digest
	 */
	public static String digest()
	{
		String configured = PluginSettings
			.load(PLUGIN_ID, new ChecksumSettingsContribution().getDefaults()).get(KEY_DIGEST);
		return ChecksumSupport.DIGESTS.contains(configured) ? configured : "SHA-256";
	}

	/**
	 * The configured algorithm, MD5 when the setting names none that exists
	 *
	 * @return the algorithm the tool starts with
	 */
	public static ChecksumAlgorithm algorithm()
	{
		String configured = PluginSettings
			.load(PLUGIN_ID, new ChecksumSettingsContribution().getDefaults()).get(KEY_ALGORITHM);
		try
		{
			return ChecksumAlgorithm.valueOf(configured.trim());
		}
		catch (IllegalArgumentException | NullPointerException exception)
		{
			return ChecksumAlgorithm.MD5;
		}
	}
}
