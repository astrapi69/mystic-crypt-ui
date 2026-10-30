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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase;
import org.linguafranca.pwdb.kdbx.jackson.KdbxEntry;

import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;

/**
 * The wallet's words, as the lethenon wallet tools store them in a vault entry (decision
 * docs/decisions/lethenon-wallet-in-vault-2026-10-30.md, option 2 as the user's choice): a protected
 * custom property {@code lethenon.mnemonic} keeps its protection flag through the KDBX entry and back,
 * and removing it leaves no trace in the entry.
 */
class LethenonMnemonicKeePassTest
{

	private static final String MNEMONIC = "lethenon.mnemonic";

	private static final String WORDS = "abandon abandon abandon abandon abandon abandon abandon "
		+ "abandon abandon abandon abandon about";

	@Test
	@DisplayName("the words keep their protection flag through the KDBX entry and back")
	void theWordsKeepTheirProtectionThroughTheKeePassEntry()
	{
		MysticCryptEntryModelBean bean = MysticCryptEntryModelBean.builder().title("lethenon wallet".toCharArray())
			.build();
		VaultEntryProperties.replace(bean, Map.of(MNEMONIC, WORDS), Set.of(MNEMONIC));

		KdbxEntry written = KeePassEntryConverter.toJacksonEntry(new KdbxDatabase(), bean);
		MysticCryptEntryModelBean read = KeePassEntryConverter.toEntryModelBean(written);

		assertTrue(written.getPropertyValue(MNEMONIC).isProtected(),
			"the KDBX entry must mark the words as protected");
		assertEquals(WORDS, VaultEntryProperties.valuesOf(read).get(MNEMONIC));
		assertTrue(VaultEntryProperties.protectedNamesOf(read).contains(MNEMONIC),
			"a protected property must not come back unprotected");
	}

	@Test
	@DisplayName("after removing the words no trace of them is left in the KDBX entry")
	void removingTheWordsLeavesNoTraceInTheKeePassEntry()
	{
		MysticCryptEntryModelBean bean = MysticCryptEntryModelBean.builder().title("lethenon wallet".toCharArray())
			.build();
		VaultEntryProperties.replace(bean, Map.of(MNEMONIC, WORDS), Set.of(MNEMONIC));
		VaultEntryProperties.replace(bean, Map.of(), Set.of());

		KdbxEntry written = KeePassEntryConverter.toJacksonEntry(new KdbxDatabase(), bean);

		assertFalse(written.getPropertyNames().contains(MNEMONIC), "the property must be gone");
		assertNull(VaultEntryProperties.valuesOf(KeePassEntryConverter.toEntryModelBean(written))
			.get(MNEMONIC));
		assertFalse(List.copyOf(VaultEntryProperties.valuesOf(bean).values()).contains(WORDS),
			"the entry model must not still hold the words");
	}
}
