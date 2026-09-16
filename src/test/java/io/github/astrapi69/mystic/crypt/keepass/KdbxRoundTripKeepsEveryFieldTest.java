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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;
import org.linguafranca.pwdb.kdbx.simple.SimpleGroup;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * A database written by KeePassXC, imported and exported by this application, has to come back the
 * same.
 * <p>
 * The yardstick is KeePassXC, not this application's model. A field the model does not carry would
 * compare equal to itself and the test would pass while the file lost it - the circular measurement
 * the KDBX investigation was set up to avoid. So both files are read with {@code keepassxc-cli} and
 * the two exports are compared.
 * <p>
 * The fixture is real third-party material: KDBX 4.0, Argon2d, written with KeePassXC 2.7.10, with
 * its content and password recorded in {@code src/test/resources/kdbx/README.md} (#380).
 * <p>
 * Deliberately outside the agreed scope, named here so nobody has to wonder: {@code UsageCount} and
 * {@code LocationChanged} are not asserted.
 */
class KdbxRoundTripKeepsEveryFieldTest
{

	/**
	 * Read off the classpath rather than as a relative path, so the test does not depend on the
	 * directory the build happens to run from
	 */
	private static final String FIXTURE_RESOURCE = "/kdbx/keepassxc-2.7.10-kdbx4.kdbx";

	/** Test material, written down on purpose - see the README beside the fixture */
	private static final String PASSWORD = "fixture";

	@TempDir
	File workingDirectory;

	private KdbxFacts source;
	private KdbxFacts roundTripped;
	private File exported;

	@BeforeEach
	void importAndExportTheFixture() throws IOException
	{
		File original = new File(workingDirectory, "source.kdbx");
		try (InputStream fixture = getClass().getResourceAsStream(FIXTURE_RESOURCE))
		{
			if (fixture == null)
			{
				throw new IllegalStateException(FIXTURE_RESOURCE
					+ " is not on the test classpath - the round trip has nothing written by a "
					+ "third party to measure against, and measuring our writer against our reader "
					+ "is what this test exists to avoid (#380)");
			}
			Files.copy(fixture, original.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		exported = new File(workingDirectory, "round-tripped.kdbx");

		exportTree(importTree(original), exported);

		source = KdbxFacts.of(KeePassXcDump.xmlOf(original, PASSWORD));
		roundTripped = KdbxFacts.of(KeePassXcDump.xmlOf(exported, PASSWORD));
	}

	@Test
	@DisplayName("the identifier of the entry and of every group survives")
	void identifiersSurvive()
	{
		assertEquals(source.onlyEntry().identifier(), roundTripped.onlyEntry().identifier(),
			"the entry identifier is what a KeePass user's other tools recognise the entry by; a "
				+ "new one on every export makes every export a different entry");
		assertEquals(source.groups().stream().map(KdbxFacts.Group::identifier).toList(),
			roundTripped.groups().stream().map(KdbxFacts.Group::identifier).toList(),
			"the group identifiers have to survive as well, and in the same order");
	}

	@Test
	@DisplayName("all four timestamps survive, and the expiry keeps its flag")
	void allFourTimestampsAndTheExpiryFlagSurvive()
	{
		KdbxFacts.Entry expected = source.onlyEntry();
		KdbxFacts.Entry actual = roundTripped.onlyEntry();

		assertEquals(expected.creationTime(), actual.creationTime(),
			"the creation time is the user's fact about their entry, not ours about our export");
		assertEquals(expected.lastModificationTime(), actual.lastModificationTime());
		assertEquals(expected.lastAccessTime(), actual.lastAccessTime());
		assertEquals(expected.expiryTime(), actual.expiryTime());
		assertEquals(expected.expires(), actual.expires(),
			"an expiry time without its flag expires nothing");
	}

	@Test
	@DisplayName("the icon index survives, on the entry and on the groups")
	void iconIndicesSurvive()
	{
		assertEquals(source.onlyEntry().iconIndex(), roundTripped.onlyEntry().iconIndex());
		assertEquals(source.groups().stream().map(KdbxFacts.Group::iconIndex).toList(),
			roundTripped.groups().stream().map(KdbxFacts.Group::iconIndex).toList(),
			"a group's icon is as much the user's choice as an entry's");
	}

	@Test
	@DisplayName("custom properties survive, including the protected one")
	void customPropertiesSurviveIncludingTheProtectedOne()
	{
		assertEquals(source.onlyEntry().customProperties(),
			roundTripped.onlyEntry().customProperties());
		assertEquals(source.onlyEntry().customPropertyProtection(),
			roundTripped.onlyEntry().customPropertyProtection(),
			"a property the user marked protected must not come back unprotected - that is a "
				+ "secret quietly downgraded, and the fixture's 'key' is exactly that case");
	}

	@Test
	@DisplayName("the attachment survives with its content, byte for byte")
	void theAttachmentSurvivesWithItsContent() throws IOException
	{
		assertEquals(source.onlyEntry().attachmentNames(),
			roundTripped.onlyEntry().attachmentNames());

		File fromSource = new File(workingDirectory, "source-attachment");
		File fromRoundTrip = new File(workingDirectory, "round-tripped-attachment");
		String attachment = source.onlyEntry().attachmentNames().get(0);
		KeePassXcDump.exportAttachment(new File(workingDirectory, "source.kdbx"), PASSWORD,
			source.onlyEntry().keePassXcPath(), attachment, fromSource);
		KeePassXcDump.exportAttachment(exported, PASSWORD, roundTripped.onlyEntry().keePassXcPath(),
			attachment, fromRoundTrip);

		assertEquals(Files.readString(fromSource.toPath(), StandardCharsets.UTF_8),
			Files.readString(fromRoundTrip.toPath(), StandardCharsets.UTF_8),
			"the name of an attachment without its bytes is a file the user cannot open");
	}

	@Test
	@DisplayName("the group structure comes back with no added level and no renaming")
	void theGroupStructureIsUnchanged()
	{
		assertEquals(source.groupPaths(), roundTripped.groupPaths(),
			"a round trip that adds a level grows the tree on every export (#377), and one that "
				+ "renames the root loses what the user called it");
	}

	@Test
	@DisplayName("title, user name, password, notes and url survive, umlauts included")
	void theTextFieldsSurvive()
	{
		assertEquals(source.onlyEntry().title(), roundTripped.onlyEntry().title());
		assertEquals(source.onlyEntry().userName(), roundTripped.onlyEntry().userName());
		assertEquals(source.onlyEntry().password(), roundTripped.onlyEntry().password());
		assertEquals(source.onlyEntry().notes(), roundTripped.onlyEntry().notes());
		assertEquals(source.onlyEntry().url(), roundTripped.onlyEntry().url());
	}

	@Test
	@DisplayName("the entry's history is carried through")
	void theHistoryIsCarriedThrough()
	{
		assertEquals(source.onlyEntry().historyVersions(),
			roundTripped.onlyEntry().historyVersions(),
			"history is carried through unchanged rather than maintained, but carried through it "
				+ "must be - losing it silently discards versions the user kept");
	}

	/**
	 * Not a defect of this application and not fixed here: the library duplicates every attachment
	 * into the XML as well as the inner header when it writes KDBX 4, and says so itself in a TODO
	 * at {@code KdbxStreamFormat.java:81-90}. KeePassXC reports it on every read.
	 * <p>
	 * It is pinned as PRESENT rather than ignored, so that the day the upstream fix lands this test
	 * fails and says so, instead of a fixed defect going unnoticed (#379).
	 */
	@Test
	@DisplayName("the library's binary warning is still there - known, not green")
	void theKnownBinaryWarningIsStillReported()
	{
		String warnings = KeePassXcDump.warningsWhileReading(exported, PASSWORD);

		assertTrue(warnings.contains("binary item"),
			"KeePassXC no longer warns about the duplicated binary pool. That is good news and this "
				+ "test is how it gets noticed: check whether KeePassJava2 fixed "
				+ "KdbxStreamFormat's TODO, then close #379 and delete this test. What it read was: "
				+ warnings);
	}

	/**
	 * The two code paths the actions use, with nothing else around them:
	 * {@code ImportKeePassDatabaseAction:117} and {@code :166}
	 */
	private BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> importTree(
		final File database) throws IOException
	{
		AtomicLong identifiers = new AtomicLong(1);
		try (InputStream in = new FileInputStream(database))
		{
			SimpleDatabase loaded = SimpleDatabase.load(credentials(), in);
			GenericTreeElement<List<MysticCryptEntryModelBean>> rootElement = new GenericTreeElement<>();
			rootElement.setName("root");
			BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = BaseTreeNode
				.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder()
				.id(identifiers.getAndIncrement()).value(rootElement).displayValue("root")
				.leaf(false).build();
			KeePassTreeConverter.toTreeNode(loaded.getRootGroup(), root,
				identifiers::getAndIncrement);
			return root;
		}
	}

	/** {@code ExportKeePassDatabaseAction:111-121} */
	private void exportTree(
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root,
		final File target) throws IOException
	{
		SimpleDatabase database = new SimpleDatabase();
		SimpleGroup rootGroup = database.getRootGroup();
		for (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> child : root
			.getChildren())
		{
			KeePassTreeConverter.toSimpleGroup(database, child, rootGroup);
		}
		try (OutputStream out = new FileOutputStream(target))
		{
			database.save(credentials(), out);
		}
	}

	private static KdbxCreds credentials()
	{
		return new KdbxCreds(PASSWORD.getBytes(StandardCharsets.UTF_8));
	}
}
