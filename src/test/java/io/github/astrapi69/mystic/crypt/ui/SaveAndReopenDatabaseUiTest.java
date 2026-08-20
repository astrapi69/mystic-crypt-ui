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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * End-to-end use case "changes survive a restart": import a KeePass database, save via the File
 * menu, shut the application down, sign in again - the imported group must still be in the tree.
 * This is the full persistence round trip through the UI, the guarantee every password manager
 * lives or dies by
 */
class SaveAndReopenDatabaseUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "reopen-db-pw-123";
	/** Credentials of the checked-in test fixture src/test/resources/test-db.kdbx */
	private static final String KEEPASS_PASSWORD = "foo-secret-bar-1969-?";

	@Test
	void importedDataIsStillThereAfterSaveAndReopen() throws Exception
	{
		File databaseFile = new File(tempHome, "reopen-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		File keePassFile = new File("src/test/resources/test-db.kdbx").getAbsoluteFile();

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.importKeePassDatabase(keePassFile, KEEPASS_PASSWORD);
		assertTrue(application.treeContainsNodeStartingWith("Imported from test-db.kdbx"),
			"the imported group must be in the tree before saving");
		application.saveDatabase();

		shutdownApplication();

		ApplicationSteps reopened = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		assertTrue(reopened.treeContainsNodeStartingWith("Imported from test-db.kdbx"),
			"the imported group must still be in the tree after save and reopen");
	}
}
