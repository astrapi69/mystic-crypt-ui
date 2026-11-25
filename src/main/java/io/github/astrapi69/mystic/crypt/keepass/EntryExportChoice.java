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
import java.util.EnumSet;
import java.util.Set;

import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;

/**
 * What of each chosen entry goes into a KeePass file that holds only those entries (#1118): the
 * fields the user chose, the attachments only when chosen, and never the history.
 *
 * @param fields
 *            the fields that go into the file
 * @param attachments
 *            whether the attachments go into the file
 */
public record EntryExportChoice(Set<EntryExportField> fields, boolean attachments) {

	/**
	 * The choice, holding its own copy of the fields
	 *
	 * @param fields
	 *            the fields that go into the file
	 * @param attachments
	 *            whether the attachments go into the file
	 */
	public EntryExportChoice {
		fields = fields.isEmpty() ? EnumSet.noneOf(EntryExportField.class) : EnumSet.copyOf(fields);
	}

	/**
	 * The entry as it goes into the file: a copy holding only what was chosen, without the history,
	 * and dated by its creation when it was never edited. Without that date the export would stamp
	 * the time of the export, and a recipient who merges a later export would take every unedited
	 * entry for a newer one. The vault's own entry is left as it is
	 *
	 * @param entry
	 *            the entry of the vault
	 * @return the copy that is written
	 */
	public MysticCryptEntryModelBean exportedCopyOf(final MysticCryptEntryModelBean entry)
	{
		MysticCryptEntryModelBean copy = entry.duplicate();
		copy.setTitle(chosen(EntryExportField.TITLE) ? copy.getTitle() : null);
		copy.setUserName(chosen(EntryExportField.USER_NAME) ? copy.getUserName() : null);
		copy.setPassword(chosen(EntryExportField.PASSWORD) ? copy.getPassword() : null);
		copy.setUrl(chosen(EntryExportField.URL) ? copy.getUrl() : null);
		copy.setNotes(chosen(EntryExportField.NOTES) ? copy.getNotes() : null);
		if (!chosen(EntryExportField.CUSTOM_FIELDS))
		{
			copy.setProperties(new ArrayList<>());
			copy.setProtectedPropertyKeys(null);
		}
		if (!attachments)
		{
			copy.setResources(null);
		}
		if (copy.getLastModificationTime() == null)
		{
			copy.setLastModificationTime(copy.getCreationTime());
		}
		return copy;
	}

	private boolean chosen(final EntryExportField field)
	{
		return fields.contains(field);
	}
}
