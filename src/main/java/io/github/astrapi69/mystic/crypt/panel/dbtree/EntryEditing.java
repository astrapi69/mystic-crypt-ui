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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.mystic.crypt.secret.SecretBuffers;

/**
 * The copy of an entry that the dialog "Edit Crypt Entry" works on, so that Cancel leaves the entry
 * as it was and OK takes what was typed (#881).
 * <p>
 * The dialog's fields write on every keystroke. Bound to the entry itself, they changed it while
 * the user typed, and Cancel had nothing to take back; the changed flag knew nothing of it either,
 * so a later save wrote the cancelled values. The copy has lists and properties of its own, because
 * the dialog changes those in place, and the entry's character buffers, because a keystroke
 * replaces a buffer rather than writing into it: copying them would only put a second copy of every
 * secret in memory.
 */
public final class EntryEditing
{

	private EntryEditing()
	{
	}

	/**
	 * The copy for the dialog: the entry's buffers and values, with its own attachment list, its
	 * own property pairs and its own set of protected property names
	 *
	 * @param entry
	 *            the entry to edit
	 * @return the copy
	 */
	public static MysticCryptEntryModelBean copyOf(final MysticCryptEntryModelBean entry)
	{
		return entry.toBuilder()
			.resources(entry.getResources() == null ? null : new ArrayList<>(entry.getResources()))
			.properties(entry.getProperties() == null ? null : ownPairs(entry.getProperties()))
			.protectedPropertyKeys(entry.getProtectedPropertyKeys() == null
				? null
				: new LinkedHashSet<>(entry.getProtectedPropertyKeys()))
			.build();
	}

	/**
	 * Takes what the dialog holds onto the entry, buffers included, so the entry stays the object
	 * the tree, the table and the plugins know.
	 * <p>
	 * The buffers the entry held before are not overwritten: "Duplicate entry" hands its copy the
	 * same buffers ({@link MysticCryptEntryModelBean#duplicate()}), so overwriting them here would
	 * empty another entry. The close of the vault overwrites them, as it always did
	 *
	 * @param entry
	 *            the entry that was edited
	 * @param edited
	 *            the dialog's copy of it
	 */
	public static void takeOver(final MysticCryptEntryModelBean entry,
		final MysticCryptEntryModelBean edited)
	{
		entry.setId(edited.getId());
		entry.setTitle(edited.getTitle());
		entry.setUserName(edited.getUserName());
		entry.setPassword(edited.getPassword());
		entry.setRepeat(edited.getRepeat());
		entry.setUrl(edited.getUrl());
		entry.setNotes(edited.getNotes());
		entry.setExpirable(edited.isExpirable());
		entry.setExpires(edited.getExpires());
		entry.setIcon(edited.getIcon());
		entry.setPreciseExpiryTime(edited.getPreciseExpiryTime());
		entry.setCreationTime(edited.getCreationTime());
		entry.setLastAccessTime(edited.getLastAccessTime());
		entry.setLastModificationTime(edited.getLastModificationTime());
		entry.setKeePassIconIndex(edited.getKeePassIconIndex());
		entry.setShowPassword(edited.isShowPassword());
		entry.setResources(edited.getResources());
		entry.setProperties(edited.getProperties());
		entry.setDateTimesOfModification(edited.getDateTimesOfModification());
		entry.setHistory(edited.getHistory());
		entry.setProtectedPropertyKeys(edited.getProtectedPropertyKeys());
	}

	/**
	 * Overwrites what the dialog typed and Cancel throws away: every buffer of the copy that is not
	 * one of the entry's, and the content of every attachment added in the dialog. Nothing the
	 * entry holds is touched
	 *
	 * @param entry
	 *            the entry that stays as it was
	 * @param edited
	 *            the dialog's copy of it
	 */
	public static void discard(final MysticCryptEntryModelBean entry,
		final MysticCryptEntryModelBean edited)
	{
		buffersOf(edited).filter(buffer -> buffersOf(entry).noneMatch(kept -> kept == buffer))
			.forEach(SecretBuffers::wipe);
		if (edited.getResources() == null)
		{
			return;
		}
		List<FileContentInfo> kept = entry.getResources() == null
			? List.of()
			: entry.getResources();
		edited.getResources().stream()
			.filter(attachment -> attachment != null
				&& kept.stream().noneMatch(keptAttachment -> keptAttachment == attachment))
			.forEach(attachment -> SecretBuffers.wipe(attachment.getContent()));
	}

	private static Stream<char[]> buffersOf(final MysticCryptEntryModelBean entry)
	{
		return Stream.of(entry.getTitle(), entry.getUserName(), entry.getPassword(),
			entry.getRepeat(), entry.getUrl(), entry.getNotes()).filter(buffer -> buffer != null);
	}

	private static List<KeyValuePair<String, String>> ownPairs(
		final List<KeyValuePair<String, String>> properties)
	{
		List<KeyValuePair<String, String>> pairs = new ArrayList<>();
		for (KeyValuePair<String, String> property : properties)
		{
			pairs.add(property == null
				? null
				: new KeyValuePair<>(property.getKey(), property.getValue()));
		}
		return pairs;
	}
}
