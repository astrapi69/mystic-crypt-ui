/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.vault;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * The two facts an entry carries about itself: which entry it is, and when it was last changed
 * (#272, #273).
 * <p>
 * Both fields existed and were filled in exactly one place - importing from a KeePass database. An
 * entry created here had no identifier at all, and its modification date stopped being true the
 * moment somebody edited it.
 * <p>
 * The vault format already stores both: the bean is serialized with all non-transient fields and
 * neither is transient, so maintaining them changes the writing paths, not the format.
 */
public final class EntryIdentitySupport
{

	private EntryIdentitySupport()
	{
	}

	/**
	 * Gives every entry without an identifier one, and reports how many needed it.
	 * <p>
	 * Done on LOAD rather than only for newly created entries (#272). Filling the field only for
	 * new entries would leave the oldest data - the data most worth referencing - permanently
	 * without identity, and the feature would quietly not apply where it matters most. The caller
	 * marks the model as changed when the count is not zero, so what is assigned here is persisted
	 * with the next save and is the same identifier the next time the file is opened; an identifier
	 * that is regenerated per session is not an identifier.
	 * <p>
	 * An identifier that is already there is never replaced. Once assigned it does not change and
	 * is not reused.
	 *
	 * @param applicationModelBean
	 *            the freshly read model; null is accepted and changes nothing
	 * @return how many entries were given an identifier
	 */
	public static int assignMissingIdentifiers(final ApplicationModelBean applicationModelBean)
	{
		if (applicationModelBean == null)
		{
			return 0;
		}
		int assigned = 0;
		for (MysticCryptEntryModelBean entry : allEntriesOf(applicationModelBean))
		{
			if (entry.getId() == null)
			{
				entry.setId(UUID.randomUUID());
				assigned++;
			}
		}
		return assigned;
	}

	/**
	 * Gives this entry an identifier if it has none, and leaves an existing one alone.
	 * <p>
	 * The single place a new entry gets its identity, so the create flow and the duplicate flow
	 * cannot drift apart on it
	 *
	 * @param entry
	 *            the entry; null is accepted and changes nothing
	 */
	public static void assignIdentifierIfMissing(final MysticCryptEntryModelBean entry)
	{
		if (entry != null && entry.getId() == null)
		{
			entry.setId(UUID.randomUUID());
		}
	}

	/**
	 * Records that this entry has just been changed.
	 * <p>
	 * Only an actual edit sets it. A missing modification time cannot be reconstructed, so filling
	 * it in on load or on first save would invent a fact that is read later as measured - empty
	 * stays empty until somebody really edits the entry (#273). Everything that displays it already
	 * shows nothing for an unset value rather than today's date or an epoch
	 * ({@code EntryTimestampFormatter}).
	 *
	 * @param entry
	 *            the entry that was edited; null is accepted and changes nothing
	 */
	public static void markAsModified(final MysticCryptEntryModelBean entry)
	{
		if (entry == null)
		{
			return;
		}
		entry.setLastModificationTime(OffsetDateTime.now());
	}

	/**
	 * Every entry the model holds, from both places one can live in: the map of a node's entries
	 * and the tree the nodes hang in
	 *
	 * @param applicationModelBean
	 *            the model
	 * @return the entries, without nulls
	 */
	private static List<MysticCryptEntryModelBean> allEntriesOf(
		final ApplicationModelBean applicationModelBean)
	{
		List<MysticCryptEntryModelBean> entries = new ArrayList<>();
		addAll(entries, entriesOfNodes(applicationModelBean.getDataOfNodes()));
		addAll(entries, entriesOfTree(applicationModelBean.getRootTreeAsMap()));
		return entries;
	}

	private static List<MysticCryptEntryModelBean> entriesOfNodes(
		final Map<Long, List<MysticCryptEntryModelBean>> entriesByNodeId)
	{
		if (entriesByNodeId == null)
		{
			return List.of();
		}
		List<MysticCryptEntryModelBean> entries = new ArrayList<>();
		entriesByNodeId.values().forEach(perNode -> addAll(entries, perNode));
		return entries;
	}

	private static List<MysticCryptEntryModelBean> entriesOfTree(
		final Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> treeAsMap)
	{
		if (treeAsMap == null)
		{
			return List.of();
		}
		List<MysticCryptEntryModelBean> entries = new ArrayList<>();
		treeAsMap.values().stream().filter(node -> node != null && node.getValue() != null)
			.map(node -> node.getValue().getDefaultContent())
			.forEach(perNode -> addAll(entries, perNode));
		return entries;
	}

	private static void addAll(final List<MysticCryptEntryModelBean> target,
		final Collection<MysticCryptEntryModelBean> source)
	{
		if (source == null)
		{
			return;
		}
		source.stream().filter(entry -> entry != null).forEach(target::add);
	}
}
