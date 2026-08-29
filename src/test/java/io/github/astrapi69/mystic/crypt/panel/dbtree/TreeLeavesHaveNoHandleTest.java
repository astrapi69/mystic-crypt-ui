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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.id.generate.LongIdGenerator;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * A row that cannot be opened must not offer to be opened.
 * <p>
 * The tree is built with {@code asksAllowsChildren}, so Swing decides leaf or not from
 * {@code DefaultMutableTreeNode.allowsChildren} and never looks at the leaf flag the application
 * maintains. The node factory sets that to true for every node it builds, so every row used to
 * carry an expand handle, including the ones with nothing behind them.
 */
class TreeLeavesHaveNoHandleTest
{

	private static SecretKeyTreeWithContentPanel newPanel()
	{
		List<MysticCryptEntryModelBean> entries = new ArrayList<>();
		entries.add(MysticCryptEntryModelBean.builder().userName("someone").build());
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = TestBaseTreeNodeFactory
			.initializeTestGenericTreeNodeElement(entries, entries, LongIdGenerator.of(0L));
		return new SecretKeyTreeWithContentPanel(BaseModel.of(root));
	}

	/** Walks the swing tree and hands back the node whose element carries the given name */
	private static DefaultMutableTreeNode swingNodeNamed(JTree tree, String name)
	{
		DefaultMutableTreeNode root = (DefaultMutableTreeNode)tree.getModel().getRoot();
		java.util.Enumeration<javax.swing.tree.TreeNode> nodes = root.preorderEnumeration();
		while (nodes.hasMoreElements())
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)nodes.nextElement();
			if (node.getUserObject() instanceof BaseTreeNode<?, ?> treeNode
				&& treeNode.getValue() instanceof GenericTreeElement<?> element
				&& name.equals(element.getName()))
			{
				return node;
			}
		}
		throw new IllegalStateException("no node named '" + name + "' in the tree");
	}

	/**
	 * The tree the application shows on open: what the model says about a leaf is what the tree
	 * answers about it
	 */
	@Test
	@DisplayName("a leaf is a leaf to the tree, so it gets no handle")
	void aLeafIsALeafToTheTree()
	{
		SecretKeyTreeWithContentPanel panel = newPanel();
		JTree tree = panel.getTree();
		TreeModel model = tree.getModel();

		DefaultMutableTreeNode leaf = swingNodeNamed(tree, "secondGrandChild");

		assertTrue(((BaseTreeNode<?, ?>)leaf.getUserObject()).isLeaf(),
			"this test needs a node the application considers a leaf");
		assertTrue(model.isLeaf(leaf),
			"the tree offers to expand a leaf, which has nothing behind it");
	}

	/**
	 * And the other way round: a node that holds children must not be treated as a leaf, or the
	 * children are not shown at all
	 */
	@Test
	@DisplayName("a node that holds children is not a leaf to the tree")
	void aNodeWithChildrenIsNotALeaf()
	{
		SecretKeyTreeWithContentPanel panel = newPanel();
		JTree tree = panel.getTree();

		DefaultMutableTreeNode node = swingNodeNamed(tree, "firstChild/search");

		assertFalse(((BaseTreeNode<?, ?>)node.getUserObject()).isLeaf(),
			"this test needs a node the application considers a folder");
		assertFalse(tree.getModel().isLeaf(node),
			"a folder is treated as a leaf, so its children are never shown");
		assertTrue(node.getChildCount() > 0, "the folder in this fixture has children");
	}
}
