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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

/**
 * Imported KeePass entries carry creation/access/modification timestamps and a precise expiry
 * time-of-day (#206) that were captured by {@code KeePassEntryConverter} but never rendered
 * anywhere - this proves the display formatting used to finally show them.
 */
class EntryTimestampFormatterTest
{

	@Test
	void formatRendersDateTimeAndOffset()
	{
		OffsetDateTime timestamp = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0,
			ZoneOffset.ofHours(2));

		assertEquals("2024-01-15 10:30 +02:00", EntryTimestampFormatter.format(timestamp));
	}

	@Test
	void formatOfNullIsBlank()
	{
		assertEquals("", EntryTimestampFormatter.format(null));
	}
}
