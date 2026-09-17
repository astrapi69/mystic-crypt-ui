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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.keepass.KdbxFacts;
import io.github.astrapi69.mystic.crypt.keepass.KeePassXcDump;

/**
 * A database written by KeePassXC, taken into this application and given back the way a user does
 * it, has to come back the same.
 * <p>
 * "The way a user does it" is the whole path, and every step of it is here because leaving one out
 * hid a real difference: File > Import from KeePass, which files the import under a node called
 * "Imported from" and the file's name; File > Save; the application ended and the vault signed into
 * again, so that only what the vault file stores reaches the export; File > Export to KeePass. An
 * earlier version of this test called the converter directly and saw {@code Root/Root/Team} where
 * the application writes {@code Root/Imported from keepassxc-2.7.10-kdbx4.kdbx/Team} - a red that
 * existed and was not the red the test showed.
 * <p>
 * The vault holds the import and nothing else. A new vault comes with a starter node,
 * {@code mykeys} ({@code ApplicationPanel:79}), and the test deletes it first, the way a user
 * deletes a node. It is the vault's own content and exporting it is right, but left in, a KeePass
 * file with two groups comes back with one more for a reason that has nothing to do with the round
 * trip - measured: {@code Root/mykeys} in the export, with icon index 0.
 * <p>
 * The yardstick is KeePassXC, not this application's model. A field the model does not carry would
 * compare equal to itself and the test would pass while the file lost it - the circular measurement
 * the KDBX investigation was set up to avoid. So both files are read with {@code keepassxc-cli} and
 * the two exports are compared.
 * <p>
 * <b>Which KeePassXC reads them is part of every result.</b> The fixture was written with KeePassXC
 * 2.7.10 and is read locally with 2.7.10. CI installs Ubuntu's package instead, because the runner
 * has no {@code keepassxc-minimal} ({@code E: Unable to locate package keepassxc-minimal}, CI run
 * 35078636917), and that package is 2.7.6. The two agreed on this fixture once, on the version of
 * this test that called the converter directly - on the same file, not in general. So every failure
 * message here names the version that read the files, and a case where the two ever read
 * differently says which one it ran against.
 * <p>
 * The fixture is real third-party material: KDBX 4.0, Argon2d, with its content and password
 * recorded in {@code src/test/resources/kdbx/README.md} (#380).
 * <p>
 * Deliberately outside the agreed scope, named here so nobody has to wonder: {@code UsageCount} and
 * {@code LocationChanged} are not asserted.
 */
class KdbxRoundTripKeepsEveryFieldUiTest extends AbstractUiTest
{

	/** The KeePassXC version that wrote the fixture, as recorded in the README beside it */
	private static final String FIXTURE_WRITTEN_WITH = "KeePassXC 2.7.10";

	/**
	 * The file name is kept as it is, because the import names its node after it and the exported
	 * group structure shows that name
	 */
	private static final String FIXTURE_NAME = "keepassxc-2.7.10-kdbx4.kdbx";

	/** The node every new vault starts with, which is not part of what is being round-tripped */
	private static final String STARTER_NODE = "mykeys";

	/** Test material, written down on purpose - see the README beside the fixture */
	private static final String PASSWORD = "fixture";

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private File original;
	private File exported;
	private KdbxFacts source;
	private KdbxFacts roundTripped;
	private String readWith;

	/**
	 * Names the instrument once per run, passed or failed: a green round trip that does not say
	 * which KeePassXC it compared with is a measurement without its measuring device. The line goes
	 * to standard output, which the test report keeps for passing tests too (the CI artifact
	 * {@code test-reports})
	 */
	@BeforeAll
	static void nameTheKeePassXcItIsMeasuredWith()
	{
		System.out.println("KDBX round trip measured with keepassxc-cli " + KeePassXcDump.version()
			+ "; the fixture was written with " + FIXTURE_WRITTEN_WITH);
	}

