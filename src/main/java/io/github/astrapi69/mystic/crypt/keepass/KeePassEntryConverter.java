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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.linguafranca.pwdb.Entry;
import org.linguafranca.pwdb.PropertyValue;
import org.linguafranca.pwdb.kdbx.jackson.JacksonDatabase;
import org.linguafranca.pwdb.kdbx.jackson.JacksonEntry;
import org.linguafranca.pwdb.kdbx.jackson.JacksonHistory;
import org.linguafranca.pwdb.kdbx.jackson.model.Times;

import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.mystic.crypt.panel.dbtree.EntryText;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;

/**
 * Converts entries between KeePassJava2's {@link JacksonEntry} and this application's
 * {@link MysticCryptEntryModelBean}, so that a KeePass entry taken in and given back out is the
 * entry it was: identifier, all four timestamps with the expiry flag, icon index, custom properties
 * with the protection the user gave them, attachments, and the version history (#384).
 * <p>
 * Jackson rather than Simple because Simple offers no way to read the history or to give an entry
 * back its identifier and timestamps. Where Jackson has no public setter either, the values go
 * through {@link KeePassLibraryFields}, the one class that reaches the library's fields by
 * reflection.
 * <p>
 * Three things about Jackson's entry that the order below depends on: {@code setProperty} always
 * stores a value unprotected, even the password; every property and binary setter stamps the
 * modification time; and a new entry already carries the five standard properties, empty.
 */
public final class KeePassEntryConverter
{

	private KeePassEntryConverter()
	{
	}

	/**
	 * Converts the given KeePass entry into a {@link MysticCryptEntryModelBean}, its history
	 * included
	 *
	 * @param entry
	 *            the KeePass entry to convert
	 * @return the converted entry model bean
	 */
	public static MysticCryptEntryModelBean toEntryModelBean(final JacksonEntry entry)
	{
		OffsetDateTime preciseExpiryTime = entry.getExpires()
			? toOffsetDateTime(entry.getExpiryTime())
			: null;
		MysticCryptEntryModelBean bean = MysticCryptEntryModelBean.builder().id(entry.getUuid())
			.title(EntryText.asCharacters(entry.getProperty(Entry.STANDARD_PROPERTY_NAME_TITLE)))
			.userName(unlessUnset(entry.getProperty(Entry.STANDARD_PROPERTY_NAME_USER_NAME)))
			.password(
				EntryText.asCharacters(entry.getProperty(Entry.STANDARD_PROPERTY_NAME_PASSWORD)))
			.url(unlessUnset(entry.getProperty(Entry.STANDARD_PROPERTY_NAME_URL)))
			.notes(unlessUnset(entry.getProperty(Entry.STANDARD_PROPERTY_NAME_NOTES)))
			.expirable(entry.getExpires())
			.expires(preciseExpiryTime != null ? preciseExpiryTime.toLocalDate() : null)
			.preciseExpiryTime(preciseExpiryTime)
			.creationTime(toOffsetDateTime(entry.getCreationTime()))
			.lastAccessTime(toOffsetDateTime(entry.getLastAccessTime()))
			.lastModificationTime(toOffsetDateTime(entry.getLastModificationTime()))
			.keePassIconIndex(entry.getIcon() != null ? entry.getIcon().getIndex() : null).build();

		Set<String> protectedKeys = new LinkedHashSet<>();
		for (String propertyName : entry.getPropertyNames())
		{
			if (Entry.STANDARD_PROPERTY_NAMES.contains(propertyName))
			{
				continue;
			}
			bean.setProperty(propertyName, entry.getProperty(propertyName));
			PropertyValue value = entry.getPropertyValue(propertyName);
			if (value != null && value.isProtected())
			{
				protectedKeys.add(propertyName);
			}
		}
		bean.setProtectedPropertyKeys(protectedKeys);

		List<FileContentInfo> resources = new ArrayList<>();
		for (String binaryPropertyName : entry.getBinaryPropertyNames())
		{
			resources.add(FileContentInfo.builder().name(binaryPropertyName)
				.content(entry.getBinaryProperty(binaryPropertyName)).build());
		}
		bean.setResources(resources);

		bean.setHistory(historyOf(entry));
		return bean;
	}

