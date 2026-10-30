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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.linguafranca.pwdb.format.KdbxCredentials;
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase;

import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;

/**
 * The wallet's words, through a real KDBX file that keepassxc-cli reads: the host writes the entry
 * with {@code lethenon.mnemonic} as a protected property, keepassxc-cli reports the protection flag,
 * and after the words are removed the file holds no trace of them. Decision:
 * docs/decisions/lethenon-wallet-in-vault-2026-10-30.md.
 */
class LethenonMnemonicKeePassXcTest
{

	private static final String MNEMONIC = "lethenon.mnemonic";

	private static final String WORDS = "abandon abandon abandon abandon abandon abandon abandon "
		+ "abandon abandon abandon abandon about";

	private static final String PASSWORD = TestPasswords.throwaway();

	@TempDir
	Path directory;

	@Test
	@DisplayName("the words are written protected and keepassxc-cli reads the flag")
	void theWordsAreProtectedInTheFileKeePassXcReads() throws Exception
	{
		MysticCryptEntryModelBean bean = wallet(Map.of(MNEMONIC, WORDS), Set.of(MNEMONIC));

		KdbxFacts facts = exportAndRead(bean, "protected.kdbx");

		assertEquals(WORDS, facts.onlyEntry().customProperties().get(MNEMONIC),
			"keepassxc-cli must read the words back");
		assertTrue(facts.onlyEntry().customPropertyProtection().get(MNEMONIC),
			"keepassxc-cli must report the words as protected");
	}

	@Test
	@DisplayName("after removing the words keepassxc-cli finds no trace of them in the file")
	void afterRemovalNoTraceOfTheWordsIsLeftInTheFile() throws Exception
	{
		MysticCryptEntryModelBean bean = wallet(Map.of(MNEMONIC, WORDS), Set.of(MNEMONIC));
		VaultEntryProperties.replace(bean, Map.of(), Set.of());

		KdbxFacts facts = exportAndRead(bean, "removed.kdbx");

		assertFalse(facts.onlyEntry().customProperties().containsKey(MNEMONIC),
			"the property must be gone from the file");
		assertFalse(facts.onlyEntry().customProperties().containsValue(WORDS),
			"no value of the file may be the words");
	}

	private MysticCryptEntryModelBean wallet(final Map<String, String> values,
		final Set<String> protectedNames)
	{
		MysticCryptEntryModelBean bean = MysticCryptEntryModelBean.builder()
			.title("lethenon wallet (testnet)".toCharArray()).build();
		VaultEntryProperties.replace(bean, values, protectedNames);
		return bean;
	}

	/** Writes the entry into a KDBX file the way the host's export does, and reads it back */
	private KdbxFacts exportAndRead(final MysticCryptEntryModelBean bean, final String name)
		throws Exception
	{
		KdbxDatabase database = new KdbxDatabase();
		database.getRootGroup().addEntry(KeePassEntryConverter.toJacksonEntry(database, bean));
		File file = directory.resolve(name).toFile();
		try (OutputStream out = new FileOutputStream(file))
		{
			database.write(new KdbxCredentials(PASSWORD.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
				out);
		}
		return KdbxFacts.of(KeePassXcDump.xmlOf(file, PASSWORD));
	}
}
