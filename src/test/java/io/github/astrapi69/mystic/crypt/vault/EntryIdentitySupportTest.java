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
package io.github.astrapi69.mystic.crypt.vault;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * Identifiers assigned on load (#272), and a modification time that only an actual edit sets
 * (#273).
 */
class EntryIdentitySupportTest
{

	@Test
	@DisplayName("an entry without an identifier gets one, and one that has it keeps it")
	void assignMissingIdentifiers_fillsOnlyTheEmptyOnes()
	{
		UUID existing = UUID.randomUUID();
		MysticCryptEntryModelBean withIdentity = MysticCryptEntryModelBean.builder().id(existing)
			.title("imported from KeePass".toCharArray()).build();
		MysticCryptEntryModelBean withoutIdentity = MysticCryptEntryModelBean.builder()
			.title("written before identifiers were maintained".toCharArray()).build();
		ApplicationModelBean applicationModelBean = ApplicationModelBean.builder()
			.dataOfNodes(entriesByNodeId(withIdentity, withoutIdentity)).build();

		int assigned = EntryIdentitySupport.assignMissingIdentifiers(applicationModelBean);

		assertEquals(1, assigned,
			"the count is what tells the caller to mark the model as changed, so an identifier "
				+ "assigned here reaches the file with the next save");
		assertEquals(existing, withIdentity.getId(),
			"an identifier once assigned never changes and is never reused");
		assertNotNull(withoutIdentity.getId(),
			"and the oldest data - the data most worth referencing - is exactly what filling this "
				+ "only for new entries would have left without identity forever");
	}

	@Test
	@DisplayName("the entries hanging in the tree are reached too")
	void assignMissingIdentifiers_reachesTheTree()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder()
			.title("in the tree".toCharArray()).build();
		ApplicationModelBean applicationModelBean = ApplicationModelBean.builder()
			.rootTreeAsMap(treeHolding(entry)).build();

		assertEquals(1, EntryIdentitySupport.assignMissingIdentifiers(applicationModelBean));
		assertNotNull(entry.getId(),
			"the tree is the second place an entry lives in this model, and an entry the loop "
				+ "forgets keeps no identity at all");
	}

	@Test
	@DisplayName("assigning twice hands out the same identifiers, not new ones")
	void assignMissingIdentifiers_isIdempotent()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder()
			.title("an entry".toCharArray()).build();
		ApplicationModelBean applicationModelBean = ApplicationModelBean.builder()
			.dataOfNodes(entriesByNodeId(entry)).build();

		EntryIdentitySupport.assignMissingIdentifiers(applicationModelBean);
		UUID afterTheFirstPass = entry.getId();
		int assignedOnTheSecondPass = EntryIdentitySupport
			.assignMissingIdentifiers(applicationModelBean);

		assertEquals(0, assignedOnTheSecondPass,
			"nothing was missing the second time, so nothing marks the model as changed either");
		assertEquals(afterTheFirstPass, entry.getId(),
			"an identifier that is regenerated per session is not an identifier");
	}

	@Test
	@DisplayName("a null among the entries does not stop the assignment")
	void assignMissingIdentifiers_carriesOn_whenTheModelHoldsANull()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder()
			.title("a real one".toCharArray()).build();
		Map<Long, List<MysticCryptEntryModelBean>> entriesByNodeId = new LinkedHashMap<>();
		entriesByNodeId.put(1L, Arrays.asList(null, entry));
		entriesByNodeId.put(2L, null);
		Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> treeWithHoles = new LinkedHashMap<>();
		treeWithHoles.put(1L, null);
		ApplicationModelBean applicationModelBean = ApplicationModelBean.builder()
			.dataOfNodes(entriesByNodeId).rootTreeAsMap(treeWithHoles).build();

		assertEquals(1, EntryIdentitySupport.assignMissingIdentifiers(applicationModelBean));
		assertNotNull(entry.getId(),
			"a null in the list comes out of a file this application did not write itself, and a "
				+ "pass that stops at the first one leaves every later entry without identity. The "
				+ "tree carries a null node here too - the same file can have holes in either "
				+ "place");
	}

	@Test
	@DisplayName("a duplicate gets its own identifier, it does not inherit one")
	void assignIdentifierIfMissing_givesADuplicateItsOwnIdentity()
	{
		MysticCryptEntryModelBean original = MysticCryptEntryModelBean.builder()
			.title("original".toCharArray()).build();
		EntryIdentitySupport.assignIdentifierIfMissing(original);
		MysticCryptEntryModelBean duplicate = original.toBuilder().id(null)
			.title("original-Copy".toCharArray()).build();

		EntryIdentitySupport.assignIdentifierIfMissing(duplicate);

		assertNotNull(duplicate.getId());
		assertNotEquals(original.getId(), duplicate.getId(),
			"toBuilder copies the identifier, so a duplicate would carry the original's identity - "
				+ "which is exactly the reuse an identifier must never do");
	}

	@Test
	@DisplayName("an empty or absent model is not an error")
	void assignMissingIdentifiers_handlesAnEmptyModel()
	{
		assertEquals(0, EntryIdentitySupport.assignMissingIdentifiers(null));
		assertDoesNotThrow(() -> EntryIdentitySupport.assignIdentifierIfMissing(null));
		assertEquals(0,
			EntryIdentitySupport.assignMissingIdentifiers(ApplicationModelBean.builder().build()));
		assertDoesNotThrow(() -> EntryIdentitySupport.markAsModified(null));
	}

	@Test
	@DisplayName("only an actual edit sets the modification time")
	void markAsModified_setsTheTimestamp_onlyWhenCalled()
	{
		MysticCryptEntryModelBean untouched = MysticCryptEntryModelBean.builder()
			.title("untouched".toCharArray()).build();
		MysticCryptEntryModelBean edited = MysticCryptEntryModelBean.builder()
			.title("edited".toCharArray()).build();
		OffsetDateTime beforeTheEdit = OffsetDateTime.now();

		EntryIdentitySupport.markAsModified(edited);

		assertNull(untouched.getLastModificationTime(),
			"a missing modification time cannot be reconstructed. Setting it on load or on first "
				+ "save would invent a fact that reads later as measured, and a stale date reads "
				+ "as a real one");
		assertNotNull(edited.getLastModificationTime());
		assertTrue(!edited.getLastModificationTime().isBefore(beforeTheEdit),
			"and it is the moment of the edit, not something carried over");
	}

	private static Map<Long, List<MysticCryptEntryModelBean>> entriesByNodeId(
		final MysticCryptEntryModelBean... entries)
	{
		Map<Long, List<MysticCryptEntryModelBean>> entriesByNodeId = new LinkedHashMap<>();
		entriesByNodeId.put(1L, List.of(entries));
		return entriesByNodeId;
	}

	private static Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> treeHolding(
		final MysticCryptEntryModelBean entry)
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> element = GenericTreeElement
			.<List<MysticCryptEntryModelBean>> builder().name("a node").build();
		element.setDefaultContent(List.of(entry));
		TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node = TreeIdNode
			.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder().id(1L)
			.value(element).build();
		Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> treeAsMap = new LinkedHashMap<>();
		treeAsMap.put(1L, node);
		return treeAsMap;
	}
}
