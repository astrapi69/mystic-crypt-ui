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
package io.github.astrapi69.mystic.crypt.keepass;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;
import org.linguafranca.pwdb.kdbx.simple.SimpleEntry;
import org.linguafranca.pwdb.kdbx.simple.SimpleGroup;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * Converts the group/entry tree of a KeePassJava2 {@link SimpleDatabase} into/from this app's own
 * {@link BaseTreeNode} of {@link GenericTreeElement}s
 */
public final class KeePassTreeConverter
{

	private KeePassTreeConverter()
	{
	}

	/**
	 * Recursively converts the given KeePass group (and its subgroups/entries) into a new
	 * {@link BaseTreeNode}, attached as a child of the given parent node
	 *
	 * @param group
	 *            the KeePass group to convert
	 * @param parent
	 *            the tree node the new node becomes a child of
	 * @param nextId
	 *            supplies the next id for each newly created tree node
	 * @return the newly created tree node for the given group
	 */
	public static BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> toTreeNode(
		SimpleGroup group,
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> parent,
		Supplier<Long> nextId)
	{
		boolean leaf = group.getGroups().isEmpty();
		String name = group.getName();

		List<MysticCryptEntryModelBean> entries = new ArrayList<>();
		for (SimpleEntry entry : group.getEntries())
		{
			entries.add(KeePassEntryConverter.toEntryModelBean(entry));
		}

		GenericTreeElement<List<MysticCryptEntryModelBean>> treeElement = GenericTreeElement
			.<List<MysticCryptEntryModelBean>> builder().name(name).leaf(leaf).build();
		treeElement.setDefaultContent(entries);

		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> treeNode = BaseTreeNode
			.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder().id(nextId.get())
			.value(treeElement).parent(parent).displayValue(name).leaf(leaf).build();

		if (parent != null)
		{
			parent.addChild(treeNode);
		}

		for (SimpleGroup subGroup : group.getGroups())
		{
			toTreeNode(subGroup, treeNode, nextId);
		}

		return treeNode;
	}

	/**
	 * Recursively converts the given tree node (and its children) into a new KeePass group of the
	 * given database
	 *
	 * @param database
	 *            the database the new group belongs to
	 * @param treeNode
	 *            the tree node to convert
	 * @param parent
	 *            the KeePass group the new group becomes a child of
	 * @return the newly created group
	 */
	public static SimpleGroup toSimpleGroup(SimpleDatabase database,
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> treeNode,
		SimpleGroup parent)
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> treeElement = treeNode.getValue();
		SimpleGroup group = database.newGroup(treeElement.getName());
		parent.addGroup(group);

		List<MysticCryptEntryModelBean> entries = treeElement.getDefaultContent();
		if (entries != null)
		{
			for (MysticCryptEntryModelBean bean : entries)
			{
				group.addEntry(KeePassEntryConverter.toSimpleEntry(database, bean));
			}
		}

		if (treeNode.getChildren() != null)
		{
			for (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> child : treeNode
				.getChildren())
			{
				toSimpleGroup(database, child, group);
			}
		}

		return group;
	}

}
