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
import java.util.UUID;
import java.util.function.Supplier;

import org.linguafranca.pwdb.kdbx.jackson.JacksonDatabase;
import org.linguafranca.pwdb.kdbx.jackson.JacksonEntry;
import org.linguafranca.pwdb.kdbx.jackson.JacksonGroup;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * Converts the group/entry tree of a KeePassJava2 {@link JacksonDatabase} into/from this
 * application's {@link BaseTreeNode} of {@link GenericTreeElement}s.
 * <p>
 * {@link GenericTreeElement} is a library type this application does not own, so the KeePass
 * metadata of a group that has no field there - identifier, icon index, recycle-bin flag - is kept
 * in its generic properties map under the {@code KEEPASS_*} keys below, and written back from
 * there.
 * <p>
 * <b>The database's root group is a group like any other.</b> A KeePass file has exactly one root,
 * and this application's vault has one root node of its own. Importing hangs the KeePass root under
 * the vault root as a node with its own name; exporting a vault whose root holds exactly one node
 * makes that node the KeePass root again, with its name, identifier and icon. That is what makes
 * import and export inverse: the round trip neither adds a level nor renames one (#377). A vault
 * root with several nodes has no single root to give back, and its nodes go under the new
 * database's root.
 */
public final class KeePassTreeConverter
{

	public static final String KEEPASS_UUID_PROPERTY = "keepass.uuid";
	public static final String KEEPASS_ICON_INDEX_PROPERTY = "keepass.iconIndex";
	public static final String KEEPASS_RECYCLE_BIN_PROPERTY = "keepass.recycleBin";

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
		final JacksonGroup group,
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> parent,
		final Supplier<Long> nextId)
	{
		boolean leaf = group.getGroups().isEmpty();
		String name = group.getName();

		List<MysticCryptEntryModelBean> entries = new ArrayList<>();
		for (JacksonEntry entry : group.getEntries())
		{
			entries.add(KeePassEntryConverter.toEntryModelBean(entry));
		}

		GenericTreeElement<List<MysticCryptEntryModelBean>> treeElement = GenericTreeElement
			.<List<MysticCryptEntryModelBean>> builder().name(name).leaf(leaf).build();
		treeElement.setDefaultContent(entries);
		treeElement.getProperties().put(KEEPASS_UUID_PROPERTY, group.getUuid());
		if (group.getIcon() != null)
		{
			int iconIndex = group.getIcon().getIndex();
			treeElement.getProperties().put(KEEPASS_ICON_INDEX_PROPERTY, iconIndex);
			// the index alone is only good for writing the group back out; what the tree cell
			// renderer draws is the icon path, and a node that has one keeps its name only while it
			// is marked as carrying text (#206)
			String iconPath = KeePassIcons.pathOf(iconIndex);
			if (iconPath != null)
			{
				treeElement.setIconPath(iconPath);
				treeElement.setWithText(true);
			}
		}
		treeElement.getProperties().put(KEEPASS_RECYCLE_BIN_PROPERTY, group.isRecycleBin());

		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> treeNode = BaseTreeNode
			.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder().id(nextId.get())
			.value(treeElement).parent(parent).displayValue(name).leaf(leaf).build();

		if (parent != null)
		{
			parent.addChild(treeNode);
		}

		for (JacksonGroup subGroup : group.getGroups())
		{
			toTreeNode(subGroup, treeNode, nextId);
		}

		return treeNode;
	}

	/**
	 * Fills the given database from the given vault root: a single node becomes the database's root
	 * group itself, several nodes go under it - see the class Javadoc
	 *
	 * @param database
	 *            the database to fill, as created
	 * @param vaultRoot
	 *            the root node of the vault
	 */
	public static void fillDatabase(final JacksonDatabase database,
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> vaultRoot)
	{
		List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> nodes = vaultRoot
			.getChildren() != null ? new ArrayList<>(vaultRoot.getChildren()) : List.of();
		JacksonGroup rootGroup = database.getRootGroup();
		if (nodes.size() == 1)
		{
			fillGroup(database, nodes.get(0), rootGroup);
			return;
		}
		for (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node : nodes)
		{
			toJacksonGroup(database, node, rootGroup);
		}
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
	public static JacksonGroup toJacksonGroup(final JacksonDatabase database,
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> treeNode,
		final JacksonGroup parent)
	{
		JacksonGroup group = database.newGroup();
		parent.addGroup(group);
		fillGroup(database, treeNode, group);
		return group;
	}

	private static void fillGroup(final JacksonDatabase database,
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> treeNode,
		final JacksonGroup group)
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> treeElement = treeNode.getValue();
		group.setName(treeElement.getName());

		Object iconIndex = treeElement.getProperties().get(KEEPASS_ICON_INDEX_PROPERTY);
		if (iconIndex instanceof Integer index)
		{
			group.setIcon(database.newIcon(index));
		}
		Object uuid = treeElement.getProperties().get(KEEPASS_UUID_PROPERTY);
		if (uuid instanceof UUID identifier)
		{
			KeePassLibraryFields.setUuid(group, identifier);
		}

		List<MysticCryptEntryModelBean> entries = treeElement.getDefaultContent();
		if (entries != null)
		{
			for (MysticCryptEntryModelBean bean : entries)
			{
				group.addEntry(KeePassEntryConverter.toJacksonEntry(database, bean));
			}
		}

		if (treeNode.getChildren() != null)
		{
			for (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> child : treeNode
				.getChildren())
			{
				toJacksonGroup(database, child, group);
			}
		}
	}

}
