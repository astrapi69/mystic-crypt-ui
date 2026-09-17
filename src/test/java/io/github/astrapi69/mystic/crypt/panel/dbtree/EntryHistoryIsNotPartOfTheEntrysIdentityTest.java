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
package io.github.astrapi69.mystic.crypt.panel.dbtree;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * An entry's history is what it was, not what it is: two entries that differ only in their history
 * are the same entry, and an entry is never compared, hashed or printed through its previous
 * versions (#402).
 * <p>
 * Measured before the field existed, on a subclass carrying one: Lombok's generated {@code equals},
 * {@code hashCode} and {@code toString} walk into the history, so an entry that appears in its own
 * history ends in a {@code StackOverflowError}, and {@code toString} prints the old passwords.
 */
class EntryHistoryIsNotPartOfTheEntrysIdentityTest
{

	@Test
	@DisplayName("two entries that differ only in their history are equal and hash alike")
	void theHistory_doesNotTakePartIn_equalsOrHashCode()
	{
		MysticCryptEntryModelBean withoutHistory = MysticCryptEntryModelBean.builder()
			.title("the bank".toCharArray()).build();
		MysticCryptEntryModelBean withHistory = withoutHistory.toBuilder().build();
		withHistory.setHistory(new ArrayList<>(List.of(
			MysticCryptEntryModelBean.builder().title("the bank, before".toCharArray()).build())));

		assertEquals(withoutHistory, withHistory);
		assertEquals(withoutHistory.hashCode(), withHistory.hashCode());
	}

	/**
	 * Decided by the maintainer for step 5 of the KDBX work (#384): a duplicate carries no history,
	 * and its own copy of the protected property names. Before, "Duplicate entry" and "copy node"
	 * both built the copy with {@code toBuilder()}, which handed it the original's history list and
	 * protected-keys set by reference
	 */
	@Test
	@DisplayName("a duplicate carries no history, and shares no list and no set with its original")
	void aDuplicate_hasNoHistory_andSharesNothingMutable()
	{
		MysticCryptEntryModelBean original = MysticCryptEntryModelBean.builder()
			.title("the bank".toCharArray()).build();
		original.setHistory(new ArrayList<>(List.of(
			MysticCryptEntryModelBean.builder().title("the bank, before".toCharArray()).build())));
		original.setProtectedPropertyKeys(new java.util.LinkedHashSet<>(List.of("TOTP seed")));
		original.setProperty("TOTP seed", "JBSWY3DPEHPK3PXP");

		MysticCryptEntryModelBean duplicate = original.duplicate();

		assertEquals(null, duplicate.getHistory(),
			"no history - and null rather than an empty list, because an empty list is written as "
				+ "<history/>, which 8.5 refuses a whole vault for (#402)");
		assertEquals(original.getProtectedPropertyKeys(), duplicate.getProtectedPropertyKeys(),
			"the protected names come along");
		assertNotSame(original.getProtectedPropertyKeys(), duplicate.getProtectedPropertyKeys(),
			"as a copy of their own");
		assertNotSame(original.getProperties(), duplicate.getProperties(),
			"and no list is shared: not the properties");
		assertNotSame(original.getResources(), duplicate.getResources(), "nor the attachments");
		duplicate.getProtectedPropertyKeys().add("a key only the duplicate has");
		assertEquals(1, original.getProtectedPropertyKeys().size(),
			"a change to the duplicate's set does not reach the original");
	}

	@Test
	@DisplayName("an entry that appears in its own history can still be compared, hashed and printed")
	void anEntryInItsOwnHistory_doesNotRecurse()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder()
			.title("the bank".toCharArray()).build();
		entry.setHistory(new ArrayList<>(List.of(entry)));

		assertDoesNotThrow(entry::hashCode);
		assertDoesNotThrow(entry::toString);
		assertDoesNotThrow(() -> entry.equals(entry.toBuilder().build()));
	}
}
