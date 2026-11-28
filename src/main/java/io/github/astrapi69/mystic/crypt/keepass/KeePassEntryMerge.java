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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * Merges the entries of an imported KeePass file into the entries the vault already holds, the way
 * KeePassXC merges a database: an entry is matched by its identifier (UUID) whatever its group, and
 * the entry stays in the group the vault has it in (#1119).
 * <p>
 * Of the two versions of a matched entry the one modified later becomes current and the other goes
 * into the entry's history. An entry never edited has no modification time and counts as modified
 * when it was created, as the export of chosen entries dates it (#1118). Equal times keep the
 * vault's version current, and two versions with the same content write no history. The history has
 * no cap; a cap belongs to the issue that makes the history visible (#402). The history of the
 * file's version is taken over, each version once.
 * <p>
 * An imported entry the vault does not hold stays where the import put it. Groups are not matched:
 * how groups merge is the question of #855.
 */
public final class KeePassEntryMerge
{

	private KeePassEntryMerge()
	{
	}

	/**
	 * The entries the vault holds, by identifier, from both places an entry lives in: the live tree
	 * and the tree map of the model, and the node data of the model. An entry that is reachable
	 * from two of them is listed once. Taken before an import converts its file, so the imported
	 * entries are not in it
	 *
	 * @param root
	 *            the live root of the vault tree
	 * @param applicationModelBean
	 *            the model of the open vault
	 * @return the entries, each identifier with every entry that carries it; an entry without an
	 *         identifier is not listed
	 */
	public static Map<UUID, List<MysticCryptEntryModelBean>> entriesById(
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root,
		final ApplicationModelBean applicationModelBean)
	{
		Map<MysticCryptEntryModelBean, Boolean> seen = new IdentityHashMap<>();
		Map<UUID, List<MysticCryptEntryModelBean>> byId = new LinkedHashMap<>();
		collectFromTree(root, seen, byId);
		Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> treeAsMap = applicationModelBean
			.getRootTreeAsMap();
		if (treeAsMap != null)
		{
			for (TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node : treeAsMap
				.values())
			{
				if (node != null && node.getValue() != null)
				{
					listAll(node.getValue().getDefaultContent(), seen, byId);
				}
			}
		}
		Map<Long, List<MysticCryptEntryModelBean>> dataOfNodes = applicationModelBean
			.getDataOfNodes();
		if (dataOfNodes != null)
		{
			dataOfNodes.values().forEach(perNode -> listAll(perNode, seen, byId));
		}
		return byId;
	}

	/**
	 * Merges every entry of the given imported node and its subnodes whose identifier the vault
	 * holds into the vault's entries, and takes it out of its imported group
	 *
	 * @param imported
	 *            the node the import converted, with its subnodes
	 * @param inTheVault
	 *            the vault's entries as {@link #entriesById} listed them before the import
	 * @return how many imported entries were merged into an entry of the vault
	 */
	public static int mergeInto(
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> imported,
		final Map<UUID, List<MysticCryptEntryModelBean>> inTheVault)
	{
		int merged = 0;
		List<MysticCryptEntryModelBean> entries = imported.getValue().getDefaultContent();
		if (entries != null)
		{
			List<MysticCryptEntryModelBean> remaining = new ArrayList<>();
			for (MysticCryptEntryModelBean entry : entries)
			{
				List<MysticCryptEntryModelBean> held = entry.getId() == null
					? null
					: inTheVault.get(entry.getId());
				if (held == null)
				{
					remaining.add(entry);
					continue;
				}
				held.forEach(inVault -> merge(inVault, entry));
				merged++;
			}
			entries.clear();
			entries.addAll(remaining);
		}
		if (imported.getChildren() != null)
		{
			for (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> child : imported
				.getChildren())
			{
				merged += mergeInto(child, inTheVault);
			}
		}
		return merged;
	}

	/**
	 * Merges the imported version of an entry into the vault's entry of the same identifier, which
	 * is changed in place: see the class Javadoc for which version becomes current
	 *
	 * @param inVault
	 *            the vault's entry
	 * @param imported
	 *            the entry of the file
	 */
	static void merge(final MysticCryptEntryModelBean inVault,
		final MysticCryptEntryModelBean imported)
	{
		List<MysticCryptEntryModelBean> versions = new ArrayList<>();
		addAll(versions, inVault.getHistory());
		addAll(versions, imported.getHistory());
		boolean same = hasTheSameContent(inVault, imported);
		if (isLater(timeOf(imported), timeOf(inVault)))
		{
			if (!same)
			{
				versions.add(inVault.duplicate());
			}
			takeOver(inVault, imported);
		}
		else if (!same)
		{
			versions.add(imported.duplicate());
		}
		inVault.setHistory(eachVersionOnce(versions));
	}

	/**
	 * When the entry was last modified, or created if it never was edited; null when neither is
	 * known
	 */
	static Instant timeOf(final MysticCryptEntryModelBean entry)
	{
		if (entry.getLastModificationTime() != null)
		{
			return entry.getLastModificationTime().toInstant();
		}
		return entry.getCreationTime() != null ? entry.getCreationTime().toInstant() : null;
	}

	private static boolean isLater(final Instant time, final Instant than)
	{
		return time != null && (than == null || time.isAfter(than));
	}

	/**
	 * The versions oldest first, one per modification time: a version already in the history is not
	 * written again when a file that carries it is imported again
	 */
	private static List<MysticCryptEntryModelBean> eachVersionOnce(
		final List<MysticCryptEntryModelBean> versions)
	{
		List<MysticCryptEntryModelBean> once = new ArrayList<>();
		Set<Instant> times = new HashSet<>();
		for (MysticCryptEntryModelBean version : versions)
		{
			Instant time = timeOf(version);
			if (time == null || times.add(time))
			{
				once.add(version);
			}
		}
		once.sort(Comparator.comparing(KeePassEntryMerge::timeOf,
			Comparator.nullsFirst(Comparator.naturalOrder())));
		return once;
	}

	/**
	 * Gives the vault's entry the content of the imported one, keeping what only the vault knows:
	 * its identifier, its history, the application's own icon, when it was created and whether its
	 * password is shown
	 */
	private static void takeOver(final MysticCryptEntryModelBean inVault,
		final MysticCryptEntryModelBean imported)
	{
		inVault.setTitle(imported.getTitle());
		inVault.setUserName(imported.getUserName());
		inVault.setPassword(imported.getPassword());
		inVault.setUrl(imported.getUrl());
		inVault.setNotes(imported.getNotes());
		inVault.setExpirable(imported.isExpirable());
		inVault.setExpires(imported.getExpires());
		inVault.setPreciseExpiryTime(imported.getPreciseExpiryTime());
		inVault.setLastModificationTime(imported.getLastModificationTime());
		if (inVault.getCreationTime() == null)
		{
			inVault.setCreationTime(imported.getCreationTime());
		}
		if (imported.getLastAccessTime() != null && (inVault.getLastAccessTime() == null
			|| imported.getLastAccessTime().isAfter(inVault.getLastAccessTime())))
		{
			inVault.setLastAccessTime(imported.getLastAccessTime());
		}
		inVault.setKeePassIconIndex(imported.getKeePassIconIndex());
		inVault.setResources(imported.getResources());
		inVault.setProperties(imported.getProperties());
		inVault.setProtectedPropertyKeys(imported.getProtectedPropertyKeys());
	}

	private static boolean hasTheSameContent(final MysticCryptEntryModelBean first,
		final MysticCryptEntryModelBean second)
	{
		return sameText(first.getTitle(), second.getTitle())
			&& sameText(first.getUserName(), second.getUserName())
			&& sameText(first.getPassword(), second.getPassword())
			&& sameText(first.getUrl(), second.getUrl())
			&& sameText(first.getNotes(), second.getNotes())
			&& first.isExpirable() == second.isExpirable()
			&& Objects.equals(first.getExpires(), second.getExpires())
			&& Objects.equals(first.getPreciseExpiryTime(), second.getPreciseExpiryTime())
			&& Objects.equals(first.getKeePassIconIndex(), second.getKeePassIconIndex())
			&& Objects.equals(nullAsEmpty(first.getProperties()),
				nullAsEmpty(second.getProperties()))
			&& Objects.equals(first.getProtectedPropertyKeys(), second.getProtectedPropertyKeys())
			&& sameAttachments(first.getResources(), second.getResources());
	}

	/** Unset and empty are the same text: the vault holds null where a file holds "" (#384) */
	private static boolean sameText(final char[] first, final char[] second)
	{
		return Arrays.equals(first == null ? new char[0] : first,
			second == null ? new char[0] : second);
	}

	private static boolean sameAttachments(final List<FileContentInfo> first,
		final List<FileContentInfo> second)
	{
		List<FileContentInfo> left = nullAsEmpty(first);
		List<FileContentInfo> right = nullAsEmpty(second);
		if (left.size() != right.size())
		{
			return false;
		}
		for (int index = 0; index < left.size(); index++)
		{
			if (!Objects.equals(left.get(index).getName(), right.get(index).getName())
				|| !Arrays.equals(left.get(index).getContent(), right.get(index).getContent()))
			{
				return false;
			}
		}
		return true;
	}

	private static <T> List<T> nullAsEmpty(final List<T> list)
	{
		return list == null ? List.of() : list;
	}

	private static void addAll(final List<MysticCryptEntryModelBean> target,
		final Collection<MysticCryptEntryModelBean> source)
	{
		if (source != null)
		{
			target.addAll(source);
		}
	}

	private static void collectFromTree(
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node,
		final Map<MysticCryptEntryModelBean, Boolean> seen,
		final Map<UUID, List<MysticCryptEntryModelBean>> byId)
	{
		if (node.getValue() != null)
		{
			listAll(node.getValue().getDefaultContent(), seen, byId);
		}
		if (node.getChildren() != null)
		{
			node.getChildren().forEach(child -> collectFromTree(child, seen, byId));
		}
	}

	private static void listAll(final Collection<MysticCryptEntryModelBean> entries,
		final Map<MysticCryptEntryModelBean, Boolean> seen,
		final Map<UUID, List<MysticCryptEntryModelBean>> byId)
	{
		if (entries == null)
		{
			return;
		}
		for (MysticCryptEntryModelBean entry : entries)
		{
			if (entry != null && entry.getId() != null && seen.put(entry, Boolean.TRUE) == null)
			{
				byId.computeIfAbsent(entry.getId(), identifier -> new ArrayList<>()).add(entry);
			}
		}
	}
}
