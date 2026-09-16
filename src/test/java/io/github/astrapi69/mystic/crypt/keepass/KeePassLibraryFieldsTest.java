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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.linguafranca.pwdb.kdbx.jackson.JacksonDatabase;
import org.linguafranca.pwdb.kdbx.jackson.JacksonEntry;
import org.linguafranca.pwdb.kdbx.jackson.JacksonGroup;
import org.linguafranca.pwdb.kdbx.jackson.JacksonHistory;
import org.linguafranca.pwdb.kdbx.jackson.model.Times;

/**
 * The capsule's whole purpose is to fail loudly the day KeePassJava2 renames one of the five fields
 * it reaches into, instead of letting an export go quietly lossy. So the test that matters is the
 * one that resolves every name against the library actually on the classpath (#382).
 */
class KeePassLibraryFieldsTest
{

	/**
	 * Every declared field name has to exist on the library class it names. A version bump that
	 * renames one turns this red, which is the only reason the capsule exists
	 */
	@Test
	@DisplayName("every field name the capsule knows exists in the library on the classpath")
	void everyFieldNameResolvesAgainstTheLibrary()
	{
		List<String> missing = KeePassLibraryFields.unresolvedFieldNames();

		assertTrue(missing.isEmpty(),
			"the capsule names fields " + KeePassLibraryFields.EXPECTED_LIBRARY_VERSION
				+ " does not have, so an export through it would be silently lossy: " + missing);
	}

	@Test
	@DisplayName("a wrong field name is reported, not swallowed")
	void anUnknownFieldNameFailsWithTheNameInTheMessage()
	{
		IllegalStateException thrown = assertThrows(IllegalStateException.class,
			() -> KeePassLibraryFields.field(JacksonEntry.class, "noSuchFieldHere"));

		assertTrue(thrown.getMessage().contains("noSuchFieldHere"),
			"the message has to name the field, or a failing export says nothing about why");
		assertTrue(thrown.getMessage().contains(JacksonEntry.class.getName()),
			"the message has to name the class the field was expected on");
		assertTrue(thrown.getMessage().contains(KeePassLibraryFields.EXPECTED_LIBRARY_VERSION),
			"the message has to name the library version the names were written against");
	}

	@Test
	@DisplayName("the identifier written into an entry is the identifier read back")
	void theEntryIdentifierIsWrittenThrough() throws IOException
	{
		JacksonDatabase database = new JacksonDatabase();
		JacksonEntry entry = database.newEntry();
		UUID identifier = UUID.fromString("527a5f19-606f-4e3a-b5ca-4c8f31602502");

		KeePassLibraryFields.setUuid(entry, identifier);

		assertEquals(identifier, entry.getUuid());
	}

	@Test
	@DisplayName("the identifier written into a group is the identifier read back")
	void theGroupIdentifierIsWrittenThrough() throws IOException
	{
		JacksonDatabase database = new JacksonDatabase();
		JacksonGroup group = database.newGroup("Team");
		UUID identifier = UUID.fromString("e39ac6c7-0d1a-4d2b-9f3e-1a2b3c4d5e6f");

		KeePassLibraryFields.setUuid(group, identifier);

		assertEquals(identifier, group.getUuid());
	}

	/**
	 * All four times at once, because they are one object in the library and a capsule that sets
	 * three of them would look right in a unit test and lose the fourth in a file
	 */
	@Test
	@DisplayName("all four times are written through, on an entry and on a group")
	void allFourTimesAreWrittenThrough() throws IOException
	{
		JacksonDatabase database = new JacksonDatabase();
		JacksonEntry entry = database.newEntry();
		JacksonGroup group = database.newGroup("Team");
		Date created = new Date(1_600_000_000_000L);
		Date modified = new Date(1_700_000_000_000L);
		Date accessed = new Date(1_750_000_000_000L);
		Date expires = new Date(1_800_000_000_000L);

		Times times = new Times();
		times.setCreationTime(created);
		times.setLastModificationTime(modified);
		times.setLastAccessTime(accessed);
		times.setExpiryTime(expires);
		times.setExpires(true);

		KeePassLibraryFields.setTimes(entry, times);
		KeePassLibraryFields.setTimes(group, times);

		assertEquals(created, entry.getCreationTime());
		assertEquals(modified, entry.getLastModificationTime());
		assertEquals(accessed, entry.getLastAccessTime());
		assertEquals(expires, entry.getExpiryTime());
		assertTrue(entry.getExpires(), "the expiry flag travels with the times, not beside them");
		// a group's times are read back through the capsule too: JacksonGroup exposes no getter for
		// any of them, so they are unreachable in both directions without it
		assertEquals(created, KeePassLibraryFields.getTimes(group).getCreationTime());
		assertEquals(expires, KeePassLibraryFields.getTimes(group).getExpiryTime());
		assertEquals(accessed, KeePassLibraryFields.getTimes(entry).getLastAccessTime());
	}

	@Test
	@DisplayName("history is written through and read back")
	void historyIsWrittenThroughAndReadBack() throws IOException
	{
		JacksonDatabase database = new JacksonDatabase();
		JacksonEntry entry = database.newEntry();
		JacksonEntry previous = database.newEntry();
		previous.setTitle("the previous version");
		JacksonHistory history = new JacksonHistory();
		history.setEntry(new ArrayList<>(List.of(previous)));

		KeePassLibraryFields.setHistory(entry, history);

		JacksonHistory readBack = KeePassLibraryFields.getHistory(entry);
		assertNotNull(readBack, "history has to come back out, or an export cannot write it");
		assertEquals(1, readBack.getEntry().size());
		assertEquals("the previous version", readBack.getEntry().get(0).getTitle());
	}

	@Test
	@DisplayName("an entry with no history reads back as none, not as a crash")
	void anEntryWithoutHistoryReadsBackAsNull() throws IOException
	{
		JacksonDatabase database = new JacksonDatabase();

		assertEquals(null, KeePassLibraryFields.getHistory(database.newEntry()),
			"a fresh entry has no history, and asking has to be allowed");
	}

	/**
	 * The capsule is only worth having if it is the ONLY place that reflects. A second site would
	 * drift, and the drifting one is the one no test covers (#382)
	 */
	@Test
	@DisplayName("nothing else in the main sources reflects on library types")
	void theCapsuleIsTheOnlyPlaceThatReflects() throws IOException
	{
		Path mainSources = Path.of("src", "main", "java");
		List<String> offenders = new ArrayList<>();
		try (Stream<Path> sources = Files.walk(mainSources))
		{
			sources.filter(path -> path.toString().endsWith(".java"))
				.filter(path -> !path.getFileName().toString().equals("KeePassLibraryFields.java"))
				.forEach(path -> {
					String source = read(path);
					// reflection AND a library type, both in one file. Either alone is legitimate:
					// ModelBinding reflects on this application's own model classes, and plenty of
					// files name a library type without reflecting. What must not exist twice is
					// the combination, which is what a renamed library field would break
					boolean reflects = source.contains("getDeclaredField")
						|| source.contains("java.lang.reflect");
					if (reflects && source.contains("org.linguafranca"))
					{
						offenders.add(mainSources.relativize(path).toString());
					}
				});
		}

		assertTrue(offenders.isEmpty(),
			"reflection on the library belongs in KeePassLibraryFields and nowhere else, so that a "
				+ "renamed field has exactly one site to fix - found it in: " + offenders);
	}

	private static String read(Path path)
	{
		try
		{
			return Files.readString(path, StandardCharsets.UTF_8);
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("cannot read " + path, exception);
		}
	}
}
