/**
 * The MIT License
 *
 * Copyright (C) 2021 Asterios Raptis
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
package io.github.astrapi69.mystic.crypt.app.factory;

import javax.swing.tree.DefaultMutableTreeNode;

import io.github.astrapi69.data.identifiable.IdGenerator;
import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;
import io.github.astrapi69.swing.renderer.tree.JTreeElement;
import io.github.astrapi69.swing.tree.factory.DefaultMutableTreeNodeFactory;
import io.github.astrapi69.swing.tree.model.TreeElement;
import lombok.NonNull;

/**
 * Factory class for generate {@link DefaultMutableTreeNode} from {@link BaseTreeNode}
 */
public class ApplicationBaseTreeNodeFactory
{

	/**
	 * Creates a new {@link DefaultMutableTreeNode} object from the given {@link BaseTreeNode}
	 * object
	 *
	 * @param <T>
	 *            the generic type of the given {@link BaseTreeNode} object
	 * @param <K>
	 *            the generic type of the id of the given {@link BaseTreeNode} object
	 * @param treeNode
	 *            the {@link BaseTreeNode} object
	 * @return the new {@link DefaultMutableTreeNode} object generated from the given
	 *         {@link BaseTreeNode} object
	 */
	public static <T, K> DefaultMutableTreeNode newDefaultMutableTreeNode(
		@NonNull BaseTreeNode<T, K> treeNode)
	{
		BaseTreeNode<T, K> rootNode = treeNode;
		if (!treeNode.isRoot())
		{
			rootNode = treeNode.getRoot();
		}
		return traverseAndAdd(null, rootNode);
	}

	/**
	 * Traverses through the given {@link BaseTreeNode} object and return the root
	 * {@link DefaultMutableTreeNode} object
	 *
	 * @param <T>
	 *            the generic type of the given {@link BaseTreeNode} object
	 * @param <K>
	 *            the generic type of the id of the given {@link BaseTreeNode} object
	 * @param rootDefaultMutableTreeNode
	 *            the {@link DefaultMutableTreeNode} object
	 * @param treeNode
	 *            the {@link BaseTreeNode} object
	 * @return the root {@link DefaultMutableTreeNode} object
	 */
	public static <T, K> DefaultMutableTreeNode traverseAndAdd(
		DefaultMutableTreeNode rootDefaultMutableTreeNode, @NonNull BaseTreeNode<T, K> treeNode)
	{
		DefaultMutableTreeNode parent = rootDefaultMutableTreeNode;
		if (rootDefaultMutableTreeNode == null)
		{
			parent = DefaultMutableTreeNodeFactory.newDefaultMutableTreeNode(null, treeNode);
		}
		for (final BaseTreeNode<T, K> data : treeNode.getChildren())
		{
			DefaultMutableTreeNode node = DefaultMutableTreeNodeFactory
				.newDefaultMutableTreeNode(data);
			parent.add(node);
			traverseAndAdd(node, data);
		}
		return parent;
	}

	/**
	 * Creates a new {@link DefaultMutableTreeNode} object from the given {@link BaseTreeNode}
	 * object
	 *
	 * @param <T>
	 *            the generic type of the given {@link BaseTreeNode} object
	 * @param <K>
	 *            the generic type of the id of the given {@link BaseTreeNode} object
	 * @param treeNode
	 *            the {@link BaseTreeNode} object
	 * @param parent
	 *            the parent {@link DefaultMutableTreeNode} object
	 * @param onlyRoot
	 *            the flag if only root tree node will be chosen, otherwise the given tree node will
	 *            be traversed and added
	 * @return the new {@link DefaultMutableTreeNode} object generated from the given
	 *         {@link BaseTreeNode} object
	 */
	public static <T, K> DefaultMutableTreeNode newDefaultMutableTreeNode(
		@NonNull BaseTreeNode<T, K> treeNode, DefaultMutableTreeNode parent, boolean onlyRoot)
	{
		BaseTreeNode<T, K> rootNode = treeNode;
		if (onlyRoot && !treeNode.isRoot())
		{
			rootNode = treeNode.getRoot();
		}
		return traverseAndAdd(parent, rootNode, true);
	}

	/**
	 * Traverses through the given {@link BaseTreeNode} object and return the root
	 * {@link DefaultMutableTreeNode} object
	 *
	 * @param <T>
	 *            the generic type of the given {@link BaseTreeNode} object
	 * @param <K>
	 *            the generic type of the id of the given {@link BaseTreeNode} object
	 * @param rootDefaultMutableTreeNode
	 *            the {@link DefaultMutableTreeNode} object
	 * @param treeNode
	 *            the {@link BaseTreeNode} object
	 * @param root
	 *            the flag if the given root tree node will be used for creation of the parent
	 *            object
	 * @return the root {@link DefaultMutableTreeNode} object
	 */
	public static <T, K> DefaultMutableTreeNode traverseAndAdd(
		DefaultMutableTreeNode rootDefaultMutableTreeNode, @NonNull BaseTreeNode<T, K> treeNode,
		boolean root)
	{
		DefaultMutableTreeNode parent = rootDefaultMutableTreeNode;
		if (rootDefaultMutableTreeNode == null)
		{
			parent = DefaultMutableTreeNodeFactory.newDefaultMutableTreeNode(null, treeNode);
		}
		else
		{
			if (root)
			{
				parent = DefaultMutableTreeNodeFactory
					.newDefaultMutableTreeNode(rootDefaultMutableTreeNode, treeNode);
			}
		}
		for (final BaseTreeNode<T, K> data : treeNode.getChildren())
		{
			DefaultMutableTreeNode node = DefaultMutableTreeNodeFactory
				.newDefaultMutableTreeNode(data);
			parent.add(node);
			traverseAndAdd(node, data, false);
		}
		return parent;
	}

