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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * What the tables of the edit dialog do to the data of an entry, driven through the dialog and read
 * back from a vault file after a save and a restart: the "Properties" table (rows are
 * {@code Map.Entry} views of the entry's own pairs since swing-table-components 4.0, #1047) and the
 * "Attachments" table. Removing and changing are the data paths the other round trips do not take:
 * they add, and they cancel (#1047, decided 2026-11-19: a gap that touches data gets its test
 * before the cut-off).
 */
class EntryTablesKeepRemovalsAndEditsUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String TITLE = "Mail Account";

	@Test
	@DisplayName("a property removed and another one changed in the table, a save and a restart: the file holds the other two, with the new value, and not the removed one")
	void removedAndChangedProperties_comeBackAsTheyWere() throws Exception
	{
		File databaseFile = new File(tempHome, "properties-remove-edit.mcrdb");
		ApplicationSteps application = signInWithAnEntry(databaseFile);
		FrameFixture frame = application.showMainFrame();
		application.selectEntryRowByTitle(frame, TITLE);
		application.openTheEditDialog(frame).addProperty("pin", "4711")
			.addProperty("seed", "JBSWY3DPEHPK3PXP").addProperty("note", "first value").ok();
		application.saveDatabase();

		application.selectEntryRowByTitle(frame, TITLE);
		application.openTheEditDialog(frame).removeProperty("seed")
			.changeProperty("note", "second value").ok();

		Map<String, String> expected = Map.of("pin", "4711", "note", "second value");
		assertEquals(expected, theOnlyEntryProperties(),
			"precondition: the open database holds the removal and the change");
		application.saveDatabase();
		shutdownApplication();
		signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		assertEquals(expected, theOnlyEntryProperties(),
			"the saved file holds the properties that were left, and the changed value");
	}

	@Test
	@DisplayName("a property removed in the table and the dialog cancelled: the entry keeps both, in memory and in the file")
	void aRemovalThatIsCancelled_leavesThePropertiesAsTheyWere() throws Exception
	{
		File databaseFile = new File(tempHome, "properties-remove-cancel.mcrdb");
		ApplicationSteps application = signInWithAnEntry(databaseFile);
		FrameFixture frame = application.showMainFrame();
		application.selectEntryRowByTitle(frame, TITLE);
		application.openTheEditDialog(frame).addProperty("pin", "4711")
			.addProperty("seed", "JBSWY3DPEHPK3PXP").ok();
		application.saveDatabase();

		application.selectEntryRowByTitle(frame, TITLE);
		application.openTheEditDialog(frame).removeProperty("seed").cancel();

		Map<String, String> expected = Map.of("pin", "4711", "seed", "JBSWY3DPEHPK3PXP");
		assertEquals(expected, theOnlyEntryProperties(),
			"Cancel after a removal leaves both properties on the entry");
		shutdownApplication();
		signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		assertEquals(expected, theOnlyEntryProperties(),
			"the file, which Cancel did not touch, holds both properties");
	}

	@Test
	@DisplayName("an attachment removed in the table, a save and a restart: the file holds the other one, name and SHA-256, and not the removed one")
	void aRemovedAttachment_isGoneAfterSaveAndRestart() throws Exception
	{
		File databaseFile = new File(tempHome, "attachment-remove.mcrdb");
		File text = AttachmentFixtures.writeText(tempHome);
		File binary = AttachmentFixtures.writeEveryByteValue(tempHome);
		ApplicationSteps application = signInWithAnEntry(databaseFile);
		FrameFixture frame = application.showMainFrame();
		application.selectEntryRowByTitle(frame, TITLE);
		application.openTheEditDialog(frame).addAttachment(text).addAttachment(binary).ok();
		application.saveDatabase();
		assertEquals(2, theOnlyEntryAttachments().size(), "precondition: two attachments saved");

		application.selectEntryRowByTitle(frame, TITLE);
		application.openTheEditDialog(frame).removeAttachment(AttachmentFixtures.TEXT_NAME).ok();
		application.saveDatabase();
		shutdownApplication();
		signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		Map<String, String> expected = new TreeMap<>();
		expected.put(binary.getName(), AttachmentFixtures.sha256Hex(binary));
		assertEquals(expected, theOnlyEntryAttachments(),
			"the saved file holds the attachment that was left, byte for byte by its SHA-256");
	}

	private ApplicationSteps signInWithAnEntry(File databaseFile) throws Exception
	{
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, TITLE, "mail-user", TestPasswords.throwaway());
		application.saveDatabase();
		return application;
	}

	private static Map<String, String> theOnlyEntryProperties()
	{
		return GuiActionRunner.execute(() -> {
			List<KeyValuePair<String, String>> properties = theOnlyEntry().getProperties() == null
				? List.of()
				: theOnlyEntry().getProperties();
			return properties.stream()
				.collect(Collectors.toMap(KeyValuePair::getKey, KeyValuePair::getValue));
		});
	}

	private static Map<String, String> theOnlyEntryAttachments()
	{
		return GuiActionRunner.execute(() -> AttachmentFixtures.byNameAndSha256(theOnlyEntry()));
	}

	/**
	 * The one entry of the open database, from the tree a save reads it from; on the event thread
	 */
	private static MysticCryptEntryModelBean theOnlyEntry()
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = MysticCryptApplicationFrame
			.getInstance().getApplicationPanel().getSecretKeyTreeWithContentPanel()
			.getModelObject();
		List<MysticCryptEntryModelBean> entries = root.traverse().stream()
			.filter(node -> node.getValue() != null && node.getValue().getDefaultContent() != null)
			.flatMap(node -> node.getValue().getDefaultContent().stream()).toList();
		assertEquals(1, entries.size(), "the database holds exactly one entry");
		return entries.get(0);
	}
}
