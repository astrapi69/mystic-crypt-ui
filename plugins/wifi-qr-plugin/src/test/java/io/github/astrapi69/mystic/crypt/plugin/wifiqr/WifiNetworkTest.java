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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;

/**
 * The network an entry describes (#1120): name from "SSID" or the title, type from "Wi-Fi type",
 * hidden from "Wi-Fi hidden"
 */
class WifiNetworkTest
{

	/** Made up per run, never a literal (quality-checks.md) */
	private static final char[] PASSWORD = UUID.randomUUID().toString().toCharArray();

	private static MysticCryptEntryModelBean entry(final String title, final char[] password,
		final Map<String, String> properties)
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().id(UUID.randomUUID())
			.title(title == null ? null : title.toCharArray()).password(password).build();
		VaultEntryProperties.replace(entry, properties, Set.of());
		return entry;
	}

	@Test
	@DisplayName("the SSID property names the network, WPA is the type without one, the password is a copy")
	void theSsidProperty_namesTheNetwork()
	{
		MysticCryptEntryModelBean entry = entry("Router", PASSWORD.clone(),
			Map.of(WifiNetwork.SSID, " home "));

		WifiNetwork network = WifiNetwork.of(entry);

		assertEquals("home", network.ssid());
		assertEquals("WPA", network.type());
		assertFalse(network.hidden());
		assertArrayEquals(PASSWORD, network.password());
		assertNotSame(entry.getPassword(), network.password());
		network.wipe();
		assertArrayEquals(new char[PASSWORD.length], network.password());
		assertArrayEquals(PASSWORD, entry.getPassword(), "the entry's password is untouched");
	}

	@Test
	@DisplayName("without an SSID property the title names the network")
	void withoutSsid_theTitleNamesIt()
	{
		assertEquals("Office", WifiNetwork.of(entry("Office", PASSWORD.clone(), Map.of())).ssid());
	}

	@Test
	@DisplayName("an entry without a password is an open network, whatever its type says")
	void withoutPassword_theNetworkIsOpen()
	{
		WifiNetwork network = WifiNetwork.of(entry("Guest", null, Map.of(WifiNetwork.TYPE, "WEP")));

		assertEquals("nopass", network.type());
		assertEquals(0, network.password().length);
	}

	@ParameterizedTest(name = "type \"{0}\", hidden \"{1}\": {2}, {3}")
	@CsvSource({ "wep, yes, WEP, true", "WPA, TRUE, WPA, true", "nopass, no, nopass, false",
			"' wpa ', '', WPA, false" })
	@DisplayName("the type and the hidden flag are read in any case")
	void theTypeAndTheHiddenFlag(final String type, final String hidden, final String expected,
		final boolean expectedHidden)
	{
		WifiNetwork network = WifiNetwork.of(entry("Net", PASSWORD.clone(),
			Map.of(WifiNetwork.TYPE, type, WifiNetwork.HIDDEN, hidden)));

		assertEquals(expected, network.type());
		assertEquals(expectedHidden, network.hidden());
	}

	@Test
	@DisplayName("an entry that names no network, or an unknown type, is refused with a sentence for the user")
	void noNetworkOrAnUnknownType_isRefused()
	{
		IllegalArgumentException noName = assertThrows(IllegalArgumentException.class,
			() -> WifiNetwork.of(entry("  ", PASSWORD.clone(), Map.of())));
		assertTrue(noName.getMessage().contains("SSID"), noName.getMessage());

		IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class,
			() -> WifiNetwork
				.of(entry("Net", PASSWORD.clone(), Map.of(WifiNetwork.TYPE, "WPA2-EAP"))));
		assertTrue(unknown.getMessage().contains("WPA2-EAP"), unknown.getMessage());
	}
}
