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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * Headless tests of the rules behind moving a node: where a node sits among its siblings, when a
 * move would change nothing, and which nodes it may be moved under.
 * <p>
 * The root is the case that matters most. It is not shown at all, there can only be one of it, and
 * nothing may end up next to it - so it must never be offered as a new parent to a node that
 * already hangs under it, and the first node of the top level must have nothing to move up past.
 */
class SecretKeyTreeMoveTest
{

	private static BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> newNode(
		long id, String name, boolean leaf,
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> parent)
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node = BaseTreeNode
			.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder().id(id)
			.value(GenericTreeElement.<List<MysticCryptEntryModelBean>> builder().name(name)
				.leaf(leaf).build())
			.displayValue(name).leaf(leaf).parent(parent).build();
		if (parent != null)
		{
			parent.addChild(node);
		}
		return node;
	}

	/** root -> [first, second -> [child], leaf] */
	private BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root;
	private BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> first;
	private BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> second;
	private BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> child;
	private BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> leaf;

	private void buildTree()
	{
		root = newNode(0, "root", false, null);
		first = newNode(1, "first", false, root);
		second = newNode(2, "second", false, root);
		child = newNode(3, "child", false, second);
		leaf = newNode(4, "leaf", true, root);
	}

	@Test
	void knowsWhereANodeSitsAmongItsSiblings()
	{
		buildTree();

		assertEquals(0, SecretKeyTreeWithContentPanel.positionOf(first));
		assertEquals(1, SecretKeyTreeWithContentPanel.positionOf(second));
		assertEquals(2, SecretKeyTreeWithContentPanel.positionOf(leaf));
		assertEquals(-1, SecretKeyTreeWithContentPanel.positionOf(root),
			"the root has no siblings, so it has no position among any");
	}

	@ParameterizedTest
	@CsvSource({ "first,-1,false", "first,1,true", "second,-1,true", "second,1,true",
			"leaf,-1,true", "leaf,1,false", "root,-1,false", "root,1,false" })
	void aMoveIsOnlyOfferedWhereItWouldChangeSomething(String nodeName, int offset,
		boolean expected)
	{
		buildTree();
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node = switch (nodeName)
		{
			case "first" -> first;
			case "second" -> second;
			case "leaf" -> leaf;
			default -> root;
		};

		assertEquals(expected, SecretKeyTreeWithContentPanel.canMove(node, offset));
	}

	@Test
	void theFirstNodeOfTheTopLevelCannotBeMovedNextToTheRoot()
	{
		buildTree();

		assertFalse(SecretKeyTreeWithContentPanel.canMove(first, -1),
			"a node moved past the first place of the top level would sit next to the root, "
				+ "which would be a second root");
	}

	@Test
	void aNodeIsNeverOfferedItsOwnParentAsANewParent()
	{
		buildTree();

		List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> targets = SecretKeyTreeWithContentPanel
			.possibleMoveTargets(first);

		assertFalse(targets.contains(root),
			"the root is where this node already hangs; offering it would move nothing");
		assertTrue(targets.contains(second), "a sibling that can hold children is a target");
		assertTrue(targets.contains(child), "a node deeper in another branch is a target");
		assertFalse(targets.contains(leaf), "a leaf cannot hold children");
		assertFalse(targets.contains(first), "a node cannot be moved under itself");
	}

	@Test
	void aNodeIsNeverOfferedOneOfItsOwnChildren()
	{
		buildTree();

		List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> targets = SecretKeyTreeWithContentPanel
			.possibleMoveTargets(second);

		assertFalse(targets.contains(child),
			"moving a node under its own child would cut that branch out of the tree");
		assertTrue(targets.contains(first), "a sibling stays a target");
		assertFalse(targets.contains(root), "its own parent is no target");
	}

	@Test
	void aNodeDeeperInTheTreeMayBeMovedUpToTheTopLevel()
	{
		buildTree();

		assertTrue(SecretKeyTreeWithContentPanel.possibleMoveTargets(child).contains(root),
			"the root is a valid new parent for a node that does not already hang under it - "
				+ "that is how a node reaches the top level");
	}

	@Test
	void theSiblingsOfTheRootAreEmpty()
	{
		buildTree();

		assertTrue(SecretKeyTreeWithContentPanel.siblingsOf(root).isEmpty());
		assertEquals(3, SecretKeyTreeWithContentPanel.siblingsOf(first).size());
	}
}
