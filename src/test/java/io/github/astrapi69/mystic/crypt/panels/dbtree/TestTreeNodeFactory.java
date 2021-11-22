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
package io.github.astrapi69.mystic.crypt.panels.dbtree;

import static io.github.astrapi69.swing.tree.TreeNodeFactory.initializeTreeNodeWithTreeElement;

import java.util.ArrayList;
import java.util.List;

import io.github.astrapi69.swing.panels.tree.JXTreeElement;
import io.github.astrapi69.swing.tree.TreeNodeFactory;
import io.github.astrapi69.tree.TreeElement;
import io.github.astrapi69.tree.TreeNode;

public class TestTreeNodeFactory
{

	public static TreeNode<JXTreeElement> initializeTestTreeNodeElement()
	{
		List<MysticCryptEntryModelBean> entries;
		TreeNode<JXTreeElement> firstChildTreeNode;
		TreeNode<JXTreeElement> firstGrandChildTreeNodeLeaf;
		TreeNode<JXTreeElement> secondGrandChildTreeNodeLeaf;
		JXTreeElement firstGrandGrandChild;
		TreeNode<JXTreeElement> firstGrandGrandChildTreeNode;
		TreeNode<JXTreeElement> parentTreeNode;
		TreeNode<JXTreeElement> secondChildTreeNode;
		List<TreeNode<JXTreeElement>> list;
		JXTreeElement parent;
		JXTreeElement firstChild;
		JXTreeElement firstGrandChild;
		JXTreeElement secondChild;
		JXTreeElement secondGrandChild;

		entries = new ArrayList<>();
		entries.add(MysticCryptEntryModelBean.builder().build());

		parent = JXTreeElement.builder().name("parent").parent(null).node(true).build()
			.setDefaultContent(entries);
		firstChild = JXTreeElement.builder().name("firstChild").parent(parent).node(true).build();
		firstGrandChild = JXTreeElement.builder().name("firstGrandChild").parent(firstChild)
			.node(true).build();
		firstGrandGrandChild = JXTreeElement.builder().name("firstGrandGrandChild")
			.parent(firstGrandChild).node(false).build();
		secondChild = JXTreeElement.builder().name("secondChild").parent(parent).node(true).build();
		secondGrandChild = JXTreeElement.builder().name("secondGrandChild").parent(firstChild)
			.node(false).build();
		parentTreeNode = initializeTreeNodeWithTreeElement(parent, null);

		firstChildTreeNode = initializeTreeNodeWithTreeElement(firstChild, parentTreeNode);

		secondChildTreeNode = initializeTreeNodeWithTreeElement(secondChild, parentTreeNode);

		firstGrandChildTreeNodeLeaf = initializeTreeNodeWithTreeElement(firstGrandChild,
			firstChildTreeNode);
		secondGrandChildTreeNodeLeaf = initializeTreeNodeWithTreeElement(secondGrandChild,
			secondChildTreeNode);

		firstGrandGrandChildTreeNode = initializeTreeNodeWithTreeElement(firstGrandGrandChild,
			firstChildTreeNode);
		return parentTreeNode;
	}



	public static TreeNode<JXTreeElement> initializeTestJXTreeNodeElement()
	{
		List<MysticCryptEntryModelBean> entries;
		TreeNode<JXTreeElement> firstChildTreeNode;
		TreeNode<JXTreeElement> firstGrandChildTreeNodeLeaf;
		TreeNode<JXTreeElement> secondGrandChildTreeNodeLeaf;
		JXTreeElement firstGrandGrandChild;
		TreeNode<JXTreeElement> firstGrandGrandChildTreeNode;
		TreeNode<JXTreeElement> parentTreeNode;
		TreeNode<JXTreeElement> secondChildTreeNode;
		List<TreeNode<JXTreeElement>> list;
		JXTreeElement parent;
		JXTreeElement firstChild;
		JXTreeElement firstGrandChild;
		JXTreeElement secondChild;
		JXTreeElement secondGrandChild;
		// this will be resolved from the mcdb file
		entries = new ArrayList<>();
		entries.add(MysticCryptEntryModelBean.builder()
				.title("foo")
				.userName("bar")
				.password("secret")
				.url("foo.com")
			.build());

		parent = JXTreeElement.builder().name("parent")
			.iconPath("io/github/astrapi69/silk/icons/disk.png")
			.withText(true)
			.parent(null).node(true).build().setDefaultContent(entries);
		firstChild = JXTreeElement.builder().name("firstChild/search").parent(parent)
			.iconPath("io/github/astrapi69/silk/icons/magnifier.png")
			.withText(true)
			.node(true).build().setDefaultContent(entries);
		firstGrandChild = JXTreeElement.builder().name("firstGrandChild")
			.iconPath("io/github/astrapi69/silk/icons/lock.png")
			.withText(false).parent(firstChild)
			.node(true).build().setDefaultContent(entries);
		firstGrandGrandChild = JXTreeElement.builder().name("firstGrandGrandChild")
			.parent(firstGrandChild).node(false).build().setDefaultContent(entries);
		secondChild = JXTreeElement.builder().name("secondChild").parent(parent).node(true).build().setDefaultContent(entries);
		secondGrandChild = JXTreeElement.builder().name("secondGrandChild").parent(firstChild)
			.node(false).build().setDefaultContent(entries);
		parentTreeNode = TreeNodeFactory.initializeTreeNodeWithTreeElement(parent, null);

		firstChildTreeNode = TreeNodeFactory.initializeTreeNodeWithTreeElement(firstChild, parentTreeNode);

		secondChildTreeNode = TreeNodeFactory.initializeTreeNodeWithTreeElement(secondChild, parentTreeNode);

		firstGrandChildTreeNodeLeaf = TreeNodeFactory.initializeTreeNodeWithTreeElement(firstGrandChild,
			firstChildTreeNode);
		secondGrandChildTreeNodeLeaf = TreeNodeFactory.initializeTreeNodeWithTreeElement(secondGrandChild,
			secondChildTreeNode);

		firstGrandGrandChildTreeNode = TreeNodeFactory.initializeTreeNodeWithTreeElement(firstGrandGrandChild,
			firstChildTreeNode);
		return parentTreeNode;
	}

}
