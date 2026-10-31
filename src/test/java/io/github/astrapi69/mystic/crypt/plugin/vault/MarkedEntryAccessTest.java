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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;
import io.github.astrapi69.mystic.crypt.plugin.api.VaultAccess;
import io.github.astrapi69.mystic.crypt.plugin.api.VaultAccessRefusedException;

/**
 * A plugin's access to the open vault, after the principle of least privilege
 * (docs/decisions/plugin-vault-access-2026-10-31.md): it sees and changes only the entries with
 * its own marker, and the access refuses on its own when the vault is closed, locked or read-only.
 */
class MarkedEntryAccessTest
{

	private static final String MARKER = "lethenon.wallet";

	private HandheldVaultState vault;

	private VaultAccess access;

	private MysticCryptEntryModelBean marked;

	private MysticCryptEntryModelBean foreign;

	@BeforeEach
	void aVaultWithOneMarkedAndOneForeignEntry()
	{
		vault = new HandheldVaultState();
		marked = entry("my wallet", Map.of(MARKER, "1"));
		foreign = entry("the bank", Map.of("keystore.alias", "main"));
		vault.root.add(foreign);
		vault.group.add(marked);
		access = new MarkedEntryAccess(MARKER, vault);
	}

	@Test
	@DisplayName("a plugin reads only the entries with its marker, as copies")
	void readsOnlyItsOwnEntriesAsCopies()
	{
		var entries = access.entries();

		assertEquals(1, entries.size());
		assertEquals(marked.getId(), entries.get(0).getId());
		entries.get(0).setTitle("changed in the copy".toCharArray());
		assertEquals("my wallet", new String(marked.getTitle()), "the vault's entry is untouched");
	}

	@Test
	@DisplayName("a plugin can neither change nor remove an entry without its marker, even by its identifier")
	void cannotChangeOrRemoveAForeignEntry()
	{
		MysticCryptEntryModelBean forged = foreign.duplicate();
		forged.setTitle("taken over".toCharArray());
		VaultEntryProperties.replace(forged, Map.of(MARKER, "1"), Set.of());

		assertThrows(VaultAccessRefusedException.class, () -> access.change(forged));
		assertThrows(VaultAccessRefusedException.class, () -> access.remove(foreign.getId()));

		assertEquals("the bank", new String(foreign.getTitle()));
		assertEquals(Map.of("keystore.alias", "main"), VaultEntryProperties.valuesOf(foreign));
		assertTrue(vault.root.contains(foreign), "the foreign entry is still in the vault");
		assertEquals(0, vault.changes);
	}

	@Test
	@DisplayName("a change that takes the marker off is refused, and the stored entry keeps it")
	void cannotTakeTheMarkerOff()
	{
		MysticCryptEntryModelBean unmarked = access.entries().get(0);
		VaultEntryProperties.replace(unmarked, Map.of(), Set.of());

		assertThrows(VaultAccessRefusedException.class, () -> access.change(unmarked));

		assertEquals("1", VaultEntryProperties.valuesOf(marked).get(MARKER));
		assertEquals(0, vault.changes);
	}

	@Test
	@DisplayName("a change to its own entry is stored and marks the vault changed")
	void changesItsOwnEntry()
	{
		MysticCryptEntryModelBean changed = access.entries().get(0);
		changed.setTitle("lethenon wallet (testnet)".toCharArray());

		access.change(changed);

		assertEquals("lethenon wallet (testnet)", new String(marked.getTitle()));
		assertEquals(1, vault.changes);
	}

	@Test
	@DisplayName("an added entry goes into the root group with an identifier, and marks the vault changed")
	void addsIntoTheRootGroup()
	{
		MysticCryptEntryModelBean wallet = entry("new wallet", Map.of(MARKER, "1"));
		wallet.setId(null);

		access.add(wallet);

		assertEquals(2, vault.root.size());
		assertNotNull(vault.root.get(1).getId(), "the host gives it an identifier");
		assertEquals(1, vault.changes);
		assertEquals(2, access.entries().size());
	}

	@Test
	@DisplayName("an entry without the plugin's marker is not added")
	void doesNotAddAnEntryWithoutItsMarker()
	{
		assertThrows(VaultAccessRefusedException.class,
			() -> access.add(entry("no marker", Map.of())));

		assertEquals(1, vault.root.size());
		assertEquals(0, vault.changes);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("vaultsThatRefuse")
	@DisplayName("adding, changing and removing are refused with the reason, and the vault stays as it was")
	void refusesWhenTheVaultMayNotBeWritten(final String state,
		final Consumer<HandheldVaultState> make, final String reason)
	{
		make.accept(vault);
		MysticCryptEntryModelBean changed = marked.duplicate();
		changed.setTitle("changed".toCharArray());

		VaultAccessRefusedException adding = assertThrows(VaultAccessRefusedException.class,
			() -> access.add(entry("new", Map.of(MARKER, "1"))));
		assertThrows(VaultAccessRefusedException.class, () -> access.change(changed));
		assertThrows(VaultAccessRefusedException.class, () -> access.remove(marked.getId()));

		assertTrue(adding.getMessage().contains(reason), adding.getMessage());
		assertEquals(1, vault.root.size());
		assertEquals("my wallet", new String(marked.getTitle()));
		assertTrue(vault.group.contains(marked));
		assertEquals(0, vault.changes);
	}

	static Stream<Arguments> vaultsThatRefuse()
	{
		return Stream.of(
			Arguments.of("no vault open", (Consumer<HandheldVaultState>)state -> state.open = false,
				"no vault is open"),
			Arguments.of("the vault is locked",
				(Consumer<HandheldVaultState>)state -> state.unlocked = false, "locked"),
			Arguments.of("the vault is read-only",
				(Consumer<HandheldVaultState>)state -> state.readOnlyReason = "written by 9.0",
				"written by 9.0"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("vaultsThatRefuse")
	@DisplayName("a closed or locked vault reads as no entries; a read-only one can still be read")
	void readsNothingWhileUnavailable(final String state, final Consumer<HandheldVaultState> make,
		final String reason)
	{
		make.accept(vault);

		boolean readable = vault.open && vault.unlocked;
		assertEquals(readable, access.isAvailable());
		assertEquals(readable ? 1 : 0, access.entries().size());
	}

	@Test
	@DisplayName("removing its own entry takes it out of the vault and marks the vault changed")
	void removesItsOwnEntry()
	{
		access.remove(marked.getId());

		assertFalse(vault.group.contains(marked));
		assertEquals(1, vault.changes);
	}

	private static MysticCryptEntryModelBean entry(final String title,
		final Map<String, String> properties)
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().id(UUID.randomUUID())
			.title(title.toCharArray()).build();
		VaultEntryProperties.replace(entry, properties, Set.of());
		return entry;
	}
}
