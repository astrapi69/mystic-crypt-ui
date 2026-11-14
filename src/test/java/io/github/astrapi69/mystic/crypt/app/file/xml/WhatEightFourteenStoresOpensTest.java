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
package io.github.astrapi69.mystic.crypt.app.file.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;

/**
 * What the published 8.14 stores beyond the plain entries of {@link VaultOfEightFourteenOpensTest}
 * opens with the reader of this build, and every value is the one 8.14 itself reads from the same
 * file: an entry the release's own "Import from KeePass" brought in, with its version history and
 * its protected property, and a lethenon wallet the release's lethenon plugin put into the vault
 * through the host's plugin vault access, the one path a plugin has into a vault (#675). Both
 * vaults were written by the 8.14 application jar (and, for the wallet, its lethenon plugin) taken
 * out of the published installer; the expected values were read from them by 8.14 itself
 * ({@code compat/vaults/README.md})
 */
class WhatEightFourteenStoresOpensTest
{

	@Test
	@DisplayName("an entry 8.14 imported from KeePass opens with its history, protected key, times and attachment")
	void anEntryEightFourteenImportedFromKeePass_opensAsEightFourteenReadsIt(
		@TempDir final File directory) throws Exception
	{
		ApplicationModelBean model = SampleVaults.open(
			fixture(directory, "vault-8.14-keepass-import.mcrdb"),
			TestPasswords.VAULT_OF_8_14_FIXTURE.toCharArray());

		MysticCryptEntryModelBean imported = SampleVaults.entriesOf(model, "Team").getFirst();
		assertEquals("Titel test foo", new String(imported.getTitle()));
		assertEquals("foo", new String(imported.getUserName()));
		assertEquals("bar", new String(imported.getPassword()));
		assertEquals("http://example.com", new String(imported.getUrl()));
		assertEquals("das kommt nicht in Fräge mit ümlauteöüä", new String(imported.getNotes()));
		assertEquals(Map.of("foo", "bar", "key", "value"), propertiesOf(imported));
		assertEquals(Set.of("key"), imported.getProtectedPropertyKeys());
		assertEquals(Integer.valueOf(57), imported.getKeePassIconIndex());
		assertEquals(OffsetDateTime.parse("2026-09-16T07:21:23Z"), imported.getCreationTime());
		assertEquals(1, imported.getHistory().size(), "the one history version KeePassXC wrote");
		MysticCryptEntryModelBean version = imported.getHistory().getFirst();
		assertEquals("Titel test foo", new String(version.getTitle()));
		assertEquals("bar", new String(version.getPassword()));
		assertEquals(OffsetDateTime.parse("2026-09-16T07:23:37Z"),
			version.getLastModificationTime());
		assertEquals("foo.txt", imported.getResources().getFirst().getName());
		assertEquals("e49fa7b1520f72614ca1086b55645f9f6624e1ed8627b8a1d50103bca7eba9f9",
			sha256(imported.getResources().getFirst().getContent()));
	}

	@Test
	@DisplayName("a lethenon wallet 8.14 stored through the plugin vault access opens with its properties and sealed file")
	void aWalletEightFourteenStored_opensAsEightFourteenReadsIt(@TempDir final File directory)
		throws Exception
	{
		ApplicationModelBean model = SampleVaults.open(
			fixture(directory, "vault-8.14-lethenon-wallet.mcrdb"),
			TestPasswords.VAULT_OF_8_14_FIXTURE.toCharArray());

		List<MysticCryptEntryModelBean> root = SampleVaults.entriesOf(model, "root");
		MysticCryptEntryModelBean wallet = root.stream()
			.filter(entry -> new String(entry.getTitle()).equals("lethenon wallet (testnet)"))
			.findFirst().orElseThrow();
		Map<String, String> properties = propertiesOf(wallet);
		assertEquals(List.of("lethenon.network", "lethenon.wallet", "lethenon.address",
			"lethenon.account.ml-dsa-65"), List.copyOf(properties.keySet()));
		assertEquals("testnet", properties.get("lethenon.network"));
		assertEquals("1", properties.get("lethenon.wallet"));
		assertEquals(
			"302a300506032b656e0321006a2343cb05068496664ad79e66f8976454dd0a97cbfbb09f4321a885bbff4826"
				+ ":302a300506032b6570032100afe170299884ce83f566b0123306d7529e465be2c745000e5e8dc9cfc4eb8970",
			properties.get("lethenon.address"));
		assertEquals("85a35f23d476f1ca939022e1c78df03d3068947f6b6174f90ed25ea23c3a4124",
			sha256(properties.get("lethenon.account.ml-dsa-65").getBytes(StandardCharsets.UTF_8)));
		assertEquals("wallet.lethwf", wallet.getResources().getFirst().getName());
		assertEquals("cbd92b23afd72e316a94cea2e1a75c99482146cb55809502f8a261e1ab9e6c4a",
			sha256(wallet.getResources().getFirst().getContent()), "the sealed wallet, unchanged");
		assertTrue(root.stream().anyMatch(entry -> new String(entry.getTitle()).equals("the bank")),
			"the entry beside it opens too");
	}

	private static Map<String, String> propertiesOf(final MysticCryptEntryModelBean entry)
	{
		Map<String, String> properties = new LinkedHashMap<>();
		for (KeyValuePair<String, String> property : entry.getProperties())
		{
			properties.put(property.getKey(), property.getValue());
		}
		return properties;
	}

	private static String sha256(final byte[] content) throws Exception
	{
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
	}

	private static File fixture(final File directory, final String name) throws IOException
	{
		File copy = new File(directory, name);
		try (InputStream in = WhatEightFourteenStoresOpensTest.class
			.getResourceAsStream("/compat/vaults/" + name))
		{
			assertTrue(in != null, name + " is on the class path");
			Files.copy(in, copy.toPath());
		}
		return copy;
	}
}
