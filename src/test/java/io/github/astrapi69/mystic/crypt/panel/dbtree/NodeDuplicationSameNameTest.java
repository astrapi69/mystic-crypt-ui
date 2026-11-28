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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.gen.tree.visitor.ReindexTreeNodeVisitor;
import io.github.astrapi69.id.generate.LongIdGenerator;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * A copy confirmed under the original's exact name, hung into the parent the way
 * SecretKeyTreeWithContentPanel.onDuplicateSelectedTreeNode does before it reindexes: the children
 * of a node are a set, and BaseTreeNode leaves only children and parent out of equals. Pin of
 * today: hung in before the reindex, an empty group copied under its own name is dropped from the
 * model.
 */
class NodeDuplicationSameNameTest
{

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

	private static int childrenAfterDuplicatingUnderTheSameName(
		final MysticCryptEntryModelBean... entries)
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = newNode(0,
			"root", null);
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> group = newNode(1,
			"group", root, entries);
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> copy = NodeDuplication
			.copyOf(group, null);
		copy.getValue().setName("group");
		copy.setDisplayValue("group");
		copy.setParent(root);
		root.addChild(copy);
		copy.accept(new ReindexTreeNodeVisitor<>(LongIdGenerator.of(10L)));
		return root.getChildren().size();
	}

	@Test
	@DisplayName("pin: a copy of an empty group confirmed under the original's name is dropped")
	void copyUnderTheSameName_ofAnEmptyGroup_isDropped()
	{
		assertEquals(1, childrenAfterDuplicatingUnderTheSameName());
	}

	@Test
	@DisplayName("a copy of a group with entries confirmed under the original's name is a second child")
	void copyUnderTheSameName_ofAGroupWithEntries_isASecondChild()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().id(UUID.randomUUID())
			.title("first".toCharArray()).build();
		assertEquals(2, childrenAfterDuplicatingUnderTheSameName(entry));
	}
}