	/**
	 * Factory method that creates a new {@link BaseTreeNode} object from the given
	 * {@link TreeElement} object
	 *
	 * @param <K>
	 *            the generic type of the id of the given {@link BaseTreeNode} object
	 * @param treeElement
	 *            the {@link TreeElement} object
	 * @param parentTreeNode
	 *            the parent object
	 * @param idGenerator
	 *            the id generator
	 * @return the new {@link BaseTreeNode} object
	 */
	public static <K> BaseTreeNode<TreeElement, K> initializeTreeNodeWithTreeElement(
		final TreeElement treeElement, BaseTreeNode<TreeElement, K> parentTreeNode,
		final @NonNull IdGenerator<K> idGenerator)
	{
		BaseTreeNode<TreeElement, K> treeNode = initializeBaseTreeNodeWithTreeElement(treeElement,
			parentTreeNode, idGenerator);
		treeNode.setLeaf(!treeElement.isNode());
		treeNode.setDisplayValue(treeElement.getName());
		return treeNode;
	}

	/**
	 * Factory method that creates a new {@link BaseTreeNode} object from the given
	 * {@link TreeElement} object
	 *
	 * @param <T>
	 *            the generic type of the given {@link BaseTreeNode} object
	 * @param <K>
	 *            the generic type of the id of the given {@link BaseTreeNode} object
	 * @param treeElement
	 *            the {@link TreeElement} object
	 * @param parentTreeNode
	 *            the parent object
	 * @param idGenerator
	 *            the id generator
	 * @return the new {@link BaseTreeNode} object
	 */
	public static <T, K> BaseTreeNode<T, K> initializeBaseTreeNodeWithTreeElement(
		final T treeElement, BaseTreeNode<T, K> parentTreeNode,
		final @NonNull IdGenerator<K> idGenerator)
	{
		BaseTreeNode<T, K> treeNode = BaseTreeNode.<T, K> builder().id(idGenerator.getNextId())
			.value(treeElement).build();
		if (parentTreeNode != null)
		{
			parentTreeNode.addChild(treeNode);
		}
		return treeNode;
	}

	/**
	 * Factory method that creates a new {@link BaseTreeNode} object from the given
	 * {@link TreeElement} object
	 *
	 * @param <K>
	 *            the generic type of the id of the given {@link BaseTreeNode} object
	 * @param treeElement
	 *            the {@link TreeElement} object
	 * @param parentTreeNode
	 *            the parent object
	 * @param idGenerator
	 *            the id generator
	 * @return the new {@link BaseTreeNode} object
	 */
	public static <K> BaseTreeNode<JTreeElement, K> initializeTreeNodeWithTreeElement(
		final JTreeElement treeElement, BaseTreeNode<JTreeElement, K> parentTreeNode,
		final @NonNull IdGenerator<K> idGenerator)
	{
		BaseTreeNode<JTreeElement, K> treeNode = initializeBaseTreeNodeWithTreeElement(treeElement,
			parentTreeNode, idGenerator);
		treeNode.setLeaf(!treeElement.isNode());
		treeNode.setDisplayValue(treeElement.getName());
		return treeNode;
	}

	/**
	 * Factory method that creates a new {@link BaseTreeNode} object from the given
	 * {@link TreeElement} object
	 *
	 * @param <T>
	 *            the generic type of the given {@link BaseTreeNode} object
	 * @param <K>
	 *            the generic type of the id of the given {@link BaseTreeNode} object
	 * @param treeElement
	 *            the {@link TreeElement} object
	 * @param parentTreeNode
	 *            the parent object
	 * @param idGenerator
	 *            the id generator
	 * @return the new {@link BaseTreeNode} object
	 */
	public static <T, K> BaseTreeNode<GenericTreeElement<T>, K> initializeTreeNodeWithTreeElement(
		final GenericTreeElement<T> treeElement,
		BaseTreeNode<GenericTreeElement<T>, K> parentTreeNode,
		final @NonNull IdGenerator<K> idGenerator)
	{
		BaseTreeNode<GenericTreeElement<T>, K> treeNode = initializeBaseTreeNodeWithTreeElement(
			treeElement, parentTreeNode, idGenerator);
		treeNode.setLeaf(treeElement.isLeaf());
		treeNode.setDisplayValue(treeElement.getName());
		return treeNode;
	}

}