	@BeforeEach
	void importSaveReopenAndExportTheFixture() throws Exception
	{
		original = copyTheFixture();
		File vault = new File(tempHome, "kdbx-round-trip.mcrdb");
		exported = new File(tempHome, "round-tripped.kdbx");
		createDatabaseFileHeadless(vault, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(vault, MASTER_PASSWORD);
		application.deleteNode(application.showMainFrame(), STARTER_NODE);
		long importStarted = System.nanoTime();
		application.importKeePassDatabase(original, PASSWORD);
		long importMillis = (System.nanoTime() - importStarted) / 1_000_000;
		application.saveDatabase();
		shutdownApplication();
		ApplicationSteps reopened = signInWithExistingDatabase(vault, MASTER_PASSWORD);
		long exportStarted = System.nanoTime();
		reopened.exportKeePassDatabase(exported, PASSWORD);
		long exportMillis = (System.nanoTime() - exportStarted) / 1_000_000;
		// the import and export wait on a key derivation; what they took is what a timeout here
		// has to be measured against (#414)
		System.out.println("KDBX round trip: import step " + importMillis + " ms, export step "
			+ exportMillis + " ms");

		readWith = " [read with keepassxc-cli " + KeePassXcDump.version()
			+ "; the fixture was written with " + FIXTURE_WRITTEN_WITH + "]";
		source = KdbxFacts.of(KeePassXcDump.xmlOf(original, PASSWORD));
		roundTripped = KdbxFacts.of(KeePassXcDump.xmlOf(exported, PASSWORD));
	}

	@Test
	@DisplayName("the identifier of the entry and of every group survives")
	void identifiersSurvive()
	{
		assertEquals(source.onlyEntry().identifier(), roundTripped.onlyEntry().identifier(),
			"the entry identifier is what a KeePass user's other tools recognise the entry by; a "
				+ "new one on every export makes every export a different entry" + readWith);
		assertEquals(source.groups().stream().map(KdbxFacts.Group::identifier).toList(),
			roundTripped.groups().stream().map(KdbxFacts.Group::identifier).toList(),
			"the group identifiers have to survive as well, and in the same order" + readWith);
	}

	@Test
	@DisplayName("all four timestamps survive, and the expiry keeps its flag")
	void allFourTimestampsAndTheExpiryFlagSurvive()
	{
		KdbxFacts.Entry expected = source.onlyEntry();
		KdbxFacts.Entry actual = roundTripped.onlyEntry();

		assertEquals(expected.creationTime(), actual.creationTime(),
			"the creation time is the user's fact about their entry, not ours about our export"
				+ readWith);
		assertEquals(expected.lastModificationTime(), actual.lastModificationTime(),
			"the last modification time" + readWith);
		assertEquals(expected.lastAccessTime(), actual.lastAccessTime(),
			"the last access time" + readWith);
		assertEquals(expected.expiryTime(), actual.expiryTime(), "the expiry time" + readWith);
		assertEquals(expected.expires(), actual.expires(),
			"an expiry time without its flag expires nothing" + readWith);
	}

	@Test
	@DisplayName("the icon index survives, on the entry and on the groups")
	void iconIndicesSurvive()
	{
		assertEquals(source.onlyEntry().iconIndex(), roundTripped.onlyEntry().iconIndex(),
			"the entry's icon" + readWith);
		assertEquals(source.groups().stream().map(KdbxFacts.Group::iconIndex).toList(),
			roundTripped.groups().stream().map(KdbxFacts.Group::iconIndex).toList(),
			"a group's icon is as much the user's choice as an entry's" + readWith);
	}

	@Test
	@DisplayName("custom properties survive, including the protected one")
	void customPropertiesSurviveIncludingTheProtectedOne()
	{
		assertEquals(source.onlyEntry().customProperties(),
			roundTripped.onlyEntry().customProperties(), "the custom properties" + readWith);
		assertEquals(source.onlyEntry().customPropertyProtection(),
			roundTripped.onlyEntry().customPropertyProtection(),
			"a property the user marked protected must not come back unprotected - that is a "
				+ "secret quietly downgraded, and the fixture's 'key' is exactly that case (#389)"
				+ readWith);
	}

	@Test
	@DisplayName("the attachment survives with its content, byte for byte")
	void theAttachmentSurvivesWithItsContent() throws IOException
	{
		assertEquals(source.onlyEntry().attachmentNames(),
			roundTripped.onlyEntry().attachmentNames(), "the attachment names" + readWith);

		File fromSource = new File(tempHome, "source-attachment");
		File fromRoundTrip = new File(tempHome, "round-tripped-attachment");
		String attachment = source.onlyEntry().attachmentNames().get(0);
		KeePassXcDump.exportAttachment(original, PASSWORD, source.onlyEntry().keePassXcPath(),
			attachment, fromSource);
		KeePassXcDump.exportAttachment(exported, PASSWORD, roundTripped.onlyEntry().keePassXcPath(),
			attachment, fromRoundTrip);

		assertEquals(Files.readString(fromSource.toPath(), StandardCharsets.UTF_8),
			Files.readString(fromRoundTrip.toPath(), StandardCharsets.UTF_8),
			"the name of an attachment without its bytes is a file the user cannot open"
				+ readWith);
	}

	@Test
	@DisplayName("the group structure comes back with no added level and no renaming")
	void theGroupStructureIsUnchanged()
	{
		assertEquals(source.groupPaths(), roundTripped.groupPaths(),
			"a round trip that adds a level grows the tree on every export (#377), and one that "
				+ "renames a group loses what the user called it" + readWith);
	}

	/**
	 * What the header SHOULD say is the converter's decision to make (#378). What it must not say
	 * is the library's placeholder, which is what every export has carried so far: the name and
	 * description a user sees in KeePass's database list before opening anything.
	 * <p>
	 * Deliberately not "the source's name survives": this application's model has no field for a
	 * KeePass database name, and carrying one is not in the agreed scope.
	 */
	@Test
	@DisplayName("the exported header does not carry the library's placeholder name")
	void theExportedHeaderIsNotTheLibraryPlaceholder()
	{
		assertTrue(
			!"New Database".equals(roundTripped.databaseName())
				&& !String.valueOf(roundTripped.databaseDescription()).contains("KeePassJava2"),
			"an exported database announces itself as '" + roundTripped.databaseName() + "', '"
				+ roundTripped.databaseDescription()
				+ "' - the defaults of a library the user has never heard of (#378)" + readWith);

		assertEquals("kdbx-round-trip", roundTripped.databaseName(),
			"what the converter decided: the database is named after the vault it came from"
				+ readWith);
		assertEquals("Exported from mystic-crypt-ui", roundTripped.databaseDescription(),
			"and says where it was exported from" + readWith);
	}

	@Test
	@DisplayName("title, user name, password, notes and url survive, umlauts included")
	void theTextFieldsSurvive()
	{
		assertEquals(source.onlyEntry().title(), roundTripped.onlyEntry().title(),
			"the title" + readWith);
		assertEquals(source.onlyEntry().userName(), roundTripped.onlyEntry().userName(),
			"the user name" + readWith);
		assertEquals(source.onlyEntry().password(), roundTripped.onlyEntry().password(),
			"the password" + readWith);
		assertEquals(source.onlyEntry().notes(), roundTripped.onlyEntry().notes(),
			"the notes, which carry umlauts on purpose" + readWith);
		assertEquals(source.onlyEntry().url(), roundTripped.onlyEntry().url(),
			"the url" + readWith);
	}

	@Test
	@DisplayName("the entry's history is carried through")
	void theHistoryIsCarriedThrough()
	{
		assertEquals(source.onlyEntry().historyVersions(),
			roundTripped.onlyEntry().historyVersions(),
			"history is carried through unchanged rather than maintained, but carried through it "
				+ "must be - losing it silently discards versions the user kept" + readWith);
	}

	/**
	 * Not a defect of this application and not fixed here: when it writes KDBX 4, the library puts
	 * every attachment into the inner header AND leaves the binary pool in the XML, and says so
	 * itself in a TODO at {@code KdbxStreamFormat.java:81-90} (#379). The TODO sits in the stream
	 * format both of the library's serializers share.
	 * <p>
	 * What KeePassXC says about it depends on which serializer wrote the pool: the Simple one, used
	 * up to 8.5.1, made it report {@code overwriting binary item "0"}; the Jackson one, used since
	 * the converter moved to it (#384), makes it report {@code skip element "Binaries"} - measured
	 * with keepassxc-cli 2.7.10 on the file this test exports. Same cause, different words.
	 * <p>
	 * It is pinned as PRESENT rather than ignored, so that the day the upstream fix lands this test
	 * fails and says so, instead of a fixed defect going unnoticed.
	 */
	@Test
	@DisplayName("the library's binary warning is still there - known, not green")
	void theKnownBinaryWarningIsStillReported()
	{
		String warnings = KeePassXcDump.warningsWhileReading(exported, PASSWORD);

		assertTrue(warnings.contains("skip element \"Binaries\""),
			"KeePassXC no longer warns about the binary pool left in the XML. That is good news and "
				+ "this test is how it gets noticed: check whether KeePassJava2 fixed "
				+ "KdbxStreamFormat's TODO, then close #379 and delete this test. What it read was: "
				+ warnings + readWith);
	}

	/**
	 * Copies the fixture off the classpath under its own name, so the test does not depend on the
	 * directory the build runs from and the import names its node the way it would for a user
	 */
	private File copyTheFixture() throws IOException
	{
		File copy = new File(tempHome, FIXTURE_NAME);
		try (InputStream fixture = getClass().getResourceAsStream("/kdbx/" + FIXTURE_NAME))
		{
			if (fixture == null)
			{
				throw new IllegalStateException("/kdbx/" + FIXTURE_NAME
					+ " is not on the test classpath - the round trip has nothing written by a "
					+ "third party to measure against, and measuring our writer against our reader "
					+ "is what this test exists to avoid (#380)");
			}
			Files.copy(fixture, copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		return copy;
	}
}
