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
package io.github.astrapi69.mystic.crypt.plugin.keygen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.key.KeySize;

/**
 * The configuration of the key generation plugin: which algorithm and which key size the tool
 * starts with.
 */
@Extension
public class KeygenSettingsContribution implements PluginSettingsContribution
{

	/** The plugin id, the same one {@code plugin.properties} declares */
	public static final String PLUGIN_ID = "keygen-plugin";

	/** The key algorithm the tool starts with */
	public static final String KEY_ALGORITHM = "default.algorithm";

	/** The key size the tool starts with, used by the size based algorithms */
	public static final String KEY_KEY_SIZE = "default.key.size";

	@Override
	public String getPluginId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getDisplayName()
	{
		return "Key Generation";
	}

	@Override
	public Map<String, String> getDefaults()
	{
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put(KEY_ALGORITHM, KeyPairGeneratorAlgorithm.RSA.name());
		defaults.put(KEY_KEY_SIZE, KeySize.KEYSIZE_2048.name());
		return defaults;
	}

	@Override
	public String getDescription(String key)
	{
		return switch (key)
		{
			case KEY_ALGORITHM -> offeredAlgorithmNames();
			case KEY_KEY_SIZE -> "one of the KeySize constants, such as KEYSIZE_4096";
			default -> null;
		};
	}

	/** The settings as stored, with the declared defaults filled in */
	public static Map<String, String> current()
	{
		return PluginSettings.load(PLUGIN_ID, new KeygenSettingsContribution().getDefaults());
	}

	/**
	 * The configured algorithm, RSA when the setting names none the key generator window offers: an
	 * algorithm outside its list opened the window on an entry its dropdown does not have (#894)
	 *
	 * @return the algorithm the tool starts with
	 */
	public static KeyPairGeneratorAlgorithm algorithm()
	{
		try
		{
			KeyPairGeneratorAlgorithm stored = KeyPairGeneratorAlgorithm
				.valueOf(current().get(KEY_ALGORITHM).trim());
			return KeygenSupport.OFFERED_ALGORITHMS.contains(stored)
				? stored
				: KeyPairGeneratorAlgorithm.RSA;
		}
		catch (IllegalArgumentException | NullPointerException exception)
		{
			return KeyPairGeneratorAlgorithm.RSA;
		}
	}

	/**
	 * The offered algorithms as the setting takes them, read from the window's own list so the
	 * description cannot leave one out again: it named five of the six and missed EC (#894)
	 */
	private static String offeredAlgorithmNames()
	{
		List<String> names = KeygenSupport.OFFERED_ALGORITHMS.stream().map(Enum::name).toList();
		return String.join(", ", names.subList(0, names.size() - 1)) + " or "
			+ names.get(names.size() - 1);
	}

	/**
	 * The configured key size, 2048 bits when the setting names none that exists
	 *
	 * @return the key size the tool starts with
	 */
	public static KeySize keySize()
	{
		try
		{
			return KeySize.valueOf(current().get(KEY_KEY_SIZE).trim());
		}
		catch (IllegalArgumentException | NullPointerException exception)
		{
			return KeySize.KEYSIZE_2048;
		}
	}
}
