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
package io.github.astrapi69.mystic.crypt.plugin.vault;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;
import io.github.astrapi69.mystic.crypt.plugin.api.VaultAccess;
import io.github.astrapi69.mystic.crypt.plugin.api.VaultAccessRefusedException;
import io.github.astrapi69.mystic.crypt.vault.EntryIdentitySupport;

/**
 * The access one plugin gets: bound to its marker, it reads and writes only the entries that carry
 * it (docs/decisions/plugin-vault-access-2026-10-31.md). Entries go out as copies and come back
 * checked, so a plugin cannot reach the vault's own objects.
 */
public final class MarkedEntryAccess implements VaultAccess
{

	private final String marker;

	private final VaultState vault;

	/**
	 * The access for one marker over one vault
	 *
	 * @param marker
	 *            the plugin's marker, already checked against its namespace
	 * @param vault
	 *            the vault's state
	 */
	public MarkedEntryAccess(final String marker, final VaultState vault)
	{
		this.marker = marker;
		this.vault = vault;
	}

	@Override
	public boolean isAvailable()
	{
		return vault.isOpen() && vault.isUnlocked();
	}

	@Override
	public List<MysticCryptEntryModelBean> entries()
	{
		if (!isAvailable())
		{
			return List.of();
		}
		List<MysticCryptEntryModelBean> copies = new ArrayList<>();
		for (List<MysticCryptEntryModelBean> group : vault.entryLists())
		{
			group.stream().filter(this::isMarked).map(MarkedEntryAccess::copyOf)
				.forEach(copies::add);
		}
		return copies;
	}

	@Override
	public void add(final MysticCryptEntryModelBean entry)
	{
		requireWritable();
		requireMarked(entry);
		MysticCryptEntryModelBean stored = copyOf(entry);
		EntryIdentitySupport.assignIdentifierIfMissing(stored);
		vault.rootEntries().add(stored);
		vault.markChanged();
	}

	@Override
	public void change(final MysticCryptEntryModelBean entry)
	{
		requireWritable();
		requireMarked(entry);
		Location location = locate(entry.getId());
		MysticCryptEntryModelBean stored = copyOf(entry);
		EntryIdentitySupport.markAsModified(stored);
		location.group().set(location.index(), stored);
		vault.markChanged();
	}

	@Override
	public void remove(final UUID id)
	{
		requireWritable();
		Location location = locate(id);
		location.group().remove(location.index());
		vault.markChanged();
	}

	private boolean isMarked(final MysticCryptEntryModelBean entry)
	{
		return VaultEntryProperties.valuesOf(entry).containsKey(marker);
	}

	private void requireWritable()
	{
		if (!vault.isOpen())
		{
			throw new VaultAccessRefusedException("no vault is open");
		}
		if (!vault.isUnlocked())
		{
			throw new VaultAccessRefusedException("the vault is locked");
		}
		String readOnly = vault.readOnlyReason();
		if (readOnly != null)
		{
			throw new VaultAccessRefusedException(readOnly);
		}
	}

	private void requireMarked(final MysticCryptEntryModelBean entry)
	{
		if (!isMarked(entry))
		{
			throw new VaultAccessRefusedException(
				"the entry does not carry this plugin's marker " + marker);
		}
	}

	private Location locate(final UUID id)
	{
		return findMarked(id).orElseThrow(() -> new VaultAccessRefusedException(
			"this plugin has no entry with the identifier " + id));
	}

	private Optional<Location> findMarked(final UUID id)
	{
		for (List<MysticCryptEntryModelBean> group : vault.entryLists())
		{
			for (int index = 0; index < group.size(); index++)
			{
				MysticCryptEntryModelBean stored = group.get(index);
				if (id != null && id.equals(stored.getId()) && isMarked(stored))
				{
					return Optional.of(new Location(group, index));
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * A copy that shares no array and no attachment with the given entry, so neither side can
	 * change the other through it
	 */
	private static MysticCryptEntryModelBean copyOf(final MysticCryptEntryModelBean entry)
	{
		MysticCryptEntryModelBean copy = entry.duplicate();
		copy.setTitle(cloned(entry.getTitle()));
		copy.setUserName(cloned(entry.getUserName()));
		copy.setPassword(cloned(entry.getPassword()));
		copy.setRepeat(cloned(entry.getRepeat()));
		copy.setUrl(cloned(entry.getUrl()));
		copy.setNotes(cloned(entry.getNotes()));
		List<FileContentInfo> attachments = new ArrayList<>();
		if (entry.getResources() != null)
		{
			for (FileContentInfo attachment : entry.getResources())
			{
				attachments.add(FileContentInfo.builder().name(attachment.getName())
					.content(
						attachment.getContent() == null ? null : attachment.getContent().clone())
					.build());
			}
		}
		copy.setResources(attachments);
		return copy;
	}

	private static char[] cloned(final char[] value)
	{
		return value == null ? null : value.clone();
	}

	private record Location(List<MysticCryptEntryModelBean> group, int index) {
	}
}
