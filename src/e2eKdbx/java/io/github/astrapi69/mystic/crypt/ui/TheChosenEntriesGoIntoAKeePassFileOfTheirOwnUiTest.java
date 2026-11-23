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
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.linguafranca.pwdb.Entry;
import org.linguafranca.pwdb.format.KdbxCredentials;
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase;

import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.keepass.KdbxFacts;
import io.github.astrapi69.mystic.crypt.keepass.KeePassXcDump;

/**
 * End-to-end test of the export of chosen entries (#1118): two of three entries are selected in the
 * entry table and exported through its popup into a KeePass file of their own. The file is read
 * with {@code keepassxc-cli} and with the library the import uses, and holds exactly those two
 * entries, with their identifiers, their fields and no history.
 */
class TheChosenEntriesGoIntoAKeePassFileOfTheirOwnUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String EXPORT_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("two of three entries exported through the popup are in the file, with their identifiers and fields and without history (#1118)")
	void twoOfThreeEntries_goIntoAFileOfTheirOwn() throws Exception
	{
		File vault = new File(tempHome, "chosen-entries.mcrdb");
		File exported = new File(tempHome, "chosen-entries.kdbx");
		createDatabaseFileHeadless(vault, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(vault, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, "mail", "mail-user", TestPasswords.throwaway());
		application.addEntry(frame, "bank", "bank-user", TestPasswords.throwaway());
		application.addEntry(frame, "shop", "shop-user", TestPasswords.throwaway());
		Map<String, UUID> identifiers = Set.of("mail", "shop").stream().collect(Collectors
			.toMap(Function.identity(), title -> KdbxFixtures.liveEntryTitled(title).getId()));

		application.selectEntryRowsByTitle(frame, "mail", "shop")
			.exportSelectedEntriesToKeePass(frame, exported, EXPORT_PASSWORD);

		KdbxFacts facts = KdbxFacts.of(KeePassXcDump.xmlOf(exported, EXPORT_PASSWORD));
		assertEquals(Set.of("mail", "shop"),
			facts.entries().stream().map(KdbxFacts.Entry::title).collect(Collectors.toSet()),
			"the file holds other entries than the chosen ones");
		for (KdbxFacts.Entry entry : facts.entries())
		{
			assertEquals(asKeePassUuid(identifiers.get(entry.title())), entry.identifier(),
				"the entry " + entry.title() + " lost its identifier");
			assertEquals(entry.title() + "-user", entry.userName());
			assertEquals(0, entry.historyVersions(), "a history went into the file");
			assertTrue(entry.attachmentNames().isEmpty(), "an attachment went into the file");
		}

		KdbxDatabase readByTheImport = KdbxDatabase.read(
			new KdbxCredentials(EXPORT_PASSWORD.getBytes(StandardCharsets.UTF_8)),
			new ByteArrayInputStream(Files.readAllBytes(exported.toPath())));
		List<? extends Entry> read = readByTheImport.getRootGroup().getEntries();
		assertEquals(Set.copyOf(identifiers.values()),
			read.stream().map(Entry::getUuid).collect(Collectors.toSet()),
			"the library the import uses reads other identifiers");
	}

	/** KeePass writes a UUID as the base64 of its sixteen bytes, most significant first */
	private static String asKeePassUuid(final UUID identifier)
	{
		ByteBuffer bytes = ByteBuffer.allocate(16);
		bytes.putLong(identifier.getMostSignificantBits());
		bytes.putLong(identifier.getLeastSignificantBits());
		return Base64.getEncoder().encodeToString(bytes.array());
	}
}
