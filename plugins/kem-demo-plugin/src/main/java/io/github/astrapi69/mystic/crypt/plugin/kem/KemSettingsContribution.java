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
package io.github.astrapi69.mystic.crypt.plugin.kem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The configuration of the key encapsulation demo: which mechanism the tool starts with.
 */
@Extension
public class KemSettingsContribution implements PluginSettingsContribution
{

	/** The plugin id, the same one {@code plugin.properties} declares */
	public static final String PLUGIN_ID = "kem-demo-plugin";

	/** The mechanism the demo starts with */
	public static final String KEY_ALGORITHM = "default.algorithm";

	/** The algorithm the exchange between two people starts with */
	public static final String KEY_EXCHANGE_ALGORITHM = "default.exchange.algorithm";

	@Override
	public String getPluginId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getDisplayName()
	{
		return "Key Encapsulation";
	}

	@Override
	public Map<String, String> getDefaults()
	{
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put(KEY_ALGORITHM, KemDemoPanel.ALGORITHMS.get(0));
		defaults.put(KEY_EXCHANGE_ALGORITHM, KeyExchangeSupport.algorithms().get(0));
		return defaults;
	}

	@Override
	public String getDescription(String key)
	{
		return switch (key)
		{
			case KEY_ALGORITHM -> "what the demo starts with: "
				+ String.join(", ", KemDemoPanel.ALGORITHMS);
			case KEY_EXCHANGE_ALGORITHM -> "what the exchange starts with: "
				+ String.join(", ", KeyExchangeSupport.algorithms());
			default -> null;
		};
	}

	/**
	 * The configured mechanism, the first offered one when the setting names one that does not
	 * exist
	 *
	 * @return the mechanism the tool starts with
	 */
	public static String algorithm()
	{
		return offeredOrFirst(KEY_ALGORITHM, KemDemoPanel.ALGORITHMS);
	}

	/**
	 * The configured algorithm of the exchange between two people, the first offered one when the
	 * setting names one that tool does not have
	 *
	 * @return the algorithm the exchange starts with
	 */
	public static String exchangeAlgorithm()
	{
		return offeredOrFirst(KEY_EXCHANGE_ALGORITHM, KeyExchangeSupport.algorithms());
	}

	private static String offeredOrFirst(final String key, final List<String> offered)
	{
		String configured = PluginSettings
			.load(PLUGIN_ID, new KemSettingsContribution().getDefaults()).get(key);
		return offered.contains(configured) ? configured : offered.get(0);
	}
}
