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
import java.util.Set;

import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;
import io.github.astrapi69.mystic.crypt.panel.properties.ProtectedPropertiesTableModel;

/**
 * What the "Properties" tab of the real "Edit Crypt Entry" dialog does with a property the entry
 * marks as protected (#946), for an entry that arrives the way a user's does: imported from the
 * KeePassXC fixture, whose custom attribute {@code key} is protected and whose {@code foo} is not.
 * <p>
 * {@code PropertiesPanelProtectedValuesTest} builds the panel over an entry it makes in memory. The
 * dialog does not: it works on {@code EntryEditing.copyOf} of the vault's entry, so a copy that
 * forgot the protected names would show every value in clear and, on OK, write the empty set onto
 * the entry, with that test green. These tests open the dialog on the way a user does.
 */
class AProtectedPropertyOfAnImportedEntryIsMaskedInTheEditDialogUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String FIXTURE = "keepassxc-2.7.10-kdbx4.kdbx";

	/** Test material, see the README beside the fixture */
	private static final String FIXTURE_PASSWORD = "fixture";

	private static final String TITLE = "Titel test foo";

	private static final String GROUP = "Team";

	/** The fixture's protected custom attribute and its value, see the README beside the fixture */
	private static final String PROTECTED_NAME = "key";

	private static final String PROTECTED_VALUE = "value";

	/** The fixture's plain custom attribute and its value */
	private static final String PLAIN_NAME = "foo";

	private static final String PLAIN_VALUE = "bar";

	private static final String RENAMED = "key renamed";

	private File vault;

	private ApplicationSteps importTheFixture() throws Exception
	{
		vault = new File(tempHome, "protected-property.mcrdb");
		createDatabaseFileHeadless(vault, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(vault, MASTER_PASSWORD);
		application.showMainFrame();
		application.importKeePassDatabase(KdbxFixtures.copy(FIXTURE, tempHome), FIXTURE_PASSWORD);
		return application;
	}

	private static EditCryptEntrySteps openTheEntry(final ApplicationSteps application,
		final FrameFixture frame)
	{
		application.selectTreeRowByName(frame, GROUP).selectEntryRowByTitle(frame, TITLE);
		return application.openTheEditDialog(frame);
	}

	@Test
	@DisplayName("the Properties table of the edit dialog masks the protected value and shows the plain one")
	void theTableOfTheEditDialogMasksTheProtectedValue() throws Exception
	{
		ApplicationSteps application = importTheFixture();
		FrameFixture frame = application.showMainFrame();
		assertEquals(Set.of(PROTECTED_NAME),
			VaultEntryProperties.protectedNamesOf(KdbxFixtures.liveEntryTitled(TITLE)),
			"the precondition: the import marked 'key' as the entry's protected property");

		EditCryptEntrySteps editor = openTheEntry(application, frame);

		assertEquals(ProtectedPropertiesTableModel.MASK,
			editor.valueShownInTheTable(PROTECTED_NAME),
			"the cell the user sees shows the protected value");
		assertEquals(ProtectedPropertiesTableModel.MASK,
			editor.valueTheTableModelGives(PROTECTED_NAME),
			"the model, which a copy of the row reads, hands out the protected value");
		assertEquals(PLAIN_VALUE, editor.valueShownInTheTable(PLAIN_NAME));
		editor.cancel();
		assertTrue(
			KdbxFixtures.liveEntryTitled(TITLE).getProperties().stream()
				.anyMatch(property -> PROTECTED_NAME.equals(property.getKey())
					&& PROTECTED_VALUE.equals(property.getValue())),
			"the mask is on the screen only, the entry still holds the value");
		assertEquals(Set.of(PROTECTED_NAME),
			VaultEntryProperties.protectedNamesOf(KdbxFixtures.liveEntryTitled(TITLE)),
			"Cancel leaves the protection as it was");
	}

	@Test
	@DisplayName("the property dialog of the edit dialog opens a protected value masked and shows it only on request")
	void thePropertyDialogOpensTheProtectedValueMaskedAndShowsItOnRequest() throws Exception
	{
		ApplicationSteps application = importTheFixture();
		FrameFixture frame = application.showMainFrame();
		EditCryptEntrySteps editor = openTheEntry(application, frame);

		EditCryptEntrySteps.PropertyDialogSteps property = editor
			.openPropertyForEditing(PROTECTED_NAME);

		assertTrue(property.valueIsMasked(), "the dialog opens the protected value in clear");
		assertTrue(property.showButtonIsShowing(), "nothing on screen offers to show the value");
		assertEquals(PROTECTED_VALUE, property.value());
		property.pressShow();
		assertFalse(property.valueIsMasked(), "the show button did not show the value");
		property.pressShow();
		assertTrue(property.valueIsMasked(), "the show button did not mask the value again");
		property.cancel();

		EditCryptEntrySteps.PropertyDialogSteps plain = editor.openPropertyForEditing(PLAIN_NAME);
		assertFalse(plain.valueIsMasked(), "a plain value opens masked");
		assertFalse(plain.showButtonIsShowing(), "a plain value offers to be shown");
		assertEquals(PLAIN_VALUE, plain.value());
		plain.cancel();
		editor.cancel();
	}

	@Test
	@DisplayName("a protected property that is renamed in the edit dialog stays masked, in the dialog and after saving and reopening")
	void aRenamedProtectedPropertyStaysMasked() throws Exception
	{
		ApplicationSteps application = importTheFixture();
		FrameFixture frame = application.showMainFrame();
		EditCryptEntrySteps editor = openTheEntry(application, frame);

		editor.renameProperty(PROTECTED_NAME, RENAMED);

		assertEquals(ProtectedPropertiesTableModel.MASK, editor.valueShownInTheTable(RENAMED),
			"the renamed property shows its value in the table");
		editor.ok();
		assertEquals(Set.of(RENAMED),
			VaultEntryProperties.protectedNamesOf(KdbxFixtures.liveEntryTitled(TITLE)),
			"OK writes the protection of the renamed property onto the entry");
		application.saveDatabase();
		shutdownApplication();

		ApplicationSteps reopened = signInWithExistingDatabase(vault, MASTER_PASSWORD);
		EditCryptEntrySteps again = openTheEntry(reopened, reopened.showMainFrame());
		assertEquals(ProtectedPropertiesTableModel.MASK, again.valueShownInTheTable(RENAMED),
			"after saving and reopening, the renamed property shows its value in the table");
		assertEquals(PLAIN_VALUE, again.valueShownInTheTable(PLAIN_NAME));
		again.cancel();
		assertEquals(Set.of(RENAMED),
			VaultEntryProperties.protectedNamesOf(KdbxFixtures.liveEntryTitled(TITLE)),
			"the vault file keeps the protection under the new name");
	}
}
