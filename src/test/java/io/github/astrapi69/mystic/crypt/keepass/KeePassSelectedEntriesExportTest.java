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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.linguafranca.pwdb.Entry;
import org.linguafranca.pwdb.format.KdbxCredentials;
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase;
import org.linguafranca.pwdb.kdbx.jackson.KdbxEntry;

import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;

/**
 * Exporting chosen entries into a KeePass file of their own (#1118): only those entries, of each
 * only the chosen fields, no history, attachments only on choice, protection and identifiers kept,
 * and an entry that was never edited dated by its creation, so a later export merges at the
 * recipient instead of looking newer. Every assertion reads the file back that was written.
 */
class KeePassSelectedEntriesExportTest
{

	private static final OffsetDateTime CREATED = OffsetDateTime.of(2025, 3, 1, 9, 0, 0, 0,
		ZoneOffset.UTC);
	private static final OffsetDateTime EDITED = OffsetDateTime.of(2026, 5, 2, 10, 30, 0, 0,
		ZoneOffset.UTC);

	private static final EntryExportChoice EVERYTHING_BUT_ATTACHMENTS = new EntryExportChoice(
		EnumSet.allOf(EntryExportField.class), false);

	private final String password = TestPasswords.throwaway();

	private static final String ENTRY_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("only the given entries go into the file, each with its identifier")
	void onlyTheGivenEntriesGoIn_eachWithItsIdentifier() throws Exception
	{
		MysticCryptEntryModelBean first = entry("first");
		MysticCryptEntryModelBean second = entry("second");

		KdbxDatabase read = writtenAndReadBack(List.of(first, second), EVERYTHING_BUT_ATTACHMENTS);

		List<? extends Entry> entries = read.getRootGroup().getEntries();
		assertEquals(2, entries.size(), "the file holds other entries than the chosen ones");
		assertEquals(Set.of(first.getId(), second.getId()),
			Set.of(entries.get(0).getUuid(), entries.get(1).getUuid()),
			"an entry lost its identifier");
		assertTrue(read.getRootGroup().getGroups().isEmpty(), "the file holds groups");
	}

	@Test
	@DisplayName("a field that was not chosen is not in the file, a chosen one is")
	void aFieldNotChosen_isNotInTheFile() throws Exception
	{
		MysticCryptEntryModelBean entry = entry("mail");
		EntryExportChoice withoutPasswordAndNotes = new EntryExportChoice(
			EnumSet.of(EntryExportField.TITLE, EntryExportField.USER_NAME, EntryExportField.URL),
			false);

		Entry written = onlyEntryOf(writtenAndReadBack(List.of(entry), withoutPasswordAndNotes));

		assertEquals("mail", written.getProperty(Entry.STANDARD_PROPERTY_NAME_TITLE));
		assertEquals("mail-user", written.getProperty(Entry.STANDARD_PROPERTY_NAME_USER_NAME));
		assertEquals("https://mail.example", written.getProperty(Entry.STANDARD_PROPERTY_NAME_URL));
		assertTrue(blank(written.getProperty(Entry.STANDARD_PROPERTY_NAME_PASSWORD)),
			"the password went out although it was not chosen");
		assertTrue(blank(written.getProperty(Entry.STANDARD_PROPERTY_NAME_NOTES)),
			"the notes went out although they were not chosen");
		assertNull(written.getProperty("pin"), "a custom field went out although none was chosen");
	}

	@Test
	@DisplayName("protected fields stay protected in the file: the password and a protected custom field")
	void protectedFieldsStayProtected() throws Exception
	{
		MysticCryptEntryModelBean entry = entry("bank");

		Entry written = onlyEntryOf(writtenAndReadBack(List.of(entry), EVERYTHING_BUT_ATTACHMENTS));

		assertTrue(written.getPropertyValue(Entry.STANDARD_PROPERTY_NAME_PASSWORD).isProtected(),
			"the password is not protected in the file");
		assertTrue(written.getPropertyValue("pin").isProtected(),
			"the protected custom field is not protected in the file");
		assertFalse(written.getPropertyValue("branch").isProtected(),
			"an unprotected custom field came out protected");
		assertEquals("1234", written.getProperty("pin"));
	}

	@Test
	@DisplayName("no history goes into the file, even when the entry has one")
	void noHistoryGoesIntoTheFile() throws Exception
	{
		MysticCryptEntryModelBean entry = entry("with history");
		entry.setHistory(List.of(entry("an older version")));

		KdbxDatabase read = writtenAndReadBack(List.of(entry), EVERYTHING_BUT_ATTACHMENTS);

		List<MysticCryptEntryModelBean> history = KeePassEntryConverter
			.toEntryModelBean((KdbxEntry)onlyEntryOf(read)).getHistory();
		assertTrue(history == null || history.isEmpty(), "the history went into the file");
	}

