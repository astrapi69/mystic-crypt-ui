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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.linguafranca.pwdb.kdbx.jackson.JacksonDatabase;
import org.linguafranca.pwdb.kdbx.jackson.JacksonEntry;
import org.linguafranca.pwdb.kdbx.jackson.JacksonGroup;
import org.linguafranca.pwdb.kdbx.jackson.model.Times;

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
	public static final String KEEPASS_CREATION_TIME_PROPERTY = "keepass.creationTime";
	public static final String KEEPASS_LAST_MODIFICATION_TIME_PROPERTY = "keepass.lastModificationTime";
	public static final String KEEPASS_LAST_ACCESS_TIME_PROPERTY = "keepass.lastAccessTime";
	public static final String KEEPASS_EXPIRY_TIME_PROPERTY = "keepass.expiryTime";
	public static final String KEEPASS_EXPIRES_PROPERTY = "keepass.expires";

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
		keepTheTimes(KeePassLibraryFields.getTimes(group), treeElement);

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
		// last: setting the icon and adding entries or groups stamp the modification time
		giveBackTheTimes(treeElement, group);
	}

	/**
	 * Keeps a group's four times and its expiry flag in the element's properties, as ISO-8601
	 * instants: text and a boolean are what the vault can carry without a new type in its format,
	 * and a vault that holds them opens in 8.5.1 like one with the group identifier already did
	 * (#413)
	 */
	private static void keepTheTimes(final Times times,
		final GenericTreeElement<List<MysticCryptEntryModelBean>> treeElement)
	{
		if (times == null)
		{
			return;
		}
		putInstant(treeElement, KEEPASS_CREATION_TIME_PROPERTY, times.getCreationTime());
		putInstant(treeElement, KEEPASS_LAST_MODIFICATION_TIME_PROPERTY,
			times.getLastModificationTime());
		putInstant(treeElement, KEEPASS_LAST_ACCESS_TIME_PROPERTY, times.getLastAccessTime());
		putInstant(treeElement, KEEPASS_EXPIRY_TIME_PROPERTY, times.getExpiryTime());
		if (times.getExpires() != null)
		{
			treeElement.getProperties().put(KEEPASS_EXPIRES_PROPERTY, times.getExpires());
		}
	}

	/**
	 * Writes a group's kept times back through the capsule; a time the element does not carry - a
	 * group created in this application - keeps the one the library gave the new group
	 */
	private static void giveBackTheTimes(
		final GenericTreeElement<List<MysticCryptEntryModelBean>> treeElement,
		final JacksonGroup group)
	{
		Times times = KeePassLibraryFields.getTimes(group);
		if (times == null)
		{
			times = new Times(new Date());
		}
		Date creation = dateOf(treeElement, KEEPASS_CREATION_TIME_PROPERTY);
		if (creation != null)
		{
			times.setCreationTime(creation);
		}
		Date modification = dateOf(treeElement, KEEPASS_LAST_MODIFICATION_TIME_PROPERTY);
		if (modification != null)
		{
			times.setLastModificationTime(modification);
		}
		Date access = dateOf(treeElement, KEEPASS_LAST_ACCESS_TIME_PROPERTY);
		if (access != null)
		{
			times.setLastAccessTime(access);
		}
		Date expiry = dateOf(treeElement, KEEPASS_EXPIRY_TIME_PROPERTY);
		if (expiry != null)
		{
			times.setExpiryTime(expiry);
		}
		if (treeElement.getProperties().get(KEEPASS_EXPIRES_PROPERTY)instanceof Boolean expires)
		{
			times.setExpires(expires);
		}
		KeePassLibraryFields.setTimes(group, times);
	}

	private static void putInstant(
		final GenericTreeElement<List<MysticCryptEntryModelBean>> treeElement, final String key,
		final Date date)
	{
		if (date != null)
		{
			treeElement.getProperties().put(key, date.toInstant().toString());
		}
	}

	private static Date dateOf(
		final GenericTreeElement<List<MysticCryptEntryModelBean>> treeElement, final String key)
	{
		return treeElement.getProperties().get(key)instanceof String text
			? Date.from(Instant.parse(text))
			: null;
	}

}
