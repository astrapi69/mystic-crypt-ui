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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.model.BaseModel;

/**
 * Imported KeePass timestamps (creation/access/modification, precise expiry) were captured by
 * {@code KeePassEntryConverter} but never rendered anywhere in the entry editor (#206) - this
 * proves the panel now shows them, read-only, and stays blank when they are absent
 */
class MysticCryptEntryPanelTimestampsTest
{

	@Test
	void timestampFieldsShowTheModelValuesReadOnly()
	{
		OffsetDateTime creationTime = OffsetDateTime.of(2020, 3, 1, 8, 0, 0, 0,
			ZoneOffset.ofHours(1));
		OffsetDateTime lastAccessTime = OffsetDateTime.of(2021, 6, 15, 12, 30, 0, 0,
			ZoneOffset.ofHours(1));
		OffsetDateTime lastModificationTime = OffsetDateTime.of(2022, 9, 30, 18, 45, 0, 0,
			ZoneOffset.ofHours(2));
		OffsetDateTime preciseExpiryTime = OffsetDateTime.of(2023, 12, 24, 23, 59, 0, 0,
			ZoneOffset.ofHours(1));

		MysticCryptEntryModelBean modelBean = MysticCryptEntryModelBean.builder()
			.creationTime(creationTime).lastAccessTime(lastAccessTime)
			.lastModificationTime(lastModificationTime).preciseExpiryTime(preciseExpiryTime)
			.build();

		MysticCryptEntryPanel panel = new MysticCryptEntryPanel(BaseModel.of(modelBean));

		assertEquals(EntryTimestampFormatter.format(creationTime), panel.getTxtCreated().getText());
		assertEquals(EntryTimestampFormatter.format(lastAccessTime),
			panel.getTxtLastAccessed().getText());
		assertEquals(EntryTimestampFormatter.format(lastModificationTime),
			panel.getTxtLastModified().getText());
		assertEquals(EntryTimestampFormatter.format(preciseExpiryTime),
			panel.getTxtPreciseExpiry().getText());
		assertFalse(panel.getTxtCreated().isEditable(), "created must be read-only");
		assertFalse(panel.getTxtLastAccessed().isEditable(), "last accessed must be read-only");
		assertFalse(panel.getTxtLastModified().isEditable(), "last modified must be read-only");
		assertFalse(panel.getTxtPreciseExpiry().isEditable(), "precise expiry must be read-only");
	}

	@Test
	void timestampFieldsAreBlankWhenNotImported()
	{
		MysticCryptEntryPanel panel = new MysticCryptEntryPanel();

		assertEquals("", panel.getTxtCreated().getText());
		assertEquals("", panel.getTxtLastAccessed().getText());
		assertEquals("", panel.getTxtLastModified().getText());
		assertEquals("", panel.getTxtPreciseExpiry().getText());
	}
}
