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
package io.github.astrapi69.mystic.crypt.keepass;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase;
import org.linguafranca.pwdb.kdbx.jackson.KdbxEntry;

import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryAttachments;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;

/**
 * A lethenon wallet entry through the KDBX entry (decision
 * docs/decisions/lethenon-wallet-in-vault-2026-10-30.md, decision 4): the sealed attachment
 * {@code wallet.lethwf} is exported byte for byte, and the words, which the default keeps out of
 * the vault, are nowhere in the exported entry.
 */
class LethenonWalletKeePassTest
{

	private static final String ATTACHMENT = "wallet.lethwf";

	private static final byte[] SEALED = "LETHWF sealed wallet bytes"
		.getBytes(StandardCharsets.US_ASCII);

	private static final String WORDS = "abandon abandon abandon abandon abandon abandon abandon "
		+ "abandon abandon abandon abandon about";

	@Test
	@DisplayName("a KDBX export carries the sealed attachment byte for byte and not the words")
	void theExportCarriesTheSealedAttachmentAndNotTheWords()
	{
		MysticCryptEntryModelBean bean = MysticCryptEntryModelBean.builder()
			.title("lethenon wallet (testnet)".toCharArray()).build();
		VaultEntryProperties.replace(bean,
			Map.of("lethenon.wallet", "1", "lethenon.network", "testnet"), Set.of());
		VaultEntryAttachments.put(bean, ATTACHMENT, SEALED);

		KdbxEntry written = KeePassEntryConverter.toJacksonEntry(new KdbxDatabase(), bean);
		MysticCryptEntryModelBean read = KeePassEntryConverter.toEntryModelBean(written);

		assertArrayEquals(SEALED, written.getBinaryProperty(ATTACHMENT),
			"the KDBX entry must carry the sealed wallet unchanged");
		assertArrayEquals(SEALED, VaultEntryAttachments.contentOf(read, ATTACHMENT).orElseThrow());
		assertEquals("1", VaultEntryProperties.valuesOf(read).get("lethenon.wallet"));
		for (String name : written.getPropertyNames())
		{
			String value = written.getPropertyValue(name).getValueAsString();
			assertFalse(value != null && value.contains("abandon"),
				"no field of the exported entry may hold a word: " + name);
		}
		assertFalse(new String(written.getBinaryProperty(ATTACHMENT), StandardCharsets.US_ASCII)
			.contains(WORDS));
	}
}
