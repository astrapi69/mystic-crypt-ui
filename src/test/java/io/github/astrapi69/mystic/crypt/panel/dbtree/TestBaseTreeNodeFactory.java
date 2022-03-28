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

import java.util.List;

import io.github.astrapi69.swing.tree.BaseTreeNodeFactory;
import io.github.astrapi69.swing.tree.GenericTreeElement;
import io.github.astrapi69.swing.tree.JXTreeElement;
import io.github.astrapi69.test.instance.TestPermissionFactory;
import io.github.astrapi69.test.object.Permission;
import io.github.astrapi69.tree.BaseTreeNode;
import io.github.astrapi69.tree.element.TreeElement;

public class TestBaseTreeNodeFactory
{

	public static BaseTreeNode<TreeElement, Long> initializeTestTreeNodeElement()
	{
		BaseTreeNode<TreeElement, Long> firstChildTreeNode;
		BaseTreeNode<TreeElement, Long> firstGrandChildTreeNodeLeaf;
		BaseTreeNode<TreeElement, Long> secondGrandChildTreeNodeLeaf;
		TreeElement firstGrandGrandChild;
		BaseTreeNode<TreeElement, Long> firstGrandGrandChildTreeNode;
		BaseTreeNode<TreeElement, Long> parentTreeNode;
		BaseTreeNode<TreeElement, Long> secondChildTreeNode;
		List<BaseTreeNode<TreeElement, Long>> list;
		TreeElement parent;
		TreeElement firstChild;
		TreeElement firstGrandChild;
		TreeElement secondChild;
		TreeElement secondGrandChild;
		parent = TreeElement.builder().name("parent").parent(null).node(true).build();
		firstChild = TreeElement.builder().name("firstChild").parent(parent).node(true).build();
		firstGrandChild = TreeElement.builder().name("firstGrandChild").parent(firstChild)
			.node(true).build();
		firstGrandGrandChild = TreeElement.builder().name("firstGrandGrandChild")
			.parent(firstGrandChild).node(false).build();
		secondChild = TreeElement.builder().name("secondChild").parent(parent).node(true).build();
		secondGrandChild = TreeElement.builder().name("secondGrandChild").parent(firstChild)
			.node(false).build();
		parentTreeNode = BaseTreeNodeFactory.initializeTreeNodeWithTreeElement(parent, null);

		firstChildTreeNode = BaseTreeNodeFactory.initializeTreeNodeWithTreeElement(firstChild,
			parentTreeNode);

		secondChildTreeNode = BaseTreeNodeFactory.initializeTreeNodeWithTreeElement(secondChild,
			parentTreeNode);

		firstGrandChildTreeNodeLeaf = BaseTreeNodeFactory
			.initializeTreeNodeWithTreeElement(firstGrandChild, firstChildTreeNode);
		secondGrandChildTreeNodeLeaf = BaseTreeNodeFactory
			.initializeTreeNodeWithTreeElement(secondGrandChild, secondChildTreeNode);

		firstGrandGrandChildTreeNode = BaseTreeNodeFactory
			.initializeTreeNodeWithTreeElement(firstGrandGrandChild, firstChildTreeNode);
		return parentTreeNode;
	}


	public static BaseTreeNode<JXTreeElement, Long> initializeTestJXTreeNodeElement()
	{
		// 1. Create a list with data.
		final List<Permission> permissions;
		List<Permission> permissions1;
		BaseTreeNode<JXTreeElement, Long> firstChildTreeNode;
		BaseTreeNode<JXTreeElement, Long> firstGrandChildTreeNodeLeaf;
		BaseTreeNode<JXTreeElement, Long> secondGrandChildTreeNodeLeaf;
		JXTreeElement firstGrandGrandChild;
		BaseTreeNode<JXTreeElement, Long> firstGrandGrandChildTreeNode;
		BaseTreeNode<JXTreeElement, Long> parentTreeNode;
		BaseTreeNode<JXTreeElement, Long> secondChildTreeNode;
		List<BaseTreeNode<JXTreeElement, Long>> list;
		JXTreeElement parent;
		JXTreeElement firstChild;
		JXTreeElement firstGrandChild;
		JXTreeElement secondChild;
		JXTreeElement secondGrandChild;

		permissions = TestPermissionFactory.getPermissionsInGerman();
		permissions1 = TestPermissionFactory.getPermissions();

		parent = JXTreeElement.builder().name("parent")
			.iconPath("io/github/astrapi69/silk/icons/disk.png").withText(true).parent(null)
			.node(true).build().setDefaultContent(permissions1);
		firstChild = JXTreeElement.builder().name("firstChild/search").parent(parent)
			.iconPath("io/github/astrapi69/silk/icons/magnifier.png").withText(true).node(true)
			.build().setDefaultContent(permissions);
		firstGrandChild = JXTreeElement.builder().name("firstGrandChild")
			.iconPath("io/github/astrapi69/silk/icons/lock.png").withText(false).parent(firstChild)
			.node(true).build().setDefaultContent(permissions);
		firstGrandGrandChild = JXTreeElement.builder().name("firstGrandGrandChild")
			.parent(firstGrandChild).node(false).build().setDefaultContent(permissions);
		secondChild = JXTreeElement.builder().name("secondChild").parent(parent).node(true).build()
			.setDefaultContent(permissions);
		secondGrandChild = JXTreeElement.builder().name("secondGrandChild").parent(firstChild)
			.node(false).build().setDefaultContent(permissions);
		parentTreeNode = BaseTreeNodeFactory.initializeTreeNodeWithTreeElement(parent, null);

		firstChildTreeNode = BaseTreeNodeFactory.initializeTreeNodeWithTreeElement(firstChild,
			parentTreeNode);

		secondChildTreeNode = BaseTreeNodeFactory.initializeTreeNodeWithTreeElement(secondChild,
			parentTreeNode);

		firstGrandChildTreeNodeLeaf = BaseTreeNodeFactory
			.initializeTreeNodeWithTreeElement(firstGrandChild, firstChildTreeNode);
		secondGrandChildTreeNodeLeaf = BaseTreeNodeFactory
			.initializeTreeNodeWithTreeElement(secondGrandChild, secondChildTreeNode);

		firstGrandGrandChildTreeNode = BaseTreeNodeFactory
			.initializeTreeNodeWithTreeElement(firstGrandGrandChild, firstChildTreeNode);
		return parentTreeNode;
	}

