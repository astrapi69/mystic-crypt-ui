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
package io.github.astrapi69.mystic.crypt.plugin.obfuscation;

import java.util.LinkedHashMap;
import java.util.Map;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The configuration of the obfuscation plugin: whether the field that was just read is emptied
 * after obfuscating or disentangling. Emptying it keeps the two fields from being mistaken for each
 * other; keeping it lets the same text be run through several rule sets in a row.
 */
@Extension
public class ObfuscationSettingsContribution implements PluginSettingsContribution
{

	/** The plugin id, the same one {@code plugin.properties} declares */
	public static final String PLUGIN_ID = "obfuscation-plugin";

	/** Whether the source field is emptied after a run */
	public static final String KEY_CLEAR_INPUT = "clear.input.after.run";

	@Override
	public String getPluginId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getDisplayName()
	{
		return "Obfuscation";
	}

	@Override
	public Map<String, String> getDefaults()
	{
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put(KEY_CLEAR_INPUT, "true");
		return defaults;
	}

	@Override
	public String getDescription(String key)
	{
		return KEY_CLEAR_INPUT.equals(key)
			? "true or false: empty the field that was read after obfuscating or disentangling"
			: null;
	}

	/**
	 * Whether the source field is emptied after a run
	 *
	 * @return true when it is emptied
	 */
	public static boolean clearInputAfterRun()
	{
		return !"false".equalsIgnoreCase(PluginSettings
			.load(PLUGIN_ID, new ObfuscationSettingsContribution().getDefaults())
			.get(KEY_CLEAR_INPUT));
	}
}
