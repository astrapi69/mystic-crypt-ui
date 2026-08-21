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
package io.github.astrapi69.mystic.crypt.keepass2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.linguafranca.pwdb.Entry;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;
import org.linguafranca.pwdb.kdbx.simple.SimpleEntry;
import org.linguafranca.pwdb.kdbx.simple.SimpleGroup;

/**
 * Smoke test for the checked-in KeePass test fixture: {@code test-db.kdbx} must load with its known
 * credentials and contain at least one group and one entry with a readable title - the same fixture
 * the KeePass-import UI test builds on
 */
public class KeePass2Test
{

	@Test
	public void testKeePass2() throws Exception
	{
		KdbxCreds credentials = new KdbxCreds("foo-secret-bar-1969-?".getBytes());
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("test-db.kdbx");
		assertNotNull(inputStream, "test fixture test-db.kdbx must be on the test classpath");
		SimpleDatabase database = SimpleDatabase.load(credentials, inputStream);
		SimpleGroup rootGroup = database.getRootGroup();
		assertNotNull(rootGroup);

		List<SimpleGroup> allGroups = getAllGroups(rootGroup);
		allGroups.add(rootGroup);
		assertFalse(allGroups.isEmpty(), "the fixture must contain at least one group");

		List<SimpleEntry> allEntries = new ArrayList<>();
		for (SimpleGroup currentGroup : allGroups)
		{
			allEntries.addAll(currentGroup.getEntries());
		}
		assertFalse(allEntries.isEmpty(), "the fixture must contain at least one entry");
		assertTrue(
			allEntries.stream()
				.allMatch(entry -> entry.getProperty(Entry.STANDARD_PROPERTY_NAME_TITLE) != null),
			"every entry in the fixture must have a readable title");
	}

	public static List<SimpleGroup> getAllGroups(SimpleGroup group)
	{
		List<SimpleGroup> returnList = new ArrayList<>(group.getGroups());
		for (SimpleGroup currentGroup : group.getGroups())
		{
			returnList.addAll(getAllGroups(currentGroup));
		}
		return returnList;
	}

}
