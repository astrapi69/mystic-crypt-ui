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

import java.util.LinkedHashMap;
import java.util.Map;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The configuration of this plugin: which store type, key algorithm and certificate details the
 * tool starts with, so someone who always works with the same kind of key store does not have to
 * set it up again on every start.
 */
@Extension
public class KeyStoreSettingsContribution implements PluginSettingsContribution
{

	/** The plugin id, the same one {@code plugin.properties} declares */
	public static final String PLUGIN_ID = "keystore-plugin";

	/** The store type the tool starts with */
	public static final String KEY_STORE_TYPE = "default.store.type";

	/** The key algorithm the tool starts with */
	public static final String KEY_ALGORITHM = "default.key.algorithm";

	/** The certificate subject the tool starts with */
	public static final String KEY_DISTINGUISHED_NAME = "default.distinguished.name";

	/** The key size used for the size based algorithms */
	public static final String KEY_KEY_SIZE = "default.key.size";

	/** How long a generated certificate is valid */
	public static final String KEY_DAYS_VALID = "default.days.valid";

	/** The length of a generated symmetric key */
	public static final String KEY_SECRET_KEY_SIZE = "default.secret.key.size";

	@Override
	public String getPluginId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getDisplayName()
	{
		return "Key Store";
	}

	@Override
	public Map<String, String> getDefaults()
	{
		// a LinkedHashMap: this is also the order the settings dialog shows them in
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put(KEY_STORE_TYPE, KeyStoreSupport.USABLE_TYPES.get(0).name());
		defaults.put(KEY_ALGORITHM, KeyStoreSupport.KEY_ALGORITHMS.get(0).name());
		defaults.put(KEY_DISTINGUISHED_NAME, "CN=mystic-crypt, O=mystic-crypt");
		defaults.put(KEY_KEY_SIZE, String.valueOf(KeyStoreSupport.DEFAULT_KEY_SIZE));
		defaults.put(KEY_DAYS_VALID, String.valueOf(KeyStoreSupport.DEFAULT_DAYS_VALID));
		defaults.put(KEY_SECRET_KEY_SIZE, "256");
		return defaults;
	}

	@Override
	public String getDescription(String key)
	{
		return switch (key)
		{
			case KEY_STORE_TYPE -> "PKCS12, JKS or JCEKS";
			case KEY_ALGORITHM -> "RSA, EC or ML_DSA_65";
			case KEY_DISTINGUISHED_NAME -> "the subject of a generated certificate";
			case KEY_KEY_SIZE -> "only used by the size based algorithms such as RSA";
			case KEY_DAYS_VALID -> "how long a generated certificate is valid";
			case KEY_SECRET_KEY_SIZE -> "the length of a generated AES key: 128, 192 or 256";
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
		return PluginSettings.load(PLUGIN_ID, new KeyStoreSettingsContribution().getDefaults());
	}
}
