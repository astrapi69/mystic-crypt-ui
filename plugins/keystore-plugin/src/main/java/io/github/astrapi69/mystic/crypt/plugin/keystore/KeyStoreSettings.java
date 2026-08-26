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
package io.github.astrapi69.mystic.crypt.plugin.keystore;

import java.util.Map;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.type.KeystoreType;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The configured defaults of this plugin, read as the types they stand for.
 * <p>
 * Both ways of using the plugin go through this class - the tool window and the command line - so a
 * setting means the same thing in both. A value that a hand-edited settings file made unusable
 * falls back to the built-in default instead of failing.
 */
public final class KeyStoreSettings
{

	private KeyStoreSettings()
	{
	}

	/** The settings as stored, with the declared defaults filled in */
	public static Map<String, String> values()
	{
		return KeyStoreSettingsContribution.current();
	}

	/**
	 * The configured store type
	 *
	 * @return the store type, the first offered one when the setting is unusable
	 */
	public static KeystoreType type()
	{
		return type(values());
	}

	/**
	 * The configured store type
	 *
	 * @param values
	 *            the settings to read
	 * @return the store type, the first offered one when the setting is unusable
	 */
	public static KeystoreType type(final Map<String, String> values)
	{
		KeystoreType type = parse(KeystoreType.class,
			values.get(KeyStoreSettingsContribution.KEY_STORE_TYPE));
		return type != null && KeyStoreSupport.USABLE_TYPES.contains(type)
			? type
			: KeyStoreSupport.USABLE_TYPES.get(0);
	}

	/**
	 * The configured key algorithm
	 *
	 * @return the key algorithm, the first offered one when the setting is unusable
	 */
	public static KeyPairGeneratorAlgorithm algorithm()
	{
		return algorithm(values());
	}

	/**
	 * The configured key algorithm
	 *
	 * @param values
	 *            the settings to read
	 * @return the key algorithm, the first offered one when the setting is unusable
	 */
	public static KeyPairGeneratorAlgorithm algorithm(final Map<String, String> values)
	{
		KeyPairGeneratorAlgorithm algorithm = parse(KeyPairGeneratorAlgorithm.class,
			values.get(KeyStoreSettingsContribution.KEY_ALGORITHM));
		return algorithm != null && KeyStoreSupport.KEY_ALGORITHMS.contains(algorithm)
			? algorithm
			: KeyStoreSupport.KEY_ALGORITHMS.get(0);
	}

	/**
	 * The configured certificate subject
	 *
	 * @return the distinguished name
	 */
	public static String distinguishedName()
	{
		return distinguishedName(values());
	}

	/**
	 * The configured certificate subject
	 *
	 * @param values
	 *            the settings to read
	 * @return the distinguished name
	 */
	public static String distinguishedName(final Map<String, String> values)
	{
		String name = values.get(KeyStoreSettingsContribution.KEY_DISTINGUISHED_NAME);
		return name == null || name.isBlank() ? "CN=mystic-crypt" : name.trim();
	}

	/**
	 * The configured key size for the size based algorithms
	 *
	 * @return the key size
	 */
	public static int keySize()
	{
		return PluginSettings.asInt(values(), KeyStoreSettingsContribution.KEY_KEY_SIZE,
			KeyStoreSupport.DEFAULT_KEY_SIZE);
	}

	/**
	 * How long a generated certificate is valid
	 *
	 * @return the number of days
	 */
	public static int daysValid()
	{
		return PluginSettings.asInt(values(), KeyStoreSettingsContribution.KEY_DAYS_VALID,
			KeyStoreSupport.DEFAULT_DAYS_VALID);
	}

	/**
	 * The length of a generated symmetric key; a length AES does not have falls back to 256
	 *
	 * @return the key size in bits
	 */
	public static int secretKeySize()
	{
		int size = PluginSettings.asInt(values(), KeyStoreSettingsContribution.KEY_SECRET_KEY_SIZE,
			256);
		return size == 128 || size == 192 || size == 256 ? size : 256;
	}

	private static <T extends Enum<T>> T parse(final Class<T> type, final String value)
	{
		if (value == null || value.isBlank())
		{
			return null;
		}
		try
		{
			return Enum.valueOf(type, value.trim());
		}
		catch (IllegalArgumentException exception)
		{
			// a value that is not one of the constants means: use the built-in default
			return null;
		}
	}
}
