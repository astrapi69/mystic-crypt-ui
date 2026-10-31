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
package io.github.astrapi69.mystic.crypt.plugin.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.astrapi69.mystic.crypt.plugin.api.VaultAccess;
import io.github.astrapi69.mystic.crypt.plugin.api.VaultAccessContribution;

/**
 * How the host hands a plugin its access (docs/decisions/plugin-vault-access-2026-10-31.md): only
 * for a marker in the plugin's own namespace, and with an event before the vault locks or closes.
 */
class PluginVaultConnectionsTest
{

	@ParameterizedTest(name = "{0} marking {1}: connected {2}")
	@CsvSource(delimiter = '|', nullValues = "null", value = { "lethenon-plugin|lethenon.wallet|true",
			"lethenon|lethenon.wallet|true", "lethenon-plugin|keystore.alias|false",
			"lethenon-plugin|lethenon|false", "lethenon-plugin|lethenonx.wallet|false",
			"lethenon-plugin|''|false", "lethenon-plugin|null|false",
			"lethenon-plugin|lethenon.|false" })
	@DisplayName("a plugin is connected only for a marker in its own namespace")
	void connectsOnlyForAMarkerInTheOwnNamespace(final String pluginId, final String marker,
		final boolean connected)
	{
		PluginVaultConnections connections = new PluginVaultConnections(new HandheldVaultState());
		RecordingContribution contribution = new RecordingContribution(marker);

		boolean accepted = connections.connect(pluginId, contribution);

		assertEquals(connected, accepted);
		assertEquals(connected, contribution.access != null,
			"a refused plugin gets no access at all");
	}

	@Test
	@DisplayName("every connected plugin hears once that the vault is going, while its entries are still there")
	void tellsEachConnectedPluginOnceBeforeTheVaultGoes()
	{
		HandheldVaultState vault = new HandheldVaultState();
		vault.root.add(io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean
			.builder().id(java.util.UUID.randomUUID()).build());
		io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties.replace(vault.root.get(0),
			java.util.Map.of("lethenon.wallet", "1"), java.util.Set.of());
		PluginVaultConnections connections = new PluginVaultConnections(vault);
		RecordingContribution lethenon = new RecordingContribution("lethenon.wallet");
		RecordingContribution refused = new RecordingContribution("keystore.alias");
		connections.connect("lethenon-plugin", lethenon);
		connections.connect("lethenon-plugin", refused);

		connections.vaultClosing();

		assertEquals(List.of(1), lethenon.entriesSeenWhenClosing,
			"told once, and its entry was still readable then");
		assertEquals(List.of(), refused.entriesSeenWhenClosing, "a refused plugin hears nothing");
	}

	@Test
	@DisplayName("a plugin connected twice is told once")
	void aPluginConnectedTwiceIsToldOnce()
	{
		PluginVaultConnections connections = new PluginVaultConnections(new HandheldVaultState());
		RecordingContribution lethenon = new RecordingContribution("lethenon.wallet");
		connections.connect("lethenon-plugin", lethenon);
		connections.connect("lethenon-plugin", lethenon);

		connections.vaultClosing();

		assertEquals(1, lethenon.entriesSeenWhenClosing.size());
		assertNotNull(lethenon.access);
		assertNull(new RecordingContribution("x.y").access);
	}

	private static final class RecordingContribution implements VaultAccessContribution
	{

		private final String marker;

		private final List<Integer> entriesSeenWhenClosing = new ArrayList<>();

		private VaultAccess access;

		private RecordingContribution(final String marker)
		{
			this.marker = marker;
		}

		@Override
		public String getEntryMarker()
		{
			return marker;
		}

		@Override
		public void connect(final VaultAccess access)
		{
			this.access = access;
		}

		@Override
		public void vaultClosing()
		{
			entriesSeenWhenClosing.add(access.entries().size());
		}
	}
}
