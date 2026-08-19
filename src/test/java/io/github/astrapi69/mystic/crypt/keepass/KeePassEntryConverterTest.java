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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.linguafranca.pwdb.Entry;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;
import org.linguafranca.pwdb.kdbx.simple.SimpleEntry;

import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;

public class KeePassEntryConverterTest
{

	@Test
	public void testToEntryModelBean()
	{
		SimpleDatabase database = new SimpleDatabase();
		SimpleEntry entry = database.newEntry();
		entry.setProperty(Entry.STANDARD_PROPERTY_NAME_TITLE, "My Title");
		entry.setProperty(Entry.STANDARD_PROPERTY_NAME_USER_NAME, "my-user");
		entry.setProperty(Entry.STANDARD_PROPERTY_NAME_PASSWORD, "s3cr3t");
		entry.setProperty(Entry.STANDARD_PROPERTY_NAME_URL, "https://example.com");
		entry.setProperty(Entry.STANDARD_PROPERTY_NAME_NOTES, "some notes");
		entry.setProperty("custom-field", "custom-value");
		entry.setBinaryProperty("attachment.txt", "hello".getBytes());

		MysticCryptEntryModelBean bean = KeePassEntryConverter.toEntryModelBean(entry);

		assertEquals("My Title", bean.getTitle());
		assertEquals("my-user", bean.getUserName());
		assertArrayEquals("s3cr3t".toCharArray(), bean.getPassword());
		assertEquals("https://example.com", bean.getUrl());
		assertEquals("some notes", bean.getNotes());
		assertEquals("custom-value", bean.getProperty("custom-field"));
		assertTrue(bean.getResources().stream()
			.anyMatch(resource -> "attachment.txt".equals(resource.getName())
				&& "hello".equals(new String(resource.getContent()))));
	}

	@Test
	public void testToSimpleEntryRoundTrip()
	{
		SimpleDatabase database = new SimpleDatabase();
		MysticCryptEntryModelBean bean = MysticCryptEntryModelBean.builder().title("My Title")
			.userName("my-user").password("s3cr3t".toCharArray()).url("https://example.com")
			.notes("some notes").build();
		bean.setProperty("custom-field", "custom-value");

		SimpleEntry entry = KeePassEntryConverter.toSimpleEntry(database, bean);

		assertEquals("My Title", entry.getProperty(Entry.STANDARD_PROPERTY_NAME_TITLE));
		assertEquals("my-user", entry.getProperty(Entry.STANDARD_PROPERTY_NAME_USER_NAME));
		assertEquals("s3cr3t", entry.getProperty(Entry.STANDARD_PROPERTY_NAME_PASSWORD));
		assertEquals("https://example.com", entry.getProperty(Entry.STANDARD_PROPERTY_NAME_URL));
		assertEquals("some notes", entry.getProperty(Entry.STANDARD_PROPERTY_NAME_NOTES));
		assertEquals("custom-value", entry.getProperty("custom-field"));
	}

}
