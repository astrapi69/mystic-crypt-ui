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
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;

/**
 * A vault the published 8.14 wrote holds an entry property as a {@link KeyValuePair} in its stored
 * XML, under the class's qualified name (it stands in {@code VaultXmlCodec.VAULT_TYPES}). This test
 * pins that the property reads back identically - across the silly-bean versions the class path may
 * carry, since the archived library's 4.0 ships the identical class (#864, mystic-crypt#278: same
 * fields, same {@code serialVersionUID}). The 7.2 fixture of {@link VaultsOfSevenXOpenTest} pins
 * the same for the oldest release 8.x still opens (#624); this one adds the vault 8.14 itself
 * wrote.
 */
class VaultOfEightFourteenOpensTest
{

	/**
	 * Opens {@code vault-8.14.mcrdb}, written by the published 8.14 through its password save path
	 * ({@code scripts/fixtures/WriteWithAnOldRelease.java}), and reads every field of the entry
	 * that carries the {@link KeyValuePair} property.
	 *
	 * @param directory
	 *            the temporary directory the fixture is copied into
	 * @throws IOException
	 *             when the fixture cannot be copied
	 */
	@Test
	@DisplayName("a vault 8.14 wrote opens, and its KeyValuePair property reads back identically")
	void aVaultOfEightFourteen_opens_withItsKeyValuePairProperty(@TempDir final File directory)
		throws IOException
	{
		ApplicationModelBean model = SampleVaults.open(fixture(directory),
			TestPasswords.VAULT_OF_8_14_FIXTURE.toCharArray());

		assertEquals(List.of(
			"ENTRY group=mykeys title=the mail userName= password=also written by the release url="
				+ " notes= properties=0 attachments=0",
			"ENTRY group=root title=the bank userName=account holder password=written by the release"
				+ " url=https://bank.example.org notes=Grüße, with umlauts properties=1 attachments=1"),
			SampleVaults.entryLines(model), "the groups and entries 8.14 wrote");
		MysticCryptEntryModelBean bank = SampleVaults.entriesOf(model, "root").getFirst();
		KeyValuePair<String, String> property = bank.getProperties().getFirst();
		assertEquals("TOTP seed", property.getKey());
		assertEquals("JBSWY3DPEHPK3PXP", property.getValue());
	}

	private static File fixture(final File directory) throws IOException
	{
		File copy = new File(directory, "vault-8.14.mcrdb");
		try (InputStream in = VaultOfEightFourteenOpensTest.class
			.getResourceAsStream("/compat/vaults/vault-8.14.mcrdb"))
		{
			assertTrue(in != null, "the fixture of 8.14 is on the class path");
			Files.copy(in, copy.toPath());
		}
		return copy;
	}
}
