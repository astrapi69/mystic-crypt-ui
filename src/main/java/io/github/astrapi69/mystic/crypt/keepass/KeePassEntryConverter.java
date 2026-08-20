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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.linguafranca.pwdb.Entry;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;
import org.linguafranca.pwdb.kdbx.simple.SimpleEntry;
import org.linguafranca.pwdb.kdbx.simple.SimpleIcon;

import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;

/**
 * Converts entries between KeePassJava2's {@link SimpleEntry} and this app's own
 * {@link MysticCryptEntryModelBean}, preserving every field the KeePassJava2 {@link Entry} API
 * exposes (UUID, icon index, creation/access/modification/expiry timestamps) so importing and then
 * exporting again is lossless. The one exception is KeePass's entry version history, which
 * {@code SimpleEntry} does not expose through any public API and so cannot be read here
 */
public final class KeePassEntryConverter
{

	private KeePassEntryConverter()
	{
	}

	/**
	 * Converts the given KeePass entry into a {@link MysticCryptEntryModelBean}
	 *
	 * @param entry
	 *            the KeePass entry to convert
	 * @return the converted entry model bean
	 */
	public static MysticCryptEntryModelBean toEntryModelBean(SimpleEntry entry)
	{
		OffsetDateTime preciseExpiryTime = entry.getExpires()
			? toOffsetDateTime(entry.getExpiryTime())
			: null;
		MysticCryptEntryModelBean bean = MysticCryptEntryModelBean.builder().id(entry.getUuid())
			.title(entry.getProperty(Entry.STANDARD_PROPERTY_NAME_TITLE))
			.userName(entry.getProperty(Entry.STANDARD_PROPERTY_NAME_USER_NAME))
			.password(toCharArray(entry.getProperty(Entry.STANDARD_PROPERTY_NAME_PASSWORD)))
			.url(entry.getProperty(Entry.STANDARD_PROPERTY_NAME_URL))
			.notes(entry.getProperty(Entry.STANDARD_PROPERTY_NAME_NOTES))
			.expirable(entry.getExpires())
			.expires(preciseExpiryTime != null ? preciseExpiryTime.toLocalDate() : null)
			.preciseExpiryTime(preciseExpiryTime)
			.creationTime(toOffsetDateTime(entry.getCreationTime()))
			.lastAccessTime(toOffsetDateTime(entry.getLastAccessTime()))
			.lastModificationTime(toOffsetDateTime(entry.getLastModificationTime()))
			.keePassIconIndex(entry.getIcon() != null ? entry.getIcon().getIndex() : null).build();

		for (String propertyName : entry.getPropertyNames())
		{
			if (!Entry.STANDARD_PROPERTY_NAMES.contains(propertyName))
			{
				bean.setProperty(propertyName, entry.getProperty(propertyName));
			}
		}

		List<FileContentInfo> resources = new ArrayList<>();
		for (String binaryPropertyName : entry.getBinaryPropertyNames())
		{
			resources.add(FileContentInfo.builder().name(binaryPropertyName)
				.content(entry.getBinaryProperty(binaryPropertyName)).build());
		}
		bean.setResources(resources);

		return bean;
	}

	/**
	 * Converts the given {@link MysticCryptEntryModelBean} into a new KeePass entry of the given
	 * database
	 *
	 * @param database
	 *            the database the new entry belongs to
	 * @param bean
	 *            the entry model bean to convert
	 * @return the new KeePass entry, not yet added to any group
	 */
	public static SimpleEntry toSimpleEntry(SimpleDatabase database, MysticCryptEntryModelBean bean)
	{
		SimpleEntry entry = database.newEntry();
		entry.setProperty(Entry.STANDARD_PROPERTY_NAME_TITLE, bean.getTitle());
		entry.setProperty(Entry.STANDARD_PROPERTY_NAME_USER_NAME, bean.getUserName());
		entry.setProperty(Entry.STANDARD_PROPERTY_NAME_PASSWORD, toStringValue(bean.getPassword()));
		entry.setProperty(Entry.STANDARD_PROPERTY_NAME_URL, bean.getUrl());
		entry.setProperty(Entry.STANDARD_PROPERTY_NAME_NOTES, bean.getNotes());

		entry.setExpires(bean.isExpirable());
		entry.setExpiryTime(toDate(expiryTimeOf(bean)));
		if (bean.getKeePassIconIndex() != null)
		{
			entry.setIcon(new SimpleIcon(bean.getKeePassIconIndex()));
		}
		// note: Entry has no public setUuid()/setCreationTime()/setLastAccessTime()/
		// setLastModificationTime() - those fields are managed internally by the library and
		// cannot be forced on export, so a re-exported entry gets a fresh uuid/timestamps even
		// though the original values were preserved above on import

		for (String propertyName : bean.getPropertyNames())
		{
			entry.setProperty(propertyName, bean.getProperty(propertyName));
		}

		for (FileContentInfo resource : bean.getResources())
		{
			entry.setBinaryProperty(resource.getName(), resource.getContent());
		}

		return entry;
	}

	private static OffsetDateTime expiryTimeOf(MysticCryptEntryModelBean bean)
	{
		if (bean.getPreciseExpiryTime() != null)
		{
			return bean.getPreciseExpiryTime();
		}
		if (bean.getExpires() != null)
		{
			return bean.getExpires().atStartOfDay().atOffset(ZoneOffset.UTC);
		}
		// Entry.setExpiryTime(...) requires a non-null value even when expirable is false
		return OffsetDateTime.now(ZoneOffset.UTC);
	}

	private static char[] toCharArray(String value)
	{
		return value == null ? null : value.toCharArray();
	}

	private static String toStringValue(char[] value)
	{
		return value == null ? null : String.valueOf(value);
	}

	private static OffsetDateTime toOffsetDateTime(Date date)
	{
		return date == null ? null : OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
	}

	private static Date toDate(OffsetDateTime dateTime)
	{
		return dateTime == null ? null : Date.from(dateTime.toInstant());
	}

}
