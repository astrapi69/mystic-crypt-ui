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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.astrapi69.collection.list.ListExtensions;
import io.github.astrapi69.collection.pair.KeySetPair;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.file.create.FileContentInfo;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MysticCryptEntryModelBean
{
	UUID id;
	String title;
	String userName;
	char[] password;
	char[] repeat;
	String url;
	String notes;
	boolean expirable;
	LocalDate expires;
	String icon;

	/** The flag if the password is displayed in plain text */
	boolean showPassword;
	@Builder.Default
	List<FileContentInfo> resources = new ArrayList<>();

	@Builder.Default
	List<KeyValuePair<String, String>> properties = new ArrayList<>();

	public String getProperty(String name)
	{
		return KeyValuePair.toMap(properties).get(name);
	}

	public void setProperty(String name, String value)
	{
		properties.add(KeyValuePair.<String, String> builder().key(name).value(value).build());
	}

	public boolean removeProperty(String name)
	{
		String value = getProperty(name);
		KeyValuePair<String, String> remove = KeyValuePair.<String, String> builder().key(name)
			.value(value).build();
		int indexToRemove = properties.indexOf(remove);
		if (-1 < indexToRemove)
		{
			properties.remove(indexToRemove);
			return true;
		}
		return false;
	}

	public List<String> getPropertyNames()
	{
		return ListExtensions.toList(KeyValuePair.toMap(properties).keySet());
	}

	public KeySetPair<String, LocalDateTime> getKeySetPair()
	{
		return null;
	}

	public String getPath()
	{
		return "";
	}
}
