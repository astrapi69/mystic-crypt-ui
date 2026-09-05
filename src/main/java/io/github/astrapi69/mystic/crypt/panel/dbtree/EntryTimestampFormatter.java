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

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import lombok.experimental.UtilityClass;

/**
 * Renders the {@link OffsetDateTime} timestamps captured on a {@link MysticCryptEntryModelBean}
 * (creation/access/modification time, precise expiry) for read-only display, keeping the original
 * offset visible since that is part of what a lossless KeePass import preserves
 */
@UtilityClass
public class EntryTimestampFormatter
{

	private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter
		.ofPattern("yyyy-MM-dd HH:mm xxx");

	/**
	 * Formats the given timestamp for display, or returns an empty string if it is not set
	 *
	 * @param timestamp
	 *            the timestamp to format, may be {@code null}
	 * @return the formatted timestamp, or an empty string if {@code timestamp} is {@code null}
	 */
	public static String format(final OffsetDateTime timestamp)
	{
		return timestamp == null ? "" : timestamp.format(DISPLAY_FORMAT);
	}
}