	@Test
	@DisplayName("attachments go into the file only when they were chosen")
	void attachmentsGoIn_onlyWhenChosen() throws Exception
	{
		byte[] content = "the scan of a contract".getBytes(StandardCharsets.UTF_8);
		MysticCryptEntryModelBean entry = entry("with attachment");
		entry.setResources(
			List.of(FileContentInfo.builder().name("contract.pdf").content(content).build()));

		Entry without = onlyEntryOf(writtenAndReadBack(List.of(entry), EVERYTHING_BUT_ATTACHMENTS));
		Entry with = onlyEntryOf(writtenAndReadBack(List.of(entry),
			new EntryExportChoice(EnumSet.allOf(EntryExportField.class), true)));

		assertTrue(without.getBinaryPropertyNames().isEmpty(),
			"an attachment went out although none was chosen");
		assertArrayEquals(content, with.getBinaryProperty("contract.pdf"));
	}

	@Test
	@DisplayName("an entry never edited is dated by its creation, an edited one by its edit")
	void anEntryNeverEdited_isDatedByItsCreation() throws Exception
	{
		MysticCryptEntryModelBean neverEdited = entry("never edited");
		MysticCryptEntryModelBean edited = entry("edited");
		edited.setLastModificationTime(EDITED);

		KdbxDatabase read = writtenAndReadBack(List.of(neverEdited, edited),
			EVERYTHING_BUT_ATTACHMENTS);

		assertEquals(CREATED.toInstant(),
			entryTitled(read, "never edited").getLastModificationTime(),
			"an unedited entry looks newer than it is");
		assertEquals(EDITED.toInstant(), entryTitled(read, "edited").getLastModificationTime());
	}

	@Test
	@DisplayName("the export changes nothing in the vault's own entries")
	void theExportChangesNothingInTheVault() throws Exception
	{
		MysticCryptEntryModelBean entry = entry("untouched");
		entry.setHistory(List.of(entry("an older version")));
		entry.setResources(List.of(FileContentInfo.builder().name("a.txt")
			.content("a".getBytes(StandardCharsets.UTF_8)).build()));

		writtenAndReadBack(List.of(entry),
			new EntryExportChoice(EnumSet.of(EntryExportField.TITLE), false));

		assertArrayEquals(ENTRY_PASSWORD.toCharArray(), entry.getPassword());
		assertNotNull(entry.getHistory(), "the export took the history away from the vault");
		assertEquals(1, entry.getResources().size());
		assertNull(entry.getLastModificationTime(), "the export dated the vault's entry");
		assertEquals("1234", entry.getProperty("pin"));
	}

	private KdbxDatabase writtenAndReadBack(final List<MysticCryptEntryModelBean> entries,
		final EntryExportChoice choice) throws Exception
	{
		KdbxDatabase database = new KdbxDatabase();
		KeePassTreeConverter.fillDatabaseWith(database, entries, choice);
		KdbxCredentials credentials = new KdbxCredentials(
			password.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		database.write(credentials, file);
		return KdbxDatabase.read(credentials, new ByteArrayInputStream(file.toByteArray()));
	}

	private static Entry onlyEntryOf(final KdbxDatabase read)
	{
		assertEquals(1, read.getRootGroup().getEntries().size());
		return read.getRootGroup().getEntries().get(0);
	}

	private static Entry entryTitled(final KdbxDatabase read, final String title)
	{
		return read.getRootGroup().getEntries().stream()
			.filter(entry -> title.equals(entry.getProperty(Entry.STANDARD_PROPERTY_NAME_TITLE)))
			.findFirst().orElseThrow();
	}

	private static boolean blank(final String value)
	{
		return value == null || value.isEmpty();
	}

	private static MysticCryptEntryModelBean entry(final String title)
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().id(UUID.randomUUID())
			.title(title.toCharArray()).userName((title + "-user").toCharArray())
			.password(ENTRY_PASSWORD.toCharArray())
			.url(("https://" + title.replace(' ', '-') + ".example").toCharArray())
			.notes("a note".toCharArray()).creationTime(CREATED).build();
		entry.setProperty("pin", "1234");
		entry.setProperty("branch", "main street");
		entry.setProtectedPropertyKeys(Set.of("pin"));
		return entry;
	}
}
