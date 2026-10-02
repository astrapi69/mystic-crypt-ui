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

import java.util.LinkedHashMap;
import java.util.Map;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The plugin's settings: which chain file the verifier opens without being asked.
 * <p>
 * One setting, editable in the plugin's settings page and read by the panel - not an internal
 * tuning knob, and not a field nobody reads (architecture.md forbids both).
 */
@Extension
public class LethenonSettingsContribution implements PluginSettingsContribution
{

	/** The plugin id, the same string as in {@code plugin.properties} */
	public static final String PLUGIN_ID = "lethenon-plugin";

	/** The chain file the verifier starts with */
	public static final String KEY_CHAIN_FILE = "chain.file";

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
		defaults.put(KEY_CHAIN_FILE, "");
		return defaults;
	}

	@Override
	public String getDescription(String key)
	{
		return switch (key)
		{
			case KEY_CHAIN_FILE -> "the chain file the verifier opens with, empty for none";
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
	 * Gets the chain file the verifier starts with
	 *
	 * @return the configured path, or an empty string when none is configured
	 */
	public static String chainFile()
	{
		return current().getOrDefault(KEY_CHAIN_FILE, "");
	}
}
