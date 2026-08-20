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
import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;

/**
 * End-to-end use case "import a KeePass database": sign in to an existing database, open the File
 * menu's import action, choose the {@code .kdbx} file, enter its password, confirm - the entries
 * must appear in the tree under a new "Imported from ..." group and the model must be marked dirty
 * so the change gets saved
 */
class KeePassImportUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "import-db-pw-123";
	/** Credentials of the checked-in test fixture src/test/resources/test-db.kdbx */
	private static final String KEEPASS_PASSWORD = "foo-secret-bar-1969-?";

	@Test
	void importKeePassDatabaseShowsEntriesInTree() throws IOException
	{
		File databaseFile = new File(tempHome, "import-target-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		File keePassFile = new File("src/test/resources/test-db.kdbx").getAbsoluteFile();
		assertTrue(keePassFile.exists(), "test fixture test-db.kdbx must exist");

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		application.importKeePassDatabase(keePassFile, KEEPASS_PASSWORD);

		assertTrue(application.treeContainsNodeStartingWith("Imported from test-db.kdbx"),
			"the tree must contain the new 'Imported from ...' group after the import");
		assertTrue(MysticCryptApplicationFrame.getInstance().getModelObject().isDirty(),
			"a successful import must mark the model dirty so it gets saved");
	}
}
