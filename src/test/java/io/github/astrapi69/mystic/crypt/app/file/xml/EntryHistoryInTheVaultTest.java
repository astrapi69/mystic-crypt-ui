/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.app.file.xml;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;

/**
 * An entry carries its KeePass history and the names of its protected properties through the vault,
 * and an entry that has neither writes nothing for them (#402).
 * <p>
 * The second half is the condition the maintainer set for adding either field (D1): 8.5 fails on
 * any element it does not know, even an empty one, so a vault that holds no history and no
 * protected property has to look to 8.5 exactly as it did. XStream writes no element for a
 * {@code null} field; the entry keeps an empty one as {@code null} so that it stays that way.
 */
class EntryHistoryInTheVaultTest
{

	private static final String CURRENT_TITLE = "the bank";

	private static final String PREVIOUS_TITLE = "the bank, as it was called before";

	@Test
	@DisplayName("an entry without history or protected properties writes neither element, even when both were set empty")
	void anEntryWithNeither_writesNoElementFor_either()
	{
		MysticCryptEntryModelBean untouched = anEntry(CURRENT_TITLE);
		MysticCryptEntryModelBean setEmpty = anEntry("another one");
		setEmpty.setHistory(new ArrayList<>());
		setEmpty.setProtectedPropertyKeys(new HashSet<>());

		String xml = new String(VaultXmlCodec.toXml(aModelWith(untouched, setEmpty)));

		assertFalse(xml.contains("<history"),
			"8.5 refuses a vault for an empty <history/> just as for a full one: " + xml);
		assertFalse(xml.contains("<protectedPropertyKeys"),
			"and the same for the protected keys: " + xml);
	}

	@Test
	@DisplayName("every version in the history comes back with its own content")
	void theHistory_comesBack_versionByVersion()
	{
		char[] previousPassword = TestPasswords.throwaway().toCharArray();
		MysticCryptEntryModelBean previous = anEntry(PREVIOUS_TITLE);
		previous.setPassword(previousPassword.clone());
		MysticCryptEntryModelBean entry = anEntry(CURRENT_TITLE);
		entry.setHistory(new ArrayList<>(List.of(previous)));

		MysticCryptEntryModelBean readBack = onlyEntryOf(
			VaultXmlCodec.toModel(VaultXmlCodec.toXml(aModelWith(entry))));

		assertEquals(1, readBack.getHistory().size(), "the one previous version is there");
		assertArrayEquals(PREVIOUS_TITLE.toCharArray(), readBack.getHistory().get(0).getTitle(),
			"with the title it had then, not the current one");
		assertArrayEquals(previousPassword, readBack.getHistory().get(0).getPassword(),
			"and the password it had then - which is what a history is kept for");
	}

	@Test
	@DisplayName("the names of the protected properties come back")
	void theProtectedKeys_comeBack()
	{
		MysticCryptEntryModelBean entry = anEntry(CURRENT_TITLE);
		Set<String> protectedKeys = new LinkedHashSet<>(List.of("TOTP seed", "recovery code"));
		entry.setProtectedPropertyKeys(protectedKeys);

		MysticCryptEntryModelBean readBack = onlyEntryOf(
			VaultXmlCodec.toModel(VaultXmlCodec.toXml(aModelWith(entry))));

		assertEquals(protectedKeys, readBack.getProtectedPropertyKeys(),
			"a property the user marked protected stays marked (#389)");
	}

	/**
	 * Whatever collection a caller hands in, the vault gets a standard one. {@code Set.of} and
	 * {@code List.of} are JDK-internal classes that XStream can only write by reflecting into
	 * {@code java.util}: without {@code --add-opens} the write fails, and with it the vault would
	 * carry {@code java.util.ImmutableCollections$Set12} as a type name (#408)
	 */
	@Test
	@DisplayName("an immutable history or set of protected names is written as a standard collection")
	void immutableCollections_areWrittenAsStandardOnes()
	{
		MysticCryptEntryModelBean entry = anEntry(CURRENT_TITLE);
		entry.setHistory(List.of(anEntry(PREVIOUS_TITLE)));
		entry.setProtectedPropertyKeys(Set.of("TOTP seed"));

		String xml = new String(VaultXmlCodec.toXml(aModelWith(entry)));

		assertFalse(xml.contains("ImmutableCollections"),
			"a JDK-internal collection class is not part of the vault format: " + xml);
		MysticCryptEntryModelBean readBack = onlyEntryOf(VaultXmlCodec.toModel(xml.toCharArray()));
		assertEquals(Set.of("TOTP seed"), readBack.getProtectedPropertyKeys());
		assertArrayEquals(PREVIOUS_TITLE.toCharArray(), readBack.getHistory().get(0).getTitle());
	}

	@Test
	@DisplayName("a vault that never had either field reads both as null, and the entry accepts that")
	void aVaultWithoutTheFields_readsThemAsNull()
	{
		MysticCryptEntryModelBean readBack = onlyEntryOf(
			VaultXmlCodec.toModel(VaultXmlCodec.toXml(aModelWith(anEntry(CURRENT_TITLE)))));

		assertNull(readBack.getHistory(), "every vault written before 8.6 has no history element");
		assertNull(readBack.getProtectedPropertyKeys());
		assertArrayEquals(CURRENT_TITLE.toCharArray(), readBack.getTitle());
	}

	private static MysticCryptEntryModelBean anEntry(final String title)
	{
		return MysticCryptEntryModelBean.builder().title(title.toCharArray())
			.password(TestPasswords.throwaway().toCharArray()).build();
	}

	private static ApplicationModelBean aModelWith(final MysticCryptEntryModelBean... entries)
	{
		Map<Long, List<MysticCryptEntryModelBean>> dataOfNodes = new HashMap<>();
		dataOfNodes.put(1L, new ArrayList<>(List.of(entries)));
		return ApplicationModelBean.builder().dataOfNodes(dataOfNodes).build();
	}

	private static MysticCryptEntryModelBean onlyEntryOf(final ApplicationModelBean model)
	{
		return model.getDataOfNodes().get(1L).get(0);
	}
}
