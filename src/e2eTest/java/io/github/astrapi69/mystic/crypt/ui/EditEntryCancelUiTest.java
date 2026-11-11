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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.EntryText;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * What the dialog "Edit Crypt Entry" leaves on the entry: OK takes what was typed, Cancel what was
 * there (#881).
 * <p>
 * The dialog's fields write on every keystroke. Bound to the entry that lives in the tree, they
 * changed it while the user typed, and Cancel kept what was typed - in the tree and the table, and
 * in the file at the next save of anything else, while the changed flag knew nothing about it. The
 * dialog now works on a copy of the entry, property pairs included.
 */
class EditEntryCancelUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String TITLE = "Mail Account";

	private static final String TYPED_TITLE = "Mail Account Typed";

	private static final String PROPERTY = "pin";

	private static final String PROPERTY_VALUE = "4711";

	private static final String TYPED_PROPERTY_VALUE = "0815";

	@Test
	@DisplayName("Cancel leaves title, password and properties as they were, and the changed flag clear")
	void cancel_leavesTheEntryAsItWas() throws Exception
	{
		String password = TestPasswords.throwaway();
		String typedPassword = TestPasswords.throwaway();
		ApplicationSteps application = signInWithASavedEntry("edit-cancel.mcrdb", password);
		FrameFixture frame = application.showMainFrame();

		application.selectEntryRowByTitle(frame, TITLE);
		application.openTheEditDialog(frame).typeTitle(TYPED_TITLE).typePassword(typedPassword)
			.addProperty(PROPERTY, PROPERTY_VALUE).cancel();

		assertEquals(new EntrySnapshot(TITLE, password, password, Map.of()), theOnlyEntry(),
			"Cancel leaves the entry as it was");
		assertFalse(databaseIsMarkedChanged(), "Cancel does not mark the database as changed");
	}

	@Test
	@DisplayName("Cancel leaves a property's value as it was when the dialog changed it")
	void cancel_leavesAPropertyValueAsItWas() throws Exception
	{
		ApplicationSteps application = signInWithASavedEntry("edit-cancel-property.mcrdb",
			TestPasswords.throwaway());
		FrameFixture frame = application.showMainFrame();
		application.selectEntryRowByTitle(frame, TITLE);
		application.openTheEditDialog(frame).addProperty(PROPERTY, PROPERTY_VALUE).ok();
		application.saveDatabase();

		application.selectEntryRowByTitle(frame, TITLE);
		application.openTheEditDialog(frame).changeProperty(PROPERTY, TYPED_PROPERTY_VALUE)
			.cancel();

		assertEquals(Map.of(PROPERTY, PROPERTY_VALUE), theOnlyEntry().properties(),
			"the property keeps its value");
		assertFalse(databaseIsMarkedChanged(), "Cancel does not mark the database as changed");
	}

	@Test
	@DisplayName("OK takes the typed title, password and property, marks the database changed, and a save keeps them")
	void ok_takesWhatWasTyped() throws Exception
	{
		String typedPassword = TestPasswords.throwaway();
		File databaseFile = new File(tempHome, "edit-ok.mcrdb");
		ApplicationSteps application = signInWithASavedEntry(databaseFile.getName(),
			TestPasswords.throwaway());
		FrameFixture frame = application.showMainFrame();

		application.selectEntryRowByTitle(frame, TITLE);
		application.openTheEditDialog(frame).typeTitle(TYPED_TITLE).typePassword(typedPassword)
			.addProperty(PROPERTY, PROPERTY_VALUE).ok();

		EntrySnapshot edited = theOnlyEntry();
		assertEquals(new EntrySnapshot(TYPED_TITLE, typedPassword, typedPassword,
			Map.of(PROPERTY, PROPERTY_VALUE)), edited, "OK takes everything that was typed");
		assertTrue(databaseIsMarkedChanged(), "OK marks the database as changed");

		application.saveDatabase();
		shutdownApplication();
		signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		assertEquals(edited, theOnlyEntry(), "the saved file holds what OK took");
	}

	private ApplicationSteps signInWithASavedEntry(String fileName, String password)
		throws Exception
	{
		File databaseFile = new File(tempHome, fileName);
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, TITLE, "mail-user", password);
		application.saveDatabase();
		return application;
	}

	/**
	 * What the one entry of the open database holds, read on the event thread from the tree the
	 * nodes hang in, which is where a save reads it from
	 */
	private static EntrySnapshot theOnlyEntry()
	{
		return GuiActionRunner.execute(() -> {
			BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = MysticCryptApplicationFrame
				.getInstance().getApplicationPanel().getSecretKeyTreeWithContentPanel()
				.getModelObject();
			List<MysticCryptEntryModelBean> entries = root.traverse().stream()
				.filter(
					node -> node.getValue() != null && node.getValue().getDefaultContent() != null)
				.flatMap(node -> node.getValue().getDefaultContent().stream()).toList();
			assertEquals(1, entries.size(), "the database holds exactly one entry");
			return EntrySnapshot.of(entries.get(0));
		});
	}

	private static boolean databaseIsMarkedChanged()
	{
		return GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getModelObject().isDirty());
	}

	/** The parts of an entry the dialog can change, as text */
	private record EntrySnapshot(String title, String password, String repeat,
		Map<String, String> properties) {
		static EntrySnapshot of(final MysticCryptEntryModelBean entry)
		{
			List<KeyValuePair<String, String>> properties = entry.getProperties() == null
				? List.of()
				: entry.getProperties();
			return new EntrySnapshot(EntryText.asText(entry.getTitle()),
				EntryText.asText(entry.getPassword()), EntryText.asText(entry.getRepeat()),
				properties.stream()
					.collect(Collectors.toMap(KeyValuePair::getKey, KeyValuePair::getValue)));
		}
	}
}
