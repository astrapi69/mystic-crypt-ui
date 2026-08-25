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
package io.github.astrapi69.mystic.crypt.plugin.signature;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The configuration of the signature plugin: which algorithm the tool starts with and what it puts
 * into the message field, so a recurring test message does not have to be typed again every time.
 */
@Extension
public class SignatureSettingsContribution implements PluginSettingsContribution
{

	/** The plugin id, the same one {@code plugin.properties} declares */
	public static final String PLUGIN_ID = "pqc-signature-plugin";

	/** The signature algorithm the tool starts with */
	public static final String KEY_ALGORITHM = "default.algorithm";

	/** The message the tool starts with */
	public static final String KEY_MESSAGE = "default.message";

	/** The message used when nothing else is configured */
	public static final String DEFAULT_MESSAGE = "The quick brown fox jumps over the lazy dog";

	@Override
	public String getPluginId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getDisplayName()
	{
		return "Signatures";
	}

	@Override
	public Map<String, String> getDefaults()
	{
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put(KEY_ALGORITHM, SignatureSupport.algorithms().get(0));
		defaults.put(KEY_MESSAGE, DEFAULT_MESSAGE);
		return defaults;
	}

	@Override
	public String getDescription(String key)
	{
		return switch (key)
		{
			case KEY_ALGORITHM -> "one of " + String.join(", ", SignatureSupport.algorithms());
			case KEY_MESSAGE -> "what the message field starts with";
			default -> null;
		};
	}

	/** The settings as stored, with the declared defaults filled in */
	public static Map<String, String> current()
	{
		return PluginSettings.load(PLUGIN_ID, new SignatureSettingsContribution().getDefaults());
	}

	/**
	 * The configured algorithm, the first offered one when the setting names one that does not
	 * exist
	 *
	 * @return the algorithm the tool starts with
	 */
	public static String algorithm()
	{
		List<String> offered = SignatureSupport.algorithms();
		String configured = current().get(KEY_ALGORITHM);
		return offered.contains(configured) ? configured : offered.get(0);
	}

	/**
	 * The configured message
	 *
	 * @return the message the tool starts with
	 */
	public static String message()
	{
		String configured = current().get(KEY_MESSAGE);
		return configured != null ? configured : DEFAULT_MESSAGE;
	}
}
