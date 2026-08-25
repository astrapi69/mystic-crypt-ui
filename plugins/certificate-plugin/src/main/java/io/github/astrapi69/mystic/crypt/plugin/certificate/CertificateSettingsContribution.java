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
package io.github.astrapi69.mystic.crypt.plugin.certificate;

import java.util.LinkedHashMap;
import java.util.Map;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The configuration of the certificate plugin: what the wizard is prefilled with - the name in the
 * certificate, how it is signed and how long it stays valid.
 */
@Extension
public class CertificateSettingsContribution implements PluginSettingsContribution
{

	/** The plugin id, the same one {@code plugin.properties} declares */
	public static final String PLUGIN_ID = "certificate-plugin";

	/** The common name the wizard starts with */
	public static final String KEY_COMMON_NAME = "default.common.name";

	/** The signature algorithm the wizard starts with */
	public static final String KEY_SIGNATURE_ALGORITHM = "default.signature.algorithm";

	/** How many years the certificate is valid */
	public static final String KEY_VALIDITY_YEARS = "default.validity.years";

	/** The common name used when nothing else is configured */
	public static final String DEFAULT_COMMON_NAME = "mystic-crypt";

	/** The signature algorithm used when nothing else is configured */
	public static final String DEFAULT_SIGNATURE_ALGORITHM = "SHA256withRSA";

	/** The validity used when nothing else is configured */
	public static final int DEFAULT_VALIDITY_YEARS = 1;

	@Override
	public String getPluginId()
	{
		return PLUGIN_ID;
	}

	@Override
	public String getDisplayName()
	{
		return "Certificate";
	}

	@Override
	public Map<String, String> getDefaults()
	{
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put(KEY_COMMON_NAME, DEFAULT_COMMON_NAME);
		defaults.put(KEY_SIGNATURE_ALGORITHM, DEFAULT_SIGNATURE_ALGORITHM);
		defaults.put(KEY_VALIDITY_YEARS, String.valueOf(DEFAULT_VALIDITY_YEARS));
		return defaults;
	}

	@Override
	public String getDescription(String key)
	{
		return switch (key)
		{
			case KEY_COMMON_NAME -> "the CN the wizard starts with, for issuer and subject";
			case KEY_SIGNATURE_ALGORITHM -> "for instance SHA256withRSA or SHA512withRSA";
			case KEY_VALIDITY_YEARS -> "how many years the certificate stays valid";
			default -> null;
		};
	}

	/** The settings as stored, with the declared defaults filled in */
	public static Map<String, String> current()
	{
		return PluginSettings.load(PLUGIN_ID, new CertificateSettingsContribution().getDefaults());
	}

	/**
	 * The configured common name
	 *
	 * @return the common name the wizard starts with
	 */
	public static String commonName()
	{
		String configured = current().get(KEY_COMMON_NAME);
		return configured == null || configured.isBlank() ? DEFAULT_COMMON_NAME : configured.trim();
	}

	/**
	 * The configured signature algorithm
	 *
	 * @return the signature algorithm the wizard starts with
	 */
	public static String signatureAlgorithm()
	{
		String configured = current().get(KEY_SIGNATURE_ALGORITHM);
		return configured == null || configured.isBlank()
			? DEFAULT_SIGNATURE_ALGORITHM
			: configured.trim();
	}

	/**
	 * The configured validity; a value below one year would produce a certificate that is already
	 * expired and is refused
	 *
	 * @return the number of years
	 */
	public static int validityYears()
	{
		int years = PluginSettings.asInt(current(), KEY_VALIDITY_YEARS, DEFAULT_VALIDITY_YEARS);
		return years < 1 ? DEFAULT_VALIDITY_YEARS : years;
	}
}