	/**
	 * Converts the given {@link MysticCryptEntryModelBean} into a new KeePass entry of the given
	 * database, carrying its identifier, timestamps, protection and history
	 *
	 * @param database
	 *            the database the new entry belongs to
	 * @param bean
	 *            the entry model bean to convert
	 * @return the new KeePass entry, not yet added to any group
	 */
	public static JacksonEntry toJacksonEntry(final JacksonDatabase database,
		final MysticCryptEntryModelBean bean)
	{
		JacksonEntry entry = database.newEntry();
		// the KeePass library takes Strings for every property, so this boundary is where an
		// entry's text becomes one; the entry's own fields stay characters (#294)
		setStandardProperty(database, entry, Entry.STANDARD_PROPERTY_NAME_TITLE, bean.getTitle());
		setStandardProperty(database, entry, Entry.STANDARD_PROPERTY_NAME_USER_NAME,
			bean.getUserName());
		setStandardProperty(database, entry, Entry.STANDARD_PROPERTY_NAME_PASSWORD,
			bean.getPassword());
		setStandardProperty(database, entry, Entry.STANDARD_PROPERTY_NAME_URL, bean.getUrl());
		setStandardProperty(database, entry, Entry.STANDARD_PROPERTY_NAME_NOTES, bean.getNotes());

		Set<String> protectedKeys = bean.getProtectedPropertyKeys();
		for (String propertyName : bean.getPropertyNames())
		{
			String value = bean.getProperty(propertyName);
			boolean isProtected = protectedKeys != null && protectedKeys.contains(propertyName);
			entry.setPropertyValue(propertyName, valueOf(database, value, isProtected));
		}

		if (bean.getResources() != null)
		{
			for (FileContentInfo resource : bean.getResources())
			{
				entry.setBinaryProperty(resource.getName(), resource.getContent());
			}
		}

		if (bean.getKeePassIconIndex() != null)
		{
			entry.setIcon(database.newIcon(bean.getKeePassIconIndex()));
		}
		if (bean.getHistory() != null)
		{
			JacksonHistory history = new JacksonHistory();
			List<JacksonEntry> versions = new ArrayList<>();
			for (MysticCryptEntryModelBean version : bean.getHistory())
			{
				versions.add(toJacksonEntry(database, version));
			}
			history.setEntry(versions);
			KeePassLibraryFields.setHistory(entry, history);
		}
		if (bean.getId() != null)
		{
			KeePassLibraryFields.setUuid(entry, bean.getId());
		}
		// last: every setter above stamped the modification time with now
		KeePassLibraryFields.setTimes(entry, timesOf(bean, KeePassLibraryFields.getTimes(entry)));
		return entry;
	}

	/**
	 * User name, URL or notes as the vault has always carried them: null when the file does not set
	 * the field. Jackson reads an unset standard field as an empty string where the Simple reader
	 * read null, and every vault that exists holds null there (#384, decided by the maintainer)
	 */
	private static char[] unlessUnset(final String value)
	{
		return value == null || value.isEmpty() ? null : value.toCharArray();
	}

	private static List<MysticCryptEntryModelBean> historyOf(final JacksonEntry entry)
	{
		JacksonHistory history = KeePassLibraryFields.getHistory(entry);
		if (history == null || history.getEntry() == null)
		{
			return null;
		}
		List<MysticCryptEntryModelBean> versions = new ArrayList<>();
		for (JacksonEntry version : history.getEntry())
		{
			versions.add(toEntryModelBean(version));
		}
		return versions;
	}

	/**
	 * A standard property, protected when the database says that property is - which Jackson's own
	 * {@code setProperty} never does. A field the entry does not have is left as the new entry
	 * holds it, empty
	 */
	private static void setStandardProperty(final JacksonDatabase database,
		final JacksonEntry entry, final String name, final char[] value)
	{
		if (value == null)
		{
			return;
		}
		entry.setPropertyValue(name,
			valueOf(database, EntryText.asText(value), database.shouldProtect(name)));
	}

	private static PropertyValue valueOf(final JacksonDatabase database, final String value,
		final boolean isProtected)
	{
		PropertyValue.Factory<? extends PropertyValue> factory = isProtected
			? database.getPropertyValueStrategy().newProtected()
			: database.getPropertyValueStrategy().newUnprotected();
		return factory.of(value == null ? "" : value);
	}

	private static Times timesOf(final MysticCryptEntryModelBean bean, final Times fromTheLibrary)
	{
		Times times = fromTheLibrary != null ? fromTheLibrary : new Times(new Date());
		if (bean.getCreationTime() != null)
		{
			times.setCreationTime(toDate(bean.getCreationTime()));
		}
		if (bean.getLastAccessTime() != null)
		{
			times.setLastAccessTime(toDate(bean.getLastAccessTime()));
		}
		if (bean.getLastModificationTime() != null)
		{
			times.setLastModificationTime(toDate(bean.getLastModificationTime()));
		}
		OffsetDateTime expiryTime = expiryTimeOf(bean);
		if (expiryTime != null)
		{
			times.setExpiryTime(toDate(expiryTime));
		}
		times.setExpires(bean.isExpirable());
		return times;
	}

	private static OffsetDateTime expiryTimeOf(final MysticCryptEntryModelBean bean)
	{
		if (bean.getPreciseExpiryTime() != null)
		{
			return bean.getPreciseExpiryTime();
		}
		if (bean.getExpires() != null)
		{
			return bean.getExpires().atStartOfDay().atOffset(ZoneOffset.UTC);
		}
		return null;
	}

	private static OffsetDateTime toOffsetDateTime(final Date date)
	{
		return date == null ? null : OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
	}

	private static Date toDate(final OffsetDateTime dateTime)
	{
		return dateTime == null ? null : Date.from(dateTime.toInstant());
	}

}
