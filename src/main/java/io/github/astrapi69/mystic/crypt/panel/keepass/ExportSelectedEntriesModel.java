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
package io.github.astrapi69.mystic.crypt.panel.keepass;

import java.util.EnumSet;
import java.util.Set;

import io.github.astrapi69.mystic.crypt.keepass.EntryExportChoice;
import io.github.astrapi69.mystic.crypt.keepass.EntryExportField;
import lombok.Getter;
import lombok.Setter;

/**
 * What the user chose in the export of chosen entries (#1118): every field goes into the file
 * unless the user takes it out, and the attachments only when the user asks for them
 */
@Getter
@Setter
public class ExportSelectedEntriesModel
{

	private boolean title = true;

	private boolean userName = true;

	private boolean password = true;

	private boolean url = true;

	private boolean notes = true;

	private boolean customFields = true;

	private boolean attachments;

	/**
	 * The choice as the export reads it
	 *
	 * @return what of each entry goes into the file
	 */
	public EntryExportChoice choice()
	{
		Set<EntryExportField> fields = EnumSet.noneOf(EntryExportField.class);
		addWhen(fields, title, EntryExportField.TITLE);
		addWhen(fields, userName, EntryExportField.USER_NAME);
		addWhen(fields, password, EntryExportField.PASSWORD);
		addWhen(fields, url, EntryExportField.URL);
		addWhen(fields, notes, EntryExportField.NOTES);
		addWhen(fields, customFields, EntryExportField.CUSTOM_FIELDS);
		return new EntryExportChoice(fields, attachments);
	}

	private static void addWhen(final Set<EntryExportField> fields, final boolean chosen,
		final EntryExportField field)
	{
		if (chosen)
		{
			fields.add(field);
		}
	}
}
