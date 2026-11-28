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
package io.github.astrapi69.mystic.crypt.plugin.wifiqr;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;

/**
 * The Wi-Fi network an entry describes (#1120): its name from the custom property {@value #SSID},
 * or the title when there is none; its password from the entry's password; its type from the custom
 * property {@value #TYPE}, WPA when there is none and {@code nopass} for an entry without a
 * password; hidden when the custom property {@value #HIDDEN} says {@code true} or {@code yes}.
 *
 * @param ssid
 *            the network's name
 * @param password
 *            the password, empty for an open network; the caller overwrites it when done
 * @param type
 *            {@code WPA}, {@code WEP} or {@code nopass}
 * @param hidden
 *            whether the network hides its name
 */
record WifiNetwork(String ssid, char[] password, String type, boolean hidden) {

	/** The custom property that names the network */
	static final String SSID = "SSID";

	/** The custom property that names the type */
	static final String TYPE = "Wi-Fi type";

	/** The custom property that says the network is hidden */
	static final String HIDDEN = "Wi-Fi hidden";

	/** The type of an open network */
	static final String OPEN = "nopass";

	/**
	 * The network of an entry
	 *
	 * @param entry
	 *            the entry, a copy the host handed over
	 * @return the network; its password is a copy of the entry's
	 * @throws IllegalArgumentException
	 *             when the entry names no network, or its type is none of WPA, WEP or nopass; the
	 *             message is the sentence for the user
	 */
	static WifiNetwork of(final MysticCryptEntryModelBean entry)
	{
		Map<String, String> properties = VaultEntryProperties.valuesOf(entry);
		String ssid = properties
			.getOrDefault(SSID, entry.getTitle() == null ? "" : new String(entry.getTitle()))
			.strip();
		if (ssid.isEmpty())
		{
			throw new IllegalArgumentException(WifiQrMessages.getString("wifiqr.no.network",
				"This entry names no network: give it a custom property \"SSID\", or a title."));
		}
		char[] password = entry.getPassword() == null ? new char[0] : entry.getPassword().clone();
		return new WifiNetwork(ssid, password, typeOf(properties.get(TYPE), password),
			isTrue(properties.get(HIDDEN)));
	}

	/** Overwrites the password this network holds */
	void wipe()
	{
		Arrays.fill(password, '\0');
	}

	private static String typeOf(final String named, final char[] password)
	{
		if (password.length == 0)
		{
			return OPEN;
		}
		if (named == null || named.isBlank())
		{
			return "WPA";
		}
		String type = named.strip();
		for (String known : new String[] { "WPA", "WEP", OPEN })
		{
			if (known.equalsIgnoreCase(type))
			{
				return known;
			}
		}
		throw new IllegalArgumentException(WifiQrMessages.format("wifiqr.unknown.type",
			"The custom property \"Wi-Fi type\" holds \"{0}\"; it is one of WPA, WEP or nopass.",
			type));
	}

	private static boolean isTrue(final String value)
	{
		if (value == null)
		{
			return false;
		}
		String said = value.strip().toLowerCase(Locale.ROOT);
		return said.equals("true") || said.equals("yes");
	}
}
