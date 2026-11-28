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
package io.github.astrapi69.mystic.crypt.panel.dbtree;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * "Duplicate node" copies the node's subtree with every entry in it, and every copied entry is a
 * new entry, as the copy of "Duplicate entry" is: an identifier of its own, in the sub-nodes too,
 * so the identifiers in a vault that holds the original and the copy stay unique, and no history.
 * <p>
 * Until this was fixed the copies carried the originals' identifiers, so two different entries
 * shared one UUID in the vault and in a KDBX export, and an import that matches by UUID would have
 * merged them (#856 named it as an open finding).
 */
class NodeDuplicationTest
{

	/** root -> [group: first, second -> [sub-group: third]] */
	private BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root;
	private BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> group;
	private MysticCryptEntryModelBean first;
	private MysticCryptEntryModelBean second;
	private MysticCryptEntryModelBean third;

	private static BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> newNode(
		final long id, final String name,
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> parent,
		final MysticCryptEntryModelBean... entries)
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> element = GenericTreeElement
			.<List<MysticCryptEntryModelBean>> builder().name(name).build();
		element.setDefaultContent(new ArrayList<>(List.of(entries)));
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node = BaseTreeNode
			.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder().id(id)
			.value(element).displayValue(name).leaf(false).parent(parent).build();
		if (parent != null)
		{
			parent.addChild(node);
		}
		return node;
	}

	private static MysticCryptEntryModelBean newEntry(final String title)
	{
		return MysticCryptEntryModelBean.builder().id(UUID.randomUUID()).title(title.toCharArray())
			.build();
	}

	private void buildTree()
	{
		first = newEntry("first");
		second = newEntry("second");
		third = newEntry("third");
		root = newNode(0, "root", null);
		group = newNode(1, "group", root, first, second);
		newNode(2, "sub-group", group, third);
	}

	/**
	 * The copy hung next to the original, the way the panel does it after the name dialog: renamed
	 * first, since the children of a node are a set and a node equals another with the same id,
	 * value and display value, so an unrenamed copy would not be added at all
	 *
	 * @return the copy
	 */
	private BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> duplicateGroup()
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> copy = NodeDuplication
			.copyOf(group, null);
		copy.getValue().setName("group-Copy");
		copy.setDisplayValue("group-Copy");
		copy.setParent(root);
		root.addChild(copy);
		assertEquals(2, root.getChildren().size(), "the copy hangs next to the original");
		return copy;
	}

	private static List<MysticCryptEntryModelBean> entriesBelow(
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node)
	{
		List<MysticCryptEntryModelBean> entries = new ArrayList<>();
		if (node.getValue().getDefaultContent() != null)
		{
			entries.addAll(node.getValue().getDefaultContent());
		}
		if (node.getChildren() != null)
		{
			node.getChildren().forEach(child -> entries.addAll(entriesBelow(child)));
		}
		return entries;
	}

	private static List<UUID> identifiersOf(final List<MysticCryptEntryModelBean> entries)
	{
		return entries.stream().map(MysticCryptEntryModelBean::getId).toList();
	}

	@Test
	@DisplayName("every entry of the copy gets an identifier of its own, in the sub-nodes too")
	void copyOf_givesEveryEntryAnIdentifierOfItsOwn()
	{
		buildTree();
		List<UUID> originalIdentifiers = identifiersOf(List.of(first, second, third));

		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> copy = duplicateGroup();

		List<MysticCryptEntryModelBean> copiedEntries = entriesBelow(copy);
		assertEquals(3, copiedEntries.size(),
			"the copy holds the group's two entries and the sub-group's one");
		List<UUID> copiedIdentifiers = identifiersOf(copiedEntries);
		assertTrue(copiedIdentifiers.stream().allMatch(identifier -> identifier != null),
			"every copied entry has an identifier: " + copiedIdentifiers);
		assertTrue(Collections.disjoint(originalIdentifiers, copiedIdentifiers),
			"no copied entry carries the identifier of an original, the one in the sub-group "
				+ "neither: originals " + originalIdentifiers + ", copies " + copiedIdentifiers);
		assertEquals(originalIdentifiers, identifiersOf(List.of(first, second, third)),
			"and the originals keep theirs");
	}

	@Test
	@DisplayName("a vault holding the original and the copy holds every identifier once")
	void copyOf_leavesTheIdentifiersInTheVaultUnique()
	{
		buildTree();

		duplicateGroup();

		List<UUID> identifiersInTheVault = identifiersOf(entriesBelow(root));
		assertEquals(6, identifiersInTheVault.size(), "three entries, each with its copy");
		assertEquals(6, new HashSet<>(identifiersInTheVault).size(),
			"no two entries share an identifier: " + identifiersInTheVault);
	}

	@Test
	@DisplayName("the copy of an entry without an identifier gets one, and the original stays as it was")
	void copyOf_givesTheCopyOfAnEntryWithoutIdentifierOne()
	{
		buildTree();
		first.setId(null);

		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> copy = duplicateGroup();

		assertNotNull(copy.getValue().getDefaultContent().get(0).getId(),
			"a copy is a new entry, and a new entry gets its identifier when it is made, as the "
				+ "copy of \"Duplicate entry\" does");
		assertNull(first.getId(), "copying does not change the original");
	}

	@Test
	@DisplayName("the entries are copies of their own, with the same text and without the history")
	void copyOf_copiesTheEntriesWithoutTheirHistory()
	{
		buildTree();
		first.setHistory(new ArrayList<>(List.of(newEntry("first, before"))));

		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> copy = duplicateGroup();

		MysticCryptEntryModelBean copyOfFirst = copy.getValue().getDefaultContent().get(0);
		assertNotSame(first, copyOfFirst, "an entry of its own, not the original's object");
		assertArrayEquals(first.getTitle(), copyOfFirst.getTitle(), "with the original's text");
		assertNull(copyOfFirst.getHistory(),
			"a duplicate is a new entry, not a later version of the original (#384): it carries "
				+ "no history, as \"Duplicate entry\" carries none");
		assertNotNull(first.getHistory(), "and the original keeps its own");
	}
}