	public static BaseTreeNode<GenericTreeElement<List<Permission>>, Long> initializeTestGenericTreeNodeElement()
	{
		return initializeTestGenericTreeNodeElement(TestPermissionFactory.getPermissionsInGerman(),
			TestPermissionFactory.getPermissions());
	}

	public static <T, K> BaseTreeNode<GenericTreeElement<List<T>>, K> initializeTestGenericTreeNodeElement(
		List<T> first, List<T> second)
	{
		BaseTreeNode<GenericTreeElement<List<T>>, K> parentTreeNode;
		BaseTreeNode<GenericTreeElement<List<T>>, K> firstChildTreeNode;
		BaseTreeNode<GenericTreeElement<List<T>>, K> firstGrandChildTreeNodeLeaf;
		BaseTreeNode<GenericTreeElement<List<T>>, K> secondGrandChildTreeNodeLeaf;
		GenericTreeElement<List<T>> firstGrandGrandChild;
		BaseTreeNode<GenericTreeElement<List<T>>, K> firstGrandGrandChildTreeNode;
		BaseTreeNode<GenericTreeElement<List<T>>, K> secondChildTreeNode;
		List<BaseTreeNode<GenericTreeElement<List<T>>, K>> list;
		GenericTreeElement<List<T>> parent;
		GenericTreeElement<List<T>> firstChild;
		GenericTreeElement<List<T>> firstGrandChild;
		GenericTreeElement<List<T>> secondChild;
		GenericTreeElement<List<T>> secondGrandChild;

		parent = GenericTreeElement.<List<T>> builder().name("parent")
			.iconPath("io/github/astrapi69/silk/icons/disk.png").withText(true).parent(null)
			.node(true).build().setDefaultContent(first);
		firstChild = GenericTreeElement.<List<T>> builder().name("firstChild/search").parent(parent)
			.iconPath("io/github/astrapi69/silk/icons/magnifier.png").withText(true).node(true)
			.build().setDefaultContent(second);
		firstGrandChild = GenericTreeElement.<List<T>> builder().name("firstGrandChild")
			.iconPath("io/github/astrapi69/silk/icons/lock.png").withText(false).parent(firstChild)
			.node(true).build().setDefaultContent(second);
		firstGrandGrandChild = GenericTreeElement.<List<T>> builder().name("firstGrandGrandChild")
			.parent(firstGrandChild).node(false).build().setDefaultContent(second);
		secondChild = GenericTreeElement.<List<T>> builder().name("secondChild").parent(parent)
			.node(true).build().setDefaultContent(second);
		secondGrandChild = GenericTreeElement.<List<T>> builder().name("secondGrandChild")
			.parent(firstChild).node(false).build().setDefaultContent(second);
		parentTreeNode = BaseTreeNodeFactory.initializeTreeNodeWithTreeElement(parent, null);

		firstChildTreeNode = BaseTreeNodeFactory.initializeTreeNodeWithTreeElement(firstChild,
			parentTreeNode);

		secondChildTreeNode = BaseTreeNodeFactory.initializeTreeNodeWithTreeElement(secondChild,
			parentTreeNode);

		firstGrandChildTreeNodeLeaf = BaseTreeNodeFactory
			.initializeTreeNodeWithTreeElement(firstGrandChild, firstChildTreeNode);
		secondGrandChildTreeNodeLeaf = BaseTreeNodeFactory
			.initializeTreeNodeWithTreeElement(secondGrandChild, secondChildTreeNode);

		firstGrandGrandChildTreeNode = BaseTreeNodeFactory
			.initializeTreeNodeWithTreeElement(firstGrandGrandChild, firstChildTreeNode);
		return parentTreeNode;
	}
}
