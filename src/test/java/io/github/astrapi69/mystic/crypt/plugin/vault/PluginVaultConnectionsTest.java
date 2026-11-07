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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;
import io.github.astrapi69.mystic.crypt.plugin.api.VaultAccess;
import io.github.astrapi69.mystic.crypt.plugin.api.VaultAccessContribution;
import io.github.astrapi69.mystic.crypt.plugin.api.VaultExport;

/**
 * How the host hands a plugin its access (docs/decisions/plugin-vault-access-2026-10-31.md): only
 * for a marker in the plugin's own namespace, and with an event before the vault locks or closes;
 * and which plugin's line the host shows before something leaves the vault (#810).
 */
class PluginVaultConnectionsTest
{

	@ParameterizedTest(name = "{0} marking {1}: connected {2}")
	@CsvSource(delimiter = '|', nullValues = "null", value = {
			"lethenon-plugin|lethenon.wallet|true", "lethenon|lethenon.wallet|true",
			"lethenon-plugin|keystore.alias|false", "lethenon-plugin|lethenon|false",
			"lethenon-plugin|lethenonx.wallet|false", "lethenon-plugin|''|false",
			"lethenon-plugin|null|false", "lethenon-plugin|lethenon.|false" })
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
		io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties.replace(
			vault.root.get(0), java.util.Map.of("lethenon.wallet", "1"), java.util.Set.of());
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

	@Test
	@DisplayName("the export of the vault gets the line of every connected plugin that gives one, in their order")
	void databaseExportNotices_areTheLinesOfTheConnectedPlugins()
	{
		PluginVaultConnections connections = new PluginVaultConnections(new HandheldVaultState());
		connections.connect("lethenon-plugin",
			new NoticeContribution("lethenon.wallet", export -> Optional.of("wallets, " + export)));
		connections.connect("keystore-plugin", new RecordingContribution("keystore.alias"));
		connections.connect("other-plugin",
			new NoticeContribution("other.thing", export -> Optional.of("things, " + export)));

		assertEquals(List.of("wallets, DATABASE", "things, DATABASE"),
			connections.databaseExportNotices());
	}

	@Test
	@DisplayName("a plugin that fails, gives a blank line or gives null gives none, and the others still give theirs")
	void aPluginThatFailsOrGivesNothing_givesNoLine()
	{
		PluginVaultConnections connections = new PluginVaultConnections(new HandheldVaultState());
		connections.connect("failing-plugin", new NoticeContribution("failing.entry", export -> {
			throw new IllegalStateException("the plugin is broken");
		}));
		connections.connect("blank-plugin",
			new NoticeContribution("blank.entry", export -> Optional.of("  ")));
		connections.connect("null-plugin", new NoticeContribution("null.entry", export -> null));
		connections.connect("lethenon-plugin",
			new NoticeContribution("lethenon.wallet", export -> Optional.of("the good line")));

		assertEquals(List.of("the good line"), connections.databaseExportNotices());
	}

	@Test
	@DisplayName("saving an attachment gets the line of the plugin that marks the entry, and of no other")
	void attachmentExportNotice_isTheLineOfThePluginThatMarksTheEntry()
	{
		PluginVaultConnections connections = new PluginVaultConnections(new HandheldVaultState());
		connections.connect("other-plugin",
			new NoticeContribution("other.thing", export -> Optional.of("thing, " + export)));
		connections.connect("lethenon-plugin",
			new NoticeContribution("lethenon.wallet", export -> Optional.of("wallet, " + export)));
		connections.connect("lethenon-plugin",
			new NoticeContribution("keystore.alias", export -> Optional.of("refused, " + export)));

		assertEquals(Optional.of("wallet, ATTACHMENT"),
			connections.attachmentExportNotice(anEntryWith("lethenon.wallet")));
		assertEquals(Optional.empty(), connections.attachmentExportNotice(anEntryWith("note")),
			"an entry no plugin marks got a line");
		assertEquals(Optional.empty(),
			connections.attachmentExportNotice(anEntryWith("keystore.alias")),
			"a plugin that was refused its marker spoke for the entry");
	}

	private static MysticCryptEntryModelBean anEntryWith(final String propertyName)
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();
		VaultEntryProperties.replace(entry, Map.of(propertyName, "1"), Set.of());
		return entry;
	}

	private static final class NoticeContribution implements VaultAccessContribution
	{

		private final String marker;

		private final Function<VaultExport, Optional<String>> notice;

		private NoticeContribution(final String marker,
			final Function<VaultExport, Optional<String>> notice)
		{
			this.marker = marker;
			this.notice = notice;
		}

		@Override
		public String getEntryMarker()
		{
			return marker;
		}

		@Override
		public void connect(final VaultAccess access)
		{
		}

		@Override
		public Optional<String> exportNotice(final VaultExport export)
		{
			return notice.apply(export);
		}
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
